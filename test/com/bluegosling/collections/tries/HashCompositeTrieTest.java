package com.bluegosling.collections.tries;

import com.bluegosling.testing.BulkTestRunner;

import java.util.List;

import org.junit.runner.RunWith;

@RunWith(BulkTestRunner.class)
public class HashCompositeTrieTest extends AbstractTestCompositeTrie {

   public HashCompositeTrieTest(String testName) {
      super(testName);
   }

   @Override
   public CompositeTrie<List<Object>, Object, Object> makeEmptyMap() {
      return new HashCompositeTrie<>(l -> l);
   }
   
   @Override
   public boolean isAllowNullKey() {
      // elements in the key list can be null, but the key itself cannot be
      return false;
   }
}
