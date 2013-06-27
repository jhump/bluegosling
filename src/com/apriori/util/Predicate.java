package com.apriori.util;

/**
 * A {@linkplain Function function} that returns a boolean value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the argument type
 */
public interface Predicate<T> extends Function<T, Boolean> {

   /**
    * A predicate that always returns true.
    */
   Predicate<Object> ALL = new Predicate<Object>() {
      @Override
      public Boolean apply(Object input) {
         return true;
      }
   };

   /**
    * A predicate that always returns false.
    */
   Predicate<Object> NONE = new Predicate<Object>() {
      @Override
      public Boolean apply(Object input) {
         return false;
      }
   };
   
   /**
    * A {@link Function.Bivariate function} that returns a boolean value.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T1> the first argument type
    * @param <T2> the second argument type
    */
   interface Bivariate<T1, T2> extends Function.Bivariate<T1, T2, Boolean> {
      /**
       * A predicate that always returns true.
       */
      @SuppressWarnings("hiding")
      Bivariate<Object, Object> ALL = new Bivariate<Object, Object>() {
         @Override
         public Boolean apply(Object input1, Object input2) {
            return true;
         }
      };

      /**
       * A predicate that always returns false.
       */
      @SuppressWarnings("hiding")
      Bivariate<Object, Object> NONE = new Bivariate<Object, Object>() {
         @Override
         public Boolean apply(Object input1, Object input2) {
            return false;
         }
      };
   }
   
   /**
    * A {@link Function.Trivariate function} that returns a boolean value.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T1> the first argument type
    * @param <T2> the second argument type
    * @param <T3> the third argument type
    */
   interface Trivariate<T1, T2, T3> extends Function.Trivariate<T1, T2, T3, Boolean> {
      /**
       * A predicate that always returns true.
       */
      @SuppressWarnings("hiding")
      Trivariate<Object, Object, Object> ALL = new Trivariate<Object, Object, Object>() {
         @Override
         public Boolean apply(Object input1, Object input2, Object input3) {
            return true;
         }
      };

      /**
       * A predicate that always returns false.
       */
      @SuppressWarnings("hiding")
      Trivariate<Object, Object, Object> NONE = new Trivariate<Object, Object, Object>() {
         @Override
         public Boolean apply(Object input1, Object input2, Object input3) {
            return false;
         }
      };
   }
}
