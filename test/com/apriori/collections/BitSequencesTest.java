package com.apriori.collections;

import static org.junit.Assert.assertEquals;

import com.apriori.collections.BitSequence.BitOrder;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//TODO: tests!
public class BitSequencesTest {

   @Test public void fromBytes_multipleWords() {
      // 0000 0001 - 0010 1111 - 0111 0001 - 1010 1010 
      // 1011 1001 - 0110 0000 - 0001 0011 - 1101 1100
      // 1100 0001
      byte bytes[] = new byte[] { 0x01, 0x4f, 0x71, (byte) 0xaa, (byte) 0xb9, 0x60, 0x13,
            (byte) 0xdc, (byte) 0xc1 };
      BitSequence bits = BitSequences.fromBytes(bytes);
      
      assertEquals(72, bits.length());
      
      // try a tuple size that equals 0 mod 64
      List<Integer> expected = Arrays.asList(0x1, 0x0, 0xf, 0x4, 0x1, 0x7, 0xa, 0xa, 0x9, 0xb, 0x0,
            0x6, 0x3, 0x1, 0xc, 0xd, 0x1, 0xc);
      List<Integer> actual = new ArrayList<Integer>();
      for (LongIterator iter = bits.bitTupleIterator(4); iter.hasNext(); ) {
         actual.add((int) iter.nextLong());
      }
      assertEquals(expected, actual);

      // and one that doesn't
      expected = Arrays.asList(0xf01, 0x714, 0x9aa, 0x60b, 0xc13, 0xc1d);
      actual.clear();
      for (LongIterator iter = bits.bitTupleIterator(12); iter.hasNext(); ) {
         actual.add((int) iter.nextLong());
      }
      assertEquals(expected, actual);
   }
   
   @Test public void fromBytes_singleWord() {
      // 0000 0001 - 0100 1111 - 0111 0001
      // 1010 1010 - 1011 1001
      byte bytes[] = new byte[] { 0x01, 0x4f, 0x71, (byte) 0xaa, (byte) 0xb9 };
      BitSequence bits = BitSequences.fromBytes(bytes);
      
      assertEquals(40, bits.length());
      
      List<Boolean> expectedBits = Arrays.asList(
            true,  false, false, false, false, false, false, false,
            true,  true,  true,  true,  false, false, true, false,
            true,  false, false, false, true,  true,  true,  false,
            false, true,  false, true,  false, true,  false, true,
            true,  false, false, true,  true,  true,  false, true);
      List<Boolean> actualBits = new ArrayList<Boolean>();
      for (BooleanIterator iter = bits.iterator(); iter.hasNext(); ) {
         actualBits.add(iter.nextBoolean());
      }
      assertEquals(expectedBits, actualBits);
      
      // try a tuple size that equals 0 mod 64
      List<Integer> expected = Arrays.asList(0x1, 0x0, 0xf, 0x4, 0x1, 0x7, 0xa, 0xa, 0x9, 0xb);
      List<Integer> actual = new ArrayList<Integer>();
      for (LongIterator iter = bits.bitTupleIterator(4); iter.hasNext(); ) {
         actual.add((int) iter.nextLong());
      }
      assertEquals(expected, actual);

      // and one that doesn't -- cut into nine bit chunks:
      // 1 0000 0001 - 0 1010 0111 - 0 1001 1100
      // 1 0011 0101 -        1011 
      expected = Arrays.asList(0x101, 0x0a7, 0x09c, 0x135, 0x00b);
      actual.clear();
      for (LongIterator iter = bits.bitTupleIterator(9); iter.hasNext(); ) {
         actual.add((int) iter.nextLong());
      }
      assertEquals(expected, actual);
   }   

   @Test public void fromLongs_oneWholeWord() {
      BitSequence bits = BitSequences.fromLongs(new long[] { 0x123456789abcdef0L });
      
      assertEquals(64, bits.length());

      assertEquals(0x123456789abcdef0L, bits.stream().next(64));
      assertEquals(0x23456L, bits.stream(40).next(20));
   }   
   
   @Test public void fromLongs_byteOrder() {
      long longs[] = new long[] { 0x123456789abcdef0L, 0xffaaccee88664422L };
      BitSequence bits = BitSequences.fromLongs(longs, BitOrder.LSB);
      
      assertEquals(128, bits.length());

      assertEquals(0x123456789abcdef0L, bits.stream().next(64));
      assertEquals(0xffaaccee88664422L, bits.stream(64).next(64));
      List<Long> expected = new ArrayList<Long>();
      for (long l : longs) {
         expected.add(l);
      }
      List<Long> actual = new ArrayList<Long>();
      for (long l : BitSequences.toLongs(bits, BitOrder.LSB)) {
         actual.add(l);
      }
      assertEquals(expected, actual);

      bits = BitSequences.fromLongs(longs, BitOrder.MSB);
      
      assertEquals(128, bits.length());
      assertEquals(0x0f7b3d591e6a2c48L, bits.stream().next(64));
      assertEquals(0x44226611773355ffL, bits.stream(64).next(64));
      actual.clear();
      for (long l : BitSequences.toLongs(bits, BitOrder.MSB)) {
         actual.add(l);
      }
      assertEquals(expected, actual);
   }   
}
