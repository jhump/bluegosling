package com.bluegosling.collections.bits;

import com.bluegosling.collections.primitive.BooleanIterator;

import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * A sequence of bits. This is very similar to a {@link java.util.BitSet} except it provides
 * better API for navigating the bits like a stream (of arbitrarily sized chunks) and less API for
 * general bit-fiddling. Unlike {@link java.util.BitSet}, a {@link BitSequence} is immutable.
 * 
 * <p>Since the sequence is immutable, all attempts to remove elements using
 * {@link java.util.Iterator}s will result in an {@link UnsupportedOperationException} being thrown.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface BitSequence extends Iterable<Boolean> {

   /** Indicates how bits are ordered when extracting bits into numeric types. */
   public enum BitOrder {
      /** Least significant bit first. */
      LSB,
      /** Most significant bit first. */
      MSB
   }
   
   /**
    * Gets the length of the sequence.
    * 
    * @return the number of bits in this sequence
    */
   int length();
   
   /**
    * Returns an iterator over the values in the sequence. The return type is more constrained
    * than {@link Iterable#iterator()} so that iteration can be done over un-boxed primitives.
    */
   @Override
   default BooleanIterator iterator() {
      return iterator(0);
   }
   
   /**
    * Returns an iterator over the values in the sequence starting at the specified index.
    * 
    * @param startIndex the starting index (inclusive) for iteration
    * @return an iterator
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       length of the sequence
    */
   BooleanIterator iterator(int startIndex);

   /**
    * Returns an iterator over arbitrarily-sized chunks in the sequence. Numeric values are returned
    * that represent the next <em>n</em> bits of the sequence where <em>n</em> is the "tuple size".
    * If the sequence length isn't a multiple of the specified tuple size then the last value
    * returned from the iterator may have 0 bits padded to it. For {@link BitOrder#LSB LSB} order,
    * the zero bits are on the more significant end of the value. But for {@link BitOrder#MSB MSB}
    * order, the zero bits are on the less significant end, which means the last value is
    * effectively shifted left by the number of bits absent from the sequence.
    * 
    * @param tupleSize the number of bits of each chunk
    * @param order the order of bits in the returned value
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see BitStream#next(int, BitSequence.BitOrder)
    */
   default PrimitiveIterator.OfLong bitTupleIterator(int tupleSize, BitOrder order) {
      return bitTupleIterator(tupleSize, 0, order);
   }
   
   /**
    * Returns an iterator over arbitrarily-sized chunks in the sequence. This method is equivalent
    * to the following:
    * <pre>bitSequence.bitTupleIterator(tupleSize, BitOrder.LSB);</pre>
    * 
    * @param tupleSize the number of bits of each chunk
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see #bitTupleIterator(int, BitOrder)
    */
   default PrimitiveIterator.OfLong bitTupleIterator(int tupleSize) {
      return bitTupleIterator(tupleSize, 0, BitOrder.LSB);
   }

   /**
    * Returns an iterator over arbitrarily-sized chunks in the sequence. The fetching of values will
    * start at the specified index (zero-based) instead of starting with the first bit.
    * 
    * @param tupleSize the number of bits of each chunk
    * @param startIndex an offset into the sequence from which the tuples will start
    * @param order the order of bits in the returned value
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see #bitTupleIterator(int, BitOrder)
    */
   PrimitiveIterator.OfLong bitTupleIterator(int tupleSize, int startIndex, BitOrder order);
   
   /**
    * Returns an iterator over arbitrarily-sized chunks in the sequence. This method is equivalent
    * to the following:
    * <pre>bitSequence.bitTupleIterator(tupleSize, startIndex, BitOrder.LSB);</pre>
    * 
    * @param tupleSize the number of bits of each chunk
    * @param startIndex an offset into the sequence from which the tuples will start
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see #bitTupleIterator(int, int, BitOrder)
    */
   default PrimitiveIterator.OfLong bitTupleIterator(int tupleSize, int startIndex) {
      return bitTupleIterator(tupleSize, startIndex, BitOrder.LSB);
   }
   
   /**
    * Returns a view of a portion of this sequence.
    *
    * @param start the starting position of the portion of interest, inclusive
    * @param end the end position of the portion of interest, exclusive
    * @return a view of a portion of this sequence, as another {@link BitSequence}
    */
   default BitSequence subSequence(int start, int end) {
      return BitSequences.subSequence(this, start, end);
   }
   
   /**
    * Returns a stream, for consuming bits in arbitrary amounts with each fetch.
    * 
    * @return a stream
    */
   default BitStream bitStream() {
      return bitStream(0);
   }
   
   /**
    * Returns a stream, for consuming bits in arbitrary amounts with each fetch. The stream will
    * begin with the specified index (zero-based) instead of beginning with the first bit.
    * 
    * @return a stream
    * @param startIndex an offset into the sequence from which the stream will start
    */
   BitStream bitStream(int startIndex);
   
   /**
    * Creates a {@link LongStream} over arbitrarily-sized chunks of bits. This is similar to
    * {@link #bitTupleIterator(int)} but allowing functional operations on the stream of values.
    *
    * @param tupleSize the number of bits of each chunk
    * @return a stream of long values, each value being a chunk of bits
    */
   default LongStream bitTupleStream(int tupleSize) {
      int size = (63 + length()) >> 6; 
      return StreamSupport.longStream(
            Spliterators.spliterator(bitTupleIterator(tupleSize), size, 0), false);
   }

   /**
    * Creates a {@link LongStream} over arbitrarily-sized chunks of bits. This is similar to
    * {@link #bitTupleIterator(int, BitOrder)} but allowing functional operations on the stream of
    * values.
    *
    * @param tupleSize the number of bits of each chunk
    * @param bitOrder the order of bits in the returned value
    * @return a stream of long values, each value being a chunk of bits
    * 
    * @see #bitTupleIterator(int, BitOrder)
    */
   default LongStream bitTupleStream(int tupleSize, BitOrder bitOrder) {
      int size = (63 + length()) >> 6; 
      return StreamSupport.longStream(
            Spliterators.spliterator(bitTupleIterator(tupleSize, bitOrder), size, 0), false);
   }

   /**
    * Returns a hash code for the sequence. So different implementations can be compared and stored
    * together in a set, the hash code should conform to the following scheme:
    * <pre>
    * hash code = length of the sequence XOR hash code of corresponding list of longs
    * </pre>
    * The corresponding list of longs means a {@link java.util.List} of {@link Long}s whose length
    * is exactly enough to contain all of the bits in the sequence, whose first element represents
    * the first 64 bits of the sequence (least significant bit first), and whose last element
    * represents the last 1 to 64 bits (least significant bit first) with any superfluous (most
    * significant) bits zeroed out when the sequence length is not a multiple of 64.
    * 
    * <p>The second term in the hash code happens to be the same as calculated by
    * {@link java.util.Arrays#hashCode(long[])} with an array whose contents correspond to the bits 
    * in the sequence (in the same manner described above).
    * 
    * @return a hash code for the sequence, calculated as described above
    */
   @Override int hashCode();
   
   /**
    * Returns true if the specified object is a {@link BitSequence} with the same contents and in
    * the same order as this sequence.
    * 
    * @param o an object
    * @return true if the object represents the same sequence of bits; false otherwise
    */
   @Override boolean equals(Object o);
}
