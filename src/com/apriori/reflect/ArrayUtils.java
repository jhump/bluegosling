package com.apriori.reflect;

import com.apriori.collections.AbstractPrimitiveList;
import com.apriori.collections.PrimitiveList;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

// TODO: javadoc
// TODO: tests
public final class ArrayUtils {
   private ArrayUtils() {
   }
   
   public static <T> T[] newInstance(Class<T> elementType, int len) {
      if (elementType.isPrimitive()) {
         throw new IllegalArgumentException("element type cannot be primitive: " + elementType);
      }
      @SuppressWarnings("unchecked")
      T ret[] = (T[]) Array.newInstance(elementType, len);
      return ret;
   }
   
   public static List<?> asList(Object array) {
      Class<?> arrayType = array.getClass();
      if (!arrayType.isArray()) {
         throw new IllegalArgumentException("specified object is not an array");
      }
      Class<?> componentType = arrayType.getComponentType();
      if (componentType.isPrimitive()) {
         if (componentType == boolean.class) {
            return new BooleanList((boolean[]) array);
         } else if (componentType == byte.class) {
            return new ByteList((byte[]) array);
         } else if (componentType == char.class) {
            return new CharList((char[]) array);
         } else if (componentType == short.class) {
            return new ShortList((short[]) array);
         } else if (componentType == int.class) {
            return new IntList((int[]) array);
         } else if (componentType == long.class) {
            return new LongList((long[]) array);
         } else if (componentType == float.class) {
            return new FloatList((float[]) array);
         } else if (componentType == double.class) {
            return new DoubleList((double[]) array);
         } else {
            throw new AssertionError("Unrecognized primitive type: " + componentType);
         }
      } else {
         // it's a reference type, so we can use Arrays#asList to wrap it
         return Arrays.asList((Object[]) array);
      }
   }
   
   public static PrimitiveList.OfBoolean asList(boolean... bools) {
      return new BooleanList(bools);
   }

   public static PrimitiveList.OfByte asList(byte... bools) {
      return new ByteList(bools);
   }

   public static PrimitiveList.OfChar asList(char... bools) {
      return new CharList(bools);
   }

   public static PrimitiveList.OfShort asList(short... bools) {
      return new ShortList(bools);
   }

   public static PrimitiveList.OfInt asList(int... bools) {
      return new IntList(bools);
   }

   public static PrimitiveList.OfLong asList(long... bools) {
      return new LongList(bools);
   }

   public static PrimitiveList.OfFloat asList(float... bools) {
      return new FloatList(bools);
   }

   public static PrimitiveList.OfDouble asList(double... bools) {
      return new DoubleList(bools);
   }
   
   public static boolean equals(Object array1, Object array2) {
      Class<?> arrayType = array1.getClass();
      if (!arrayType.isArray()) {
         throw new IllegalArgumentException("specified object is not an array");
      }
      Class<?> componentType = arrayType.getComponentType();
      if (componentType.isPrimitive()) {
         if (componentType == boolean.class) {
            return array2 instanceof boolean[]
                  && Arrays.equals((boolean[]) array1, (boolean[]) array2);
         } else if (componentType == byte.class) {
            return array2 instanceof byte[] && Arrays.equals((byte[]) array1, (byte[]) array2);
         } else if (componentType == char.class) {
            return array2 instanceof char[] && Arrays.equals((char[]) array1, (char[]) array2);
         } else if (componentType == short.class) {
            return array2 instanceof short[] && Arrays.equals((short[]) array1, (short[]) array2);
         } else if (componentType == int.class) {
            return array2 instanceof int[] && Arrays.equals((int[]) array1, (int[]) array2);
         } else if (componentType == long.class) {
            return array2 instanceof long[] && Arrays.equals((long[]) array1, (long[]) array2);
         } else if (componentType == float.class) {
            return array2 instanceof float[] && Arrays.equals((float[]) array1, (float[]) array2);
         } else if (componentType == double.class) {
            return array2 instanceof double[]
                  && Arrays.equals((double[]) array1, (double[]) array2);
         } else {
            throw new AssertionError("Unrecognized primitive type: " + componentType);
         }
      } else {
         return array2 instanceof Object[] && Arrays.equals((Object[]) array1, (Object[]) array2);
      }
   }

   public static int hashCode(Object array) {
      Class<?> arrayType = array.getClass();
      if (!arrayType.isArray()) {
         throw new IllegalArgumentException("specified object is not an array");
      }
      Class<?> componentType = arrayType.getComponentType();
      if (componentType.isPrimitive()) {
         if (componentType == boolean.class) {
            return Arrays.hashCode((boolean[]) array);
         } else if (componentType == byte.class) {
            return Arrays.hashCode((byte[]) array);
         } else if (componentType == char.class) {
            return Arrays.hashCode((char[]) array);
         } else if (componentType == short.class) {
            return Arrays.hashCode((short[]) array);
         } else if (componentType == int.class) {
            return Arrays.hashCode((int[]) array);
         } else if (componentType == long.class) {
            return Arrays.hashCode((long[]) array);
         } else if (componentType == float.class) {
            return Arrays.hashCode((float[]) array);
         } else if (componentType == double.class) {
            return Arrays.hashCode((double[]) array);
         } else {
            throw new AssertionError("Unrecognized primitive type: " + componentType);
         }
      } else {
         return Arrays.hashCode((Object[]) array);
      }
   }
   
   public static String toString(Object array) {
      Class<?> arrayType = array.getClass();
      if (!arrayType.isArray()) {
         throw new IllegalArgumentException("specified object is not an array");
      }
      Class<?> componentType = arrayType.getComponentType();
      if (componentType.isPrimitive()) {
         if (componentType == boolean.class) {
            return Arrays.toString((boolean[]) array);
         } else if (componentType == byte.class) {
            return Arrays.toString((byte[]) array);
         } else if (componentType == char.class) {
            return Arrays.toString((char[]) array);
         } else if (componentType == short.class) {
            return Arrays.toString((short[]) array);
         } else if (componentType == int.class) {
            return Arrays.toString((int[]) array);
         } else if (componentType == long.class) {
            return Arrays.toString((long[]) array);
         } else if (componentType == float.class) {
            return Arrays.toString((float[]) array);
         } else if (componentType == double.class) {
            return Arrays.toString((double[]) array);
         } else {
            throw new AssertionError("Unrecognized primitive type: " + componentType);
         }
      } else {
         return Arrays.toString((Object[]) array);
      }
   }
   
   private static class BooleanList extends AbstractPrimitiveList.OfBoolean {
      private final boolean array[];
      
      BooleanList(boolean array[]) {
         this.array = array;
      }
      
      @Override
      public boolean getBoolean(int index) {
         return array[index];
      }

      @Override
      public boolean setBoolean(int index, boolean value) {
         Boolean ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class ByteList extends AbstractPrimitiveList.OfByte {
      private final byte array[];
      
      ByteList(byte array[]) {
         this.array = array;
      }
      
      @Override
      public byte getByte(int index) {
         return array[index];
      }

      @Override
      public byte setByte(int index, byte value) {
         Byte ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class CharList extends AbstractPrimitiveList.OfChar {
      private final char array[];
      
      CharList(char array[]) {
         this.array = array;
      }
      
      @Override
      public char getChar(int index) {
         return array[index];
      }

      @Override
      public char setChar(int index, char value) {
         char ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class ShortList extends AbstractPrimitiveList.OfShort {
      private final short array[];
      
      ShortList(short array[]) {
         this.array = array;
      }
      
      @Override
      public short getShort(int index) {
         return array[index];
      }

      @Override
      public short setShort(int index, short value) {
         Short ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class IntList extends AbstractPrimitiveList.OfInt {
      private final int array[];
      
      IntList(int array[]) {
         this.array = array;
      }
      
      @Override
      public int getInt(int index) {
         return array[index];
      }

      @Override
      public int setInt(int index, int value) {
         int ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class LongList extends AbstractPrimitiveList.OfLong {
      private final long array[];
      
      LongList(long array[]) {
         this.array = array;
      }
      
      @Override
      public long getLong(int index) {
         return array[index];
      }

      @Override
      public long setLong(int index, long value) {
         long ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class FloatList extends AbstractPrimitiveList.OfFloat {
      private final float array[];
      
      FloatList(float array[]) {
         this.array = array;
      }
      
      @Override
      public float getFloat(int index) {
         return array[index];
      }

      @Override
      public float setFloat(int index, float value) {
         float ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }

   private static class DoubleList extends AbstractPrimitiveList.OfDouble {
      private final double array[];
      
      DoubleList(double array[]) {
         this.array = array;
      }
      
      @Override
      public double getDouble(int index) {
         return array[index];
      }

      @Override
      public double setDouble(int index, double value) {
         double ret = array[index];
         array[index] = value;
         return ret;
      }

      @Override
      public int size() {
         return array.length;
      }
   }
}
