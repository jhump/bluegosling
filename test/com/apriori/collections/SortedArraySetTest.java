// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.NavigableSet;

/**
 * Tests the {@code SortedArraySet} class using the sorted set tests provided in the Apache Commons
 * Collections library.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@RunWith(BulkTestRunner.class)
public class SortedArraySetTest extends AbstractTestNavigableSet {
   
   /**
    * Constructs a new test case.
    * 
    * @param name the name of the test (provided at runtime by the JUnit test runner)
    */
   public SortedArraySetTest(String name) {
      super(name);
   }

   // TODO: add additional tests for methods specific to SortedArraySet, like constructors,
   // trimToSize(), etc.

   @Override
   public NavigableSet<?> makeEmptySet() {
      return new SortedArraySet<Object>();
   }

   @Override
   public boolean isFailFastSupported() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
