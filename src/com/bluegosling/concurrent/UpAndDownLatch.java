package com.bluegosling.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A latch that allows counting up, not just down. Inspired by Go's {@code sync.WaitGroup}. Code
 * can both add and subtract references from the latch. Awaiting the latch will return when the
 * count of references reaches zero. Unlike a {@link CountDownLatch}, this can be re-used after it
 * reaches zero by simply adding one or more references. Subsequent attempts to await will block
 * until the count reaches zero again.
 * 
 * <p>The count of references cannot be negative. Operations that would cause this condition will
 * throw an {@link IllegalStateException}. Similarly, the count of references cannot exceed
 * {@link Integer#MAX_VALUE}, and operations that would cause this condition will also throw.
 * 
 * <p>This latch has qualities similar to a {@link Phaser}, but is much simpler. These latches
 * cannot be grouped into trees (for handling huge numbers of references), and they do not track
 * "phases" or "generations". Awaiting the latch just waits for it to become zero, essentially for
 * references to "quiesce". It is not possible to wait for a particular phase to end. For example,
 * if one thread queries the count, sees a positive number, and then awaits the latch, it could be
 * awaiting a subsequent phase. This can happen if, concurrently, the count reached zero and then
 * another reference was added, all in-between the other thread's observing the count and initiating
 * an await operation.
 * 
 * <p>Memory consistency effects: things that occur in a thread before a call to {@link #countUp()}
 * or {@link #countDown()} <em>happen before</em> any actions on a thread after a subsequent return
 * from a successful await operation. Similarly, things that happen in a thread before calls to
 * {@link #countDown()} that result in the count reaching zero <em>happen before</em> any actions
 * after a subsequent call to {@link #countUp()} that returns true (e.g. when the count becomes
 * non-zero). Vice versa is also true: things that happen in a thread prior to calls to
 * {@link #countUp()} <em>happen before</em> actions on another thread after a subsequent call to
 * {@link #countDown()} that returns true (e.g. when the count returns to zero). 
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class UpAndDownLatch implements Awaitable {
   private final Sync sync = new Sync();
   
   /**
    * Constructs a new latch that has zero references. Callers must use {@link #countUp()} to
    * add references.
    */
   public UpAndDownLatch() {
   }
   
   /**
    * Constructs a new latch that has the given number of initial references.
    * 
    * @throws IllegalArgumentException if the given count is negative
    */
   public UpAndDownLatch(int count) {
      if (count < 0) {
         throw new IllegalArgumentException("count cannot be negative");
      }
      sync.addRefs(count);
   }
   
   /**
    * Adds a single reference to the latch. The latch is closed while it has references and opens
    * (allowing {@linkplain #await() awaiting threads} to advance) once the count reaches zero.
    * 
    * @return true if this update caused the latch to close (e.g. the number of references was zero
    *           prior to adding this reference)
    * @throws IllegalStateException if adding a reference would cause the count of latches to
    *           overflow (e.g. exceed {@link Integer#MAX_VALUE})
    */
   public boolean countUp() {
      return countUp(1);
   }

   /**
    * Adds the given number of references to the latch. The latch is closed while it has references
    * and opens (allowing {@linkplain #await() awaiting threads} to advance) once the count reaches
    * zero.
    * 
    * <p>If a negative number is provided, this acts like {@link #countDown()}, removing references,
    * but its return value would always be false in this case.
    * 
    * @return true if this update caused the latch to close (e.g. the number of references
    *           was zero prior to adding these references)
    * @throws IllegalStateException if adding the given number of references would cause the count
    *           of latches to overflow (e.g. exceed {@link Integer#MAX_VALUE})
    */
   public boolean countUp(int count) {
      int oldRefs = sync.addRefs(count);
      return oldRefs == 0 && count > 0;
   }
   
   /**
    * Removes a single reference from the latch. The latch is closed while it has references and
    * opens (allowing {@linkplain #await() awaiting threads} to advance) once the count reaches
    * zero.
    * 
    * @return true if this update caused the latch to open (e.g. the number of references reached
    *           zero after removing this reference)
    * @throws IllegalStateException if the latch has no references
    */
   public boolean countDown() {
      return countDown(1);
   }
   
   /**
    * Removes the given number of single references from the latch. The latch is closed while it has
    * references and opens (allowing {@linkplain #await() awaiting threads} to advance) once the
    * count reaches zero.
    * 
    * <p>If a negative number is provided, this acts like {@link #countUp()}, adding references,
    * but its return value would always be false in this case.
    * 
    * @return true if this update caused the latch to open (e.g. the number of references reached
    *           zero after removing these references)
    * @throws IllegalStateException if the latch has fewer references than the given count
    */
   public boolean countDown(int count) {
      int oldRefs = sync.addRefs(-count);
      return oldRefs - count == 0 && count > 0;
   }

   /**
    * Returns the current number of references.
    * @return the current number of references
    */
   public int getCount() {
      return sync.getCount();
   }
   
   @Override
   public boolean isDone() {
      return getCount() == 0;
   }

   /**
    * Waits for the number of references to reach zero. If the number of references is already
    * zero, returns immediately.
    *
    * @throws InterruptedException if this thread is interrupted while waiting
    */
   @Override
   public void await() throws InterruptedException {
      sync.acquireSharedInterruptibly(0);
   }

   /**
    * Waits up to specified amount of time for the number of references to reach zero. If the number
    * of references is already zero, returns immediately.
    *
    * @param limit the maximum amount of time to wait
    * @param unit the unit of {@code limit}
    * @return true if the count reached zero or false if the time limit was encountered first
    * @throws InterruptedException if this thread is interrupted while waiting
    */
   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      return sync.tryAcquireSharedNanos(0, unit.toNanos(limit));
   }

   @Override
   public String toString() {
       return super.toString() + "[count=" + sync.getCount() + "]";
   }
   
   private static class Sync extends AbstractQueuedSynchronizer {
      private static final long serialVersionUID = -6547241985969900801L;

      @Override
      protected int tryAcquireShared(int arg) {
         if (getState() == 0) {
            return 1;
         }
         return -1;
      }
      
      @Override
      protected boolean tryReleaseShared(int arg) {
         // always return true, to allow subsequent acquirers; only called when
         // addRefs results in count reaching zero
         return true;
      }

      int addRefs(int i) {
         while (true) {
            int s = getState();
            if (i > 0) {
               if (s > Integer.MAX_VALUE - i) {
                  throw new IllegalStateException("count would overflow");
               }
            } else if (i == Integer.MIN_VALUE || s < -i) {
               throw new IllegalStateException("count would become negative");
            }
            if (compareAndSetState(s, s + i)) {
               if (s + i == 0) {
                  releaseShared(0);
               }
               return s;
            }
         }
      }

      int getCount() {
         return getState();
      }
   }
}
