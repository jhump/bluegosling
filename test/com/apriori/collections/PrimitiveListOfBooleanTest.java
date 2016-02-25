package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.List;

@RunWith(BulkTestRunner.class)
public class PrimitiveListOfBooleanTest extends AbstractTestListOfBoolean {

   public PrimitiveListOfBooleanTest(String testName) {
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
   public List<Boolean> makeEmptyList() {
      // A simple class for testing abstract super-class implementations. Stores elements
      // in an array of primitives.
      return new AbstractPrimitiveList.OfBoolean() {
         boolean values[] = new boolean[0];
         
         @Override
         public boolean getBoolean(int index) {
            return values[index];
         }

         @Override
         public boolean removeBoolean(int index) {
            checkRange(index);
            boolean newValues[] = new boolean[values.length - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, newValues.length - index);
            boolean ret = values[index];
            values = newValues;
            modCount++;
            return ret;
         }

         @Override
         public boolean setBoolean(int index, boolean value) {
            boolean ret = values[index];
            values[index] = value;
            return ret;
         }
         
         @Override
         public void addBoolean(int index, boolean value) {
            checkRangeWide(index);
            boolean newValues[] = new boolean[values.length + 1];
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
