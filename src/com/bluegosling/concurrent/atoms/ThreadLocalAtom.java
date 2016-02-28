package com.bluegosling.concurrent.atoms;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An atom whose values are in thread-local storage.  This varies a bit from other atoms in that
 * one thread <em>cannot</em> see changes made to the atom by other threads. For that reason, the
 * normal "watcher" behavior does not make sense. Instead, watchers of thread-local atoms will only
 * be notified when the root value changes. The root value is the one used to seed the atom's
 * value for new threads.
 *
 * @param <T> the type of the atom's value
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ThreadLocalAtom<T> extends AbstractSynchronousAtom<T> {
   
   /** 
    * The root value, used to seed the atom's value in new threads.
    */
   private final AtomicReference<T> rootValue;
   
   /**
    * The atom's thread-local value.
    */
   private final ThreadLocal<T> value = new ThreadLocal<T>() {
      @Override protected T initialValue() {
         return getRootValue();
      }
   };

   /**
    * Constructs a new thread-local atom with a {@code null} value and no validator.
    */
   public ThreadLocalAtom() {
      rootValue = new AtomicReference<T>(null);
   }

   /**
    * Constructs a new thread-local atom with the specified value and no validator.
    */
   public ThreadLocalAtom(T rootValue) {
      this.rootValue = new AtomicReference<T>(rootValue);
   }

   /**
    * Constructs a new thread-local atom with the specified value and the specified validator.
    */
   public ThreadLocalAtom(T rootValue, Predicate<? super T> validator) {
      super(validator);
      validate(rootValue);
      this.rootValue = new AtomicReference<T>(rootValue);
   }

   @Override
   public T get() {
      return value.get();
   }
   
   @Override
   public T set(T newValue) {
      validate(newValue);
      T oldValue = value.get();
      value.set(newValue);
      return oldValue;
   }

   @Override
   T update(Function<? super T, ? extends T> function, boolean returnNew) {
      T oldValue = value.get();
      T newValue = function.apply(oldValue);
      validate(newValue);
      value.set(newValue);
      return returnNew ? newValue : oldValue;
   }
   
   /**
    * Returns the atom's root value, used to seed the thread-local value for new threads.
    *
    * @return the atom's root value
    */
   public T getRootValue() {
      return rootValue.get();
   }
   
   /**
    * Sets the atom's root value. Watchers will be notified of this change and passed the specified
    * new value as well as the previous root value.
    *
    * @param newRootValue the new root value
    * @return the previous root value
    */
   public T setRootValue(T newRootValue) {
      validate(newRootValue);
      T oldRootValue = rootValue.getAndSet(newRootValue);
      notify(oldRootValue, newRootValue);
      return oldRootValue;
   }
}
