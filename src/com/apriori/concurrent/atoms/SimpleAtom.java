package com.apriori.concurrent.atoms;

import com.apriori.concurrent.unsafe.UnsafeReferenceFieldUpdater;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An atom with a volatile value. Atomic updates are made using atomic compare-and-set operations
 * on the value. 
 *
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SimpleAtom<T> extends AbstractSynchronousAtom<T> {

   /**
    * The object used to update the value atomically.
    */
   @SuppressWarnings("rawtypes") // cannot use type args with class token
   private static final UnsafeReferenceFieldUpdater<SimpleAtom, Object> updater =
         new UnsafeReferenceFieldUpdater<>(SimpleAtom.class, Object.class, "value");
   
   /**
    * The atom's value.
    */
   private volatile T value;
   
   /**
    * Constructs a new atom with a {@code null} value and no validator.
    */
   public SimpleAtom() {
   }
   
   /**
    * Constructs a new atom with the specified value and no validator.
    */
   public SimpleAtom(T value) {
      this.value = value;
   }

   /**
    * Constructs a new atom with the specified value and the specified validator.
    */
   public SimpleAtom(T value, Predicate<? super T> validator) {
      super(validator);
      validate(value);
      this.value = value;
   }

   @Override
   public T get() {
      return value;
   }

   @Override
   public T set(T newValue) {
      validate(newValue);
      @SuppressWarnings("unchecked") // compiler checks type during construction and on mutation
      T oldValue = (T) updater.getAndSet(this, newValue);
      notify(oldValue, newValue);
      return oldValue;
   }

   @Override
   T update(Function<? super T, ? extends T> function, boolean returnNew) {
      T oldValue;
      T newValue;
      while (true) {
         oldValue = value;
         newValue = function.apply(oldValue);
         validate(newValue);
         if (updater.compareAndSet(this, oldValue, newValue)) {
            break;
         }
      }
      notify(oldValue, newValue);
      return returnNew ? newValue : oldValue;
   }
}
