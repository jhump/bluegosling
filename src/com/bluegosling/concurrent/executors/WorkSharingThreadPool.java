package com.bluegosling.concurrent.executors;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class WorkSharingThreadPool extends AbstractExecutorService {
   
   private static class QueueKey implements Comparable<QueueKey> {
      private static final AtomicLong idGen = new AtomicLong();
      
      private final int sz;
      private final long id;
      
      QueueKey(int queueSize) {
         this.sz = queueSize;
         this.id = idGen.getAndIncrement();
      }

      @Override
      public int compareTo(QueueKey o) {
         int c = Integer.compare(sz, o.sz);
         return c == 0
               ? Long.compare(id, o.id)
               : c;
      }
   }
   
   private final ConcurrentSkipListMap<QueueKey, ConcurrentLinkedDeque<Runnable>> queues =
         new ConcurrentSkipListMap<>();

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
      Entry<QueueKey, ConcurrentLinkedDeque<Runnable>> shortest;
      while (true) {
         if (tryStartCoreThread(command)) {
            return;
         }
         shortest = queues.pollFirstEntry();
         if (shortest == null || shortest.getValue().isEmpty()) {
            if (tryStartWorker(command)) {
               return;
            }
            // another thread concurrently started the last worker; try again
            Thread.yield();
            continue;
         }
         ConcurrentLinkedDeque<Runnable> queue = shortest.getValue();
         queue.addLast(command);
         queues.put(new QueueKey(queue.size()), queue);
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
}
