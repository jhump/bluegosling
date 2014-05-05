package com.apriori.concurrent;

import com.apriori.concurrent.scheduler.Rescheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// TODO: javadoc
public class ListenableRepeatingFutureTask<T> extends ListenableScheduledFutureTask<T>
      implements ListenableRepeatingFuture<T> {

   private static ThreadLocal<AtomicReference<?>> lastCreatedResultHolder =
         new ThreadLocal<AtomicReference<?>>();

   private static <T> Callable<T> wrap(final Callable<T> callable) {
      final AtomicReference<T> latestResult = new AtomicReference<T>();
      Callable<T> wrapped = new Callable<T>() {
         @Override
         public T call() throws Exception {
            T result = callable.call();
            latestResult.set(result);
            return result;
         }
      };
      lastCreatedResultHolder.set(latestResult);
      return  wrapped;
   }
   
   private final Rescheduler<? super T> rescheduler;
   private final AtomicInteger executionCount = new AtomicInteger();
   private final AtomicReference<T> latestResult;
   private final FutureListenerSet<T> occurrenceListeners = new FutureListenerSet<T>(this);
   
   public ListenableRepeatingFutureTask(Callable<T> callable, long startTimeNanos,
         Rescheduler<? super T> rescheduler) {
      super(wrap(callable), startTimeNanos);
      this.rescheduler = rescheduler;
      @SuppressWarnings("unchecked") // we just put object of right type in call to wrap()
      AtomicReference<T> resultHolder = (AtomicReference<T>) lastCreatedResultHolder.get();
      lastCreatedResultHolder.remove();
      this.latestResult = resultHolder;
   }

   public ListenableRepeatingFutureTask(Runnable runnable, T result, long startTimeNanos,
         Rescheduler<? super T> rescheduler) {
      super(runnable, result, startTimeNanos);
      this.rescheduler = rescheduler;
      // No need to wrap the runnable and set the result per invocation because it is
      // always the same value. So we set it eagerly. Calls to getMostRecentResult() will
      // still fail if the task has never run since it checks that execution count > 0.
      this.latestResult = new AtomicReference<T>(result);
   }

   @Override
   public void addListenerForEachInstance(FutureListener<? super T> listener, Executor executor) {
      occurrenceListeners.addListener(listener, executor);
   }
   
   @Override
   public void run() {
      if (runAndReset()) {
         executionCount.incrementAndGet();
         Runnable toExecute = occurrenceListeners.snapshot();
         setScheduledNanoTime(rescheduler.computeNextStartTime(this, getScheduledNanoTime()));
         toExecute.run();
      }
   }
   
   @Override
   protected void done() {
      // increment count to account for this final instance
      executionCount.incrementAndGet();
      super.done();
      occurrenceListeners.run();
   }

   @Override
   public int executionCount() {
      return executionCount.get();
   }

   @Override
   public T getMostRecentResult() {
      if (executionCount.get() == 0) {
         throw new IllegalStateException();
      }
      return latestResult.get();
   }

   @Override
   public boolean isPeriodic() {
      return true;
   }
}
