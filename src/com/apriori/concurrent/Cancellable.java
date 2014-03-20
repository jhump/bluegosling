package com.apriori.concurrent;

import java.util.Arrays;
import java.util.concurrent.Future;

/**
 * Represents an activity that can be cancelled. The activity may be in the future or may be a
 * concurrently executing activity.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: test
//TODO: javadoc
public interface Cancellable {
   /**
    * Cancels the activity.
    *
    * @param mayInterrupt if true and the activity is executing concurrently, the thread executing
    *       the activity will be interrupted
    * @return true if the activity was cancelled; false if it could not be cancelled because the
    *       activity has already completed
    */
   boolean cancel(boolean mayInterrupt);

   static Cancellable fromFuture(final Future<?> future) {
      if (future instanceof Cancellable) {
         return (Cancellable) future;
      }
      return new Cancellable() {
         @Override
         public boolean cancel(boolean mayInterrupt) {
            return future.cancel(mayInterrupt);
         }
      };
   }
   
   static Cancellable fromFutures(Future<?>... futures) {
      return fromFutures(Arrays.asList(futures));
   }
   
   static Cancellable fromFutures(final Iterable<? extends Future<?>> futures) {
      return new Cancellable() {
         @Override
         public boolean cancel(boolean mayInterrupt) {
            boolean ret = false;
            for (Future<?> f : futures) {
               if (f.cancel(mayInterrupt)) {
                  ret = true;
               }
            }
            return ret;
         }
      };
   }
   
   static Cancellable group(Cancellable... cancellables) {
      return group(Arrays.asList(cancellables));      
   }
   
   static Cancellable group(final Iterable<? extends Cancellable> cancellables) {
      return new Cancellable() {
         @Override
         public boolean cancel(boolean mayInterrupt) {
            boolean ret = false;
            for (Cancellable c : cancellables) {
               if (c.cancel(mayInterrupt)) {
                  ret = true;
               }
            }
            return ret;
         }
      };
   }
}
