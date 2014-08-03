package com.apriori.util;

import com.apriori.reflect.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;


public final class Operators {
   private Operators() {
   }

   public static <T> BinaryOperator<List<T>> concatList() {
      return (l1, l2) -> {
         List<T> result = new ArrayList<>(l1.size() + l2.size());
         result.addAll(l1);
         result.addAll(l2);
         return result;
      };
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

   /*
    * The code in concatArray will work for primitive element types, too. But the generic
    * signature won't work because primitive arrays don't extend Object[]. Thanks to erasure,
    * we can make it work below with some raw type + unchecked cast trickery.
    */

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<boolean[]> concatBooleanArray() {
      return (BinaryOperator) concatArray(boolean.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<byte[]> concatByteArray() {
      return (BinaryOperator) concatArray(byte.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<char[]> concatCharArray() {
      return (BinaryOperator) concatArray(char.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<short[]> concatShortArray() {
      return (BinaryOperator) concatArray(short.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<int[]> concatIntArray() {
      return (BinaryOperator) concatArray(int.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<long[]> concatLongArray() {
      return (BinaryOperator) concatArray(long.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<float[]> concatFloatArray() {
      return (BinaryOperator) concatArray(float.class);
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static BinaryOperator<double[]> concatDoubleArray() {
      return (BinaryOperator) concatArray(double.class);
   }
}
