package com.apriori.concurrent;

import com.apriori.util.Cloner;
import com.apriori.util.Cloners;

import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A concurrency mechanism allowing concurrent readers and an exclusive writer. This is based on the
 * technique called <a href="http://concurrencyfreaks.blogspot.com/2013/11/double-instance-locking.html">
 * Double Instance Locking</a>. The linked article describes the use of two read-write locks, one
 * for each instance, plus another mutex to serialize writes. This implementation uses a custom
 * synchronizer that combines all three into one.
 * 
 * <p>Readers never block, even when a write operation is in progress. Essentially, two instances of
 * the data are kept (hence the name). Reads all safely happen against one instance while writes
 * happen to the other. The two versions are atomically swapped to make changes visible to readers.
 * Writers must make the same change to both halves: once to the instance not being read, then again
 * to the other side after the instances are swapped.
 * 
 * <p>All write operations must be deterministic, such that applying it to two equal objects
 * produces two equal results. Otherwise, when writes are applied to both halves, the state may be
 * corrupted and the two instances could get out of sync.
 * 
 * <p>It is important that read and write operations neither return references to the object they
 * read/write nor allow such a reference to otherwise "escape" (e.g. storing it in another object
 * that ends up publishing the value to other threads). This is because any thread that tries to
 * inspect or interact with that value will encounter concurrency issues as the object is not
 * thread-safe. The <em>only</em> thread-safe interaction is through this
 * {@link DoubleInstanceLock}. If an operation needs to provide access to the object, use
 * {@link #snapshot()} instead of the read or write methods. Note that write operations should never
 * be made to such a snapshot (actual copies are made lazily, to defer any cost of copying to the
 * next write operation and to allow multiple readers to share the same snapshot).
 * 
 * <p>The object must be cloneable so that stable snapshots of the object can be captured. (Also, it
 * is cloned initially to generate the two instances from the one object.) It is <em>very
 * important</em> that no operations be performed directly against the object after the
 * {@link DoubleInstanceLock} is created. All subsequent operations must use read and write methods
 * on the {@link DoubleInstanceLock} to access the data. Failing to comply can result in corruption
 * of the data and undefined behavior.
 *
 * @param <T> the type of element that is being accessed by readers and writers
 *
 * @see <a href="http://concurrencyfreaks.blogspot.com/2013/11/double-instance-locking.html">
 *       Double Instance Locking</a>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class DoubleInstanceLock<T> {
   
   /**
    * Constructs a new double instance lock around the given object. The object's {@link #clone()}
    * method is used to create additional instances.
    *
    * @param t an object
    * @return a double instance lock around the given object
    */
   public static <T extends Cloneable> DoubleInstanceLock<T> newLock(T t) {
      return new DoubleInstanceLock<>(t, Cloners.forCloneable());
   }

   /**
    * Constructs a new double instance lock around the given object that uses the given cloner to
    * create additional instances.
    * 
    * <p><strong>Note</strong>: If the supplied cloner does not properly adhere to the contract,
    * (e.g. does not produce a different instance that is otherwise identical to the input object)
    * then the two instances of data used by the lock can get out of sync. This means data
    * corruption and that subsequent operations will be undefined.
    *
    * @param t an object
    * @param cloner a cloner
    * @return a double instance lock around the given object
    */
   public static <T> DoubleInstanceLock<T> newLock(T t, Cloner<T> cloner) {
      return new DoubleInstanceLock<>(t, cloner);
   }
   
   /*
    * Identifiers below use the naming conventions of "left" and "right" to distinguish between the
    * two instances.
    */

   /**
    * The left side. This is returned from synchronization methods when indicating the left side of
    * this structure. This is also an array index into {@link #values} for accessing the value of
    * the left side.
    */
   private static final int LEFT = 0;

   /**
    * The right side. This is returned from synchronization methods when indicating the right side
    * of this structure. This is also an array index into {@link #values} for accessing the value of
    * the right side.
    */
   private static final int RIGHT = 1;
   
   private final Sync sync = new Sync();
   private final Cloner<T> cloner;
   private final T values[];
   
   private DoubleInstanceLock(T value, Cloner<T> cloner) {
      this.cloner = cloner;
      @SuppressWarnings("unchecked")
      T a[] = (T[]) new Object[2];
      values = a;
      values[0] = value;
      values[1] = cloner.clone(value);
   }
   
   /**
    * Performs a read operation that does not produce a result value. To maximize write throughput,
    * read operations should be very fast. If a read operation needs to do something expensive,
    * consider performing the operation on a {@linkplain #snapshot() snapshot} instead of using this
    * method.
    * 
    * <p><strong>Note</strong>: if the action performs any mutations on the object passed to it,
    * this data structure will become corrupted and further operations will produce undefined
    * results.
    *
    * @param action the read operation to perform
    */
   public void readWith(Consumer<? super T> action) {
      read((T t) -> { action.accept(t); return null; });
   }

   /**
    * Performs a read operation that produces a result value. To maximize write throughput, read
    * operations should be very fast. If a read operation needs to do something expensive, consider
    * performing the operation on a {@linkplain #snapshot() snapshot} instead of using this method.
    * 
    * <p><strong>Note</strong>: if the action performs any mutations on the object passed to it,
    * this data structure will become corrupted and further operations will produce undefined
    * results.
    *
    * @param action the read operation to perform
    */
   public <U> U read(Function<? super T, ? extends U> action) {
      int s = sync.addShared();
      try {
         return action.apply(values[s]);
      } finally {
         sync.releaseShared(s);
      }
   }

   /**
    * Performs a write operation that does not produce a value. Note that write operations are
    * <strong>not</strong> allowed to be reentrant. If the given action tries to invoke
    * {@link #write(Function)} or {@link #writeWith(Consumer)} then a {@link ReentranceException}
    * is thrown.
    *
    * <p><strong>Note</strong>: if the action is not deterministic, then applying the action could
    * cause divergence of the two sides of this structure, thus causing corruption and making the
    * results of subsequent operations undefined.
    * 
    * @param action the write operation to perform
    * @throws ReentranceException if the given action is reentrant
    */
   public void writeWith(Consumer<? super T> action) {
      write((T t) -> { action.accept(t); return null; });
   }
   
   /**
    * Performs a write operation that produces a result value. Note that write operations are
    * <strong>not</strong> allowed to be reentrant. If the given action tries to invoke
    * {@link #write(Function)} or {@link #writeWith(Consumer)} then a {@link ReentranceException}
    * is thrown.
    *
    * <p><strong>Note</strong>: if the action is not deterministic, then applying the action could
    * cause divergence of the two sides of this structure, thus causing corruption and making the
    * results of subsequent operations undefined.
    * 
    * @param action the write operation to perform
    * @throws ReentranceException if the given action is reentrant
    */
   public <U> U write(Function<? super T, ? extends U> action) {
      // only one writer at a time allowed 
      sync.acquire(-1);
      try {
         // first get side that readers aren't using
         int s2 = sync.leftOrRight();
         int s1 = s2 == LEFT ? RIGHT : LEFT;
         T val = values[s1];
         if (sync.resetSnapshot(s1)) {
            values[s1] = val = cloner.clone(values[s1]);
         }
         action.apply(val); // write the first side!
         
         // then flip which side readers are using
         int s = sync.flip();
         assert s == s2;
         sync.awaitDrain(s2); // make sure readers have drained
         val = values[s2];
         if (sync.resetSnapshot(s2)) {
            values[s2] = val = cloner.clone(values[s2]);
         }
         return action.apply(val); // write the second side!
      } finally {
         sync.release(-1);
      }
   }

   /**
    * Returns a snapshot of the data, like for performing expensive work that requires a strongly
    * consistent view.
    *
    * <p><strong>Note</strong>: the returned object <em>should not be modified in anyway</em>. For
    * efficiency, creation of the snapshot is deferred to when a writer needs to modify the object.
    * This allows multiple readers to share snapshots. Mutating the returned object will corrupt the
    * data for other readers and result in undefined behavior for subsequent operations. 
    *
    * @return a snapshot of the data that will not be modified by subsequent write operations
    */
   public T snapshot() {
      int s = sync.addShared();
      try {
         sync.snapshot(s);
         return values[s];
      } finally {
         sync.releaseShared(s);
      }
   }
   
   /**
    * The synchronizer used by a {@link DoubleInstanceLock}. Writers acquire it in exclusive mode
    * and can also use it wait for readers to drain from a given side. Readers use it to mark their
    * presence and to signal writers that are waiting for reads to drain.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync extends AbstractQueuedLongSynchronizer {
      private static final long serialVersionUID = 6710412710619401566L;
      
      private static final long WRITE_LOCKED =      0x8000000000000000L;
      private static final long LEFT_OR_RIGHT =     0x0000000080000000L;
      private static final long LEFT_OR_RIGHT_SHIFT = 31;
      private static final long LEFT_SNAPSHOT =     0x4000000000000000L;
      private static final long LEFT_READER_MASK =  0x3fffffff00000000L;
      private static final long LEFT_READER_INC  =  0x0000000100000000L;
      private static final long LEFT_READER_SHIFT = 32;
      private static final long RIGHT_SNAPSHOT =    0x0000000040000000L;
      private static final long RIGHT_READER_MASK = 0x000000003fffffffL;
      private static final long RIGHT_READER_INC  = 0x0000000000000001L;
      
      /**
       * A thread that is waiting for readers to drain from the left side. This will be {@code null}
       * if no such thread is waiting. Only threads that have write-locked this synchronizer need to
       * wait for readers to drain, and the writers are exclusive. So we just need a single field to
       * store the thread, not a queue.
       */
      private volatile Thread awaitingDrainLeft;

      /**
       * A thread that is waiting for readers to drain from the right side. This will be
       * {@code null} if no such thread is waiting. Only threads that have write-locked this
       * synchronizer need to wait for readers to drain, and the writers are exclusive. So we just
       * need a single field to store the thread, not a queue. 
       */
      private volatile Thread awaitingDrainRight;
      
      Sync() {
      }
      
      /**
       * Acquire the synchronizer in "write" mode.
       *
       * @param i unused
       * @return true if acquisition succeeded
       */
      @Override
      protected boolean tryAcquire(long i) {
         // exclusive lock, for writers
         if (getExclusiveOwnerThread() == Thread.currentThread()) {
            // re-entrance not allowed!
            throw new ReentranceException();
         }
         while (true) {
            long s = getState();
            if ((s & WRITE_LOCKED) != 0) {
               return false;
            }
            if (compareAndSetState(s, s | WRITE_LOCKED)) {
               setExclusiveOwnerThread(Thread.currentThread());
               return true;
            }
         }
      }
      
      /**
       * Waits for readers to drain from the given side. This will block if there are in-process
       * readers of the given side. On return, the given side has no readers. This is generally
       * only used after a {@link #flip()} to make sure no new readers will try to use the given
       * side.
       *
       * @param leftOrRight the side which should have no readers on return
       */
      void awaitDrain(int leftOrRight) {
         if (getExclusiveOwnerThread() != Thread.currentThread()) {
            // only exclusive writer can wait for threads to drain
            throw new IllegalMonitorStateException();
         }
         // eagerly set the thread field, so exiting readers can unpark us
         long mask;
         if (leftOrRight == LEFT) {
            awaitingDrainLeft = Thread.currentThread();
            mask = LEFT_READER_MASK;
         } else {
            awaitingDrainRight = Thread.currentThread();
            mask = RIGHT_READER_MASK;
         }            
         try {
            // wait for the reader count to drain to zero
            while (true) {
               if ((getState() & mask) == 0) {
                  return;
               } else {
                  LockSupport.park(this);
               }
            }
         } finally {
            if (leftOrRight == LEFT) {
               awaitingDrainLeft = null;
            } else {
               awaitingDrainRight = null;
            }
         }
      }
      
      /**
       * Releases the synchronizer when held in "write" mode.
       *
       * @param i unused
       * @return always true, indicating the lock is available for another writer to acquire
       */
      @Override
      protected boolean tryRelease(long i) {
         if (getExclusiveOwnerThread() != Thread.currentThread()) {
            throw new IllegalMonitorStateException();
         }
         while (true) {
            long s = getState();
            assert (s & WRITE_LOCKED) != 0;
            if (compareAndSetState(s, s & ~WRITE_LOCKED)) {
               setExclusiveOwnerThread(null);
               return true;
            }
         }
      }
      
      /**
       * Returns which side, left or right, currently used by readers. Only a writer, with the write
       * lock, should use this as it is otherwise inherently racy. 
       *
       * @return the side that is currently being used by readers
       */
      int leftOrRight() {
         return leftOrRight(getState());
      }

      /**
       * Returns which side, left or right, used by readers according to the given state.
       *
       * @param s state value
       * @return the side that is being used by readers, as encoded in the state value
       */
      private int leftOrRight(long s) { 
         return (int) ((s & LEFT_OR_RIGHT) >> LEFT_OR_RIGHT_SHIFT) & 1;
      }

      /**
       * Swaps the side that readers are currently using. If readers are reading from left at the
       * onset, they will be reading from right when the method returns, and vice versa. Only a
       * writer, with the write lock, can use this.
       *
       * @return the side that readers were using before the flip
       */
      int flip() {
         if (getExclusiveOwnerThread() != Thread.currentThread()) {
            throw new IllegalMonitorStateException();
         }
         while (true) {
            long s = getState();
            long n;
            int leftOrRight = leftOrRight(s);
            if (leftOrRight == LEFT) {
               n = s | LEFT_OR_RIGHT;
            } else {
               n = s & ~LEFT_OR_RIGHT;
            }
            if (compareAndSetState(s, n)) {
               return leftOrRight;
            }
         }
      }
      
      /**
       * Sets a flag that a snapshot is needed for the given side. The next write operation should
       * copy the value before modifying because it has been returned to readers, for use as a
       * stable snapshot.
       *
       * @param leftOrRight the side for which a snapshot must be created before applying the next
       *       write
       */
      void snapshot(int leftOrRight) {
         while (true) {
            long s = getState();
            long n = s;
            if (leftOrRight == LEFT) {
               if ((s & LEFT_SNAPSHOT) == 0) {
                  n |= LEFT_SNAPSHOT;
               } else {
                  return;
               }
            } else {
               if ((s & RIGHT_SNAPSHOT) == 0) {
                  n |= RIGHT_SNAPSHOT;
               } else {
                  return;
               }
            }
            if (compareAndSetState(s, n)) {
               return;
            }
         }
      }

      /**
       * Resets the flag that indicates a snapshot is needed. Writers call this to atomically clear
       * the flag as well as inspect the flag's value so they know whether or not making a snapshot
       * is necessary.
       *
       * @param leftOrRight the side whose {@linkplain #snapshot(int) snapshot} flag is checked
       * @return true if the flag was previously set and now cleared; false if no action was
       *       necessary because the flag was unset
       */
      boolean resetSnapshot(int leftOrRight) {
         while (true) {
            long s = getState();
            long n = s;
            if (leftOrRight == LEFT) {
               if ((s & LEFT_SNAPSHOT) != 0) {
                  n &= ~LEFT_SNAPSHOT;
               } else {
                  return false;
               }
            } else {
               if ((s & RIGHT_SNAPSHOT) != 0) {
                  n &= ~RIGHT_SNAPSHOT;
               } else {
                  return false;
               }
            }
            if (compareAndSetState(s, n)) {
               return true;
            }
         }
      }
      
      /**
       * Adds a shared acquisition. The synchronizer's built-in operations for acquiring in shared
       * mode are not applicable since we <em>never</em> block readers. Readers must use this to
       * record the fact they are reading. They must then call {@link #releaseShared(int)}, passing
       * the value returned by this method, to record the fact they are done reading.
       *
       * @return the side that the reader should use (and subsequently release)
       */
      int addShared() {
         while (true) {
            long s = getState();
            int leftOrRight = leftOrRight(s);
            long m;
            long a;
            if (leftOrRight == LEFT) {
               m = LEFT_READER_MASK;
               a = LEFT_READER_INC;
            } else {
               m = RIGHT_READER_MASK;
               a = RIGHT_READER_INC;
            }
            // we can support > 1 billion readers, so we don't really need the overhead of a runtime
            // check, but an assertion makes sense
            assert (s & m) != m;
            if (compareAndSetState(s, s + a)) {
               return leftOrRight;
            }
         }
      }
      
      /**
       * Indicates that a reader is releasing its shared hold.
       *
       * @param i the side that the reader is releasing
       * @return true if a writer can subsequently acquire the side that was released (e.g. reads
       *       have acquiesced on that side)
       */
      @Override
      protected boolean tryReleaseShared(long i) {
         while (true) {
            long s = getState();
            long m;
            long a;
            if (i == LEFT) {
               m = LEFT_READER_MASK;
               a = LEFT_READER_INC;
            } else {
               m = RIGHT_READER_MASK;
               a = RIGHT_READER_INC;
            }
            if ((s & m) == 0) {
               throw new IllegalMonitorStateException();
            }
            long n = s - a;
            if (compareAndSetState(s, n)) {
               if ((n & m) == 0) {
                  // no more readers on this side; awake any threads waiting for it to drain
                  Thread th = i == LEFT ? awaitingDrainLeft : awaitingDrainRight;
                  if (th != null) {
                     LockSupport.unpark(th);
                  }
               }
               return false;
            }
         }
      }
      
      @Override
      public String toString() {
         return toString(getClass().getSimpleName() + " { ", getState(), " }");
      }
      
      /**
       * Constructs a string representation of this synchronizer's state.
       *
       * @param prefix a prefix for the string representation
       * @param s the state value to present in string form
       * @param suffix a suffix for the string representation
       * @return a string representation of the given state value with the given prefix and suffix
       */
      private String toString(String prefix, long s, String suffix) {
         StringBuilder sb = new StringBuilder();
         sb.append(prefix);
         sb.append("State = 0x");
         String st = Long.toHexString(s);
         for (int i = 0, len = 16 - st.length(); i < len; i++) {
            sb.append('0');
         }
         sb.append(st);
         sb.append(", ");
         sb.append("Write locked? ");
         sb.append((s & WRITE_LOCKED) != 0);
         sb.append(", ");
         sb.append("Snapshot left? ");
         sb.append((s & LEFT_SNAPSHOT) != 0);
         sb.append(", ");
         sb.append("Snapshot right? ");
         sb.append((s & RIGHT_SNAPSHOT) != 0);
         sb.append(", ");
         sb.append("Current side: ");
         long side = leftOrRight(s);
         if (side == LEFT) {
            sb.append("left");
         } else if (side == RIGHT) {
            sb.append("right");
         } else {  
            sb.append("???");
         }
         sb.append(", ");
         sb.append("Reading left: ");
         sb.append((s & LEFT_READER_MASK) >> LEFT_READER_SHIFT);
         sb.append(", ");
         sb.append("Reading right: ");
         sb.append(s & RIGHT_READER_MASK);
         sb.append(suffix);
         return sb.toString();
      }
   }
}
