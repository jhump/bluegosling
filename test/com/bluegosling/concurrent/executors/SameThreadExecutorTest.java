package com.bluegosling.concurrent.executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.vars.Variable;
import com.bluegosling.vars.VariableInt;

import org.junit.Test;

import java.util.concurrent.FutureTask;


public class SameThreadExecutorTest {

   @Test public void runsAllTasksOnCallingThread() {
      Thread caller = Thread.currentThread();
      VariableInt count = new VariableInt();
      for (int i = 0; i < 100; i++) {
         SameThreadExecutor.get().execute(() -> {
            // verify that every task sees it's running in  calling thread
            if (Thread.currentThread() == caller) {
               count.incrementAndGet();
            }
         });
      }
      assertEquals(100, count.get());
   }
   
   // TODO: maybe this executor *should* leak interruption state from caller to task and vice versa
   
   @Test public void doesNotLeakInterruption_fromCallerToTask() throws Exception {
      FutureTask<Void> task = new FutureTask<>(() -> {
         assertFalse(Thread.currentThread().isInterrupted());
      }, null);
      // caller is interrupted
      Thread.currentThread().interrupt();
      // but task will not be
      SameThreadExecutor.get().execute(task);
      assertTrue(task.isDone());
      assertTrue(Thread.interrupted()); // clears interrupt status
      task.get();
   }

   @Test public void doesNotLeakInterruption_fromTaskToCaller() throws Exception {
      FutureTask<Void> task = new FutureTask<>(() -> {
         // task is interrupted
         Thread.currentThread().interrupt();
      }, null);
      SameThreadExecutor.get().execute(task);
      assertTrue(task.isDone());
      // but caller will not be
      assertFalse(Thread.currentThread().isInterrupted());
      task.get();
   }

   @Test public void doesNotPropagateExceptions() {
      for (int i = 0; i < 100; i++) {
         SameThreadExecutor.get().execute(() -> { throw new RuntimeException(); });
      }
   }
   
   @Test public void usesUncaughtExceptionHandler() {
      VariableInt uncaught = new VariableInt();
      Thread.currentThread()
            .setUncaughtExceptionHandler((thread, ex) -> uncaught.incrementAndGet());
      for (int i = 0; i < 100; i++) {
         SameThreadExecutor.get().execute(() -> { throw new RuntimeException(); });
      }
      assertEquals(100, uncaught.get());
   }

   @Test public void doesNotCauseStackOverflow() {
      Variable<Throwable> caught = new Variable<>();
      Thread.currentThread().setUncaughtExceptionHandler((thread, ex) -> caught.set(ex));
      
      VariableInt countDown = new VariableInt(10_000_000);
      VariableInt invocations = new VariableInt();
      Variable<Runnable> task = new Variable<>();
      // chains together ten million tasks 
      task.set(() -> {
         invocations.incrementAndGet();
         if (countDown.decrementAndGet() > 0) {
            SameThreadExecutor.get().execute(task.get());
         }
      });
      SameThreadExecutor.get().execute(task.get());
      // If we chained tasks together incorrectly (e.g. directly invoke one task while the other
      // is still on the stack), we'll get a StackOverflowError. Make sure that didn't happen.
      assertNull("Unwanted exception: " + caught.get(), caught.get());
      
      assertEquals(0, countDown.get());
      assertEquals(10_000_000, invocations.get());
   }
}
