package com.apriori.collections;

import java.util.NoSuchElementException;

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
    * A stream of bits. This interface is similar to an iterator, except it allows for selecting an
    * arbitrary number of bits with each fetch.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Stream {
      /** 
       * Returns the remaining number of bits in the stream.
       * 
       * @return the remaining number of bits in the stream
       */
      int remaining();
      
      /** 
       * Returns the current index of the stream. A stream can be "reset" to this point using
       * {@link #jumpTo(int)}. A new stream can be created that starts back from this point using
       * {@link BitSequence#stream(int)}.
       * 
       * @return the index of the next bit that would be retrieved by this stream
       */
      int currentIndex();

      /**
       * Sets the current index of the stream. The bit at the specified index in the sequence will
       * be the next one fetched by a call to {@link #next()}. This can be used to perform random
       * access in the sequence.
       * 
       * @param index the new index
       * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
       *       length of the underlying sequence
       */
      void jumpTo(int index);
      
      /**
       * Returns the next bit in the stream.
       * 
       * @return the next bit
       * @throws NoSuchElementException if there are no bits remaining
       */
      boolean next();
      
      /**
       * Returns the next chunk of bits in the stream. Since the bits are returned as a primitive
       * long, the chunk size must be 64 bits or less. The next bit in the sequence will either be
       * the least or most significant bit in the returned value. For example, if the next six bits
       * in the sequence are as follows:
       * <pre>0 1 0 0 1 1</pre>
       * Then fetching the next six bits in {@link BitOrder#LSB LSB} order will return the number
       * 50, which is 110010 in binary. In {@link BitOrder#MSB MSB} order, the fetch will return the
       * number 19, or 010011 in binary.
       * 
       * @param numberOfBits the number of bits to fetch
       * @param order the order of bits in the returned value
       * @return the next chunk of bits
       * @throws IllegalArgumentException if the requested number of bits is less than one or
       *       greater than sixty-four
       * @throws NoSuchElementException if the requested number of bits is greater than the number
       *       of bits remaining in the stream
       */
      long next(int numberOfBits, BitOrder order);
      
      /**
       * Returns the next chunk of bits in the stream. This is equivalent to the following:
       * <pre>stream.next(numberOfBits, BitOrder.LSB);</pre>
       * 
       * @param numberOfBits the number of bits to fetch
       * @return the next chunk of bits
       * @throws IllegalArgumentException if the requested number of bits is less than one or
       *       greater than sixty-four
       * @throws NoSuchElementException if the requested number of bits is greater than the number
       *       of bits remaining in the stream
       */
      long next(int numberOfBits);
      
      /**
       * Returns the next chunk of bits in the stream. The chunk can be arbitrarily large provided
       * that there are sufficient remaining bits.
       * 
       * @param numberOfBits the number of bits to fetch
       * @return the next chunk of bits, as a {@link BitSequence}
       * @throws IllegalArgumentException if the requested number of bits is negative
       * @throws NoSuchElementException if the requested number of bits is greater than the number
       *       of bits remaining in the stream
       */
      BitSequence nextAsSequence(int numberOfBits);
   }
   
   /**
    * Gets the length of the sequence.
    * 
    * @return the number of bits in this sequnce
    */
   int length();
   
   /**
    * Returns an iterator over the values in the sequence. The return type is more constrained
    * than {@link Iterable#iterator()} so that iteration can be done over un-boxed primitives.
    */
   @Override BooleanIterator iterator();
   
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
    * order, the zero bits are on the less signifcant end, which means the last value is effectively
    * shifted left by the number of bits absent from the sequence.
    * 
    * @param tupleSize the number of bits of each chunk
    * @param order the order of bits in the returned value
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see BitSequence.Stream#next(int, BitSequence.BitOrder)
    */
   LongIterator bitTupleIterator(int tupleSize, BitOrder order);
   
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
   LongIterator bitTupleIterator(int tupleSize);

   /**
    * Returns an iterator over arbitrarily-sized chunks in the sequence. The fetching of values will
    * start at the specified index (zero-based) instead of starting with the first bit.
    * 
    * @param tupleSize the number of bits of each chunk
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see #bitTupleIterator(int, BitOrder)
    */
   LongIterator bitTupleIterator(int tupleSize, int startIndex, BitOrder order);
   
   /**
    * Returns an iterator over arbitrarily-sized chunks in the sequence. This method is equivalent
    * to the following:
    * <pre>bitSequence.bitTupleIterator(tupleSize, startIndex, BitOrder.LSB);</pre>
    * 
    * @return an iterator
    * @throws IllegalArgumentException if the specified tuple size is less than one or greater than
    *       sixty-four.
    * @see #bitTupleIterator(int, int, BitOrder)
    */
   LongIterator bitTupleIterator(int tupleSize, int startIndex);
   
   /**
    * Returns a stream, for consuming bits in arbitrary amounts with each fetch.
    * 
    * @return a stream
    */
   Stream stream();
   
   /**
    * Returns a stream, for consuming bits in arbitrary amounts with each fetch. The stream will
    * begin with the specified index (zero-based) instead of beginning with the first bit.
    * 
    * @return a stream
    */
   Stream stream(int startIndex);
   
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