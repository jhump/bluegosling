// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.concurrent.ShardedConcurrentSets;
import com.bluegosling.testing.BulkTestRunner;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.set.AbstractTestSortedSet;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests the implementation of concurrent {@link SortedSet}s returned from
 * {@link ShardedConcurrentSets#withSortedSet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@RunWith(BulkTestRunner.class)
public class ShardedConcurrentSortedSetTest extends AbstractTestSortedSet {

   /**
    * Constructs a new test.
    *
    * @param name the name of the test case
    */
   public ShardedConcurrentSortedSetTest(String name) {
      super(name);
   }

   /** {@inheritDoc} */
   @Override
   public Set<?> makeEmptySet() {
      return ShardedConcurrentSets.withSortedSet(new TreeSet<Object>()).create();
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
