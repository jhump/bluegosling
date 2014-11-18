package com.apriori.util;

/**
 * A simple variable reference for an {@code char}. This is effectively a primitive specialization
 * of {@link Variable}.
 * 
 * <p>This class is <strong>not</strong> thread-safe.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableChar {
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
    * @param value the new value
    * @return the variable's previous value
    */
   public char set(char value) {
      char ret = this.value;
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
   public char apply(CharUnaryOperator fn) {
      return this.value = fn.applyAsChar(this.value);
   }
}
