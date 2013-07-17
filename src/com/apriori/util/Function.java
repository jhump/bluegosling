package com.apriori.util;

import java.util.Map;

/**
 * A functional interface that accepts one argument and returns a value. This can be used in
 * functional style programming and is similar to a lambda (although constrained to one argument
 * and one return value).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type of the single argument
 * @param <O> the "output" type of the returned value
 */
public interface Function<I, O> {
   /**
    * Invokes the function with the specified argument and returns the result.
    * 
    * @param input the single argument
    * @return the function's result
    */
   O apply(I input);
   
   /**
    * Just like a {@link Function} except that it takes two arguments instead of just one.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I1> the type of the first argument
    * @param <I2> the type of the second argument
    * @param <O> the result type
    */
   interface Bivariate<I1, I2, O> {
      /**
       * Invokes the function with the specified arguments and returns the result.
       * 
       * @param input1 the first argument
       * @param input2 the second argument
       * @return the function's result
       */
      O apply(I1 input1, I2 input2);
   }
   
   /**
    * Just like a {@link Function} except that it takes three arguments instead of just one.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I1> the type of the first argument
    * @param <I2> the type of the second argument
    * @param <I3> the type of the third argument
    * @param <O> the result type
    */
   interface Trivariate<I1, I2, I3, O> {
      /**
       * Invokes the function with the specified arguments and returns the result.
       * 
       * @param input1 the first argument
       * @param input2 the second argument
       * @param input3 the third argument
       * @return the function's result
       */
      O apply(I1 input1, I2 input2, I3 input3);
   }
   
   /**
    * A utility implementation that determines function results by looking up entries in a
    * {@link Map}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the type of the argument (e.g. key)
    * @param <O> the type of the result (e.g. value)
    */
   public static class FromMap<I, O> implements Function<I, O> {
      private final Map<I, O> map;
      private final boolean strict;
      
      /**
       * Constructs a new function that performs look-ups against the specified map. When the
       * function is invoked for a given argument, the map is inspected. If the map contains a
       * key equal to the specified argument, then the corresponding value will be the function's
       * result. If there is no such key then {@code null} is returned.
       * 
       * @param map the map that backs this function
       * 
       * @see #strict()
       */
      public FromMap(Map<I, O> map) {
         this(map, false);
      }
      
      private FromMap(Map<I, O> map, boolean strict) {
         this.map = map;
         this.strict = strict;
      }
      
      /**
       * Creates a strict function backed by the same map as this instance. A strict map will throw
       * an exception if invoked with a value that is not present as a key in the underlying map.
       * 
       * @return a new function that does not allow function arguments that are not represented in
       *       the underlying map
       *       
       * @see #relaxed()
       */
      public FromMap<I, O> strict() {
         return new FromMap<I, O>(map, true);
      }

      /**
       * Creates a relaxed function backed by the same map as this instance. A relaxed map will
       * return {@code null} if invoked with a value that is not present as a key in the underlying
       * map.
       * 
       * @return a new function that allows function arguments that are not represented in the
       *       underlying map
       * 
       * @see #strict()
       */
      public FromMap<I, O> relaxed() {
         return new FromMap<I, O>(map, false);
      }

      /**
       * {@inheritDoc}
       * 
       * If the underlying map does not have an entry for the specified argument and this map is
       * {@linkplain #strict() strict} then a runtime exception will occur.
       * 
       * @throws IllegalArgumentException if the function cannot produce a value for the specified
       *       argument
       */
      @Override
      public O apply(I input) {
         O val = map.get(input);
         if (strict && val == null && !map.containsKey(input)) {
            throw new IllegalArgumentException("Function cannot produce value for specified input (" + input + ")");
         }
         return val;
      }
   }
}
