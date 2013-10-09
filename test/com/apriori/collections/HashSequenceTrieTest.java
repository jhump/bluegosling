package com.apriori.collections;

import java.util.Map;

import junit.framework.TestSuite;

public class HashSequenceTrieTest extends AbstractTestSequenceTrie {

   public static TestSuite suite() {
      return makeSuite(HashSequenceTrieTest.class);
   }
   
   public HashSequenceTrieTest(String testName) {
      super(testName);
   }

   @Override
   public Map<Iterable<Object>, Object> makeEmptyMap() {
      return new HashSequenceTrie<Object, Object>();
   }
}
