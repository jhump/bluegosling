package com.apriori.concurrent;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Represents a duration of time that is backed by an integral length and a unit.
 *
 * <p>
 * When two durations are added or subtracted, the result will convert to the finest common unit
 * that won't result in overflow or underflow. If a result unit is {@link TimeUnit#DAYS} but still
 * overflows or underflows, the result will be clipped to {@link Long#MAX_VALUE} or
 * {@link Long#MIN_VALUE}, which mirrors the behavior of converting between units using
 * {@link TimeUnit#convert(long, TimeUnit)}.
 *
 * <p>
 * This provides a sane way to compare and use durations of varying units. To provide the duration
 * to APIs that accept a {@code long} and a {@code TimeUnit}, simply pass {@code duration.length()}
 * and {@code duration.unit()}.
 *
 * <p>
 * <strong>Note</strong>: this class has a natural ordering that is inconsistent with equals. In
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
    * Represents the scale of converting from one given unit to another. The first index for this
    * array is the ordinal of the "from" unit, and the second index is the ordinal of the "to" unit.
    * The value is the scale for converting such that multiplying times a value effectively converts
    * it from unit to the other. The scale is computed using {@link TimeUnit#convert(long, TimeUnit)
    * from.convert(1, to)}.
    */
   private static final long UNIT_SCALE[][];

   /**
    * Represents the scale of converting from a given unit (indexed by ordinal) to the next unit
    * (the one with the next highest ordinal). So {@code UNIT_SCALE_TO_NEXT[i]} is equivalent to
    * {@code UNIT_SCALE[i][i + 1]} where {@code i} is the ordinal of any unit smaller than
    * {@link TimeUnit#DAYS}.
    */
   private static final long UNIT_SCALE_TO_NEXT[];

   /**
    * The maximum value for a given unit to convert to another given unit. The first index for this
    * array is the ordinal of the "from" unit, and the second index is the ordinal of the "to" unit.
    * The value in the array represents the maximum value allowed for conversion to proceed without
    * overflow.
    */
   private static final long UNIT_CONVERSION_MAX[][];

   /**
    * The minimum value for a given unit to convert to another given unit. The first index for this
    * array is the ordinal of the "from" unit, and the second index is the ordinal of the "to" unit.
    * The value in the array represents the minimum value allowed for conversion to proceed without
    * underflow.
    */
   private static final long UNIT_CONVERSION_MIN[][];

   /**
    * The maximum threshold, in 128 bit values, for coarsening from unit to another. The first index
    * for this array is the ordinal "from" unit, the second index is the ordinal of the "to" unit,
    * and the third index is a 0 or 1 to get the high-order and low-order 64-bits of the value
    * respectively. The value in the array represents the maximum value that can be converted to the
    * target unit without overflow.
    */
   private static final long UNIT_128_TO_64_COARSEN_MAX[][][];

   /**
    * The minimum threshold, in 128 bit values, for coarsening from unit to another. The first index
    * for this array is the ordinal "from" unit, the second index is the ordinal of the "to" unit,
    * and the third index is a 0 or 1 to get the high-order and low-order 64-bits of the value
    * respectively. The value in the array represents the minimum value that can be converted to the
    * target unit without underflow.
    */
   private static final long UNIT_128_TO_64_COARSEN_MIN[][][];

   static {
      // compute maximums for preventing overflow/underflow
      int len = UNITS_BY_ORDINAL.length;
      UNIT_SCALE = new long[len][len];
      UNIT_CONVERSION_MAX = new long[len][len];
      UNIT_CONVERSION_MIN = new long[len][len];
      UNIT_128_TO_64_COARSEN_MAX = new long[len][len][2];
      UNIT_128_TO_64_COARSEN_MIN = new long[len][len][2];
      for (int i = 0; i < len; i++) {
         TimeUnit unitI = UNITS_BY_ORDINAL[i];
         for (int j = 0; j < len; j++) {
            TimeUnit unitJ = UNITS_BY_ORDINAL[j];
            UNIT_SCALE[i][j] = unitI.convert(1, unitJ);
            if (i <= j) {
               UNIT_CONVERSION_MAX[i][j] = Long.MAX_VALUE;
               UNIT_CONVERSION_MIN[i][j] = Long.MIN_VALUE;
               // also compute 128-bit max from which we can coarsen without 64-bit overflow
               long scale = unitI.convert(1, unitJ);
               long lo = Long.MAX_VALUE * scale;
               long hi = int128_multiply64x64High(Long.MAX_VALUE, scale);
               UNIT_128_TO_64_COARSEN_MAX[i][j][0] = hi;
               UNIT_128_TO_64_COARSEN_MAX[i][j][1] = lo;
               lo = Long.MIN_VALUE * scale;
               hi = int128_multiply64x64High(Long.MIN_VALUE, scale);
               UNIT_128_TO_64_COARSEN_MIN[i][j][0] = hi;
               UNIT_128_TO_64_COARSEN_MIN[i][j][1] = lo;
            } else {
               long max = unitI.convert(Long.MAX_VALUE, unitJ);
               assert unitI.convert(Long.MIN_VALUE, unitJ) == -max;
               UNIT_CONVERSION_MAX[i][j] = max;
               long min = unitI.convert(Long.MIN_VALUE, unitJ);
               UNIT_CONVERSION_MIN[i][j] = min;
            }
         }
      }
      // compute the scale of one time unit to the next
      len--; // don't need entry for last unit (days)
      UNIT_SCALE_TO_NEXT = new long[len];
      for (int i = 0; i < len; i++) {
         // example: when i represents nanoseconds, the unit scale will be 1000 since that is the
         // scale from nanos to the next unit, microseconds.
         UNIT_SCALE_TO_NEXT[i] = UNIT_SCALE[i][i + 1];
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
    * Returns the length of this duration in microseconds. This will return {@link Long#MAX_VALUE}
    * or {@link Long#MIN_VALUE} if the conversion overflows or underflows.
    *
    * @return the length of this duration in microseconds
    */
   public long toMicros() {
      return unit.toMicros(length);
   }

   /**
    * Returns the length of this duration in milliseconds. This will return {@link Long#MAX_VALUE}
    * or {@link Long#MIN_VALUE} if the conversion overflows or underflows.
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
      return desiredUnit.convert(length, unit);
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
    * Returns the sum of this duration and the specified duration. This is equivalent to:
    * 
    * <pre>
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
      if (length == 0) {
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
    * <p>
    * If the result would be greater than {@link Long#MAX_VALUE} days then the result is clipped to
    * {@link Long#MAX_VALUE}. Similarly, if the result would be less than {@link Long#MIN_VALUE}
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
            while (other > UNIT_CONVERSION_MAX[sourceOrdinal][ordinal]
                  || other < UNIT_CONVERSION_MIN[sourceOrdinal][ordinal]) {
               ordinal++;
            }
         } else {
            ordinal = otherUnit.ordinal();
            int sourceOrdinal = unit.ordinal();
            while (length > UNIT_CONVERSION_MAX[sourceOrdinal][ordinal]
                  || length < UNIT_CONVERSION_MIN[sourceOrdinal][ordinal]) {
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
            long scale = UNIT_SCALE_TO_NEXT[ordinal];
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
    * 
    * <pre>
    * this.plus(-duration.length(), duration.unit())
    * </pre>
    * 
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
    * 
    * <pre>
    * this.plus(-other, otherUnit)
    * </pre>
    * 
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
   public Duration times(long multiplicand) {
      if (length == 0 || multiplicand == 1) {
         return this;
      } else if (multiplicand == 0 || length == 1) {
         return of(multiplicand, unit);
      }
      TimeUnit resultUnit = unit;
      long len = length;
      long product = len * multiplicand;
      // we have to do something more sophisticated if we overflow 64 bits
      int nlz1 = Long.numberOfLeadingZeros(len) + Long.numberOfLeadingZeros(~len);
      int nlz2 = Long.numberOfLeadingZeros(multiplicand) + Long.numberOfLeadingZeros(~multiplicand);
      product = len * multiplicand;
      if (nlz1 + nlz2 < 66) {
         // may have overflowed
         long productHighBits = int128_multiply64x64High(len, multiplicand);
         if (!int128_fitsIn64(productHighBits, product)) {
            /*
             * We overflowed! We now scale the 128-bit product down to a coarser unit to fit the
             * answer into 64-bits.
             * 
             * We could use BigInteger to do this work. But it is slower since its algorithms must
             * support arbitrary precision. Also, BigInteger is immutable, so each calculation must
             * allocate a new BigInteger. Instead, we just keep two longs to represent the 128-bit
             * product and then use specialized techniques to compute the full-width product and to
             * divide by 64-bit divisors. This allows us to compute an accurate duration in the
             * event of overflow and do so very efficiently.
             * 
             * We do use BigInteger in the tests, to confirm that our multi-word arithmetic
             * computes the correct results.
             */
            
            // find the target unit to which we can coarsen the result without overflow
            int sourceOrdinal = unit.ordinal();
            int targetOrdinal = -1;
            for (int ordinal = sourceOrdinal + 1, last = UNITS_BY_ORDINAL.length; ordinal < last;
                  ordinal++) {
               long[] max = UNIT_128_TO_64_COARSEN_MAX[sourceOrdinal][ordinal];
               long[] min = UNIT_128_TO_64_COARSEN_MIN[sourceOrdinal][ordinal];
               if (int128_compare(productHighBits, product, max[0], max[1]) <= 0
                     && int128_compare(productHighBits, product, min[0], min[1]) >= 0) {
                  // wont' overflow/underflow
                  targetOrdinal = ordinal;
                  break;
               }
            }
            
            if (targetOrdinal == -1) {
               // product overflows even coarsened to days, so clamp
               product = int128_saturatedCast(productHighBits, product);
               resultUnit = TimeUnit.DAYS;
               
            } else {
               // convert to target unit by dividing out the scale factor
               resultUnit = UNITS_BY_ORDINAL[targetOrdinal];
               long divisor = UNIT_SCALE[sourceOrdinal][targetOrdinal];
               assert divisor > 0;
               
               boolean negative = Long.signum(productHighBits) < 0;
               if (negative) {
                  // get absolute value via 2's complement negate
                  productHighBits = ~productHighBits;
                  product = ~product;
                  if (product == -1) {
                     productHighBits++;
                  }
                  product++;
               }
               // divide 128 bits by 64 bit divisor
               if (productHighBits < divisor) {
                  product = int128_udivideBy64(productHighBits, product, divisor);
                  productHighBits = 0;
               } else {
                  product = int128_udivideBy64(productHighBits % divisor, product, divisor);
                  productHighBits = productHighBits / divisor;
               }
               // restore sign bit
               if (negative) {
                  productHighBits = ~productHighBits;
                  product = ~product;
                  if (product == -1) {
                     productHighBits++;
                  }
                  product++;
               }
               assert int128_fitsIn64(productHighBits, product);
            }
         }
      }
      return of(product, resultUnit);
   }
   
   public static void main(String args[]) {
      long lo = Long.MIN_VALUE * Long.MIN_VALUE;
      long hi = int128_multiply64x64High(Long.MIN_VALUE, Long.MIN_VALUE);
      System.out.println(Long.toHexString(Long.MIN_VALUE) + " * " + Long.toHexString(Long.MIN_VALUE)
            + " = " + Long.toHexString(hi) + " " + Long.toHexString(lo));

      
      
      Random rnd = new Random();
      long[] required = new long[] { 1, -1, 0, 5, 100, Integer.MAX_VALUE, Integer.MIN_VALUE,
            Long.MAX_VALUE, Long.MIN_VALUE };
      long[] operands = new long[100 + required.length];
      System.arraycopy(required, 0, operands, 0, required.length);
      for (int i = 0; i < 100; i++) {
         operands[i + required.length] = rnd.nextLong(); 
      }

      long[] product = new long[2];
      for (long l1 : operands) {
         for (long l2 : operands) {
            product[1] = l1 * l2;
            product[0] = int128_multiply64x64High(l1, l2);
            BigInteger expectedResult = BigInteger.valueOf(l1).multiply(BigInteger.valueOf(l2));
            
            long[] product2 = new long[2];
            product2[0] = expectedResult.shiftRight(64).longValueExact();
            product2[1] = expectedResult.longValue();
            
            if (product[0] != product2[0] || product[1] != product2[1]) {
               System.err.printf("%x * %x should be %x %x but was %x %x%n", l1, l2,
                     product2[0], product2[1], product[0], product[1]);
            }
         }
      }
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
   public Duration dividedBy(long divisor) {
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
         if (len > UNIT_CONVERSION_MAX[ordinal][ordinal - 1]
               || len < UNIT_CONVERSION_MIN[ordinal][ordinal - 1]) {
            // converting to finer unit would overflow/underflow
            break;
         }
         len *= UNIT_SCALE_TO_NEXT[--ordinal];
      }
      return of(len / divisor, UNITS_BY_ORDINAL[ordinal]);
   }

   /**
    * Returns a value less than zero, equal to zero, or greater than zero as this duration is
    * shorter than, equal to, or longer than the specified duration.
    *
    * @param o another duration
    * @return {@inheritDoc}
    */
   @Override
   public int compareTo(Duration o) {
      int c = unit.compareTo(o.unit);
      if (c == 0) {
         return Long.compare(length, o.length);
      } else if (c <= 0) {
         long theirs = unit.convert(o.length, o.unit);
         if (theirs == Long.MAX_VALUE) {
            // other value must be larger
            return -1;
         } else if (theirs == Long.MIN_VALUE) {
            // other value must be smaller
            return 1;
         } else {
            return Long.compare(length, theirs);
         }
      } else {
         long ours = o.unit.convert(length, unit);
         if (ours == Long.MAX_VALUE) {
            // this value must be larger
            return 1;
         } else if (ours == Long.MIN_VALUE) {
            // this value must be smaller
            return -1;
         } else {
            return Long.compare(ours, o.length);
         }
      }
   }

   /**
    * Returns the greater of two given durations.
    *
    * @param d1 a duration
    * @param d2 another duration
    * @return whichever given duration is larger, or the first one if they are equal
    */
   public static Duration greaterOf(Duration d1, Duration d2) {
      return d1.compareTo(d2) >= 0 ? d1 : d2;
   }

   /**
    * Returns the greatest of the given durations.
    *
    * @param durations one or more durations
    * @return whichever given duration is largest; the earliest such value if more than one has the
    *         same greatest value
    * @throws IllegalArgumentException if the given array is empty
    */
   public static Duration greatestOf(Duration... durations) {
      int len = durations.length;
      if (len == 0) {
         throw new IllegalArgumentException();
      }
      Duration max = durations[0];
      for (int i = 1; i < len; i++) {
         Duration d = durations[i];
         if (max.compareTo(d) < 0) {
            max = d;
         }
      }
      return max;
   }

   /**
    * Returns the greatest of the given durations.
    *
    * @param durations one or more durations
    * @return whichever given duration is largest; the earliest such value if more than one has the
    *         same greatest value
    * @throws IllegalArgumentException if the given iterable is empty
    */
   public static Duration greatestOf(Iterable<Duration> durations) {
      Iterator<Duration> iter = durations.iterator();
      if (!iter.hasNext()) {
         throw new IllegalArgumentException();
      }
      Duration max = iter.next();
      while (iter.hasNext()) {
         Duration d = iter.next();
         if (max.compareTo(d) < 0) {
            max = d;
         }
      }
      return max;
   }

   /**
    * Returns the lesser of two given durations.
    *
    * @param d1 a duration
    * @param d2 another duration
    * @return whichever given duration is smaller, or the first one if they are equal
    */
   public static Duration lesserOf(Duration d1, Duration d2) {
      return d1.compareTo(d2) <= 0 ? d1 : d2;
   }

   /**
    * Returns the least of the given durations.
    *
    * @param durations one or more durations
    * @return whichever given duration is smallest; the earliest such value if more than one has the
    *         same least value
    * @throws IllegalArgumentException if the given array is empty
    */
   public static Duration leastOf(Duration... durations) {
      int len = durations.length;
      if (len == 0) {
         throw new IllegalArgumentException();
      }
      Duration min = durations[0];
      for (int i = 1; i < len; i++) {
         Duration d = durations[i];
         if (min.compareTo(d) > 0) {
            min = d;
         }
      }
      return min;
   }

   /**
    * Returns the least of the given durations.
    *
    * @param durations one or more durations
    * @return whichever given duration is smallest; the earliest such value if more than one has the
    *         same least value
    * @throws IllegalArgumentException if the given iterable is empty
    */
   public static Duration leastOf(Iterable<Duration> durations) {
      Iterator<Duration> iter = durations.iterator();
      if (!iter.hasNext()) {
         throw new IllegalArgumentException();
      }
      Duration min = iter.next();
      while (iter.hasNext()) {
         Duration d = iter.next();
         if (min.compareTo(d) > 0) {
            min = d;
         }
      }
      return min;
   }

   @Override
   public String toString() {
      return length + " " + unit().name().toLowerCase();
   }

   @Override
   public int hashCode() {
      return Long.hashCode(length) ^ unit.hashCode();
   }

   /**
    * Returns true if the specified object is a {@link Duration} with the same length and unit.
    */
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Duration)) {
         return false;
      }
      Duration other = (Duration) o;
      return length == other.length && unit == other.unit;
   }
   
   /**
    * Casts the given 128-bit value to a 64-bit long. If the value cannot be represented in just
    * 64-bits then this will clamp the value to {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}.
    *
    * @param hi the high-order 64-bits of the given value
    * @param lo the low-order 64-bits of the given value
    * @return the given 128-bit value as a 64-bit integer, saturating (instead of overflowing) if
    *       the value cannot be represented in 64 bits
    */
   static long int128_saturatedCast(long hi, long lo) {
      if (hi > 0 || (hi == 0 && lo < 0)) {
         return Long.MAX_VALUE;
      } else if (hi < -1 || (hi == -1 && lo > 0)) {
         return Long.MIN_VALUE;
      } else {
         return lo;
      }
   }

   /**
    * Divides a 128-bit unsigned integer by a 64-bit unsigned divisor and produces the low-order
    * 64-bits of the quotient.
    *
    * @param u1 high-order 64 bits of the dividend
    * @param u0 low-order 64 bits of the dividend
    * @param v the divisor
    * @return low-order 64 bits of the quotient
    */
   static long int128_udivideBy64(long u1, long u0, long v) {
      long b = 1l << 32;
      long un1, un0, vn1, vn0, q1, q0, un32, un21, un10, rhat, left, right;
      int s;

      s = Long.numberOfLeadingZeros(v);
      v <<= s;
      vn1 = v >>> 32;
      vn0 = v & 0xffffffffL;

      if (s > 0) {
         un32 = (u1 << s) | (u0 >>> (64 - s));
         un10 = u0 << s;
      } else {
         un32 = u1;
         un10 = u0;
      }

      un1 = un10 >>> 32;
      un0 = un10 & 0xffffffffL;

      q1 = Long.divideUnsigned(un32, vn1);
      rhat = Long.remainderUnsigned(un32, vn1);

      left = q1 * vn0;
      right = (rhat << 32) + un1;

      while (true) {
         if (Long.compareUnsigned(q1, b) >= 0 || Long.compareUnsigned(left, right) > 0) {
            --q1;
            rhat += vn1;
            if (Long.compareUnsigned(rhat, b) < 0) {
               left -= vn0;
               right = (rhat << 32) | un1;
               continue;
            }
         }
         break;
      }

      un21 = (un32 << 32) + (un1 - (q1 * v));

      q0 = Long.divideUnsigned(un21, vn1);
      rhat = Long.remainderUnsigned(un21, vn1);

      left = q0 * vn0;
      right = (rhat << 32) | un0;
      while (true) {
         if (Long.compareUnsigned(q0, b) >= 0 || Long.compareUnsigned(left, right) > 0) {
            --q0;
            rhat += vn1;
            if (Long.compareUnsigned(rhat, b) < 0) {
               left -= vn0;
               right = (rhat << 32) | un0;
               continue;
            }
         }
         break;
      }

      return (q1 << 32) | q0;
   }
   
   /**
    * Computes the high-order 64-bits of the 128-bit product of multiplying the given two 64-bit
    * signed integers.
    *
    * @param x a 64-bit signed integer
    * @param y a 64-bit signed integer
    * @return the high-order 64-bits of the 128-bit signed integer product of the given values
    */
   static long int128_multiply64x64High(long x, long y) {
      long u0 = x >>> 32;
      long u1 = x & 0xffffffffl;
      long v0 = y >>> 32;
      long v1 = y & 0xffffffffl;

      long w0 = u1 * v1;
      long w1 = u1 * v0;
      long w2 = u0 * v1;
      long w3 = u0 * v0;
      
      long c = ((w0 >>> 32) + (w1 & 0xffffffffl) + (w2 & 0xffffffffl)) >>> 32;
      long hi = w3 + (w2 >>> 32) + (w1 >>> 32) + c;
      
      return hi - ((x < 0) ? y : 0) - ((y < 0) ? x : 0);
   }
   
   /**
    * Determines if a 128-bit signed integer can be represented in just 64 bits.
    *
    * @param hi the high-order 64 bits of the 128-bit number
    * @param lo the low-order 64 bits of the 128-bit number
    * @return true if the given value can be represented in just 64 bits
    */
   static boolean int128_fitsIn64(long hi, long lo) {
      return (hi == 0 && lo > 0) || (hi == -1 && lo < 0);
   }
   
   /**
    * Compares the two given signed 128-bit values and returns less than zero, zero, or greater than
    * zero as the first value is less than, equal to, or greater than the second value.
    *
    * @param hi1 the high-order 64 bits of the first value
    * @param lo1 the low-order 64 bits of the first value
    * @param hi2 the high-order 64 bits of the second value
    * @param lo2 the low-order 64 bits of the second value
    * @return a value less than, equal to, or greater than zero as the first given value is less
    *       than, equal to, or greater than the second given value
    */
   static int int128_compare(long hi1, long lo1, long hi2, long lo2) {
      int c = Long.compare(hi1, hi2);
      return c == 0 ? Long.compareUnsigned(lo1, lo2) : c;
   }
}
