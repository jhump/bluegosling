package com.apriori.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * Test cases for {@link SimpleListenableFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SimpleListenableFutureTest extends AbstractListenableFutureTest {
   
   @Override
   protected ListenableFuture<String> makeFuture() {
      return new SimpleListenableFuture<String>() {
         @Override protected void interrupt() {
            // let test know that we got the interrupt
            SimpleListenableFutureTest.this.interrupted();
         }
      };
   }

   protected SimpleListenableFuture<String> future() {
      return (SimpleListenableFuture<String>) future;
   }
   
   @Override
   protected State completeSuccessfully(String result) {
      assertTrue(future().setValue(result));
      return null;
   }

   @Override
   protected State completeUnsuccessfully(Throwable failure) {
      assertTrue(future().setFailure(failure));
      return null;
   }
   
   @Override
   protected void assertDone(State taskState) throws Exception {
      super.assertDone(taskState);
      // once done, no other result can be set
      assertFalse(future().setValue("def"));
      assertFalse(future().setValue(null));
      assertFalse(future().setFailure(new RuntimeException()));
      assertFalse(future().setCancelled());
   }

   @Test public void cancellation_viaSetCancelled() throws Exception {
      doCancellation(new Callable<State>() {
         @Override public State call() {
            assertTrue(future().setCancelled());
            return null;
         }
      });
      assertEquals(0, interruptCount.get());
   }
}
