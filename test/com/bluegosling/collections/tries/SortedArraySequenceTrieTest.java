package com.bluegosling.collections.tries;

import org.junit.runner.RunWith;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.testing.BulkTestRunner;

@RunWith(BulkTestRunner.class)
public class SortedArraySequenceTrieTest extends AbstractTestNavigableSequenceTrie {

   public SortedArraySequenceTrieTest(String testName) {
      super(testName);
   }

   @Override
   public NavigableSequenceTrie<Object, Object> makeEmptyMap() {
      return new SortedArraySequenceTrie<>(CollectionUtils.naturalOrder());
   }

   @Override
   public boolean isAllowNullKey() {
      // elements in the key list can be null, but the key itself cannot be
      return false;
   }
}
