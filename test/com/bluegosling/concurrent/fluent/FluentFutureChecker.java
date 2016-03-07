package com.bluegosling.concurrent.fluent;

import static com.bluegosling.concurrent.FutureListener.forRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.bluegosling.concurrent.SimpleFutureVisitor;
import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides checks for simplifying tests and assertions for {@link FluentFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: moar doc
public class FluentFutureChecker {
   
   private final FluentFuture<?> future;
   private final boolean cancelReturnsTrueAfterFutureIsCancelled;
   final CountDownLatch listenerCompletion;
   final AtomicInteger listenCount;
   
   public FluentFutureChecker(FluentFuture<?> future) {
      this(future, new AtomicInteger(), false);
   }

   public FluentFutureChecker(FluentFuture<?> future, AtomicInteger listenCount,
         boolean cancelReturnsTrueAfterFutureIsCancelled) {
      this.future = future;
      this.listenCount = listenCount;
      this.cancelReturnsTrueAfterFutureIsCancelled = cancelReturnsTrueAfterFutureIsCancelled;
      this.listenerCompletion = new CountDownLatch(1);
      // go ahead and add listener
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenCount.incrementAndGet();
            listenerCompletion.countDown();
         }
      }), SameThreadExecutor.get());
   }
   
   public FluentFutureChecker assertNotDone() throws Exception {
      assertFalse(future.isDone());
      assertFalse(future.isCancelled());
      assertFalse(future.isFailed());
      assertFalse(future.isSuccessful());
      
      assertEquals(0, listenCount.get());
      
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but did not catch TimeoutException");
      } catch (TimeoutException expected) {
      }

      assertFalse(future.await(0, TimeUnit.MILLISECONDS));
      
      try {
         future.getResult();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      try {
         future.getFailure();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      try {
         future.visit(new SimpleFutureVisitor<Object>() {
            @Override public void defaultAction() {
               fail("Visitor should not be called");
            }
         });
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      
      return this;
   }
   
   public void assertSuccessful(Object value) throws Exception {
      assertTrue(future.isSuccessful());
      assertFalse(future.isFailed());
      assertFalse(future.isCancelled());
      
      assertDone(false);

      assertEquals(value, future.get(0, TimeUnit.MILLISECONDS));
      assertEquals(value, future.get());
      assertEquals(value, future.getResult());
      try {
         future.getFailure();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      final AtomicBoolean visited = new AtomicBoolean();
      future.visit(new SimpleFutureVisitor<Object>() {
         @Override public void defaultAction() {
            fail("Only successful should be visited");
         }
         
         @Override public void successful(Object result) {
            assertEquals(value, result);
            visited.set(true);
         }
      });
      assertTrue(visited.get());
   }
   
   public void assertFailed(Throwable failure) throws Exception {
      assertFalse(future.isSuccessful());
      assertTrue(future.isFailed());
      assertFalse(future.isCancelled());
      
      assertDone(false);
      
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but did not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(failure, e.getCause());
      }
      try {
         future.get();
         fail("Expected but did not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(failure, e.getCause());
      }
      try {
         future.getResult();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      assertSame(failure, future.getFailure());
      final AtomicBoolean visited = new AtomicBoolean();
      future.visit(new SimpleFutureVisitor<Object>() {
         @Override public void defaultAction() {
            fail("Only failed should be visited");
         }
         
         @Override public void failed(Throwable cause) {
            assertEquals(failure, cause);
            visited.set(true);
         }
      });
      assertTrue(visited.get());
   }
   
   public void assertCancelled() throws Exception {
      assertFalse(future.isSuccessful());
      assertFalse(future.isFailed());
      assertTrue(future.isCancelled());
      
      assertDone(true);

      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but did not catch CancellationException");
      } catch (CancellationException expected) {
      }
      try {
         future.get();
         fail("Expected but did not catch CancellationException");
      } catch (CancellationException expected) {
      }
      try {
         future.getResult();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      try {
         future.getFailure();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      final AtomicBoolean visited = new AtomicBoolean();
      future.visit(new SimpleFutureVisitor<Object>() {
         @Override public void defaultAction() {
            fail("Only cancelled should be visited");
         }
         
         @Override public void cancelled() {
            visited.set(true);
         }
      });
      assertTrue(visited.get());
   }
   
   private void assertDone(boolean cancelled) throws Exception {
      assertTrue(future.isDone());
      assertTrue(future.await(0, TimeUnit.NANOSECONDS));
      // listener could be executing concurrently after future completed, so we give it some time
      assertTrue(listenerCompletion.await(100, TimeUnit.MILLISECONDS));
      assertEquals(1, listenCount.get());
      if (cancelReturnsTrueAfterFutureIsCancelled && cancelled) {
         assertTrue(future.cancel(false));
         assertTrue(future.cancel(true));
      } else {
         assertFalse(future.cancel(false));
         assertFalse(future.cancel(true));
      }
      
      // since the future is completed, the listener should be invoked immediately
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenCount.incrementAndGet();
         }
      }), SameThreadExecutor.get());
      assertEquals(2, listenCount.get());
   }
}
