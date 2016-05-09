package com.bluegosling.collections.concurrent;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.runner.RunWith;

import com.bluegosling.testing.BulkTestRunner;

@RunWith(BulkTestRunner.class)
public class SimpleTreiberStackTest extends AbstractTestConcurrentStack {
   
   public SimpleTreiberStackTest(String testName) {
      super(testName);
   }

   @Override
   public ConcurrentStack<Object> makeCollection() {
      return new SimpleTreiberStack<>();
   }

   @Override
   public ConcurrentStack<Object> makeFullCollection() {
      return new SimpleTreiberStack<>(Arrays.asList(getFullElements()));
   }
   
   @Override
   public boolean isRemoveSupported() {
      return false;
   }

   @Override
   public void testCollectionClear() {
      /*
       * Below was copied from the superclass, except that the test that checks #isRemoveSupported()
       * has been removed. That way we run the tests for #clear() since it is actually supported,
       * despite #isRemoveSupported() returning false. (Only arbitrary removals are disallowed.)
       */
      resetEmpty();
      collection.clear(); // just to make sure it doesn't raise anything
      verify();

      resetFull();
      collection.clear();
      confirmed.clear();
      verify();
   }

   @Override
   @SuppressWarnings("unchecked") // copied from superclass, which doesn't use generics :(
   public void testUnsupportedRemove() {
      /*
       * Below was copied from the superclass, except that the case for Stack#clear() has been
       * omitted since the clear operation is supported (only arbitrary removes are disallowed).
       */
      resetEmpty();
      try {
          collection.remove(null);
          fail("remove should raise UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
          // expected
      }
      verify();

      try {
          collection.removeAll(null);
          fail("removeAll should raise UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
          // expected
      }
      verify();

      try {
          collection.retainAll(null);
          fail("removeAll should raise UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
          // expected
      }
      verify();

      resetFull();
      try {
          Iterator<?> iterator = collection.iterator();
          iterator.next();
          iterator.remove();
          fail("iterator.remove should raise UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
          // expected
      }
      verify();
  }
   
   // TODO: concurrency test
}
