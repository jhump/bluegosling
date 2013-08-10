package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// TODO: javadoc
// TODO: tests
public class ListenableRepeatingFutureTask<T> extends ListenableScheduledFutureTask<T>
      implements ListenableRepeatingFuture<T> {

   private static ThreadLocal<AtomicReference<?>> lastCreatedResultHolder =
         new ThreadLocal<AtomicReference<?>>();

   private static <T> Callable<T> wrap(final Callable<T> callable) {
      final AtomicReference<T> latestResult = new AtomicReference<T>();
      Callable<T> wrapped = new Callable<T>() {
         @Override
         public T call() throws Exception {
            try {
               T result = callable.call();
               latestResult.set(result);
               return result;
            } catch (Exception e) {
               latestResult.set(null);
               throw e;
            }
         }
      };
      lastCreatedResultHolder.set(latestResult);
      return  wrapped;
   }
   
   private final Rescheduler rescheduler;
   private final AtomicInteger executionCount = new AtomicInteger();
   private final AtomicReference<T> latestResult;
   private FutureListenerSet<T> occurrenceListeners = new FutureListenerSet<T>(this);
   
   public ListenableRepeatingFutureTask(Callable<T> callable, long startTimeNanos,
         Rescheduler rescheduler) {
      super(wrap(callable), startTimeNanos);
      this.rescheduler = rescheduler;
      @SuppressWarnings("unchecked") // we just put object of right type in call to wrap()
      AtomicReference<T> resultHolder = (AtomicReference<T>) lastCreatedResultHolder.get();
      lastCreatedResultHolder.remove();
      this.latestResult = resultHolder;
   }

   public ListenableRepeatingFutureTask(Runnable runnable, T result, long startTimeNanos,
         Rescheduler rescheduler) {
      super(runnable, result, startTimeNanos);
      this.rescheduler = rescheduler;
      // No need to wrap the runnable and set the result per invocation because it is
      // always the same value. So we set it eagerly. Calls to getMostRecentResult() will
      // still fail if the task has never run since it checks that execution count > 0.
      this.latestResult = new AtomicReference<T>(result);
   }

   @Override
   public void addListenerForEachInstance(FutureListener<? super T> listener, Executor executor) {
      completionLock.lock();
      try {
         if (occurrenceListeners != null) {
            // listener set gets set to null during done() so this
            // means future isn't yet complete
            occurrenceListeners.addListener(listener, executor);
            return;
         }
      } finally {
         completionLock.unlock();
      }
      // if we get here, future is complete so run listener immediately
      FutureListenerSet.runListener(this, listener, executor);
   }
   
   @Override
   public void run() {
      if (runAndReset()) {
         executionCount.incrementAndGet();
         FutureListenerSet<T> toExecute;
         completionLock.lock();
         try {
            // snapshot, so we can run listeners w/out
            // interference from concurrent attempts to add listeners;
            toExecute = occurrenceListeners == null ? null : occurrenceListeners.clone();
         } finally {
            completionLock.unlock();
         }
         // We need to check for null because concurrent call to runAndReset() could be interleaved
         // such that it executes, fails, and clears the listeners during done() before we get here.
         // In that case, we are unable to notify listeners of this penultimate instance. But that's
         // okay since this can only happen if run() is called by misbehaving client. An actual
         // executor won't be able to call next scheduled run until after we fire these listeners.
         if (toExecute != null) {
            changeStartTimeNanos(rescheduler.scheduleNextStartTime(this, getStartTimeNanos()));
            toExecute.runListeners();
         }
      }
   }
   
   @Override
   protected void done() {
      // increment count to account for this final instance
      executionCount.incrementAndGet();
      super.done();
      // notify for-each-instance listeners of this final instance
      FutureListenerSet<T> toExecute;
      completionLock.lock();
      try {
         toExecute = occurrenceListeners;
         occurrenceListeners = null;
      } finally {
         completionLock.unlock();
      }
      toExecute.runListeners();
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
}
