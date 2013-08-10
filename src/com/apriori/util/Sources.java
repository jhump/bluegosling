package com.apriori.util;

import com.apriori.possible.Possible;

import java.util.concurrent.Callable;

/**
 * Utility methods for constructing and using {@link Source}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class Sources {
   private Sources() {
   }
   
   /**
    * Converts a source into a {@link Callable}.
    * 
    * @param source a source
    * @return a callable that invokes the specified source
    */
   public static <T> Callable<T> toCallable(final Source<? extends T> source) {
      return new Callable<T>() {
         @Override
         public T call() {
            return source.get();
         }
      };
   }
   
   /**
    * Converts a {@link Callable} into a source. If the {@link Callable} throws a checked exception
    * then that exception will be wrapped in a {@link RuntimeException} and propagated.
    * 
    * @param callable a callable
    * @return a source that invokes the specified callable
    */
   public static <T> Source<T> fromCallable(final Callable<? extends T> callable) {
      return new Source<T>() {
         @Override
         public T get() {
            try {
               return callable.call();
            } catch (Exception e) {
               if (e instanceof RuntimeException) {
                  throw (RuntimeException) e;
               }
               throw new RuntimeException(e);
            }
         }
      };
   }
   
   /**
    * Converts a possible value into a source.
    * 
    * @param possible a possible value
    * @return a source that attempts to get a present value from the specified possible value
    */
   public static <T> Source<T> fromPossible(final Possible<? extends T> possible) {
      return new Source<T>() {
         @Override
         public T get() {
            return possible.get();
         }
      };
   }

   /**
    * Returns a source that always provides the specified instance.
    * 
    * @param t a value
    * @return a source that always returns the specified value
    */
   public static <T> Source<T> of(final T t) {
      return new Source<T>() {
         @Override
         public T get() {
            return t;
         }
      };
   }
   
   /**
    * Returns a source that will never invoke the underlying source more than once. The result is
    * "memo-ized". All subsequent attempts to get a value from the returned source will just get the
    * same memo-ized result of that first invocation.
    * 
    * @param source a source
    * @return a source that will invoke the specified source no more than one time and then memo-ize
    *       the result
    */
   public static <T> Source<T> memoize(final Source<T> source) {
      return new Source<T>() {
         private volatile boolean invoked;
         private volatile T result;
         
         @Override
         public T get() {
            if (!invoked) {
               // don't bother synchronizing if we know the value's already been computed
               synchronized (this) {
                  if (!invoked) {
                     invoked = true;
                     result = source.get();
                  }
               }
            }
            return result;
         }
      };
   }
}
