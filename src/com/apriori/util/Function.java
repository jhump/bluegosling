package com.apriori.util;

import java.util.Map;

// TODO: javadoc
public interface Function<I, O> {
   O apply(I input);
   interface Bivariate<I1, I2, O> {
      O apply(I1 input1, I2 input);
   }
   interface Trivariate<I1, I2, I3, O> {
      O apply(I1 input1, I2 input2, I3 input3);
   }
   
   public static class FromMap<I, O> implements Function<I, O> {
      private final Map<I, O> map;
      private final boolean strict;
      
      public FromMap(Map<I, O> map) {
         this(map, false);
      }
      
      private FromMap(Map<I, O> map, boolean strict) {
         this.map = map;
         this.strict = strict;
      }
      
      public Function<I, O> strict() {
         return new FromMap<I, O>(map, true);
      }

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
