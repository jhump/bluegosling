package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.List;

@RunWith(BulkTestRunner.class)
public class PrimitiveListOfIntTest extends AbstractTestList {

   public PrimitiveListOfIntTest(String testName) {
      super(testName);
   }
   
   @Override
   public boolean isFailFastSupported() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }      
   
   @Override
   public Object[] getFullElements() {
      return new Integer[]
            { 1, 2, 3, 10, 20, 30, 100, 200, 300, 1000, 2000, 3000, 10_000, 20_000, 30_000 };
   }

   @Override
   public Object[] getOtherElements() {
      return new Integer[]
            { -1, 0, 7, 8, 9, 101, 202, 303, 1001, 2002, 3003, 100_001, 200_002, 300_003 };
   }

   @Override
   public List<Integer> makeEmptyList() {
      // A simple class for testing abstract super-class implementations. Stores elements
      // in an array of primitives.
      return new AbstractPrimitiveList.OfInt() {
         int values[] = new int[0];
         
         @Override
         public int getInt(int index) {
            return values[index];
         }

         @Override
         public int removeInt(int index) {
            checkRange(index);
            int newValues[] = new int[values.length - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, newValues.length - index);
            int ret = values[index];
            values = newValues;
            modCount++;
            return ret;
         }

         @Override
         public int setInt(int index, int value) {
            int ret = values[index];
            values[index] = value;
            return ret;
         }
         
         @Override
         public void addInt(int index, int value) {
            checkRangeWide(index);
            int newValues[] = new int[values.length + 1];
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
