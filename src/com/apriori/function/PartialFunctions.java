package com.apriori.function;

import com.apriori.possible.Optionals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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
         Function<? super I, Optional<? extends O>> function) {
      return (o) -> Optionals.upcast(function.apply(o));
   }
   
   /**
    * Adapts a compatible bivariate function to the {@link PartialBiFunction} interface.
    * 
    * @param function a function that returns an optional result
    * @return a partial function that is equivalent to the specified function
    */
   public static <I1, I2, O> PartialBiFunction<I1, I2, O> fromFunction(
         BiFunction<? super I1, ? super I2, Optional<? extends O>> function) {
      return (o1, o2) -> Optionals.upcast(function.apply(o1, o2));
   }

   /**
    * Adapts a compatible three-argument function to the {@link PartialTriFunction}
    * interface.
    * 
    * @param function a function that returns an optional result
    * @return a partial function that is equivalent to the specified function
    */
   public static <I1, I2, I3, O> PartialTriFunction<I1, I2, I3, O> fromFunction(
         TriFunction<? super I1, ? super I2, ? super I3, Optional<? extends O>> function) {
      return (o1, o2, o3) -> Optionals.upcast(function.apply(o1, o2, o3));
   }

   /**
    * Returns a partial function that is backed by a map. The set of supported inputs are those for
    * which keys exist in the map. For other inputs, {@linkplain Optional#empty() empty} is
    * returned.
    * 
    * @param map a map
    * @return a partial function that computes results by looking up values in the map
    */
   public static <K, V> PartialFunction<K, V> fromMap(Map<? super K, ? extends V> map) {
      return (o) -> Optional.of(map.get(o));
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
         PartialFunction<? super I, ? extends O> function1,
         PartialFunction<? super I, ? extends O> function2) {
      return (o) -> {
         Optional<? extends O> result = function1.apply(o);
         if (!result.isPresent()) {
            result = function2.apply(o);
         }
         return Optionals.upcast(result);
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
   public static <I1, I2, O> PartialBiFunction<I1, I2, O> chain(
         PartialBiFunction<? super I1, ? super I2, ? extends O> function1,
         PartialBiFunction<? super I1, ? super I2, ? extends O> function2) {
      return (o1, o2) -> {
         Optional<? extends O> result = function1.apply(o1, o2);
         if (!result.isPresent()) {
            result = function2.apply(o1, o2);
         }
         return Optionals.upcast(result);
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
   public static <I1, I2, I3, O> PartialTriFunction<I1, I2, I3, O> chain(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function1,
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function2) {
      return (o1, o2, o3) -> {
         Optional<? extends O> result = function1.apply(o1, o2, o3);
         if (!result.isPresent()) {
            result = function2.apply(o1, o2, o3);
         }
         return Optionals.upcast(result);
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
   public static <I, O> Function<I, O> lift(PartialFunction<? super I, ? extends O> function,
         O ifUndefined) {
      return (o) -> {
         Optional<? extends O> result = function.apply(o);
         return result.isPresent() ? result.get() : ifUndefined;
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
   public static <I, O> Function<I, O> lift(PartialFunction<? super I, ? extends O> function,
         Function<? super I, ? extends O> fallback) {
      return (o) -> {
         Optional<? extends O> result = function.apply(o);
         return result.isPresent() ? result.get() : fallback.apply(o);
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
   public static <I, O> Function<I, O> lift(PartialFunction<? super I, ? extends O> function) {
      return (o) -> function.apply(o).get();
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
   public static <I, O> Function<I, O> lift(PartialFunction<? super I, ? extends O> function,
         Class<? extends RuntimeException> throwIfUndefined) {
      Constructor<? extends RuntimeException> ctor = defaultConstructor(throwIfUndefined);
      return (o) -> {
         Optional<? extends O> result = function.apply(o);
         if (result.isPresent()) {
            return result.get();
         }
         throw construct(ctor);
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
   public static <I1, I2, O> BiFunction<I1, I2, O> lift(
         PartialBiFunction<? super I1, ? super I2, ? extends O> function, O ifUndefined) {
      return (o1, o2) -> {
         Optional<? extends O> result = function.apply(o1, o2);
         return result.isPresent() ? result.get() : ifUndefined;
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
   public static <I1, I2, O> BiFunction<I1, I2, O> lift(
         PartialBiFunction<? super I1, ? super I2, ? extends O> function,
         BiFunction<? super I1, ? super I2, ? extends O> fallback) {
      return (o1, o2) -> {
         Optional<? extends O> result = function.apply(o1, o2);
         return result.isPresent() ? result.get() : fallback.apply(o1, o2);
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
   public static <I1, I2, O> BiFunction<I1, I2, O> lift(
         final PartialBiFunction<? super I1, ? super I2, ? extends O> function) {
      return (o1, o2) -> function.apply(o1, o2).get();
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
   public static <I1, I2, O> BiFunction<I1, I2, O> lift(
         PartialBiFunction<? super I1, ? super I2, ? extends O> function,
         Class<? extends RuntimeException> throwIfUndefined) {
      Constructor<? extends RuntimeException> ctor = defaultConstructor(throwIfUndefined);
      return (o1, o2) -> {
         Optional<? extends O> result = function.apply(o1, o2);
         if (result.isPresent()) {
            return result.get();
         }
         throw construct(ctor);
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
   public static <I1, I2, I3, O> TriFunction<I1, I2, I3, O> lift(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         O ifUndefined) {
      return (o1, o2, o3) -> {
         Optional<? extends O> result = function.apply(o1, o2, o3);
         return result.isPresent() ? result.get() : ifUndefined;
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
   public static <I1, I2, I3, O> TriFunction<I1, I2, I3, O> lift(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         TriFunction<? super I1, ? super I2, ? super I3, ? extends O> fallback) {
      return (o1, o2, o3) -> {
         Optional<? extends O> result = function.apply(o1, o2, o3);
         return result.isPresent() ? result.get() : fallback.apply(o1, o2, o3);
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
   public static <I1, I2, I3, O> TriFunction<I1, I2, I3, O> lift(
         final PartialTriFunction<? super I1, ? super I2, ? super I3,
               ? extends O> function) {
      return (o1, o2, o3) -> function.apply(o1, o2, o3).get();
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
   public static <I1, I2, I3, O> TriFunction<I1, I2, I3, O> lift(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         Class<? extends RuntimeException> throwIfUndefined) {
      Constructor<? extends RuntimeException> ctor = defaultConstructor(throwIfUndefined);
      return (o1, o2, o3) -> {
         Optional<? extends O> result = function.apply(o1, o2, o3);
         if (result.isPresent()) {
            return result.get();
         }
         throw construct(ctor);
      };
   }
   
   // TODO: javadoc
   public static <I, O> Supplier<Optional<O>> curry(
         PartialFunction<? super I, ? extends O> function, I arg) {
      return () -> Optionals.upcast(function.apply(arg));
   }

   public static <I1, I2, O> Supplier<Optional<O>> curry(
         PartialBiFunction<? super I1, ? super I2, ? extends O> function, I1 arg1, I2 arg2) {
      return () -> Optionals.upcast(function.apply(arg1, arg2));
   }

   public static <I1, I2, O> PartialFunction<I2, O> curry(
         PartialBiFunction<? super I1, ? super I2, ? extends O> function, I1 arg1) {
      return (arg2) -> Optionals.upcast(function.apply(arg1, arg2));
   }

   public static <I1, I2, I3, O> Supplier<Optional<O>> curry(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         I1 arg1, I2 arg2, I3 arg3) {
      return () -> Optionals.upcast(function.apply(arg1, arg2, arg3));
   }
   
   public static <I1, I2, I3, O> PartialFunction<I3, O> curry(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function,
         I1 arg1, I2 arg2) {
      return (arg3) -> Optionals.upcast(function.apply(arg1, arg2, arg3));
   }
   
   public static <I1, I2, I3, O> PartialBiFunction<I2, I3, O> curry(
         PartialTriFunction<? super I1, ? super I2, ? super I3, ? extends O> function, I1 arg1) {
      return (arg2, arg3) -> Optionals.upcast(function.apply(arg1, arg2, arg3));
   }
}
