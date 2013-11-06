package com.apriori.collections;

import com.apriori.collections.BitSequence.BitOrder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Utility methods for working with {@link BitSequence}s.
 * 
 * <p>Most instances of {@link BitSequence} that are created by methods in this class use an array
 * of 64-bit longs to store the sequence of bits. Exceptions include the following:
 * <ul>
 * <li>{@link #empty()}: returns a static instance (every invocation returns the same object). This
 * special sequence has no need for backing data.</li>
 * <li>{@link #singleton(boolean)}: uses a single {@code boolean}.</li>
 * <li>{@link #concat(BitSequence...)}: uses an array of other {@link BitSequence}s. This sort of
 * sequence may not be as efficient as an array of longs, but its performance is still good and
 * eliminates array-copy operations which could otherwise make concatenations slower.
 * <li>{@link #fromBitSet(BitSet)}: uses a {@link BitSet} to store the sequence. Under the hood,
 * {@link BitSet} also uses an array of longs, but it does not expose the array directly so
 * operations on a {@link BitSet}-backed {@link BitSequence} are not as efficient as those directly
 * backed by an array.</li>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: finish javadoc
//TODO: bit sequence implementations should be serializable
public final class BitSequences {
   
   /** Masks for extracting a prefix from a long, clearing trailing bits. */
   static final long PREFIX_MASKS[];
   
   /** Masks for extracting a single bit from a long. */
   static final long INDEX_MASKS[];
   
   /**
    * Method for converting a {@link BitSet} to an array of longs. This method exists in Java 7 but
    * not in Java 6. To maintain compatibility with Java 6, we only use this method if its
    * available (and it's invoked reflectively).
    */
   private static final Method bitSetToLongArray;
   
   static {
      PREFIX_MASKS = new long[63];
      long mask = 1;
      for (int i = 0; i < 63; i++, mask = (mask << 1) | 1) {
         PREFIX_MASKS[i] = mask;
      }

      INDEX_MASKS = new long[64];
      mask = 1;
      for (int i = 0; i < 64; i++, mask <<= 1) {
         INDEX_MASKS[i] = mask;
      }
      
      Method toLongArray;
      try {
         toLongArray = BitSet.class.getMethod("");
      }
      catch (SecurityException e) {
         toLongArray = null;
      }
      catch (NoSuchMethodException e) {
         toLongArray = null;
      }
      bitSetToLongArray = toLongArray;
   }

   /** Prevents instantiation. */
   private BitSequences() {
   }

   /**
    * Returns an empty sequence. All calls to this method return the same instance (since sequences
    * are immutable).
    * 
    * @return an empty sequence
    */
   public static BitSequence empty() {
      return EmptyBitSequence.INSTANCE;
   }

   /**
    * Returns a sequence that contains a single bit.
    * 
    * @param bit the value of the sequence's bit
    * @return a sequence with a single bit
    */
   public static BitSequence singleton(boolean bit) {
      return bit ? SingletonBitSequence.TRUE : SingletonBitSequence.FALSE;
   }

   /**
    * Concatenates multiple sequences together. The resulting sequence contains the bits of all
    * specified sequences strung together. The length of the resulting sequence is equal to the sum
    * of lengths of all supplied sequences. If the supplied array is empty or only contains empty
    * sequences then the resulting sequence will be empty.
    * 
    * @param sequences the input sequences
    * @return a concatenation of the input sequences
    */
   public static BitSequence concat(BitSequence... sequences) {
      int len = 0;
      for (BitSequence bs : sequences) {
         len += (bs instanceof AggregateBitSequence)
               ? ((AggregateBitSequence) bs).components.length : 1;
      }

      BitSequence components[] = new BitSequence[len];
      int cumulativeCounts[] = new int[len];
      int numComponents = 0;
      int cumulativeCount = 0;
      for (BitSequence bs : sequences) {
         if (bs instanceof AggregateBitSequence) {
            for (BitSequence component : ((AggregateBitSequence) bs).components) {
               if (component.length() > 0) {
                  components[numComponents] = component;
                  cumulativeCount += component.length();
                  cumulativeCounts[numComponents++] = cumulativeCount;
               }
            }
         } else if (bs.length() > 0) {
            components[numComponents] = bs;
            cumulativeCount += bs.length();
            cumulativeCounts[numComponents++] = cumulativeCount;
         }
      }
      if (numComponents == 0) {
         return EmptyBitSequence.INSTANCE;
      }
      if (numComponents != len) {
         BitSequence trimmedComponents[] = new BitSequence[numComponents];
         System.arraycopy(components, 0, trimmedComponents, 0, numComponents);
         components = trimmedComponents;
         int trimmedCounts[] = new int[numComponents];
         System.arraycopy(cumulativeCounts, 0, trimmedCounts, 0, numComponents);
         cumulativeCounts = trimmedCounts;
      }
      return new AggregateBitSequence(components, cumulativeCounts);
   }
   
   /**
    * Extracts a sub-sequence of bits from the specified sequence.
    * 
    * @param bits the bit sequence
    * @param start the start index of the sub-sequence to extract (inclusive)
    * @param end the end index of the sub-sequence to extract (exclusive)
    * @return the sub-sequence
    * @throws IndexOutOfBoundsException if the specified start index is negative or is greater than
    *       or equal to the length of the sequence or if the specified end index is less than the
    *       start index or greater than the length of the sequence
    */
   public static BitSequence subSequence(BitSequence bits, int start, int end) {
      BitSequence.Stream stream = bits.stream(start);
      if (end < start || end > bits.length()) {
         throw new IndexOutOfBoundsException();
      }
      return nextAsSequence(stream, end - start);
   }
   
   /**
    * Gets the next chunk of bits as a {@link BitSequence}. This only uses
    * {@link BitSequence.Stream#next(int)} so it can safely be used to implement
    * {@link BitSequence.Stream#nextAsSequence(int)}
    * 
    * @param stream the stream from which the bits are fetched
    * @param numberOfBits the number of bits to fetch to build the sequence
    * @return a new sequence
    */
   static BitSequence nextAsSequence(BitSequence.Stream stream, int numberOfBits) {
      if (numberOfBits == 0) {
         return EmptyBitSequence.INSTANCE;
      }
      
      int len = (numberOfBits + 63) >> 6;
      long words[] = new long[len];
      int trailingBits = numberOfBits & 0x3f;
      int limit = trailingBits == 0 ? len : len - 1;
      for (int i = 0; i < limit; i++) {
         words[i] = stream.next(64);
      }
      if (trailingBits != 0) {
         words[len - 1] = stream.next(trailingBits);
      }
      return fromArray(words, numberOfBits);
   }
   
   /**
    * Returns a new sequence that represents the reverse of the specified sequence.
    * 
    * @param bits the bit sequence
    * @return a bit sequence with bits in the reverse order of the specified sequence
    */
   public static BitSequence reverse(BitSequence bits) {
      int numBits = bits.length();
      int len = (numBits + 63) >> 6;
      long words[] = new long[len];
      Iterator<Long> iter = bits.bitTupleIterator(64);
      for (int i = len - 1; i >= 0; i--) {
         words[i] = Long.reverse(iter.next());
      }
      // if last word is not a full 64 bits, then reverse moved bits to the end and now we need
      // to shift them back
      int trailingBits = numBits & 0x3f;
      if (trailingBits != 0) {
         words[len - 1] >>>= 64 - trailingBits;
      }
      return fromArray(words, numBits);
   }
   
   /**
    * Returns a new sequence whose bits are the inverse of the specified sequence.
    * 
    * @param bits the sequence of bits
    * @return a new sequence that is the one's complement of the specified sequence
    */
   public static BitSequence not(BitSequence bits) {
      int numBits = bits.length();
      int len = (numBits + 63) >> 6;
      long words[] = new long[len];
      Iterator<Long> iter = bits.bitTupleIterator(64);
      for (int i = 0; i < len; i++) {
         words[i] = ~iter.next();
      }
      return fromArray(words, numBits);
   }
   
   /**
    * Computes the bitwise AND for two bit sequences. If one sequence is longer than the other then
    * the shorter sequence will have zeroes padded to the end so the lengths match.
    * 
    * @param lbits a sequence of bits
    * @param rbits a sequence of bits
    * @return the bitwise AND result of the two sequences
    */
   public static BitSequence and(BitSequence lbits, BitSequence rbits) {
      int numBits = lbits.length() > rbits.length() ? lbits.length() : rbits.length();
      int len = (numBits + 63) >> 6;
      long words[] = new long[len];
      Iterator<Long> iter1 = lbits.bitTupleIterator(64);
      Iterator<Long> iter2 = rbits.bitTupleIterator(64);
      for (int i = 0; i < len; i++) {
         long l1, l2;
         if (iter1.hasNext()) {
            l1 = iter1.next();
         } else {
            l1 = 0;
         }
         if (iter2.hasNext()) {
            l2 = iter2.next();
         } else {
            l2 = 0;
         }
         words[i] = l1 & l2;
      }
      return fromArray(words, numBits);
   }
   
   /**
    * Computes the bitwise OR for two bit sequences. If one sequence is longer than the other then
    * the shorter sequence will have zeroes padded to the end so the lengths match.
    * 
    * @param lbits a sequence of bits
    * @param rbits a sequence of bits
    * @return the bitwise OR result of the two sequences
    */
   public static BitSequence or(BitSequence lbits, BitSequence rbits) {
      int numBits = lbits.length() > rbits.length() ? lbits.length() : rbits.length();
      int len = (numBits + 63) >> 6;
      long words[] = new long[len];
      Iterator<Long> iter1 = lbits.bitTupleIterator(64);
      Iterator<Long> iter2 = rbits.bitTupleIterator(64);
      for (int i = 0; i < len; i++) {
         long l1, l2;
         if (iter1.hasNext()) {
            l1 = iter1.next();
         } else {
            l1 = 0;
         }
         if (iter2.hasNext()) {
            l2 = iter2.next();
         } else {
            l2 = 0;
         }
         words[i] = l1 | l2;
      }
      return fromArray(words, numBits);
   }
   
   /**
    * Computes the bitwise XOR for two bit sequences. If one sequence is longer than the other then
    * the shorter sequence will have zeroes padded to the end so the lengths match.
    * 
    * @param lbits a sequence of bits
    * @param rbits a sequence of bits
    * @return the bitwise XOR result of the two sequences
    */
   public static BitSequence xor(BitSequence lbits, BitSequence rbits) {
      int numBits = lbits.length() > rbits.length() ? lbits.length() : rbits.length();
      int len = (numBits + 63) >> 6;
      long words[] = new long[len];
      Iterator<Long> iter1 = lbits.bitTupleIterator(64);
      Iterator<Long> iter2 = rbits.bitTupleIterator(64);
      for (int i = 0; i < len; i++) {
         long l1, l2;
         if (iter1.hasNext()) {
            l1 = iter1.next();
         } else {
            l1 = 0;
         }
         if (iter2.hasNext()) {
            l2 = iter2.next();
         } else {
            l2 = 0;
         }
         words[i] = l1 ^ l2;
      }
      return fromArray(words, numBits);
   }
   
   public static List<Boolean> asList(final BitSequence bits) {
      return new AbstractList<Boolean>() {
         @Override
         public Boolean get(int index) {
            return bits.stream(index).next();
         }

         @Override
         public int size() {
            return bits.length();
         }
      };
   }

   public static List<Byte> asListOfBytes(BitSequence bits) {
      return asListOfBytes(bits, BitOrder.LSB);
   }

   public static List<Byte> asListOfBytes(final BitSequence bits, final BitOrder bitOrder) {
      return new AbstractList<Byte>() {
         @Override
         public Byte get(int index) {
            return (byte) bits.bitTupleIterator(8, index << 3, bitOrder).nextLong();
         }

         @Override
         public int size() {
            return (bits.length() + 7) >> 3;
         }
      };
   }

   public static List<Short> asListOfShorts(BitSequence bits) {
      return asListOfShorts(bits, BitOrder.LSB);
   }

   public static List<Short> asListOfShorts(final BitSequence bits, final BitOrder bitOrder) {
      return new AbstractList<Short>() {
         @Override
         public Short get(int index) {
            return (short) bits.bitTupleIterator(16, index << 4, bitOrder).nextLong();
         }

         @Override
         public int size() {
            return (bits.length() + 15) >> 4;
         }
      };
   }

   public static List<Integer> asListOfInts(BitSequence bits) {
      return asListOfInts(bits, BitOrder.LSB);
   }

   public static List<Integer> asListOfInts(final BitSequence bits, final BitOrder bitOrder) {
      return new AbstractList<Integer>() {
         @Override
         public Integer get(int index) {
            return (int) bits.bitTupleIterator(32, index << 5, bitOrder).nextLong();
         }

         @Override
         public int size() {
            return (bits.length() + 31) >> 5;
         }
      };
   }

   public static List<Long> asListOfLongs(BitSequence bits) {
      return asListOfLongs(bits, BitOrder.LSB);
   }

   public static List<Long> asListOfLongs(final BitSequence bits, final BitOrder bitOrder) {
      return new AbstractList<Long>() {
         @Override
         public Long get(int index) {
            return bits.bitTupleIterator(64, index << 6, bitOrder).nextLong();
         }

         @Override
         public int size() {
            return (bits.length() + 63) >> 6;
         }
      };
   }

   public static List<Long> asListOfBitTuples(BitSequence bits, int tupleSize) {
      return asListOfBitTuples(bits, tupleSize, BitOrder.LSB);
   }
   
   public static List<Long> asListOfBitTuples(final BitSequence bits, final int tupleSize,
         final BitOrder bitOrder) {
      return new AbstractList<Long>() {
         @Override
         public Long get(int index) {
            return bits.bitTupleIterator(tupleSize, index * tupleSize, bitOrder).nextLong();
         }

         @Override
         public int size() {
            return (bits.length() + tupleSize - 1) / tupleSize;
         }
      };
   }

   public static boolean[] toBits(BitSequence bits) {
      int len = bits.length();
      boolean ret[] = new boolean[len];
      BooleanIterator iter = bits.iterator();
      for (int i = 0; i < len; i++) {
         ret[i] = iter.nextBoolean();
      }
      return ret;
   }

   /**
    * Returns a byte array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 8, the last element in the array will be padded with zeroes in
    * its most significant bits (so the final bits of the sequence are in the byte's least
    * significant bits).
    * 
    * <p>Invoking this method is equivalent to the following:
    * <pre>BitSequences.toBytes(bits, BitOrder.LSB);</pre>
    * 
    * @param bits the bit sequence
    * @return a byte array with the same sequence of bits
    */
   public static byte[] toBytes(BitSequence bits) {
      return toBytes(bits, BitOrder.LSB);
   }
   
   /**
    * Returns a byte array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 8, the sequence is artificially extended with zero bits. The
    * specified bit order indicates how bits are filled into the byte, either least significant
    * first or most significant first.
    * 
    * @param bits the bit sequence
    * @param order the order of filling in bits
    * @return a short array with the same sequence of bits
    */
   public static byte[] toBytes(BitSequence bits, BitOrder order) {
      int len = (bits.length() + 7) >> 3;
      byte bytes[] = new byte[len];
      Iterator<Long> iter = bits.bitTupleIterator(8, order);
      for (int i = 0; i < len; i++) {
         bytes[i] = iter.next().byteValue();
      }
      return bytes;
   }
   
   /**
    * Returns a short array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 16, the last element in the array will be padded with zeroes in
    * its most significant bits (so the final bits of the sequence are in the short's least
    * significant bits).
    * 
    * <p>Invoking this method is equivalent to the following:
    * <pre>BitSequences.toShorts(bits, BitOrder.LSB);</pre>
    * 
    * @param bits the bit sequence
    * @return a short array with the same sequence of bits
    */
   public static short[] toShorts(BitSequence bits) {
      return toShorts(bits, BitOrder.LSB);
   }

   /**
    * Returns a short array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 16, the sequence is artificially extended with zero bits. The
    * specified bit order indicates how bits are filled into the short, either least significant
    * first or most significant first.
    * 
    * @param bits the bit sequence
    * @param order the order of filling in bits
    * @return a short array with the same sequence of bits
    */
   public static short[] toShorts(BitSequence bits, BitOrder order) {
      int len = (bits.length() + 15) >> 4;
      short shorts[] = new short[len];
      Iterator<Long> iter = bits.bitTupleIterator(16, order);
      for (int i = 0; i < len; i++) {
         shorts[i] = iter.next().shortValue();
      }
      return shorts;
   }

   /**
    * Returns a char array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 16, the last element in the array will be padded with zeroes in
    * its most significant bits (so the final bits of the sequence are in the short's least
    * significant bits).
    * 
    * <p>Invoking this method is equivalent to the following:
    * <pre>BitSequences.toChars(bits, BitOrder.LSB);</pre>
    * 
    * @param bits the bit sequence
    * @return a char array with the same sequence of bits
    */
   public static char[] toChars(BitSequence bits) {
      return toChars(bits, BitOrder.LSB);
   }

   /**
    * Returns a char array that represents the specified sequence of bits. The byte-encoding used
    * is the natural encoding of a Java {@code char} in memory, which is UTF-16. The specified bit
    * order indicates how bits are filled into the byte, either least significant first or most
    * significant first.
    * 
    * @param bits the bit sequence
    * @param order the order of filling in bits
    * @return a char array with the same sequence of bits
    */
   public static char[] toChars(BitSequence bits, BitOrder order) {
      int len = (bits.length() + 15) >> 4;
      char chars[] = new char[len];
      Iterator<Long> iter = bits.bitTupleIterator(16, order);
      for (int i = 0; i < len; i++) {
         chars[i] = (char) iter.next().shortValue();
      }
      return chars;
   }

   /**
    * Returns a char array that represents the specified sequence of bits. The bytes are decoded
    * using the specified character set. If the length of the sequence is not a multiple of 16, the
    * sequence is artificially extended with zero bits.
    * 
    * <p>Invoking this method is equivalent to the following:
    * <pre>BitSequences.toChars(bits, BitOrder.LSB);</pre>
    * 
    * @param bits the bit sequence
    * @param charset the character set used to decode bytes into characters
    * @return a char array with the same sequence of bits
    */
   public static char[] toChars(BitSequence bits, Charset charset) {
      return toChars(bits, BitOrder.LSB, charset);
   }
   
   /**
    * Returns a char array that represents the specified sequence of bits. The bytes are decoded
    * using the specified character set. The specified bit order indicates how bits are filled into
    * the char, either least significant first or most significant first.
    * 
    * @param bits the bit sequence
    * @param order the order of filling in bits
    * @param charset the character set used to decode bytes into characters
    * @return a char array with the same sequence of bits
    */
   public static char[] toChars(BitSequence bits, BitOrder order, Charset charset) {
      return new String(toBytes(bits, order), charset).toCharArray();
   }

   /**
    * Returns an int array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 32, the last element in the array will be padded with zeroes in
    * its most significant bits (so the final bits of the sequence are in the int's least
    * significant bits).
    * 
    * <p>Invoking this method is equivalent to the following:
    * <pre>BitSequences.toInts(bits, BitOrder.LSB);</pre>
    * 
    * @param bits the bit sequence
    * @return an int array with the same sequence of bits
    */
   public static int[] toInts(BitSequence bits) {
      return toInts(bits, BitOrder.LSB);
   }

   /**
    * Returns an int array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 32, the sequence is artificially extended with zero bits. The
    * specified bit order indicates how bits are filled into the int, either least significant
    * first or most significant first.
    * 
    * @param bits the bit sequence
    * @param order the order of filling in bits
    * @return an int array with the same sequence of bits
    */
   public static int[] toInts(BitSequence bits, BitOrder order) {
      int len = (bits.length() + 31) >> 5;
      int ints[] = new int[len];
      Iterator<Long> iter = bits.bitTupleIterator(32, order);
      for (int i = 0; i < len; i++) {
         ints[i] = iter.next().intValue();
      }
      return ints;
   }

   /**
    * Returns a long array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 64, the last element in the array will be padded with zeroes in
    * its most significant bits (so the final bits of the sequence are in the long's least
    * significant bits).
    * 
    * <p>Invoking this method is equivalent to the following:
    * <pre>BitSequences.toLongs(bits, BitOrder.LSB);</pre>
    * 
    * @param bits the bit sequence
    * @return a long array with the same sequence of bits
    */
   public static long[] toLongs(BitSequence bits) {
      return toLongs(bits, BitOrder.LSB);
   }
   
   /**
    * Returns a long array that represents the specified sequence of bits. If the length of the
    * sequence is not a multiple of 64, the sequence is artificially extended with zero bits. The
    * specified bit order indicates how bits are filled into the long, either least significant
    * first or most significant first.
    * 
    * @param bits the bit sequence
    * @param order the order of filling in bits
    * @return a long array with the same sequence of bits
    */
   public static long[] toLongs(BitSequence bits, BitOrder order) {
      int len = (bits.length() + 63) >> 6;
      long longs[] = new long[len];
      Iterator<Long> iter = bits.bitTupleIterator(64, order);
      for (int i = 0; i < len; i++) {
         longs[i] = iter.next();
      }
      return longs;
   }
   
   public static long[] toBitTuples(BitSequence bits, int tupleSize) {
      return toBitTuples(bits, tupleSize, BitOrder.LSB);
   }

   public static long[] toBitTuples(BitSequence bits, int tupleSize, BitOrder order) {
      int len = (bits.length() + tupleSize - 1) / tupleSize;
      long longs[] = new long[len];
      Iterator<Long> iter = bits.bitTupleIterator(tupleSize, order);
      for (int i = 0; i < len; i++) {
         longs[i] = iter.next();
      }
      return longs;
   }

   public static BitSequence fromBits(boolean bits[]) {
      int len = bits.length;
      long words[] = new long[(len + 63) >> 6];
      int j = 0;
      long m = 1;
      for (int i = 0; i < len; i++) {
         if (bits[i]) {
            words[j] |= m;
         }
         m <<= 1;
         if (m == 0) {
            m = 1;
            j++;
         }
      }
      return fromArray(words, len);
   }

   public static BitSequence fromIterator(Iterator<Boolean> iter) {
      class Element {
         long value;
         Element next;
      }
      int numBits = 0;
      Element start = new Element();
      Element current = start;
      long m = 1;
      while (iter.hasNext()) {
         if (iter.next()) {
            current.value |= m;
         }
         m <<= 1;
         if (m == 0) {
            m = 1;
            current.next = new Element();
            current = current.next;
         }
         numBits++;
      }
      long words[] = new long[(numBits + 63) >> 6];
      for (int i = 0; start != null; start = start.next, i++) {
         words[i] = start.value;
      }
      return fromArray(words, numBits);
   }
   
   public static BitSequence fromBytes(byte bytes[]) {
      return fromBytes(bytes, BitOrder.LSB);
   }
   
   public static BitSequence fromBytes(byte bytes[], BitOrder order) {
      int len = (bytes.length + 7) >> 3;
      long words[] = new long[len];
      int trailingBytes = bytes.length & 0x7;
      int limit = trailingBytes == 0 ? len : len - 1;
      int j = 0;
      if (order == BitOrder.MSB) {
         for (int i = 0; i < limit; i++) {
            words[i] = (bytes[j++] & 0xffL) << 56
                  | (bytes[j++] & 0xffL) << 48
                  | (bytes[j++] & 0xffL) << 40
                  | (bytes[j++] & 0xffL) << 32
                  | (bytes[j++] & 0xffL) << 24
                  | (bytes[j++] & 0xffL) << 16
                  | (bytes[j++] & 0xffL) << 8
                  | (bytes[j++] & 0xffL);
            words[i] = Long.reverse(words[i]);
         }
      } else {
         for (int i = 0; i < limit; i++) {
            words[i] = (bytes[j++] & 0xffL)
                  | (bytes[j++] & 0xffL) << 8
                  | (bytes[j++] & 0xffL) << 16
                  | (bytes[j++] & 0xffL) << 24
                  | (bytes[j++] & 0xffL) << 32
                  | (bytes[j++] & 0xffL) << 40
                  | (bytes[j++] & 0xffL) << 48
                  | (bytes[j++] & 0xffL) << 56;
         }
      }
      if (trailingBytes != 0) {
         long val = 0;
         if (order == BitOrder.MSB) {
            int shift = 56;
            while (trailingBytes != 0) {
               val |= (bytes[j++] & 0xffL) << shift;
               shift -= 8;
               trailingBytes--;
            }
            val = Long.reverse(val);
         } else {
            int shift = 0;
            while (trailingBytes != 0) {
               val |= (bytes[j++] & 0xffL) << shift;
               shift += 8;
               trailingBytes--;
            }
         }
         words[limit] = val;
      }
      return fromArray(words, bytes.length << 3);
   }

   public static BitSequence fromShorts(short shorts[]) {
      return fromShorts(shorts, BitOrder.LSB);
   }

   public static BitSequence fromShorts(short shorts[], BitOrder order) {
      int len = (shorts.length + 3) >> 2;
      long words[] = new long[len];
      int trailingShorts = shorts.length & 0x3;
      int limit = trailingShorts == 0 ? len : len - 1;
      int j = 0;
      if (order == BitOrder.MSB) {
         for (int i = 0; i < limit; i++) {
            words[i] = (shorts[j++] & 0xffffL) << 48
                  | (shorts[j++] & 0xffffL) << 32
                  | (shorts[j++] & 0xffffL) << 16
                  | (shorts[j++] & 0xffffL);
            words[i] = Long.reverse(words[i]);
         }
      } else {
         for (int i = 0; i < limit; i++) {
            words[i] = (shorts[j++] & 0xffffL)
                  | (shorts[j++] & 0xffffL) << 16
                  | (shorts[j++] & 0xffffL) << 32
                  | (shorts[j++] & 0xffffL) << 48;
         }
      }
      if (trailingShorts != 0) {
         long val = 0;
         if (order == BitOrder.MSB) {
            int shift = 48;
            while (trailingShorts != 0) {
               val |= (shorts[j++] & 0xffffL) << shift;
               shift -= 16;
               trailingShorts--;
            }
            val = Long.reverse(val);
         } else {
            int shift = 0;
            while (trailingShorts != 0) {
               val |= (shorts[j++] & 0xffffL) << shift;
               shift += 16;
               trailingShorts--;
            }
         }
         words[limit] = val;
      }
      return fromArray(words, shorts.length << 4);
   }

   public static BitSequence fromChars(char chars[]) {
      return fromString(String.valueOf(chars), Charset.forName("UTF-8"), BitOrder.LSB);
   }

   public static BitSequence fromChars(char chars[], Charset charset) {
      return fromString(String.valueOf(chars), charset, BitOrder.LSB);
   }

   public static BitSequence fromChars(char chars[], BitOrder order) {
      return fromString(String.valueOf(chars), Charset.forName("UTF-8"), order);
   }

   public static BitSequence fromChars(char chars[], Charset charset, BitOrder order) {
      return fromString(String.valueOf(chars), charset, order);
   }

   public static BitSequence fromString(CharSequence string) {
      return fromString(string, Charset.forName("UTF-8"), BitOrder.LSB);
   }

   public static BitSequence fromString(CharSequence string, Charset charset) {
      return fromString(string, charset, BitOrder.LSB);
   }

   public static BitSequence fromString(CharSequence string, BitOrder order) {
      return fromString(string, Charset.forName("UTF-8"), order);
   }

   public static BitSequence fromString(CharSequence string, Charset charset, BitOrder order) {
      return fromByteBuffer(charset.encode(string.toString()), order);
   }

   public static BitSequence fromInts(int ints[]) {
      return fromInts(ints, BitOrder.LSB);
   }

   public static BitSequence fromInts(int ints[], BitOrder order) {
      int len = (ints.length + 1) >> 1;
      long words[] = new long[len];
      int trailingInts = ints.length & 0x3;
      int limit = trailingInts == 0 ? len : len - 1;
      int j = 0;
      if (order == BitOrder.MSB) {
         for (int i = 0; i < limit; i++) {
            words[i] = (ints[j++] & 0xffffffffL) << 32
                  | (ints[j++] & 0xffffffffL);
            words[i] = Long.reverse(words[i]);
         }
      } else {
         for (int i = 0; i < limit; i++) {
            words[i] = (ints[j++] & 0xffffffffL)
                  | (ints[j++] & 0xffffffffL) << 32;
         }
      }
      if (trailingInts != 0) {
         if (order == BitOrder.MSB) {
            words[limit] |= (Integer.reverse(ints[j++]) & 0xffffffffL);
         } else {
            words[limit] |= (ints[j++] & 0xffffffffL);
         }
      }
      return fromArray(words, ints.length << 5);
   }

   public static BitSequence fromLongs(long longs[]) {
      return fromLongs(longs, BitOrder.LSB);
   }

   public static BitSequence fromLongs(long longs[], BitOrder order) {
      long copy[];
      if (order == BitOrder.MSB) {
         int len = longs.length;
         copy = new long[len];
         for (int i = 0; i < len; i++) {
            copy[i] = Long.reverse(longs[i]);
         }
      } else {
         copy = longs.clone();
      }
      return fromArray(copy, copy.length << 6);
   }

   public static BitSequence fromByteBuffer(ByteBuffer buffer) {
      return fromByteBuffer(buffer, BitOrder.LSB);
   }

   public static BitSequence fromByteBuffer(ByteBuffer buffer, BitOrder order) {
      byte bytes[] = new byte[buffer.remaining()];
      buffer.get(bytes);
      return fromBytes(bytes, order);
   }

   public static BitSequence fromShortBuffer(ShortBuffer buffer) {
      return fromShortBuffer(buffer, BitOrder.LSB);
   }
   
   public static BitSequence fromShortBuffer(ShortBuffer buffer, BitOrder order) {
      short shorts[] = new short[buffer.remaining()];
      buffer.get(shorts);
      return fromShorts(shorts, order);
   }
   
   public static BitSequence fromCharBuffer(CharBuffer buffer) {
      return fromCharBuffer(buffer, Charset.forName("UTF-8"), BitOrder.LSB);
   }
   
   public static BitSequence fromCharBuffer(CharBuffer buffer, Charset charset) {
      return fromCharBuffer(buffer, charset, BitOrder.LSB);
   }
   
   public static BitSequence fromCharBuffer(CharBuffer buffer, BitOrder order) {
      return fromCharBuffer(buffer, Charset.forName("UTF-8"), order);
   }
   
   public static BitSequence fromCharBuffer(CharBuffer buffer, Charset charset, BitOrder order) {
      return fromByteBuffer(charset.encode(buffer), order);
   }
   
   public static BitSequence fromIntBuffer(IntBuffer buffer) {
      return fromIntBuffer(buffer, BitOrder.LSB);
   }
   
   public static BitSequence fromIntBuffer(IntBuffer buffer, BitOrder order) {
      int ints[] = new int[buffer.remaining()];
      buffer.get(ints);
      return fromInts(ints, order);
   }
   
   public static BitSequence fromLongBuffer(LongBuffer buffer) {
      return fromLongBuffer(buffer, BitOrder.LSB);
   }
   
   public static BitSequence fromLongBuffer(LongBuffer buffer, BitOrder order) {
      long longs[] = new long[buffer.remaining()];
      buffer.get(longs);
      if (order == BitOrder.MSB) {
         for (int i = 0, len = longs.length; i < len; i++) {
            longs[i] = Long.reverse(longs[i]);
         }
      }
      return fromArray(longs, longs.length << 3);
   }
   
   private static BitSequence fromArray(final long words[], final int numberOfBits) {
      assert numberOfBits <= (words.length << 6) && numberOfBits > ((words.length - 1) << 6);
      if (numberOfBits <= 64) {
         return fromLong(words[0], numberOfBits);
      }
      // clear any unused/trailing bits from the last element
      int trailingBits = numberOfBits & 0x3f;
      if (trailingBits != 0) {
         words[words.length - 1] &= PREFIX_MASKS[trailingBits - 1];
      }
      return new LongArrayBitSequence(words, numberOfBits);
   }
   
   private static BitSequence fromLong(long inputWord, final int numberOfBits) {
      assert numberOfBits >= 0 && numberOfBits <= 64;
      final long word;
      // clear any unused/trailing bits
      if (numberOfBits == 0) {
         word = 0;
      } else if (numberOfBits == 64) {
         word = inputWord;
      } else {
         word = inputWord & PREFIX_MASKS[numberOfBits - 1];
      }
      return new LongBitSequence(word, numberOfBits);
   }

   public static BitSequence fromBitSet(BitSet bits) {
      if (bitSetToLongArray != null) {
         // Try to create a sequence from array of longs (more efficient than just wrapping the
         // BitSet)
         try {
            long words[] = (long[]) bitSetToLongArray.invoke(bits);
            return fromArray(words, bits.size());
         }
         catch (IllegalAccessException e) {
            throw new RuntimeException(e);
         }
         catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
               throw (RuntimeException) t;
            } else if (t instanceof Error) {
               throw (Error) t;
            } else {
               throw  new RuntimeException(t);
            }
         }
      }
      // fall back to just wrapping the BitSet (less efficient, but required for Java 6)
      return new BitSetBitSequence((BitSet) bits.clone() /* defensive copy */);
   }
   
   /**
    * An empty bit sequence. Since bit sequences are immutable, only a single instance is ever
    * needed.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * @see #INSTANCE
    */
   private static class EmptyBitSequence extends AbstractBitSequence {
      static final BitSequence INSTANCE = new EmptyBitSequence();
      
      private EmptyBitSequence() {
      }

      @Override public int length() {
         return 0;
      }

      @Override public Stream stream(int startIndex) {
         rangeCheck(startIndex);
         
         return new AbstractStream() {
            @Override public int remaining() {
               return 0;
            }

            @Override public int currentIndex() {
               return 0;
            }

            @Override public void jumpTo(int index) {
               rangeCheck(index);
            }

            @Override public boolean next() {
               throw new NoSuchElementException();
            }

            @Override public long next(int numberOfBits, BitOrder order) {
               return next(numberOfBits);
            }

            @Override public long next(int numberOfBits) {
               checkTupleLength(numberOfBits);
               throw new NoSuchElementException();
            }

            @Override
            public BitSequence nextAsSequence(int numberOfBits) {
               checkSequenceLength(numberOfBits);
               return INSTANCE;
            }
         };
      }
   }
   
   /**
    * A sequence consisting of just a single bit. Since there are only two possible states for a
    * single bit, just two instances are ever created (one for "true", another for "false").
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SingletonBitSequence extends AbstractBitSequence {
      final boolean val;
      
      static final BitSequence TRUE = new SingletonBitSequence(true);
      static final BitSequence FALSE = new SingletonBitSequence(false);
      
      private SingletonBitSequence(boolean val) {
         this.val = val;
      }
      
      @Override public int length() {
         return 1;
      }

      @Override public Stream stream(int startIndex) {
         rangeCheck(startIndex);

         return new AbstractStream() {
            boolean used;
            
            @Override public int remaining() {
               return used ? 0 : 1;
            }

            @Override public int currentIndex() {
               return used ? 1 : 0;
            }

            @Override public void jumpTo(int index) {
               rangeCheck(index);
               used = index == 1;
            }

            @Override public boolean next() {
               if (used) {
                  throw new NoSuchElementException();
               }
               return val;
            }

            @Override public long next(int numberOfBits, BitOrder order) {
               return next(numberOfBits);
            }

            @Override public long next(int numberOfBits) {
               checkTupleLength(numberOfBits);
               return val ? 1 : 0;
            }

            @Override
            public BitSequence nextAsSequence(int numberOfBits) {
               checkSequenceLength(numberOfBits);
               return numberOfBits == 0 ? EmptyBitSequence.INSTANCE : SingletonBitSequence.this;
            }
         };
      }
   }
   
   /**
    * A sequence that represents a concatenation of multiple other bit sequences.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AggregateBitSequence extends AbstractBitSequence {
      final BitSequence components[];
      final int cumulativeCounts[];
      final int length;
      
      AggregateBitSequence(BitSequence components[], int cumulativeCounts[]) {
         // NB: Inputs are trusted (which is why this class is private). See #concat(BitSequence...)
         // for details on how the inputs are constructed.
         this.components = components;
         this.cumulativeCounts = cumulativeCounts;
         length = cumulativeCounts[cumulativeCounts.length - 1];
      }
      
      @Override public int length() {
         return length;
      }
      
      @Override public Stream stream(final int startIndex) {
         rangeCheck(startIndex);

         return new AbstractStream() {
            {
               findPointInStream(startIndex);
            }
            
            private int remaining;
            private int currentComponent;
            private Stream componentStream;

            private void findPointInStream(int index) {
               remaining = length - index;
               int i = Arrays.binarySearch(cumulativeCounts, index);
               currentComponent = i < 0 ? (-i - 1) : i + 1;
               componentStream = components[currentComponent].stream();
            }
            
            private Stream getComponentStream() {
               if (componentStream.remaining() == 0) {
                  componentStream = components[++currentComponent].stream();
               }
               return componentStream;
            }
            
            @Override public int remaining() {
               return remaining;
            }
            
            @Override public int currentIndex() {
               return length - remaining;
            }
            
            @Override public void jumpTo(int newIndex) {
               rangeCheck(newIndex);
               findPointInStream(newIndex);
            }
            
            @Override public boolean next() {
               if (remaining < 1) {
                  throw new NoSuchElementException();
               }
               return getComponentStream().next();
            }

            @Override public long next(int tupleSize) {
               checkTupleLength(tupleSize);
               
               long val = 0;
               int bitsLeft = tupleSize;
               int shiftOffset = 0;
               while (bitsLeft > 0) {
                  Stream stream = getComponentStream();
                  int nextBits = Math.min(stream.remaining(), bitsLeft);
                  val |= stream.next(nextBits) << shiftOffset;
                  if ((bitsLeft -= nextBits) > 0) {
                     shiftOffset += nextBits;
                  }
               }
               return val;
            }
         };
      } 
   }
   
   /**
    * A bit sequence represented by a single 64-bit long. The sequence can have up to 64 bits.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class LongArrayBitSequence extends AbstractBitSequence {
      final long words[];
      final int numberOfBits;
      
      LongArrayBitSequence(long words[], int numberOfBits) {
         this.words = words;
         this.numberOfBits = numberOfBits;
      }
      
      @Override public int length() {
         return numberOfBits;
      }

      @Override public Stream stream(final int startIndex) {
         rangeCheck(startIndex);

         return new AbstractStream() {
            private int remaining = numberOfBits - startIndex;
            private int arrayIndex = startIndex >> 6;
            private int bitIndex = startIndex & 0x3f;

            @Override public int remaining() {
               return remaining;
            }
            
            @Override public int currentIndex() {
               return numberOfBits - remaining;
            }
            
            @Override public void jumpTo(int newIndex) {
               rangeCheck(newIndex);
               
               remaining = numberOfBits - newIndex;
               arrayIndex = newIndex >> 6;
               bitIndex = newIndex & 0x3f;
            }
            
            @Override public boolean next() {
               if (remaining < 1) {
                  throw new NoSuchElementException();
               }
               long bitMask = INDEX_MASKS[bitIndex];
               boolean ret = (words[arrayIndex] & bitMask) != 0;
               if (bitMask == 0x8000000000000000L) {
                  bitMask = 0;
                  arrayIndex++;
               } else {
                  bitMask <<= 1;
               }
               remaining--;
               return ret; 
            }

            @Override public long next(int tupleSize) {
               checkTupleLength(tupleSize);
               
               long val;
               if (bitIndex == 0) {
                  val = words[arrayIndex];
                  if (tupleSize < 64) {
                     val &= PREFIX_MASKS[tupleSize - 1];
                     bitIndex += tupleSize;
                  } else {
                     arrayIndex++;
                  }
               } else {
                  val = words[arrayIndex] >>> bitIndex;
                  bitIndex += tupleSize;
                  if (bitIndex > 64) {
                     bitIndex &= 0x3f;
                     if (++arrayIndex < words.length) {
                        // rest of the tuple is in the next word
                        val |= (words[arrayIndex] & PREFIX_MASKS[bitIndex - 1])
                              << (tupleSize - bitIndex);
                     }
                  } else if (bitIndex == 64) {
                     bitIndex = 0;
                     arrayIndex++;
                  } else if (tupleSize < 64) {
                     // got the whole value -- mask off any extra trailing bits
                     val &= PREFIX_MASKS[tupleSize - 1];
                  }
               }
               remaining -= tupleSize;
               return val;
            }
         };
      } 
   }
   
   /**
    * A bit sequence represented by an array of 64-bit longs. The length of the sequence is nearly
    * unlimited (for practical reasons, it is limited to no greater than {@link Integer#MAX_VALUE}).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class LongBitSequence extends AbstractBitSequence {
      final long word;
      final int numberOfBits;
      
      LongBitSequence(long word, int numberOfBits) {
         this.word = word;
         this.numberOfBits = numberOfBits;
      }
      
      @Override public int length() {
         return numberOfBits;
      }

      @Override public Stream stream(final int startIndex) {
         rangeCheck(startIndex);

         return new AbstractStream() {
            private int remaining = numberOfBits - startIndex;
            private int index = startIndex;

            @Override public int remaining() {
               return remaining;
            }
            
            @Override public int currentIndex() {
               return numberOfBits - remaining;
            }
            
            @Override public void jumpTo(int newIndex) {
               rangeCheck(newIndex);
               remaining = numberOfBits - newIndex;
               index = newIndex;
            }
            
            @Override public boolean next() {
               if (remaining < 1) {
                  throw new NoSuchElementException();
               }
               boolean ret = (word & INDEX_MASKS[index++]) != 0;
               remaining--;
               return ret; 
            }
            
            @Override public long next(int tupleSize) {
               checkTupleLength(tupleSize);
               
               long val;
               if (index == 0) {
                  val = word;
               } else {
                  val = word >>> index;
               }
               if (tupleSize < 64) {
                  val &= PREFIX_MASKS[tupleSize - 1];
               }
               index += tupleSize;
               remaining -= tupleSize;
               return val;
            }
         };
      }
   }
   
   /**
    * A bit sequence that is backed by a {@link BitSet}. The underlying set should be a defensive
    * copy to preserve the immutability of the sequence.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class BitSetBitSequence extends AbstractBitSequence {
      final BitSet bits;
      
      BitSetBitSequence(BitSet bits) {
         this.bits = bits;
      }
      
      @Override public int length() {
         return bits.length();
      }
      
      @Override public Stream stream(final int startIndex) {
         rangeCheck(startIndex);

         return new AbstractStream() {
            private int remaining = bits.length() - startIndex;
            private int index = startIndex;
            
            @Override public int remaining() {
               return remaining;
            }

            @Override public int currentIndex() {
               return bits.length() - remaining;
            }
            
            @Override public void jumpTo(int newIndex) {
               rangeCheck(newIndex);
               remaining = bits.length() - newIndex;
               index = newIndex;
            }
            
            @Override public boolean next() {
               if (remaining < 1) {
                  throw new NoSuchElementException();
               }
               remaining--;
               return bits.get(index++);
            }

            @Override public long next(int tupleSize) {
               checkTupleLength(tupleSize);

               long val = 0;
               int len = bits.length();
               int limit = index + tupleSize;
               if (limit > len) {
                  limit = len;
               }
               for (int bit = 1; index < limit; index++, bit <<= 1) {
                  if (bits.get(index)) {
                     val |= bit;
                  }
               }
               remaining -= tupleSize;
               return val;
            }
         };
      }
   }
}
