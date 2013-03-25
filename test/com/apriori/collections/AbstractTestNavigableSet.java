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
import java.util.SortedSet;

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
    * Determines if the values returned by {@link NavigableSet#headSet(Object)} return a
    * {@link NavigableSet}. The interface is defined only to return {@link SortedSet} but many
    * implementations are in fact {@link NavigableSet}s.
    * 
    * <p>Defaults to true if {@link #isSubSetReturningNavigableSet()} returns true so that the one
    * method can be overridden to change assumption for all of head-sets, tail-sets, and sub-sets.
    * 
    * @return true to cast head-sets to {@link NavigableSet}
    */
   public boolean isHeadSetReturningNavigableSet() {
      return isSubSetReturningNavigableSet();
   }

   /**
    * Determines if the values returned by {@link NavigableSet#tailSet(Object)} return a
    * {@link NavigableSet}. The interface is defined only to return {@link SortedSet} but many
    * implementations are in fact {@link NavigableSet}s.
    * 
    * <p>Defaults to true if {@link #isSubSetReturningNavigableSet()} returns true so that the one
    * method can be overridden to change assumption for all of head-sets, tail-sets, and sub-sets.
    * 
    * @return true to cast tail-sets to {@link NavigableSet}
    */
   public boolean isTailSetReturningNavigableSet() {
      return isSubSetReturningNavigableSet();
   }

   /**
    * Determines if the values returned by {@link NavigableSet#subSet(Object, Object)} return a
    * {@link NavigableSet}. The interface is defined only to return {@link SortedSet} but many
    * implementations are in fact {@link NavigableSet}s.
    * 
    * <p>Defaults to true. Sub-classes must override to change this setting.
    * 
    * @return true to cast sub-sets to {@link NavigableSet}
    */
   public boolean isSubSetReturningNavigableSet() {
      return true;
   }
   
   BulkTest makeSubSetBulkTest(int bound, boolean head) {
      return new BulkTestSubSet(bound, head);
   }

   BulkTest makeSubSetBulkTest(int lobound, int hibound) {
      return new BulkTestSubSet(lobound, hibound);
   }
   
   @Override
   public BulkTest bulkTestSortedSetSubSet() {
      int length = getFullElements().length;
      int lobound = length / 3;
      int hibound = lobound * 2;
      return makeSubSetBulkTest(lobound, hibound);
   }

   @Override
   public BulkTest bulkTestSortedSetHeadSet() {
      int length = getFullElements().length;
      int lobound = length / 3;
      int hibound = lobound * 2;
      return makeSubSetBulkTest(hibound, true);
   }

   @Override
   public BulkTest bulkTestSortedSetTailSet() {
      int length = getFullElements().length;
      int lobound = length / 3;
      return makeSubSetBulkTest(lobound, false);
   }

   /**
    * Adds test cases for the methods of {@link NavigableSet}.
    * 
    * @return a bulk test
    */
   public BulkTest bulkTestNavigableSet() {
      return new BulkTestNavigableSet(this, false, false);
   }
   
   /**
    * An enumeration of sub-set types. In addition to <em>sub-set</em>, this also includes
    * head-set and tail-set types.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static enum SubSetType {
      
      /** A type that represents a {@linkplain NavigableSet#headSet(Object) head-set}. */
      HEAD() {
         @Override
         public boolean isNavigableSet(AbstractTestNavigableSet test) {
            return test.isHeadSetReturningNavigableSet();
         }
      },
      
      /** A type that represents a {@linkplain NavigableSet#tailSet(Object) tail-set}. */
      TAIL() {
         @Override
         public boolean isNavigableSet(AbstractTestNavigableSet test) {
            return test.isTailSetReturningNavigableSet();
         }
      },
      
      /** A type that represents a {@linkplain NavigableSet#subSet(Object, Object) sub-set}. */
      SUB() {
         @Override
         public boolean isNavigableSet(AbstractTestNavigableSet test) {
            return test.isSubSetReturningNavigableSet();
         }
      };
      
      /**
       * Returns true if this sub-set type will implement {@link NavigableSet} instead of just
       * implementing {@link SortedSet}.
       *
       * @param test the test for the underlying {@link NavigableSet}
       * @return true if this sub-set type implements {@link NavigableSet}
       */
      public abstract boolean isNavigableSet(AbstractTestNavigableSet test);
   }

   /**
    * A bulk set of test cases for the methods of {@link NavigableSet}. This adds the tests using
    * composition instead of inheritance so we can also decorate head-, tail-, and sub-set tests
    * with these same additional tests.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestNavigableSet extends BulkTest {

      private final AbstractTestSortedSet test;
      private final boolean forSubSet;
      private final boolean forDescendingSet;
      
      /**
       * Constructs a new test.
       *
       * @param test the tests for the {@link SortedSet} super-interface
       * @param forSubSet true if this is adding tests for a head-set, tail-set, or sub-set
       * @param forDescendingSet true if this is adding tests for a descending set
       */
      public BulkTestNavigableSet(AbstractTestSortedSet test, boolean forSubSet,
            boolean forDescendingSet) {
         super("");
         this.test = test;
         this.forSubSet = forSubSet;
         this.forDescendingSet = forDescendingSet;
      }
      
      /**
       * Adds test cases in bulk for testing {@link NavigableSet#descendingSet()}.
       *
       * @return tests
       */
      public BulkTest bulkTestDescendingSet() {
         if (forDescendingSet) {
            return null; // prevent infinite recursion
         }
         return new BulkTestDescendingSet(test, forSubSet);
      }

      /**
       * Adds test cases in bulk for testing {@link NavigableSet#iterator()}.
       *
       * @return tests
       */
      public BulkTest bulkTestIterator() {
         return new BulkTestIterator(test);
      }
      
      /**
       * Adds test cases in bulk for testing {@link NavigableSet#descendingIterator()}.
       *
       * @return tests
       */
      public BulkTest bulkTestDescendingIterator() {
         return new BulkTestDescendingIterator(test);
      }
      
      /**
       * Tests {@link NavigableSet#ceiling(Object)}.
       */
      @SuppressWarnings("unchecked")
      public void testCeiling() {
         
         test.resetEmpty();
         Object items[] = test.getFullElements();
         NavigableSet<Object> set = ((NavigableSet<Object>) test.getSet());
         assertNull("ceiling() on empty set should always be null",
               set.ceiling(items[0]));
         assertNull("ceiling() on empty set should always be null",
               set.ceiling(items[items.length-1]));
         test.verify();
         
         test.resetFull();
         set = ((NavigableSet<Object>) test.getSet());
         // an item in the set
         Object item = items[items.length / 2];
         assertEquals("ceiling() for item in set should return that item",
               item, set.ceiling(item));
         // an item not in the set
         items = test.getOtherElements();
         item = items[0];
         Object nextItem = forDescendingSet ? ((Integer) item) - 1 : ((Integer) item) + 1;
         assertEquals("ceiling() for item not in set should return the next item",
               nextItem, set.ceiling(item));
         // an item outside range of set -- low
         items = test.getFullElements();
         item = items[0];
         Object prevItem = forDescendingSet ? ((Integer) item) + 1 : ((Integer) item) - 1;
         assertEquals("ceiling() for item lower than set range should return first item",
               item, set.ceiling(prevItem));
         // an item outside range of set -- high
         items = test.getFullElements();
         item = items[items.length - 1];
         nextItem = forDescendingSet ? ((Integer) item) - 1 : ((Integer) item) + 1;
         assertNull("ceiling() for item higher than set range should return null",
               set.ceiling(nextItem));
         
         test.verify();
      }
      
      // TODO: add additional tests for the other methods of NavigableSet (ceiling, floor, higher,
      // and lower) and for add'l ways to create sub-sets (head-, tail-, and sub-sets with
      // inclusive/exclusive bounds).
   }

   /**
    * Test cases for the head-, tail-, or sub-set of a {@link NavigableSet}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public class BulkTestSubSet extends AbstractTestSortedSet.TestSortedSetSubSet {
      
      SubSetType type;
      
      /**
       * Constructs a new test.
       * 
       * @param bound the bound of this sub-set
       * @param head true if this is a head-set, false if a tail-set
       */
      public BulkTestSubSet(int bound, boolean head) {
         AbstractTestNavigableSet.this.super(bound, head);
         type = head ? SubSetType.HEAD : SubSetType.TAIL;
      }
      
      /**
       * Constructs a new test.
       *
       * @param lobound the lower bound of this sub-set
       * @param hibound the upper bound of this sub-set
       */
      public BulkTestSubSet(int lobound, int hibound) {
         AbstractTestNavigableSet.this.super(lobound, hibound);
         type = SubSetType.SUB;
      }
      
      BulkTest makeNavigableSetBulkTest() {
         return new BulkTestNavigableSet(this, true, false);
      }
      
      /**
       * Adds test cases for the methods of {@link NavigableSet}.
       *
       * @return a bulk test
       */
      public BulkTest bulkTestNavigableSet() {
         return type.isNavigableSet(AbstractTestNavigableSet.this) ?
               makeNavigableSetBulkTest() : null;
      }
   }

   /**
    * Tests for a the descending view of a {@link NavigableSet}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestDescendingSet extends AbstractTestNavigableSet {
      
      AbstractTestSortedSet outer;
      boolean forSubSet;
      
      /**
       * Constructs a new test.
       * 
       * @param outer the test for the underlying {@link NavigableSet}
       * @param forSubSet true if this test is for a head-set, tail-set, or sub-set
       */
      public BulkTestDescendingSet(AbstractTestSortedSet outer, boolean forSubSet) {
         super("");
         this.outer = outer;
         this.forSubSet = forSubSet;
      }
      
      @Override
      public BulkTest bulkTestNavigableSet() {
         return new BulkTestNavigableSet(this, forSubSet, true);
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
      BulkTest makeSubSetBulkTest(int bound, boolean head) {
         if (forSubSet) {
            return null; // prevent infinite recursion
         }
         return new BulkTestDescendingSubSet(bound, head);
      }

      @Override
      BulkTest makeSubSetBulkTest(int lobound, int hibound) {
         if (forSubSet) {
            return null; // prevent infinite recursion
         }
         return new BulkTestDescendingSubSet(lobound, hibound);
      }
      
      @Override
      public NavigableSet<?> makeEmptySet() {
         return ((NavigableSet<?>) outer.makeEmptySet()).descendingSet();
      }
      
      private Object[] reverse(Object array[]) {
         array = array.clone();
         ArrayUtils.reverse(array);
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
      public class BulkTestDescendingSubSet extends BulkTestSubSet {

         /**
          * Constructs a new test.
          * 
          * @param bound the bound of this sub-set
          * @param head true if this is a head-set, false if a tail-set
          */
         public BulkTestDescendingSubSet(int bound, boolean head) {
            super(bound, head);
         }

         /**
          * Constructs a new test.
          *
          * @param lobound the lower bound of this sub-set
          * @param hibound the upper bound of this sub-set
          */
         public BulkTestDescendingSubSet(int lobound, int hibound) {
            super(lobound, hibound);
         }
         
         @Override
         BulkTest makeNavigableSetBulkTest() {
            return new BulkTestNavigableSet(this, true, true);
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
            Object ret[] = super.getOtherElements().clone();
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
      AbstractTestSortedSet outer;
      Iterator<?> testIter;
      Iterator<?> confirmedIter;

      /**
       * Constructs a new test.
       *
       * @param outer the test for the underlying {@link NavigableSet}
       */
      public BulkTestIterator(AbstractTestSortedSet outer) {
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
      public BulkTestDescendingIterator(AbstractTestSortedSet outer) {
         super(outer);
      }
      
      @Override
      protected Iterator<?> makeIterator(Set<?> set) {
         return ((NavigableSet<?>) set).descendingIterator();
      }
   }

}
