package com.bluegosling.concurrent.fluent;

import static com.bluegosling.concurrent.fluent.FluentFuture.makeFluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.fluent.FluentFuture;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Test cases for the future implementation returned from
 * {@link FluentFuture#fromCompletionStage(CompletionStage)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MakeFluentCompletableFutureTest extends AbstractFluentFutureTest {

   /**
    * The {@link CompletableFuture} that backs the {@link FluentFuture} under test.
    */
   CompletableFuture<String> completableFuture;
   
   @Override
   public void setUp() {
      completableFuture = new CompletableFuture<>();
      super.setUp();
   }
   
   @Override
   protected boolean cancelReturnsTrueAfterTaskCancelled() {
      // silly CompletableFuture...
      return true;
   }
   
   @Override
   protected boolean supportsInterruption() {
      // Sadly, CompletableFuture doesn't do anything useful when it is cancelled
      // (unless sub-classed)
      return false;
   }
   
   @Override
   protected FluentFuture<String> makeFuture() {
      return makeFluent(completableFuture);
   }
   
   @Override
   protected TaskState completeSuccessfully(String result) {
      completableFuture.complete(result);
      return TaskState.NOT_STARTED;
   }

   @Override
   protected TaskState completeUnsuccessfully(Throwable t) throws Exception {
      completableFuture.completeExceptionally(t);
      return TaskState.NOT_STARTED;
   }

   @Test public void cancellation_wrappedFutureCancelled() throws Exception {
      doCancellation(new Callable<TaskState>() {
         @Override public TaskState call() throws Exception {
            // may interrupt, but since task never started there's nothing to interrupt
            assertTrue(completableFuture.cancel(true));
            return TaskState.NOT_STARTED;
         }
      });
      assertEquals(0, interruptCount.get());
   }
   
   @Test public void asCompletionStage() {
      // make sure that unwrapping it works correctly
      assertSame(completableFuture, future.asCompletionStage());
   }
}
