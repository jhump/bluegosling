package com.apriori.concurrent.atoms;

import com.apriori.collections.HamtPersistentSet;
import com.apriori.collections.Immutables;
import com.apriori.collections.PersistentSet;
import com.apriori.util.Predicate;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract base class for {@link Atom} implementations. Sub-classes must implement the
 * {@link #get()} method.
 *
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractAtom<T> implements Atom<T> {

   /**
    * A thread-safe set of watchers.
    */
   private final AtomicReference<PersistentSet<Watcher<? super T>>> watchers =
         new AtomicReference<PersistentSet<Watcher<? super T>>>(
               new HamtPersistentSet<Watcher<? super T>>());

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
      for (Watcher<? super T> watcher : Immutables.asIfMutable(watchers.get())) {
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
      while (true) {
         PersistentSet<Watcher<? super T>> oldSet = watchers.get();
         if (oldSet.contains(watcher)) {
            return false;
         }
         PersistentSet<Watcher<? super T>> newSet = oldSet.add(watcher);
         if (watchers.compareAndSet(oldSet, newSet)) {
            return true;
         }
      }
   }

   @Override
   public boolean removeWatcher(Watcher<? super T> watcher) {
      while (true) {
         PersistentSet<Watcher<? super T>> oldSet = watchers.get();
         if (!oldSet.contains(watcher)) {
            return false;
         }
         PersistentSet<Watcher<? super T>> newSet = oldSet.remove(watcher);
         if (watchers.compareAndSet(oldSet, newSet)) {
            return true;
         }
      }
   }
}
