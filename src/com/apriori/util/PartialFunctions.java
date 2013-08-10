package com.apriori.util;

import com.apriori.possible.Optional;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Numerous utility methods for using {@link PartialFunction}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Functions
 */
//TODO: tests
public final class PartialFunctions {
   private PartialFunctions() {
   }
   
   /**
    * Adapts a compatible function to the {@link PartialFunction} interface.
    * 
    * @param function a function that returns an optional result
    * @return a partial function that is equivalent to the specified function
    */
   public static <I, O> PartialFunction<I, O> fromFunction(
         final Function<? super I, Optional<? extends O>> function) {
      return new PartialFunction<I, O>() {
         @Override
         public Optional<O> apply(I input) {
            return Optional.upcast(function.apply(input));
         }
      };
   }
   
   /**
    * Adapts a compatible bivariate function to the {@link PartialFunction.Bivariate} interface.
    * 
    * @param function a function that returns an optional result
    * @return a partial function that is equivalent to the specified function
    */
   public static <I1, I2, O> PartialFunction.Bivariate<I1, I2, O> fromFunction(
         final Function.Bivariate<? super I1, ? super I2, Optional<? extends O>> function) {
      return new PartialFunction.Bivariate<I1, I2, O>() {
         @Override
         public Optional<O> apply(I1 input1, I2 input2) {
            return Optional.upcast(function.apply(input1, input2));
         }
      };
   }

   /**
    * Adapts a compatible three-argument function to the {@link PartialFunction.Trivariate}
    * interface.
    * 
    * @param function a function that returns an optional result
    * @return a partial function that is equivalent to the specified function
    */
   public static <I1, I2, I3, O> PartialFunction.Trivariate<I1, I2, I3, O> fromFunction(
         final Function.Trivariate<? super I1, ? super I2, ? super I3,
               Optional<? extends O>> function) {
      return new PartialFunction.Trivariate<I1, I2, I3, O>() {
         @Override
         public Optional<O> apply(I1 input1, I2 input2, I3 input3) {
            return Optional.upcast(function.apply(input1, input2, input3));
         }
      };
   }

   /**
    * Returns a partial function that is backed by a map. The set of supported inputs are those for
    * which keys exist in the map. For other inputs, {@linkplain Optional#none() none} is returned.
    * 
    * @param map a map
    * @return a partial function that computes results by looking up values in the map
    */
   public static <K, V> PartialFunction<K, V> fromMap(
         final Map<? super K, ? extends V> map) {
      return new PartialFunction<K, V>() {
         @Override
         public Optional<V> apply(K input) {
            return Optional.<V>of(map.get(input));
         }
      };
   }
   
   /**
    * Returns a partial function that chains two partial functions together. If the first function
    * does not support an input, then the second function will be invoked. The result for a given
    * input will be absent only if neither function can compute it. If both functions can compute
    * a result for an input, the result from the first function will be used.
    * 
    * @param function1 the first partial function
    * @param function2 the second partial function
    * @return a partial function that invokes the first function and then, if necessary, the second
    *       in order to compute result
    */
   public static <I, O> PartialFunction<I, O> chain(
         final PartialFunction<? super I, ? extends O> function1,
         final PartialFunction<? super I, ? extends O> function2) {
      return new PartialFunction<I, O>() {
         @Override
         public Optional<O> apply(I input) {
            Optional<? extends O> result = function1.apply(input);
            if (!result.isPresent()) {
               result = function2.apply(input);
            }
            return Optional.upcast(result);
         }
      };
   }

   /**
    * Returns a bivariate partial function that chains two bivariate partial functions together. If
    * the first function does not support an input, then the second function will be invoked. The
    * result for a given input will be absent only if neither function can compute it. If both
    * functions can compute a result for an input, the result from the first function will be used.
    * 
    * @param function1 the first partial function
    * @param function2 the second partial function
    * @return a partial function that invokes the first function and then, if necessary, the second
    *       in order to compute result
    */
   public static <I1, I2, O> PartialFunction.Bivariate<I1, I2, O> chain(
         final PartialFunction.Bivariate<? super I1, ? super I2, ? extends O> function1,
         final PartialFunction.Bivariate<? super I1, ? super I2, ? extends O> function2) {
      return new PartialFunction.Bivariate<I1, I2, O>() {
         @Override
         public Optional<O> apply(I1 input1, I2 input2) {
            Optional<? extends O> result = function1.apply(input1, input2);
            if (!result.isPresent()) {
               result = function2.apply(input1, input2);
            }
            return Optional.upcast(result);
         }
      };
   }

   /**
    * Returns a bivariate partial function that chains two bivariate partial functions together. If
    * the first function does not support an input, then the second function will be invoked. The
    * result for a given input will be absent only if neither function can compute it. If both
    * functions can compute a result for an input, the result from the first function will be used.
    * 
    * @param function1 the first partial function
    * @param function2 the second partial function
    * @return a partial function that invokes the first function and then, if necessary, the second
    *       in order to compute result
    */
   public static <I1, I2, I3, O> PartialFunction.Trivariate<I1, I2, I3, O> chain(
         final PartialFunction.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function1,
         final PartialFunction.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function2) {
      return new PartialFunction.Trivariate<I1, I2, I3, O>() {
         @Override
         public Optional<O> apply(I1 input1, I2 input2, I3 input3) {
            Optional<? extends O> result = function1.apply(input1, input2, input3);
            if (!result.isPresent()) {
               result = function2.apply(input1, input2, input3);
            }
            return Optional.upcast(result);
         }
      };
   }
   
   static <T> Constructor<T> defaultConstructor(Class<T> clazz) {
      try {
         Constructor<T> ctor = clazz.getConstructor();
         ctor.setAccessible(true);
         return ctor;
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException("exception type cannot be instantiated", e);
      }
   }
   
   static <T> T construct(Constructor<T> ctor) {
      try {
         return ctor.newInstance();
      }
      catch (InstantiationException e) {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
      catch (InvocationTargetException e) {
         throw new RuntimeException(e.getCause());
      }
   }

   /**
    * Lifts a partial function to a regular function via default value. The returned value of the
    * regular function is not optional.
    * 
    * <p>This form of the method returns a default value for elements in the input domain for which
    * the partial function cannot compute a result.
    * 
    * @param function a partial function
    * @param ifUndefined the value to return for unsupported inputs
    * @return a regular function that returns a default value for unsupported inputs
    */
   public static <I, O> Function<I, O> lift(final PartialFunction<? super I, ? extends O> function,
         final O ifUndefined) {
      return new Function<I, O>() {
         @Override public O apply(I input) {
            Optional<? extends O> result = function.apply(input);
            return result.isPresent() ? result.get() : ifUndefined;
         }
      };
   }

   /**
    * Lifts a partial function to a regular function via a fallback function. The returned value of
    * the regular function is not optional.
    * 
    * <p>This form of the method returns a value for incomputable elements in the input domain by
    * falling back to another function to supply those results.
    * 
    * @param function a partial function
    * @param fallback the fallback function
    * @return a regular function that invokes a fallback function for unsupported inputs
    */
   public static <I, O> Function<I, O> lift(
         final PartialFunction<? super I, ? extends O> function,
         final Function<? super I, ? extends O> fallback) {
      return new Function<I, O>() {
         @Override public O apply(I input) {
            Optional<? extends O> result = function.apply(input);
            return result.isPresent() ? result.get() : fallback.apply(input);
         }
      };
   }

   /**
    * Lifts a partial function to a regular function. The returned value of the regular function is
    * not optional.
    * 
    * <p>This form of the method does not provide a real fallback strategy for inputs that cannot be
    * computed. Instead, the returned function will throw {@link IllegalStateException} when given
    * an input that isn't supported.
    * 
    * @param function a partial function
    * @return a regular function that throws an exception for unsupported inputs
    */
   public static <I, O> Function<I, O> lift(
         final PartialFunction<? super I, ? extends O> function) {
      return new Function<I, O>() {
         @Override public O apply(I input) {
            return function.apply(input).get();
         }
      };
   }
   
   /**
    * Lifts a partial function to a regular function that throws a custom exception for incomputable
    * inputs. The returned value of the regular function is not optional.
    * 
    * <p>This form of the method does not provide a real fallback strategy for inputs that cannot be
    * computed. Instead, the returned function will throw a custom exception when given an input
    * that isn't supported. The specified exception type must be unchecked (since a partial function
    * cannot throw a checked exception due to its signature). It also must have a default,
    * no-argument constructor which will be used to construct instances of the exception.
    * 
    * @param function a partial function
    * @param throwIfUndefined the type of exception to 
    * @return a regular function that throws an exception for unsupported inputs
    */
   public static <I, O> Function<I, O> lift(final PartialFunction<? super I, ? extends O> function,
         Class<? extends RuntimeException> throwIfUndefined) {
      final Constructor<? extends RuntimeException> ctor = defaultConstructor(throwIfUndefined);
      return new Function<I, O>() {
         @Override public O apply(I input) {
            Optional<? extends O> result = function.apply(input);
            if (result.isPresent()) {
               return result.get();
            }
            throw construct(ctor);
         }
      };
   }

   /**
    * Lifts a bivariate partial function to a regular function via default value. The returned value
    * of the regular function is not optional.
    * 
    * <p>This form of the method returns a default value for elements in the input domain for which
    * the partial function cannot compute a result.
    * 
    * @param function a partial function
    * @param ifUndefined the value to return for unsupported inputs
    * @return a regular function that returns a default value for unsupported inputs
    */
   public static <I1, I2, O> Function.Bivariate<I1, I2, O> lift(
         final PartialFunction.Bivariate<? super I1, ? super I2, ? extends O> function,
         final O ifUndefined) {
      return new Function.Bivariate<I1, I2, O>() {
         @Override public O apply(I1 input1, I2 input2) {
            Optional<? extends O> result = function.apply(input1, input2);
            return result.isPresent() ? result.get() : ifUndefined;
         }
      };
   }
   
   /**
    * Lifts a bivariate partial function to a regular function via a fallback function. The returned
    * value of the regular function is not optional.
    * 
    * <p>This form of the method returns a value for incomputable elements in the input domain by
    * falling back to another function to supply those results.
    * 
    * @param function a partial function
    * @param fallback the fallback function
    * @return a regular function that invokes a fallback function for unsupported inputs
    */
   public static <I1, I2, O> Function.Bivariate<I1, I2, O> lift(
         final PartialFunction.Bivariate<? super I1, ? super I2, ? extends O> function,
         final Function.Bivariate<? super I1, ? super I2, ? extends O> fallback) {
      return new Function.Bivariate<I1, I2, O>() {
         @Override public O apply(I1 input1, I2 input2) {
            Optional<? extends O> result = function.apply(input1, input2);
            return result.isPresent() ? result.get() : fallback.apply(input1, input2);
         }
      };
   }

   /**
    * Lifts a bivariate partial function to a regular function. The returned value of the regular
    * function is not optional.
    * 
    * <p>This form of the method does not provide a real fallback strategy for inputs that cannot be
    * computed. Instead, the returned function will throw {@link IllegalStateException} when given
    * an input that isn't supported.
    * 
    * @param function a partial function
    * @return a regular function that throws an exception for unsupported inputs
    */
   public static <I1, I2, O> Function.Bivariate<I1, I2, O> lift(
         final PartialFunction.Bivariate<? super I1, ? super I2, ? extends O> function) {
      return new Function.Bivariate<I1, I2, O>() {
         @Override public O apply(I1 input1, I2 input2) {
            return function.apply(input1, input2).get();
         }
      };
   }
   
   /**
    * Lifts a bivariate partial function to a regular function that throws a custom exception for
    * incomputable inputs. The returned value of the regular function is not optional.
    * 
    * <p>This form of the method does not provide a real fallback strategy for inputs that cannot be
    * computed. Instead, the returned function will throw a custom exception when given an input
    * that isn't supported. The specified exception type must be unchecked (since a partial function
    * cannot throw a checked exception due to its signature). It also must have a default,
    * no-argument constructor which will be used to construct instances of the exception.
    * 
    * @param function a partial function
    * @param throwIfUndefined the type of exception to 
    * @return a regular function that throws an exception for unsupported inputs
    */
   public static <I1, I2, O> Function.Bivariate<I1, I2, O> lift(
         final PartialFunction.Bivariate<? super I1, ? super I2, ? extends O> function,
         Class<? extends RuntimeException> throwIfUndefined) {
      final Constructor<? extends RuntimeException> ctor = defaultConstructor(throwIfUndefined);
      return new Function.Bivariate<I1, I2, O>() {
         @Override public O apply(I1 input1, I2 input2) {
            Optional<? extends O> result = function.apply(input1, input2);
            if (result.isPresent()) {
               return result.get();
            }
            throw construct(ctor);
         }
      };
   }

   /**
    * Lifts a three-argument partial function to a regular function via default value. The returned
    * value of the regular function is not optional.
    * 
    * <p>This form of the method returns a default value for elements in the input domain for which
    * the partial function cannot compute a result.
    * 
    * @param function a partial function
    * @param ifUndefined the value to return for unsupported inputs
    * @return a regular function that returns a default value for unsupported inputs
    */
   public static <I1, I2, I3, O> Function.Trivariate<I1, I2, I3, O> lift(
         final PartialFunction.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function,
         final O ifUndefined) {
      return new Function.Trivariate<I1, I2, I3, O>() {
         @Override public O apply(I1 input1, I2 input2, I3 input3) {
            Optional<? extends O> result = function.apply(input1, input2, input3);
            return result.isPresent() ? result.get() : ifUndefined;
         }
      };
   }
   
   /**
    * Lifts a three-argument partial function to a regular function via a fallback function. The
    * returned value of the regular function is not optional.
    * 
    * <p>This form of the method returns a value for incomputable elements in the input domain by
    * falling back to another function to supply those results.
    * 
    * @param function a partial function
    * @param fallback the fallback function
    * @return a regular function that invokes a fallback function for unsupported inputs
    */
   public static <I1, I2, I3, O> Function.Trivariate<I1, I2, I3, O> lift(
         final PartialFunction.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function,
         final Function.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> fallback) {
      return new Function.Trivariate<I1, I2, I3, O>() {
         @Override public O apply(I1 input1, I2 input2, I3 input3) {
            Optional<? extends O> result = function.apply(input1, input2, input3);
            return result.isPresent() ? result.get() : fallback.apply(input1, input2, input3);
         }
      };
   }
   
   /**
    * Lifts a three-argument partial function to a regular function. The returned value of the
    * regular function is not optional.
    * 
    * <p>This form of the method does not provide a real fallback strategy for inputs that cannot be
    * computed. Instead, the returned function will throw {@link IllegalStateException} when given
    * an input that isn't supported.
    * 
    * @param function a partial function
    * @return a regular function that throws an exception for unsupported inputs
    */
   public static <I1, I2, I3, O> Function.Trivariate<I1, I2, I3, O> lift(
         final PartialFunction.Trivariate<? super I1, ? super I2, ? super I3,
               ? extends O> function) {
      return new Function.Trivariate<I1, I2, I3, O>() {
         @Override public O apply(I1 input1, I2 input2, I3 input3) {
            return function.apply(input1, input2, input3).get();
         }
      };
   }
   
   /**
    * Lifts a three-argument partial function to a regular function that throws a custom exception
    * for incomputable inputs. The returned value of the regular function is not optional.
    * 
    * <p>This form of the method does not provide a real fallback strategy for inputs that cannot be
    * computed. Instead, the returned function will throw a custom exception when given an input
    * that isn't supported. The specified exception type must be unchecked (since a partial function
    * cannot throw a checked exception due to its signature). It also must have a default,
    * no-argument constructor which will be used to construct instances of the exception.
    * 
    * @param function a partial function
    * @param throwIfUndefined the type of exception to 
    * @return a regular function that throws an exception for unsupported inputs
    */
   public static <I1, I2, I3, O> Function.Trivariate<I1, I2, I3, O> lift(
         final PartialFunction.Trivariate<? super I1, ? super I2, ? super I3, ? extends O> function,
         Class<? extends RuntimeException> throwIfUndefined) {
      final Constructor<? extends RuntimeException> ctor = defaultConstructor(throwIfUndefined);
      return new Function.Trivariate<I1, I2, I3, O>() {
         @Override public O apply(I1 input1, I2 input2, I3 input3) {
            Optional<? extends O> result = function.apply(input1, input2, input3);
            if (result.isPresent()) {
               return result.get();
            }
            throw construct(ctor);
         }
      };
   }
}
