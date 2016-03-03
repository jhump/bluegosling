package com.bluegosling.collections.primitive;

import com.bluegosling.function.BooleanConsumer;
import com.bluegosling.function.BooleanPredicate;
import com.bluegosling.function.BooleanUnaryOperator;
import com.bluegosling.function.ByteConsumer;
import com.bluegosling.function.BytePredicate;
import com.bluegosling.function.ByteUnaryOperator;
import com.bluegosling.function.CharConsumer;
import com.bluegosling.function.CharPredicate;
import com.bluegosling.function.CharUnaryOperator;
import com.bluegosling.function.FloatConsumer;
import com.bluegosling.function.FloatPredicate;
import com.bluegosling.function.FloatUnaryOperator;
import com.bluegosling.function.ShortConsumer;
import com.bluegosling.function.ShortPredicate;
import com.bluegosling.function.ShortUnaryOperator;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Primitive specializations of the {@link List} interface.
 *
 * @param <T> the reference type (e.g. "boxed" type) for elements in the list
 * @param <T_CONS> a primitive specialization of {@link Consumer} for the element type
 * @param <T_ITER> a primitive specialization of {@link Iterator} for the element type
 * @param <T_LISTITER> a primitive specialization of {@link ListIterator} for the element type
 * @param <T_LIST> a self reference to this primitive specialization of {@link List}
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PrimitiveList<T, T_CONS, T_ITER extends PrimitiveIterator<T, T_CONS>,
         T_LISTITER extends PrimitiveListIterator<T, T_CONS>,
         T_LIST extends PrimitiveList<T, T_CONS, T_ITER, T_LISTITER, T_LIST>>
      extends List<T> {
   
   @Override T_ITER iterator();
            
   @Override T_LISTITER listIterator();
   
   @Override T_LISTITER listIterator(int startIndex);
   
   @Override T_LIST subList(int fromIndex, int toIndex);
   
   default void forEach(T_CONS action) {
      iterator().forEachRemaining(action);
   }
   
   interface OfBoolean extends PrimitiveList<Boolean, BooleanConsumer, BooleanIterator,
         PrimitiveListIterator.OfBoolean, OfBoolean> {
      boolean getBoolean(int index);
      void addBoolean(boolean value);
      void addBoolean(int index, boolean value);
      boolean setBoolean(int index, boolean value);
      boolean containsBoolean(boolean value);
      int indexOfBoolean(boolean value);
      int lastIndexOfBoolean(boolean value);
      boolean[] toBooleanArray();
      boolean removeBoolean(int index);
      boolean removeIf(BooleanPredicate filter);
      void replaceAll(BooleanUnaryOperator operator);
   }

   interface OfByte extends PrimitiveList<Byte, ByteConsumer, ByteIterator,
         PrimitiveListIterator.OfByte, OfByte> {
      byte getByte(int index);
      void addByte(byte value);
      void addByte(int index, byte value);
      byte setByte(int index, byte value);
      boolean containsByte(byte value);
      int indexOfByte(byte value);
      int lastIndexOfByte(byte value);
      byte[] toByteArray();
      byte removeByte(int index);
      boolean removeIf(BytePredicate filter);
      void replaceAll(ByteUnaryOperator operator);
   }

   interface OfChar extends PrimitiveList<Character, CharConsumer, CharIterator,
         PrimitiveListIterator.OfChar, OfChar> {
      char getChar(int index);
      void addChar(char value);
      void addChar(int index, char value);
      char setChar(int index, char value);
      boolean containsChar(char value);
      int indexOfChar(char value);
      int lastIndexOfChar(char value);
      char[] toCharArray();
      char removeChar(int index);
      boolean removeIf(CharPredicate filter);
      void replaceAll(CharUnaryOperator operator);
   }

   interface OfShort extends PrimitiveList<Short, ShortConsumer, ShortIterator,
         PrimitiveListIterator.OfShort, OfShort> {
      short getShort(int index);
      void addShort(short value);
      void addShort(int index, short value);
      short setShort(int index, short value);
      boolean containsShort(short value);
      int indexOfShort(short value);
      int lastIndexOfShort(short value);
      short[] toShortArray();
      short removeShort(int index);
      boolean removeIf(ShortPredicate filter);
      void replaceAll(ShortUnaryOperator operator);
   }

   interface OfInt extends PrimitiveList<Integer, IntConsumer, PrimitiveIterator.OfInt,
         PrimitiveListIterator.OfInt, OfInt> {
      int getInt(int index);
      void addInt(int value);
      void addInt(int index, int value);
      int setInt(int index, int value);
      boolean containsInt(int value);
      int indexOfInt(int value);
      int lastIndexOfInt(int value);
      int[] toIntArray();
      int removeInt(int index);
      boolean removeIf(IntPredicate filter);
      void replaceAll(IntUnaryOperator operator);

      @Override Spliterator.OfInt spliterator();
      IntStream streamOfInt();
      IntStream parallelStreamOfInt();
   }

   interface OfLong extends PrimitiveList<Long, LongConsumer, PrimitiveIterator.OfLong,
         PrimitiveListIterator.OfLong, OfLong> {
      long getLong(int index);
      void addLong(long value);
      void addLong(int index, long value);
      long setLong(int index, long value);
      boolean containsLong(long value);
      int indexOfLong(long value);
      int lastIndexOfLong(long value);
      long[] toLongArray();
      long removeLong(int index);
      boolean removeIf(LongPredicate filter);
      void replaceAll(LongUnaryOperator operator);

      @Override Spliterator.OfLong spliterator();
      LongStream streamOfLong();
      LongStream parallelStreamOfLong();
   }

   interface OfFloat extends PrimitiveList<Float, FloatConsumer, FloatIterator,
         PrimitiveListIterator.OfFloat, OfFloat> {
      float getFloat(int index);
      void addFloat(float value);
      void addFloat(int index, float value);
      float setFloat(int index, float value);
      boolean containsFloat(float value);
      int indexOfFloat(float value);
      int lastIndexOfFloat(float value);
      float[] toFloatArray();
      float removeFloat(int index);
      boolean removeIf(FloatPredicate filter);
      void replaceAll(FloatUnaryOperator operator);
   }

   interface OfDouble extends PrimitiveList<Double, DoubleConsumer,
         PrimitiveIterator.OfDouble, PrimitiveListIterator.OfDouble, OfDouble> {
      double getDouble(int index);
      void addDouble(double value);
      void addDouble(int index, double value);
      double setDouble(int index, double value);
      boolean containsDouble(double value);
      int indexOfDouble(double value);
      int lastIndexOfDouble(double value);
      double[] toDoubleArray();
      double removeDouble(int index);
      boolean removeIf(DoublePredicate filter);
      void replaceAll(DoubleUnaryOperator operator);

      @Override Spliterator.OfDouble spliterator();
      DoubleStream streamOfDouble();
      DoubleStream parallelStreamOfDouble();
   }
}
