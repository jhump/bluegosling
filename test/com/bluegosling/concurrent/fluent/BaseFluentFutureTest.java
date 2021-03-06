package com.bluegosling.concurrent.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.fluent.AbstractFluentFuture;
import com.bluegosling.concurrent.fluent.FluentFuture;

import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * Test cases for the base implementation of {@link AbstractFluentFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class BaseFluentFutureTest extends AbstractFluentFutureTest {
   
   @Override
   protected FluentFuture<String> makeFuture() {
      return new AbstractFluentFuture<String>() {
         @Override protected void interrupt() {
            // let test know that we got the interrupt
            interrupted();
         }
      };
   }

   protected AbstractFluentFuture<String> future() {
      return (AbstractFluentFuture<String>) future;
   }
   
   @Override
   protected TaskState completeSuccessfully(String result) {
      assertTrue(future().setValue(result));
      return null;
   }

   @Override
   protected TaskState completeUnsuccessfully(Throwable failure) {
      assertTrue(future().setFailure(failure));
      return null;
   }
   
   @Override
   protected void whenDone(TaskState taskState) throws Exception {
      // once done, no other result can be set
      assertFalse(future().setValue("def"));
      assertFalse(future().setValue(null));
      assertFalse(future().setFailure(new RuntimeException()));
      assertFalse(future().setCancelled());
   }

   @Test public void cancellation_viaSetCancelled() throws Exception {
      doCancellation(new Callable<TaskState>() {
         @Override public TaskState call() {
            assertTrue(future().setCancelled());
            return null;
         }
      });
      assertEquals(0, interruptCount.get());
   }
}
