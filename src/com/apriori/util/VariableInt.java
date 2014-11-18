package com.apriori.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * A simple variable reference for an {@code int}. This is effectively a primitive specialization of
 * {@link Variable}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. If the variable is being accessed from
 * multiple threads, use an {@link AtomicInteger}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableInt {
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
    * @param value the new value
    * @return the variable's previous value
    */
   public int set(int value) {
      int ret = this.value;
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
   public int apply(IntUnaryOperator fn) {
      return this.value = fn.applyAsInt(this.value);
   }
}
