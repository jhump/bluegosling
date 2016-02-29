package com.bluegosling.concurrent.executors;

import com.bluegosling.concurrent.futures.fluent.FluentRepeatingFuture;
import com.bluegosling.concurrent.futures.fluent.FluentScheduledFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * A {@link InterceptingExecutorService} that provides the facility to schedule tasks.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see InterceptingExecutorService
 */
public class InterceptingScheduledExecutorService extends InterceptingExecutorService
      implements FluentScheduledExecutorService {

   /**
    * Constructs a new intercepting scheduled executor service.
    *
    * @param delegate the underlying scheduled executor service, for executing and scheduling tasks
    * @param interceptors the interceptors that are applied to each task executed
    */
   public InterceptingScheduledExecutorService(ScheduledExecutorService delegate,
         Iterable<? extends Interceptor> interceptors) {
      super(delegate, interceptors);
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
