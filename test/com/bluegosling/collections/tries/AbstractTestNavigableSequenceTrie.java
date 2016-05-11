package com.bluegosling.collections.tries;

import java.util.Arrays;

import org.apache.commons.collections.BulkTest;

// TODO: add tests for NavigableMap methods or extend AbstractTestNavigableMap?
public abstract class AbstractTestNavigableSequenceTrie extends AbstractTestSequenceTrie {
   

   public AbstractTestNavigableSequenceTrie(String testName) {
      super(testName);
   }

   @Override public abstract NavigableSequenceTrie<Object, Object> makeEmptyMap();

   @Override public Object[] getSampleKeys() {
      return new Object[] {
            Arrays.asList(),
            Arrays.asList(1, 2, 3),
            Arrays.asList(1, 2, 3, 4),
            Arrays.asList(1, 2, 3, 4, 5),
            Arrays.asList(1, 2, 3, 4, 5, 6),
            Arrays.asList(1, 2, 3, 9),
            Arrays.asList(1, 2, 3, 9, 9),
            Arrays.asList(1, 2, 3, 9, 9, 9),
            Arrays.asList(1, 2, 3, 9, 9, 8),
            Arrays.asList(5, 4, 3, 2, 1),
            Arrays.asList(10, 11, 12),
            Arrays.asList(10, 11, 12, 13),
            Arrays.asList(10, 11, 12, 13, 14),
            Arrays.asList(10, 11, 12, 14),
            Arrays.asList(10, 11, 12, 13, 14, 15),
            Arrays.asList(10, 11, 12, 15)
      };
   }   

   @Override public Object[] getOtherKeys() {
      return new Object[] {
            Arrays.asList(42, 43, 44),
            Arrays.asList(26, 25, 24),
            Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
            Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16),
            Arrays.asList(24, 25, 26),
            Arrays.asList(13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23),
            Arrays.asList(0)
      };
   }
   
   public BulkTest bulkTestPrefixMap() {
      return new BulkTestNavigablePrefixMap(this) {};
   }
   
   // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
   // only runnable when instantiated by enclosing test.
   public abstract static class BulkTestNavigablePrefixMap
         extends AbstractTestSequenceTrie.BulkTestPrefixMap {

      public BulkTestNavigablePrefixMap(AbstractTestSequenceTrie outer) {
         super(outer);
      }

      @Override public SequenceTrie<Object, Object> makeEmptyMap() {
         return outer.makeFullMap().prefixMap(0);
      }

      @Override public Object[] getOtherKeys() {
         return outer.getOtherKeys();
      }

      @Override
      public BulkTest bulkTestPrefixMap() {
         return new BulkTestRecursiveNavigablePrefixMap(this) {};
      }
   }
   
   // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
   // only runnable when instantiated by enclosing test.
   public abstract static class BulkTestRecursiveNavigablePrefixMap
         extends AbstractTestSequenceTrie.BulkTestRecursivePrefixMap {

      public BulkTestRecursiveNavigablePrefixMap(BulkTestPrefixMap outer) {
         super(outer);
      }

      @Override public SequenceTrie<Object, Object> makeEmptyMap() {
         return outer.makeFullMap().prefixMap(0);
      }
}   
}
