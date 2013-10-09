package com.apriori.util;

import com.apriori.possible.Optional;
import com.apriori.tuples.Pair;
import com.apriori.tuples.Trio;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
         
   private static Function.Bivariate<Object, Object, Pair<Object, Object>> PAIR =
         new Function.Bivariate<Object, Object, Pair<Object, Object>>() {
            @Override public Pair<Object, Object> apply(Object input1, Object input2) {
               return Pair.create(input1, input2);
            }
         };

   private static Function.Trivariate<Object, Object, Object, Trio<Object, Object, Object>> TRIO =
         new Function.Trivariate<Object, Object, Object, Trio<Object, Object, Object>>() {
            @Override
            public Trio<Object, Object, Object> apply(Object input1, Object input2, Object input3) {
               return Trio.create(input1, input2, input3);
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

   // TODO: javadoc
   
   public static <T> Function<Object, T> returning(final T value) {
      return new Function<Object, T>() {
         @Override public T apply(Object input) {
            return value;
         }
         
      };
   }
   
   @SuppressWarnings({"unchecked", "rawtypes"}) // PAIR accepts all args and returns pair
   public static <T, U> Function.Bivariate<T, U, Pair<T, U>> pairFunction() {
      Function.Bivariate ret = PAIR;
      return ret;
   }

   @SuppressWarnings({"unchecked", "rawtypes"}) // TRIO accepts all args and returns pair
   public static <T, U, V> Function.Trivariate<T, U, V, Trio<T, U, V>> trioFunction() {
      Function.Trivariate ret = TRIO;
      return ret;
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
   
   // TODO: javadoc
   /**
    * Performs a fold-right operation over the specified values and returns the result. This
    * operation is recursive and is defined as follows:<pre>
    *  foldr(list, fun, seed) => list.empty?
    *                              then seed
    *                              else fun(list.first, foldr(list.rest, fun, seed)
    * </pre> 
    *
    * @param values
    * @param function
    * @param seed
    * @return
    */
   public static <I, O> O foldRight(Iterable<I> values,
         Function.Bivariate<? super I, ? super O, ? extends O> function, O seed) {
      return foldRight(values.iterator(), function, seed);
   }

   public static <I, O> O foldLeft(Iterable<I> values,
         Function.Bivariate<? super O, ? super I, ? extends O> function, O seed) {
      return foldLeft(values.iterator(), function, seed);
   }

   public static <I, O> O foldRight(Iterator<I> values,
         Function.Bivariate<? super I, ? super O, ? extends O> function, O seed) {
      return values.hasNext() ? function.apply(values.next(), foldRight(values, function, seed))
            : seed;
   }

   public static <I, O> O foldLeft(Iterator<I> values,
         Function.Bivariate<? super O, ? super I, ? extends O> function, O seed) {
      return values.hasNext() ? foldLeft(values, function, function.apply(seed, values.next()))
            : seed;
   }
   
   public static <T> Iterable<T> unfold(final Function<? super T, Pair<T, T>> unspool,
         final Predicate<? super T> finished, final T seed) {
      return new Iterable<T>() {
         @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
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
                  Pair<T, T> unspooled = unspool.apply(val);
                  T result = unspooled.getFirst();
                  val = unspooled.getSecond();
                  hasNext = null;
                  return result;
               }

               @Override
               public void remove() {
                  throw new UnsupportedOperationException("remove");
               }
            };
         }
      };
   }
   
   public static <I, O> Source<O> curry(final Function<? super I, ? extends O> function,
         final I arg) {
      return new Source<O>() {
         @Override public O get() {
            return function.apply(arg);
         }
      };
   }

   public static <I1, I2, O> Source<O> curry(
         final Function.Bivariate<? super I1, ? super I2, ? extends O> function,
         final I1 arg1, final I2 arg2) {
      return new Source<O>() {
         @Override public O get() {
            return function.apply(arg1, arg2);
         }
      };
   }

   public static <I1, I2, O> Function<I2, O> curry(
         final Function.Bivariate<? super I1, ? super I2, ? extends O> function,
         final I1 arg1) {
      return new Function<I2, O>() {
         @Override public O apply(I2 arg2) {
            return function.apply(arg1, arg2);
         }
      };
   }

   public static <I1, I2, I3, O> Source<O> curry(
         final Function.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function,
         final I1 arg1, final I2 arg2, final I3 arg3) {
      return new Source<O>() {
         @Override public O get() {
            return function.apply(arg1, arg2, arg3);
         }
      };
   }
   
   public static <I1, I2, I3, O> Function<I3, O> curry(
         final Function.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function,
         final I1 arg1, final I2 arg2) {
      return new Function<I3, O>() {
         @Override public O apply(I3 arg3) {
            return function.apply(arg1, arg2, arg3);
         }
      };
   }
   
   public static <I1, I2, I3, O> Function.Bivariate<I2, I3, O> curry(
         final Function.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function,
         final I1 arg1) {
      return new Function.Bivariate<I2, I3, O>() {
         @Override public O apply(I2 arg2, I3 arg3) {
            return function.apply(arg1, arg2, arg3);
         }
      };
   }
   
   public static <T> Function<T, T> yCombinator(
         final Function<Function<? super T, ? extends T>, Function<? super T, ? extends T>> funcGen) {
      return new Function<T, T>() {
         @Override public T apply(T input) {
            return funcGen.apply(this).apply(input);
         }
      };
   }
}