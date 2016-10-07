package com.bluegosling.concurrent.contended;

import com.bluegosling.util.IsDerivedFrom;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
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

   private static final AtomicLongFieldUpdater<LhsPaddedLong> valueUpdater =
         AtomicLongFieldUpdater.newUpdater(LhsPaddedLong.class, "value");

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
    */
   public final void lazySet(long newValue) {
      valueUpdater.lazySet(this, newValue);
   }

   /**
    * Atomically sets to the given value and returns the old value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   public final long getAndSet(long newValue) {
      return valueUpdater.getAndSet(this, newValue);
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
      return valueUpdater.compareAndSet(this, expect, update);
   }

   /**
    * Atomically sets the value to the given updated value if the current value {@code ==} the
    * expected value.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful
    * 
    * @see AtomicLong#weakCompareAndSet(long, long)
    */
   public final boolean weakCompareAndSet(long expect, long update) {
      return valueUpdater.weakCompareAndSet(this, expect, update);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the previous value
    */
   public final long getAndIncrement() {
      return valueUpdater.getAndIncrement(this);
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the previous value
    */
   public final long getAndDecrement() {
      return valueUpdater.getAndDecrement(this);
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the previous value
    */
   public final long getAndAdd(long delta) {
      return valueUpdater.getAndAdd(this, delta);
   }

   /**
    * Atomically increments by one the current value.
    *
    * @return the updated value
    */
   public final long incrementAndGet() {
      return valueUpdater.incrementAndGet(this);
   }

   /**
    * Atomically decrements by one the current value.
    *
    * @return the updated value
    */
   public final long decrementAndGet() {
      return valueUpdater.decrementAndGet(this);
   }

   /**
    * Atomically adds the given value to the current value.
    *
    * @param delta the value to add
    * @return the updated value
    */
   public final long addAndGet(long delta) {
      return valueUpdater.addAndGet(this, delta);
   }

   /**
    * Atomically updates the current value with the results of applying the given function,
    * returning the previous value. The function should be side-effect-free, since it may be
    * re-applied when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the previous value
    */
   public final long getAndUpdate(LongUnaryOperator updateFunction) {
      return valueUpdater.getAndUpdate(this, updateFunction);
   }

   /**
    * Atomically updates the current value with the results of applying the given function,
    * returning the updated value. The function should be side-effect-free, since it may be
    * re-applied when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the updated value
    */
   public final long updateAndGet(LongUnaryOperator updateFunction) {
      return valueUpdater.updateAndGet(this, updateFunction);
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
    */
   public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
      return valueUpdater.getAndAccumulate(this, x, accumulatorFunction);
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
    */
   public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
      return valueUpdater.accumulateAndGet(this, x, accumulatorFunction);
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
