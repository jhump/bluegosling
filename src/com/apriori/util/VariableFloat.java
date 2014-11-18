package com.apriori.util;

/**
 * A simple variable reference for an {@code float}. This is effectively a primitive specialization
 * of {@link Variable}.
 * 
 * <p>This class is <strong>not</strong> thread-safe.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableFloat {
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
    * @param value the new value
    * @return the variable's previous value
    */
   public float set(float value) {
      float ret = this.value;
      this.value = value;
      return ret;
   }
   
   /**
    * Applies the given function to the variable. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public float apply(FloatUnaryOperator fn) {
      return this.value = fn.applyAsFloat(this.value);
   }
}
