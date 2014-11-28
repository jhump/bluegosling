package com.apriori.util;

import java.io.Serializable;

/**
 * A simple variable reference for a {@code char}. This is the primitive specialization of
 * {@link Variable} for {@code char}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. This class is very similar to the boxed type
 * {@link Character} except that it is mutable.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableChar implements Serializable, Cloneable {
   private static final long serialVersionUID = 4652666838087015320L;
   
   private char value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableChar() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableChar(char value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public char get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public char getAndSet(char v) {
      char ret = this.value;
      this.value = v;
      return ret;
   }
   
   /**
    * Sets this variable's value.
    * 
    * @param v the new value
    */
   public void set(char v) {
      this.value = v;
   }
   
   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public char updateAndGet(CharUnaryOperator fn) {
      return this.value = fn.applyAsChar(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public char getAndUpdate(CharUnaryOperator fn) {
      char ret = this.value;
      this.value = fn.applyAsChar(ret);
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
   public char accumulateAndGet(char v, CharBinaryOperator fn) {
      return this.value = fn.applyAsChar(this.value, v);
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
   public char getAndAccumulate(char v, CharBinaryOperator fn) {
      char ret = this.value;
      this.value = fn.applyAsChar(ret, v);
      return ret;
   }
   
   /**
    * Creates a copy of this variable. The returned instance has the same value as this.
    */
   @Override
   public VariableChar clone() {
      try {
         return (VariableChar) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableChar && this.value == ((VariableChar) o).value;
   }
   
   @Override
   public int hashCode() {
      return Character.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Character.toString(value);
   }
}
