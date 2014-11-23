package com.apriori.util;


/**
 * A simple variable reference for an {@code short}. This is the primitive specialization of
 * {@link Variable} for {@code short}.
 * 
 * <p>This class is <strong>not</strong> thread-safe. This class is very similar to the boxed type
 * {@link Short} except that it is mutable.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class VariableShort extends Number implements Cloneable {
   private static final long serialVersionUID = -8948943243479719520L;
   
   private short value;
   
   /**
    * Creates a new variable whose value is zero.
    */
   public VariableShort() {
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public VariableShort(short value) {
      this.value = value;
   }

   /**
    * Gets this variable's current value.
    */
   public short get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public short getAndSet(short v) {
      short ret = this.value;
      this.value = v;
      return ret;
   }
   
   /**
    * Sets this variable's value.
    * 
    * @param v the new value
    */
   public void set(short v) {
      this.value = v;
   }
   
   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public short updateAndGet(ShortUnaryOperator fn) {
      return this.value = fn.applyAsShort(this.value);
   }

   /**
    * Updates the variable using the given function. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's previous value
    */
   public short getAndUpdate(ShortUnaryOperator fn) {
      short ret = this.value;
      this.value = fn.applyAsShort(ret);
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
   public short accumulateAndGet(short v, ShortBinaryOperator fn) {
      return this.value = fn.applyAsShort(this.value, v);
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
   public short getAndAccumulate(short v, ShortBinaryOperator fn) {
      short ret = this.value;
      this.value = fn.applyAsShort(ret, v);
      return ret;
   }
   
   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @return the variable's new value
    */
   public short incrementAndGet() {
      return ++this.value;
   }

   /**
    * Increments the variable by one. After this method returns, the variable's value is its
    * previous value plus one.
    *
    * @return the variable's previous value
    */
   public short getAndIncrement() {
      return this.value++;
   }
   
   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @return the variable's new value
    */
   public short decrementAndGet() {
      return --this.value;
   }

   /**
    * Decrements the variable by one. After this method returns, the variable's value is its
    * previous value minus one.
    *
    * @return the variable's previous value
    */
   public short getAndDecrement() {
      return this.value--;
   }
   
   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @return the variable's new value
    */
   public short addAndGet(short a) {
      return this.value += a;
   }

   /**
    * Adds the given amount to the variable. After this method returns, the variable's value is its
    * previous value plus the specified one.
    *
    * @param a the other addend
    * @return the variable's previous value
    */
   public short getAndAdd(short a) {
      short ret = this.value;
      this.value += a;
      return ret;
   }
   
   @Override
   public byte byteValue() {
      return (byte) value;
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
   public VariableShort clone() {
      try {
         return (VariableShort) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof VariableShort && this.value == ((VariableShort) o).value;
   }
   
   @Override
   public int hashCode() {
      return Short.hashCode(value);
   }
   
   @Override
   public String toString() {
      return Short.toString(value);
   }
}
