// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import org.apache.commons.collections.set.AbstractTestSet;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestSuite;

/**
 * Tests the implementation of concurrent {@link Set}s returned from
 * {@link ConcurrentSets#withSet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ConcurrentSetTest extends AbstractTestSet {

   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-sets, etc.).
    *
    * @return a test suite that includes all test cases
    */
   public static TestSuite suite() {
      return makeSuite(ConcurrentSetTest.class);
   }
   
   /**
    * Constructs a new test.
    *
    * @param name the name of the test case
    */
   public ConcurrentSetTest(String name) {
      super(name);
   }

   /** {@inheritDoc} */
   @Override
   public Set<?> makeEmptySet() {
      return ConcurrentSets.withSet(new HashSet<Object>()).create();
   }
   
   @Override
   public boolean isNullSupported() {
      return true;
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

}
