package com.apriori.util;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.NoSuchElementException;

//TODO: javadoc
//TODO: tests!
public final class BitSequences {
   private BitSequences() {
   }
   
   public static BitSequence fromBytes(final byte[] bytes) {
      return new BitSequence() {
         
         @Override
         public int length() {
            return bytes.length * 8;
         }
         
         @Override
         public boolean isBitSet(int index) {
            if (index >= length()) {
               throw new NoSuchElementException();               
            }
            //TODO
            return false;
         }
         
         @Override
         public int firstSetBit() {
            // TODO Auto-generated method stub
            return 0;
         }

         @Override
         public int nextSetBit(int index) {
            // TODO Auto-generated method stub
            return 0;
         }
      };
   }
   
   public static BitSequence fromByteBuffer(ByteBuffer buffer) {
      return null;
   }

   public static BitSequence fromByteBuffer(BitSet bits) {
      return null;
   }
}
