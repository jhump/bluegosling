package com.apriori.concurrent;

import java.util.Arrays;
import java.util.concurrent.Future;

//TODO: test
//TODO: javadoc
public final class Cancellables {
   private Cancellables() {
   }

   public static Cancellable fromFuture(final Future<?> future) {
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
   
   public static Cancellable fromFutures(Future<?>... futures) {
      return fromFutures(Arrays.asList(futures));
   }
   
   public static Cancellable fromFutures(final Iterable<? extends Future<?>> futures) {
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
   
   public static Cancellable group(Cancellable... cancellables) {
      return group(Arrays.asList(cancellables));      
   }
   
   public static Cancellable group(final Iterable<? extends Cancellable> cancellables) {
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
