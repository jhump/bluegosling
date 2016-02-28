package com.bluegosling.function;

import com.bluegosling.reflect.ArrayUtils;

import java.lang.reflect.Array;
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
   
   private static BinaryOperator<?> concatAnyArray(Class<?> elementType) {
      return (a1, a2) -> {
         int a1len = Array.getLength(a1);
         int a2len = Array.getLength(a2);
         Object result = ArrayUtils.newInstance(elementType, a1len + a2len);
         System.arraycopy(a1, 0, result, 0, a1len);
         System.arraycopy(a2, 0, result, a1len, a2len);
         return result;
      };
   }

   /*
    * The code in concatAnyArray will work for both reference and primitive element types. But a
    * signature with a generic array type can't work because primitive arrays extends Object, not
    * Object[]. Thanks to erasure, we can make it work with some unchecked-cast trickery.
    */

   @SuppressWarnings("unchecked")
   public static <T> BinaryOperator<T[]> concatArray(Class<T> elementType) {
      return (BinaryOperator<T[]>) concatAnyArray(elementType);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<boolean[]> concatBooleanArray() {
      return (BinaryOperator<boolean[]>) concatAnyArray(boolean.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<byte[]> concatByteArray() {
      return (BinaryOperator<byte[]>) concatAnyArray(byte.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<char[]> concatCharArray() {
      return (BinaryOperator<char[]>) concatAnyArray(char.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<short[]> concatShortArray() {
      return (BinaryOperator<short[]>) concatAnyArray(short.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<int[]> concatIntArray() {
      return (BinaryOperator<int[]>) concatAnyArray(int.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<long[]> concatLongArray() {
      return (BinaryOperator<long[]>) concatAnyArray(long.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<float[]> concatFloatArray() {
      return (BinaryOperator<float[]>) concatAnyArray(float.class);
   }

   @SuppressWarnings("unchecked")
   public static BinaryOperator<double[]> concatDoubleArray() {
      return (BinaryOperator<double[]>) concatAnyArray(double.class);
   }
}
