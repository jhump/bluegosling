package com.bluegosling.collections;

import com.bluegosling.reflect.Members;

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
      // Default methods often check preconditions before delegating to other interface methods.
      // So, the way the fail-fast tests work, they will often throw NPE instead of
      // ConcurrentModificationException (because invoking each method is done reflectively, passing
      // nulls and zeros).
      Method actualMethod = Members.findMethod(list.getClass(), m.getName(), m.getParameterTypes());
      if (!actualMethod.isDefault()) {
         super.failFastMethod(list, m);
      }
   }
}
