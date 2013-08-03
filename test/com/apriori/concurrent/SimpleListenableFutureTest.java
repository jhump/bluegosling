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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for {@link SimpleListenableFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: need more tests/assertions for other ListenableFuture methods
public class SimpleListenableFutureTest {
   
   SimpleListenableFuture<String> future;
   AtomicInteger listenCount;
   AtomicInteger interruptCount;
   
   @Before public void setUp() {
      future = new SimpleListenableFuture<String>() {
         @Override protected void interrupt() {
            interruptCount.incrementAndGet();
         }
      };
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

   private void assertDone() {
      assertTrue(future.isDone());
      assertEquals(1, listenCount.get());
      assertFalse(future.setValue("abc"));
      assertFalse(future.setFailure(new RuntimeException()));
      assertFalse(future.setCancelled());
      assertFalse(future.cancel(false));
      assertFalse(future.cancel(true));
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

   private void doSuccessfullyComplete(String value) throws Exception {
      preAsserts();
      
      assertTrue(future.setValue(value));
      assertFalse(future.setValue("def"));
      assertFalse(future.setValue(null));
      assertDone();
      assertFalse(future.isCancelled());
      assertEquals(value, future.get(0, TimeUnit.MILLISECONDS));
      assertEquals(value, future.get());

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
      
      Throwable t = new RuntimeException();
      assertTrue(future.setFailure(t));
      assertFalse(future.setFailure(new IllegalStateException()));
      assertDone();
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

   private void doCancel(Runnable cancel) throws Exception {
      preAsserts();

      cancel.run();
      assertFalse(future.setCancelled());
      assertFalse(future.cancel(false));
      assertFalse(future.cancel(true));
      assertDone();
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
      doCancel(new Runnable() {
         @Override public void run() {
            assertTrue(future.cancel(false));
         }
      });
      assertEquals(0, interruptCount.get());
   }   

   @Test public void cancellation_interrupt() throws Exception {
      doCancel(new Runnable() {
         @Override public void run() {
            assertTrue(future.cancel(true));
            assertEquals(1, interruptCount.get());
         }
      });
      assertEquals(1, interruptCount.get());
   }
   
   @Test public void cancellation_viaSetCancelled() throws Exception {
      doCancel(new Runnable() {
         @Override public void run() {
            assertTrue(future.setCancelled());
         }
      });
      assertEquals(0, interruptCount.get());
   } 
   
   @Test(timeout = 1000) public void asyncCompletion_get() throws Exception {
      new Thread() {
         @Override public void run() {
            try {
               Thread.sleep(200);
            } catch (InterruptedException e) {
               return;
            }
            future.setValue("done");
         }
      }.start();
      assertEquals("done", future.get());
   }

   @Test(timeout = 1000) public void asyncCompletion_timedGet() throws Exception {
      new Thread() {
         @Override public void run() {
            try {
               Thread.sleep(200);
            } catch (InterruptedException e) {
               return;
            }
            future.setValue("done");
         }
      }.start();
      assertEquals("done", future.get(1, TimeUnit.SECONDS));
   }
}
