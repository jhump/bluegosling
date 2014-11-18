package com.apriori.util;

/**
 * A simple variable reference for an {@code byte}. This is effectively a primitive specialization
 * of {@link Variable}.
 * 
 * <p>This class is <strong>not</strong> thread-safe.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableByte {
   private byte value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableByte() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableByte(byte value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public byte get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param value the new value
    * @return the variable's previous value
    */
   public byte set(byte value) {
      byte ret = this.value;
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
   public byte apply(ByteUnaryOperator fn) {
      return this.value = fn.applyAsByte(this.value);
   }
}
