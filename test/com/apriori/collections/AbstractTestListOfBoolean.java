package com.apriori.collections;

import java.util.concurrent.atomic.AtomicBoolean;


public abstract class AbstractTestListOfBoolean extends AbstractTestList {

   private static final AtomicBoolean useTrueInFullElements = new AtomicBoolean();
   
   private Boolean[] fullElements;
   private final Object[] otherElements;
   
   public AbstractTestListOfBoolean(String testName) {
      super(testName);
      boolean useTrue;
      // thread-safe, in case a parallel test runner is in use
      while (true) {
         useTrue = useTrueInFullElements.get();
         if (useTrueInFullElements.compareAndSet(useTrue, !useTrue)) {
            break;
         }
      }
      // tests expect that "other" elements don't overlap w/ full elements, so
      // we don't have much choice in how to set them up
      fullElements = new Boolean[] { useTrue, useTrue, useTrue, useTrue, useTrue, useTrue };
      otherElements = new Object[] { !useTrue, !useTrue };
   }

   @Override
   public Object[] getFullElements() {
      return fullElements;
   }

   @Override
   public Object[] getOtherElements() {
      return otherElements;
   }
   
   @Override
   public void testListEquals() {
      // this test expects full elements to be different when reversed, so a list of
      // all the same values won't work
      fullElements = new Boolean[] { true, false, false, true, true, true };
   }
   
   @Override
   public void testCollectionIteratorFailFast() {
      // this test expects that retaining a sublist (from index 2 to 5) will change the list,
      // so a list of all the same values won't work
      fullElements = new Boolean[] { true, true, true, true, true, true, false, false };
   }
   
   @Override
   public void testCollectionRetainAll() {
      // this test does a check that retaining just part of the original list will change the
      // list (which doesn't work when list has all the same value), but it does this check
      // only conditionally, when full elements has more than one value... so disable the check
      fullElements = new Boolean[] { true };
   }
}
