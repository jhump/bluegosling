package com.bluegosling.concurrent.futures.fluent;

import static com.bluegosling.concurrent.futures.fluent.FluentFuture.makeFluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.futures.fluent.FluentScheduledFutureTaskTest.TestDelayed;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for the future implementation returned from
 * {@link FluentFuture#makeFluent(ScheduledFuture)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MakeFluentScheduledFutureTest extends MakeFluentFutureTest {
   
   ScheduledExecutorService scheduledExecutor;
   
   @Override
   public void setUp() {
      scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
      super.setUp();
   }
   
   @After
   public void tearDown() {
      scheduledExecutor.shutdownNow();
   }
   
   @Override
   protected FluentFuture<String> makeFuture() {
      return makeFuture(5, TimeUnit.SECONDS);
   }
   
   FluentScheduledFuture<String> makeFuture(long delay, TimeUnit unit) {
      ScheduledFuture<String> scheduledFuture =
            scheduledExecutor.schedule(underlyingTask(), delay, unit);
      plainFuture = (RunnableScheduledFuture<String>) scheduledFuture;
      return makeFluent(scheduledFuture);
   }
   
   @Override
   protected RunnableScheduledFuture<String> future() {
      return (RunnableScheduledFuture<String>) plainFuture;
   }
   
   @Test public void getDelay() {
      assertDelayApproximately(
            makeFuture(999999, TimeUnit.SECONDS), TimeUnit.SECONDS.toNanos(999999));
      assertDelayApproximately(makeFuture(3, TimeUnit.SECONDS), TimeUnit.SECONDS.toNanos(3));
      assertDelayApproximately(makeFuture(1, TimeUnit.SECONDS), TimeUnit.SECONDS.toNanos(1));
      assertDelayApproximately(makeFuture(0, TimeUnit.SECONDS), TimeUnit.SECONDS.toNanos(0));
   }
   
   @Test public void compareTo() {
      // can't really test when compareTo returns 0 since that requires creating two instances
      // scheduled for exactly the same nanosecond
      
      // against other scheduled futures
      assertEquals(1, makeFuture(3, TimeUnit.SECONDS)
            .compareTo(makeFuture(1, TimeUnit.SECONDS)));
      assertEquals(-1, makeFuture(1, TimeUnit.SECONDS)
            .compareTo(makeFuture(3, TimeUnit.SECONDS)));
      
      // against other instances of Delayed
      Delayed f = makeFuture(1, TimeUnit.SECONDS);
      Delayed d = new TestDelayed(0);
      assertEquals(1, f.compareTo(d));
      d = new TestDelayed(TimeUnit.SECONDS.toNanos(3));
      assertEquals(-1, f.compareTo(d));
   }
   
   private void assertDelayApproximately(Delayed d, long expectedDelayNanos) {
      long lowerBoundNanos = expectedDelayNanos - TimeUnit.MILLISECONDS.toNanos(200);
      for (TimeUnit unit : TimeUnit.values()) {
         long upperBound = unit.convert(expectedDelayNanos, TimeUnit.NANOSECONDS);
         long lowerBound = unit.convert(lowerBoundNanos, TimeUnit.NANOSECONDS);
         long delay = d.getDelay(unit);
         assertTrue(delay <= upperBound);
         assertTrue(delay >= lowerBound);
      }
   }
}
