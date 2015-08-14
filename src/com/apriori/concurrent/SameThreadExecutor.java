package com.apriori.concurrent;

import java.util.concurrent.Executor;

/**
 * A simple executor that runs each task synchronously in the same thread that submits it.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class SameThreadExecutor implements Executor {
   private static final SameThreadExecutor INSTANCE = new SameThreadExecutor();
   
   public static SameThreadExecutor get() {
      return INSTANCE;
   }
   
   private SameThreadExecutor() {
   }
   
   @Override
   public void execute(Runnable command) {
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
}
