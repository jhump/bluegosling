package com.bluegosling.collections.lists;

import com.bluegosling.collections.AbstractTestList;
import com.bluegosling.collections.lists.ArrayBackedLinkedList;
import com.bluegosling.testing.BulkTestRunner;

import org.apache.commons.collections.BulkTest;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@code ArrayBackedLinkedList} class using the list tests provided in the Apache Commons
 * Collections library.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@RunWith(BulkTestRunner.class)
public class ArrayBackedLinkedListTest extends AbstractTestList {
   
   /**
    * Constructs a new test case.
    * 
    * @param testName the name of the test (provided at runtime by the JUnit test runner)
    */
   public ArrayBackedLinkedListTest(String testName) {
      super(testName);
   }

   /**
    * Adds additional test cases to test the value returned from
    * {@link ArrayBackedLinkedList#asRandomAccess(boolean)}.
    *
    * @return a bulk test that includes all list test cases
    */
   public BulkTest bulkTestAsRandomAccess() {
      return new BulkTestAsRandomAccess(this) {};
   }
   
   // TODO: add additional tests for methods specific to ArrayBackedLinkedList (like trimToSize(),
   // compact(), optimize()) and additional bulk tests for the unordered and reversed iterators.
   // Also extend AbstractTestCollection to create queue/deque tests and include those here, too.

   @Override
   public List<?> makeEmptyList() {
      return new ArrayBackedLinkedList<>();
   }

   @Override
   public List<?> makeFullList() {
      return new ArrayBackedLinkedList<>(Arrays.asList(getFullElements()));
   }

   @Override
   public boolean isFailFastSupported() {
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
   // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
   // only runnable when instantiated by enclosing test.
   public abstract static class BulkTestAsRandomAccess extends AbstractTestList {

      private final ArrayBackedLinkedListTest outer;

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
      
      // TODO: add tests to verify behavior of add() and remove() given their unusual constraints
      // (here, on iterator, on sub-list, and on sub-list's iterator)

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
         return new BulkTestAsRandomAccessSubList(this) {};
      }
      
      @Override
      public BulkTest bulkTestListIterator() {
         return new BulkTestAsRandomAccessListIterator() {};
      }

      /**
       * An iterator test that works for the {@code ListIterator} returned by random access views
       * of {@code ArrayBackedLinkedList}.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
      // only runnable when instantiated by enclosing test.
      public abstract class BulkTestAsRandomAccessListIterator
      extends AbstractTestList.TestListIterator {
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
   // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
   // only runnable when instantiated by enclosing test.
   public abstract static class BulkTestAsRandomAccessSubList
   extends AbstractTestList.BulkTestSubList {

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
         return new BulkTestAsRandomAccessSubListIterator() {};
      }

      /**
       * An iterator test that works for the {@code ListIterator} returned by sub-lists of random
       * access views of {@code ArrayBackedLinkedList}.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
      // only runnable when instantiated by enclosing test.
      public abstract class BulkTestAsRandomAccessSubListIterator
      extends AbstractTestList.TestListIterator {
         @Override
         public void testRemove() {
            // skip this test - doesn't pass due to atypical constraints on remove()
         }
      }
   }
   
}
