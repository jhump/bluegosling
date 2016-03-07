package com.bluegosling.concurrent.fluent;

import static com.bluegosling.concurrent.FutureListener.forRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.bluegosling.concurrent.SimpleFutureVisitor;
import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentRepeatingFutureTask;
import com.bluegosling.concurrent.scheduler.Rescheduler;
import com.bluegosling.util.Clock;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for {@link FluentRepeatingFutureTask}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FluentRepeatingFutureTaskTest extends FluentScheduledFutureTaskTest {

   AtomicInteger instanceListenCount;
   
   @Override
   public void setUp() {
      super.setUp();
      instanceListenCount = new AtomicInteger();
   }
   
   @Override
   protected FluentRepeatingFutureTask<String> makeFuture(long scheduledStartNanoTime,
         final Clock clock) {
      return new FluentRepeatingFutureTask<String>(underlyingTask(), scheduledStartNanoTime,
            Rescheduler.atFixedRate(100, TimeUnit.SECONDS)) {
         @Override protected long now() {
            return clock.nanoTime();
         }
      };
   }
   
   @Override
   protected FluentRepeatingFutureTask<String> makeFuture(long scheduledStartNanoTime) {
      FluentRepeatingFutureTask<String> result =
            new FluentRepeatingFutureTask<String>(underlyingTask(), scheduledStartNanoTime,
                  Rescheduler.atFixedRate(100, TimeUnit.SECONDS));
      
      result.addListenerForEachInstance(forRunnable(new Runnable() {
         @Override public void run() {
            instanceListenCount.incrementAndGet();
         }
      }), SameThreadExecutor.get());
      
      return result;
   }

   @Override
   protected FluentRepeatingFutureTask<String> future() {
      return (FluentRepeatingFutureTask<String>) future;
   }
   
   @Override
   protected void whenDone(TaskState taskState) throws Exception {
      assertEquals(1, instanceListenCount.get());
      
      // since the future is completed, the listener should be invoked immediately
      future().addListenerForEachInstance(forRunnable(new Runnable() {
         @Override public void run() {
            instanceListenCount.incrementAndGet();
         }
      }), SameThreadExecutor.get());
      assertEquals(2, instanceListenCount.get());
   }

   protected void afterSuccessChecks(String s, int howManyExecutions) throws Exception {
      assertEquals(s, future().getMostRecentResult());
      assertEquals(howManyExecutions, future().executionCount());
      assertEquals(howManyExecutions, runCount.get());
      assertEquals(0, listenCount.get());
      assertEquals(howManyExecutions, instanceListenCount.get());
      assertFalse(future.isDone());
      assertFalse(future.isCancelled());
      assertFalse(future.isFailed());
      assertFalse(future.isSuccessful());
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but did not catch TimeoutException");
      } catch (TimeoutException expected) {
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
      try {
         future.visit(new SimpleFutureVisitor<String>() {
            @Override public void defaultAction() {
               fail("Visitor should not be called");
            }
         });
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      
      assertEquals(0, interruptCount.get());   
   }
   
   @Override
   protected void doSuccessfulCompletion(String s) throws Exception {
      assertNotDone();
      completeSuccessfully(s);
      afterSuccessChecks(s, 1);
   }
   
   private void failAsynchronously(Throwable t, int millisDelay) {
      failure = t;
      completionDelay = millisDelay;
      new Thread(future()).start();
   }

   @Override public void asyncCompletion_get() throws Exception {
      Throwable t = new Exception();
      failAsynchronously(t, 200);
      
      try {
         future.get();
         fail("Expected but did not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(t, e.getCause());
      }
      assertTrue(future.isDone());
   }

   @Override public void asyncCompletion_timedGet() throws Exception {
      Throwable t = new Exception();
      failAsynchronously(t, 200);
      
      try {
         future.get(500, TimeUnit.MILLISECONDS);
         fail("Expected but did not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(t, e.getCause());
      }
      assertTrue(future.isDone());
   }
   
   @Override public void asyncCompletion_await() throws Exception {
      Throwable t = new Exception();
      failAsynchronously(t, 200);
      
      future.await();
      assertSame(t, future.getFailure());
      assertTrue(future.isDone());
   }

   @Override public void asyncCompletion_timedAwait() throws Exception {
      Throwable t = new Exception();
      failAsynchronously(t, 200);
      
      assertTrue(future.await(500, TimeUnit.MILLISECONDS));
      assertSame(t, future.getFailure());
      assertTrue(future.isDone());
   }
   
   @Test public void executeRepeatedly() throws Exception {
      value = "1";
      future().run();
      afterSuccessChecks("1", 1);

      value = "2";
      future().run();
      afterSuccessChecks("2", 2);

      // add another instance listener and make sure it's getting called
      final AtomicInteger otherListenCount = new AtomicInteger();
      future().addListenerForEachInstance(forRunnable(new Runnable() {
         @Override public void run() {
            otherListenCount.incrementAndGet();
         }
      }), SameThreadExecutor.get());
      assertEquals(0, otherListenCount.get()); // not invoked yet
      
      value = "3";
      future().run();
      afterSuccessChecks("3", 3);
      assertEquals(1, otherListenCount.get());

      value = "4";
      future().run();
      afterSuccessChecks("4", 4);
      assertEquals(2, otherListenCount.get());
   }      
}
