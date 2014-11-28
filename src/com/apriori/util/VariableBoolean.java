package com.apriori.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple variable reference for a {@code boolean}. This is the primitive specialization of
 * {@link Variable} for {@code boolean}.
 * 
 * <p>This class provides nearly the same API as {@link AtomicBoolean}. However, this version is
 * <strong>not</strong> thread-safe. If the variable is being accessed from multiple threads, use an
 * {@link AtomicBoolean} instead.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableBoolean implements Serializable, Cloneable {
   private static final long serialVersionUID = -6625609901250555921L;
   
   private boolean value;
   
   /**
    * Creates a new variable whose value is false.
    */
   public VariableBoolean() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableBoolean(boolean value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public boolean get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public boolean getAndSet(boolean v) {
      boolean ret = this.value;
      this.value = v;
      return ret;
   }
   
   /**
    * Sets this variable's value.
    *
    * @param value the new value
    */
   public void set(boolean value) {
      this.value = value;
   }
   
   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public boolean updateAndGet(BooleanUnaryOperator fn) {
      return this.value = fn.applyAsBoolean(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public boolean getAndUpdate(BooleanUnaryOperator fn) {
      boolean ret = this.value;
      this.value = fn.applyAsBoolean(ret);
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
   public boolean accumulateAndGet(boolean v, BooleanBinaryOperator fn) {
      return this.value = fn.applyAsBoolean(this.value, v);
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
   public boolean getAndAccumulate(boolean v, BooleanBinaryOperator fn) {
      boolean ret = this.value;
      this.value = fn.applyAsBoolean(ret, v);
      return ret;
   }

   /**
    * Creates a copy of this variable. The returned instance has the same value as this.
    */
   @Override
   public VariableBoolean clone() {
      try {
         return (VariableBoolean) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableBoolean && this.value == ((VariableBoolean) o).value;
   }
   
   @Override
   public int hashCode() {
      return Boolean.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Boolean.toString(value);
   }
}
