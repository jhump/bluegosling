package com.apriori.collections;

import java.lang.reflect.Method;
import java.util.List;

/**
 * A shim to make Apache's {@code AbstractTestList} work with Java 8. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractTestList
      extends org.apache.commons.collections.list.AbstractTestList {

   public AbstractTestList(String testName) {
      super(testName);
   }

   @Override protected void failFastMethod(@SuppressWarnings("rawtypes") List list, Method m) {
      // Default methods often check preconditions. So, the way the fail-fast tests
      // work, they will often throw NPE instead of ConcurrentModificationException
      // (because invoking each method is done reflectively, passing nulls and zeros).
      if (!m.isDefault()) {
         super.failFastMethod(list, m);
      }
   }
}
