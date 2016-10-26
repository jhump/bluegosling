package com.bluegosling.concurrent.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.FluentScheduledFutureTask;
import com.bluegosling.time.Clock;
import com.bluegosling.time.FakeClock;

import org.junit.Test;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for {@link FluentScheduledFutureTask}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FluentScheduledFutureTaskTest extends FluentFutureTaskTest {

   protected FluentScheduledFutureTask<String> makeFuture(long scheduledStartNanoTime,
         final Clock clock) {
      return new FluentScheduledFutureTask<String>(underlyingTask(), scheduledStartNanoTime) {
         @Override protected long now() {
            return clock.nanoTime();
         }
      };
   }

   protected FluentScheduledFutureTask<String> makeFuture(long scheduledStartNanoTime) {
      return new FluentScheduledFutureTask<String>(underlyingTask(), scheduledStartNanoTime);
   }
   
   @Override
   protected FluentFuture<String> makeFuture() {
      return makeFuture(0);
   }

   @Override
   protected FluentScheduledFutureTask<String> future() {
      return (FluentScheduledFutureTask<String>) future;
   }
   
   @Test public void getDelay() throws Exception {
      FakeClock clock = new FakeClock();
      future = makeFuture(TimeUnit.SECONDS.toNanos(3), clock);
      
      assertEquals(3, future().getDelay(TimeUnit.SECONDS));
      assertEquals(3000, future().getDelay(TimeUnit.MILLISECONDS));
      assertEquals(3000 * 1000, future().getDelay(TimeUnit.MICROSECONDS));
      assertEquals(3000 * 1000 * 1000L, future().getDelay(TimeUnit.NANOSECONDS));
      
      clock.sleep(2,  TimeUnit.SECONDS);
      assertEquals(1, future().getDelay(TimeUnit.SECONDS));
      assertEquals(1000, future().getDelay(TimeUnit.MILLISECONDS));
      assertEquals(1000 * 1000, future().getDelay(TimeUnit.MICROSECONDS));
      assertEquals(1000 * 1000 * 1000L, future().getDelay(TimeUnit.NANOSECONDS));
      
      clock.sleep(1,  TimeUnit.SECONDS);
      assertEquals(0, future().getDelay(TimeUnit.SECONDS));
      assertEquals(0, future().getDelay(TimeUnit.MILLISECONDS));
      assertEquals(0, future().getDelay(TimeUnit.MICROSECONDS));
      assertEquals(0, future().getDelay(TimeUnit.NANOSECONDS));
      
      clock.sleep(5,  TimeUnit.MICROSECONDS);
      assertEquals(0, future().getDelay(TimeUnit.SECONDS));
      assertEquals(0, future().getDelay(TimeUnit.MILLISECONDS));
      assertEquals(-5, future().getDelay(TimeUnit.MICROSECONDS));
      assertEquals(-5000, future().getDelay(TimeUnit.NANOSECONDS));
   }
   
   @Test public void compareTo() throws Exception {
      // against another Scheduled (no need to use a fake clock for this)
      assertEquals(1, makeFuture(TimeUnit.SECONDS.toNanos(3))
            .compareTo(makeFuture(TimeUnit.SECONDS.toNanos(1))));
      assertEquals(-1, makeFuture(TimeUnit.SECONDS.toNanos(1))
            .compareTo(makeFuture(TimeUnit.SECONDS.toNanos(3))));
      assertEquals(0, makeFuture(TimeUnit.SECONDS.toNanos(2))
            .compareTo(makeFuture(TimeUnit.SECONDS.toNanos(2))));

      assertEquals(1, makeFuture(1).compareTo(makeFuture(0)));
      assertEquals(-1, makeFuture(0).compareTo(makeFuture(1)));
      assertEquals(0, makeFuture(0).compareTo(makeFuture(0)));

      // against a Delayed that is not an instance of Scheduled
      FakeClock clock = new FakeClock();
      future = makeFuture(1, clock);
      Delayed d = new TestDelayed(0);
      
      // adjusting clock effects future, but not d
      assertEquals(1, future().compareTo(d));
      clock.sleep(1, TimeUnit.NANOSECONDS);
      assertEquals(0, future().compareTo(d));
      clock.sleep(1, TimeUnit.NANOSECONDS);
      assertEquals(-1, future().compareTo(d));
   }
   
   static class TestDelayed implements Delayed {
      private final long delayNanos;
      
      TestDelayed(long delayNanos) {
         this.delayNanos = delayNanos;
      }
      
      @Override
      public int compareTo(Delayed o) {
         fail("TestDelayed.compareTo() should not get invoked");
         return 0; // make compiler happy
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
      }
      
   }
}
