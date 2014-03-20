package com.apriori.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 * Test cases for {@link CircularBuffer}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class CircularBufferTest {
   
   private void checkEmptyBuffer(CircularBuffer<?> buffer) {
      assertEquals(0, buffer.count());
      try {
         buffer.peek(0);
         fail("Expecting but did not catch IllegalArgumentException");
      }
      catch (IllegalArgumentException expected) {
      }

      try {
         buffer.peekFirst();
         fail("Expecting but did not catch IllegalStateException");
      }
      catch (IllegalStateException expected) {
      }
      
      try {
         buffer.peekLast();
         fail("Expecting but did not catch IllegalStateException");
      }
      catch (IllegalStateException expected) {
      }

      try {
         buffer.removeFirst();
         fail("Expecting but did not catch IllegalStateException");
      }
      catch (IllegalStateException expected) {
      }

      try {
         buffer.removeLast();
         fail("Expecting but did not catch IllegalStateException");
      }
      catch (IllegalStateException expected) {
      }
      
      // check that all refs are null
      for (Object o : buffer.elements) {
         assertNull(o);
      }
   }
   
   @Test public void newBuffer() {
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(2);
      assertEquals(2, buffer.capacity());
      checkEmptyBuffer(buffer);
      
      // cannot instantiate w/ negative capacity
      try {
         new CircularBuffer<Object>(-1);
         fail("Expected but did not catch IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }

      // or, for that matter, any value less than 2
      try {
         new CircularBuffer<Object>(0);
         fail("Expected but did not catch IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      
      try {
         new CircularBuffer<Object>(1);
         fail("Expected but did not catch IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
   }
   
   @Test public void addFirst() {
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(4);

      buffer.addFirst("abc");
      assertEquals("abc", buffer.peekFirst());
      assertEquals("abc", buffer.peekLast());
      assertEquals("abc", buffer.peek(0));
      assertEquals(1, buffer.count());

      buffer.addFirst("def");
      assertEquals("def", buffer.peekFirst());
      assertEquals("abc", buffer.peekLast());
      assertEquals("def", buffer.peek(0));
      assertEquals("abc", buffer.peek(1));
      assertEquals(2, buffer.count());

      buffer.addFirst("ghi");
      assertEquals("ghi", buffer.peekFirst());
      assertEquals("abc", buffer.peekLast());
      assertEquals("ghi", buffer.peek(0));
      assertEquals("def", buffer.peek(1));
      assertEquals("abc", buffer.peek(2));
      assertEquals(3, buffer.count());
      
      buffer.addFirst("jkl");
      assertEquals("jkl", buffer.peekFirst());
      assertEquals("abc", buffer.peekLast());
      assertEquals("jkl", buffer.peek(0));
      assertEquals("ghi", buffer.peek(1));
      assertEquals("def", buffer.peek(2));
      assertEquals("abc", buffer.peek(3));
      assertEquals(4, buffer.count());
      
      try {
         buffer.addFirst("mno");
         fail("Expecting but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
   }

   @Test public void addLast() {
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(4);

      buffer.addLast("abc");
      assertEquals("abc", buffer.peekFirst());
      assertEquals("abc", buffer.peekLast());
      assertEquals("abc", buffer.peek(0));
      assertEquals(1, buffer.count());

      buffer.addLast("def");
      assertEquals("abc", buffer.peekFirst());
      assertEquals("def", buffer.peekLast());
      assertEquals("abc", buffer.peek(0));
      assertEquals("def", buffer.peek(1));
      assertEquals(2, buffer.count());

      buffer.addLast("ghi");
      assertEquals("abc", buffer.peekFirst());
      assertEquals("ghi", buffer.peekLast());
      assertEquals("abc", buffer.peek(0));
      assertEquals("def", buffer.peek(1));
      assertEquals("ghi", buffer.peek(2));
      assertEquals(3, buffer.count());
      
      buffer.addLast("jkl");
      assertEquals("abc", buffer.peekFirst());
      assertEquals("jkl", buffer.peekLast());
      assertEquals("abc", buffer.peek(0));
      assertEquals("def", buffer.peek(1));
      assertEquals("ghi", buffer.peek(2));
      assertEquals("jkl", buffer.peek(3));
      assertEquals(4, buffer.count());
      
      try {
         buffer.addLast("mno");
         fail("Expecting but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
   }
   
   @Test public void removeFirst() {
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(4);
      buffer.addLast("abc");
      buffer.addLast("def");
      buffer.addLast("ghi");
      buffer.addLast("jkl");
      
      assertEquals("abc", buffer.removeFirst());
      assertEquals(3, buffer.count());
      assertEquals("def", buffer.peekFirst());
      assertEquals("jkl", buffer.peekLast());
      
      assertEquals("def", buffer.removeFirst());
      assertEquals(2, buffer.count());
      assertEquals("ghi", buffer.peekFirst());
      assertEquals("jkl", buffer.peekLast());
      
      assertEquals("ghi", buffer.removeFirst());
      assertEquals(1, buffer.count());
      assertEquals("jkl", buffer.peekFirst());
      assertEquals("jkl", buffer.peekLast());
      
      assertEquals("jkl", buffer.removeFirst());
      checkEmptyBuffer(buffer);
   }

   @Test public void removeLast() {
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(4);
      buffer.addLast("abc");
      buffer.addLast("def");
      buffer.addLast("ghi");
      buffer.addLast("jkl");
      
      assertEquals("jkl", buffer.removeLast());
      assertEquals(3, buffer.count());
      assertEquals("abc", buffer.peekFirst());
      assertEquals("ghi", buffer.peekLast());
      
      assertEquals("ghi", buffer.removeLast());
      assertEquals(2, buffer.count());
      assertEquals("abc", buffer.peekFirst());
      assertEquals("def", buffer.peekLast());

      assertEquals("def", buffer.removeLast());
      assertEquals(1, buffer.count());
      assertEquals("abc", buffer.peekFirst());
      assertEquals("abc", buffer.peekLast());
      
      assertEquals("abc", buffer.removeLast());
      checkEmptyBuffer(buffer);
   }
   
   @Test public void clear() {
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(4);
      assertEquals(0, buffer.count());
      buffer.clear();
      checkEmptyBuffer(buffer);

      buffer.addLast("abc");
      buffer.addLast("def");
      assertEquals(2, buffer.count());
      buffer.clear();
      checkEmptyBuffer(buffer);

      // get the offset away from zero and make sure clear still works
      buffer.addLast("abc");
      buffer.addLast("def");
      buffer.addLast("ghi");
      buffer.addLast("jkl");
      buffer.removeFirst();
      buffer.removeFirst();
      buffer.addLast("mno");
      buffer.addLast("qpr");
      assertEquals(4, buffer.count());
      buffer.clear();
      checkEmptyBuffer(buffer);
   }
   
   private void checkSameContents(CircularBuffer<?> buffer1, CircularBuffer<?> buffer2) {
      assertEquals(buffer1.count(), buffer2.count());
      int maxCapacity = Math.max(buffer1.capacity(), buffer2.capacity());
      for (int i = 0; i < maxCapacity; i++) {
         int index1 = i + buffer1.offset;
         if (index1 >= buffer1.capacity()) {
            index1 -= buffer1.capacity();
         }
         int index2 = i + buffer2.offset;
         if (index2 >= buffer2.capacity()) {
            index2 -= buffer2.capacity();
         }
         if (i >= buffer1.count()) {
            if (i < buffer1.capacity()) {
               assertNull(buffer1.elements[index1]);
            }
            if (i < buffer2.capacity()) {
               assertNull(buffer2.elements[index2]);
            }
         } else {
            assertSame(buffer1.elements[index1], buffer2.elements[index2]);
            assertSame(buffer1.elements[index1], buffer1.peek(i));
            assertSame(buffer2.elements[index2], buffer2.peek(i));
         }
      }
   }
   
   @Test public void changeBufferSize() {
      CircularBuffer<Object> buffer1 = new CircularBuffer<Object>(4);
      buffer1.addLast("abc");
      buffer1.addLast("def");
      buffer1.addLast("ghi");
      buffer1.addLast("jkl");

      // grow
      CircularBuffer<Object> buffer2 = buffer1.changeCapacity(10);
      assertEquals(10, buffer2.capacity());
      checkSameContents(buffer1, buffer2);
      
      try {
         // buffer1 is full
         buffer1.addLast("mno");
         fail("Expecting but did not catch IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      
      // but buffer2 is not
      buffer2.addLast("mno");
      assertEquals("mno", buffer2.peekLast());
      
      try {
         // buffer2 has 5 elements, so this is too small
         buffer2.changeCapacity(4);
         fail("Expecting but did not catch IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      
      // shrink
      CircularBuffer<Object> buffer3 = buffer2.changeCapacity(5);
      checkSameContents(buffer2, buffer3);
      
      // even if source is empty, capacity must be >= 2
      buffer1.clear();
      try {
         buffer1.changeCapacity(1);
         fail("Expected but did not catch IllegalArgumentException");
      } catch (IllegalArgumentException expected) {
      }
      
      CircularBuffer<Object> empty = buffer1.changeCapacity(2);
      checkSameContents(buffer1, empty);
      checkEmptyBuffer(empty);
      
      // change size of buffer with non-zero offset
      buffer1.addFirst("abc");
      buffer1.addFirst("abc");
      buffer1.addFirst("abc");
      buffer1.removeFirst();
      buffer1.removeFirst();
      buffer1.addFirst("abc");
      buffer1.addFirst("abc");
      buffer1.addFirst("abc");
      buffer2 = buffer1.changeCapacity(6);
      checkSameContents(buffer1, buffer2);
   }
   
   private void checkSameContents(CircularBuffer<?> buffer, Deque<?> deque) {
      assertEquals(deque.size(), buffer.count());
      Iterator<?> iter = deque.iterator();
      for (int i = 0; i < buffer.capacity(); i++) {
         int index = i + buffer.offset;
         if (index >= buffer.capacity()) {
            index -= buffer.capacity();
         }
         if (i < buffer.count()) {
            assertSame(iter.next(), buffer.elements[index]);
            assertSame(buffer.elements[index], buffer.peek(i));
         } else {
            assertFalse(iter.hasNext());
            assertNull(buffer.elements[index]);
         }
      }
      assertFalse(iter.hasNext());
   }
   
   @Test public void deque() {
      // test the double-ended-queuing functionality
      CircularBuffer<Object> buffer = new CircularBuffer<Object>(10);
      ArrayDeque<Object> deque = new ArrayDeque<Object>(10);
      
      Random rand = new Random(1);
      // add to end, remove from front
      for (int i = 0; i < 100; i++) {
         int numItems = rand.nextInt(10);
         for (int n = 0; n < numItems; n++) {
            Integer value = i + n;
            buffer.addLast(value);
            deque.addLast(value);
            checkSameContents(buffer, deque);
         }
         for (int n = 0; n < numItems; n++) {
            assertSame(buffer.removeFirst(), deque.removeFirst());
            checkSameContents(buffer, deque);
         }
      }
      // change direction: add to front, remove from end
      for (int i = 0; i < 100; i++) {
         int numItems = rand.nextInt(10);
         for (int n = 0; n < numItems; n++) {
            Integer value = i + n;
            buffer.addFirst(value);
            deque.addFirst(value);
            checkSameContents(buffer, deque);
         }
         for (int n = 0; n < numItems; n++) {
            assertSame(buffer.removeLast(), deque.removeLast());
            checkSameContents(buffer, deque);
         }
      }
   }
   
   private void shiftingLeftToRight(CircularBuffer<Object> buffer1, CircularBuffer<Object> buffer2,
         IntConsumer shift) {
      Random rand = new Random(1);
      Deque<Object> deque1 = new ArrayDeque<Object>();
      Deque<Object> deque2 = new ArrayDeque<Object>();
      for (int i = 0; i < 100; i++) {
         if (buffer1.count() == 0) {
            assertTrue(deque1.isEmpty());
            // put items into left buffer
            int numItems = rand.nextInt(buffer1.capacity()) + 1;
            while (numItems > 0) {
               Integer v = numItems;
               buffer1.addFirst(v);
               deque1.addFirst(v);
               checkSameContents(buffer1, deque1);
               numItems--;
            }
         }
         int numToShift = Math.min(rand.nextInt(buffer1.count()) + 1, buffer2.capacity());
         while (numToShift > buffer2.remainingCapacity()) {
            assertSame(deque2.removeLast(), buffer2.removeLast());
            checkSameContents(buffer2, deque2);
         }
         // shift elements in buffers
         shift.accept(numToShift);
         // and do the same in the deques
         while (numToShift > 0) {
            // can shift one element at a time
            deque2.addFirst(deque1.removeLast());
            numToShift--;
         }
         // and make sure we come up with same results
         checkSameContents(buffer1, deque1);
         checkSameContents(buffer2, deque2);
      }
   }
   
   @Test public void pullLast() {
      final CircularBuffer<Object> left = new CircularBuffer<Object>(10);
      final CircularBuffer<Object> right = new CircularBuffer<Object>(20);
      shiftingLeftToRight(left, right, (i) -> right.pullLast(i, left));
   }

   @Test public void pushFirst() {
      final CircularBuffer<Object> left = new CircularBuffer<Object>(10);
      final CircularBuffer<Object> right = new CircularBuffer<Object>(20);
      shiftingLeftToRight(left, right, (i) -> left.pushFirst(i, right));
   }
   
   private void shiftingRightToLeft(CircularBuffer<Object> buffer1, CircularBuffer<Object> buffer2,
         IntConsumer shift) {
      Random rand = new Random(1);
      Deque<Object> deque1 = new ArrayDeque<Object>();
      Deque<Object> deque2 = new ArrayDeque<Object>();
      for (int i = 0; i < 100; i++) {
         if (buffer2.count() == 0) {
            assertTrue(deque2.isEmpty());
            // put items into left buffer
            int numItems = rand.nextInt(buffer2.capacity()) + 1;
            while (numItems > 0) {
               Integer v = numItems;
               buffer2.addFirst(v);
               deque2.addFirst(v);
               checkSameContents(buffer2, deque2);
               numItems--;
            }
         }
         int numToShift = Math.min(rand.nextInt(buffer2.count()) + 1, buffer1.capacity());
         while (numToShift > buffer1.remainingCapacity()) {
            assertSame(deque1.removeLast(), buffer1.removeLast());
            checkSameContents(buffer1, deque1);
         }
         // shift elements in buffers
         shift.accept(numToShift);
         // and do the same in the deques
         while (numToShift > 0) {
            // can shift one element at a time
            deque1.addLast(deque2.removeFirst());
            numToShift--;
         }
         // and make sure we come up with same results
         checkSameContents(buffer1, deque1);
         checkSameContents(buffer2, deque2);
      }
   }
   
   @Test public void pullFirst() {
      final CircularBuffer<Object> left = new CircularBuffer<Object>(10);
      final CircularBuffer<Object> right = new CircularBuffer<Object>(20);
      shiftingRightToLeft(left, right, (i) -> left.pullFirst(i, right));
   }

   @Test public void pushLast() {
      final CircularBuffer<Object> left = new CircularBuffer<Object>(10);
      final CircularBuffer<Object> right = new CircularBuffer<Object>(20);
      shiftingRightToLeft(left, right, (i) -> right.pushLast(i, left));
   }
   
   private void shiftingEndToFront(CircularBuffer<Object> buffer, IntConsumer shift) {
      Random rand = new Random(1);
      Deque<Object> deque = new ArrayDeque<Object>();
      for (int i = 0; i < 100; i++) {
         if (buffer.count() == 0 || rand.nextInt(10) == 0) {
            // first pass, and ~10% of the time, we re-build the buffer
            buffer.clear();
            deque.clear();
            int numItems = rand.nextInt(buffer.capacity()) + 1;
            while (numItems > 0) {
               Integer v = numItems;
               buffer.addFirst(v);
               deque.addFirst(v);
               checkSameContents(buffer, deque);
               numItems--;
            }
         }
         int numToShift = rand.nextInt(buffer.count()) + 1;
         shift.accept(numToShift);
         // and do the same in the deques
         while (numToShift > 0) {
            // can shift one element at a time
            deque.addFirst(deque.removeLast());
            numToShift--;
         }
         // and make sure we come up with same results
         checkSameContents(buffer, deque);
      }
   }
   
   @Test public void pullLast_toFromSelf() {
      final CircularBuffer<Object> buffer = new CircularBuffer<Object>(10);
      shiftingEndToFront(buffer, (i) -> buffer.pullLast(i, buffer));
   }

   @Test public void pushFirst_toFromSelf() {
      final CircularBuffer<Object> buffer = new CircularBuffer<Object>(10);
      shiftingEndToFront(buffer, (i) -> buffer.pushFirst(i, buffer));
   }

   private void shiftingFrontToEnd(CircularBuffer<Object> buffer, IntConsumer shift) {
      Random rand = new Random(1);
      Deque<Object> deque = new ArrayDeque<Object>();
      for (int i = 0; i < 100; i++) {
         if (buffer.count() == 0 || rand.nextInt(10) == 0) {
            // first pass, and ~10% of the time, we re-build the buffer
            buffer.clear();
            deque.clear();
            int numItems = rand.nextInt(buffer.capacity()) + 1;
            while (numItems > 0) {
               Integer v = numItems;
               buffer.addFirst(v);
               deque.addFirst(v);
               checkSameContents(buffer, deque);
               numItems--;
            }
         }
         int numToShift = rand.nextInt(buffer.count()) + 1;
         shift.accept(numToShift);
         // and do the same in the deques
         while (numToShift > 0) {
            // can shift one element at a time
            deque.addLast(deque.removeFirst());
            numToShift--;
         }
         // and make sure we come up with same results
         checkSameContents(buffer, deque);
      }
   }
   
   @Test public void pullFirst_toFromSelf() {
      final CircularBuffer<Object> buffer = new CircularBuffer<Object>(10);
      shiftingFrontToEnd(buffer, (i) -> buffer.pullFirst(i, buffer));
   }

   @Test public void pushLast_toFromSelf() {
      final CircularBuffer<Object> buffer = new CircularBuffer<Object>(10);
      shiftingFrontToEnd(buffer, (i) -> buffer.pushLast(i, buffer));
   }
}
