// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Set;

import junit.framework.TestSuite;

/**
 * Tests the {@code SortedArraySet} class using the sorted set tests provided in the Apache Commons
 * Collections library.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SortedArraySetTest extends AbstractTestNavigableSet {
   
   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-sets, etc.).
    *
    * @return a test suite that includes all test cases for {@code SortedArraySet}
    */
   public static TestSuite suite() {
      return makeSuite(SortedArraySetTest.class);
   }
   
   /**
    * Constructs a new test case.
    * 
    * @param name the name of the test (provided at runtime by the JUnit test runner)
    */
   public SortedArraySetTest(String name) {
      super(name);
   }

   // TODO: add additional tests for methods specific to SortedArraySet, like trimToSize(), etc.

   @Override
   public Set<?> makeEmptySet() {
      return new SortedArraySet<Object>();
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
}
