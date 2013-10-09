package com.apriori.collections;

import java.util.Arrays;

/**
 * A simple fixed-capacity buffer of object references. Items can be pushed to the front or the back
 * of the buffer, so it has deque-like functionality. To support this double-ended behavior, items
 * are stored as a <a href="http://en.wikipedia.org/wiki/Circular_buffer">circular buffer</a>. This
 * buffer does not allow old entries to be automatically overwritten on writes after the buffer is
 * full. Instead, exceptions are thrown if an attempt is made to store more items in the buffer than
 * it can actually hold.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of object in the buffer
 */
public class CircularBuffer<E> {
   
   /**
    * The buffer. It is allocated once with a fixed capacity. When not full, unused spaces in the
    * array will be set to null.
    */
   final Object elements[];
   
   /**
    * The number of actual items in the buffer.
    */
   int count;
   
   /**
    * The offset of the first element in the buffer. To support
    */
   int offset;

   /**
    * Constructs a new buffer with the specified capacity. The capacity must be greater than or
    * equal to two elements.
    * 
    * @param capacity the capacity for the buffer
    * @throws IllegalArgumentException if the specified capacity is less than 2
    */
   public CircularBuffer(int capacity) {
      if (capacity < 2) {
         throw new IllegalArgumentException("buffer capacity must be >= 2");
      }
      this.elements = new Object[capacity];
   }
   
   /**
    * Returns the maximum capacity of the buffer, as specified during construction.
    * 
    * @return the capacity of this buffer
    */
   public int capacity() {
      return elements.length;
   }
   
   /**
    * Returns the number of items in the buffer. This will always be less than or equal to the
    * buffer's capacity.
    * 
    * @return the number of items in the buffer
    */
   public int count() {
      return count;
   }
   
   /**
    * Returns the remaining capacity of the buffer. This is the number of available positions in
    * the buffer. If the buffer is empty then this will return the full capacity. If the buffer is
    * full then this returns zero.
    * 
    * @return the remaining capacity of the buffer
    */
   public int remainingCapacity() {
      return capacity() - count();
   }
   
   /**
    * Adds an item to the front of the buffer. When this method returns, the specified item will
    * be the first item in the buffer.
    * 
    * @param element the item to add
    * @throws IllegalStateException if the buffer is full
    */
   public void addFirst(E element) {
      int capacity = capacity();
      if (count == capacity) {
         throw new IllegalStateException("buffer is full");
      }
      offset--;
      if (offset < 0) {
         offset += capacity;
      }
      elements[offset] = element;
      count++;
   }
   
   /**
    * Removes the first item from the buffer.
    * 
    * @return the item that was removed from the front of the buffer
    * @throws IllegalStateException if the buffer is empty
    */
   public E removeFirst() {
      if (count == 0) {
         throw new IllegalStateException("buffer is empty");
      }
      @SuppressWarnings("unchecked")
      E ret = (E) elements[offset];
      elements[offset] = null;
      int capacity = capacity();
      if (++offset >= capacity) {
         offset -= capacity;
      }
      count--;
      return ret;
   }
   
   /**
    * Adds an item to the end of the buffer. When this method returns, the specified item will
    * be the last item in the buffer.
    * 
    * @param element the item to add
    * @throws IllegalStateException if the buffer is full
    */
   public void addLast(E element) {
      int capacity = capacity();
      if (count == capacity) {
         throw new IllegalStateException("buffer is full");
      }
      int index = offset + count;
      if (index >= capacity) {
         index -= capacity;
      }
      elements[index] = element;
      count++;
   }
   
   /**
    * Removes the last item from the buffer.
    * 
    * @return the item that was removed from the end of the buffer
    * @throws IllegalStateException if the buffer is empty
    */
   public E removeLast() {
      if (count == 0) {
         throw new IllegalStateException("buffer is empty");
      }
      int index = offset + --count;
      int capacity = capacity();
      if (index >= capacity) {
         index -= capacity;
      }
      @SuppressWarnings("unchecked")
      E ret = (E) elements[index];
      elements[index] = null;
      return ret;
   }
   
   /**
    * Shifts the specified number of elements from the end of this buffer to the front.
    * Conceptually, this is the same as the following, but this method implements it in a more
    * efficient manner:<pre>
    * for (int i = 0; i < numElements; i++) {
    *   buffer.addFirst(buffer.removeLast());
    * }
    * </pre>
    * 
    * @param numElements the number of elements to shift
    */
   private void shiftFromEndToFront(int numElements) {
      // TODO
   }
   
   /**
    * Pulls elements from the front of the specified buffer and adds them to the end of this buffer.
    * This can be visualized as a right-to-left shift between two adjacent buffers. For example, let
    * there be two buffers, each with a capacity of eight items. The first has 5 items in the buffer
    * (and remaining capacity for three more), the second has 6 items:
    * <pre>
    * Buffer #1                               Buffer #2
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+
    * | A | B | C | D | E |   |   |   |       | F | G | H | I | J | K |   |   |
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+      
    * front            end                    front                end
    * </pre>
    * So the following invocation will shift elements from buffer #2 to buffer #1:<br/>
    * <pre>buffer1.pullFirst(3, buffer2);</pre>
    * This would result in the following state after the operation completes:
    * <pre>
    * Buffer #1                               Buffer #2
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+
    * | A | B | C | D | E | F | G | H |       | I | J | K |   |   |   |   |   |      
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+
    * front                        end        front    end
    * </pre>
    * The items are removed from the source buffer and moved into this buffer.
    * 
    * <p>This shifts items in the opposite direction as {@link #pullLast(int, CircularBuffer)}. It
    * shifts in the same direction as {@link #pushLast(int, CircularBuffer)} but with the roles of
    * source and destination swapped.
    * 
    * @param numElements the number of elements to pull
    * @param source the other buffer from which the elements are pulled
    * @throws IllegalArgumentException if the specified number of items is too large to fit in this
    *       buffer's remaining capacity, if it is greater than the actual number of items in the
    *       specified source buffer, or if it is less than zero
    * @throws NullPointerException if the specified source buffer is null
    */
   public void pullFirst(int numElements, CircularBuffer<? extends E> source) {
      if (numElements < 0) {
         throw new IllegalArgumentException(
               "specified number of items must not be negative: " + numElements);
      }
      int capacity = capacity();
      if (numElements > source.count) {
         throw new IllegalArgumentException("insufficient items in source");
      }
      
      // if source and dest are both this buffer, we need to handle it differently
      if (source == this) {
         shiftFromEndToFront(numElements);
         return;
      }
      
      // otherwise, we're moving elements from one buffer to a different one
      if (count + numElements > capacity) {
         throw new IllegalArgumentException("insufficient capacity");
      }

      int sourceCapacity = source.capacity();
      int sourceOffset = source.offset;
      int currOffset = offset + count;
      if (currOffset >= capacity) {
         currOffset -= capacity;
      }
      int elementsRemaining = numElements;
      while (elementsRemaining > 0) {
         int chunkSizeA = currOffset + elementsRemaining > capacity
               ? capacity - currOffset : elementsRemaining;
         int chunkSizeB = sourceOffset + elementsRemaining > sourceCapacity
               ? sourceCapacity - sourceOffset : elementsRemaining;
         int chunkSize = Math.min(chunkSizeA, chunkSizeB);
         // copy data over
         System.arraycopy(source.elements, sourceOffset, elements, currOffset, chunkSize);
         // null out references in source
         Arrays.fill(source.elements, sourceOffset, sourceOffset + chunkSize, null);
         
         sourceOffset += chunkSize;
         if (sourceOffset >= sourceCapacity) {
            sourceOffset -= sourceCapacity;
         }
         currOffset += chunkSize;
         if (currOffset >= capacity) {
            currOffset -= capacity;
         }
         elementsRemaining -= chunkSize;
      }

      source.offset = sourceOffset;
      source.count -= numElements;
      count += numElements;
   }

   /**
    * Shifts the specified number of elements from the front of this buffer to the end.
    * Conceptually, this is the same as the following, but this method implements it in a more
    * efficient manner:<pre>
    * for (int i = 0; i < numElements; i++) {
    *   buffer.addLast(buffer.removeFirst());
    * }
    * </pre>
    * 
    * @param numElements the number of elements to shift
    */
   private void shiftFromFrontToEnd(int numElements) {
      // TODO
   }
   
   /**
    * Pulls elements from the end of the specified buffer and adds them to the front of this buffer.
    * This can be visualized as a left-to-right shift between two adjacent buffers. For example, let
    * there be two buffers, each with a capacity of eight items. The first has 5 items in the
    * buffer, the second has 6 items (and remaining capacity for two more):
    * <pre>
    * Buffer #1                               Buffer #2
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+
    * | A | B | C | D | E |   |   |   |       | F | G | H | I | J | K |   |   |
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+      
    * front            end                    front                end
    * </pre>
    * So the following invocation will shift elements from buffer #1 to buffer #2:<br/>
    * <pre>buffer2.pullLast(2, buffer1);</pre>
    * This would result in the following state after the operation completes:
    * <pre>
    * Buffer #1                               Buffer #2
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+
    * | A | B | C |   |   |   |   |   |       | D | E | F | G | H | I | J | K |      
    * +---+---+---+---+---+---+---+---+       +---+---+---+---+---+---+---+---+
    * front                        end        front    end
    * </pre>
    * The items are removed from the source buffer and moved into this buffer.
    * 
    * <p>This shifts items in the opposite direction as {@link #pullFirst(int, CircularBuffer)}. It
    * shifts in the same direction as {@link #pushFirst(int, CircularBuffer)} but with the roles of
    * source and destination swapped.
    * 
    * @param numElements the number of elements to pull
    * @param source the other buffer from which the elements are pulled
    * @throws IllegalArgumentException if the specified number of items is too large to fit in this
    *       buffer's remaining capacity, if it is greater than the actual number of items in the
    *       specified source buffer, or if it is less than zero
    * @throws NullPointerException if the specified source buffer is null
    */
   public void pullLast(int numElements, CircularBuffer<? extends E> source) {
      if (numElements < 0) {
         throw new IllegalArgumentException(
               "specified number of items must not be negative: " + numElements);
      }
      int capacity = capacity();
      if (numElements > source.count) {
         throw new IllegalArgumentException("insufficient items in source");
      }
      
      // if source and dest are both this buffer, we need to handle it differently
      if (source == this) {
         shiftFromFrontToEnd(numElements);
         return;
      }

      // otherwise, we're moving elements from one buffer to a different one
      if (count + numElements > capacity) {
         throw new IllegalArgumentException("insufficient capacity");
      }

      int sourceCapacity = source.capacity();
      int sourceOffset = source.offset + source.count - numElements;
      if (sourceOffset >= sourceCapacity) {
         sourceOffset -= sourceCapacity;
      }
      if (count > 0) {
         offset -= numElements;
         if (offset < 0) {
            offset += capacity;
         }
      }
      int currOffset = offset;
      int elementsRemaining = numElements;
      while (elementsRemaining > 0) {
         int chunkSizeA = currOffset + elementsRemaining > capacity
               ? capacity - currOffset : elementsRemaining;
         int chunkSizeB = sourceOffset + elementsRemaining > sourceCapacity
               ? sourceCapacity - sourceOffset : elementsRemaining;
         int chunkSize = Math.min(chunkSizeA, chunkSizeB);
         // copy data over
         System.arraycopy(source.elements, sourceOffset, elements, currOffset, chunkSize);
         // null out references in source
         Arrays.fill(source.elements, sourceOffset, sourceOffset + chunkSize, null);
         
         sourceOffset += chunkSize;
         if (sourceOffset >= sourceCapacity) {
            sourceOffset -= sourceCapacity;
         }
         currOffset += chunkSize;
         if (currOffset >= capacity) {
            currOffset -= capacity;
         }
         elementsRemaining -= chunkSize;
      }

      source.count -= numElements;
      count += numElements;
   }

   /**
    * Pushes to the front of the specified buffer elements that are removed from the end of this
    * buffer. This can be visualized as a left-to-right shift between two adjacent buffers. The
    * following two statements are equivalent:<pre>
    * dest.pullLast(numElements, source);
    * source.pushFirst(numElements, dest);
    * </pre>
    *  
    * @param numElements the number of elements to push
    * @param dest the other buffer to which the elements are pushed
    * @throws IllegalArgumentException if the specified number of items is too large to fit in the
    *       destintation buffer's remaining capacity, if it is greater than the actual number of
    *       items in this buffer, or if it is less than zero
    * @throws NullPointerException if the specified destination buffer is null
    * 
    * @see #pullLast(int, CircularBuffer)
    */
   public void pushFirst(int numElements, CircularBuffer<? super E> dest) {
      dest.pullLast(numElements, this);
   }

   /**
    * Pushes to the end of the specified buffer elements that are removed from the front of this
    * buffer. This can be visualized as a right-to-left shift between two adjacent buffers. The
    * following two statements are equivalent:<pre>
    * dest.pullFirst(numElements, source);
    * source.pushLast(numElements, dest);
    * </pre>
    *  
    * @param numElements the number of elements to push
    * @param dest the other buffer to which the elements are pushed
    * @throws IllegalArgumentException if the specified number of items is too large to fit in the
    *       destintation buffer's remaining capacity, if it is greater than the actual number of
    *       items in this buffer, or if it is less than zero
    * @throws NullPointerException if the specified destination buffer is null
    * 
    * @see #pullFirst(int, CircularBuffer)
    */
   public void pushLast(int numElements, CircularBuffer<? super E> dest) {
      dest.pullFirst(numElements, this);
   }

   /**
    * Returns the first item in the buffer.
    * 
    * @return the first item in the buffer
    * @throws IllegalStateException if the buffer is empty
    */
   public E peekFirst() {
      if (count == 0) {
         throw new IllegalStateException("buffer is empty");
      }
      return peek(0);
   }
   
   /**
    * Returns the last item in the buffer.
    * 
    * @return the last item in the buffer
    * @throws IllegalStateException if the buffer is empty
    */
   public E peekLast() {
      if (count == 0) {
         throw new IllegalStateException("buffer is empty");
      }
      return peek(count - 1);
   }
   
   /**
    * Returns the item in the buffer at the specified position, where zero is the position of the
    * first item.
    * 
    * @param index the position of the item to return
    * @return the item at the specified position in the buffer
    * @throws IllegalArgumentException if the specified position is less than zero or is greater
    *       than or equal to the number of elements in the buffer
    */
   @SuppressWarnings("unchecked")
   public E peek(int index) {
      if (index < 0 || index >= count) {
         throw new IllegalArgumentException("invalid index: " + index);
      }
      int bufferIndex = offset + index;
      int capacity = capacity();
      if (bufferIndex >= capacity) {
         bufferIndex -= capacity;
      }
      return (E) elements[bufferIndex];
   }
   
   /**
    * Creates a new buffer with the same elements as the current buffer but with a different
    * capacity.
    * 
    * @param newCapacity the capacity of the new buffer
    * @return a new buffer with the same elements as this buffer but with a different capacity
    * @throws IllegalArgumentException if the specified capacity is too small to hold the current
    *       buffer contents or if the specified capacity is less than two (the minimum allowed
    *       capacity)
    */
   public CircularBuffer<E> changeCapacity(int newCapacity) {
      if (newCapacity < count) {
         throw new IllegalArgumentException("specified capacity insufficient for buffer contents");
      }
      if (newCapacity < 2) {
         throw new IllegalArgumentException("buffer capacity must be >= 2");
      }
      CircularBuffer<E> other = new CircularBuffer<E>(newCapacity);
      if (count > 0) {
         int capacity = capacity();
         if (count + offset > capacity) {
            int chunk1 = capacity - offset; // first chunk
            System.arraycopy(elements, offset, other.elements, 0, chunk1);
            int chunk2 = count - chunk1; // second chunk 
            System.arraycopy(elements, 0, other.elements, chunk1, chunk2);
         } else {
            System.arraycopy(elements, offset, other.elements, 0, count);
         }
         other.count = count;
      }
      return other;
   }
   
   /**
    * Clears all elements from this buffer. Upon this method returning, the buffer will be empty.
    */
   public void clear() {
      int capacity = capacity();
      int overflow = offset + count - capacity;
      if (overflow > 0) {
         Arrays.fill(elements, offset, capacity, null);
         Arrays.fill(elements, 0, overflow, null);
      } else {
         Arrays.fill(elements, offset, offset + count, null);
      }
      count = offset = 0;
   }
   
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[");
      boolean first = true;
      int capacity = capacity();
      for (int i = 0, o = offset; i < count; i++, o++) {
         if (o >= capacity) {
            o -= capacity;
         }
         if (first) {
            first = false;
         } else {
            sb.append(", ");
         }
         sb.append(elements[o]);
      }
      sb.append("]");
      return sb.toString();
   }
}
