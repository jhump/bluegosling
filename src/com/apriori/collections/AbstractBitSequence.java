package com.apriori.collections;

import java.util.NoSuchElementException;

/**
 * An abstract base class for implementations of {@link BitSequence}. Concrete sub-classes need only
 * implement {@link #length()} and {@link #stream(int)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO tests
public abstract class AbstractBitSequence implements BitSequence {
   private volatile int hashCode;
   
   @Override public BooleanIterator iterator() {
      return iterator(0);
   }
   
   @Override public BooleanIterator iterator(int startIndex) {
      final Stream stream = stream(startIndex);
      return new BooleanIterator() {
         @Override public boolean hasNext() {
            return stream.remaining() > 0;
         }

         @Override public Boolean next() {
            return nextBoolean();
         }

         @Override public boolean nextBoolean() {
            return stream.next();
         }
         
         @Override public void remove() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override public LongIterator bitTupleIterator(int tupleSize) {
      return bitTupleIterator(tupleSize, 0, BitOrder.LSB);
   }

   @Override public LongIterator bitTupleIterator(int tupleSize, BitOrder order) {
      return bitTupleIterator(tupleSize, 0, order);
   }

   @Override public LongIterator bitTupleIterator(final int tupleSize, int startIndex) {
      return bitTupleIterator(tupleSize, startIndex, BitOrder.LSB);
   }
   
   @Override public LongIterator bitTupleIterator(final int tupleSize, int startIndex,
         final BitOrder order) {
      final Stream stream = stream(startIndex);
      if (tupleSize < 1) {
         throw new IllegalArgumentException("tuple size < 1");
      } else if (tupleSize > 64) {
         throw new IllegalArgumentException("tupleSize (" + tupleSize + ") must be <= 64");
      } else if (tupleSize == 1) {
         return new LongIterator() {
            @Override public boolean hasNext() {
               return stream.remaining() > 0;
            }

            @Override public Long next() {
               return nextLong();
            }

            @Override public long nextLong() {
               return stream.next() ? 1L : 0L;
            }
            
            @Override public void remove() {
               throw new UnsupportedOperationException();
            }
         };
      } else {
         return new LongIterator() {
            @Override public boolean hasNext() {
               return stream.remaining() > 0;
            }

            @Override public Long next() {
               return nextLong();
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
   
   @Override public Stream stream() {
      return stream(0);
   }
   
   @Override public int hashCode() {
      if (hashCode == 0) {
         synchronized(this) {
            // don't recalc if we lost a race to get in this block
            if (hashCode == 0) {
               int result = 1;
               LongIterator iter = bitTupleIterator(64);
               while (iter.hasNext()) {
                  long element = iter.nextLong();
                  int elementHash = (int)(element ^ (element >>> 32));
                  result = 31 * result + elementHash;
               }
               hashCode = result ^ length();
            }
         }
      }
      return hashCode;
   }
   
   @Override public boolean equals(Object o) {
      if (o instanceof BitSequence) {
         BitSequence other = (BitSequence) o;
         if (length() != other.length()) {
            return false;
         }
         LongIterator iter1 = bitTupleIterator(64);
         LongIterator iter2 = other.bitTupleIterator(64);
         while (iter1.hasNext()) {
            if (iter1.nextLong() != iter2.nextLong()) {
               return false;
            }
         }
         return true;
      }
      return false;
   }
}