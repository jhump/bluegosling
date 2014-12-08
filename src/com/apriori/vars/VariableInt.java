package com.apriori.vars;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * A simple variable reference for an {@code int}. This is the primitive specialization of
 * {@link Variable} for {@code int}.
 * 
 * <p>This class provides nearly the same API as {@link AtomicInteger}. However, this version is
 * <strong>not</strong> thread-safe. If the variable is being accessed from multiple threads, use an
 * {@link AtomicInteger} instead.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableInt extends Number implements Cloneable {
   private static final long serialVersionUID = 4022947442065262818L;
   
   private int value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableInt() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableInt(int value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public int get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public int getAndSet(int v) {
      int ret = this.value;
      this.value = v;
      return ret;
   }

   /**
    * Sets this variable's value.
    * 
    * @param v the new value
    */
   public void set(int v) {
      this.value = v;
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public int updateAndGet(IntUnaryOperator fn) {
      return this.value = fn.applyAsInt(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public int getAndUpdate(IntUnaryOperator fn) {
      int ret = this.value;
      this.value = fn.applyAsInt(ret);
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
   public int accumulateAndGet(int v, IntBinaryOperator fn) {
      return this.value = fn.applyAsInt(this.value, v);
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
   public int getAndAccumulate(int v, IntBinaryOperator fn) {
      int ret = this.value;
      this.value = fn.applyAsInt(ret, v);
      return ret;
   }
   
   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @return the variable's new value
    */
   public int incrementAndGet() {
      return ++this.value;
   }

   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @return the variable's previous value
    */
   public int getAndIncrement() {
      return this.value++;
   }
   
   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @return the variable's new value
    */
   public int decrementAndGet() {
      return --this.value;
   }

   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @return the variable's previous value
    */
   public int getAndDecrement() {
      return this.value--;
   }
   
   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @return the variable's new value
    */
   public int addAndGet(int a) {
      return this.value += a;
   }

   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @return the variable's previous value
    */
   public int getAndAdd(int a) {
      int ret = this.value;
      this.value += a;
      return ret;
   }

   @Override
   public int intValue() {
      return value;
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
   public VariableInt clone() {
      try {
         return (VariableInt) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableInt && this.value == ((VariableInt) o).value;
   }
   
   @Override
   public int hashCode() {
      return Integer.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Integer.toString(value);
   }
}
