package com.bluegosling.function;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility methods for constructing and using {@link Supplier}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class Suppliers {
   private Suppliers() {
   }
   
   /**
    * Converts a {@link Callable} into a supplier. If the {@link Callable} throws a checked
    * exception then that exception will be wrapped in a {@link RuntimeException} and propagated.
    * 
    * @param callable a callable
    * @return a supplier that invokes the specified callable
    */
   public static <T> Supplier<T> fromCallable(final Callable<? extends T> callable) {
      return () ->
         {
            try {
               return callable.call();
            } catch (Exception e) {
               if (e instanceof RuntimeException) {
                  throw (RuntimeException) e;
               }
               throw new RuntimeException(e);
            }
         };
   }
   
   /**
    * Returns a supplier that will never invoke the underlying supplier more than once. The result
    * is "memo-ized". All subsequent attempts to get a value from the returned supplier will just
    * get the same memo-ized result of that first invocation.
    * 
    * @param supplier a supplier
    * @return a supplier that will invoke the specified supplier no more than one time and then
    *       memo-ize the result
    */
   public static <T> Supplier<T> memoize(Supplier<T> supplier) {
      return new Supplier<T>() {
         private volatile boolean invoked;
         private T result; // visibility piggy-backs on invoked
         
         @Override
         public T get() {
            if (!invoked) {
               // don't bother synchronizing if we know the value's already been computed
               synchronized (this) {
                  if (!invoked) {
                     result = supplier.get();
                     invoked = true;
                  }
               }
            }
            return result;
         }
      };
   }
}
