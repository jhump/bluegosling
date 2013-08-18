package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled executor service that returns {@link ListenableFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public interface ListenableScheduledExecutorService
      extends ListenableExecutorService, ScheduledExecutorService {
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override
   ListenableScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override
   <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override
   ListenableRepeatingFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay,
         long period, TimeUnit unit);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override
   ListenableRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command, long initialDelay,
         long delay, TimeUnit unit);
}
