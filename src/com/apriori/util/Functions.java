package com.apriori.util;

import com.apriori.possible.Optional;

import java.util.Comparator;

/**
 * Numerous utility methods for using {@link Function}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Predicates
 * @see PartialFunctions
 */
//TODO: tests
public final class Functions {
   private Functions() {
   }
   
   private static Function<Object, Object> IDENTITY =
         new Function<Object, Object>() {
            @Override public Object apply(Object input) {
               return input;
            }
         };

   private static Function<Object, String> TO_STRING =
         new Function<Object, String>() {
            @Override public String apply(Object input) {
               return input.toString();
            }
         };

   private static Function<Object, Integer> HASH_CODE =
         new Function<Object, Integer>() {
            @Override public Integer apply(Object input) {
               return input.hashCode();
            }
         };
         
   /**
    * Returns a function that always returns its argument unchanged.
    * 
    * @return a function that always returns its argument unchanged
    */
   @SuppressWarnings("unchecked") // safe due to type var constraints and the way IDENTITY works
   public static <T> Function<T, T> identityFunction() {
      return (Function<T, T>) IDENTITY;
   }

   /**
    * Returns a function that returns the string representation of its argument.
    * 
    * @return a function that returns the string representation of its argument
    */
   @SuppressWarnings("unchecked") // safe since TO_STRING accepts all args
   public static <T> Function<T, String> toStringFunction() {
      return (Function<T, String>) TO_STRING;
   }

   /**
    * Returns a function that returns the hash code value of its argument.
    * 
    * @return a function that returns the hash code value of its argument
    */
   @SuppressWarnings("unchecked") // safe since HASH_CODE accepts all args
   public static <T> Function<T, Integer> hashCodeFunction() {
      return (Function<T, Integer>) HASH_CODE;
   }

   /**
    * Returns a function that composes two other functions. Given the first function {@code f(x)}
    * and a second function {@code g(x)}, the returned function computes {@code g(f(x))}.
    * 
    * @param function1 the first function
    * @param function2 the second function
    * @return a function that composes the two specified functions
    */
   public static <A, B, C> Function<A, C> compose(final Function<? super A, ? extends B> function1,
         final Function<? super B, ? extends C> function2) {
      return new Function<A, C>() {
         @Override
         public C apply(A input) {
            return function2.apply(function1.apply(input));
         }
      };
   }
   
   /**
    * Returns a function that composes a two-argument and single-argument function. Given the
    * first function {@code f(x, y)} and a second function {@code g(x)}, the returned function
    * computes {@code g(f(x, y))}.
    * 
    * @param function1 the first function
    * @param function2 the second function
    * @return a function that composes the two specified functions
    */
   public static <A1, A2, B, C> Function.Bivariate<A1, A2, C> compose(
         final Function.Bivariate<? super A1, ? super A2, ? extends B> function1,
         final Function<? super B, ? extends C> function2) {
      return new Function.Bivariate<A1, A2, C>() {
         @Override
         public C apply(A1 input1, A2 input2) {
            return function2.apply(function1.apply(input1, input2));
         }
      };
   }

   /**
    * Returns a function that composes one two-argument and two single-argument functions. Given the
    * first function {@code f(x)}, a second function {@code g(x)}, and a third function
    * {@code h(x, y)}, the returned function computes {@code h(f(x), g(y))}.
    * 
    * @param function1 the first function
    * @param function2 the second function
    * @param function3 the third function
    * @return a function that composes the three specified functions
    */
   public static <A1, A2, B1, B2, C> Function.Bivariate<A1, A2, C> compose(
         final Function<? super A1, ? extends B1> function1,
         final Function<? super A2, ? extends B2> function2,
         final Function.Bivariate<? super B1, ? super B2, ? extends C> function3) {
      return new Function.Bivariate<A1, A2, C>() {
         @Override
         public C apply(A1 input1, A2 input2) {
            return function3.apply(function1.apply(input1), function2.apply(input2));
         }
      };
   }

   /**
    * Returns a function that composes a three-argument and single-argument function. Given the
    * first function {@code f(x, y, z)} and a second function {@code g(x)}, the returned function
    * computes {@code g(f(x, y, z))}.
    * 
    * @param function1 the first function
    * @param function2 the second function
    * @return a function that composes the two specified functions
    */
   public static <A1, A2, A3, B, C> Function.Trivariate<A1, A2, A3, C> compose(
         final Function.Trivariate<? super A1, ? super A2, ? super A3, ? extends B> function1,
         final Function<? super B, ? extends C> function2) {
      return new Function.Trivariate<A1, A2, A3, C>() {
         @Override
         public C apply(A1 input1, A2 input2, A3 input3) {
            return function2.apply(function1.apply(input1, input2, input3));
         }
      };
   }

   /**
    * Returns a function that composes one three-argument and three single-argument functions. Given
    * the first function {@code f(x)}, a second function {@code g(x)}, a third function {@code
    * h(x)}, and a fourth function {@code i(x, y, z)}, the returned function computes
    * {@code i(f(x), g(y), h(z))}.
    * 
    * @param function1 the first function
    * @param function2 the second function
    * @param function3 the third function
    * @return a function that composes the three specified functions
    */
   public static <A1, A2, A3, B1, B2, B3, C> Function.Trivariate<A1, A2, A3, C> compose(
         final Function<? super A1, ? extends B1> function1,
         final Function<? super A2, ? extends B2> function2,
         final Function<? super A3, ? extends B3> function3,
         final Function.Trivariate<? super B1, ? super B2, ? super B3, ? extends C> function4) {
      return new Function.Trivariate<A1, A2, A3, C>() {
         @Override
         public C apply(A1 input1, A2 input2, A3 input3) {
            return function4.apply(function1.apply(input1), function2.apply(input2),
                  function3.apply(input3));
         }
      };
   }
   
   /**
    * Converts a comparator to a bivariate function. The result of the function is the result of
    * calling {@link Comparator#compare(Object, Object)} with the function's two arguments.
    * 
    * @param comparator the comparator
    * @return a function that compares its arguments via the comparator and returns the integer
    *       result
    */
   public static <A> Function.Bivariate<A, A, Integer> fromComparator(
         final Comparator<? super A> comparator) {
      return new Function.Bivariate<A, A, Integer>() {
         @Override
         public Integer apply(A input1, A input2) {
            return comparator.compare(input1,  input2);
         }
      };
   }
   
   /**
    * Converts a predicate to a function.
    * 
    * @param predicate a predicate
    * @return a function that tests its argument with the predicate and returns the boolean result
    */
   public static <T> Function<T, Boolean> fromPredicate(final Predicate<T> predicate) {
      return new Function<T, Boolean>() {
         @Override
         public Boolean apply(T input) {
            return predicate.test(input);
         }
      };
   }

   /**
    * Converts a bivariate predicate to a bivariate function.
    * 
    * @param predicate a predicate
    * @return a function that tests its arguments with the predicate and returns the boolean result
    */
   public static <T1, T2> Function.Bivariate<T1, T2, Boolean> fromPredicate(
         final Predicate.Bivariate<T1, T2> predicate) {
      return new Function.Bivariate<T1, T2, Boolean>() {
         @Override
         public Boolean apply(T1 input1, T2 input2) {
            return predicate.test(input1, input2);
         }
      };
   }

   /**
    * Converts a three-argument predicate to a three-argument function.
    * 
    * @param predicate a predicate
    * @return a function that tests its arguments with the predicate and returns the boolean result
    */
   public static <T1, T2, T3> Function.Trivariate<T1, T2, T3, Boolean> fromPredicate(
         final Predicate.Trivariate<T1, T2, T3> predicate) {
      return new Function.Trivariate<T1, T2, T3, Boolean>() {
         @Override
         public Boolean apply(T1 input1, T2 input2, T3 input3) {
            return predicate.test(input1, input2, input3);
         }
      };
   }

   /**
    * Returns a partial function for the specified function. In spirit, this is the opposite of
    * {@link PartialFunctions#lift(PartialFunction) "lifting"} a partial function to a total
    * function. Since partial functions do not support {@code null} results, the returned partial
    * function will be undefined for any inputs where the specified function returns {@code null}.
    * All other values in the input domain behave as expected and produce a result.
    * 
    * @param function a function
    * @return a partial function that is defined for all values in the input domain except the ones
    *       where the specified function returns {@code null}
    */
   public static <I, O> PartialFunction<I, O> unlift(
         final Function<? super I, ? extends O> function) {
      return new PartialFunction<I, O>() {
         @Override
         public Optional<O> apply(I input) {
            return Optional.<O>of(function.apply(input));
         }
      };
   }

   /**
    * Returns a bivariate partial function for the specified bivariate function. In spirit, this is
    * the opposite of {@link PartialFunctions#lift(PartialFunction.Bivariate) "lifting"} a partial
    * function to a total function. Since partial functions do not support {@code null} results, the
    * returned partial function will be undefined for any inputs where the specified function
    * returns {@code null}. All other values in the input domain behave as expected and produce a
    * result.
    * 
    * @param function a function
    * @return a partial function that is defined for all values in the input domain except the ones
    *       where the specified function returns {@code null}
    */
   public static <I1, I2, O> PartialFunction.Bivariate<I1, I2, O> unlift(
         final Function.Bivariate<? super I1, ? super I2, ? extends O> function) {
      return new PartialFunction.Bivariate<I1, I2, O>() {
         @Override
         public Optional<O> apply(I1 input1, I2 input2) {
            return Optional.<O>of(function.apply(input1, input2));
         }
      };
   }

   /**
    * Returns a three-argument partial function for the specified three-argument function. In
    * spirit, this is the opposite of {@link PartialFunctions#lift(PartialFunction.Trivariate)
    * "lifting"} a partial function to a total function. Since partial functions do not support
    * {@code null} results, the returned partial function will be undefined for any inputs where the
    * specified function returns {@code null}. All other values in the input domain behave as
    * expected and produce a result.
    * 
    * @param function a function
    * @return a partial function that is defined for all values in the input domain except the ones
    *       where the specified function returns {@code null}
    */
   public static <I1, I2, I3, O> PartialFunction.Trivariate<I1, I2, I3, O> unlift(
         final Function.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function) {
      return new PartialFunction.Trivariate<I1, I2, I3, O>() {
         @Override
         public Optional<O> apply(I1 input1, I2 input2, I3 input3) {
            return Optional.<O>of(function.apply(input1, input2, input3));
         }
      };
   }
}
