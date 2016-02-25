package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.List;

@RunWith(BulkTestRunner.class)
public class PrimitiveListOfByteTest extends AbstractTestList {

   public PrimitiveListOfByteTest(String testName) {
      super(testName);
   }
   
   @Override
   public boolean isFailFastSupported() {
      return true;
   }
   
   @Override
   public boolean isNullSupported() {
      return false;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }      
   
   @Override
   public Object[] getFullElements() {
      return new Byte[]
            { 1, 2, 3, 10, 20, 30, 100 };
   }

   @Override
   public Object[] getOtherElements() {
      return new Byte[]
            { -1, 0, 7, 8, 9, 101 };
   }

   @Override
   public List<Byte> makeEmptyList() {
      // A simple class for testing abstract super-class implementations. Stores elements
      // in an array of primitives.
      return new AbstractPrimitiveList.OfByte() {
         byte values[] = new byte[0];
         
         @Override
         public byte getByte(int index) {
            return values[index];
         }

         @Override
         public byte removeByte(int index) {
            checkRange(index);
            byte newValues[] = new byte[values.length - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, newValues.length - index);
            byte ret = values[index];
            values = newValues;
            modCount++;
            return ret;
         }

         @Override
         public byte setByte(int index, byte value) {
            byte ret = values[index];
            values[index] = value;
            return ret;
         }
         
         @Override
         public void addByte(int index, byte value) {
            checkRangeWide(index);
            byte newValues[] = new byte[values.length + 1];
            System.arraycopy(values, 0, newValues, 0, index);
            newValues[index] = value;
            System.arraycopy(values, index, newValues, index + 1, values.length - index);
            values = newValues;
            modCount++;
         }

         @Override
         public int size() {
            return values.length;
         }
      };
   }
}
