package com.bluegosling.concurrent.contended;

import com.bluegosling.concurrent.unsafe.UnsafeUtils;
import com.bluegosling.util.IsDerivedFrom;

import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * Like an {@link AtomicReference}, except uses padding to reduce cache contention (aka
 * false-sharing) with writes to nearby memory locations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@IsDerivedFrom(AtomicReference.class)
public class ContendedReference<V> extends LhsPaddedReference<V> {
   // RHS padding
   long p9, p10, p11, p12, p13, p14, p15;

   private static final Unsafe unsafe = UnsafeUtils.getUnsafe();
   private static final long valueOffset;

   static {
      try {
         valueOffset = unsafe.objectFieldOffset(LhsPaddedReference.class.getDeclaredField("value"));
      } catch (Exception ex) {
         throw new Error(ex);
      }
   }

   /**
    * Creates a new {@link ContendedReference} with the given initial value.
    *
    * @param initialValue the initial value
    */
   public ContendedReference(V initialValue) {
      value = initialValue;
   }

   /**
    * Creates a new {@link ContendedReference} with null initial value.
    */
   public ContendedReference() {
   }

   /**
    * Gets the current value.
    *
    * @return the current value
    */
   public final V get() {
      return value;
   }

   /**
    * Sets to the given value.
    *
    * @param newValue the new value
    */
   public final void set(V newValue) {
      value = newValue;
   }

   /**
    * Eventually sets to the given value.
    *
    * @param newValue the new value
    * @since 1.6
    */
   public final void lazySet(V newValue) {
      unsafe.putOrderedObject(this, valueOffset, newValue);
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
   public final boolean compareAndSet(V expect, V update) {
      return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
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
   public final boolean weakCompareAndSet(V expect, V update) {
      return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
   }

   /**
    * Atomically sets to the given value and returns the old value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   @SuppressWarnings("unchecked")
   public final V getAndSet(V newValue) {
      return (V) unsafe.getAndSetObject(this, valueOffset, newValue);
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
   public final V getAndUpdate(UnaryOperator<V> updateFunction) {
      V prev, next;
      do {
         prev = get();
         next = updateFunction.apply(prev);
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
   public final V updateAndGet(UnaryOperator<V> updateFunction) {
      V prev, next;
      do {
         prev = get();
         next = updateFunction.apply(prev);
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
   public final V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction) {
      V prev, next;
      do {
         prev = get();
         next = accumulatorFunction.apply(prev, x);
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
   public final V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction) {
      V prev, next;
      do {
         prev = get();
         next = accumulatorFunction.apply(prev, x);
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
      return String.valueOf(get());
   }
}
