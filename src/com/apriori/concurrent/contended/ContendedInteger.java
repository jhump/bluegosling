package com.apriori.concurrent.contended;

import com.apriori.concurrent.unsafe.UnsafeUtils;
import com.apriori.util.IsDerivedFrom;

import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;


/**
 * Like an {@link AtomicInteger}, except uses padding to reduce cache contention (aka false-sharing)
 * with writes to nearby memory locations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@IsDerivedFrom(AtomicInteger.class)
public class ContendedInteger extends LhsPaddedInteger {
   // RHS padding
   long p9, p10, p11, p12, p13, p14, p15;

   // setup to use Unsafe.compareAndSwapInt for updates
   private static final Unsafe unsafe = UnsafeUtils.getUnsafe();
   private static final long valueOffset;

   static {
      try {
         valueOffset = unsafe.objectFieldOffset(LhsPaddedInteger.class.getDeclaredField("value"));
      } catch (Exception ex) {
         throw new Error(ex);
      }
   }

   /**
    * Creates a new {@link ContendedInteger} with the given initial value.
    *
    * @param initialValue the initial value
    */
   public ContendedInteger(int initialValue) {
      value = initialValue;
   }

   /**
    * Creates a new {@link ContendedInteger} with initial value {@code 0}.
    */
   public ContendedInteger() {
   }

   /**
    * Gets the current value.
    *
    * @return the current value
    */
   public final int get() {
      return value;
   }

   /**
    * Sets to the given value.
    *
    * @param newValue the new value
    */
   public final void set(int newValue) {
      value = newValue;
   }

   /**
    * Eventually sets to the given value.
    *
    * @param newValue the new value
    * @since 1.6
    */
   public final void lazySet(int newValue) {
      unsafe.putOrderedInt(this, valueOffset, newValue);
   }

   /**
    * Atomically sets to the given value and returns the old value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   public final int getAndSet(int newValue) {
      return unsafe.getAndSetInt(this, valueOffset, newValue);
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
   public final boolean compareAndSet(int expect, int update) {
      return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
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
   public final boolean weakCompareAndSet(int expect, int update) {
      return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the previous value
    */
   public final int getAndIncrement() {
      return unsafe.getAndAddInt(this, valueOffset, 1);
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the previous value
    */
   public final int getAndDecrement() {
      return unsafe.getAndAddInt(this, valueOffset, -1);
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the previous value
    */
   public final int getAndAdd(int delta) {
      return unsafe.getAndAddInt(this, valueOffset, delta);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the updated value
    */
   public final int incrementAndGet() {
      return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the updated value
    */
   public final int decrementAndGet() {
      return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the updated value
    */
   public final int addAndGet(int delta) {
      return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
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
   public final int getAndUpdate(IntUnaryOperator updateFunction) {
      int prev, next;
      do {
         prev = get();
         next = updateFunction.applyAsInt(prev);
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
   public final int updateAndGet(IntUnaryOperator updateFunction) {
      int prev, next;
      do {
         prev = get();
         next = updateFunction.applyAsInt(prev);
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
   public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
      int prev, next;
      do {
         prev = get();
         next = accumulatorFunction.applyAsInt(prev, x);
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
   public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
      int prev, next;
      do {
         prev = get();
         next = accumulatorFunction.applyAsInt(prev, x);
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
      return Integer.toString(get());
   }
}
