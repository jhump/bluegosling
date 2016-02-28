package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@code TreeList} class using the list tests provided in the Apache Commons Collections
 * library.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@RunWith(BulkTestRunner.class)
public class TreeListTest extends AbstractTestList {
   
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
