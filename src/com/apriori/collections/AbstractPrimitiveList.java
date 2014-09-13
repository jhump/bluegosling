package com.apriori.collections;

import com.apriori.util.BooleanConsumer;
import com.apriori.util.BooleanPredicate;
import com.apriori.util.BooleanUnaryOperator;
import com.apriori.util.ByteConsumer;
import com.apriori.util.BytePredicate;
import com.apriori.util.ByteUnaryOperator;
import com.apriori.util.CharConsumer;
import com.apriori.util.CharPredicate;
import com.apriori.util.CharUnaryOperator;
import com.apriori.util.FloatConsumer;
import com.apriori.util.FloatPredicate;
import com.apriori.util.FloatUnaryOperator;
import com.apriori.util.ShortConsumer;
import com.apriori.util.ShortPredicate;
import com.apriori.util.ShortUnaryOperator;

import java.util.AbstractList;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
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
import java.util.stream.StreamSupport;

// TODO: javadoc
// TODO: tests
public abstract class AbstractPrimitiveList<T, T_CONS,
         T_ITER extends PrimitiveIterator<T, T_CONS>,
         T_LISTITER extends PrimitiveListIterator<T, T_CONS>,
         T_LIST extends PrimitiveList<T, T_CONS, T_ITER, T_LISTITER, T_LIST>>
      extends AbstractList<T> implements PrimitiveList<T, T_CONS, T_ITER, T_LISTITER, T_LIST> {
      
   @SuppressWarnings("unchecked")
   @Override public T_ITER iterator() {
      return (T_ITER) listIterator();
   }
   
   @Override public T_LISTITER listIterator() {
      return listIterator(0);
   }
   
   @Override public abstract T_LISTITER listIterator(int index);
   
   @Override public abstract T_LIST subList(int fromIndex, int toIndex);

   int modCount() {
      return modCount;
   }

   @Override
   protected void removeRange(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
   }

   void checkRange(int index) {
      if (index < 0 || index >= size()) {
         throw new IndexOutOfBoundsException();
      }
   }

   void checkRangeWide(int index) {
      if (index < 0 || index > size()) {
         throw new IndexOutOfBoundsException();
      }
   }

   /**
    * An abstract base class for implementations of {@link PrimitiveListIterator}.
    *
    * @param <T> the "boxed" type corresponding to the primitive element type
    * @param <T_CONS> a consumer interface for the primitive type
    * @param <T_LIST> the primitive list type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static abstract class AbstractListIterator<T, T_CONS,
            T_LIST extends AbstractPrimitiveList<T, T_CONS, ?, ?, ?>>
         implements PrimitiveListIterator<T, T_CONS> {

      T_LIST list;
      int modCount;
      int index;
      int lastFetched = -1;
      
      AbstractListIterator(T_LIST list, int index) {
         this.list = list;
         this.modCount = list.modCount();
         this.index = index;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         return index < list.size();
      }

      protected void afterNext() {
         lastFetched = index++;
      }
      
      @Override
      public boolean hasPrevious() {
         checkModCount();
         return index > 0;
      }

      protected void afterPrevious() {
         lastFetched = --index;
      }
      

      @Override
      public int nextIndex() {
         checkModCount();
         return index;
      }

      @Override
      public int previousIndex() {
         checkModCount();
         return index - 1;
      }

      @Override
      public void remove() {
         checkModCount();
         if (lastFetched == -1) {
            throw new IllegalStateException();
         }
         list.remove(lastFetched);
         this.modCount = list.modCount();
         if (lastFetched < index) {
            index--;
         }
         lastFetched = -1;
      }

      @Override
      public void set(T e) {
         checkModCount();
         if (lastFetched == -1) {
            throw new IllegalStateException();
         }
         list.set(lastFetched, e);
         this.modCount = list.modCount();
      }

      @Override
      public void add(T e) {
         checkModCount();
         list.add(index, e);
         this.modCount = list.modCount();
         index++;
         lastFetched = -1;
      }
   }
         
   /**
    * A specialization for lists of boolean elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfBoolean extends AbstractPrimitiveList<Boolean, BooleanConsumer,
            BooleanIterator, PrimitiveListIterator.OfBoolean, PrimitiveList.OfBoolean>
         implements PrimitiveList.OfBoolean {

      @Override
      public Boolean get(int index) {
         return getBoolean(index);
      }
      
      @Override
      public void addBoolean(boolean value) {
         addBoolean(size(), value);
      }
      
      @Override
      public boolean add(Boolean value) {
         addBoolean(value);
         return true;
      }

      @Override
      public void addBoolean(int index, boolean value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Boolean value) {
         addBoolean(index, value);
      }

      @Override
      public boolean setBoolean(int index, boolean value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Boolean set(int index, Boolean value) {
         return setBoolean(index, value);
      }

      @Override
      public boolean containsBoolean(boolean value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getBoolean(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Boolean && containsBoolean((Boolean) o);
      }

      @Override
      public int indexOfBoolean(boolean value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getBoolean(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Boolean ? indexOfBoolean((Boolean) o) : -1;
      }

      @Override
      public int lastIndexOfBoolean(boolean value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getBoolean(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Boolean ? lastIndexOfBoolean((Boolean) o) : -1;
      }

      @Override
      public boolean[] toBooleanArray() {
         int len = size();
         boolean ret[] = new boolean[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getBoolean(i);
         }
         return ret;
      }

      @Override
      public boolean removeBoolean(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Boolean remove(int index) {
         return removeBoolean(index);
      }
      
      @Override
      public PrimitiveListIterator.OfBoolean listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfBoolean subList(int fromIndex, int toIndex) {
         return new SubListOfBoolean(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(BooleanPredicate filter) {
         boolean ret = false;
         for (BooleanIterator iter = iterator(); iter.hasNext();) {
            boolean i = iter.nextBoolean();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(BooleanUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            boolean o = getBoolean(i);
            boolean n = operator.applyAsBoolean(o);
            if (o != n) {
               setBoolean(i, n);
            }
         }
      }
      
      /**
       * The boolean primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Boolean, BooleanConsumer,
               AbstractPrimitiveList.OfBoolean>
            implements PrimitiveListIterator.OfBoolean {

         Iter(int index) {
            super(AbstractPrimitiveList.OfBoolean.this, index);
         }

         @Override
         public boolean nextBoolean() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            boolean ret = list.getBoolean(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public boolean previousBoolean() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            boolean ret = list.getBoolean(previousIndex());
            afterPrevious();
            return ret;
         }
         
         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Boolean next() {
            return nextBoolean();
         }
         
         @Override
         public Boolean previous() {
            return previousBoolean();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive booleans.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfBoolean extends OfBoolean {

      private final AbstractPrimitiveList.OfBoolean list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfBoolean(AbstractPrimitiveList.OfBoolean list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean getBoolean(int index) {
         checkModCount();
         checkRange(index);
         return list.getBoolean(index + fromIndex);
      }
      
      @Override
      public boolean removeBoolean(int index) {
         checkModCount();
         checkRange(index);
         boolean ret = list.removeBoolean(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public boolean setBoolean(int index, boolean value) {
         checkModCount();
         checkRange(index);
         boolean ret = list.setBoolean(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addBoolean(int index, boolean value) {
         checkModCount();
         checkRange(index);
         list.addBoolean(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfBoolean subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfBoolean(list, from + fromIndex, to + fromIndex);
      }
   }

   /**
    * A specialization for lists of byte elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfByte extends AbstractPrimitiveList<Byte, ByteConsumer,
            ByteIterator, PrimitiveListIterator.OfByte, PrimitiveList.OfByte>
         implements PrimitiveList.OfByte {

      @Override
      public Byte get(int index) {
         return getByte(index);
      }
      
      @Override
      public void addByte(byte value) {
         addByte(size(), value);
      }
      
      @Override
      public boolean add(Byte value) {
         addByte(value);
         return true;
      }

      @Override
      public void addByte(int index, byte value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Byte value) {
         addByte(index, value);
      }

      @Override
      public byte setByte(int index, byte value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Byte set(int index, Byte value) {
         return setByte(index, value);
      }

      @Override
      public boolean containsByte(byte value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getByte(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Byte && containsByte((Byte) o);
      }

      @Override
      public int indexOfByte(byte value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getByte(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Byte ? indexOfByte((Byte) o) : -1;
      }

      @Override
      public int lastIndexOfByte(byte value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getByte(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Byte ? lastIndexOfByte((Byte) o) : -1;
      }

      @Override
      public byte[] toByteArray() {
         int len = size();
         byte ret[] = new byte[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getByte(i);
         }
         return ret;
      }

      @Override
      public byte removeByte(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Byte remove(int index) {
         return removeByte(index);
      }
      
      @Override
      public PrimitiveListIterator.OfByte listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfByte subList(int fromIndex, int toIndex) {
         return new SubListOfByte(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(BytePredicate filter) {
         boolean ret = false;
         for (ByteIterator iter = iterator(); iter.hasNext();) {
            byte i = iter.nextByte();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(ByteUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            byte o = getByte(i);
            byte n = operator.applyAsByte(o);
            if (o != n) {
               setByte(i, n);
            }
         }
      }

      /**
       * The byte primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Byte, ByteConsumer,
               AbstractPrimitiveList.OfByte>
            implements PrimitiveListIterator.OfByte {

         Iter(int index) {
            super(AbstractPrimitiveList.OfByte.this, index);
         }

         @Override
         public byte nextByte() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            byte ret = list.getByte(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public byte previousByte() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            byte ret = list.getByte(previousIndex());
            afterPrevious();
            return ret;
         }
         
         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Byte next() {
            return nextByte();
         }
         
         @Override
         public Byte previous() {
            return previousByte();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive bytes.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfByte extends OfByte {

      private final AbstractPrimitiveList.OfByte list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfByte(AbstractPrimitiveList.OfByte list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public byte getByte(int index) {
         checkModCount();
         checkRange(index);
         return list.getByte(index + fromIndex);
      }
      
      @Override
      public byte removeByte(int index) {
         checkModCount();
         checkRange(index);
         byte ret = list.removeByte(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public byte setByte(int index, byte value) {
         checkModCount();
         checkRange(index);
         byte ret = list.setByte(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addByte(int index, byte value) {
         checkModCount();
         checkRange(index);
         list.addByte(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfByte subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfByte(list, from + fromIndex, to + fromIndex);
      }
   }
   
   /**
    * A specialization for lists of char elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfChar extends AbstractPrimitiveList<Character, CharConsumer,
            CharIterator, PrimitiveListIterator.OfChar, PrimitiveList.OfChar>
         implements PrimitiveList.OfChar {

      @Override
      public Character get(int index) {
         return getChar(index);
      }
      
      @Override
      public void addChar(char value) {
         addChar(size(), value);
      }
      
      @Override
      public boolean add(Character value) {
         addChar(value);
         return true;
      }

      @Override
      public void addChar(int index, char value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Character value) {
         addChar(index, value);
      }

      @Override
      public char setChar(int index, char value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Character set(int index, Character value) {
         return setChar(index, value);
      }

      @Override
      public boolean containsChar(char value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getChar(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Character && containsChar((Character) o);
      }

      @Override
      public int indexOfChar(char value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getChar(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Character ? indexOfChar((Character) o) : -1;
      }

      @Override
      public int lastIndexOfChar(char value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getChar(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Character ? lastIndexOfChar((Character) o) : -1;
      }

      @Override
      public char[] toCharArray() {
         int len = size();
         char ret[] = new char[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getChar(i);
         }
         return ret;
      }

      @Override
      public char removeChar(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Character remove(int index) {
         return removeChar(index);
      }
      
      @Override
      public PrimitiveListIterator.OfChar listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfChar subList(int fromIndex, int toIndex) {
         return new SubListOfChar(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(CharPredicate filter) {
         boolean ret = false;
         for (CharIterator iter = iterator(); iter.hasNext();) {
            char i = iter.nextChar();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(CharUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            char o = getChar(i);
            char n = operator.applyAsChar(o);
            if (o != n) {
               setChar(i, n);
            }
         }
      }

      /**
       * The char primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Character, CharConsumer,
               AbstractPrimitiveList.OfChar>
            implements PrimitiveListIterator.OfChar {

         Iter(int index) {
            super(AbstractPrimitiveList.OfChar.this, index);
         }

         @Override
         public char nextChar() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            char ret = list.getChar(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public char previousChar() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            char ret = list.getChar(previousIndex());
            afterPrevious();
            return ret;
         }

         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Character next() {
            return nextChar();
         }
         
         @Override
         public Character previous() {
            return previousChar();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive chars.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfChar extends OfChar {

      private final AbstractPrimitiveList.OfChar list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfChar(AbstractPrimitiveList.OfChar list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public char getChar(int index) {
         checkModCount();
         checkRange(index);
         return list.getChar(index + fromIndex);
      }
      
      @Override
      public char removeChar(int index) {
         checkModCount();
         checkRange(index);
         char ret = list.removeChar(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public char setChar(int index, char value) {
         checkModCount();
         checkRange(index);
         char ret = list.setChar(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addChar(int index, char value) {
         checkModCount();
         checkRange(index);
         list.addChar(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfChar subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfChar(list, from + fromIndex, to + fromIndex);
      }
   }
   
   /**
    * A specialization for lists of short elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfShort extends AbstractPrimitiveList<Short, ShortConsumer,
            ShortIterator, PrimitiveListIterator.OfShort, PrimitiveList.OfShort>
         implements PrimitiveList.OfShort {

      @Override
      public Short get(int index) {
         return getShort(index);
      }
      
      @Override
      public void addShort(short value) {
         addShort(size(), value);
      }
      
      @Override
      public boolean add(Short value) {
         addShort(value);
         return true;
      }

      @Override
      public void addShort(int index, short value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Short value) {
         addShort(index, value);
      }

      @Override
      public short setShort(int index, short value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Short set(int index, Short value) {
         return setShort(index, value);
      }

      @Override
      public boolean containsShort(short value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getShort(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Short && containsShort((Short) o);
      }

      @Override
      public int indexOfShort(short value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getShort(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Short ? indexOfShort((Short) o) : -1;
      }

      @Override
      public int lastIndexOfShort(short value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getShort(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Short ? lastIndexOfShort((Short) o) : -1;
      }

      @Override
      public short[] toShortArray() {
         int len = size();
         short ret[] = new short[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getShort(i);
         }
         return ret;
      }

      @Override
      public short removeShort(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Short remove(int index) {
         return removeShort(index);
      }
      
      @Override
      public PrimitiveListIterator.OfShort listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfShort subList(int fromIndex, int toIndex) {
         return new SubListOfShort(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(ShortPredicate filter) {
         boolean ret = false;
         for (ShortIterator iter = iterator(); iter.hasNext();) {
            short i = iter.nextShort();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(ShortUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            short o = getShort(i);
            short n = operator.applyAsShort(o);
            if (o != n) {
               setShort(i, n);
            }
         }
      }

      /**
       * The short primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Short, ShortConsumer,
               AbstractPrimitiveList.OfShort>
            implements PrimitiveListIterator.OfShort {

         Iter(int index) {
            super(AbstractPrimitiveList.OfShort.this, index);
         }

         @Override
         public short nextShort() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            short ret = list.getShort(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public short previousShort() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            short ret = list.getShort(previousIndex());
            afterPrevious();
            return ret;
         }
         
         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Short next() {
            return nextShort();
         }
         
         @Override
         public Short previous() {
            return previousShort();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive shorts.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfShort extends OfShort {

      private final AbstractPrimitiveList.OfShort list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfShort(AbstractPrimitiveList.OfShort list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public short getShort(int index) {
         checkModCount();
         checkRange(index);
         return list.getShort(index + fromIndex);
      }
      
      @Override
      public short removeShort(int index) {
         checkModCount();
         checkRange(index);
         short ret = list.removeShort(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public short setShort(int index, short value) {
         checkModCount();
         checkRange(index);
         short ret = list.setShort(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addShort(int index, short value) {
         checkModCount();
         checkRange(index);
         list.addShort(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }
      
      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfShort subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfShort(list, from + fromIndex, to + fromIndex);
      }
   }

   /**
    * A specialization for lists of int elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfInt extends AbstractPrimitiveList<Integer, IntConsumer,
            PrimitiveIterator.OfInt, PrimitiveListIterator.OfInt, PrimitiveList.OfInt>
         implements PrimitiveList.OfInt {

      @Override
      public Integer get(int index) {
         return getInt(index);
      }
      
      @Override
      public void addInt(int value) {
         addInt(size(), value);
      }
      
      @Override
      public boolean add(Integer value) {
         addInt(value);
         return true;
      }

      @Override
      public void addInt(int index, int value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Integer value) {
         addInt(index, value);
      }

      @Override
      public int setInt(int index, int value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Integer set(int index, Integer value) {
         return setInt(index, value);
      }

      @Override
      public boolean containsInt(int value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getInt(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Integer && containsInt((Integer) o);
      }

      @Override
      public int indexOfInt(int value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getInt(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Integer ? indexOfInt((Integer) o) : -1;
      }

      @Override
      public int lastIndexOfInt(int value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getInt(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Integer ? lastIndexOfInt((Integer) o) : -1;
      }

      @Override
      public int[] toIntArray() {
         int len = size();
         int ret[] = new int[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getInt(i);
         }
         return ret;
      }

      @Override
      public int removeInt(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Integer remove(int index) {
         return removeInt(index);
      }
      
      @Override
      public PrimitiveListIterator.OfInt listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfInt subList(int fromIndex, int toIndex) {
         return new SubListOfInt(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(IntPredicate filter) {
         boolean ret = false;
         for (PrimitiveIterator.OfInt iter = iterator(); iter.hasNext();) {
            int i = iter.nextInt();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(IntUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            int o = getInt(i);
            int n = operator.applyAsInt(o);
            if (o != n) {
               setInt(i, n);
            }
         }
      }

      @Override
      public Spliterator.OfInt spliterator() {
         return Spliterators.spliterator(iterator(), size(), 0);
      }

      @Override
      public IntStream streamOfInt() {
         return StreamSupport.intStream(spliterator(), false);
      }

      @Override
      public IntStream parallelStreamOfInt() {
         return StreamSupport.intStream(spliterator(), true);
      }
      
      /**
       * The int primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Integer, IntConsumer,
               AbstractPrimitiveList.OfInt>
            implements PrimitiveListIterator.OfInt {

         Iter(int index) {
            super(AbstractPrimitiveList.OfInt.this, index);
         }

         @Override
         public int nextInt() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            int ret = list.getInt(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public int previousInt() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            int ret = list.getInt(previousIndex());
            afterPrevious();
            return ret;
         }

         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Integer next() {
            return nextInt();
         }
         
         @Override
         public Integer previous() {
            return previousInt();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive ints.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfInt extends OfInt {

      private final AbstractPrimitiveList.OfInt list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfInt(AbstractPrimitiveList.OfInt list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public int getInt(int index) {
         checkModCount();
         checkRange(index);
         return list.getInt(index + fromIndex);
      }
      
      @Override
      public int removeInt(int index) {
         checkModCount();
         checkRange(index);
         int ret = list.removeInt(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public int setInt(int index, int value) {
         checkModCount();
         checkRange(index);
         int ret = list.setInt(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addInt(int index, int value) {
         checkModCount();
         checkRange(index);
         list.addInt(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveListIterator.OfInt listIterator(int index) {
         checkModCount();
         return super.listIterator(index);
      }

      @Override
      public PrimitiveList.OfInt subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfInt(list, from + fromIndex, to + fromIndex);
      }
   }
   
   /**
    * A specialization for lists of long elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfLong extends AbstractPrimitiveList<Long, LongConsumer,
            PrimitiveIterator.OfLong, PrimitiveListIterator.OfLong, PrimitiveList.OfLong>
         implements PrimitiveList.OfLong {

      @Override
      public Long get(int index) {
         return getLong(index);
      }
      
      @Override
      public void addLong(long value) {
         addLong(size(), value);
      }
      
      @Override
      public boolean add(Long value) {
         addLong(value);
         return true;
      }

      @Override
      public void addLong(int index, long value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Long value) {
         addLong(index, value);
      }

      @Override
      public long setLong(int index, long value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Long set(int index, Long value) {
         return setLong(index, value);
      }

      @Override
      public boolean containsLong(long value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getLong(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Long && containsLong((Long) o);
      }

      @Override
      public int indexOfLong(long value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getLong(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Long ? indexOfLong((Long) o) : -1;
      }

      @Override
      public int lastIndexOfLong(long value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getLong(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Long ? lastIndexOfLong((Long) o) : -1;
      }

      @Override
      public long[] toLongArray() {
         int len = size();
         long ret[] = new long[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getLong(i);
         }
         return ret;
      }

      @Override
      public long removeLong(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Long remove(int index) {
         return removeLong(index);
      }
      
      @Override
      public PrimitiveListIterator.OfLong listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfLong subList(int fromIndex, int toIndex) {
         return new SubListOfLong(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(LongPredicate filter) {
         boolean ret = false;
         for (PrimitiveIterator.OfLong iter = iterator(); iter.hasNext();) {
            long i = iter.nextLong();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(LongUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            long o = getLong(i);
            long n = operator.applyAsLong(o);
            if (o != n) {
               setLong(i, n);
            }
         }
      }

      @Override
      public Spliterator.OfLong spliterator() {
         return Spliterators.spliterator(iterator(), size(), 0);
      }

      @Override
      public LongStream streamOfLong() {
         return StreamSupport.longStream(spliterator(), false);
      }

      @Override
      public LongStream parallelStreamOfLong() {
         return StreamSupport.longStream(spliterator(), true);
      }
      
      /**
       * The long primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Long, LongConsumer,
               AbstractPrimitiveList.OfLong>
            implements PrimitiveListIterator.OfLong {

         Iter(int index) {
            super(AbstractPrimitiveList.OfLong.this, index);
         }

         @Override
         public long nextLong() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            long ret = list.getLong(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public long previousLong() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            long ret = list.getLong(previousIndex());
            afterPrevious();
            return ret;
         }
         
         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Long next() {
            return nextLong();
         }
         
         @Override
         public Long previous() {
            return previousLong();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive longs.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfLong extends OfLong {

      private final AbstractPrimitiveList.OfLong list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfLong(AbstractPrimitiveList.OfLong list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public long getLong(int index) {
         checkModCount();
         checkRange(index);
         return list.getLong(index + fromIndex);
      }
      
      @Override
      public long removeLong(int index) {
         checkModCount();
         checkRange(index);
         long ret = list.removeLong(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public long setLong(int index, long value) {
         checkModCount();
         checkRange(index);
         long ret = list.setLong(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addLong(int index, long value) {
         checkModCount();
         checkRange(index);
         list.addLong(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfLong subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfLong(list, from + fromIndex, to + fromIndex);
      }
   }
   
   /**
    * A specialization for lists of float elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfFloat extends AbstractPrimitiveList<Float, FloatConsumer,
            FloatIterator, PrimitiveListIterator.OfFloat, PrimitiveList.OfFloat>
         implements PrimitiveList.OfFloat {

      @Override
      public Float get(int index) {
         return getFloat(index);
      }
      
      @Override
      public void addFloat(float value) {
         addFloat(size(), value);
      }
      
      @Override
      public boolean add(Float value) {
         addFloat(value);
         return true;
      }

      @Override
      public void addFloat(int index, float value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Float value) {
         addFloat(index, value);
      }

      @Override
      public float setFloat(int index, float value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Float set(int index, Float value) {
         return setFloat(index, value);
      }

      @Override
      public boolean containsFloat(float value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getFloat(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Float && containsFloat((Float) o);
      }

      @Override
      public int indexOfFloat(float value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getFloat(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Float ? indexOfFloat((Float) o) : -1;
      }

      @Override
      public int lastIndexOfFloat(float value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getFloat(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Float ? lastIndexOfFloat((Float) o) : -1;
      }

      @Override
      public float[] toFloatArray() {
         int len = size();
         float ret[] = new float[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getFloat(i);
         }
         return ret;
      }

      @Override
      public float removeFloat(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Float remove(int index) {
         return removeFloat(index);
      }
      
      @Override
      public PrimitiveListIterator.OfFloat listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfFloat subList(int fromIndex, int toIndex) {
         return new SubListOfFloat(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(FloatPredicate filter) {
         boolean ret = false;
         for (FloatIterator iter = iterator(); iter.hasNext();) {
            float i = iter.nextFloat();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(FloatUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            float o = getFloat(i);
            float n = operator.applyAsFloat(o);
            if (o != n) {
               setFloat(i, n);
            }
         }
      }

      /**
       * The float primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Float, FloatConsumer,
               AbstractPrimitiveList.OfFloat>
            implements PrimitiveListIterator.OfFloat {

         Iter(int index) {
            super(AbstractPrimitiveList.OfFloat.this, index);
         }

         @Override
         public float nextFloat() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            float ret = list.getFloat(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public float previousFloat() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            float ret = list.getFloat(previousIndex());
            afterPrevious();
            return ret;
         }
         
         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Float next() {
            return nextFloat();
         }
         
         @Override
         public Float previous() {
            return previousFloat();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive floats.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfFloat extends OfFloat {

      private final AbstractPrimitiveList.OfFloat list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfFloat(AbstractPrimitiveList.OfFloat list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public float getFloat(int index) {
         checkModCount();
         checkRange(index);
         return list.getFloat(index + fromIndex);
      }
      
      @Override
      public float removeFloat(int index) {
         checkModCount();
         checkRange(index);
         float ret = list.removeFloat(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public float setFloat(int index, float value) {
         checkModCount();
         checkRange(index);
         float ret = list.setFloat(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addFloat(int index, float value) {
         checkModCount();
         checkRange(index);
         list.addFloat(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfFloat subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfFloat(list, from + fromIndex, to + fromIndex);
      }
   }

   /**
    * A specialization for lists of double elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class OfDouble extends AbstractPrimitiveList<Double, DoubleConsumer,
            PrimitiveIterator.OfDouble, PrimitiveListIterator.OfDouble, PrimitiveList.OfDouble>
         implements PrimitiveList.OfDouble {

      @Override
      public Double get(int index) {
         return getDouble(index);
      }
      
      @Override
      public void addDouble(double value) {
         addDouble(size(), value);
      }
      
      @Override
      public boolean add(Double value) {
         addDouble(value);
         return true;
      }

      @Override
      public void addDouble(int index, double value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public void add(int index, Double value) {
         addDouble(index, value);
      }

      @Override
      public double setDouble(int index, double value) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Double set(int index, Double value) {
         return setDouble(index, value);
      }

      @Override
      public boolean containsDouble(double value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getDouble(i) == value) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean contains(Object o) {
         return o instanceof Double && containsDouble((Double) o);
      }

      @Override
      public int indexOfDouble(double value) {
         for (int i = 0, len = size(); i < len; i++) {
            if (getDouble(i) == value) {
               return i;
            }
         }
         return -1;
      }
      
      @Override
      public int indexOf(Object o) {
         return o instanceof Double ? indexOfDouble((Double) o) : -1;
      }

      @Override
      public int lastIndexOfDouble(double value) {
         for (int i = size() - 1; i >= 0; i--) {
            if (getDouble(i) == value) {
               return i;
            }
         }
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return o instanceof Double ? lastIndexOfDouble((Double) o) : -1;
      }

      @Override
      public double[] toDoubleArray() {
         int len = size();
         double ret[] = new double[size()];
         for (int i = 0; i < len; i++) {
            ret[i] = getDouble(i);
         }
         return ret;
      }

      @Override
      public double removeDouble(int index) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public Double remove(int index) {
         return removeDouble(index);
      }
      
      @Override
      public PrimitiveListIterator.OfDouble listIterator(int index) {
         checkRangeWide(index);
         return new Iter(index);
      }

      @Override
      public PrimitiveList.OfDouble subList(int fromIndex, int toIndex) {
         return new SubListOfDouble(this, fromIndex, toIndex);
      }
      
      @Override
      public boolean removeIf(DoublePredicate filter) {
         boolean ret = false;
         for (PrimitiveIterator.OfDouble iter = iterator(); iter.hasNext();) {
            double i = iter.nextDouble();
            if (filter.test(i)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void replaceAll(DoubleUnaryOperator operator) {
         for (int i = 0, len = size(); i < len; i++) {
            double o = getDouble(i);
            double n = operator.applyAsDouble(o);
            if (o != n) {
               setDouble(i, n);
            }
         }
      }

      @Override
      public Spliterator.OfDouble spliterator() {
         return Spliterators.spliterator(iterator(), size(), 0);
      }

      @Override
      public DoubleStream streamOfDouble() {
         return StreamSupport.doubleStream(spliterator(), false);
      }

      @Override
      public DoubleStream parallelStreamOfDouble() {
         return StreamSupport.doubleStream(spliterator(), true);
      }
      
      /**
       * The double primitive list iterator.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter extends AbstractListIterator<Double, DoubleConsumer,
               AbstractPrimitiveList.OfDouble>
            implements PrimitiveListIterator.OfDouble {

         Iter(int index) {
            super(AbstractPrimitiveList.OfDouble.this, index);
         }

         @Override
         public double nextDouble() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            double ret = list.getDouble(nextIndex());
            afterNext();
            return ret;
         }

         @Override
         public double previousDouble() {
            if (!hasPrevious()) {
               throw new NoSuchElementException();
            }
            double ret = list.getDouble(previousIndex());
            afterPrevious();
            return ret;
         }

         /*
          * Having to re-define these, even though they are defined as default methods, because
          * otherwise compiler fails to synthesize bridge methods. AbstractMethodErrors can
          * result...
          */
         
         @Override
         public Double next() {
            return nextDouble();
         }
         
         @Override
         public Double previous() {
            return previousDouble();
         }
      }
   }

   /**
    * A sub-list view of a list of primitive doubles.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SubListOfDouble extends OfDouble {

      private final AbstractPrimitiveList.OfDouble list;
      private final int fromIndex;
      private int toIndex;
      
      SubListOfDouble(AbstractPrimitiveList.OfDouble list, int fromIndex, int toIndex) {
         this.list = list;
         this.modCount = list.modCount();
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
      }
      
      void checkModCount() {
         if (list.modCount() != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public double getDouble(int index) {
         checkModCount();
         checkRange(index);
         return list.getDouble(index + fromIndex);
      }
      
      @Override
      public double removeDouble(int index) {
         checkModCount();
         checkRange(index);
         double ret = list.removeDouble(index + fromIndex);
         toIndex--;
         modCount = list.modCount();
         return ret;
      }

      @Override
      public double setDouble(int index, double value) {
         checkModCount();
         checkRange(index);
         double ret = list.setDouble(index + fromIndex, value);
         modCount = list.modCount();
         return ret;
      }

      @Override
      public void addDouble(int index, double value) {
         checkModCount();
         checkRange(index);
         list.addDouble(index + fromIndex, value);
         toIndex++;
         modCount = list.modCount();
      }

      @Override
      public int size() {
         checkModCount();
         return toIndex - fromIndex;
      }

      @Override
      public void clear() {
         checkModCount();
         list.removeRange(fromIndex, toIndex);
      }
      
      @Override
      public PrimitiveList.OfDouble subList(int from, int to) {
         checkModCount();
         checkRangeWide(from);
         checkRangeWide(to);
         return new SubListOfDouble(list, from + fromIndex, to + fromIndex);
      }
   }
}
