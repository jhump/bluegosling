package com.apriori.util;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * A {@link Generator} that does not throw checked exceptions during generation.
 *
 * @param <T> the type of value generated
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public abstract class UncheckedGenerator<T> extends Generator<T, RuntimeException> {

   protected UncheckedGenerator() {
      super();
   }

   protected UncheckedGenerator(ThreadFactory factory) {
      super(factory);
   }

   protected UncheckedGenerator(Executor executor) {
      super(executor);
   }

   @Override public UncheckedSequence<T> start() {
      final Sequence<T, RuntimeException> s = super.start();
      return new UncheckedSequence<T>() {
         @Override
         public T next() {
            return s.next();
         }

         @Override
         public Iterator<T> asIterator() {
            return s.asIterator();
         }
      };
      
   }
}
