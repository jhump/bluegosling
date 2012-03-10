// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.iterators.AbstractTestIterator;
import org.apache.commons.collections.set.AbstractTestSortedSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Abstract test that builds on the Apache Commons Collection's {@link AbstractTestSortedSet} to
 * test the additional methods in {@link NavigableSet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractTestNavigableSet extends AbstractTestSortedSet {

   /**
    * Constructs a new test.
    *
    * @param name test case name
    */
   public AbstractTestNavigableSet(String name) {
      super(name);
   }

   /**
    * Adds test cases in bulk for testing {@link NavigableSet#descendingSet()}.
    *
    * @return tests
    */
   public BulkTest bulkTestDescendingSet() {
      return new BulkTestDescendingSet(this);
   }

   /**
    * Adds test cases in bulk for testing {@link NavigableSet#iterator()}.
    *
    * @return tests
    */
   public BulkTest bulkTestIterator() {
      return new BulkTestIterator(this);
   }
   
   /**
    * Adds test cases in bulk for testing {@link NavigableSet#descendingIterator()}.
    *
    * @return tests
    */
   public BulkTest bulkTestDescendingIterator() {
      return new BulkTestDescendingIterator(this);
   }
   
   // TODO: add additional tests for the other methods of NavigableSet (ceiling, floor, higher,
   // and lower) and for add'l ways to create sub-sets (head-, tail-, and sub-sets with
   // inclusive/exclusive bounds).

   /**
    * Tests for a the descending view of a {@link NavigableSet}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestDescendingSet extends AbstractTestNavigableSet {
      
      AbstractTestNavigableSet outer;
      
      /**
       * Constructs a new test.
       * 
       * @param outer the test for the underlying {@link NavigableSet}
       */
      public BulkTestDescendingSet(AbstractTestNavigableSet outer) {
         super("");
         this.outer = outer;
      }
      
      @Override
      public BulkTest bulkTestDescendingSet() {
         return null; // prevent infinite recursion
      }
      
      /**
       * Confirms that the {@link NavigableSet#descendingSet() descending view} of a descending
       * view is equivalent to the original set.
       */
      public void testDescendingSet() {
         resetFull();
         // descendingSet() of a descendingSet() should be equivalent to the original set
         this.confirmed = outer.getSet();
         this.collection = ((NavigableSet<?>) getSet()).descendingSet();
         
         assertEquals("descendingSet().descendingSet() hash should be equivalent to original set",
               getConfirmedSet().hashCode(), getSet().hashCode());
         // continue with other verification to make sure the two sets match
         verify();
      }
      
      @Override
      public BulkTest bulkTestSortedSetSubSet() {
         int length = getFullElements().length;
         int lobound = length / 3;
         int hibound = lobound * 2;
         return new BulkTestSubSet(lobound, hibound);
      }
      
      @Override
      public BulkTest bulkTestSortedSetHeadSet() {
         int length = getFullElements().length;
         int lobound = length / 3;
         int hibound = lobound * 2;
         return new BulkTestSubSet(hibound, true);
      }
      
      @Override
      public BulkTest bulkTestSortedSetTailSet() {
         int length = getFullElements().length;
         int lobound = length / 3;
         return new BulkTestSubSet(lobound, false);
     }
      
      @Override
      public NavigableSet<?> makeEmptySet() {
         return ((NavigableSet<?>) outer.makeEmptySet()).descendingSet();
      }
      
      private Object[] reverse(Object array[]) {
         // reverse the array in place
         for (int i = 0, j = array.length - 1; j > i; i++, j--) {
            Object tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
         }
         return array;
      }
      
      @Override
      public Object[] getFullElements() {
         return reverse(outer.getFullElements());
      }

      @Override
      public Object[] getOtherElements() {
         return reverse(outer.getOtherElements());
      }

      @Override
      public boolean isAddSupported() {
         return outer.isAddSupported();
      }

      @Override
      public boolean isNullSupported() {
         return outer.isNullSupported();
      }

      @Override
      public boolean isRemoveSupported() {
         return outer.isRemoveSupported();
      }

      @Override
      public void resetEmpty() {
         outer.resetEmpty();
         this.collection = ((NavigableSet<?>) outer.getSet()).descendingSet();
         this.confirmed = ((NavigableSet<?>) outer.getConfirmedSet()).descendingSet();
      }

      @Override
      public void resetFull() {
         outer.resetFull();
         this.collection = ((NavigableSet<?>) outer.getSet()).descendingSet();
         this.confirmed = ((NavigableSet<?>) outer.getConfirmedSet()).descendingSet();
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

      /**
       * Tests for the head-, tail-, and sub-sets of a descending set.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      public class BulkTestSubSet extends AbstractTestSortedSet.TestSortedSetSubSet {

         /**
          * Constructs a new test.
          * 
          * @param bound the bound of this sub-set
          * @param head true if this is a head-set, false if a tail-set
          */
         public BulkTestSubSet(int bound, boolean head) {
            BulkTestDescendingSet.this.super(bound, head);
         }

         /**
          * Constructs a new test.
          *
          * @param lobound the lower bound of this sub-set
          * @param hibound the upper bound of this sub-set
          */
         public BulkTestSubSet(int lobound, int hibound) {
            BulkTestDescendingSet.this.super(lobound, hibound);
         }
         
         @Override
         public Collection<?> makeConfirmedCollection() {
            return ((NavigableSet<?>) super.makeConfirmedCollection()).descendingSet();
         }
         
         @Override
         public Object[] getOtherElements() {
            // Super-class includes integers that are equal to the elements of
            // getFullElements(), but one greater. In other words:
            //   super.getOtherElements()[i] == super.getFullElements()[i] + 1
            // However, since we're testing a descending set, this will generate
            // elements that are outside the valid range of values for the subset.
            // We have to "reverse" the relationship between 'other' and 'full'
            // elements, just as everything else is reversed for a descending set.
            // So we instead want:
            //   super.getOtherElements()[i] == super.getFullElements()[i] - 1
            // Easily achieved by just subtracting two from every element...
            Object ret[] = super.getOtherElements();
            for (int i = 0, len = ret.length; i < len; i++) {
               ret[i] = ((Integer) ret[i]) - 2;
            }
            return ret;
         }
      }
   }

   /**
    * Tests the {@link Iterator} for a {@link NavigableSet}. This may be redundant with some of the
    * other test cases in {@link AbstractTestSortedSet}, but having explicit tests for these cases
    * adds clarity.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestIterator extends AbstractTestIterator {
      AbstractTestNavigableSet outer;
      Iterator<?> testIter;
      Iterator<?> confirmedIter;

      /**
       * Constructs a new test.
       *
       * @param outer the test for the underlying {@link NavigableSet}
       */
      public BulkTestIterator(AbstractTestNavigableSet outer) {
         super("");
         this.outer = outer;
      }

      /**
       * Retrieves the iterator to test from the given set. This allows us to re-use this class
       * and just override this method in order to also test descending iterators.
       *
       * @param set the set whose iterator is returned
       * @return an iterator over the elements in {@code set}
       */
      protected Iterator<?> makeIterator(Set<?> set) {
         return set.iterator();
      }
      
      /**
       * Creates an iterator that checks each method call on the iterator under test to make sure
       * their results match those of the "confirmed" iterator.
       *
       * @return an iterator that checks method calls
       */
      private Iterator<?> makeCheckingIterator() {
         return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
               boolean ret = testIter.hasNext();
               assertEquals("hasNext() should return same value as confirmed iterator",
                     confirmedIter.hasNext(), ret);
               return ret;
            }

            @Override
            public Object next() {
               Object ret;
               try {
                  ret = testIter.next();
               } catch (NoSuchElementException e) {
                  // confirmed should behave the same
                  try {
                     confirmedIter.next();
                     fail("next() should only throw exception when confirmed iterator throws exception");
                  } catch (NoSuchElementException expected) {
                  }
                  throw e;
               }
               assertEquals("next() should return same value as confirmed iterator",
                     confirmedIter.next(), ret);
               return ret;
            }

            @Override
            public void remove() {
               // remove from both iterators
               testIter.remove();
               confirmedIter.remove();
               // mutations to underlying sets will be checked during verify() step
            }
         };
      }
      
      @Override
      public Iterator<?> makeEmptyIterator() {
         outer.resetEmpty();
         testIter = makeIterator(outer.getSet());
         confirmedIter = makeIterator(outer.getConfirmedSet());
         return makeCheckingIterator();
      }

      @Override
      public Iterator<?> makeFullIterator() {
         outer.resetFull();
         testIter = makeIterator(outer.getSet());
         confirmedIter = makeIterator(outer.getConfirmedSet());
         return makeCheckingIterator();
      }
      
      @Override
      public void verify() {
         super.verify();
         // make sure to verify the underlying set, too -- especially important when testing
         // Iterator.remove()
         outer.verify();
      }
      
      @Override
      public void testRemove() {
         super.testRemove();
         // final verification now that remove() has been called
         verify();
      }
   }
   
   /**
    * Tests the {@linkplain NavigableSet#descendingIterator() descending iterator} for a
    * {@link NavigableSet}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestDescendingIterator extends BulkTestIterator {
      
      /**
       * Constructs a new test.
       *
       * @param outer the test for the underlying {@link NavigableSet}
       */
      public BulkTestDescendingIterator(AbstractTestNavigableSet outer) {
         super(outer);
      }
      
      @Override
      protected Iterator<?> makeIterator(Set<?> set) {
         return ((NavigableSet<?>) set).descendingIterator();
      }
   }

}
