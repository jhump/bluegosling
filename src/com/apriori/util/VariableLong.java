package com.apriori.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

/**
 * A simple variable reference for an {@code long}. This is effectively a primitive specialization
 * of {@link Variable}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. If the variable is being accessed from
 * multiple threads, use an {@link AtomicLong}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableLong {
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
   public long set(long value) {
      long ret = this.value;
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
   public long apply(LongUnaryOperator fn) {
      return this.value = fn.applyAsLong(this.value);
   }
}
