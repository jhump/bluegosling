package com.bluegosling.concurrent.fluent;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * A scheduled executor service that returns {@link FluentFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface FluentScheduledExecutorService
      extends FluentExecutorService, ListeningScheduledExecutorService {
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override
   FluentScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override
   <V> FluentScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override
   FluentRepeatingFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay,
         long period, TimeUnit unit);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be fluent.
    */
   @Override
   FluentRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command, long initialDelay,
         long delay, TimeUnit unit);
}
