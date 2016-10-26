package com.bluegosling.function;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Numerous utility methods for using {@link Function}s. This is quite similar to Guava's class of
 * the {@linkplain com.google.common.base.Functions same name}, except these utility methods are for
 * the Java 8 {@link Function} functional interface as opposed to Guava's interface of the
 * {@linkplain com.google.common.base.Function same name}.
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

   // TODO: javadoc
   
   public static <T> Function<Object, T> returning(final T value) {
      return (o) -> value;
   }
   
   public static <T> TriFunction<Boolean, T, T, T> ternary() {
      return (condition, t1, t2) -> (condition != null && condition) ? t1 : t2;
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
      return (a) -> function2.apply(function1.apply(a));
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
   public static <A1, A2, B, C> BiFunction<A1, A2, C> compose(
         final BiFunction<? super A1, ? super A2, ? extends B> function1,
         final Function<? super B, ? extends C> function2) {
      return (a1, a2) -> function2.apply(function1.apply(a1, a2));
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
   public static <A1, A2, B1, B2, C> BiFunction<A1, A2, C> compose(
         final Function<? super A1, ? extends B1> function1,
         final Function<? super A2, ? extends B2> function2,
         final BiFunction<? super B1, ? super B2, ? extends C> function3) {
      return (a1, a2) -> function3.apply(function1.apply(a1), function2.apply(a2));
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
   public static <A1, A2, A3, B, C> TriFunction<A1, A2, A3, C> compose(
         final TriFunction<? super A1, ? super A2, ? super A3, ? extends B> function1,
         final Function<? super B, ? extends C> function2) {
      return (a1, a2, a3) -> function2.apply(function1.apply(a1, a2, a3));
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
   public static <A1, A2, A3, B1, B2, B3, C> TriFunction<A1, A2, A3, C> compose(
         final Function<? super A1, ? extends B1> function1,
         final Function<? super A2, ? extends B2> function2,
         final Function<? super A3, ? extends B3> function3,
         final TriFunction<? super B1, ? super B2, ? super B3, ? extends C> function4) {
      return (a1, a2, a3) -> function4.apply(function1.apply(a1), function2.apply(a2),
            function3.apply(a3));
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
      return (o) -> Optional.of(function.apply(o));
   }

   /**
    * Returns a bivariate partial function for the specified bivariate function. In spirit, this is
    * the opposite of {@link PartialFunctions#lift(PartialBiFunction) "lifting"} a partial
    * function to a total function. Since partial functions do not support {@code null} results, the
    * returned partial function will be undefined for any inputs where the specified function
    * returns {@code null}. All other values in the input domain behave as expected and produce a
    * result.
    * 
    * @param function a function
    * @return a partial function that is defined for all values in the input domain except the ones
    *       where the specified function returns {@code null}
    */
   public static <I1, I2, O> PartialBiFunction<I1, I2, O> unlift(
         final BiFunction<? super I1, ? super I2, ? extends O> function) {
      return (o1, o2) -> Optional.of(function.apply(o1, o2));
   }

   /**
    * Returns a three-argument partial function for the specified three-argument function. In
    * spirit, this is the opposite of {@link PartialFunctions#lift(PartialTriFunction)
    * "lifting"} a partial function to a total function. Since partial functions do not support
    * {@code null} results, the returned partial function will be undefined for any inputs where the
    * specified function returns {@code null}. All other values in the input domain behave as
    * expected and produce a result.
    * 
    * @param function a function
    * @return a partial function that is defined for all values in the input domain except the ones
    *       where the specified function returns {@code null}
    */
   public static <I1, I2, I3, O> PartialTriFunction<I1, I2, I3, O> unlift(
         final TriFunction<? super I1, ? super I2, ? super I3, ? extends O> function) {
      return (o1, o2, o3) -> Optional.of(function.apply(o1, o2, o3));
   }
   
   /**
    * Performs a fold-right operation over the specified values and returns the result. This
    * operation is recursive and is defined as follows:<pre>
    *  foldr(list, fun, seed) =&gt; list.empty?
    *                            then seed
    *                            else fun(list.first, foldr(list.rest, fun, seed))
    * </pre>
    * This results in the following diagram of how elements are composed via the folding function:<pre>
    *       fun
    *       / \
    * list[0]  fun
    *          / \
    *    list[1]  fun
    *             / \
    *       list[2]  fun
    *                / \
    *          list[n]  seed
    * </pre>
    * That the shape of this tree descends to the right is why this function its named "fold right".
    *
    * @param values the values to fold
    * @param function the reducing function used at each pair-wise fold
    * @param seed the root value; the result represents this root value, folded into the last
    *       element, the result of which is then folded into the next-to-the-last element, etc.
    * @return the result of folding all elements via the given function
    */
   public static <I, O> O foldRight(Iterable<I> values,
         BiFunction<? super I, ? super O, ? extends O> function, O seed) {
      return foldRight(values.iterator(), function, seed);
   }

   /**
    * Performs a fold-left operation over the specified values and returns the result. This
    * operation is recursive and is defined as follows:<pre>
    *  foldl(list, fun, seed) =&gt; list.empty?
    *                            then seed
    *                            else foldl(list.rest, fun, fun(seed, list.first))
    * </pre> 
    * This results in the following diagram of how elements are composed via the folding function:<pre>
    *            fun
    *            / \
    *          fun  list[n]
    *          / \
    *        fun  list[2]
    *        / \
    *     fun  list[1]
    *     / \
    * seed  list[0]
    * </pre>
    * That the shape of this tree descends to the left is why this function its named "fold left".
    *
    * @param values the values to fold
    * @param function the reducing function used at each pair-wise fold
    * @param seed the root value; the result represents this root value, folded into the first
    *       element, the result of which is then folded into the next element, etc.
    * @return the result of folding all elements via the given function
    */
   public static <I, O> O foldLeft(Iterable<I> values,
         BiFunction<? super O, ? super I, ? extends O> function, O seed) {
      return foldLeft(values.iterator(), function, seed);
   }

   // TODO: javadoc
   public static <I, O> O foldRight(Iterator<I> values,
         BiFunction<? super I, ? super O, ? extends O> function, O seed) {
      return values.hasNext() ? function.apply(values.next(), foldRight(values, function, seed))
            : seed;
   }

   public static <I, O> O foldLeft(Iterator<I> values,
         BiFunction<? super O, ? super I, ? extends O> function, O seed) {
      return values.hasNext() ? foldLeft(values, function, function.apply(seed, values.next()))
            : seed;
   }

   public static <T> Iterable<T> unfold(T seed, Function<? super T, ? extends T> unspoolResult,
         BiFunction<? super T, ? super T, ? extends T> unspoolNext) {
      return unfold(seed, unspoolResult, unspoolNext, Predicates.isNull());
   }
   
   public static <T> Iterable<T> unfold(T seed, Function<? super T, ? extends T> unspoolResult,
         BiFunction<? super T, ? super T, ? extends T> unspoolNext, Predicate<? super T> finished) {
      return () -> new Iterator<T>() {
         private T val = seed;
         private Boolean hasNext;
         
         @Override
         public boolean hasNext() {
            if (hasNext == null) {
               hasNext = !finished.test(val);
            }
            return hasNext;
         }

         @Override
         public T next() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            T result = unspoolResult.apply(val);
            val = unspoolNext.apply(val, result);
            hasNext = null;
            return result;
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException("remove");
         }
      };
   }
   
   public static <I, O> Supplier<O> curry(final Function<? super I, ? extends O> function,
         final I arg) {
      return () -> function.apply(arg);
   }

   public static <I1, I2, O> Supplier<O> curry(
         final BiFunction<? super I1, ? super I2, ? extends O> function,
         final I1 arg1, final I2 arg2) {
      return () -> function.apply(arg1, arg2);
   }

   public static <I1, I2, O> Function<I2, O> curry(
         final BiFunction<? super I1, ? super I2, ? extends O> function,
         final I1 arg1) {
      return (arg2) -> function.apply(arg1, arg2);
   }

   public static <I1, I2, I3, O> Supplier<O> curry(
         final TriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         final I1 arg1, final I2 arg2, final I3 arg3) {
      return () -> function.apply(arg1, arg2, arg3);
   }
   
   public static <I1, I2, I3, O> Function<I3, O> curry(
         final TriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         final I1 arg1, final I2 arg2) {
      return (arg3) -> function.apply(arg1, arg2, arg3);
   }
   
   public static <I1, I2, I3, O> BiFunction<I2, I3, O> curry(
         final TriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         final I1 arg1) {
      return (arg2, arg3) -> function.apply(arg1, arg2, arg3);
   }
   
   public static <T> Function<T, T> yCombinator(
         final Function<Function<? super T, ? extends T>, Function<? super T, ? extends T>> fn) {
      return new Function<T, T>() {
         // can't use lambda notation because we refer to "this" anonymous function
         @Override public T apply(T input) {
            return fn.apply(this).apply(input);
         }
      };
   }
}
