package com.apriori.util;

/**
 * Numerous utility methods related to using {@link Predicate}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Functions
 */
//TODO: tests
public final class Predicates {
   private Predicates() {
   }

   /**
    * A predicate that always returns true.
    */
   private static Predicate<Object> ALL = new Predicate<Object>() {
      @Override
      public boolean test(Object input) {
         return true;
      }
   };

   /**
    * A predicate that always returns false.
    */
   private static Predicate<Object> NONE = new Predicate<Object>() {
      @Override
      public boolean test(Object input) {
         return false;
      }
   };
   
   /**
    * A two-argument predicate that always returns true.
    */
   private static Predicate.Bivariate<Object, Object> ALL_BIVARIATE =
         new Predicate.Bivariate<Object, Object>() {
            @Override
            public boolean test(Object input1, Object input2) {
               return true;
            }
         };

   /**
    * A two-argument predicate that always returns false.
    */
   private static Predicate.Bivariate<Object, Object> NONE_BIVARIATE =
         new Predicate.Bivariate<Object, Object>() {
            @Override
            public boolean test(Object input1, Object input2) {
               return false;
            }
         };

   /**
    * A three-argument predicate that always returns true.
    */
   private static Predicate.Trivariate<Object, Object, Object> ALL_TRIVARIATE =
         new Predicate.Trivariate<Object, Object, Object>() {
            @Override
            public boolean test(Object input1, Object input2, Object input3) {
               return true;
            }
         };

   /**
    * A predicate that always returns false.
    */
   private static Predicate.Trivariate<Object, Object, Object> NONE_TRIVARIATE =
         new Predicate.Trivariate<Object, Object, Object>() {
            @Override
            public boolean test(Object input1, Object input2, Object input3) {
               return false;
            }
         };
         
   // TODO: javadoc
   private static Predicate<Object> IS_NULL = new Predicate<Object>() {
      @Override public boolean test(Object input) {
         return input == null;
      }
   };

   private static Predicate<Object> NOT_NULL = new Predicate<Object>() {
      @Override public boolean test(Object input) {
         return input != null;
      }
   };

   /**
    * Returns a predicate that always returns true.
    * 
    * @return a predicate that always returns true
    */
   @SuppressWarnings("unchecked") // ALL accepts any object, so re-cast will be safe
   public static <T> Predicate<T> acceptAll() {
      return (Predicate<T>) ALL;
   }

   /**
    * Returns a bivariate predicate that always returns true.
    * 
    * @return a predicate that always returns true
    */
   @SuppressWarnings("unchecked") // ALL_BIVARIATE accepts any object, so re-cast will be safe
   public static <T1, T2> Predicate.Bivariate<T1, T2> acceptAllBivariate() {
      return (Predicate.Bivariate<T1, T2>) ALL_BIVARIATE;
   }

   /**
    * Returns a three-argument predicate that always returns true.
    * 
    * @return a predicate that always returns true
    */
   @SuppressWarnings("unchecked") // ALL_TRIVARIATE accepts any object, so re-cast will be safe
   public static <T1, T2, T3> Predicate.Trivariate<T1, T2, T3> acceptAllTrivariate() {
      return (Predicate.Trivariate<T1, T2, T3>) ALL_TRIVARIATE;
   }

   /**
    * Returns a predicate that always returns false.
    * 
    * @return a predicate that always returns false
    */
   @SuppressWarnings("unchecked") // NONE accepts any object, so re-cast will be safe
   public static <T> Predicate<T> rejectAll() {
      return (Predicate<T>) NONE;
   }

   /**
    * Returns a bivariate predicate that always returns false.
    * 
    * @return a predicate that always returns false
    */
   @SuppressWarnings("unchecked") // NONE_BIVARIATE accepts any object, so re-cast will be safe
   public static <T1, T2> Predicate.Bivariate<T1, T2> rejectAllBivariate() {
      return (Predicate.Bivariate<T1, T2>) NONE_BIVARIATE;
   }

   /**
    * Returns a three-argument predicate that always returns false.
    * 
    * @return a predicate that always returns false
    */
   @SuppressWarnings("unchecked") // NONE_TRIVARIATE accepts any object, so re-cast will be safe
   public static <T1, T2, T3> Predicate.Trivariate<T1, T2, T3> rejectAllTrivariate() {
      return (Predicate.Trivariate<T1, T2, T3>) NONE_TRIVARIATE;
   }
   
   // TODO: isNull(), isNotNull(), javadoc...
   
   public static <T> Predicate<T> isEqualTo(final T object) {
      return new Predicate<T>() {
         @Override public boolean test(T input) {
            return object == null ? input == null : object.equals(input);
         }
      };
   }

   public static <T> Predicate<T> isSameAs(final T object) {
      return new Predicate<T>() {
         @Override public boolean test(T input) {
            return object == input;
         }
      };
   }

   public static <T> Predicate<T> isInstanceOf(final Class<? extends T> clazz) {
      return new Predicate<T>() {
         @Override public boolean test(T input) {
            return clazz.isInstance(input);
         }
      };
   }
   
   // TODO: Predicate.Bivariate: areSameObject, areEqual

   // TODO: boolean arithmetic combinations accept var-args?
   
   /**
    * Returns a predicate that combines the results from two predicates using an AND operation. The
    * operation is short-circuited so that the second predicate will not be invoked if the first
    * predicate returns false.
    * 
    * @param p1 the first predicate
    * @param p2 the second predicate
    * @return a predicate that returns {@code p1.test(input) && p2.test(input)}
    */
   public static <T> Predicate<T> and(final Predicate<? super T> p1,
         final Predicate<? super T> p2) {
      return new Predicate<T>() {
         @Override
         public boolean test(T input) {
            return p1.test(input) && p2.test(input);
         }
      };
   }

   /**
    * Returns a predicate that combines the results from two predicates using an OR operation. The
    * operation is short-circuited so that the second predicate will not be invoked if the first
    * predicate returns true.
    * 
    * @param p1 the first predicate
    * @param p2 the second predicate
    * @return a predicate that returns {@code p1.test(input) || p2.test(input)}
    */
   public static <T> Predicate<T> or(final Predicate<? super T> p1,
         final Predicate<? super T> p2) {
      return new Predicate<T>() {
         @Override
         public boolean test(T input) {
            return p1.test(input) || p2.test(input);
         }
      };
   }
   
   /**
    * Returns a predicate that combines the results from two predicates using an XOR operation.
    * 
    * @param p1 the first predicate
    * @param p2 the second predicate
    * @return a predicate that returns {@code p1.test(input) ^ p2.test(input)}
    */
   public static <T> Predicate<T> xor(final Predicate<? super T> p1,
         final Predicate<? super T> p2) {
      return new Predicate<T>() {
         @Override
         public boolean test(T input) {
            return p1.test(input) ^ p2.test(input);
         }
      };
   }
   
   /**
    * Returns a predicate that negates the results of the specified predicate.
    * 
    * @param p a predicate
    * @return a predicate that returns {@code !p.test(input)}
    */
   public static <T> Predicate<T> not(final Predicate<? super T> p) {
      return new Predicate<T>() {
         @Override
         public boolean test(T input) {
            return !p.test(input);
         }
      };
   }

   // TODO: Predicate.[Bi,Tri]variate versions of above boolean arithmetic combinations

   static boolean toPrimitive(Boolean b) {
      return b != null && b;
   }
   
   /**
    * Converts a function into a predicate. Functions return boxed booleans instead of primitives,
    * and thus can return a {@code null}. The returned predicate returns true if the function
    * returns true and false otherwise, so a {@code null} function result is considered false.
    * 
    * @param function a function
    * @return the specified function converted to a predicate
    */
   public static <T> Predicate<T> fromFunction(final Function<T, Boolean> function) {
      return new Predicate<T>() {
         @Override
         public boolean test(T input) {
            return toPrimitive(function.apply(input));
         }
      };
   }

   /**
    * Converts a bivarite function into a bivariate predicate. Functions return boxed booleans
    * instead of primitives, and thus can return a {@code null}. The returned predicate returns
    * true if the function returns true and false otherwise, so a {@code null} function result is
    * considered false.
    * 
    * @param function a function
    * @return the specified function converted to a predicate
    */
   public static <T1, T2> Predicate.Bivariate<T1, T2> fromFunction(
         final Function.Bivariate<T1, T2, Boolean> function) {
      return new Predicate.Bivariate<T1, T2>() {
         @Override
         public boolean test(T1 input1, T2 input2) {
            return toPrimitive(function.apply(input1, input2));
         }
      };
   }

   /**
    * Converts a three-argument function into a three-argument predicate. Functions return boxed
    * booleans instead of primitives, and thus can return a {@code null}. The returned predicate
    * returns true if the function returns true and false otherwise, so a {@code null} function
    * result is considered false.
    * 
    * @param function a function
    * @return the specified function converted to a predicate
    */
   public static <T1, T2, T3> Predicate.Trivariate<T1, T2, T3> fromFunction(
         final Function.Trivariate<T1, T2, T3, Boolean> function) {
      return new Predicate.Trivariate<T1, T2, T3>() {
         @Override
         public boolean test(T1 input1, T2 input2, T3 input3) {
            return toPrimitive(function.apply(input1, input2, input3));
         }
      };
   }
   
   // TODO: javadoc
   public static <I> Source<Boolean> curry(final Predicate<? super I> predicate, final I arg) {
      return new Source<Boolean>() {
         @Override public Boolean get() {
            return predicate.test(arg);
         }
      };
   }

   public static <I1, I2> Source<Boolean> curry(
         final Predicate.Bivariate<? super I1, ? super I2> predicate,
         final I1 arg1, final I2 arg2) {
      return new Source<Boolean>() {
         @Override public Boolean get() {
            return predicate.test(arg1, arg2);
         }
      };
   }

   public static <I1, I2> Predicate<I2> curry(
         final Predicate.Bivariate<? super I1, ? super I2> predicate,
         final I1 arg1) {
      return new Predicate<I2>() {
         @Override public boolean test(I2 arg2) {
            return predicate.test(arg1, arg2);
         }
      };
   }

   public static <I1, I2, I3> Source<Boolean> curry(
         final Predicate.Trivariate<? super I1, ? super I2, ? super I3> predicate,
         final I1 arg1, final I2 arg2, final I3 arg3) {
      return new Source<Boolean>() {
         @Override public Boolean get() {
            return predicate.test(arg1, arg2, arg3);
         }
      };
   }
   
   public static <I1, I2, I3> Predicate<I3> curry(
         final Predicate.Trivariate<? super I1, ? super I2, ? super I3> predicate,
         final I1 arg1, final I2 arg2) {
      return new Predicate<I3>() {
         @Override public boolean test(I3 arg3) {
            return predicate.test(arg1, arg2, arg3);
         }
      };
   }
   
   public static <I1, I2, I3> Predicate.Bivariate<I2, I3> curry(
         final Predicate.Trivariate<? super I1, ? super I2, ? super I3> predicate,
         final I1 arg1) {
      return new Predicate.Bivariate<I2, I3>() {
         @Override public boolean test(I2 arg2, I3 arg3) {
            return predicate.test(arg1, arg2, arg3);
         }
      };
   }
}
