package com.bluegosling.concurrent.contended;

import com.bluegosling.concurrent.unsafe.UnsafeUtils;
import com.bluegosling.util.IsDerivedFrom;

import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Like an {@link AtomicLong}, except uses padding to reduce cache contention (aka false-sharing)
 * with writes to nearby memory locations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@IsDerivedFrom(AtomicLong.class)
public class ContendedLong extends LhsPaddedLong {
   // RHS padding
   long p9, p10, p11, p12, p13, p14, p15;

   // setup to use Unsafe.compareAndSwapLong for updates
   private static final Unsafe unsafe = UnsafeUtils.getUnsafe();
   private static final long valueOffset;

   static {
      try {
         valueOffset = unsafe.objectFieldOffset(LhsPaddedLong.class.getDeclaredField("value"));
      } catch (Exception ex) {
         throw new Error(ex);
      }
   }

   /**
    * Creates a new {@link ContendedLong} with the given initial value.
    *
    * @param initialValue the initial value
    */
   public ContendedLong(long initialValue) {
      value = initialValue;
   }

   /**
    * Creates a new {@link ContendedLong} with initial value {@code 0}.
    */
   public ContendedLong() {
   }

   /**
    * Gets the current value.
    *
    * @return the current value
    */
   public final long get() {
      return value;
   }

   /**
    * Sets to the given value.
    *
    * @param newValue the new value
    */
   public final void set(long newValue) {
      value = newValue;
   }

   /**
    * Eventually sets to the given value.
    *
    * @param newValue the new value
    * @since 1.6
    */
   public final void lazySet(long newValue) {
      unsafe.putOrderedLong(this, valueOffset, newValue);
   }

   /**
    * Atomically sets to the given value and returns the old value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   public final long getAndSet(long newValue) {
      return unsafe.getAndSetLong(this, valueOffset, newValue);
   }

   /**
    * Atomically sets the value to the given updated value if the current value {@code ==} the
    * expected value.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful. False return indicates that the actual value was not equal
    *         to the expected value.
    */
   public final boolean compareAndSet(long expect, long update) {
      return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
   }

   /**
    * Atomically sets the value to the given updated value if the current value {@code ==} the
    * expected value.
    *
    * <p>
    * <a href="package-summary.html#weakCompareAndSet">May fail spuriously and does not provide
    * ordering guarantees</a>, so is only rarely an appropriate alternative to {@code compareAndSet}.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful
    */
   public final boolean weakCompareAndSet(long expect, long update) {
      return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the previous value
    */
   public final long getAndIncrement() {
      return unsafe.getAndAddLong(this, valueOffset, 1L);
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the previous value
    */
   public final long getAndDecrement() {
      return unsafe.getAndAddLong(this, valueOffset, -1L);
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the previous value
    */
   public final long getAndAdd(long delta) {
      return unsafe.getAndAddLong(this, valueOffset, delta);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the updated value
    */
   public final long incrementAndGet() {
      return unsafe.getAndAddLong(this, valueOffset, 1L) + 1L;
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the updated value
    */
   public final long decrementAndGet() {
      return unsafe.getAndAddLong(this, valueOffset, -1L) - 1L;
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the updated value
    */
   public final long addAndGet(long delta) {
      return unsafe.getAndAddLong(this, valueOffset, delta) + delta;
   }

   /**
    * Atomically updates the current value with the results of applying the given function,
    * returning the previous value. The function should be side-effect-free, since it may be
    * re-applied when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the previous value
    * @since 1.8
    */
   public final long getAndUpdate(LongUnaryOperator updateFunction) {
      long prev, next;
      do {
         prev = get();
         next = updateFunction.applyAsLong(prev);
      } while (!compareAndSet(prev, next));
      return prev;
   }

   /**
    * Atomically updates the current value with the results of applying the given function,
    * returning the updated value. The function should be side-effect-free, since it may be
    * re-applied when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the updated value
    * @since 1.8
    */
   public final long updateAndGet(LongUnaryOperator updateFunction) {
      long prev, next;
      do {
         prev = get();
         next = updateFunction.applyAsLong(prev);
      } while (!compareAndSet(prev, next));
      return next;
   }

   /**
    * Atomically updates the current value with the results of applying the given function to the
    * current and given values, returning the previous value. The function should be
    * side-effect-free, since it may be re-applied when attempted updates fail due to contention
    * among threads. The function is applied with the current value as its first argument, and the
    * given update as the second argument.
    *
    * @param x the update value
    * @param accumulatorFunction a side-effect-free function of two arguments
    * @return the previous value
    * @since 1.8
    */
   public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
      long prev, next;
      do {
         prev = get();
         next = accumulatorFunction.applyAsLong(prev, x);
      } while (!compareAndSet(prev, next));
      return prev;
   }

   /**
    * Atomically updates the current value with the results of applying the given function to the
    * current and given values, returning the updated value. The function should be
    * side-effect-free, since it may be re-applied when attempted updates fail due to contention
    * among threads. The function is applied with the current value as its first argument, and the
    * given update as the second argument.
    *
    * @param x the update value
    * @param accumulatorFunction a side-effect-free function of two arguments
    * @return the updated value
    * @since 1.8
    */
   public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
      long prev, next;
      do {
         prev = get();
         next = accumulatorFunction.applyAsLong(prev, x);
      } while (!compareAndSet(prev, next));
      return next;
   }

   /**
    * Returns the String representation of the current value.
    * 
    * @return the String representation of the current value
    */
   @Override
   public String toString() {
      return Long.toString(get());
   }
}
