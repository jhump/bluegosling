package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// TODO: javadoc
// TODO: tests
public class ListenableScheduledFutureTask<T> extends ListenableFutureTask<T>
      implements ListenableScheduledFuture<T> {
   private static final AtomicLong idSequence = new AtomicLong();

   /*
    * We change both of these fields together, atomically, by synchronizing on "this". But
    * startTimeNanos is volatile so that methods that only need to read it (and not the id)
    * can do so w/out having to acquire the monitor.
    */
   
   private volatile long startTimeNanos = 0;
   private long id = 0;
   
   public ListenableScheduledFutureTask(Callable<T> callable, long startTimeNanos) {
      super(callable);
      changeStartTimeNanos(startTimeNanos);
   }

   public ListenableScheduledFutureTask(Runnable runnable, T result, long startTimeNanos) {
      super(runnable, result);
      changeStartTimeNanos(startTimeNanos);
   }

   @Override
   public long getDelay(TimeUnit unit) {
      long delayNanos = startTimeNanos - System.nanoTime();
      return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
   }

   @Override
   public int compareTo(Delayed o) {
      if (o instanceof ListenableScheduledFutureTask) {
         ListenableScheduledFutureTask<?> other = (ListenableScheduledFutureTask<?>) o;
         long myStart;
         long myId;
         long otherStart;
         long otherId;
         // atomically fetch values for both start time & id
         synchronized (this) {
            myStart = startTimeNanos;
            myId = id;
         }
         synchronized (other) {
            otherStart = other.startTimeNanos;
            otherId = other.id;
         }
         // now we can compare in a thread-safe way
         if (myStart > otherStart) {
            return 1;
         } else if (myStart < otherStart) {
            return -1;
         } else {
            // break ties using the ID, so tasks created first end up running first in the case
            // there are too few threads to run them all concurrently
            return myId > otherId ? 1 : (myId < otherId ? -1 : 0);
         }
      } else {
         long delayNanos = startTimeNanos - System.nanoTime();
         long otherDelayNanos = o.getDelay(TimeUnit.NANOSECONDS);
         return delayNanos > otherDelayNanos ? 1 
               : (delayNanos < otherDelayNanos ? -1 : 0);
      }
   }
   
   protected long getStartTimeNanos() {
      return startTimeNanos;
   }
   
   protected synchronized void changeStartTimeNanos(long newStartTimeNanos) {
      this.startTimeNanos = newStartTimeNanos;
      this.id = idSequence.incrementAndGet();
   }
}
