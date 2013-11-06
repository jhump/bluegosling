package com.apriori.collections;

import junit.framework.TestSuite;

public class HashSequenceTrieTest extends AbstractTestSequenceTrie {

   public static TestSuite suite() {
      return makeSuite(HashSequenceTrieTest.class);
   }
   
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
