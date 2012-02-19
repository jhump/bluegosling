package com.apriori.collections;

import org.apache.commons.collections.list.AbstractTestList;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@code ArrayBackedLinkedList} class using the list tests provided in the Apache Commons
 * Collections library.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ArrayBackedLinkedListTest extends AbstractTestList {

   /**
    * Constructs a new test case.
    * 
    * @param testName the name of the test (provided at runtime by the JUnit test runner)
    */
   public ArrayBackedLinkedListTest(String testName) {
      super(testName);
   }

   @Override
   public List<?> makeEmptyList() {
      return new ArrayBackedLinkedList<Object>();
   }

   @Override
   public List<?> makeFullList() {
      return new ArrayBackedLinkedList<Object>(Arrays.asList(getFullElements()));
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
