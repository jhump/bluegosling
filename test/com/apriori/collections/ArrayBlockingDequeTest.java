package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

@RunWith(BulkTestRunner.class)
public class ArrayBlockingDequeTest extends AbstractTestBlockingDeque {
   
   public ArrayBlockingDequeTest(String testName) {
      super(testName);
   }

   @Override
   public BlockingDeque<Object> makeCollection() {
      // this class doesn't allow unbounded size, but 100k should be big enough for all tests
      return makeCollection(100 * 1024);
   }

   @Override
   public BlockingDeque<Object> makeCollection(int maxCapacity) {
      return new ArrayBlockingDeque<>(maxCapacity);
   }
   
   @Override
   public boolean isRemoveSupported() {
      return false;
   }
   
   @Override
   public void testCollectionClear() {
      // Super-class implementation expects clear() to not be supported if other removes are
      // unsupported. So we have to override to verify the right behavior.
      // This is pretty much the same as the super-class implementation except we skip the
      // query to isRemoveSupported().
      
      resetEmpty();
      collection.clear(); // just to make sure it doesn't raise anything
      verify();

      resetFull();
      collection.clear();
      confirmed.clear();
      verify();
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public void testUnsupportedRemove() {
      // Super-class implementation expects clear() to not be supported if other removes are
      // unsupported. So we have to override to verify the right behavior.
      // This is pretty much the same as the super-class implementation except we exclude a
      // check of clear().

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
         Iterator iterator = collection.iterator();
         iterator.next();
         iterator.remove();
         fail("iterator.remove should raise UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
         // expected
      }
      verify();
   }
   
   public void testWrapping() {
      resetEmpty();
      BlockingQueue<Object> queue = makeCollection(10);
      collection = queue;
      Queue<Object> benchmark = getConfirmed();
      
      List<Object> elements = new ArrayList<>(Arrays.asList(getFullElements()));
      while (elements.size() < 10) {
         elements.addAll(Arrays.asList(getFullElements()));
      }
      
      // this tests many cases of adding and removing that wrap over the end of the internal array
      for (int n = 1; n <= 10; n++) {
         for (int i = 0; i < 20; i++) {
            for (int c = 0; c < n; c++) {
               assertEquals(10 - c, queue.remainingCapacity());
               Object o = elements.get(c);
               assertTrue(queue.offer(o));
               benchmark.add(o);
            }
            if (n == 10) {
               assertFalse(queue.offer(elements.get(0)));
            }
            verify();
            for (int c = 0; c < n; c++) {
               assertEquals(10 - n + c, queue.remainingCapacity());
               Object expected = elements.get(c);
               assertSame(expected, queue.poll());
               benchmark.remove();
            }
            assertEquals(10, queue.remainingCapacity());
            assertTrue(queue.isEmpty());
            verify();
         }
      }
   }
}
