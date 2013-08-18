package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListeners.forRunnable;
import static com.apriori.concurrent.ListenableExecutors.sameThreadExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for tests of the {@link ListenableFuture} interface. Implementations of
 * this interface can extend this class to test their implementation.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractListenableFutureTest {
   
   /**
    * The state of a task represented by a completed future.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected enum State {
      /**
       * The task was never started.
       */
      NOT_STARTED,
      
      /**
       * The task was started but may not yet be completed.
       */
      RUNNING,
      
      /**
       * The started and completed.
       */
      FINISHED
   }
   
   /**
    * The listenable future under test.
    */
   protected ListenableFuture<String> future;
   
   /**
    * The number of times the listener is called.
    */
   protected AtomicInteger listenCount;
   
   /**
    * The number of times the 
    */
   protected AtomicInteger interruptCount;
   
   /**
    * Creates the implementation under test.
    * 
    * @return the implementation under test
    */
   protected abstract ListenableFuture<String> makeFuture();
   
   /**
    * Completes the future with the specified result. When this method returns, the future already
    * has completed or will complete and yield the specified result string. The value returned
    * represents the state of the corresponding task, if any. A return value of {@code null} is
    * allowed if the implementation is not backed by a task that actually executes to produce a
    * result.
    * 
    * @param result the successful result for the future under test
    * @return the state of any corresponding task
    */
   protected abstract State completeSuccessfully(String result) throws Exception;

   /**
    * Completes the future asynchronously. This is expected to schedule the future to complete with
    * the specified value after the specified delay. This default implementation spins up a new
    * thread that simple sleeps for the delay and then calls {@link #completeSuccessfully(String)}.
    * Subclasses should override if there is a more appropriate way to schedule the completion.
    * 
    * @param result the successful result for the future under test
    * @param millisDelay the delay after which the future should complete, in milliseconds
    */
   protected void completeAsynchronously(final String result, final int millisDelay)
         throws Exception {
      new Thread() {
         @Override public void run() {
            try {
               Thread.sleep(millisDelay);
               completeSuccessfully(result);
            } catch (Exception e) {
            }
         }
      }.start();
   }
   
   /**
    * Causes the future to fail with the specified cause. When this method returns, the future
    * already has completed or will complete unsuccessfully due to the specified throwable. The
    * value returned represents the state of the corresponding task, if any. A return value of
    * {@code null} is allowed if the implementation is not backed by a task that actually executes
    * to produce a result.
    * 
    * @param failure the cause of failure in the future under test
    * @return the state of any corresponding task
    */
   protected abstract State completeUnsuccessfully(Throwable failure) throws Exception;
   
   /**
    * Cancels the future. This is done via the following code:<pre>
    * assertTrue(future().cancel(false));</pre>
    * This default implementation invokes {@code cancel} and then returns {@code null}. If
    * implementations are backed by a possibly executing task, they should override and return the
    * state of that task.
    * 
    * @return the state of any corresponding task (this implementation returns {@code null})
    */
   protected State cancelNoInterrupt() throws Exception {
      assertTrue(future.cancel(false));
      return null;
   }

   /**
    * Cancels the future and interrupts it if is running. This is done via the following code:<pre>
    * assertTrue(future().cancel(true));</pre>
    * This default implementation invokes {@code cancel} and then returns {@code null}. If
    * implementations are backed by a possibly executing task, they should override and return the
    * state of that task.
    * 
    * @return the state of any corresponding task (this implementation returns {@code null})
    */
   protected State cancelMayInterruptIfRunning() throws Exception {
      assertTrue(future.cancel(true));
      return null;
   }
   
   /**
    * Returns whether or not the implementation under test supports interruption. If so, it is
    * expected that calling {@code future().cancel(true)} from a test will interrupt and the
    * future or corresponding task will in turn invoke {@link #interrupted()} on this test object.
    * 
    * <p>The default implementation returns true, assuming the future under test supports
    * interruptions. Override if necessary to return false instead.
    * 
    * @return true if the future under test supports interruptions; false otherwise
    */
   protected boolean supportsInterruption() {
      return true;
   }

   /**
    * Sub-classes should call this method when they receive an interrupt. This is used to test
    * {@link ListenableFuture#cancel(boolean)} when true is passed, to interrupt any currently
    * running task.
    */
   protected void interrupted() {
      interruptCount.incrementAndGet();
   }
   
   @Before public void setUp() {
      future = makeFuture();
      listenCount = new AtomicInteger();
      interruptCount = new AtomicInteger();
   }
   
   /**
    * Assertions made about a brand new (not done) future task. This also adds the listener that
    * increments {@link #listenCount}.
    */
   protected void preAsserts() throws Exception {
      assertFalse(future.isDone());
      assertFalse(future.isCancelled());
      assertFalse(future.isFailed());
      assertFalse(future.isSuccessful());
      
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenCount.incrementAndGet();
         }
      }), sameThreadExecutor());
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
         future.visit(new SimpleFutureVisitor<String>() {
            @Override public void defaultAction() {
               fail("Visitor should not be called");
            }
         });
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
   }

   /**
    * Assertions made about a completed future, depending on the state of the underlying task.
    * 
    * @param taskState the state of the underlying task
    */
   @SuppressWarnings("unused") // sub-classes might need to override and use it
   protected void assertDone(State taskState) throws Exception {
      assertTrue(future.isDone());
      assertTrue(future.await(0, TimeUnit.NANOSECONDS));
      assertEquals(1, listenCount.get());
      assertFalse(future.cancel(false));
      assertFalse(future.cancel(true));
      
      // since the future is completed, the listener should be invoked immediately
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenCount.incrementAndGet();
         }
      }), sameThreadExecutor());
      assertEquals(2, listenCount.get());
   }

   /**
    * Tests when the task completes successfully.
    * 
    * @param s the task's result
    */
   protected void doSuccessfulCompletion(final String s) throws Exception {
      preAsserts();
      
      assertDone(completeSuccessfully(s));
      assertFalse(future.isCancelled());
      assertFalse(future.isFailed());
      assertTrue(future.isSuccessful());
      assertEquals(s, future.get(0, TimeUnit.MILLISECONDS));
      assertEquals(s, future.get());
      assertEquals(s, future.getResult());
      try {
         future.getFailure();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      final AtomicBoolean visited = new AtomicBoolean();
      future.visit(new SimpleFutureVisitor<String>() {
         @Override public void defaultAction() {
            fail("Only successful should be visited");
         }
         
         @Override public void successful(String result) {
            assertEquals(s, result);
            visited.set(true);
         }
      });
      assertTrue(visited.get());
      
      assertEquals(0, interruptCount.get());      
   }
   
   @Test public void successfulCompletion() throws Exception {
      doSuccessfulCompletion("abc");
   }
   
   @Test public void successfulCompletion_null() throws Exception {
      doSuccessfulCompletion(null);
   }
   
   @Test public void failedCompletion() throws Exception {
      preAsserts();
      
      final Throwable t = new RuntimeException();
      assertDone(completeUnsuccessfully(t));
      
      assertFalse(future.isCancelled());
      assertTrue(future.isFailed());
      assertFalse(future.isSuccessful());
      try {
         future.get(0, TimeUnit.MILLISECONDS);
         fail("Expected but did not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(t, e.getCause());
      }
      try {
         future.get();
         fail("Expected but did not catch ExecutionException");
      } catch (ExecutionException e) {
         assertSame(t, e.getCause());
      }
      try {
         future.getResult();
         fail("Expected but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      assertSame(t, future.getFailure());
      final AtomicBoolean visited = new AtomicBoolean();
      future.visit(new SimpleFutureVisitor<String>() {
         @Override public void defaultAction() {
            fail("Only failed should be visited");
         }
         
         @Override public void failed(Throwable cause) {
            assertEquals(t, cause);
            visited.set(true);
         }
      });
      assertTrue(visited.get());

      assertEquals(0, interruptCount.get());      
   }   

   /**
    * Tests when the task is cancelled. This calls the specified callable, which both cancels the
    * task and returns the expected state of the task on cancellation. After calling it, assertions
    * about the cancelled task are then made.
    * 
    * @param cancel cancels the task and returns its state
    */
   protected void doCancellation(Callable<State> cancel) throws Exception {
      preAsserts();

      assertDone(cancel.call());
      assertTrue(future.isCancelled());
      assertFalse(future.isFailed());
      assertFalse(future.isSuccessful());
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
      future.visit(new SimpleFutureVisitor<String>() {
         @Override public void defaultAction() {
            fail("Only cancelled should be visited");
         }
         
         @Override public void cancelled() {
            visited.set(true);
         }
      });
      assertTrue(visited.get());
   }
   
   @Test public void cancellation() throws Exception {
      doCancellation(new Callable<State>() {
         @Override public State call() throws Exception {
            return cancelNoInterrupt();
         }
      });
      assertEquals(0, interruptCount.get());
   }   

   @Test public void cancellation_interrupt() throws Exception {
      doCancellation(new Callable<State>() {
         @Override public State call() throws Exception {
            return cancelMayInterruptIfRunning();
         }
      });
      assertEquals(supportsInterruption() ? 1 : 0, interruptCount.get());
   }
   
   @Test(timeout = 500) public void asyncCompletion_get() throws Exception {
      completeAsynchronously("done", 200);
      assertEquals("done", future.get());
      assertTrue(future.isDone());
   }

   @Test public void asyncCompletion_timedGet() throws Exception {
      completeAsynchronously("done", 200);
      assertEquals("done", future.get(500, TimeUnit.MILLISECONDS));
      assertTrue(future.isDone());
   }
   
   @Test(timeout = 500) public void asyncCompletion_await() throws Exception {
      completeAsynchronously("done", 200);
      future.await();
      assertEquals("done", future.getResult());
      assertTrue(future.isDone());
   }

   @Test public void asyncCompletion_timedAwait() throws Exception {
      completeAsynchronously("done", 200);
      assertTrue(future.await(500, TimeUnit.MILLISECONDS));
      assertEquals("done", future.getResult());
      assertTrue(future.isDone());
   }
}
