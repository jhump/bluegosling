package com.apriori.concurrent.atoms;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * An atom with a volatile value. Atomic updates are made using atomic compare-and-set operations
 * on the value. 
 *
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public class SimpleAtom<T> extends AbstractAtom<T> implements SynchronousAtom<T> {

   /**
    * The object used to update the value atomically.
    */
   @SuppressWarnings("rawtypes") // cannot use type args with class token
   private static final AtomicReferenceFieldUpdater<SimpleAtom, Object> updater =
         AtomicReferenceFieldUpdater.newUpdater(SimpleAtom.class, Object.class, "value");
   
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
   public T apply(Function<? super T, ? extends T> function) {
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
      return newValue;
   }
}
