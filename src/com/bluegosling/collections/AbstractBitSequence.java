package com.bluegosling.collections;

import com.bluegosling.collections.primitive.BooleanIterator;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * An abstract base class for implementations of {@link BitSequence}. Concrete sub-classes need only
 * implement {@link #length()} and {@link #bitStream(int)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO tests
abstract class AbstractBitSequence implements BitSequence {
   private int hashCode;
   
   void rangeCheck(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index > length()) {
         throw new IndexOutOfBoundsException(index + " > " + length());
      }
   }
   
   void checkTupleLength(int i) {
      if (i < 1) {
         throw new IllegalArgumentException(i + " < 1");
      } else if (i > 64) {
         throw new IllegalArgumentException(i + " > 64");
      }
   }
   
   @Override public BooleanIterator iterator(int startIndex) {
      final BitStream stream = bitStream(startIndex);
      return new BooleanIterator() {
         @Override public boolean hasNext() {
            return stream.remaining() > 0;
         }

         @Override public boolean nextBoolean() {
            return stream.next();
         }
      };
   }

   @Override public PrimitiveIterator.OfLong bitTupleIterator(final int tupleSize, int startIndex,
         final BitOrder order) {
      checkTupleLength(tupleSize);
      final BitStream stream = bitStream(startIndex);
      if (tupleSize == 1) {
         return new PrimitiveIterator.OfLong() {
            @Override public boolean hasNext() {
               return stream.remaining() > 0;
            }

            @Override public long nextLong() {
               return stream.next() ? 1L : 0L;
            }
            
            @Override public void remove() {
               throw new UnsupportedOperationException();
            }
         };
      } else {
         return new PrimitiveIterator.OfLong() {
            @Override public boolean hasNext() {
               return stream.remaining() > 0;
            }

            @Override public long nextLong() {
               int remaining = stream.remaining();
               if (remaining == 0) {
                  throw new NoSuchElementException();
               }
               if (tupleSize > remaining) {
                  long val = stream.next(remaining, order);
                  if (order == BitOrder.MSB) {
                     val <<= tupleSize - remaining;
                  }
                  return val;
               } else {
                  return stream.next(tupleSize, order);
               }
            }
            
            @Override public void remove() {
               throw new UnsupportedOperationException();
            }
         };
      }
   }
   
   @Override public int hashCode() {
      if (hashCode == 0) {
         int result = 1;
         PrimitiveIterator.OfLong iter = bitTupleIterator(64);
         while (iter.hasNext()) {
            long element = iter.nextLong();
            int elementHash = (int)(element ^ (element >>> 32));
            result = 31 * result + elementHash;
         }
         hashCode = result ^ length();
      }
      return hashCode;
   }
   
   @Override public boolean equals(Object o) {
      if (o instanceof BitSequence) {
         BitSequence other = (BitSequence) o;
         if (length() != other.length()) {
            return false;
         }
         PrimitiveIterator.OfLong iter1 = bitTupleIterator(64);
         PrimitiveIterator.OfLong iter2 = other.bitTupleIterator(64);
         while (iter1.hasNext()) {
            if (iter1.nextLong() != iter2.nextLong()) {
               return false;
            }
         }
         return true;
      }
      return false;
   }
   
   /**
    * An abstract base class for {@link BitStream}s that are backed by an
    * {@link AbstractBitSequence}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   abstract class AbstractStream implements BitStream {

      void checkSequenceLength(int i) {
         if (i < 0) {
            throw new IllegalArgumentException(i + " < 0");
         } else if (i > remaining()) {
            throw new NoSuchElementException();
         }
      }

      void checkTupleLength(int i) {
         AbstractBitSequence.this.checkTupleLength(i);
         if (i > remaining()) {
            throw new NoSuchElementException();
         }
      }

      @Override
      public long next(int numberOfBits, BitOrder order) {
         long val = next(numberOfBits);
         if (order == BitOrder.MSB) {
            val = Long.reverse(val);
            if (numberOfBits < 64) {
               val >>= 64 - numberOfBits;
            }
         }
         return val;
      }
      
      @Override public BitSequence nextAsSequence(int sequenceLength) {
         checkSequenceLength(sequenceLength);
         int offset = currentIndex();
         return BitSequences.subSequence(AbstractBitSequence.this, offset, offset + sequenceLength);
      }
   }
}
