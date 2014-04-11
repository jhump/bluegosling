package com.apriori.reflect;

import java.lang.reflect.Array;

// TODO: javadoc
// TODO: tests
public final class ArrayUtils {
   private ArrayUtils() {
   }
   
   public static <T> T[] newInstance(Class<T> elementType, int len) {
      @SuppressWarnings("unchecked")
      T ret[] = (T[]) Array.newInstance(elementType, len);
      return ret;
   }
}
