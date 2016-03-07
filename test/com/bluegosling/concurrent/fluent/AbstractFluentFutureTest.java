package com.bluegosling.concurrent.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.fluent.FluentFuture;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for tests of the {@link FluentFuture} interface. Implementations of
 * this interface can extend this class to test their implementation.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractFluentFutureTest {
   
   /**
    * The state of a task represented by a completed future.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected enum TaskState {
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
    * The fluent future under test.
    */
   protected FluentFuture<String> future;
   
   /**
    * Provides assertions about a complete future.
    */
   protected FluentFutureChecker futureChecker;
   
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
   protected abstract FluentFuture<String> makeFuture();
   
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
   protected abstract TaskState completeSuccessfully(String result) throws Exception;

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
   protected abstract TaskState completeUnsuccessfully(Throwable failure) throws Exception;
   
   /**
    * Cancels the future. This is done via the following code:<pre>
    * assertTrue(future().cancel(false));</pre>
    * This default implementation invokes {@code cancel} and then returns {@code null}. If
    * implementations are backed by a possibly executing task, they should override and return the
    * state of that task.
    * 
    * @return the state of any corresponding task (this implementation returns {@code null})
    */
   protected TaskState cancelNoInterrupt() throws Exception {
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
   protected TaskState cancelMayInterruptIfRunning() throws Exception {
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
    * {@link FluentFuture#cancel(boolean)} when true is passed, to interrupt any currently
    * running task.
    */
   protected void interrupted() {
      interruptCount.incrementAndGet();
   }
   
   /**
    * Controls how calls to {@link FluentFuture#cancel(boolean)} are verified. Most future
    * implementations only return true if the task was not already completed. However,
    * {@link CompletableFuture} will always return true after the task is cancelled, even if it
    * was a different (earlier) invocation that actually caused the future to be cancelled.
    *
    * @return false by default
    */
   protected boolean cancelReturnsTrueAfterTaskCancelled() {
      return false;
   }
   
   @Before public void setUp() {
      future = makeFuture();
      listenCount = new AtomicInteger();
      interruptCount = new AtomicInteger();
      futureChecker = new FluentFutureChecker(future, listenCount,
            cancelReturnsTrueAfterTaskCancelled());
   }
   
   /**
    * Assertions made about a brand new (not done) future task. This also adds the listener that
    * increments {@link #listenCount}.
    */
   protected void assertNotDone() throws Exception {
      futureChecker.assertNotDone();
   }

   /**
    * Assertions made about a future that has completed successfully.
    * 
    * @param taskState the state of the underlying task
    */
   protected void assertSuccessful(TaskState taskState, Object value)
         throws Exception {
      futureChecker.assertSuccessful(value);
      whenDone(taskState);
   }
   
   /**
    * Assertions made about a future that has failed.
    * 
    * @param taskState the state of the underlying task
    */
   protected void assertFailed(TaskState taskState, Throwable failure)
         throws Exception {
      futureChecker.assertFailed(failure);
      whenDone(taskState);
   }
   
   /**
    * Assertions made about a completed future that was cancelled.
    * 
    * @param taskState the state of the underlying task
    */
   protected void assertCancelled(TaskState taskState)
         throws Exception {
      futureChecker.assertCancelled();
      whenDone(taskState);
   }

   /**
    * Called when making assertions about a completed future, regardless of its final disposition.
    *
    * @param taskState the state of the underlying task
    */
   @SuppressWarnings("unused") // sub-classes might need to override and use taskState
   protected void whenDone(TaskState taskState) throws Exception {
   }

   /**
    * Tests when the task completes successfully.
    * 
    * @param s the task's result
    */
   protected void doSuccessfulCompletion(final String s) throws Exception {
      assertNotDone();
      assertSuccessful(completeSuccessfully(s), s);
      assertEquals(0, interruptCount.get());      
   }
   
   @Test public void successfulCompletion() throws Exception {
      doSuccessfulCompletion("abc");
   }
   
   @Test public void successfulCompletion_null() throws Exception {
      doSuccessfulCompletion(null);
   }
   
   @Test public void failedCompletion() throws Exception {
      assertNotDone();
      final Throwable t = new RuntimeException();
      assertFailed(completeUnsuccessfully(t), t);
      assertEquals(0, interruptCount.get());      
   }   

   /**
    * Tests when the task is cancelled. This calls the specified callable, which both cancels the
    * task and returns the expected state of the task on cancellation. After calling it, assertions
    * about the cancelled task are then made.
    * 
    * @param cancel cancels the task and returns its state
    */
   protected void doCancellation(Callable<TaskState> cancel) throws Exception {
      assertNotDone();
      assertCancelled(cancel.call());
   }
   
   @Test public void cancellation() throws Exception {
      doCancellation(() -> cancelNoInterrupt());
      assertEquals(0, interruptCount.get());
   }   

   @Test public void cancellation_interrupt() throws Exception {
      doCancellation(() -> cancelMayInterruptIfRunning());
      assertEquals(supportsInterruption() ? 1 : 0, interruptCount.get());
   }
   
   @Test(timeout = 500) public void asyncCompletion_get() throws Exception {
      completeAsynchronously("done", 200);
      assertFalse(future.isDone());
      assertEquals("done", future.get());
      assertTrue(future.isDone());
   }

   @Test public void asyncCompletion_timedGet() throws Exception {
      completeAsynchronously("done", 200);
      assertFalse(future.isDone());
      assertEquals("done", future.get(500, TimeUnit.MILLISECONDS));
      assertTrue(future.isDone());
   }
   
   @Test(timeout = 500) public void asyncCompletion_await() throws Exception {
      completeAsynchronously("done", 200);
      assertFalse(future.isDone());
      future.await();
      assertTrue(future.isDone());
      assertEquals("done", future.getResult());
   }

   @Test public void asyncCompletion_timedAwait() throws Exception {
      completeAsynchronously("done", 200);
      assertFalse(future.isDone());
      assertTrue(future.await(500, TimeUnit.MILLISECONDS));
      assertTrue(future.isDone());
      assertEquals("done", future.getResult());
   }
}
