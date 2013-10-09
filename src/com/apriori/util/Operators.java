package com.apriori.util;

import java.util.ArrayList;
import java.util.List;


public final class Operators {
   private Operators() {
   }

   private static final Operator<String> STRING_CONCAT = new Operator<String>() {
      @Override public String apply(String input1, String input2) {
         return input1 + input2;
      }
   };

   private static final Operator<Object[]> ARRAY_CONCAT = new Operator<Object[]>() {
      @Override public Object[] apply(Object[] input1, Object[] input2) {
         Object result[] = new Object[input1.length + input2.length];
         System.arraycopy(input1, 0, result, 0, input1.length);
         System.arraycopy(input2, 0, result, input1.length, input2.length);
         return result;
      }
   };

   private static final Operator<List<?>> LIST_CONCAT = new Operator<List<?>>() {
      @Override public List<?> apply(List<?> input1, List<?> input2) {
         List<Object> result = new ArrayList<Object>(input1.size() + input2.size());
         result.addAll(input1);
         result.addAll(input2);
         return result;
      }
   };
   
   public static <T> Operator<T> fromFunction(
         final Function.Bivariate<? super T, ? super T, ? extends T> function) {
      return new Operator<T>() {
         @Override public T apply(T input1, T input2) {
            return function.apply(input1, input2);
         }
      };
   }
   
   // TODO: concat operators, maybe other arithmetic operators?
}
