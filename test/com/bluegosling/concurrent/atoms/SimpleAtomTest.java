package com.bluegosling.concurrent.atoms;

import java.util.function.Predicate;

/**
 * Test cases for {@link SimpleAtom}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: views from other threads? contention over CAS (easy to do w/ apply(...))?
public class SimpleAtomTest extends AbstractSynchronousAtomTest {

   @Override
   protected <T> SynchronousAtom<T> create() {
      return new SimpleAtom<T>();
   }

   @Override
   protected <T> SynchronousAtom<T> create(T initialValue) {
      return new SimpleAtom<T>(initialValue);
   }

   @Override
   protected <T> SynchronousAtom<T> create(T initialValue, Predicate<T> validator) {
      return new SimpleAtom<T>(initialValue, validator);
   }
}
