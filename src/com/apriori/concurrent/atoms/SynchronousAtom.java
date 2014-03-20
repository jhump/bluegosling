package com.apriori.concurrent.atoms;

import java.util.function.Function;

/**
 * An atom whose value is modified synchronously and immediately.
 *
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface SynchronousAtom<T> extends Atom<T> {
   /**
    * Updates the atom's value to the one specified. Upon changing the value, watchers will be
    * notified.
    *
    * @param value the atom's new value
    * @return the atom's previous value
    */
   T set(T value);
   
   /**
    * Applies a function to the atom's value. The atom's new value is the result of applying the
    * specified function to the atom's current value. Watchers are notified when the value is
    * changed.
    *
    * @param function the function to apply
    * @return the atom's new value
    */
   T apply(Function<? super T, ? extends T> function);
}
