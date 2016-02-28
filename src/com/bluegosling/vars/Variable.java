package com.bluegosling.vars;

import com.bluegosling.concurrent.atoms.Atom;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * A simple variable reference. This can be useful for simulating "out" parameters to a function or
 * for simulating non-final values being mutated from within an anonymous class or lambda. This is
 * lighter-weight than using a single element array as the container and provides an API that is
 * easier to read.
 * 
 * <p>In many respects, this is similar to an {@link Atom} except that it is <strong>not</strong>
 * thread-safe. If the variable is being accessed from multiple threads, use an {@link Atom} or an
 * {@link AtomicReference}.
 *
 * @param <T> the type value held by this variable
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class Variable<T> implements Serializable, Cloneable {
   private static final long serialVersionUID = 7879572538527054255L;
   
   private T value;

   /**
    * Creates a new variable whose value is null.
    */
   public Variable() { 
   }

   /**
    * Creates a new variable with the given value.
    *
    * @param value the variable's initial value
    */
   public Variable(T value) {
      this.value = value;
   }
   
   /**
    * Gets this variable's current value.
    */
   public T get() {
      return value;
   }
   
   /**
    * Sets this variable's value, returning the previously held value.
    * 
    * @param v the new value
    * @return the variable's previous value
    */
   public T getAndSet(T v) {
      T ret = this.value;
      this.value = v;
      return ret;
   }
   
   /**
    * Sets this variable's value.
    *
    * @param value the new value
    */
   public void set(T value) {
      this.value = value;
   }

   /**
    * Accumulates the given value into this variable, using the given function to combine the
    * current value with the given one. This stores the results of the function in this variable,
    * but returns the variable's value from before the function's being applied.
    *
    * @param v a value
    * @param fn the function that combines the current value and the given value
    * @return the variable's value before accumulation
    * 
    * @see #accumulateAndGet(Object, BinaryOperator)
    */
   public T getAndAccumulate(T v, BinaryOperator<T> fn) {
      T ret = this.value;
      this.value = fn.apply(ret, v);
      return ret;
   }
   
   /**
    * Accumulates the given value into this variable, using the given function to combine the
    * current value with the given one. This stores the results of the function in this variable,
    * and returns the variable's new value.
    *
    * @param v a value
    * @param fn the function that combines the current value and the given value
    * @return the variable's new value after accumulation
    * 
    * @see #getAndAccumulate(Object, BinaryOperator)
    */
   public T accumulateAndGet(T v, BinaryOperator<T> fn) {
      return this.value = fn.apply(this.value, v);
   }
   
   /**
    * Updates the variable's current value using the given function. This stores the results of the
    * function in this variable, but returns the variable's value from before the function's being
    * applied.
    *
    * @param fn the function that computes the new value from the current one
    * @return the variable's value before updating
    * 
    * @see #updateAndGet(UnaryOperator)
    */
   public T getAndUpdate(UnaryOperator<T> fn) {
      T ret = this.value;
      this.value = fn.apply(ret);
      return ret;
   }
   
   /**
    * Updates the variable's current value using the given function. This stores the results of the
    * function in this variable, and returns the variable's new value.
    *
    * @param fn the function that computes the new value from the current one
    * @return the variable's new value after updating
    * 
    * @see #getAndUpdate(UnaryOperator)
    */
   public T updateAndGet(UnaryOperator<T> fn) {
      return this.value = fn.apply(this.value);
   }

   /**
    * Clears this variable's value, setting it to {@code null}.
    */
   public void clear() {
      set(null);
   }
   
   /**
    * Creates a shallow copy of this variable. The returned instance refers to the same value as
    * this.
    */
   @SuppressWarnings("unchecked")
   @Override
   public Variable<T> clone() {
      try {
         return (Variable<T>) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e);
      }
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Variable && Objects.equals(this.value, ((Variable<?>) o).value);
   }
   
   @Override
   public int hashCode() {
      return Objects.hashCode(value);
   }
   
   @Override
   public String toString() {
      return String.valueOf(value);
   }
}
