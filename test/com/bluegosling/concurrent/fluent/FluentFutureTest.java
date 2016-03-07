package com.bluegosling.concurrent.fluent;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.AbstractFluentFuture;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.FluentFutures;
import com.bluegosling.concurrent.fluent.SettableFluentFuture;

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
 * Tests for the default and static methods in {@link FluentFuture} and the supporting future
 * implementations in {@link FluentFutures}. Since {@link SettableFluentFuture} doesn't
 * override any of the default methods, that is the implementation class used to test them.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FluentFutureTest {
   
   private AbstractFluentFuture<String> future;
   private AtomicInteger executionCount;
   private AtomicInteger interruptCount;
   private Executor executor;
   
   @Before public void setUp() {
      future = new AbstractFluentFuture<String>() {
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
      new FluentFutureChecker(FluentFuture.completedFuture(null)).assertSuccessful(null);
      new FluentFutureChecker(FluentFuture.completedFuture(123)).assertSuccessful(123);
      new FluentFutureChecker(FluentFuture.completedFuture("Abc")).assertSuccessful("Abc");
   }

   @Test public void failedFuture() throws Exception {
      Throwable t = new Throwable();
      new FluentFutureChecker(FluentFuture.failedFuture(t)).assertFailed(t);
      RuntimeException e = new RuntimeException();
      new FluentFutureChecker(FluentFuture.failedFuture(e)).assertFailed(e);
   }
   
   @Test public void cancelledFuture() throws Exception {
      new FluentFutureChecker(FluentFuture.cancelledFuture()).assertCancelled();
   }
   
   @Test public void unfinishableFuture() throws Exception {
      FluentFuture<Object> f = FluentFuture.unfinishableFuture();
      FluentFutureChecker checker = new FluentFutureChecker(f).assertNotDone();
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
      FluentFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      FluentFutureChecker checker1 = new FluentFutureChecker(chain1).assertNotDone();
      // Runnable
      FluentFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      FluentFutureChecker checker2 = new FluentFutureChecker(chain2).assertNotDone();
      // Runnable + result
      FluentFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      FluentFutureChecker checker3 = new FluentFutureChecker(chain3).assertNotDone();
      // Function
      FluentFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      FluentFutureChecker checker4 = new FluentFutureChecker(chain4).assertNotDone();
      
      future.setValue("abc");
      assertEquals(4, executionCount.get());
      
      checker1.assertSuccessful(1);
      checker2.assertSuccessful(null);
      checker3.assertSuccessful(2);
      checker4.assertSuccessful(3);
   }
   
   @Test public void chainTo_originalFails() throws Exception {
      // Callable
      FluentFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      FluentFutureChecker checker1 = new FluentFutureChecker(chain1).assertNotDone();
      // Runnable
      FluentFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      FluentFutureChecker checker2 = new FluentFutureChecker(chain2).assertNotDone();
      // Runnable + result
      FluentFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      FluentFutureChecker checker3 = new FluentFutureChecker(chain3).assertNotDone();
      // Function
      FluentFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      FluentFutureChecker checker4 = new FluentFutureChecker(chain4).assertNotDone();
      
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
      FluentFuture<Integer> chain1 = future.chainTo((Callable<Integer>) () -> { throw e; }, executor);
      FluentFutureChecker checker1 = new FluentFutureChecker(chain1).assertNotDone();
      // Runnable
      FluentFuture<Void> chain2 = future.chainTo((Runnable) () -> { throw e; }, executor);
      FluentFutureChecker checker2 = new FluentFutureChecker(chain2).assertNotDone();
      // Runnable + result
      FluentFuture<Integer> chain3 = future.chainTo(() -> { throw e; }, 2, executor);
      FluentFutureChecker checker3 = new FluentFutureChecker(chain3).assertNotDone();
      // Function
      FluentFuture<Integer> chain4 = future.chainTo((str) -> { throw e; }, executor);
      FluentFutureChecker checker4 = new FluentFutureChecker(chain4).assertNotDone();
      
      future.setValue("abc");
      assertEquals(4, executionCount.get());
      
      checker1.assertFailed(e);
      checker2.assertFailed(e);
      checker3.assertFailed(e);
      checker4.assertFailed(e);
   }
   
   @Test public void chainTo_originalCancelled() throws Exception {
      // Callable
      FluentFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      FluentFutureChecker checker1 = new FluentFutureChecker(chain1).assertNotDone();
      // Runnable
      FluentFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      FluentFutureChecker checker2 = new FluentFutureChecker(chain2).assertNotDone();
      // Runnable + result
      FluentFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      FluentFutureChecker checker3 = new FluentFutureChecker(chain3).assertNotDone();
      // Function
      FluentFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      FluentFutureChecker checker4 = new FluentFutureChecker(chain4).assertNotDone();
      
      future.setCancelled();
      assertEquals(4, executionCount.get());
      
      checker1.assertCancelled();
      checker2.assertCancelled();
      checker3.assertCancelled();
      checker4.assertCancelled();
   }

   @Test public void chainTo_chainedCancelled() throws Exception {
      // Callable
      FluentFuture<Integer> chain1 = future.chainTo(() -> 1, executor);
      FluentFutureChecker checker1 = new FluentFutureChecker(chain1).assertNotDone();
      // Runnable
      FluentFuture<Void> chain2 = future.chainTo(() -> {}, executor);
      FluentFutureChecker checker2 = new FluentFutureChecker(chain2).assertNotDone();
      // Runnable + result
      FluentFuture<Integer> chain3 = future.chainTo(() -> {}, 2, executor);
      FluentFutureChecker checker3 = new FluentFutureChecker(chain3).assertNotDone();
      // Function
      FluentFuture<Integer> chain4 = future.chainTo((str) -> str.length(), executor);
      FluentFutureChecker checker4 = new FluentFutureChecker(chain4).assertNotDone();
      
      FluentFutureChecker checkerOrig = new FluentFutureChecker(future);

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
         FluentFuture<Integer> chain1 =
               future.chainTo(() -> { block.run(); return 1; }, service);
         FluentFutureChecker checker1 = new FluentFutureChecker(chain1).assertNotDone();
         // Runnable
         FluentFuture<Void> chain2 = future.chainTo(block, service);
         FluentFutureChecker checker2 = new FluentFutureChecker(chain2).assertNotDone();
         // Runnable + result
         FluentFuture<Integer> chain3 = future.chainTo(block, 2, service);
         FluentFutureChecker checker3 = new FluentFutureChecker(chain3).assertNotDone();
         // Function
         FluentFuture<Integer> chain4 =
               future.chainTo((str) -> { block.run(); return str.length(); }, service);
         FluentFutureChecker checker4 = new FluentFutureChecker(chain4).assertNotDone();
   
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
      FluentFuture<Integer> transform = future.map((str) -> str.length());
      FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();
      
      future.setValue("abc");
      checker.assertSuccessful(3);
   }
   
   @Test public void map_originalFails() throws Exception {
      FluentFuture<Integer> transform = future.map((str) -> str.length());
      FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();
      
      RuntimeException e = new RuntimeException();
      future.setFailure(e);
      checker.assertFailed(e);
   }
   
   @Test public void map_mappedFails() throws Exception {
      RuntimeException e = new RuntimeException();
      FluentFuture<Integer> transform = future.map((str) -> { throw e; });
      FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();
      
      future.setValue("abc");
      checker.assertFailed(e);
   }
   
   @Test public void map_originalCancelled() throws Exception {
      FluentFuture<Integer> transform = future.map((str) -> str.length());
      FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();
      
      future.setCancelled();
      checker.assertCancelled();
   }

   @Test public void map_mappedCancelled() throws Exception {
      FluentFuture<Integer> transform = future.map((str) -> str.length());
      FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();
      
      FluentFutureChecker checkerOrig = new FluentFutureChecker(future);

      assertTrue(transform.cancel(false));
      checker.assertCancelled();
      checkerOrig.assertCancelled();
      assertEquals(0, interruptCount.get());
   }

   @Test public void map_mappedCancelled_originalInterrupted() throws Exception {
      FluentFuture<Integer> transform = future.map((str) -> str.length());
      FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();

      FluentFutureChecker checkerOrig = new FluentFutureChecker(future);

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
         FluentFuture<Integer> transform =
               future.map((str) -> { block.run(); return str.length(); });
         FluentFutureChecker checker = new FluentFutureChecker(transform).assertNotDone();

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
