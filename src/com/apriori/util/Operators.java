package com.apriori.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;


public final class Operators {
   private Operators() {
   }

   private static final BinaryOperator<String> STRING_CONCAT = (s1, s2) -> s1 + s2;

   private static final BinaryOperator<Object[]> ARRAY_CONCAT = (a1, a2) -> {
      Object result[] = new Object[a1.length + a1.length];
      System.arraycopy(a1, 0, result, 0, a1.length);
      System.arraycopy(a2, 0, result, a1.length, a2.length);
      return result;
   };

   private static final BinaryOperator<List<?>> LIST_CONCAT = (l1, l2) -> {
      List<Object> result = new ArrayList<Object>(l1.size() + l2.size());
      result.addAll(l1);
      result.addAll(l2);
      return result;
   };
   
   // TODO: concat operators, maybe other arithmetic operators?
}
