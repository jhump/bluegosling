package com.bluegosling.concurrent.executors;

import com.bluegosling.concurrent.futures.fluent.FluentFuture;
import com.bluegosling.concurrent.futures.fluent.FluentRepeatingFuture;
import com.bluegosling.concurrent.futures.fluent.FluentScheduledFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled executor service that returns {@link FluentFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface FluentScheduledExecutorService
      extends FluentExecutorService, ScheduledExecutorService {
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
