package com.bluegosling.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/** Test cases for {@link Duration}. */
public class DurationTest {

  @Test public void basic() {
    int length = 1;
    for (TimeUnit unit : TimeUnit.values()) {
       Duration d = Duration.of(length, unit);
      assertEquals(length, d.length());
      assertEquals(unit, d.unit());

      d = create(length, unit);
      assertEquals(length, d.length());
      assertEquals(unit, d.unit());
      length++;
    }
  }

  private Duration create(long length, TimeUnit unit) {
    switch (unit) {
      case DAYS:
        return Duration.days(length);
      case HOURS:
        return Duration.hours(length);
      case MINUTES:
        return Duration.minutes(length);
      case SECONDS:
        return Duration.seconds(length);
      case MILLISECONDS:
        return Duration.millis(length);
      case MICROSECONDS:
        return Duration.micros(length);
      case NANOSECONDS:
        return Duration.nanos(length);
      default:
        // shouldn't be possible
        throw new AssertionError();
    }
  }

  @Test public void unitConversions() {
    int length = 1;
    for (TimeUnit unit : TimeUnit.values()) {
      Duration d = Duration.of(length, unit);
      for (TimeUnit targetUnit : TimeUnit.values()) {
        long expected = targetUnit.convert(length, unit);
        assertEquals(expected, d.to(targetUnit));
        assertEquals(expected, convert(d, targetUnit));
      }
      length++;
    }
  }

  private long convert(Duration d, TimeUnit unit) {
    switch (unit) {
      case DAYS:
        return d.toDays();
      case HOURS:
        return d.toHours();
      case MINUTES:
        return d.toMinutes();
      case SECONDS:
        return d.toSeconds();
      case MILLISECONDS:
        return d.toMillis();
      case MICROSECONDS:
        return d.toMicros();
      case NANOSECONDS:
        return d.toNanos();
      default:
        // shouldn't be possible
        throw new AssertionError();
    }
  }

  @Test public void unitConversions_clipOnOverflow() {
    // will overflow or underflow if converted to next finer unit ("smaller" per Enum.compareTo)
    long overflowLength = Long.MAX_VALUE / 20;
    long underflowLength = Long.MIN_VALUE / 20;

    for (TimeUnit unit : TimeUnit.values()) {
      for (TimeUnit targetUnit : TimeUnit.values()) {
        if (targetUnit.compareTo(unit) >= 0) {
          // skip conversions that won't generate overflow/underflow
          continue;
        }
        Duration d = Duration.of(overflowLength, unit);
        assertEquals(Long.MAX_VALUE, d.to(targetUnit));
        assertEquals(Long.MAX_VALUE, convert(d, targetUnit));

        d = Duration.of(underflowLength, unit);
        assertEquals(Long.MIN_VALUE, d.to(targetUnit));
        assertEquals(Long.MIN_VALUE, convert(d, targetUnit));
      }
    }
  }

  @Test public void plus() {
    Duration d1 = Duration.seconds(100);
    // plus 0
    assertSame(d1, d1.plus(0, TimeUnit.DAYS));
    assertSame(d1, Duration.nanos(0).plus(d1));
    // plus itself
    assertEquals(Duration.seconds(200), d1.plus(d1));
    // negative addend, zero result
    assertEquals(Duration.seconds(0), d1.plus(-100, TimeUnit.SECONDS));

    // some sums that require converting units

    // milliseconds is more precise
    Duration d2 = Duration.millis(1000);
    Duration expectedSum = Duration.millis(101000);
    assertEquals(expectedSum, d1.plus(d2));
    assertEquals(expectedSum, d2.plus(d1));
    assertEquals(0, d2.plus(d1).compareTo(Duration.seconds(101)));

    // seconds
    d2 = Duration.days(1);
    expectedSum = Duration.seconds(TimeUnit.DAYS.toSeconds(1) + 100);
    assertEquals(expectedSum, d1.plus(d2));
    assertEquals(expectedSum, d2.plus(d1));

    // nanoseconds
    d1 = Duration.nanos(10101);
    d2 = Duration.millis(5);
    expectedSum = Duration.nanos(5010101);
    assertEquals(expectedSum, d1.plus(d2));
    assertEquals(expectedSum, d2.plus(d1));

    // big big values
    d1 = Duration.nanos(Long.MAX_VALUE);
    d2 = Duration.nanos(Long.MIN_VALUE);
    expectedSum = Duration.nanos(-1);
    assertEquals(expectedSum, d1.plus(d2));
    assertEquals(expectedSum, d2.plus(d1));
  }

  @Test public void minus() {
    Duration d1 = Duration.seconds(100);
    // minus 0
    assertSame(d1, d1.minus(0, TimeUnit.DAYS));
    // 0 minus
    assertEquals(Duration.seconds(-100), Duration.seconds(0).minus(d1));
    // minus itself
    assertEquals(Duration.seconds(0), d1.minus(d1));
    // negative subtrahend, zero result
    assertEquals(Duration.seconds(200), d1.minus(-100, TimeUnit.SECONDS));

    // convert units -- milliseconds is more precise
    Duration d2 = Duration.millis(1000);
    assertEquals(Duration.millis(99000), d1.minus(d2));
    assertEquals(Duration.millis(-99000), d2.minus(d1));
    assertEquals(0, d1.minus(d2).compareTo(Duration.seconds(99)));

    // seconds
    d2 = Duration.days(1);
    assertEquals(Duration.seconds(100 - TimeUnit.DAYS.toSeconds(1)), d1.minus(d2));
    assertEquals(Duration.seconds(TimeUnit.DAYS.toSeconds(1) - 100), d2.minus(d1));

    // nanoseconds
    d1 = Duration.nanos(10101);
    d2 = Duration.millis(5);
    assertEquals(Duration.nanos(-4989899), d1.minus(d2));
    assertEquals(Duration.nanos(4989899), d2.minus(d1));

    // big big values
    d1 = Duration.nanos(-Long.MAX_VALUE);
    d2 = Duration.nanos(Long.MIN_VALUE);
    assertEquals(Duration.nanos(1), d1.minus(d2));
  }

  @Test public void plus_lessPreciseOnOverflow() {
    Duration d1 = Duration.nanos(Long.MAX_VALUE);
    assertEquals(Duration.micros(Long.MAX_VALUE / 500), d1.plus(d1));

    d1 = Duration.seconds(Long.MAX_VALUE);
    assertEquals(Duration.minutes(Long.MAX_VALUE / 30), d1.plus(d1));

    d1 = Duration.hours(Long.MAX_VALUE);
    assertEquals(Duration.days(Long.MAX_VALUE / 12), d1.plus(d1));

    d1 = Duration.days(Long.MAX_VALUE);
    assertEquals(d1, d1.plus(d1)); // can't get any longer than this

    d1 = Duration.nanos(Long.MAX_VALUE);
    Duration d2 = Duration.nanos(100);
    // no escalation of unit because 100 nanos isn't enough to get to next microsecond
    assertEquals(Duration.nanos(Long.MAX_VALUE), d1.plus(d2));

    d2 = Duration.nanos(1000);
    // now we escalate
    assertEquals(Duration.micros(Long.MAX_VALUE / 1000 + 1), d1.plus(d2));

    // make sure it works on mixed units

    d1 = Duration.days(1);
    d2 = Duration.nanos(Long.MAX_VALUE);
    // should end up in microseconds
    assertEquals(Duration.micros(Long.MAX_VALUE / 1000 + d1.toMicros()), d1.plus(d2));

    d1 = Duration.seconds(Long.MAX_VALUE / 1000);
    d2 = Duration.nanos(Long.MAX_VALUE);
    // should end up in seconds
    assertEquals(Duration.seconds(Long.MAX_VALUE / 1000 + d2.toSeconds()), d1.plus(d2));
  }

  @Test public void plus_lessPreciseOnUnderflow() {
    Duration d1 = Duration.nanos(Long.MIN_VALUE);
    assertEquals(Duration.micros(Long.MIN_VALUE / 500), d1.plus(d1));

    d1 = Duration.seconds(Long.MIN_VALUE);
    assertEquals(Duration.minutes(Long.MIN_VALUE / 30), d1.plus(d1));

    d1 = Duration.hours(Long.MIN_VALUE);
    assertEquals(Duration.days(Long.MIN_VALUE / 12), d1.plus(d1));

    d1 = Duration.days(Long.MIN_VALUE);
    assertEquals(d1, d1.plus(d1)); // can't get any more negative than this

    d1 = Duration.nanos(Long.MIN_VALUE);
    Duration d2 = Duration.nanos(-100);
    // no escalation of unit because 100 nanos isn't enough to get to next microsecond
    assertEquals(Duration.nanos(Long.MIN_VALUE), d1.plus(d2));

    d2 = Duration.nanos(-1000);
    // now we escalate
    assertEquals(Duration.micros(Long.MIN_VALUE / 1000 - 1), d1.plus(d2));

    // make sure it works on mixed units

    d1 = Duration.days(-1);
    d2 = Duration.nanos(-Long.MAX_VALUE);
    // should end up in microseconds
    assertEquals(Duration.micros(-Long.MAX_VALUE / 1000 + d1.toMicros()), d1.plus(d2));

    d1 = Duration.seconds(-Long.MAX_VALUE / 1000);
    d2 = Duration.nanos(-Long.MAX_VALUE);
    // should end up in seconds
    assertEquals(Duration.seconds(-Long.MAX_VALUE / 1000 + d2.toSeconds()), d1.plus(d2));
  }

  @Test public void minus_lessPreciseOnOverflow() {
    Duration d1 = Duration.nanos(Long.MAX_VALUE);
    Duration d2 = Duration.nanos(-Long.MAX_VALUE);
    assertEquals(Duration.micros(Long.MAX_VALUE / 500), d1.minus(d2));

    d1 = Duration.seconds(Long.MAX_VALUE);
    d2 = Duration.seconds(-Long.MAX_VALUE);
    assertEquals(Duration.minutes(Long.MAX_VALUE / 30), d1.minus(d2));

    d1 = Duration.hours(Long.MAX_VALUE);
    d2 = Duration.hours(-Long.MAX_VALUE);
    assertEquals(Duration.days(Long.MAX_VALUE / 12), d1.minus(d2));

    d1 = Duration.days(Long.MAX_VALUE);
    d2 = Duration.days(-Long.MAX_VALUE);
    assertEquals(d1, d1.minus(d2)); // can't get any longer than this

    d1 = Duration.nanos(Long.MAX_VALUE);
    d2 = Duration.nanos(-100);
    // no escalation of unit because 100 nanos isn't enough to get to next microsecond
    assertEquals(Duration.nanos(Long.MAX_VALUE), d1.minus(d2));

    d2 = Duration.nanos(-1000);
    // now we escalate
    assertEquals(Duration.micros(Long.MAX_VALUE / 1000 + 1), d1.minus(d2));

    // make sure it works on mixed units

    d1 = Duration.days(1);
    d2 = Duration.nanos(-Long.MAX_VALUE);
    // should end up in microseconds
    assertEquals(Duration.micros(Long.MAX_VALUE / 1000 + d1.toMicros()), d1.minus(d2));

    d1 = Duration.seconds(Long.MAX_VALUE / 1000);
    d2 = Duration.nanos(-Long.MAX_VALUE);
    // should end up in seconds
    assertEquals(Duration.seconds(Long.MAX_VALUE / 1000 - d2.toSeconds()), d1.minus(d2));
  }

  @Test public void minus_lessPreciseOnUnderflow() {
    Duration d1 = Duration.nanos(-Long.MAX_VALUE);
    Duration d2 = Duration.nanos(Long.MAX_VALUE);
    assertEquals(Duration.micros(-Long.MAX_VALUE / 500), d1.minus(d2));

    d1 = Duration.seconds(-Long.MAX_VALUE);
    d2 = Duration.seconds(Long.MAX_VALUE);
    assertEquals(Duration.minutes(-Long.MAX_VALUE / 30), d1.minus(d2));

    d1 = Duration.hours(-Long.MAX_VALUE);
    d2 = Duration.hours(Long.MAX_VALUE);
    assertEquals(Duration.days(-Long.MAX_VALUE / 12), d1.minus(d2));

    d1 = Duration.days(Long.MIN_VALUE);
    d2 = Duration.days(Long.MAX_VALUE);
    assertEquals(d1, d1.minus(d2)); // can't get any more negative than this

    d1 = Duration.nanos(Long.MIN_VALUE);
    d2 = Duration.nanos(100);
    // no escalation of unit because 100 nanos isn't enough to get to next microsecond
    assertEquals(Duration.nanos(Long.MIN_VALUE), d1.minus(d2));

    d2 = Duration.nanos(1000);
    // now we escalate
    assertEquals(Duration.micros(Long.MIN_VALUE / 1000 - 1), d1.minus(d2));

    // make sure it works on mixed units

    d1 = Duration.days(-1);
    d2 = Duration.nanos(Long.MAX_VALUE);
    // should end up in microseconds
    assertEquals(Duration.micros(-Long.MAX_VALUE / 1000 + d1.toMicros()), d1.minus(d2));

    d1 = Duration.seconds(-Long.MAX_VALUE / 1000);
    d2 = Duration.nanos(Long.MAX_VALUE);
    // should end up in seconds
    assertEquals(Duration.seconds(-Long.MAX_VALUE / 1000 - d2.toSeconds()), d1.minus(d2));
  }

  @Test public void times() {
     Duration d1 = Duration.seconds(100);
     // simple cases: 0, 1, -1
     assertEquals(Duration.seconds(0), d1.times(0));
     assertEquals(d1, d1.times(1));
     assertEquals(Duration.seconds(-100), d1.times(-1));
     // larger multiplier
     assertEquals(Duration.seconds(100_000_000), d1.times(1_000_000));
  }

  @Test public void times_lessPreciseOnOverflow() {
     // jump to next unit
     Duration d = Duration.seconds(10);
     long expectedLen = BigInteger.valueOf(Long.MAX_VALUE)
           .multiply(BigInteger.valueOf(10))
           .divide(BigInteger.valueOf(60)) // convert to min
           .longValue();
     assertEquals(Duration.minutes(expectedLen), d.times(Long.MAX_VALUE));
     // jump multiple units
     d = Duration.seconds(100);
     expectedLen = BigInteger.valueOf(Long.MAX_VALUE)
           .multiply(BigInteger.valueOf(100))
           .divide(BigInteger.valueOf(60)) // to min
           .divide(BigInteger.valueOf(60)) // to hr
           .longValue();
     assertEquals(Duration.hours(expectedLen), d.times(Long.MAX_VALUE));
     d = Duration.millis(4_000_000);
     expectedLen = BigInteger.valueOf(Long.MAX_VALUE)
           .multiply(BigInteger.valueOf(4_000_000))
           .divide(BigInteger.valueOf(1000)) // to sec
           .divide(BigInteger.valueOf(60)) // to min
           .divide(BigInteger.valueOf(60)) // to hr
           .divide(BigInteger.valueOf(24)) // to day
           .longValue();
     assertEquals(Duration.days(expectedLen), d.times(Long.MAX_VALUE));
     // clamp to Long.MAX_VALUE days
     d = Duration.hours(100);
     assertEquals(Duration.days(Long.MAX_VALUE), d.times(Long.MAX_VALUE));
     d = Duration.nanos(Long.MIN_VALUE);
     assertEquals(Duration.days(Long.MAX_VALUE), d.times(Long.MIN_VALUE));
  }
  
  @Test public void times_lessPreciseOnUnderflow() {
     // TODO
  }
  
  // TODO: tests for dividedBy()


  @Test public void compareTo() {
    for (TimeUnit unit : TimeUnit.values()) {
      Duration least = Duration.of(-123, unit);
      Duration middle = Duration.of(500, unit);
      Duration greatest = Duration.of(999999, unit);

      assertTrue(least.compareTo(middle) < 0);
      assertTrue(greatest.compareTo(middle) > 0);
      assertEquals(0, middle.compareTo(middle));

      for (TimeUnit otherUnit : TimeUnit.values()) {
        Duration other = Duration.of(500, otherUnit);
        assertEquals(Long.signum(unit.compareTo(otherUnit)), Long.signum(middle.compareTo(other)));
      }
    }
  }

  @Test public void compareTo_saneOnOverflow() {
    // will overflow or underflow if converted to next finer unit ("smaller" per Enum.compareTo)
    long overflowLength = Long.MAX_VALUE / 20;
    long underflowLength = Long.MIN_VALUE / 20;

    for (TimeUnit unit : TimeUnit.values()) {
      for (TimeUnit smallerUnit : TimeUnit.values()) {
        if (smallerUnit.compareTo(unit) >= 0) {
          // skip comparisons where conversion to common unit won't generate overflow/underflow
          continue;
        }
        Duration d1 = Duration.of(Long.MAX_VALUE, unit);
        Duration d2 = Duration.of(Long.MAX_VALUE, smallerUnit);
        assertTrue(d1.compareTo(d2) > 0);

        d1 = Duration.of(overflowLength, unit);
        assertTrue(d1.compareTo(d2) > 0);


        d1 = Duration.of(Long.MIN_VALUE, unit);
        d2 = Duration.of(Long.MIN_VALUE, smallerUnit);
        assertTrue(d1.compareTo(d2) < 0);

        d1 = Duration.of(underflowLength, unit);
        assertTrue(d1.compareTo(d2) < 0);
      }
    }
  }
}
