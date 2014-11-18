package com.apriori.util;

import com.apriori.concurrent.atoms.Atom;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
public class Variable<T> {
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
    * @param value the new value
    * @return the variable's previous value
    */
   public T set(T value) {
      T ret = this.value;
      this.value = value;
      return ret;
   }

   /**
    * Clears this variable's value, setting it to {@code null}.
    */
   public void clear() {
      set(null);
   }

   /**
    * Applies the given function to the variable. After this method returns, the variable's value
    * is the result of applying the function to the variable's previous value.
    *
    * @param fn the function to apply
    * @return the variable's new value
    */
   public T apply(Function<? super T, ? extends T> fn) {
      return this.value = fn.apply(this.value);
   }
}
