package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.List;

@RunWith(BulkTestRunner.class)
public class PrimitiveListOfCharTest extends AbstractTestList {

   public PrimitiveListOfCharTest(String testName) {
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
      return new Character[]
            { '1', 'a', 'b', 'c', '&', 'X', 'Y', 'Z', '@', '+', '8', '9', '0' };
   }

   @Override
   public Object[] getOtherElements() {
      return new Object[]
            { '!', 'A', 'B', 'C', '7', 'x', 'y', 'z', '2', '=', '*', '(', ')' };
   }

   @Override
   public List<Character> makeEmptyList() {
      // A simple class for testing abstract super-class implementations. Stores elements
      // in an array of primitives.
      return new AbstractPrimitiveList.OfChar() {
         char values[] = new char[0];
         
         @Override
         public char getChar(int index) {
            return values[index];
         }

         @Override
         public char removeChar(int index) {
            checkRange(index);
            char newValues[] = new char[values.length - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, newValues.length - index);
            char ret = values[index];
            values = newValues;
            modCount++;
            return ret;
         }

         @Override
         public char setChar(int index, char value) {
            char ret = values[index];
            values[index] = value;
            return ret;
         }
         
         @Override
         public void addChar(int index, char value) {
            checkRangeWide(index);
            char newValues[] = new char[values.length + 1];
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
