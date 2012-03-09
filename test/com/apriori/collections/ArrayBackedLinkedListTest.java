package com.apriori.collections;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.list.AbstractTestList;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestSuite;

/**
 * Tests the {@code ArrayBackedLinkedList} class using the list tests provided in the Apache Commons
 * Collections library.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ArrayBackedLinkedListTest extends AbstractTestList {

   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-lists, iterators, etc.).
    *
    * @return a test suite that includes all test cases for {@code ArrayBackedLinkedList}
    */
   public static TestSuite suite() {
      return makeSuite(ArrayBackedLinkedListTest.class);
   }
   
   /**
    * Adds additional test cases to test the value returned from
    * {@link ArrayBackedLinkedList#asRandomAccess(boolean)}.
    *
    * @return a bulk test that includes all list test cases
    */
   public BulkTest bulkTestAsRandomAccess() {
      return new BulkTestAsRandomAccess(this);
   }

   /**
    * Constructs a new test case.
    * 
    * @param testName the name of the test (provided at runtime by the JUnit test runner)
    */
   public ArrayBackedLinkedListTest(String testName) {
      super(testName);
   }

   @Override
   public List<?> makeEmptyList() {
      return new ArrayBackedLinkedList<Object>();
   }

   @Override
   public List<?> makeFullList() {
      return new ArrayBackedLinkedList<Object>(Arrays.asList(getFullElements()));
   }

   @Override
   public boolean isFailFastSupported() {
      return true;
   }

   @Override
   public boolean isEqualsCheckable() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }

   /**
    * A set of test cases to test the list returned from
    * {@link ArrayBackedLinkedList#asRandomAccess(boolean)}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestAsRandomAccess extends AbstractTestList {

      private ArrayBackedLinkedListTest outer;

      /**
       * Constructs a new test.
       *
       * @param outer the encompassing {@code ArrayBackedLinkedListTest}
       */
      public BulkTestAsRandomAccess(ArrayBackedLinkedListTest outer) {
         super("");
         this.outer = outer;
      }

      @Override
      public Object[] getFullElements() {
         return outer.getFullElements();
      }

      @Override
      public Object[] getOtherElements() {
         return outer.getOtherElements();
      }

      @Override
      public boolean isAddSupported() {
         // adds other than appends throw exceptions
         return false;
      }

      @Override
      public boolean isSetSupported() {
         return outer.isSetSupported();
      }

      @Override
      public boolean isRemoveSupported() {
         // removes other than from tail of list throw exceptions
         return false;
      }

      @Override
      public List<?> makeEmptyList() {
         return ((ArrayBackedLinkedList<?>) outer.makeEmptyList()).asRandomAccess(true);
      }

      @Override
      public void resetEmpty() {
         outer.resetEmpty();
         this.collection = ((ArrayBackedLinkedList<?>) outer.getList()).asRandomAccess(true);
         this.confirmed = outer.getConfirmedList();
      }

      @Override
      public void resetFull() {
         outer.resetFull();
         this.collection = ((ArrayBackedLinkedList<?>) outer.getList()).asRandomAccess(true);
         this.confirmed = outer.getConfirmedList();
      }

      @Override
      public void verify() {
         super.verify();
         outer.verify();
      }

      @Override
      public boolean isTestSerialization() {
         return false;
      }
      
      @Override
      public void testUnsupportedAdd() {
         // skip this test - doesn't pass due to atypical constraints on add()
      }
      
      @Override
      public void testUnsupportedRemove() {
         // skip this test - doesn't pass due to atypical constraints on remove()
      }
      
      @Override
      public BulkTest bulkTestSubList() {
         // overridden to return sub list test that makes exceptions for testUnsupportedAdd and
         // testUnsupportedRemove to work around odd constraints on this list
         return new BulkTestAsRandomAccessSubList(this);
      }
      
      @Override
      public BulkTest bulkTestListIterator() {
         return new BulkTestAsRandomAccessListIterator();
      }

      /**
       * An iterator test that works for the {@code ListIterator} returned by random access views
       * of {@code ArrayBackedLinkedList}.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      public class BulkTestAsRandomAccessListIterator extends AbstractTestList.TestListIterator {
         @Override
         public void testRemove() {
            // skip this test - doesn't pass due to atypical constraints on remove()
         }
      }
   }

   /**
    * A set of test cases to test sub-lists of the list returned from
    * {@link ArrayBackedLinkedList#asRandomAccess(boolean)}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestAsRandomAccessSubList extends AbstractTestList.BulkTestSubList {

      /**
       * Constructs a new test.
       *
       * @param outer the encompassing {@code BulkTestAsRandomAccess}
       */
      public BulkTestAsRandomAccessSubList(BulkTestAsRandomAccess outer) {
         super(outer);
      }
      
      @Override
      public void testUnsupportedAdd() {
         // skip this test - doesn't pass due to atypical constraints on add()
      }
      
      @Override
      public void testUnsupportedRemove() {
         // skip this test - doesn't pass due to atypical constraints on remove()
      }
      
      @Override
      public BulkTest bulkTestListIterator() {
         return new BulkTestAsRandomAccessSubListIterator();
      }

      /**
       * An iterator test that works for the {@code ListIterator} returned by sub-lists of random
       * access views of {@code ArrayBackedLinkedList}.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      public class BulkTestAsRandomAccessSubListIterator extends AbstractTestList.TestListIterator {
         @Override
         public void testRemove() {
            // skip this test - doesn't pass due to atypical constraints on remove()
         }
      }
   }
   
}
