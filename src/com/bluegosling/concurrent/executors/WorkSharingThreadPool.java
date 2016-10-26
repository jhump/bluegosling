package com.bluegosling.concurrent.executors;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.bluegosling.collections.concurrent.ConcurrentSkipListOrderedQueue;
import com.bluegosling.collections.queues.OrderedDeque;

public class WorkSharingThreadPool extends AbstractExecutorService {
   
   private final WorkSharingThread[] workers;
   private final OrderedDeque<WorkSharingThread> orderedWorkers =
         new ConcurrentSkipListOrderedQueue<>(Comparator.comparing(t -> t.workQueue.size()));
   
   public WorkSharingThreadPool(int poolSize, ThreadFactory threadFactory) {
      workers = new WorkSharingThread[poolSize];
   }

   @Override
   public void shutdown() {
      // TODO: implement me
   }

   @Override
   public List<Runnable> shutdownNow() {
      // TODO: implement me
      return null;
   }

   @Override
   public boolean isShutdown() {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean isTerminated() {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      // TODO: implement me
      return false;
   }

   @Override
   public void execute(Runnable command) {
      WorkSharingThread leastLoaded;
      while (true) {
         if (tryStartCoreThread(command)) {
            return;
         }
         leastLoaded = orderedWorkers.pollFirst();
         if (leastLoaded == null || leastLoaded.workQueue.isEmpty()) {
            if (tryStartWorker(command)) {
               return;
            }
            // another thread concurrently started the last worker; try again
            Thread.yield();
            continue;
         }
         leastLoaded.workQueue.addLast(command);
         LockSupport.unpark(leastLoaded.thread);
         orderedWorkers.add(leastLoaded);
         return;
      }
   }

   private boolean tryStartCoreThread(Runnable command) {
      // TODO: implement me
      return false;
   }

   private boolean tryStartWorker(Runnable command) {
      // TODO: implement me
      return false;
   }
   
   private class WorkSharingThread implements Runnable {
      final ConcurrentLinkedDeque<Runnable> workQueue = new ConcurrentLinkedDeque<>();
      final Thread thread;
      
      WorkSharingThread(ThreadFactory factory) {
         this.thread = factory.newThread(this);
      }
      
      @Override
      public void run() {
         // TODO
      }
   }
}
