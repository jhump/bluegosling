package com.bluegosling.concurrent.contended;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import com.bluegosling.util.IsDerivedFrom;

/**
 * Like an {@link AtomicReference}, except uses padding to reduce cache contention (aka
 * false-sharing) with writes to nearby memory locations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@IsDerivedFrom(AtomicReference.class)
@SuppressWarnings("unchecked") // must cast from Object to V for atomic access
public class ContendedReference<V> extends LhsPaddedReference<V> {
   // RHS padding
   long p9, p10, p11, p12, p13, p14, p15;

   @SuppressWarnings("rawtypes")
   private static final AtomicReferenceFieldUpdater<LhsPaddedReference, Object> valueUpdater =
         AtomicReferenceFieldUpdater.newUpdater(LhsPaddedReference.class, Object.class, "value");

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
    */
   public final void lazySet(V newValue) {
      valueUpdater.lazySet(this, newValue);
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
    * @see AtomicReference#weakCompareAndSet(Object, Object)
    */
   public final boolean weakCompareAndSet(V expect, V update) {
      return valueUpdater.weakCompareAndSet(this, expect, update);
   }

   /**
    * Atomically sets to the given value and returns the old value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   public final V getAndSet(V newValue) {
      return (V) valueUpdater.getAndSet(this, newValue);
   }

   /**
    * Atomically updates the current value with the results of applying the given function,
    * returning the previous value. The function should be side-effect-free, since it may be
    * re-applied when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the previous value
    */
   public final V getAndUpdate(UnaryOperator<V> updateFunction) {
      return (V) valueUpdater.getAndUpdate(this, (UnaryOperator<Object>) updateFunction);
   }

   /**
    * Atomically updates the current value with the results of applying the given function,
    * returning the updated value. The function should be side-effect-free, since it may be
    * re-applied when attempted updates fail due to contention among threads.
    *
    * @param updateFunction a side-effect-free function
    * @return the updated value
    */
   public final V updateAndGet(UnaryOperator<V> updateFunction) {
      return (V) valueUpdater.updateAndGet(this, (UnaryOperator<Object>) updateFunction);
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
   public final V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction) {
      return (V) valueUpdater.getAndAccumulate(this, x,
            (BinaryOperator<Object>) accumulatorFunction);
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
   public final V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction) {
      return (V) valueUpdater.accumulateAndGet(this, x,
            (BinaryOperator<Object>) accumulatorFunction);
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
