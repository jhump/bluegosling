package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListeners.forRunnable;
import static com.apriori.concurrent.ListenableFutures.sameThreadExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for {@link ListenableFutureTask}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: need more tests/assertions for other ListenableFuture methods
public class ListenableFutureTaskTest {

   private enum State {
      NOT_STARTED, RUNNING, FINISHED
   }
   
   ListenableFutureTask<String> future;
   volatile int completionDelay;
   CountDownLatch completedLatch;
   String value;
   Exception failure;
   AtomicInteger runCount;
   AtomicInteger listenCount;
   AtomicInteger interruptCount;
   
   @Before public void setUp() {
      completionDelay = 0;
      completedLatch = new CountDownLatch(1);
      future = new ListenableFutureTask<String>(new Callable<String>() {
         @Override public String call() throws Exception {
            runCount.incrementAndGet();
            try {
               if (completionDelay > 0) {
                  Thread.sleep(completionDelay);
               }
               if (failure != null) {
                  throw failure;
               }
               return value;
            } catch (InterruptedException e) {
               interruptCount.incrementAndGet();
               throw e;
            } finally {
               completedLatch.countDown();
            }
         }
      });
      runCount = new AtomicInteger();
      listenCount = new AtomicInteger();
      interruptCount = new AtomicInteger();
   }
   
   private void preAsserts() throws Exception {
      assertFalse(future.isDone());
      assertFalse(future.isCancelled());
      
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenCount.incrementAndGet();
         }
      }), sameThreadExecutor());
      assertEquals(0, listenCount.get());
      
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but not catch TimeoutException");
      } catch (TimeoutException expected) {
      }
   }

   private void assertDone(State taskState) throws Exception {
      assertTrue(future.isDone());
      assertEquals(1, listenCount.get());
      assertFalse(future.cancel(false));
      assertFalse(future.cancel(true));
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

   private void postAsserts() {
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenCount.incrementAndGet();
         }
      }), sameThreadExecutor());
      assertEquals(2, listenCount.get());
      assertTrue(future.isDone());
   }

   private void doSuccessfullyComplete(String s) throws Exception {
      preAsserts();
      
      this.value = s;
      future.run();
      assertDone(State.FINISHED);
      assertFalse(future.isCancelled());
      assertEquals(s, future.get(0, TimeUnit.MILLISECONDS));
      assertEquals(s, future.get());

      postAsserts();
      
      assertEquals(0, interruptCount.get());
   }
   
   @Test public void successfulCompletion() throws Exception {
      doSuccessfullyComplete("abc");
   }
   
   @Test public void successfulCompletion_null() throws Exception {
      doSuccessfullyComplete(null);
   }
   
   @Test public void failedCompletion() throws Exception {
      preAsserts();
      
      Throwable t = failure = new RuntimeException();
      future.run();
      assertDone(State.FINISHED);
      assertFalse(future.isCancelled());
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(t, e.getCause());
      }
      try {
         future.get();
         fail("Expected but not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(t, e.getCause());
      }

      postAsserts();
      
      assertEquals(0, interruptCount.get());
   }   

   private void doCancel(Callable<State> cancel) throws Exception {
      preAsserts();

      assertDone(cancel.call());
      assertTrue(future.isCancelled());
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but not catch CancellationException");
      } catch (CancellationException expected) {
      }
      try {
         future.get();
         fail("Expected but not catch CancellationException");
      } catch (CancellationException expected) {
      }

      postAsserts();
   }
   
   @Test public void cancellation() throws Exception {
      doCancel(new Callable<State>() {
         @Override public State call() {
            assertTrue(future.cancel(false));
            future.run();
            return State.NOT_STARTED;
         }
      });
      assertEquals(0, interruptCount.get());
   }   

   @Test public void cancellation_noOpInterrupt() throws Exception {
      doCancel(new Callable<State>() {
         @Override public State call() {
            // may interrupt, but since task never started there's nothing to interrupt
            assertTrue(future.cancel(true));
            future.run();
            return State.NOT_STARTED;
         }
      });
      assertEquals(0, interruptCount.get());
   }

   @Test(timeout = 500) public void cancellation_interrupt() throws Exception {
      completionDelay = 1000;
      doCancel(new Callable<State>() {
         @Override public State call() throws InterruptedException {
            new Thread(future).start();
            Thread.sleep(200);
            assertTrue(future.cancel(true));
            return State.RUNNING;
         }
      });
      assertEquals(1, interruptCount.get());
   }

   @Test(timeout = 500) public void asyncCompletion_get() throws Exception {
      value = "done";
      completionDelay = 200;
      new Thread(future).start();
      assertEquals("done", future.get());
   }

   @Test(timeout = 500) public void asyncCompletion_timedGet() throws Exception {
      value = "done";
      completionDelay = 200;
      new Thread(future).start();
      assertEquals("done", future.get(1, TimeUnit.SECONDS));
   }
}
