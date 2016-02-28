package com.bluegosling.vars;

import com.bluegosling.function.FloatBinaryOperator;
import com.bluegosling.function.FloatUnaryOperator;


/**
 * A simple variable reference for a {@code float}. This is the primitive specialization of
 * {@link Variable} for {@code float}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. This class is very similar to the boxed type
 * {@link Float} except that it is mutable.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableFloat extends Number implements Cloneable {
   private static final long serialVersionUID = -8164641785412683342L;
   
   private float value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableFloat() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableFloat(float value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public float get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public float getAndSet(float v) {
      float ret = this.value;
      this.value = v;
      return ret;
   }
   
   /**
    * Sets this variable's value.
    * 
    * @param v the new value
    */
   public void set(float v) {
      this.value = v;
   }
   
   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public float updateAndGet(FloatUnaryOperator fn) {
      return this.value = fn.applyAsFloat(this.value);
   }
   
   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public float getAndUpdate(FloatUnaryOperator fn) {
      float ret = this.value;
      this.value = fn.applyAsFloat(ret);
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
   public float accumulateAndGet(float v, FloatBinaryOperator fn) {
      return this.value = fn.applyAsFloat(this.value, v);
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
   public float getAndAccumulate(float v, FloatBinaryOperator fn) {
      float ret = this.value;
      this.value = fn.applyAsFloat(ret, v);
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
   public VariableFloat clone() {
      try {
         return (VariableFloat) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableFloat && this.value == ((VariableFloat) o).value;
   }
   
   @Override
   public int hashCode() {
      return Float.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Float.toString(value);
   }
}
