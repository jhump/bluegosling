package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

@RunWith(BulkTestRunner.class)
public class HashSequenceTrieTest extends AbstractTestSequenceTrie {

   public HashSequenceTrieTest(String testName) {
      super(testName);
   }

   @Override
   public SequenceTrie<Object, Object> makeEmptyMap() {
      return new HashSequenceTrie<Object, Object>();
   }
   
   @Override
   public boolean isAllowNullKey() {
      // elements in the key list can be null, but the key itself cannot be
      return false;
   }
}
