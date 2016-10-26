package com.bluegosling.collections.bits;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bluegosling.collections.bits.BitSequence.BitOrder;

/**
 * A stream of bits. This interface is similar to an iterator, except it allows for selecting an
 * arbitrary number of bits with each fetch. It also includes a method that allows for random
 * access seeks over the underlying bits.
 * 
 * <p>Conceptually, this is like an I/O stream from which you can read arbitrarily sized chunks of
 * bits as opposed to just reading bytes.
 * 
 * <p>Despite the similar name, it is not a Java 8 {@link Stream}. But a
 * {@linkplain #asIntStream() method} is provided to adapt to that kind of stream.
 * 
 * @see BitSequence
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface BitStream {
   /** 
    * Returns the remaining number of bits in the stream.
    * 
    * @return the remaining number of bits in the stream
    */
   int remaining();
   
   /** 
    * Returns the current index of the stream. A stream can be "reset" to this point using
    * {@link #jumpTo(int)}. A new stream can be created that starts back from this point using
    * {@link BitSequence#bitStream(int)}.
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
   
   /**
    * Provides a view of this bit stream as an {@link IntStream}. This allows code to operate on
    * the stream in a functional way. The elements of the int streams are just bits, so each value
    * in the stream is a zero or one.
    *
    * @return a view of the bits as an {@link IntStream}
    */
   default IntStream asIntStream() {
      return StreamSupport.intStream(() -> {
         PrimitiveIterator.OfInt iter = new PrimitiveIterator.OfInt() {
            @Override
            public boolean hasNext() {
               return remaining() > 0;
            }

            @Override
            public int nextInt() {
               return BitStream.this.next() ? 1 : 0;
            }
         };
         return Spliterators.spliterator(iter, remaining(), 0);
      }, Spliterator.SIZED | Spliterator.SUBSIZED, false); 
   }
}
