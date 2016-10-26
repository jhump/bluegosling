package com.bluegosling.concurrent.contended;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import com.bluegosling.util.IsDerivedFrom;

/**
 * Like an {@link AtomicBoolean}, except uses padding to reduce cache contention (aka false-sharing)
 * with writes to nearby memory locations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@IsDerivedFrom(AtomicBoolean.class)
public class ContendedBoolean extends LhsPaddedInteger {
   // RHS padding
   long p9, p10, p11, p12, p13, p14, p15;

   private static final AtomicIntegerFieldUpdater<LhsPaddedInteger> valueUpdater =
         AtomicIntegerFieldUpdater.newUpdater(LhsPaddedInteger.class, "value");

   /**
    * Creates a new {@link ContendedBoolean} with the given initial value.
    *
    * @param initialValue the initial value
    */
   public ContendedBoolean(boolean initialValue) {
      value = initialValue ? 1 : 0;
   }

   /**
    * Creates a new {@link ContendedBoolean} with initial value {@code false}.
    */
   public ContendedBoolean() {
   }

   /**
    * Returns the current value.
    *
    * @return the current value
    */
   public final boolean get() {
      return value != 0;
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
   public final boolean compareAndSet(boolean expect, boolean update) {
      int e = expect ? 1 : 0;
      int u = update ? 1 : 0;
      return valueUpdater.compareAndSet(this, e, u);
   }

   /**
    * Atomically sets the value to the given updated value if the current value {@code ==} the
    * expected value.
    *
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful
    * 
    * @see AtomicBoolean#weakCompareAndSet(boolean, boolean)
    */
   public boolean weakCompareAndSet(boolean expect, boolean update) {
      int e = expect ? 1 : 0;
      int u = update ? 1 : 0;
      return valueUpdater.weakCompareAndSet(this, e, u);
   }

   /**
    * Unconditionally sets to the given value.
    *
    * @param newValue the new value
    */
   public final void set(boolean newValue) {
      value = newValue ? 1 : 0;
   }

   /**
    * Eventually sets to the given value.
    *
    * @param newValue the new value
    */
   public final void lazySet(boolean newValue) {
      int v = newValue ? 1 : 0;
      valueUpdater.lazySet(this, v);
   }

   /**
    * Atomically sets to the given value and returns the previous value.
    *
    * @param newValue the new value
    * @return the previous value
    */
   public final boolean getAndSet(boolean newValue) {
      int v = newValue ? 1 : 0;
      int o = valueUpdater.getAndSet(this, v);
      return o != 0;
   }

   /**
    * Returns the String representation of the current value.
    * 
    * @return the String representation of the current value
    */
   @Override
   public String toString() {
      return Boolean.toString(get());
   }
}
