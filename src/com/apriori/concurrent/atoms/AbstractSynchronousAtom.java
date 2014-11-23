package com.apriori.concurrent.atoms;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An abstract base class for {@link SynchronousAtom} implementations.
 *
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractSynchronousAtom<T> extends AbstractAtom<T> implements SynchronousAtom<T> {

   /**
    * Constructs a new atom with no validator.
    */
   AbstractSynchronousAtom() {
   }
   
   /**
    * Constructs a new atom with the specified validator.
    * 
    * @param validator a predicate that determines if a given value is valid for this atom
    */
   AbstractSynchronousAtom(Predicate<? super T> validator) {
      super(validator);
   }
   
   @Override
   public T updateAndGet(Function<? super T, ? extends T> function) {
      return update(function, true);
   }

   @Override
   public T getAndUpdate(Function<? super T, ? extends T> function) {
      return update(function, false);
   }
   
   /**
    * Updates the atom's value by applying the given function to the atom's current value. Returns
    * either the original or new value, based on the given flag.
    *
    * @param function the function used to update the atom's value
    * @param returnNew indicates the atom's new value should be returned if true; otherwise the
    *       atom's previous value should be returned
    * @return either the original or new value, based on the given flag
    */
   abstract T update(Function<? super T, ? extends T> function, boolean returnNew);
}
