package com.bluegosling.collections.tries;

import java.util.List;

import org.junit.runner.RunWith;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.testing.BulkTestRunner;

@RunWith(BulkTestRunner.class)
public class SortedArrayCompositeTrieTest extends AbstractTestNavigableCompositeTrie {

   public SortedArrayCompositeTrieTest(String testName) {
      super(testName);
   }

   @Override
   public NavigableCompositeTrie<List<Object>, Object, Object> makeEmptyMap() {
      return new SortedArrayCompositeTrie<>(l -> l, CollectionUtils.naturalOrder());
   }

   @Override
   public boolean isAllowNullKey() {
      // elements in the key list can be null, but the key itself cannot be
      return false;
   }
}
