package com.apriori.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

/**
 * Represents a duration of time that is backed by an integral length and a unit.
 *
 * <p>When two durations are added or subtracted, the result will convert to the finest common unit
 * that won't result in overflow or underflow. If a result unit is {@link TimeUnit#DAYS} but still
 * overflows or underflows, the result will be clipped to {@link Long#MAX_VALUE} or
 * {@link Long#MIN_VALUE}, which mirrors the behavior of converting between units using
 * {@link TimeUnit#convert(long, TimeUnit)}.
 *
 * <p>This provides a sane way to compare and use durations of varying units. To provide the
 * duration to APIs that accept a {@code long} and a {@code TimeUnit}, simply pass
 * {@code duration.length()} and {@code duration.unit()}.
 *
 * <p><strong>Note</strong>: this class has a natural ordering that is inconsistent with equals. In
 * particular, two objects that represent the same amount of time are "equal" according to their
 * natural ordering but unequal according to the {@code equals} method if they have different units.
 * For example, a duration 2 seconds and one of 2,000 milliseconds are unequal but considered equal
 * per the natural ordering.
 */
public class Duration implements Comparable<Duration> {

  /**
   * Save a copy of this array instead of invoking TimeUnit.values() repeatedly (since it must
   * create and return defensive copy).
   */
  private static final TimeUnit UNITS_BY_ORDINAL[] = TimeUnit.values();

  /**
   * The maximum value for a given unit to convert to another given unit. The first index for this
   * array is the ordinal of the "from" unit, and the second index is the ordinal of the "to" unit.
   * The value in the array represents the maximum absolute value allowed for conversion to proceed
   * without overflow or underflow.
   */
  private static final long UNIT_MAX_BY_ORDINAL[][];
  
  /**
   * Represents the scale of converting from a given unit (indexed by ordinal) to the next unit (the
   * one with the next highest ordinal).
   */
  private static final long UNIT_SCALE_BY_ORDINAL[];

  static {
    // compute maximums for preventing overflow/underflow
    int len = UNITS_BY_ORDINAL.length;
    UNIT_MAX_BY_ORDINAL = new long[len][len];
    for (int i = 0; i < len; i++) {
      TimeUnit unitI = UNITS_BY_ORDINAL[i];
      for (int j = 0; j < len; j++) {
        if (i <= j) {
          UNIT_MAX_BY_ORDINAL[i][j] = Long.MAX_VALUE;
        } else {
          TimeUnit unitJ = UNITS_BY_ORDINAL[j];
          long max = unitI.convert(Long.MAX_VALUE, unitJ);
          assert unitI.convert(Long.MIN_VALUE, unitJ) == -max;
          UNIT_MAX_BY_ORDINAL[i][j] = max;
        }
      }
    }
    // compute the scale of one time unit to the next
    len--; // don't need entry for last unit (days)
    UNIT_SCALE_BY_ORDINAL = new long[len];
    for (int i = 0; i < len; i++) {
      // example: when i represents nanoseconds, the unit scale will be 1000 since that is the scale
      // from nanos to the next unit, microseconds.
      UNIT_SCALE_BY_ORDINAL[i] = UNITS_BY_ORDINAL[i].convert(1, UNITS_BY_ORDINAL[i + 1]);
    }
  }

  /**
   * Constructs a new duration of the specified length and unit.
   *
   * @param length the length of the duration
   * @param unit the unit of the duration
   * @return a new duration object
   */
  public static Duration of(long length, TimeUnit unit) {
     return new Duration(length, requireNonNull(unit));
  }

  /** Returns a new duration whose length is the specified number of nanoseconds. */
  public static Duration nanos(long nanos) {
    return new Duration(nanos, TimeUnit.NANOSECONDS);
  }

  /** Returns a new duration whose length is the specified number of microseconds. */
  public static Duration micros(long micros) {
    return new Duration(micros, TimeUnit.MICROSECONDS);
  }

  /** Returns a new duration whose length is the specified number of milliseconds. */
  public static Duration millis(long millis) {
    return new Duration(millis, TimeUnit.MILLISECONDS);
  }

  /** Returns a new duration whose length is the specified number of seconds. */
  public static Duration seconds(long seconds) {
    return new Duration(seconds, TimeUnit.SECONDS);
  }

  /** Returns a new duration whose length is the specified number of minutes. */
  public static Duration minutes(long minutes) {
    return new Duration(minutes, TimeUnit.MINUTES);
  }

  /** Returns a new duration whose length is the specified number of hours. */
  public static Duration hours(long hours) {
    return new Duration(hours, TimeUnit.HOURS);
  }

  /** Returns a new duration whose length is the specified number of days. */
  public static Duration days(long days) {
    return new Duration(days, TimeUnit.DAYS);
  }

  private final long length;
  private final TimeUnit unit;

  private Duration(long length, TimeUnit unit) {
    this.length = length;
    this.unit = unit;
  }

  /**
   * Returns the length of this duration in nanoseconds. This will return {@link Long#MAX_VALUE} or
   * {@link Long#MIN_VALUE} if the conversion overflows or underflows.
   *
   * @return the length of this duration in nanoseconds
   */
  public long toNanos() {
    return unit.toNanos(length);
  }

  /**
   * Returns the length of this duration in microseconds. This will return {@link Long#MAX_VALUE} or
   * {@link Long#MIN_VALUE} if the conversion overflows or underflows.
   *
   * @return the length of this duration in microseconds
   */
  public long toMicros() {
    return unit.toMicros(length);
  }

  /**
   * Returns the length of this duration in milliseconds. This will return {@link Long#MAX_VALUE} or
   * {@link Long#MIN_VALUE} if the conversion overflows or underflows.
   *
   * @return the length of this duration in milliseconds
   */
  public long toMillis() {
    return unit.toMillis(length);
  }

  /**
   * Returns the length of this duration in seconds. This will return {@link Long#MAX_VALUE} or
   * {@link Long#MIN_VALUE} if the conversion overflows or underflows.
   *
   * @return the length of this duration in seconds
   */
  public long toSeconds() {
    return unit.toSeconds(length);
  }

  /**
   * Returns the length of this duration in minutes. This will return {@link Long#MAX_VALUE} or
   * {@link Long#MIN_VALUE} if the conversion overflows or underflows.
   *
   * @return the length of this duration in minutes
   */
  public long toMinutes() {
    return unit.toMinutes(length);
  }

  /**
   * Returns the length of this duration in hours. This will return {@link Long#MAX_VALUE} or
   * {@link Long#MIN_VALUE} if the conversion overflows or underflows.
   *
   * @return the length of this duration in hours
   */
  public long toHours() {
    return unit.toHours(length);
  }

  /**
   * Returns the length of this duration in days.
   *
   * @return the length of this duration in hours
   */
  public long toDays() {
    return unit.toDays(length);
  }

  /**
   * Returns the length of this duration in the specified unit.
   *
   * @param desiredUnit the unit to which the length is converted
   * @return the length of this duration in the specified unit
   */
  public long to(TimeUnit desiredUnit) {
    return desiredUnit.convert(length, this.unit);
  }

  /**
   * Returns the length of this duration, in this object's {@link #unit()}.
   *
   * @return the length, in units of {@link #unit()}
   */
  public long length() {
    return length;
  }

  /**
   * Returns the unit of this object's {@link #length}.
   *
   * @return the unit of {@link #length}
   */
  public TimeUnit unit() {
    return unit;
  }

  /**
   * Returns the sum of this duration and the specified duration. This is equivalent to:<pre>
   * this.plus(duration.length(), duration.unit())
   * </pre>
   *
   * @param duration the other addend
   * @return the sum of {@code this + duration}
   *
   * @see #plus(long, TimeUnit)
   */
  public Duration plus(Duration duration) {
    // don't create a new object for the result needlessly
    if (this.length == 0) {
      return duration;
    }
    return plus(duration.length(), duration.unit());
  }

  /**
   * Returns the sum of this duration and the specified duration. The unit of the returned sum will
   * be the finer of this or the specified duration's {@link #unit()} unless that would result in
   * overflow or underflow. In such case, a coarser unit is chosen, the finest unit that can
   * represent the sum without overflow or underflow.
   *
   * <p>If the result would be greater than {@link Long#MAX_VALUE} days then the result is clipped
   * to {@link Long#MAX_VALUE}. Similarly, if the result would be less than {@link Long#MIN_VALUE}
   * days, the result is clipped to {@link Long#MIN_VALUE}.
   *
   * @param other the other addend
   * @param otherUnit the unit of the other addend
   * @return the sum of this duration plus the one specified
   */
  public Duration plus(long other, TimeUnit otherUnit) {
    if (other == 0) {
      return this;
    }
    // find the common conversion unit to use
    long a, b;
    TimeUnit resultUnit;
    int unitCompare = unit.compareTo(otherUnit);
    if (unitCompare == 0) {
      resultUnit = unit;
      a = length;
      b = other;
    } else {
       int ordinal;
       if (unitCompare < 0) {
          ordinal = unit.ordinal();
          int sourceOrdinal = otherUnit.ordinal();
          while (Math.abs(other) > UNIT_MAX_BY_ORDINAL[sourceOrdinal][ordinal]) {
             ordinal++;
          }
       } else {
          ordinal = otherUnit.ordinal();
          int sourceOrdinal = unit.ordinal();
          while (Math.abs(length) > UNIT_MAX_BY_ORDINAL[sourceOrdinal][ordinal]) {
             ordinal++;
          }
       }
       resultUnit = UNITS_BY_ORDINAL[ordinal];
       a = resultUnit.convert(length, unit);
       b = resultUnit.convert(other, otherUnit);
    }
    // compute sum
    long r = a + b;
    
    if (((a ^ r) & (b ^ r)) < 0) {
      // overflow! use a coarser unit to prevent this
      if (resultUnit == TimeUnit.DAYS) {
        // doesn't get any coarser than day; just clip value
        r = r < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
      } else {
        int ordinal = resultUnit.ordinal();
        long scale = UNIT_SCALE_BY_ORDINAL[ordinal];
        long err = (a % scale) + (b % scale);
        a /= scale;
        b /= scale;
        long absErr = Math.abs(err);
        if ((a == 0 || b == 0) && absErr < scale) {
          // one addend is too small to represent in coarser unit, so just clip value
          r = r < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        } else {
          if (absErr >= scale) {
            // account for error introduced by integer division truncation
            a += Long.signum(err);
          }
          resultUnit = UNITS_BY_ORDINAL[ordinal + 1];
          r = a + b;
        }
      }
    }
    return of(r, resultUnit);
  }

  /**
   * Returns the difference of this duration minus the specified duration. This is logically
   * equivalent to the following:
   * <pre>
   * this.plus(-duration.length(), duration.unit())
   * </pre>
   * This method's implementation varies from the sample code above to properly handle without
   * overflow the case where the given duration's length is {@link Long#MIN_VALUE}.
   *
   * @param duration the subtrahend
   * @return the difference of {@code this - duration}
   *
   * @see #minus(long, TimeUnit)
   * @see #plus(long, TimeUnit)
   */
  public Duration minus(Duration duration) {
    return minus(duration.length(), duration.unit());
  }

  /**
   * Returns the difference of this duration minus the specified duration. This is logically
   * equivalent to the following:
   * <pre>
   * this.plus(-other, otherUnit)
   * </pre>
   * This method's implementation varies from the sample code above to properly handle without
   * overflow the case where the given subtrahend is {@link Long#MIN_VALUE}.
   *
   * @param other the subtrahend
   * @param otherUnit the unit of the subtrahend
   * @return the difference of this duration minus the one specified
   *
   * @see #plus(long, TimeUnit)
   */
  public Duration minus(long other, TimeUnit otherUnit) {
    if (other == Long.MIN_VALUE) {
      // pathological case: can't negate subtrahend without overflow
      return plus(Long.MAX_VALUE, otherUnit).plus(1, otherUnit);
    }
    return plus(-other, otherUnit);
  }
  
  /**
   * Returns this duration multiplied by the specified number of times. The unit of the returned
   * duration will be the same as this duration except on overflow/underflow, in which case it will
   * be a more coarse unit. If the product is larger than {@link Long#MAX_VALUE} days or less than
   * {@link Long#MIN_VALUE} days, it will be clipped.
   *
   * @param multiplicand the multiplicand
   * @return the product of this duration times the specified multiplicand
   */
  public Duration times(int multiplicand) {
     if (length == 0 || multiplicand == 1) {
        return this; 
     } else if (multiplicand == 0 || length == 1) {
        return of(multiplicand, unit);
     }
     
     int nlz1 = Long.numberOfLeadingZeros(multiplicand) + Long.numberOfLeadingZeros(~multiplicand);
     long len = length;
     TimeUnit resultUnit = unit;
     long product;
     while (true) {
        product = len * multiplicand;
        int nlz = nlz1 + Long.numberOfLeadingZeros(len) + Long.numberOfLeadingZeros(~len);
        // break if product didn't overflow/underflow
        if (nlz > 34) {
           break;
        } else if (nlz == 33) {
           if ((len ^ multiplicand ^ product) >= 0) {
              break;
           }
        } else if (nlz == 32) {
           if ((multiplicand >= 0 || len != Long.MIN_VALUE)
                 && (len >= 0 || multiplicand != Long.MIN_VALUE)
                 && (product / len == multiplicand)) {
              break;
           }
        }
        // overflow! use coarser unit
        if (resultUnit == TimeUnit.DAYS) {
           // can't get any more coarse; just clip
           if (Long.signum(len) * Long.signum(multiplicand) < 0) {
              product = Long.MIN_VALUE;
           } else {
              product = Long.MAX_VALUE;
           }
           break;
        }
        int ordinal = resultUnit.ordinal();
        len /= UNIT_SCALE_BY_ORDINAL[ordinal];
        resultUnit = UNITS_BY_ORDINAL[ordinal + 1];
     }
     return of(product, resultUnit);
  }
  
  /**
   * Returns this duration divided by the specified number. The unit of the returned duration will
   * be either the same as this duration or a more precise unit. A finer unit may be used if the
   * length of this duration is not evenly divisible by the specified divisor. If using the more
   * precise unit would result in overflow/underflow then the more coarse unit is kept and any
   * remainder is discarded.
   *
   * @param divisor the divisor
   * @return the quotient of this duration divided by the specified divisor
   * 
   * @throws ArithmeticException if the specified divisor is zero
   */
  public Duration dividedBy(int divisor) {
     if (divisor == 0) {
        throw new ArithmeticException("div/0");
     }
     if (length == Long.MIN_VALUE && divisor == -1) {
        // this is the only edge case where division can overflow
        return of(Long.MAX_VALUE, unit);
     }
     long len = length;
     int ordinal = unit.ordinal();
     while (ordinal != 0) {
        if (len % divisor == 0) {
           // divides evenly: no need to try finer unit
           break;
        }
        if (len > UNIT_MAX_BY_ORDINAL[ordinal][ordinal - 1]) {
           // converting to finer unit would overflow
           break;
        }
        len *= UNIT_SCALE_BY_ORDINAL[--ordinal];
     }
     return of(len / divisor, UNITS_BY_ORDINAL[ordinal]);
  }

  /**
   * Returns a value less than zero, equal to zero, or greater than zero as this duration is shorter
   * than, equal to, or longer than the specified duration.
   *
   * @param o another duration
   * @return {@inheritDoc}
   */
  @Override public int compareTo(Duration o) {
    long ours, theirs;
    if (unit.compareTo(o.unit) <= 0) {
      ours = length;
      theirs = unit.convert(o.length, o.unit);
    } else {
      ours = o.unit.convert(length, unit);
      theirs = o.length;
    }
    if ((ours == Long.MAX_VALUE || ours == Long.MIN_VALUE) && ours == theirs && unit != o.unit) {
      // overflow from converting to smaller unit; try comparing at coarser unit
      if (unit.compareTo(o.unit) > 0) {
        ours = length;
        theirs = unit.convert(o.length, o.unit);
      } else {
        ours = o.unit.convert(length, unit);
        theirs = o.length;
      }
    }
    return ours < theirs ? -1 : (ours > theirs ? 1 : 0);
  }

  @Override public String toString() {
    return length + " " + unit().name().toLowerCase();
  }

  @Override public int hashCode() {
    return Long.hashCode(length) ^ unit.hashCode();
  }

  /**
   * Returns true if the specified object is a {@link Duration} with the same length and unit.
   */
  @Override public boolean equals(Object o) {
    if (!(o instanceof Duration)) {
      return false;
    }
    Duration other = (Duration) o;
    return length == other.length && unit == other.unit;
  }
}
