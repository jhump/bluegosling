package com.apriori.concurrent;

import com.apriori.tuples.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: javadoc
//TODO: test
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

   private final Lock completionLock = new ReentrantLock();
   private boolean complete;
   private List<Pair<Runnable, Executor>> listeners = new LinkedList<Pair<Runnable, Executor>>();
   
   public ListenableFutureTask(Callable<T> callable) {
      super(callable);
   }

   public ListenableFutureTask(Runnable runnable, T result) {
      super(runnable, result);
   }

   @Override
   public void addListener(Runnable listener, Executor executor) {
      completionLock.lock();
      try {
         if (!complete) {
            listeners.add(Pair.create(listener, executor));
            return;
         }
      } finally {
         completionLock.unlock();
      }
      // if we get here, future is complete so run listener immediately
      executor.execute(listener);
   }
   
   @Override
   protected void done() {
      List<Pair<Runnable, Executor>> toExecute;
      completionLock.lock();
      toExecute = listeners;
      complete = true;
      listeners = null;
      completionLock.unlock();
      for (Pair<Runnable, Executor> listener : toExecute) {
         try {
            listener.getSecond().execute(listener.getFirst());
         } catch (RuntimeException e) {
            // TODO: log?
         }
      }
   }
}
