package com.apriori.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * A simple variable reference for an {@code long}. This is the primitive specialization of
 * {@link Variable} for {@code long}.
 * 
 * <p>This class provides nearly the same API as {@link AtomicLong}. However, this version is
 * <strong>not</strong> thread-safe. If the variable is being accessed from multiple threads, use an
 * {@link AtomicLong} instead.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableLong extends Number implements Cloneable {
   private static final long serialVersionUID = -8333227033380651229L;
   
   private long value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableLong() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableLong(long value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public long get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param value the new value
    * @return the variable's previous value
    */
   public long getAndSet(long v) {
      long ret = this.value;
      this.value = v;
      return ret;
   }

   /**
    * Sets this variable's value.
    * 
    * @param value the new value
    */
   public void set(long v) {
      this.value = v;
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public long updateAndGet(LongUnaryOperator fn) {
      return this.value = fn.applyAsLong(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public long getAndUpdate(LongUnaryOperator fn) {
      long ret = this.value;
      this.value = fn.applyAsLong(ret);
      return ret;
   }
   
   /**
    * Accumulates the given value into  the variable using the given function. After this method
    * returns, the variable's value is the result of applying the function to the variable's
    * previous value and the specified value.
    *
    * @param v the value to accumulate into the variable
    * @param fn the function to apply
    * @return the variable's new value
    */
   public long accumulateAndGet(long v, LongBinaryOperator fn) {
      return this.value = fn.applyAsLong(this.value, v);
   }

   /**
    * Accumulates the given value into  the variable using the given function. After this method
    * returns, the variable's value is the result of applying the function to the variable's
    * previous value and the specified value.
    *
    * @param v the value to accumulate into the variable
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public long getAndAccumulate(long v, LongBinaryOperator fn) {
      long ret = this.value;
      this.value = fn.applyAsLong(ret, v);
      return ret;
   }
   
   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public long incrementAndGet() {
      return ++this.value;
   }

   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public long getAndIncrement() {
      return this.value++;
   }
   
   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public long decrementAndGet() {
      return --this.value;
   }

   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public long getAndDecrement() {
      return this.value--;
   }
   
   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @param fn the function to apply
    * @return the variable's new value
    */
   public long addAndGet(long a) {
      return this.value += a;
   }

   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public long getAndAdd(long a) {
      long ret = this.value;
      this.value += a;
      return ret;
   }

   @Override
   public byte byteValue() {
      return (byte) value;
   }

   @Override
   public short shortValue() {
      return (short) value;
   }

   @Override
   public int intValue() {
      return (int) value;
   }

   @Override
   public long longValue() {
      return value;
   }

   @Override
   public float floatValue() {
      return value;
   }

   @Override
   public double doubleValue() {
      return value;
   }
   
   /**
    * Creates a copy of this variable. The returned instance has the same value as this.
    */
   @Override
   public VariableLong clone() {
      try {
         return (VariableLong) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableLong && this.value == ((VariableLong) o).value;
   }
   
   @Override
   public int hashCode() {
      return Long.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Long.toString(value);
   }
}
