package com.bluegosling.collections.immutable;

import com.bluegosling.collections.AbstractTestList;
import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

//TODO: test cases for PersistentMap methods
@RunWith(BulkTestRunner.class)
public class LinkedPersistentListTest extends AbstractTestList {

   public LinkedPersistentListTest(String testName) {
      super(testName);
   }
   
   @Override
   public List<Object> makeEmptyList() {
      return LinkedPersistentList.create();
   }

   @Override
   public List<Object> makeFullList() {
      return LinkedPersistentList.create(Arrays.asList(getFullElements()));
   }
   
   @Override
   public boolean isAddSupported() {
      return false;
   }

   @Override
   public boolean isSetSupported() {
      return false;
   }

   @Override
   public boolean isRemoveSupported() {
      return false;
   }
   
   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
