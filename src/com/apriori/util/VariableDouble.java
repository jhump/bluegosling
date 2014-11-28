package com.apriori.util;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * A simple variable reference for a {@code double}. This is the primitive specialization of
 * {@link Variable} for {@code double}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. This class is very similar to the boxed type
 * {@link Double} except that it is mutable.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableDouble extends Number implements Cloneable {
   private static final long serialVersionUID = 5600611853384563871L;
   
   private double value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableDouble() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableDouble(double value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public double get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public double getAndSet(double v) {
      double ret = this.value;
      this.value = v;
      return ret;
   }

   /**
    * Sets this variable's value.
    * 
    * @param v the new value
    */
   public void set(double v) {
      this.value = v;
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public double updateAndGet(DoubleUnaryOperator fn) {
      return this.value = fn.applyAsDouble(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public double getAndUpdate(DoubleUnaryOperator fn) {
      double ret = this.value;
      this.value = fn.applyAsDouble(ret);
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
   public double accumulateAndGet(double v, DoubleBinaryOperator fn) {
      return this.value = fn.applyAsDouble(this.value, v);
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
   public double getAndAccumulate(double v, DoubleBinaryOperator fn) {
      double ret = this.value;
      this.value = fn.applyAsDouble(ret, v);
      return ret;
   }

   @Override
   public int intValue() {
      return (int) value;
   }

   @Override
   public long longValue() {
      return (long) value;
   }

   @Override
   public float floatValue() {
      return (float) value;
   }

   @Override
   public double doubleValue() {
      return value;
   }
   
   /**
    * Creates a copy of this variable. The returned instance has the same value as this.
    */
   @Override
   public VariableDouble clone() {
      try {
         return (VariableDouble) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableDouble && this.value == ((VariableDouble) o).value;
   }
   
   @Override
   public int hashCode() {
      return Double.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Double.toString(value);
   }
}
