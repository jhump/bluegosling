// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import org.apache.commons.collections.BulkTest;

import java.util.NavigableSet;
import java.util.TreeSet;

import junit.framework.TestSuite;

/**
 * Tests the implementation of concurrent {@link NavigableSet}s returned from
 * {@link ShardedConcurrentSets#withNavigableSet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ShardedConcurrentNavigableSetTest extends AbstractTestNavigableSet {

   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-sets, etc.).
    *
    * @return a test suite that includes all test cases
    */
   public static TestSuite suite() {
      return makeSuite(ShardedConcurrentNavigableSetTest.class);
   }
   
   /**
    * Constructs a new test.
    *
    * @param name the name of the test case
    */
   public ShardedConcurrentNavigableSetTest(String name) {
      super(name);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<?> makeEmptySet() {
      return ShardedConcurrentSets.withNavigableSet(new TreeSet<Object>()).create();
   }
   
   @Override
   public boolean isNullSupported() {
      return false;
   }

   @Override
   public boolean isFailFastSupported() {
      return false;
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
    * Returns a bulk test with cases that verify concurrent access
    * behavior.
    *
    * @return a bulk test
    */
   public BulkTest bulkTestConcurrentAccess() {
      return new ShardedConcurrentSetTest.BulkTestConcurrentAccess(this);
   }
}
