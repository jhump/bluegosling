package com.bluegosling.concurrent.executors;

import com.bluegosling.concurrent.executors.FluentExecutors.FluentExecutorServiceWrapper;
import com.bluegosling.concurrent.executors.FluentExecutors.FluentScheduledExecutorServiceSchedulingWrapper;
import com.bluegosling.concurrent.executors.FluentExecutors.FluentScheduledExecutorServiceWrapper;
import com.bluegosling.concurrent.executors.FluentExecutors.SameThreadExecutorService;
import com.bluegosling.concurrent.futures.fluent.FluentFuture;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An executor service that returns {@link FluentFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface FluentExecutorService extends ExecutorService {
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override <T> FluentFuture<T> submit(Callable<T> task);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override <T> FluentFuture<T> submit(Runnable task, T result);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override FluentFuture<Void> submit(Runnable task);

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will implement {@link FluentFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException;

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will implement {@link FluentFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
         long timeout, TimeUnit unit) throws InterruptedException;

   /**
    * Returns a new executor service that runs each task synchronously in the same thread that
    * submits it. Submissions that return futures will always return completed futures.
    *
    * @return a new executor service that runs tasks immediately in the current thread
    */
   static FluentExecutorService sameThreadExecutorService() {
      return new SameThreadExecutorService();
   }

   /**
    * Converts the specified service into a {@link FluentExecutorService}. If the specified
    * service <em>is</em> already fluent, it is returned without any conversion.
    * 
    * @param executor the executor service
    * @return a fluent version of the specified service
    */
   static FluentExecutorService makeFluent(ExecutorService executor) {
      if (executor instanceof ScheduledExecutorService) {
         return executor instanceof FluentScheduledExecutorService
               ? (FluentScheduledExecutorService) executor
               : new FluentScheduledExecutorServiceWrapper((ScheduledExecutorService) executor);
      }
      
      return executor instanceof FluentExecutorService
            ? (FluentExecutorService) executor
            : new FluentExecutorServiceWrapper(executor);
   }

   /**
    * Converts the specified service into a {@link FluentScheduledExecutorService}. If the
    * specified service <em>is</em> already fluent, it is returned without any conversion.
    * 
    * @param executor the scheduled executor service
    * @return a fluent version of the specified service
    */
   static FluentScheduledExecutorService makeFluent(ScheduledExecutorService executor) {
      if (executor instanceof FluentScheduledExecutorService) {
         return (FluentScheduledExecutorService) executor;
      }
      return new FluentScheduledExecutorServiceWrapper(executor);
   }

   /**
    * Converts the specified service into a {@link FluentScheduledExecutorService}, adding
    * scheduling capability if needed. If the specified service <em>is</em> already fluent, it
    * is returned without any conversion.
    * 
    * <p>If scheduling functionality is added, any scheduled tasks that are pending when the
    * executor is shutdown will be canceled.
    * 
    * @param executor the executor service
    * @return a fluent version of the specified service that also provides scheduling functions
    * 
    * @see #makeFluentScheduled(ExecutorService, boolean)
    */
   static FluentScheduledExecutorService makeFluentScheduled(ExecutorService executor) {
      return makeFluentScheduled(executor, false);
   }

   /**
    * Converts the specified service into a {@link FluentScheduledExecutorService}, adding
    * scheduling capability if needed. If the specified service <em>is</em> already fluent, it
    * is returned without any conversion.
    * 
    * <p>If the specified service does not already support scheduling, this will return a new
    * service that adds that capability at the expense of adding one "scheduler" thread. (The thread
    * won't actually be started until the first scheduled task is submitted. Other tasks, submitted
    * for immediate execution, do not cause the thread to be created.) When
    * 
    * <p>The specified service should not be used other than through the returned wrapper. If it is,
    * it is possible for tasks to be accepted when the executor should be shutdown and vice versa,
    * for tasks to be rejected when the executor should still be active.
    * 
    * @param executor the executor service
    * @param waitForScheduledTasksOnShutdown if adding scheduling capability and this flag is true,
    *       the returned service will not terminate until after shutdown until all scheduled tasks
    *       have completed; if adding scheduling capability and this flag is false, the service will
    *       cancel pending scheduled tasks on shutdown; if not adding scheduling capability, this
    *       flag is ignored
    * @return a fluent version of the specified service that also provides scheduling functions
    */
   static FluentScheduledExecutorService makeFluentScheduled(ExecutorService executor,
         boolean waitForScheduledTasksOnShutdown) {
      if (executor instanceof FluentScheduledExecutorService) {
         return (FluentScheduledExecutorService) executor;
      } else if (executor instanceof ScheduledExecutorService) {
         return new FluentScheduledExecutorServiceWrapper((ScheduledExecutorService) executor);
      }
      return new FluentScheduledExecutorServiceSchedulingWrapper(executor,
            waitForScheduledTasksOnShutdown);
   }
}
