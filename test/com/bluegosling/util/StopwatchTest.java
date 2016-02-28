package com.bluegosling.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class StopwatchTest {
   
   FakeClock clock;
   Stopwatch sw;
   
   @Before public void setUp() {
      clock = new FakeClock();
      sw = new Stopwatch(clock);
   }
   
   @Test public void justInitialized() {
      assertFalse(sw.isRunning());
      assertEquals(0, sw.read());
      clock.uninterruptedSleep(100, TimeUnit.NANOSECONDS);
      assertEquals(0, sw.read());
      assertTrue(Double.isNaN(sw.lapAverage()));
      assertArrayEquals(new long[0], sw.lapResults());
   }
   
   @Test public void startAndStop() {
      assertEquals(0, sw.start().stop().read());
      assertFalse(sw.isRunning());
      sw.start();
      assertTrue(sw.isRunning());
      clock.uninterruptedSleep(10, TimeUnit.NANOSECONDS);
      assertEquals(10, sw.read());
      clock.uninterruptedSleep(10, TimeUnit.MICROSECONDS);
      // stop and make sure it doesn't record elapsed time while stopped
      assertEquals(10_010, sw.stop().read());
      assertFalse(sw.isRunning());
      clock.uninterruptedSleep(10, TimeUnit.MILLISECONDS);
      assertEquals(10_010, sw.read());
      // resume adds more to existing time 
      sw.start();
      assertTrue(sw.isRunning());
      clock.uninterruptedSleep(10, TimeUnit.NANOSECONDS);
      assertEquals(10_020, sw.read());
   }
   
   @Test public void reset() {
      sw.start();
      assertTrue(sw.isRunning());
      clock.uninterruptedSleep(10,  TimeUnit.MILLISECONDS);
      assertEquals(10_000_000, sw.read());
      sw.lap();
      assertArrayEquals(new long[] { 10_000_000 }, sw.lapResults());
      sw.reset();
      // as if new:
      justInitialized();
      startAndStop();
   }
   
   @Test public void read_unit() {
      sw.start();
      clock.uninterruptedSleep(1000, TimeUnit.SECONDS);
      clock.uninterruptedSleep(100, TimeUnit.MILLISECONDS);
      clock.uninterruptedSleep(10, TimeUnit.MICROSECONDS);
      clock.uninterruptedSleep(1, TimeUnit.NANOSECONDS);
      assertEquals(1_000_100_010_001L, sw.read());
      assertEquals(1_000_100_010_001L, sw.read(TimeUnit.NANOSECONDS));
      assertEquals(1_000_100_010, sw.read(TimeUnit.MICROSECONDS));
      assertEquals(1_000_100, sw.read(TimeUnit.MILLISECONDS));
      assertEquals(1_000, sw.read(TimeUnit.SECONDS));
      assertEquals(16, sw.read(TimeUnit.MINUTES));
      assertEquals(0, sw.read(TimeUnit.HOURS));
      assertEquals(0, sw.read(TimeUnit.DAYS));
      clock.uninterruptedSleep(165, TimeUnit.MINUTES);
      assertEquals(3, sw.read(TimeUnit.HOURS));
      assertEquals(0, sw.read(TimeUnit.DAYS));
      clock.uninterruptedSleep(7, TimeUnit.DAYS);
      clock.uninterruptedSleep(21, TimeUnit.HOURS);
      assertEquals(8, sw.read(TimeUnit.DAYS));
   }
   
   @Test public void laps() {
      sw.start();
      clock.uninterruptedSleep(120, TimeUnit.SECONDS);
      sw.lap();
      clock.uninterruptedSleep(100, TimeUnit.SECONDS);
      sw.lap();
      clock.uninterruptedSleep(80, TimeUnit.SECONDS);
      sw.lap();
      clock.uninterruptedSleep(60, TimeUnit.SECONDS);
      sw.lap();
      sw.stop();
      clock.uninterruptedSleep(1000, TimeUnit.SECONDS);
      sw.lap(); // will be zero since stopwatch is stopped
      clock.uninterruptedSleep(50, TimeUnit.SECONDS);
      sw.start();
      clock.uninterruptedSleep(100, TimeUnit.SECONDS);
      sw.lap();
      assertArrayEquals(new long[] { 120, 100, 80, 60, 0, 100 }, sw.lapResults(TimeUnit.SECONDS));
      assertArrayEquals(new long[] { 120_000, 100_000, 80_000, 60_000, 0, 100_000 },
            sw.lapResults(TimeUnit.MILLISECONDS));
      assertArrayEquals(new long[] { 120_000_000_000L, 100_000_000_000L, 80_000_000_000L,
            60_000_000_000L, 0, 100_000_000_000L }, sw.lapResults());
      
      assertEquals(460.0 / 6, sw.lapAverage(TimeUnit.SECONDS), 0.0);
      assertEquals(460_000.0 / 6, sw.lapAverage(TimeUnit.MILLISECONDS), 0.0);
      assertEquals(460_000_000_000.0 / 6, sw.lapAverage(), 0.0);
   }
}
