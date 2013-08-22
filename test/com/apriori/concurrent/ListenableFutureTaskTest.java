package com.apriori.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for {@link ListenableFutureTask}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ListenableFutureTaskTest extends AbstractListenableFutureTest {

   /**
    * How long the task will wait, in milliseconds, before completing. Set this field from a test
    * case before running the task.
    */
   int completionDelay;
   
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
   
   @Override
   protected ListenableFuture<String> makeFuture() {
      return new ListenableFutureTask<String>(underlyingTask());
   }

   protected ListenableFutureTask<String> future() {
      return (ListenableFutureTask<String>) future;
   }
   
   @Override
   protected State completeSuccessfully(String result) {
      this.value = result;
      future().run();
      return State.FINISHED;
   }

   @Override
   protected void completeAsynchronously(String result, int millisDelay) {
      value = result;
      completionDelay = millisDelay;
      new Thread(future()).start();
   }

   @Override
   protected State completeUnsuccessfully(Throwable cause) {
      if (!(cause instanceof Error || cause instanceof Exception)) {
         throw new IllegalArgumentException();
      }
      this.failure = cause;
      future().run();
      return State.FINISHED;
   }
   
   @Override
   protected State cancelNoInterrupt() {
      assertTrue(future.cancel(false));
      future().run(); // run to make sure this doesn't "overwrite" the cancellation above
      return State.NOT_STARTED;
   }
   
   @Override
   protected State cancelMayInterruptIfRunning() throws Exception {
      completionDelay = 1000;
      new Thread(future()).start();
      Thread.sleep(200); // make sure thread has begun executing
      assertTrue(future.cancel(true));
      return State.RUNNING;
   }
   
   @Override
   public void setUp() {
      super.setUp();
      completionDelay = 0;
      completedLatch = new CountDownLatch(1);
      runCount = new AtomicInteger();
   }

   @Override
   protected void assertDone(State taskState) throws Exception {
      super.assertDone(taskState);
      if (taskState == State.FINISHED) {
         assertTrue(completedLatch.getCount() == 0);
         assertEquals(1, runCount.get());
      } else if (taskState == State.RUNNING) {
         // if async runner not necessarily done, we need to wait on it
         assertTrue(completedLatch.await(100,  TimeUnit.MILLISECONDS));
         assertEquals(1, runCount.get());
      } else {
         assertEquals(0, runCount.get());
      }
   }

   @Test public void cancellation_noOpInterrupt() throws Exception {
      doCancellation(new Callable<State>() {
         @Override public State call() throws Exception {
            // may interrupt, but since task never started there's nothing to interrupt
            assertTrue(future.cancel(true));
            future().run();
            return State.NOT_STARTED;
         }
      });
      assertEquals(0, interruptCount.get());
   }
}
