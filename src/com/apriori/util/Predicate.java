package com.apriori.util;

import java.util.Random;

//TODO: javadoc
public interface Predicate<T> extends Function<T, Boolean> {
   interface Bivariate<T1, T2> extends Function.Bivariate<T1, T2, Boolean> {
   }
   interface Trivariate<T1, T2, T3> extends Function.Trivariate<T1, T2, T3, Boolean> {
   }
   
   Predicate<Object> ALL = new Predicate<Object>() {
      @Override
      public Boolean apply(Object input) {
         return true;
      }
   };

   Predicate<Object> NONE = new Predicate<Object>() {
      @Override
      public Boolean apply(Object input) {
         return false;
      }
   };
   
   Predicate<Object> RANDOM = new Predicate<Object>() {
      private final Random random = new Random();
      @Override
      public Boolean apply(Object input) {
         return random.nextBoolean();
      }
   };
}
