// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestSuite;

/**
 * Tests the implementation of concurrent {@link NavigableSet}s returned from
 * {@link ConcurrentSets#withNavigableSet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ConcurrentNavigableSetTest extends AbstractTestNavigableSet {

   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-sets, etc.).
    *
    * @return a test suite that includes all test cases
    */
   public static TestSuite suite() {
      return makeSuite(ConcurrentNavigableSetTest.class);
   }
   
   /**
    * Constructs a new test.
    *
    * @param name the name of the test case
    */
   public ConcurrentNavigableSetTest(String name) {
      super(name);
   }

   /** {@inheritDoc} */
   @Override
   public Set<?> makeEmptySet() {
      return ConcurrentSets.withNavigableSet(new TreeSet<Object>()).create();
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
}
