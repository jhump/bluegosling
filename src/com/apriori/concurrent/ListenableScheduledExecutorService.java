package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TODO: javadoc
public interface ListenableScheduledExecutorService
      extends ListenableExecutorService, ScheduledExecutorService {
   @Override
   ListenableScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit);

   @Override
   <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

   @Override
   ListenableRepeatingFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay,
         long period, TimeUnit unit);
   
   @Override
   ListenableRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command, long initialDelay,
         long delay, TimeUnit unit);
}
