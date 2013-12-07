package com.apriori.concurrent.atoms;

import com.apriori.util.Predicate;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An abstract base class for {@link Atom} implementations. Sub-classes must implement the
 * {@link #get()} method.
 *
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractAtom<T> implements Atom<T> {

   /**
    * A thread-safe set of watchers.
    */
   private final Set<Watcher<? super T>> watchers = new CopyOnWriteArraySet<Watcher<? super T>>();

   /**
    * An optional validator (might be {@code null}).
    */
   private final Predicate<? super T> validator;
   
   /**
    * Constructs a new atom with no validator.
    */
   AbstractAtom() {
      this.validator = null;
   }
   
   /**
    * Constructs a new atom with the specified validator.
    * 
    * @param validator a predicate that determines if a given value is valid for this atom
    */
   AbstractAtom(Predicate<? super T> validator) {
      this.validator = validator;
   }
   
   /**
    * Validates the specified value. If this atom has a validator, it will be invoked and an
    * exception is thrown if the value is not valid (e.g. the validator predicate returns false).
    *
    * @param value the value to validate
    * 
    * @throws IllegalArgumentException if the specified value is invalid
    */
   protected void validate(T value) {
      if (validator != null && !validator.test(value)) {
         throw new IllegalArgumentException("value " + value + " is not valid for this atom");
      }
   }
   
   /**
    * Return the atom's validator, or {@code null} if there is no validator. The validator is a
    * predicate that returns true when invoked on valid values for the item and false otherwise.
    *
    * @return the atom's validator
    */
   public Predicate<? super T> getValidator() {
      return validator;
   }

   /**
    * Notifies the atom's watchers that the value has changed.
    *
    * @param oldValue the atom's old value
    * @param newValue the atom's new value
    */
   protected void notify(T oldValue, T newValue) {
      for (Watcher<? super T> watcher : watchers) {
         notify(watcher, oldValue, newValue);
      }
   }
   
   /**
    * Notifies a single watcher and ignores exceptions that the watcher throws.
    *
    * @param watcher the watcher to notify
    * @param oldValue the atom's old value
    * @param newValue the atom's new value
    */
   private <K> void notify(Watcher<? super T> watcher, T oldValue, T newValue) {
      try {
         watcher.changed(this, oldValue, newValue);
      } catch (Exception e) {
         // TODO: log?
      }
   }
   
   @Override
   public boolean addWatcher(Watcher<? super T> watcher) {
      return watchers.add(watcher);
   }

   @Override
   public boolean removeWatcher(Watcher<? super T> watcher) {
      return watchers.remove(watcher);
   }
}
