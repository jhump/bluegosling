package com.apriori.concurrent;

import static com.apriori.concurrent.ListenableFuture.makeListenable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for the future implementation returned from
 * {@link ListenableFutures#makeCompletable(Future)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MakeListenableFutureTest extends AbstractListenableRunnableFutureTest {
   
   /**
    * The {@link Future} that backs the {@link ListenableFuture} under test.
    */
   RunnableFuture<String> plainFuture;
   
   @Override
   protected ListenableFuture<String> makeFuture() {
      plainFuture = new FutureTask<String>(underlyingTask());
      return makeListenable(plainFuture);
   }
   
   @Override
   protected RunnableFuture<String> future() {
      return plainFuture;
   }

   @Override
   protected TaskState completeSuccessfully(String result) {
      super.completeSuccessfully(result);
      letWatcherThreadComplete();
      return TaskState.FINISHED;
   }

   @Override
   protected TaskState completeUnsuccessfully(Throwable cause) {
      super.completeUnsuccessfully(cause);
      letWatcherThreadComplete();
      return TaskState.FINISHED;
   }
   
   @Test public void cancellation_wrappedFutureCancelled() throws Exception {
      doCancellation(new Callable<TaskState>() {
         @Override public TaskState call() throws Exception {
            // may interrupt, but since task never started there's nothing to interrupt
            assertTrue(plainFuture.cancel(true));
            letWatcherThreadComplete();
            return TaskState.NOT_STARTED;
         }
      });
      assertEquals(0, interruptCount.get());
   }
   
   void letWatcherThreadComplete() {
      // Underlying future is now done. But this ListenableFuture wrapper uses a thread
      // that blocks on the underlying one. We need to give that thread time to act on the
      // underlying future's completion to make sure the wrapper is also done. We can't
      // simply await the wrapper, because it could signal that the future is complete
      // before listeners have been executed. And we also want to wait for listeners to
      // run, too. So we'll add a listener and wait for it to be invoked.
      CountDownLatch latch = new CountDownLatch(1);
      future.addListener(FutureListener.forRunnable(() -> { latch.countDown(); }),
            SameThreadExecutor.get());
      try {
         assertTrue(latch.await(100,  TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
}
