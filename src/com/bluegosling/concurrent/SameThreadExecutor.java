package com.bluegosling.concurrent;

import com.google.common.util.concurrent.MoreExecutors;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * A simple executor that runs each task synchronously in the same thread that submits it. Unlike
 * Guava's {@link MoreExecutors#directExecutor()} and
 * {@link MoreExecutors#newDirectExecutorService()}, this implementation provides a "trampoline"
 * that queues the work for the current thread to execute, instead of actually executing it
 * immediately. This allows for very long chains of tasks without the risk of stack overflow.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class SameThreadExecutor implements Executor {
   private static final SameThreadExecutor INSTANCE = new SameThreadExecutor();
   
   private static final ThreadLocal<Queue<Runnable>> QUEUE = new ThreadLocal<>();
   
   public static SameThreadExecutor get() {
      return INSTANCE;
   }
   
   private SameThreadExecutor() {
   }
   
   @Override
   public void execute(Runnable command) {
      Queue<Runnable> queue = QUEUE.get();
      if (queue != null) {
         // We are already running a task in this executor. Just enqueue the item instead of
         // immediately running, so as not to cause a stack overflow if many commands are chained
         // this way.
         queue.add(command);
         return;
      }
      queue = new LinkedList<>();
      QUEUE.set(queue);
      boolean interrupted = false;
      try {
         queue.add(command);
         while ((command = queue.poll()) != null) {
            // clear interrupt status before invoking task
            if (Thread.interrupted()) {
               interrupted = true;
            }
            try {
               command.run();
            } catch (Throwable t) {
               try {
                  Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), t);
               } catch (Exception e) {
                  // TODO: log?
               }
            }
         }
      } finally {
         QUEUE.remove();
         // restore interrupt status
         if (interrupted) {
            Thread.currentThread().interrupt(); // sets
         } else {
            Thread.interrupted(); // clears
         }
      }
   }
}
