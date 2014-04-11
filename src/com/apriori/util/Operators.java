package com.apriori.util;

import com.apriori.reflect.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;


public final class Operators {
   private Operators() {
   }

   public static BinaryOperator<Object[]> concatArray() {
      return (a1, a2) -> {
         Object result[] = new Object[a1.length + a1.length];
         System.arraycopy(a1, 0, result, 0, a1.length);
         System.arraycopy(a2, 0, result, a1.length, a2.length);
         return result;
      };
   }

   public static <T> BinaryOperator<T[]> concatArray(Class<T> elementType) {
      return (a1, a2) -> {
         T result[] = ArrayUtils.newInstance(elementType, a1.length + a1.length);
         System.arraycopy(a1, 0, result, 0, a1.length);
         System.arraycopy(a2, 0, result, a1.length, a2.length);
         return result;
      };
   }

   public static <T> BinaryOperator<List<T>> concatList() {
      return (l1, l2) -> {
         List<T> result = new ArrayList<>(l1.size() + l2.size());
         result.addAll(l1);
         result.addAll(l2);
         return result;
      };
   }
}
