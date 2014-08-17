package com.apriori.collections;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestSuite;

/**
 * Tests the {@code TreeList} class using the list tests provided in the Apache Commons Collections
 * library.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TreeListTest extends AbstractTestList {
   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-lists, iterators, etc.).
    *
    * @return a test suite that includes all test cases for {@code TreeList}
    */
   public static TestSuite suite() {
      return makeSuite(TreeListTest.class);
   }
   
   /**
    * Constructs a new test case.
    * 
    * @param testName the name of the test (provided at runtime by the JUnit test runner)
    */
   public TreeListTest(String testName) {
      super(testName);
   }

   @Override
   public List<?> makeEmptyList() {
      return new TreeList<Object>();
   }

   @Override
   public List<?> makeFullList() {
      return new TreeList<Object>(Arrays.asList(getFullElements()));
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
