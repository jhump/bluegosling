package com.bluegosling.concurrent.futures.fluent;

import com.bluegosling.concurrent.Scheduled;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

// TODO: javadoc
public class FluentScheduledFutureTask<T> extends FluentFutureTask<T>
      implements FluentScheduledFuture<T>, Scheduled, RunnableScheduledFuture<T> {

   private volatile long scheduledNanoTime;
   
   public FluentScheduledFutureTask(Callable<T> callable, long scheduledNanoTime) {
      super(callable);
      this.scheduledNanoTime = scheduledNanoTime;
   }

   public FluentScheduledFutureTask(Runnable runnable, T result, long scheduledNanoTime) {
      super(runnable, result);
      this.scheduledNanoTime = scheduledNanoTime;
   }
   
   // TODO: can we make this package-private? have to fix up references in ScheduledTaskManager
   // stuff, which is outside this package...
   protected long now() {
      return System.nanoTime();
   }
   
   @Override
   public long getDelay(TimeUnit unit) {
      long delayNanos = scheduledNanoTime - now();
      return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
   }

   @Override
   public int compareTo(Delayed o) {
      return Scheduled.COMPARATOR.compare(this,  o);
   }
   
   @Override
   public long getScheduledNanoTime() {
      return scheduledNanoTime;
   }
   
   protected void setScheduledNanoTime(long newStartTimeNanos) {
      this.scheduledNanoTime = newStartTimeNanos;
   }

   @Override
   public boolean isPeriodic() {
      return false;
   }
}
