package com.bluegosling.collections.primitive;

import com.bluegosling.collections.AbstractTestList;
import com.bluegosling.collections.primitive.AbstractPrimitiveList;
import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.List;

@RunWith(BulkTestRunner.class)
public class PrimitiveListOfShortTest extends AbstractTestList {

   public PrimitiveListOfShortTest(String testName) {
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
      return new Short[]
            { 1, 2, 3, 10, 20, 30, 100, 200, 300, 1000, 2000, 3000, 10_000, 20_000, 30_000 };
   }

   @Override
   public Object[] getOtherElements() {
      return new Short[]
            { -1, 0, 7, 8, 9, 101, 202, 303, 1001, 2002, 3003, 10_001, 20_002, 30_003 };
   }

   @Override
   public List<Short> makeEmptyList() {
      // A simple class for testing abstract super-class implementations. Stores elements
      // in an array of primitives.
      return new AbstractPrimitiveList.OfShort() {
         short values[] = new short[0];
         
         @Override
         public short getShort(int index) {
            return values[index];
         }

         @Override
         public short removeShort(int index) {
            checkRange(index);
            short newValues[] = new short[values.length - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, newValues.length - index);
            short ret = values[index];
            values = newValues;
            modCount++;
            return ret;
         }

         @Override
         public short setShort(int index, short value) {
            short ret = values[index];
            values[index] = value;
            return ret;
         }
         
         @Override
         public void addShort(int index, short value) {
            checkRangeWide(index);
            short newValues[] = new short[values.length + 1];
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
