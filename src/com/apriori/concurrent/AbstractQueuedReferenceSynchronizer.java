package com.apriori.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * Provides a framework for implementing blocking locks and related synchronizer that rely on FIFO
 * wait queues. This variant can be useful above and beyond its kin,
 * {@link AbstractQueuedSynchronizer} and {@link AbstractQueuedLongSynchronizer}, since it allows
 * for atomically maintaining state outside of just 32- and 64-bit numeric values. With those
 * classes, encoding multiple counters into a single state value can mean drastic loss in precision,
 * often manifesting as arbitrary limits in the usage of derived synchronizers. This class provides
 * an alternative that allows you to instead encode multiple full-precision counters into a simple
 * value type. A reference to that value type is the managed state, not just a fixed-precision
 * numeric value.
 * 
 * <p>
 * Aside from the type of value used to represent synchronizer state, this class provides the same
 * API as its kin with one main exception: {@link Condition}s are not supported.
 *
 * @param <S> the type of state atomically maintained by the synchronizer
 * @param <R> the type of request used during acquisitions and releases
 * 
 * @see AbstractQueuedSynchronizer
 * @see AbstractQueuedLongSynchronizer
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class AbstractQueuedReferenceSynchronizer<S, R> extends AbstractOwnableSynchronizer {
   
   /*
    * This implementation was copied from AbstractQueueSynchronizer and then just adjusted for the
    * different type of state variable and to remove support for Conditions.
    */

   private static final long serialVersionUID = 2087545583039578782L;

   @SuppressWarnings("rawtypes")
   static final AtomicReferenceFieldUpdater<AbstractQueuedReferenceSynchronizer, Object>
   stateUpdater =
         AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedReferenceSynchronizer.class,
               Object.class, "state");

   @SuppressWarnings("rawtypes")
   static final AtomicReferenceFieldUpdater<AbstractQueuedReferenceSynchronizer, Node> headUpdater =
         AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedReferenceSynchronizer.class,
               Node.class, "head");

   @SuppressWarnings("rawtypes")
   static final AtomicReferenceFieldUpdater<AbstractQueuedReferenceSynchronizer, Node> tailUpdater =
         AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedReferenceSynchronizer.class,
               Node.class, "tail");

   static final AtomicIntegerFieldUpdater<Node> waitStatusUpdater = AtomicIntegerFieldUpdater
         .newUpdater(Node.class, "waitStatus");

   static final AtomicReferenceFieldUpdater<Node, Node> nextUpdater = AtomicReferenceFieldUpdater
         .newUpdater(Node.class, Node.class, "next");

   /**
    * Creates a new instance with initial synchronization state of {@code null}.
    */
   protected AbstractQueuedReferenceSynchronizer() {
   }

   /**
    * Wait queue node class.
    *
    * <p>
    * The wait queue is a variant of a "CLH" (Craig, Landin, and Hagersten) lock queue. CLH locks
    * are normally used for spinlocks. We instead use them for blocking synchronizers, but use the
    * same basic tactic of holding some of the control information about a thread in the predecessor
    * of its node. A "status" field in each node keeps track of whether a thread should block. A
    * node is signalled when its predecessor releases. Each node of the queue otherwise serves as a
    * specific-notification-style monitor holding a single waiting thread. The status field does NOT
    * control whether threads are granted locks etc though. A thread may try to acquire if it is
    * first in the queue. But being first does not guarantee success; it only gives the right to
    * contend. So the currently released contender thread may need to rewait.
    *
    * <p>
    * To enqueue into a CLH lock, you atomically splice it in as new tail. To dequeue, you just set
    * the head field.
    * 
    * <pre>
    *      +------+  prev +-----+       +-----+
    * head |      | <---- |     | <---- |     |  tail
    *      +------+       +-----+       +-----+
    * </pre>
    *
    * <p>
    * Insertion into a CLH queue requires only a single atomic operation on "tail", so there is a
    * simple atomic point of demarcation from unqueued to queued. Similarly, dequeuing involves only
    * updating the "head". However, it takes a bit more work for nodes to determine who their
    * successors are, in part to deal with possible cancellation due to timeouts and interrupts.
    *
    * <p>
    * The "prev" links (not used in original CLH locks), are mainly needed to handle cancellation.
    * If a node is cancelled, its successor is (normally) relinked to a non-cancelled predecessor.
    * For explanation of similar mechanics in the case of spin locks, see the <a
    * href="http://www.cs.rochester.edu/u/scott/synchronization/">papers by Scott and Scherer</a>.
    *
    * <p>
    * We also use "next" links to implement blocking mechanics. The thread id for each node is kept
    * in its own node, so a predecessor signals the next node to wake up by traversing next link to
    * determine which thread it is. Determination of successor must avoid races with newly queued
    * nodes to set the "next" fields of their predecessors. This is solved when necessary by
    * checking backwards from the atomically updated "tail" when a node's successor appears to be
    * null. (Or, said differently, the next-links are an optimization so that we don't usually need
    * a backward scan.)
    *
    * <p>
    * Cancellation introduces some conservatism to the basic algorithms. Since we must poll for
    * cancellation of other nodes, we can miss noticing whether a cancelled node is ahead or behind
    * us. This is dealt with by always unparking successors upon cancellation, allowing them to
    * stabilize on a new predecessor, unless we can identify an uncancelled predecessor who will
    * carry this responsibility.
    *
    * <p>
    * CLH queues need a dummy header node to get started. But we don't create them on construction,
    * because it would be wasted effort if there is never contention. Instead, the node is
    * constructed and head and tail pointers are set upon first contention.
    *
    * <p>
    * Threads waiting on Conditions use the same nodes, but use an additional link. Conditions only
    * need to link nodes in simple (non-concurrent) linked queues because they are only accessed
    * when exclusively held. Upon await, a node is inserted into a condition queue. Upon signal, the
    * node is transferred to the main queue. A special value of status field is used to mark which
    * queue a node is on.
    *
    * <p>
    * Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill Scherer and Michael Scott, along
    * with members of JSR-166 expert group, for helpful ideas, discussions, and critiques on the
    * design of this class.
    */
   static final class Node {

      /** Marker to indicate a node is waiting in shared mode. */
      static final Node SHARED = new Node();

      /** Marker to indicate a node is waiting in exclusive mode. */
      static final Node EXCLUSIVE = null;

      /** Wait status that indicates thread has cancelled. */
      static final int CANCELLED = 1;

      /** Wait status that indicates successor's thread needs unparking. */
      static final int SIGNAL = -1;

      /** Wait status that indicates the next acquireShared should unconditionally propagate */
      static final int PROPAGATE = -3;

      /**
       * Status field, taking on only the values:
       * <dl>
       * <dt>SIGNAL</dt>
       * <dd>The successor of this node is (or will soon be) blocked (via park), so the current node
       * must unpark its successor when it releases or cancels. To avoid races, acquire methods must
       * first indicate they need a signal, then retry the atomic acquire, and then, on failure,
       * block.</dd>
       * <dt>CANCELLED</dt>
       * <dd>This node is cancelled due to timeout or interrupt. Nodes never leave this state. In
       * particular, a thread with cancelled node never again blocks.</dd>
       * <dt>CONDITION</dt>
       * <dd>This node is currently on a condition queue. It will not be used as a sync queue node
       * until transferred, at which time the status will be set to 0. (Use of this value here has
       * nothing to do with the other uses of the field, but simplifies mechanics.)</dd>
       * <dt>PROPAGATE</dt>
       * <dd>A releaseShared should be propagated to other nodes. This is set (for head node only)
       * in doReleaseShared to ensure propagation continues, even if other operations have since
       * intervened.</dd>
       * <dt>0</dt>
       * <dd>None of the above.</dd>
       * </dl>
       *
       * <p>
       * The values are arranged numerically to simplify use. Non-negative values mean that a node
       * doesn't need to signal. So, most code doesn't need to check for particular values, just for
       * sign.
       *
       * <p>
       * The field is initialized to 0 for normal sync nodes, and {@link #CONDITION} for condition
       * nodes. It is modified using CAS (or when possible, unconditional volatile writes).
       */
      volatile int waitStatus;

      /**
       * Link to predecessor node that current node/thread relies on for checking waitStatus.
       * Assigned during enqueuing, and nulled out (for sake of GC) only upon dequeuing. Also, upon
       * cancellation of a predecessor, we short-circuit while finding a non-cancelled one, which
       * will always exist because the head node is never cancelled: A node becomes head only as a
       * result of successful acquire. A cancelled thread never succeeds in acquiring, and a thread
       * only cancels itself, not any other node.
       */
      volatile Node prev;

      /**
       * Link to the successor node that the current node/thread unparks upon release. Assigned
       * during enqueuing, adjusted when bypassing cancelled predecessors, and nulled out (for sake
       * of GC) when dequeued. The enq operation does not assign next field of a predecessor until
       * after attachment, so seeing a null next field does not necessarily mean that node is at end
       * of queue. However, if a next field appears to be null, we can scan prev's from the tail to
       * double-check. The next field of cancelled nodes is set to point to the node itself instead
       * of null, to make life easier for isOnSyncQueue.
       */
      volatile Node next;

      /**
       * The thread that enqueued this node. Initialized on construction and nulled out after use.
       */
      volatile Thread thread;

      /**
       * Link to next node waiting on condition, or the special value {@link #SHARED}. Because
       * condition queues are accessed only when holding in exclusive mode, we just need a simple
       * linked queue to hold nodes while they are waiting on conditions. They are then transferred
       * to the queue to re-acquire. And because conditions can only be exclusive, we save a field
       * by using special value to indicate shared mode.
       */
      Node nextWaiter;

      // Used to establish initial head or SHARED marker
      Node() {
      }

      // Used by addWaiter
      Node(Thread thread, Node mode) {
         this.nextWaiter = mode;
         this.thread = thread;
      }

      // Used by Condition
      Node(Thread thread, int waitStatus) {
         this.waitStatus = waitStatus;
         this.thread = thread;
      }

      /**
       * Returns true if node is waiting in shared mode.
       */
      final boolean isShared() {
         return nextWaiter == SHARED;
      }

      /**
       * Returns previous node, or throws NullPointerException if null. Use when predecessor cannot
       * be null. The null check could be elided, but is present to help the VM.
       *
       * @return the predecessor of this node
       */
      final Node predecessor() throws NullPointerException {
         Node p = prev;
         if (p == null) {
            throw new NullPointerException();
         }
         return p;
      }
   }

   /**
    * Head of the wait queue, lazily initialized. Except for initialization, it is modified only via
    * method setHead. Note: If head exists, its waitStatus is guaranteed not to be
    * {@link #CANCELLED}.
    */
   private transient volatile Node head;

   /**
    * Tail of the wait queue, lazily initialized. Modified only via method enq to add new wait node.
    */
   private transient volatile Node tail;

   /**
    * The synchronization state.
    */
   private volatile S state;

   /**
    * Returns the current value of synchronization state. This operation has memory semantics of a
    * {@code volatile} read.
    * 
    * @return current state value
    */
   protected final S getState() {
      return state;
   }

   /**
    * Sets the value of synchronization state. This operation has memory semantics of a
    * {@code volatile} write.
    * 
    * @param newState the new state value
    */
   protected final void setState(S newState) {
      state = newState;
   }

   /**
    * Atomically sets synchronization state to the given updated value if the current state value
    * equals the expected value. This operation has memory semantics of a {@code volatile} read and
    * write.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful, false if the actual value was not equal to the expected
    *         value.
    */
   protected final boolean compareAndSetState(S expect, S update) {
      return stateUpdater.compareAndSet(this, expect, update);
   }

   // Queuing utilities

   /**
    * The number of nanoseconds for which it is faster to spin rather than to use timed park. A
    * rough estimate suffices to improve responsiveness with very short timeouts.
    */
   static final long spinForTimeoutThreshold = 1000L;

   /**
    * Inserts node into queue, initializing if necessary. See picture above.
    * 
    * @param node the node to insert
    * @return node's predecessor
    */
   private Node enq(final Node node) {
      while (true) {
         Node t = tail;
         if (t == null) {
            // Must initialize
            if (headUpdater.compareAndSet(this, null, new Node())) {
               tail = head;
            }
         } else {
            node.prev = t;
            if (tailUpdater.compareAndSet(this, t, node)) {
               t.next = node;
               return t;
            }
         }
      }
   }

   /**
    * Creates and enqueues node for current thread and given mode.
    *
    * @param mode {@link Node#EXCLUSIVE} or {@link Node#SHARED}
    * @return the new node
    */
   private Node addWaiter(Node mode) {
      Node node = new Node(Thread.currentThread(), mode);
      // Try the fast path of enq; backup to full enq on failure
      Node pred = tail;
      if (pred != null) {
         node.prev = pred;
         if (tailUpdater.compareAndSet(this, pred, node)) {
            pred.next = node;
            return node;
         }
      }
      enq(node);
      return node;
   }

   /**
    * Sets head of queue to be node, thus dequeuing. Called only by acquire methods. Also nulls out
    * unused fields for sake of GC and to suppress unnecessary signals and traversals.
    *
    * @param node the node
    */
   private void setHead(Node node) {
      head = node;
      node.thread = null;
      node.prev = null;
   }

   /**
    * Wakes up node's successor, if one exists.
    *
    * @param node the node
    */
   private void unparkSuccessor(Node node) {
      // If status is negative (i.e., possibly needing signal) try to clear in anticipation of
      // signalling. It is OK if this fails or if status is changed by waiting thread.
      int ws = node.waitStatus;
      if (ws < 0) {
         waitStatusUpdater.compareAndSet(node, ws, 0);
      }

      // Thread to unpark is held in successor, which is normally just the next node. But if
      // cancelled or apparently null, traverse backwards from tail to find the actual
      Node s = node.next;
      if (s == null || s.waitStatus > 0) {
         s = null;
         for (Node t = tail; t != null && t != node; t = t.prev) {
            if (t.waitStatus <= 0) {
               s = t;
            }
         }
      }
      if (s != null) {
         LockSupport.unpark(s.thread);
      }
   }

   /**
    * Release action for shared mode -- signals successor and ensures propagation. (Note: For
    * exclusive mode, release just amounts to calling unparkSuccessor of head if it needs signal.)
    */
   private void doReleaseShared() {
      // Ensure that a release propagates, even if there are other in-progress acquires/releases.
      // This proceeds in the usual way of trying to unparkSuccessor of head if it needs signal.
      // But if it does not, status is set to PROPAGATE to ensure that upon release, propagation
      // continues. Additionally, we must loop in case a new node is added while we are doing this.
      // Also, unlike other uses of unparkSuccessor, we need to know if CAS to reset status fails,
      // if so rechecking.
      while (true) {
         Node h = head;
         if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
               if (!waitStatusUpdater.compareAndSet(h, Node.SIGNAL, 0)) {
                  // loop to recheck cases
                  continue;
               }
               unparkSuccessor(h);
            } else if (ws == 0 && !waitStatusUpdater.compareAndSet(h, 0, Node.PROPAGATE)) {
               // loop on failed CAS
               continue;
            }
         }
         if (h == head) {
            // loop if head changed
            break;
         }
      }
   }

   /**
    * Sets head of queue, and checks if successor may be waiting in shared mode, if so propagating
    * if either propagate > 0 or PROPAGATE status was set.
    *
    * @param node the node
    * @param propagate the return value from a tryAcquireShared
    */
   private void setHeadAndPropagate(Node node, int propagate) {
      Node h = head; // Record old head for check below
      setHead(node);
      /* Try to signal next queued node if: Propagation was indicated by caller, or was recorded (as
       * h.waitStatus either before or after setHead) by a previous operation (note: this uses
       * sign-check of waitStatus because PROPAGATE status may transition to SIGNAL.) and The next
       * node is waiting in shared mode, or we don't know, because it appears null
       * 
       * The conservatism in both of these checks may cause unnecessary wake-ups, but only when
       * there are multiple racing acquires/releases, so most need signals now or soon anyway. */
      if (propagate > 0 || h == null || h.waitStatus < 0 || (h = head) == null || h.waitStatus < 0) {
         Node s = node.next;
         if (s == null || s.isShared()) doReleaseShared();
      }
   }

   // Utilities for various versions of acquire

   /**
    * Cancels an ongoing attempt to acquire.
    *
    * @param node the node
    */
   private void cancelAcquire(Node node) {
      // Ignore if node doesn't exist
      if (node == null) return;

      node.thread = null;

      // Skip cancelled predecessors
      Node pred = node.prev;
      while (pred.waitStatus > 0)
         node.prev = pred = pred.prev;

      // predNext is the apparent node to unsplice. CASes below will
      // fail if not, in which case, we lost race vs another cancel
      // or signal, so no further action is necessary.
      Node predNext = pred.next;

      // Can use unconditional write instead of CAS here.
      // After this atomic step, other Nodes can skip past us.
      // Before, we are free of interference from other threads.
      node.waitStatus = Node.CANCELLED;

      // If we are the tail, remove ourselves.
      if (node == tail && tailUpdater.compareAndSet(this, node, pred)) {
         nextUpdater.compareAndSet(pred, predNext, null);
      } else {
         // If successor needs signal, try to set pred's next-link
         // so it will get one. Otherwise wake it up to propagate.
         int ws;
         if (pred != head
               && ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && waitStatusUpdater
                     .compareAndSet(pred, ws, Node.SIGNAL))) && pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0)
               nextUpdater.compareAndSet(pred, predNext, next);
         } else {
            unparkSuccessor(node);
         }

         node.next = node; // help GC
      }
   }

   /**
    * Checks and updates status for a node that failed to acquire. Returns true if thread should
    * block. This is the main signal control in all acquire loops. Requires that pred == node.prev.
    *
    * @param pred node's predecessor holding status
    * @param node the node
    * @return {@code true} if thread should block
    */
   private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
      int ws = pred.waitStatus;
      if (ws == Node.SIGNAL)
      /* This node has already set status asking a release to signal it, so it can safely park. */
      return true;
      if (ws > 0) {
         /* Predecessor was cancelled. Skip over predecessors and indicate retry. */
         do {
            node.prev = pred = pred.prev;
         } while (pred.waitStatus > 0);
         pred.next = node;
      } else {
         /* waitStatus must be 0 or PROPAGATE. Indicate that we need a signal, but don't park yet.
          * Caller will need to retry to make sure it cannot acquire before parking. */
         waitStatusUpdater.compareAndSet(pred, ws, Node.SIGNAL);
      }
      return false;
   }

   /**
    * Convenience method to interrupt current thread.
    */
   static void selfInterrupt() {
      Thread.currentThread().interrupt();
   }

   /**
    * Convenience method to park and then check if interrupted
    *
    * @return {@code true} if interrupted
    */
   private final boolean parkAndCheckInterrupt() {
      LockSupport.park(this);
      return Thread.interrupted();
   }

   /* Various flavors of acquire, varying in exclusive/shared and control modes. Each is mostly the
    * same, but annoyingly different. Only a little bit of factoring is possible due to interactions
    * of exception mechanics (including ensuring that we cancel if tryAcquire throws exception) and
    * other control, at least not without hurting performance too much. */

   /**
    * Acquires in exclusive uninterruptible mode for thread already in queue. Used by condition wait
    * methods as well as acquire.
    *
    * @param node the node
    * @param arg the acquire argument
    * @return {@code true} if interrupted while waiting
    */
   final boolean acquireQueued(final Node node, R arg) {
      boolean failed = true;
      try {
         boolean interrupted = false;
         for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
               setHead(node);
               p.next = null; // help GC
               failed = false;
               return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
               interrupted = true;
         }
      } finally {
         if (failed) cancelAcquire(node);
      }
   }

   /**
    * Acquires in exclusive interruptible mode.
    * 
    * @param arg the acquire argument
    */
   private void doAcquireInterruptibly(R arg) throws InterruptedException {
      final Node node = addWaiter(Node.EXCLUSIVE);
      boolean failed = true;
      try {
         for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
               setHead(node);
               p.next = null; // help GC
               failed = false;
               return;
            }
            if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
               throw new InterruptedException();
         }
      } finally {
         if (failed) cancelAcquire(node);
      }
   }

   /**
    * Acquires in exclusive timed mode.
    *
    * @param arg the acquire argument
    * @param nanosTimeout max wait time
    * @return {@code true} if acquired
    */
   private boolean doAcquireNanos(R arg, long nanosTimeout) throws InterruptedException {
      if (nanosTimeout <= 0L) return false;
      final long deadline = System.nanoTime() + nanosTimeout;
      final Node node = addWaiter(Node.EXCLUSIVE);
      boolean failed = true;
      try {
         for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
               setHead(node);
               p.next = null; // help GC
               failed = false;
               return true;
            }
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) return false;
            if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
               LockSupport.parkNanos(this, nanosTimeout);
            if (Thread.interrupted()) throw new InterruptedException();
         }
      } finally {
         if (failed) cancelAcquire(node);
      }
   }

   /**
    * Acquires in shared uninterruptible mode.
    * 
    * @param arg the acquire argument
    */
   private void doAcquireShared(R arg) {
      final Node node = addWaiter(Node.SHARED);
      boolean failed = true;
      try {
         boolean interrupted = false;
         for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
               int r = tryAcquireShared(arg);
               if (r >= 0) {
                  setHeadAndPropagate(node, r);
                  p.next = null; // help GC
                  if (interrupted) selfInterrupt();
                  failed = false;
                  return;
               }
            }
            if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
               interrupted = true;
         }
      } finally {
         if (failed) cancelAcquire(node);
      }
   }

   /**
    * Acquires in shared interruptible mode.
    * 
    * @param arg the acquire argument
    */
   private void doAcquireSharedInterruptibly(R arg) throws InterruptedException {
      final Node node = addWaiter(Node.SHARED);
      boolean failed = true;
      try {
         for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
               int r = tryAcquireShared(arg);
               if (r >= 0) {
                  setHeadAndPropagate(node, r);
                  p.next = null; // help GC
                  failed = false;
                  return;
               }
            }
            if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
               throw new InterruptedException();
         }
      } finally {
         if (failed) cancelAcquire(node);
      }
   }

   /**
    * Acquires in shared timed mode.
    *
    * @param arg the acquire argument
    * @param nanosTimeout max wait time
    * @return {@code true} if acquired
    */
   private boolean doAcquireSharedNanos(R arg, long nanosTimeout) throws InterruptedException {
      if (nanosTimeout <= 0L) return false;
      final long deadline = System.nanoTime() + nanosTimeout;
      final Node node = addWaiter(Node.SHARED);
      boolean failed = true;
      try {
         for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
               int r = tryAcquireShared(arg);
               if (r >= 0) {
                  setHeadAndPropagate(node, r);
                  p.next = null; // help GC
                  failed = false;
                  return true;
               }
            }
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) return false;
            if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
               LockSupport.parkNanos(this, nanosTimeout);
            if (Thread.interrupted()) throw new InterruptedException();
         }
      } finally {
         if (failed) cancelAcquire(node);
      }
   }

   // Main exported methods

   /**
    * Attempts to acquire in exclusive mode. This method should query if the state of the object
    * permits it to be acquired in the exclusive mode, and if so to acquire it.
    *
    * <p>
    * This method is always invoked by the thread performing acquire. If this method reports
    * failure, the acquire method may queue the thread, if it is not already queued, until it is
    * signalled by a release from some other thread. This can be used to implement method
    * {@link Lock#tryLock()}.
    *
    * <p>
    * The default implementation throws {@link UnsupportedOperationException}.
    *
    * @param arg the acquire argument. This value is always the one passed to an acquire method, or
    *        is the value saved on entry to a condition wait. The value is otherwise uninterpreted
    *        and can represent anything you like.
    * @return {@code true} if successful. Upon success, this object has been acquired.
    * @throws IllegalMonitorStateException if acquiring would place this synchronizer in an illegal
    *         state. This exception must be thrown in a consistent fashion for synchronization to
    *         work correctly.
    * @throws UnsupportedOperationException if exclusive mode is not supported
    */
   protected boolean tryAcquire(@SuppressWarnings("unused") R arg) {
      throw new UnsupportedOperationException();
   }

   /**
    * Attempts to set the state to reflect a release in exclusive mode.
    *
    * <p>
    * This method is always invoked by the thread performing release.
    *
    * <p>
    * The default implementation throws {@link UnsupportedOperationException}.
    *
    * @param arg the release argument. This value is always the one passed to a release method, or
    *        the current state value upon entry to a condition wait. The value is otherwise
    *        uninterpreted and can represent anything you like.
    * @return {@code true} if this object is now in a fully released state, so that any waiting
    *         threads may attempt to acquire; and {@code false} otherwise.
    * @throws IllegalMonitorStateException if releasing would place this synchronizer in an illegal
    *         state. This exception must be thrown in a consistent fashion for synchronization to
    *         work correctly.
    * @throws UnsupportedOperationException if exclusive mode is not supported
    */
   protected boolean tryRelease(@SuppressWarnings("unused") R arg) {
      throw new UnsupportedOperationException();
   }

   /**
    * Attempts to acquire in shared mode. This method should query if the state of the object
    * permits it to be acquired in the shared mode, and if so to acquire it.
    *
    * <p>
    * This method is always invoked by the thread performing acquire. If this method reports
    * failure, the acquire method may queue the thread, if it is not already queued, until it is
    * signalled by a release from some other thread.
    *
    * <p>
    * The default implementation throws {@link UnsupportedOperationException}.
    *
    * @param arg the acquire argument. This value is always the one passed to an acquire method, or
    *        is the value saved on entry to a condition wait. The value is otherwise uninterpreted
    *        and can represent anything you like.
    * @return a negative value on failure; zero if acquisition in shared mode succeeded but no
    *         subsequent shared-mode acquire can succeed; and a positive value if acquisition in
    *         shared mode succeeded and subsequent shared-mode acquires might also succeed, in which
    *         case a subsequent waiting thread must check availability. (Support for three different
    *         return values enables this method to be used in contexts where acquires only sometimes
    *         act exclusively.) Upon success, this object has been acquired.
    * @throws IllegalMonitorStateException if acquiring would place this synchronizer in an illegal
    *         state. This exception must be thrown in a consistent fashion for synchronization to
    *         work correctly.
    * @throws UnsupportedOperationException if shared mode is not supported
    */
   protected int tryAcquireShared(@SuppressWarnings("unused") R arg) {
      throw new UnsupportedOperationException();
   }

   /**
    * Attempts to set the state to reflect a release in shared mode.
    *
    * <p>
    * This method is always invoked by the thread performing release.
    *
    * <p>
    * The default implementation throws {@link UnsupportedOperationException}.
    *
    * @param arg the release argument. This value is always the one passed to a release method, or
    *        the current state value upon entry to a condition wait. The value is otherwise
    *        uninterpreted and can represent anything you like.
    * @return {@code true} if this release of shared mode may permit a waiting acquire (shared or
    *         exclusive) to succeed; and {@code false} otherwise
    * @throws IllegalMonitorStateException if releasing would place this synchronizer in an illegal
    *         state. This exception must be thrown in a consistent fashion for synchronization to
    *         work correctly.
    * @throws UnsupportedOperationException if shared mode is not supported
    */
   protected boolean tryReleaseShared(@SuppressWarnings("unused") R arg) {
      throw new UnsupportedOperationException();
   }

   /**
    * Acquires in exclusive mode, ignoring interrupts. Implemented by invoking at least once
    * {@link #tryAcquire}, returning on success. Otherwise the thread is queued, possibly repeatedly
    * blocking and unblocking, invoking {@link #tryAcquire} until success. This method can be used
    * to implement method {@link Lock#lock}.
    *
    * @param arg the acquire argument. This value is conveyed to {@link #tryAcquire} but is
    *        otherwise uninterpreted and can represent anything you like.
    */
   public final void acquire(R arg) {
      if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) selfInterrupt();
   }

   /**
    * Acquires in exclusive mode, aborting if interrupted. Implemented by first checking interrupt
    * status, then invoking at least once {@link #tryAcquire}, returning on success. Otherwise the
    * thread is queued, possibly repeatedly blocking and unblocking, invoking {@link #tryAcquire}
    * until success or the thread is interrupted. This method can be used to implement method
    * {@link Lock#lockInterruptibly}.
    *
    * @param arg the acquire argument. This value is conveyed to {@link #tryAcquire} but is
    *        otherwise uninterpreted and can represent anything you like.
    * @throws InterruptedException if the current thread is interrupted
    */
   public final void acquireInterruptibly(R arg) throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      if (!tryAcquire(arg)) doAcquireInterruptibly(arg);
   }

   /**
    * Attempts to acquire in exclusive mode, aborting if interrupted, and failing if the given
    * timeout elapses. Implemented by first checking interrupt status, then invoking at least once
    * {@link #tryAcquire}, returning on success. Otherwise, the thread is queued, possibly
    * repeatedly blocking and unblocking, invoking {@link #tryAcquire} until success or the thread
    * is interrupted or the timeout elapses. This method can be used to implement method
    * {@link Lock#tryLock(long, TimeUnit)}.
    *
    * @param arg the acquire argument. This value is conveyed to {@link #tryAcquire} but is
    *        otherwise uninterpreted and can represent anything you like.
    * @param nanosTimeout the maximum number of nanoseconds to wait
    * @return {@code true} if acquired; {@code false} if timed out
    * @throws InterruptedException if the current thread is interrupted
    */
   public final boolean tryAcquireNanos(R arg, long nanosTimeout) throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
   }

   /**
    * Releases in exclusive mode. Implemented by unblocking one or more threads if
    * {@link #tryRelease} returns true. This method can be used to implement method
    * {@link Lock#unlock}.
    *
    * @param arg the release argument. This value is conveyed to {@link #tryRelease} but is
    *        otherwise uninterpreted and can represent anything you like.
    * @return the value returned from {@link #tryRelease}
    */
   public final boolean release(R arg) {
      if (tryRelease(arg)) {
         Node h = head;
         if (h != null && h.waitStatus != 0) unparkSuccessor(h);
         return true;
      }
      return false;
   }

   /**
    * Acquires in shared mode, ignoring interrupts. Implemented by first invoking at least once
    * {@link #tryAcquireShared}, returning on success. Otherwise the thread is queued, possibly
    * repeatedly blocking and unblocking, invoking {@link #tryAcquireShared} until success.
    *
    * @param arg the acquire argument. This value is conveyed to {@link #tryAcquireShared} but is
    *        otherwise uninterpreted and can represent anything you like.
    */
   public final void acquireShared(R arg) {
      if (tryAcquireShared(arg) < 0) doAcquireShared(arg);
   }

   /**
    * Acquires in shared mode, aborting if interrupted. Implemented by first checking interrupt
    * status, then invoking at least once {@link #tryAcquireShared}, returning on success. Otherwise
    * the thread is queued, possibly repeatedly blocking and unblocking, invoking
    * {@link #tryAcquireShared} until success or the thread is interrupted.
    * 
    * @param arg the acquire argument. This value is conveyed to {@link #tryAcquireShared} but is
    *        otherwise uninterpreted and can represent anything you like.
    * @throws InterruptedException if the current thread is interrupted
    */
   public final void acquireSharedInterruptibly(R arg) throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      if (tryAcquireShared(arg) < 0) doAcquireSharedInterruptibly(arg);
   }

   /**
    * Attempts to acquire in shared mode, aborting if interrupted, and failing if the given timeout
    * elapses. Implemented by first checking interrupt status, then invoking at least once
    * {@link #tryAcquireShared}, returning on success. Otherwise, the thread is queued, possibly
    * repeatedly blocking and unblocking, invoking {@link #tryAcquireShared} until success or the
    * thread is interrupted or the timeout elapses.
    *
    * @param arg the acquire argument. This value is conveyed to {@link #tryAcquireShared} but is
    *        otherwise uninterpreted and can represent anything you like.
    * @param nanosTimeout the maximum number of nanoseconds to wait
    * @return {@code true} if acquired; {@code false} if timed out
    * @throws InterruptedException if the current thread is interrupted
    */
   public final boolean tryAcquireSharedNanos(R arg, long nanosTimeout) throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
   }

   /**
    * Releases in shared mode. Implemented by unblocking one or more threads if
    * {@link #tryReleaseShared} returns true.
    *
    * @param arg the release argument. This value is conveyed to {@link #tryReleaseShared} but is
    *        otherwise uninterpreted and can represent anything you like.
    * @return the value returned from {@link #tryReleaseShared}
    */
   public final boolean releaseShared(R arg) {
      if (tryReleaseShared(arg)) {
         doReleaseShared();
         return true;
      }
      return false;
   }

   // Queue inspection methods

   /**
    * Queries whether any threads are waiting to acquire. Note that because cancellations due to
    * interrupts and timeouts may occur at any time, a {@code true} return does not guarantee that
    * any other thread will ever acquire.
    *
    * <p>
    * In this implementation, this operation returns in constant time.
    *
    * @return {@code true} if there may be other threads waiting to acquire
    */
   public final boolean hasQueuedThreads() {
      return head != tail;
   }

   /**
    * Queries whether any threads have ever contended to acquire this synchronizer; that is if an
    * acquire method has ever blocked.
    *
    * <p>
    * In this implementation, this operation returns in constant time.
    *
    * @return {@code true} if there has ever been contention
    */
   public final boolean hasContended() {
      return head != null;
   }

   /**
    * Returns the first (longest-waiting) thread in the queue, or {@code null} if no threads are
    * currently queued.
    *
    * <p>
    * In this implementation, this operation normally returns in constant time, but may iterate upon
    * contention if other threads are concurrently modifying the queue.
    *
    * @return the first (longest-waiting) thread in the queue, or {@code null} if no threads are
    *         currently queued
    */
   public final Thread getFirstQueuedThread() {
      // handle only fast path, else relay
      return (head == tail) ? null : fullGetFirstQueuedThread();
   }

   /**
    * Version of getFirstQueuedThread called when fastpath fails
    */
   private Thread fullGetFirstQueuedThread() {
      /* The first node is normally head.next. Try to get its thread field, ensuring consistent
       * reads: If thread field is nulled out or s.prev is no longer head, then some other thread(s)
       * concurrently performed setHead in between some of our reads. We try this twice before
       * resorting to traversal. */
      Node h, s;
      Thread st;
      if (((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null)
            || ((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null))
         return st;

      /* Head's next field might not have been set yet, or may have been unset after setHead. So we
       * must check to see if tail is actually first node. If not, we continue on, safely traversing
       * from tail back to head to find first, guaranteeing termination. */

      Node t = tail;
      Thread firstThread = null;
      while (t != null && t != head) {
         Thread tt = t.thread;
         if (tt != null) firstThread = tt;
         t = t.prev;
      }
      return firstThread;
   }

   /**
    * Returns true if the given thread is currently queued.
    *
    * <p>
    * This implementation traverses the queue to determine presence of the given thread.
    *
    * @param thread the thread
    * @return {@code true} if the given thread is on the queue
    * @throws NullPointerException if the thread is null
    */
   public final boolean isQueued(Thread thread) {
      if (thread == null) throw new NullPointerException();
      for (Node p = tail; p != null; p = p.prev)
         if (p.thread == thread) return true;
      return false;
   }

   /**
    * Returns {@code true} if the apparent first queued thread, if one exists, is waiting in
    * exclusive mode. If this method returns {@code true}, and the current thread is attempting to
    * acquire in shared mode (that is, this method is invoked from {@link #tryAcquireShared}) then
    * it is guaranteed that the current thread is not the first queued thread. Used only as a
    * heuristic in ReentrantReadWriteLock.
    */
   final boolean apparentlyFirstQueuedIsExclusive() {
      Node h, s;
      return (h = head) != null && (s = h.next) != null && !s.isShared() && s.thread != null;
   }

   /**
    * Queries whether any threads have been waiting to acquire longer than the current thread.
    *
    * <p>
    * An invocation of this method is equivalent to (but may be more efficient than):
    * 
    * <pre>
    * {@code
    * getFirstQueuedThread() != Thread.currentThread() &&
    * hasQueuedThreads()}
    * </pre>
    *
    * <p>
    * Note that because cancellations due to interrupts and timeouts may occur at any time, a
    * {@code true} return does not guarantee that some other thread will acquire before the current
    * thread. Likewise, it is possible for another thread to win a race to enqueue after this method
    * has returned {@code false}, due to the queue being empty.
    *
    * <p>
    * This method is designed to be used by a fair synchronizer to avoid <a
    * href="AbstractQueuedSynchronizer#barging">barging</a>. Such a synchronizer's
    * {@link #tryAcquire} method should return {@code false}, and its {@link #tryAcquireShared}
    * method should return a negative value, if this method returns {@code true} (unless this is a
    * reentrant acquire). For example, the {@code tryAcquire} method for a fair, reentrant,
    * exclusive mode synchronizer might look like this:
    *
    * <pre>
    * {@code protected boolean tryAcquire(int arg) if (isHeldExclusively()) { // A reentrant
    * acquire; increment hold count return true; } else if (hasQueuedPredecessors()) { return false;
    * } else { // try to acquire normally } }}
    * </pre>
    *
    * @return {@code true} if there is a queued thread preceding the current thread, and
    *         {@code false} if the current thread is at the head of the queue or the queue is empty
    * @since 1.7
    */
   public final boolean hasQueuedPredecessors() {
      // The correctness of this depends on head being initialized
      // before tail and on head.next being accurate if the current
      // thread is first in queue.
      Node t = tail; // Read fields in reverse initialization order
      Node h = head;
      Node s;
      return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
   }

   // Instrumentation and monitoring methods

   /**
    * Returns an estimate of the number of threads waiting to acquire. The value is only an estimate
    * because the number of threads may change dynamically while this method traverses internal data
    * structures. This method is designed for use in monitoring system state, not for
    * synchronization control.
    *
    * @return the estimated number of threads waiting to acquire
    */
   public final int getQueueLength() {
      int n = 0;
      for (Node p = tail; p != null; p = p.prev) {
         if (p.thread != null) ++n;
      }
      return n;
   }

   /**
    * Returns a collection containing threads that may be waiting to acquire. Because the actual set
    * of threads may change dynamically while constructing this result, the returned collection is
    * only a best-effort estimate. The elements of the returned collection are in no particular
    * order. This method is designed to facilitate construction of subclasses that provide more
    * extensive monitoring facilities.
    *
    * @return the collection of threads
    */
   public final Collection<Thread> getQueuedThreads() {
      ArrayList<Thread> list = new ArrayList<Thread>();
      for (Node p = tail; p != null; p = p.prev) {
         Thread t = p.thread;
         if (t != null) list.add(t);
      }
      return list;
   }

   /**
    * Returns a collection containing threads that may be waiting to acquire in exclusive mode. This
    * has the same properties as {@link #getQueuedThreads} except that it only returns those threads
    * waiting due to an exclusive acquire.
    *
    * @return the collection of threads
    */
   public final Collection<Thread> getExclusiveQueuedThreads() {
      ArrayList<Thread> list = new ArrayList<Thread>();
      for (Node p = tail; p != null; p = p.prev) {
         if (!p.isShared()) {
            Thread t = p.thread;
            if (t != null) list.add(t);
         }
      }
      return list;
   }

   /**
    * Returns a collection containing threads that may be waiting to acquire in shared mode. This
    * has the same properties as {@link #getQueuedThreads} except that it only returns those threads
    * waiting due to a shared acquire.
    *
    * @return the collection of threads
    */
   public final Collection<Thread> getSharedQueuedThreads() {
      ArrayList<Thread> list = new ArrayList<Thread>();
      for (Node p = tail; p != null; p = p.prev) {
         if (p.isShared()) {
            Thread t = p.thread;
            if (t != null) list.add(t);
         }
      }
      return list;
   }

   /**
    * Returns a string identifying this synchronizer, as well as its state. The state, in brackets,
    * includes the String {@code "State ="} followed by the current value of {@link #getState}, and
    * either {@code "nonempty"} or {@code "empty"} depending on whether the queue is empty.
    *
    * @return a string identifying this synchronizer, as well as its state
    */
   @Override
   public String toString() {
      S s = getState();
      String q = hasQueuedThreads() ? "non" : "";
      return super.toString() + "[State = " + s + ", " + q + "empty queue]";
   }
}
