package com.bluegosling.concurrent.executors;

import com.bluegosling.concurrent.fluent.FluentRepeatingFuture;
import com.bluegosling.concurrent.fluent.FluentScheduledExecutorService;
import com.bluegosling.concurrent.fluent.FluentScheduledFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled executor service that allows for propagation of context from submitting threads to
 * worker threads.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ContextPropagatingScheduledExecutorService extends ContextPropagatingExecutorService
      implements FluentScheduledExecutorService {

   /**
    * Constructs a new context propagating scheduled executor service.
    *
    * @param delegate the underlying scheduled executor service, for executing and scheduling tasks
    * @param propagators objects that manage and propagate context for each task submission
    */
   public ContextPropagatingScheduledExecutorService(ScheduledExecutorService delegate,
         Iterable<ContextPropagator<?>> propagators) {
      super(delegate, propagators);
   }

   /**
    * Returns the underlying executor service.
    *
    * @return the underlying executor service
    */
   @Override
   protected FluentScheduledExecutorService delegate() {
      return (FluentScheduledExecutorService) super.delegate();
   }

   @Override
   public FluentScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit) {
      return delegate().schedule(wrap(Executors.callable(command, null)), delay, unit);
   }

   @Override
   public <V> FluentScheduledFuture<V> schedule(Callable<V> callable, long delay,
         TimeUnit unit) {
      return delegate().schedule(wrap(callable), delay, unit);
   }

   @Override
   public FluentRepeatingFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay,
         long period, TimeUnit unit) {
      return delegate().scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
   }

   @Override
   public FluentRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command,
         long initialDelay, long delay, TimeUnit unit) {
      return delegate().scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
   }
}
