package com.apriori.generator;

import java.util.concurrent.Executor;

/**
 * A {@link Generator} that does not throw checked exceptions during generation.
 *
 * @param <T> the type of value generated
 * @param <U> the type of value passed to the generator
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public abstract class UncheckedGenerator<T, U> extends Generator<T, U, RuntimeException> {

   protected UncheckedGenerator() {
      super();
   }

   protected UncheckedGenerator(Executor executor) {
      super(executor);
   }

   @Override public UncheckedSequence<T, U> start() {
      Sequence<T, U, RuntimeException> s = super.start();
      return s::next;
   }
}
