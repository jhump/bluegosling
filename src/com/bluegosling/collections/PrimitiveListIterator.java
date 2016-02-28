package com.bluegosling.collections;

import com.bluegosling.function.BooleanConsumer;
import com.bluegosling.function.ByteConsumer;
import com.bluegosling.function.CharConsumer;
import com.bluegosling.function.FloatConsumer;
import com.bluegosling.function.ShortConsumer;

import java.util.ListIterator;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

// TODO: javadoc
public interface PrimitiveListIterator<T, T_CONS>
      extends PrimitiveIterator<T, T_CONS>, ListIterator<T> {

   interface OfBoolean extends PrimitiveListIterator<Boolean, BooleanConsumer>, BooleanIterator {
      @Override
      default Boolean next() {
         return nextBoolean();
      }

      boolean previousBoolean();
      
      @Override
      default Boolean previous() {
         return previousBoolean();
      }
   }

   interface OfByte extends PrimitiveListIterator<Byte, ByteConsumer>, ByteIterator {
      @Override
      default Byte next() {
         return nextByte();
      }

      byte previousByte();
      
      @Override
      default Byte previous() {
         return previousByte();
      }
   }

   interface OfChar extends PrimitiveListIterator<Character, CharConsumer>, CharIterator {
      @Override
      default Character next() {
         return nextChar();
      }

      char previousChar();
      
      @Override
      default Character previous() {
         return previousChar();
      }
   }

   interface OfShort extends PrimitiveListIterator<Short, ShortConsumer>, ShortIterator {
      @Override
      default Short next() {
         return nextShort();
      }

      short previousShort();
      
      @Override
      default Short previous() {
         return previousShort();
      }
   }

   interface OfInt extends PrimitiveListIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt {
      @Override
      default Integer next() {
         return nextInt();
      }

      int previousInt();
      
      @Override
      default Integer previous() {
         return previousInt();
      }
   }

   interface OfLong extends PrimitiveListIterator<Long, LongConsumer>, PrimitiveIterator.OfLong {
      @Override
      default Long next() {
         return nextLong();
      }

      long previousLong();
      
      @Override
      default Long previous() {
         return previousLong();
      }
   }

   interface OfFloat extends PrimitiveListIterator<Float, FloatConsumer>, FloatIterator {
      @Override
      default Float next() {
         return nextFloat();
      }

      float previousFloat();
      
      @Override
      default Float previous() {
         return previousFloat();
      }
   }

   interface OfDouble
         extends PrimitiveListIterator<Double, DoubleConsumer>, PrimitiveIterator.OfDouble {
      @Override
      default Double next() {
         return nextDouble();
      }

      double previousDouble();
      
      @Override
      default Double previous() {
         return previousDouble();
      }
   }
}
