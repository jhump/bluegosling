package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled executor service that provides a way to wrap tasks that are executed. Wrappers can
 * perform a range of cross-cutting concerns before and after delegating to the wrapped task.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class WrappingScheduledExecutorService extends WrappingExecutorService
      implements ListenableScheduledExecutorService {

   /**
    * Constructs a new scheduled executor service that delegates to the given scheduled executor
    * service.
    *
    * @param delegate a scheduled executor service
    */
   protected WrappingScheduledExecutorService(ScheduledExecutorService delegate) {
      super(delegate);
   }
   
   /**
    * Returns the underlying executor service.
    *
    * @return the underlying executor service
    */
   @Override
   protected ListenableScheduledExecutorService delegate() {
      return (ListenableScheduledExecutorService) super.delegate();
   }

   @Override
   public ListenableScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit) {
      return delegate().schedule(wrap(Executors.callable(command, null)), delay, unit);
   }

   @Override
   public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay,
         TimeUnit unit) {
      return delegate().schedule(wrap(callable), delay, unit);
   }

   @Override
   public ListenableRepeatingFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay,
         long period, TimeUnit unit) {
      return delegate().scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
   }

   @Override
   public ListenableRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command,
         long initialDelay, long delay, TimeUnit unit) {
      return delegate().scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
   }
}
