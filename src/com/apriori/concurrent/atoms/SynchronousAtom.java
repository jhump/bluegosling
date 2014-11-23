package com.apriori.concurrent.atoms;

import java.util.function.BiFunction;
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
    * Sets the atom's value to the one specified and returns the previous value. Upon changing the
    * value, watchers will be notified.
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
   T updateAndGet(Function<? super T, ? extends T> function);

   /**
    * Applies a function to the atom's value. The atom's new value is the result of applying the
    * specified function to the atom's current value. Watchers are notified when the value is
    * changed.
    *
    * @param function the function to apply
    * @return the atom's previous value
    */
   T getAndUpdate(Function<? super T, ? extends T> function);

   /**
    * Combines the atom's value with the given value, using the given function. The atom's new value
    * is the result of applying the function. Watchers are notified when the value is changed.
    *
    * @param t the value to combine
    * @param function the function to apply
    * @return the atom's new value
    */
   default T accumulateAndGet(T t, BiFunction<? super T, ? super T, ? extends T> function) {
      return updateAndGet(v -> function.apply(v, t));
   }
   
   /**
    * Combines the atom's value with the given value, using the given function. The atom's new value
    * is the result of applying the function. Watchers are notified when the value is changed.
    *
    * @param t the value to combine
    * @param function the function to apply
    * @return the atom's previous value
    */
   default T getAndAccumulate(T t, BiFunction<? super T, ? super T, ? extends T> function) {
      return getAndUpdate(v -> function.apply(v, t));
   }
}
