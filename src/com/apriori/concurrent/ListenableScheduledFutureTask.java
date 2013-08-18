package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

// TODO: javadoc
// TODO: tests
public class ListenableScheduledFutureTask<T> extends ListenableFutureTask<T>
      implements ListenableScheduledFuture<T>, Scheduled {

   private volatile long scheduledNanoTime;
   
   public ListenableScheduledFutureTask(Callable<T> callable, long scheduledNanoTime) {
      super(callable);
      this.scheduledNanoTime = scheduledNanoTime;
   }

   public ListenableScheduledFutureTask(Runnable runnable, T result, long scheduledNanoTime) {
      super(runnable, result);
      this.scheduledNanoTime = scheduledNanoTime;
   }

   @Override
   public long getDelay(TimeUnit unit) {
      long delayNanos = scheduledNanoTime - System.nanoTime();
      return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
   }

   @Override
   public int compareTo(Delayed o) {
      if (o instanceof ListenableScheduledFutureTask) {
         ListenableScheduledFutureTask<?> other = (ListenableScheduledFutureTask<?>) o;
         long myStart;
         long otherStart;
         // read values once (so we don't get inconsistent reads from volatile value)
         myStart = scheduledNanoTime;
         otherStart = other.scheduledNanoTime;
         return myStart > otherStart ? 1 : (myStart < otherStart ? -1 : 0);
      } else {
         long delayNanos = scheduledNanoTime - System.nanoTime();
         long otherDelayNanos = o.getDelay(TimeUnit.NANOSECONDS);
         return delayNanos > otherDelayNanos ? 1 
               : (delayNanos < otherDelayNanos ? -1 : 0);
      }
   }
   
   @Override
   public long getScheduledNanoTime() {
      return scheduledNanoTime;
   }
   
   protected void setScheduledNanoTime(long newStartTimeNanos) {
      this.scheduledNanoTime = newStartTimeNanos;
   }
}
