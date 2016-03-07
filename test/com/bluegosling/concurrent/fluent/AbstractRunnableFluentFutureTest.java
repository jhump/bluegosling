package com.bluegosling.concurrent.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.fluent.FluentFuture;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Additional scaffolding for testing {@link FluentFuture} implementations that are instances of
 * or are based on instances of {@link RunnableFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractRunnableFluentFutureTest extends AbstractFluentFutureTest {

   /**
    * How long the task will wait, in milliseconds, before completing. Set this field from a test
    * case before running the task.
    */
   int completionDelay;
   
   /**
    * A latch that is counted down when the task starts.
    */
   CountDownLatch startedLatch;
   
   /**
    * A latch that is counted down when the task completes.
    */
   CountDownLatch completedLatch;
   
   /**
    * The value returned by the task. Set this field from a test case to control the task's result.
    */
   String value;
   
   /**
    * The exception thrown by the task. If not null, the task will fail and {@link #value} is
    * ignored. Set this field from a test case to cause the task to fail.
    */
   Throwable failure;
   
   /**
    * The number of times the task has run. The task itself increments this value when it runs.
    */
   AtomicInteger runCount;
   
   protected Callable<String> underlyingTask() {
      return new Callable<String>() {
         @Override public String call() throws Exception {
            startedLatch.countDown();
            runCount.incrementAndGet();
            try {
               if (completionDelay > 0) {
                  Thread.sleep(completionDelay);
               }
               if (failure != null) {
                  if (failure instanceof Error) {
                     throw (Error) failure;
                  }
                  throw (Exception) failure;
               }
               return value;
            } catch (InterruptedException e) {
               interruptCount.incrementAndGet();
               throw e;
            } finally {
               completedLatch.countDown();
            }
         }
      };
   }
   
   @SuppressWarnings("unchecked")
   protected RunnableFuture<String> future() {
      return (RunnableFuture<String>) future;
   }
   
   @Override
   protected TaskState completeSuccessfully(String result) {
      this.value = result;
      future().run();
      return TaskState.FINISHED;
   }

   @Override
   protected void completeAsynchronously(String result, int millisDelay) {
      value = result;
      completionDelay = millisDelay;
      new Thread(future()).start();
   }

   @Override
   protected TaskState completeUnsuccessfully(Throwable cause) {
      if (!(cause instanceof Error || cause instanceof Exception)) {
         throw new IllegalArgumentException();
      }
      this.failure = cause;
      future().run();
      return TaskState.FINISHED;
   }
   
   @Override
   protected TaskState cancelNoInterrupt() {
      assertTrue(future.cancel(false));
      future().run(); // run to make sure this doesn't "overwrite" the cancellation above
      return TaskState.NOT_STARTED;
   }
   
   @Override
   protected TaskState cancelMayInterruptIfRunning() throws Exception {
      completionDelay = 5000;
      Thread th = new Thread(future());
      th.start();
      startedLatch.await();
      assertTrue(future.cancel(true));
      // we want to make sure actual thread sees cancellation and returns so we can safely
      // make some more assertions
      th.join(300);
      assertFalse(th.isAlive());
      return TaskState.FINISHED;
   }
   
   @Override
   public void setUp() {
      super.setUp();
      completionDelay = 0;
      completedLatch = new CountDownLatch(1);
      startedLatch = new CountDownLatch(1);
      runCount = new AtomicInteger();
   }

   @Override
   protected void whenDone(TaskState taskState) throws Exception {
      if (taskState == TaskState.FINISHED) {
         assertTrue(completedLatch.getCount() == 0);
         assertEquals(1, runCount.get());
      } else if (taskState == TaskState.RUNNING) {
         // if async runner not necessarily done, we need to wait on it
         assertTrue(completedLatch.await(100,  TimeUnit.MILLISECONDS));
         assertEquals(1, runCount.get());
      } else {
         assertEquals(0, runCount.get());
      }
   }

   @Test public void cancellation_noOpInterrupt() throws Exception {
      doCancellation(new Callable<TaskState>() {
         @Override public TaskState call() throws Exception {
            // may interrupt, but since task never started there's nothing to interrupt
            assertTrue(future.cancel(true));
            future().run();
            return TaskState.NOT_STARTED;
         }
      });
      assertEquals(0, interruptCount.get());
   }
}
