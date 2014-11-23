package com.apriori.util;

/**
 * A simple variable reference for an {@code byte}. This is the primitive specialization of
 * {@link Variable} for {@code byte}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. This class is very similar to the boxed type
 * {@link Byte} except that it is mutable.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableByte extends Number implements Cloneable {
   private static final long serialVersionUID = -9061697113254534341L;
   
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
   public byte getAndSet(byte v) {
      byte ret = this.value;
      this.value = v;
      return ret;
   }

   /**
    * Sets this variable's value.
    * 
    * @param value the new value
    */
   public void set(byte v) {
      this.value = v;
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public byte updateAndGet(ByteUnaryOperator fn) {
      return this.value = fn.applyAsByte(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public byte getAndUpdate(ByteUnaryOperator fn) {
      byte ret = this.value;
      this.value = fn.applyAsByte(ret);
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
   public byte accumulateAndGet(byte v, ByteBinaryOperator fn) {
      return this.value = fn.applyAsByte(this.value, v);
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
   public byte getAndAccumulate(byte v, ByteBinaryOperator fn) {
      byte ret = this.value;
      this.value = fn.applyAsByte(ret, v);
      return ret;
   }
   
   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public byte incrementAndGet() {
      return ++this.value;
   }

   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public byte getAndIncrement() {
      return this.value++;
   }
   
   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public byte decrementAndGet() {
      return --this.value;
   }

   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public byte getAndDecrement() {
      return this.value--;
   }
   
   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @param fn the function to apply
    * @return the variable's new value
    */
   public byte addAndGet(byte a) {
      return this.value += a;
   }

   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public byte getAndAdd(byte a) {
      byte ret = this.value;
      this.value += a;
      return ret;
   }
   
   @Override
   public byte byteValue() {
      return value;
   }

   @Override
   public short shortValue() {
      return value;
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
   public VariableByte clone() {
      try {
         return (VariableByte) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof VariableByte && this.value == ((VariableByte) o).value;
   }
   
   @Override
   public int hashCode() {
      return Byte.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Byte.toString(value);
   }
}
