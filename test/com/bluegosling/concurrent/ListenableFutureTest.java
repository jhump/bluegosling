package com.bluegosling.concurrent;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for the default and static methods in {@link ListenableFuture} and the supporting future
 * implementations in {@link ListenableFutures}. Since {@link SettableFuture} doesn't
 * override any of the default methods, that is the implementation class used to test them.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ListenableFutureTest {
   
   private AbstractListenableFuture<String> future;
   private AtomicInteger executionCount;
   private AtomicInteger interruptCount;
   private Executor executor;
   
   @Before public void setUp() {
      future = new AbstractListenableFuture<String>() {
         @SuppressWarnings("synthetic-access")
         @Override protected void interrupt() {
            interruptCount.incrementAndGet();
         }
      };
      executionCount = new AtomicInteger();
      interruptCount = new AtomicInteger();
      executor = (runnable) -> {
         executionCount.incrementAndGet(); 
         SameThreadExecutor.get().execute(runnable);
      };
   }
   
   @Test public void completedFuture() throws Exception {
      new ListenableFutureChecker(ListenableFuture.completedFuture(null)).assertSuccessful(null);
      new ListenableFutureChecker(ListenableFuture.completedFuture(123)).assertSuccessful(123);
      new ListenableFutureChecker(ListenableFuture.completedFuture("Abc")).assertSuccessful("Abc");
   }

   @Test public void failedFuture() throws Exception {
      Throwable t = new Throwable();
      new ListenableFutureChecker(ListenableFuture.failedFuture(t)).assertFailed(t);
      RuntimeException e = new RuntimeException();
      new ListenableFutureChecker(ListenableFuture.failedFuture(e)).assertFailed(e);
   }
   
   @Test public void cancelledFuture() throws Exception {
      new ListenableFutureChecker(ListenableFuture.cancelledFuture()).assertCancelled();
   }
   
   @Test public void unfinishableFuture() throws Exception {
      ListenableFuture<Object> f = ListenableFuture.unfinishableFuture();
      ListenableFutureChecker checker = new ListenableFutureChecker(f).assertNotDone();
      assertFalse(f.cancel(false));
      assertFalse(f.cancel(true));
      checker.assertNotDone();
      
      assertThrows(UnsupportedOperationException.class, () -> { f.await(); });
      assertThrows(UnsupportedOperationException.class, () -> { f.get(); });
      assertThrows(UnsupportedOperationException.class, () -> { f.await(1, TimeUnit.SECONDS); });
      assertThrows(UnsupportedOperationException.class, () -> { f.get(1, TimeUnit.SECONDS); });
   }

   // Test cases for chainTo(...) 
   
   @Test public void chainTo_success() throws Exception {
      // Callable
      ListenableFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      ListenableFutureChecker checker1 = new ListenableFutureChecker(chain1).assertNotDone();
      // Runnable
      ListenableFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      ListenableFutureChecker checker2 = new ListenableFutureChecker(chain2).assertNotDone();
      // Runnable + result
      ListenableFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      ListenableFutureChecker checker3 = new ListenableFutureChecker(chain3).assertNotDone();
      // Function
      ListenableFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      ListenableFutureChecker checker4 = new ListenableFutureChecker(chain4).assertNotDone();
      
      future.setValue("abc");
      assertEquals(4, executionCount.get());
      
      checker1.assertSuccessful(1);
      checker2.assertSuccessful(null);
      checker3.assertSuccessful(2);
      checker4.assertSuccessful(3);
   }
   
   @Test public void chainTo_originalFails() throws Exception {
      // Callable
      ListenableFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      ListenableFutureChecker checker1 = new ListenableFutureChecker(chain1).assertNotDone();
      // Runnable
      ListenableFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      ListenableFutureChecker checker2 = new ListenableFutureChecker(chain2).assertNotDone();
      // Runnable + result
      ListenableFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      ListenableFutureChecker checker3 = new ListenableFutureChecker(chain3).assertNotDone();
      // Function
      ListenableFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      ListenableFutureChecker checker4 = new ListenableFutureChecker(chain4).assertNotDone();
      
      RuntimeException e = new RuntimeException();
      future.setFailure(e);
      assertEquals(4, executionCount.get());
      
      checker1.assertFailed(e);
      checker2.assertFailed(e);
      checker3.assertFailed(e);
      checker4.assertFailed(e);
   }
   
   @Test public void chainTo_chainedFails() throws Exception {
      RuntimeException e = new RuntimeException();
      // Callable
      ListenableFuture<Integer> chain1 = future.chainTo((Callable<Integer>) () -> { throw e; }, executor);
      ListenableFutureChecker checker1 = new ListenableFutureChecker(chain1).assertNotDone();
      // Runnable
      ListenableFuture<Void> chain2 = future.chainTo((Runnable) () -> { throw e; }, executor);
      ListenableFutureChecker checker2 = new ListenableFutureChecker(chain2).assertNotDone();
      // Runnable + result
      ListenableFuture<Integer> chain3 = future.chainTo(() -> { throw e; }, 2, executor);
      ListenableFutureChecker checker3 = new ListenableFutureChecker(chain3).assertNotDone();
      // Function
      ListenableFuture<Integer> chain4 = future.chainTo((str) -> { throw e; }, executor);
      ListenableFutureChecker checker4 = new ListenableFutureChecker(chain4).assertNotDone();
      
      future.setValue("abc");
      assertEquals(4, executionCount.get());
      
      checker1.assertFailed(e);
      checker2.assertFailed(e);
      checker3.assertFailed(e);
      checker4.assertFailed(e);
   }
   
   @Test public void chainTo_originalCancelled() throws Exception {
      // Callable
      ListenableFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      ListenableFutureChecker checker1 = new ListenableFutureChecker(chain1).assertNotDone();
      // Runnable
      ListenableFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      ListenableFutureChecker checker2 = new ListenableFutureChecker(chain2).assertNotDone();
      // Runnable + result
      ListenableFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      ListenableFutureChecker checker3 = new ListenableFutureChecker(chain3).assertNotDone();
      // Function
      ListenableFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      ListenableFutureChecker checker4 = new ListenableFutureChecker(chain4).assertNotDone();
      
      future.setCancelled();
      assertEquals(4, executionCount.get());
      
      checker1.assertCancelled();
      checker2.assertCancelled();
      checker3.assertCancelled();
      checker4.assertCancelled();
   }

   @Test public void chainTo_chainedCancelled() throws Exception {
      // Callable
      ListenableFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      ListenableFutureChecker checker1 = new ListenableFutureChecker(chain1).assertNotDone();
      // Runnable
      ListenableFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      ListenableFutureChecker checker2 = new ListenableFutureChecker(chain2).assertNotDone();
      // Runnable + result
      ListenableFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      ListenableFutureChecker checker3 = new ListenableFutureChecker(chain3).assertNotDone();
      // Function
      ListenableFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      ListenableFutureChecker checker4 = new ListenableFutureChecker(chain4).assertNotDone();
      
      ListenableFutureChecker checkerOrig = new ListenableFutureChecker(future);

      assertTrue(chain1.cancel(false));
      checker1.assertCancelled();
      checkerOrig.assertNotDone();
      checker2.assertNotDone();
      checker3.assertNotDone();
      checker4.assertNotDone();
      assertEquals(0, executionCount.get());

      assertTrue(chain2.cancel(false));
      checker2.assertCancelled();
      checkerOrig.assertNotDone();
      checker3.assertNotDone();
      checker4.assertNotDone();
      assertEquals(0, executionCount.get());

      assertTrue(chain3.cancel(false));
      checker3.assertCancelled();
      checkerOrig.assertNotDone();
      checker4.assertNotDone();
      assertEquals(0, executionCount.get());

      assertTrue(chain4.cancel(false));
      checker4.assertCancelled();
      checkerOrig.assertNotDone();
      assertEquals(0, executionCount.get());
   }
   
   @Test public void chainTo_chainedInterrupted() throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicInteger chainsComplete = new AtomicInteger();
      AtomicInteger chainsInterrupted = new AtomicInteger();
      CountDownLatch start = new CountDownLatch(4);
      CountDownLatch done = new CountDownLatch(4);
      Runnable block = () -> {
         try {
            start.countDown();
            latch.await();
         } catch (InterruptedException e) {
            chainsInterrupted.incrementAndGet();
         } finally {
            chainsComplete.incrementAndGet();
            done.countDown();
         }
      };
      ExecutorService service = Executors.newFixedThreadPool(4);
      try {
         // Callable
         ListenableFuture<Integer> chain1 =
               future.chainTo(() -> { block.run(); return 1; }, service);
         ListenableFutureChecker checker1 = new ListenableFutureChecker(chain1).assertNotDone();
         // Runnable
         ListenableFuture<Void> chain2 = future.chainTo(block, service);
         ListenableFutureChecker checker2 = new ListenableFutureChecker(chain2).assertNotDone();
         // Runnable + result
         ListenableFuture<Integer> chain3 = future.chainTo(block, 2, service);
         ListenableFutureChecker checker3 = new ListenableFutureChecker(chain3).assertNotDone();
         // Function
         ListenableFuture<Integer> chain4 =
               future.chainTo((str) -> { block.run(); return str.length(); }, service);
         ListenableFutureChecker checker4 = new ListenableFutureChecker(chain4).assertNotDone();
   
         future.setValue("abc");
         
         start.await();
         // chains aren't done because they are all awaiting latch
         checker1.assertNotDone();
         checker2.assertNotDone();
         checker3.assertNotDone();
         checker4.assertNotDone();
         
         assertTrue(chain1.cancel(true));
         checker1.assertCancelled();
         checker2.assertNotDone();
         checker3.assertNotDone();
         checker4.assertNotDone();
   
         assertTrue(chain2.cancel(true));
         checker2.assertCancelled();
         checker3.assertNotDone();
         checker4.assertNotDone();
   
         assertTrue(chain3.cancel(true));
         checker3.assertCancelled();
         checker4.assertNotDone();
   
         assertTrue(chain4.cancel(true));
         checker4.assertCancelled();
   
         // make sure that all chains were interrupted
         latch.countDown();
         done.await();
         assertEquals(4, chainsComplete.get());
         assertEquals(4, chainsInterrupted.get());
         
      } finally {
         service.shutdown();
      }
   }

   // Test cases for map(...) 
   
   @Test public void map_success() throws Exception {
      ListenableFuture<Integer> transform = future.map((str) -> str.length());
      ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();
      
      future.setValue("abc");
      checker.assertSuccessful(3);
   }
   
   @Test public void map_originalFails() throws Exception {
      ListenableFuture<Integer> transform = future.map((str) -> str.length());
      ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();
      
      RuntimeException e = new RuntimeException();
      future.setFailure(e);
      checker.assertFailed(e);
   }
   
   @Test public void map_mappedFails() throws Exception {
      RuntimeException e = new RuntimeException();
      ListenableFuture<Integer> transform = future.map((str) -> { throw e; });
      ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();
      
      future.setValue("abc");
      checker.assertFailed(e);
   }
   
   @Test public void map_originalCancelled() throws Exception {
      ListenableFuture<Integer> transform = future.map((str) -> str.length());
      ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();
      
      future.setCancelled();
      checker.assertCancelled();
   }

   @Test public void map_mappedCancelled() throws Exception {
      ListenableFuture<Integer> transform = future.map((str) -> str.length());
      ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();
      
      ListenableFutureChecker checkerOrig = new ListenableFutureChecker(future);

      assertTrue(transform.cancel(false));
      checker.assertCancelled();
      checkerOrig.assertCancelled();
      assertEquals(0, interruptCount.get());
   }

   @Test public void map_mappedCancelled_originalInterrupted() throws Exception {
      ListenableFuture<Integer> transform = future.map((str) -> str.length());
      ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();

      ListenableFutureChecker checkerOrig = new ListenableFutureChecker(future);

      // interrupting the transformed future will pass through to underlying
      assertTrue(transform.cancel(true));
      checker.assertCancelled();
      checkerOrig.assertCancelled();
      assertEquals(1, interruptCount.get());
   }

   @Test public void map_mappedNotInterrupted() throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicInteger chainsComplete = new AtomicInteger();
      AtomicInteger chainsInterrupted = new AtomicInteger();
      CountDownLatch start = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(1);
      Runnable block = () -> {
         try {
            start.countDown();
            latch.await();
         } catch (InterruptedException e) {
            chainsInterrupted.incrementAndGet();
         } finally {
            chainsComplete.incrementAndGet();
            done.countDown();
         }
      };
      ExecutorService service = Executors.newSingleThreadExecutor();
      try {
         ListenableFuture<Integer> transform =
               future.map((str) -> { block.run(); return str.length(); });
         ListenableFutureChecker checker = new ListenableFutureChecker(transform).assertNotDone();

         service.execute(() -> { future.setValue("abc"); });
         
         start.await();
         // transform isn't done -- waiting on latch
         checker.assertNotDone();
         
         assertTrue(transform.cancel(true));
         checker.assertCancelled();
   
         latch.countDown();
         done.await();
         // function doesn't get interrupted
         assertEquals(1, chainsComplete.get());
         assertEquals(0, chainsInterrupted.get());
         assertEquals(0, interruptCount.get());
         
      } finally {
         service.shutdown();
      }
   }
}
