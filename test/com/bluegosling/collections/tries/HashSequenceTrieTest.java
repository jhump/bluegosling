package com.bluegosling.collections.tries;

import com.bluegosling.collections.tries.HashSequenceTrie;
import com.bluegosling.collections.tries.SequenceTrie;
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
