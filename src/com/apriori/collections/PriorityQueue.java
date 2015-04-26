package com.apriori.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * A priority queue abstract data structure. Conceptually this is like a cross between a normal
 * {@link Queue} and a {@link Map}. Each element in the queue also has a priority, which determines
 * the order of removal of items. It is similar to a map in that the priority of each element can be
 * considered like a key. Unlike a map, multiple elements with the same priority are permitted
 * (expected, actually). This type of data structure is usually backed by a
 * <a href="http://en.wikipedia.org/wiki/Heap_(data_structure)">heap</a>.
 * 
 * <p>This interface varies considerably from {@link java.util.PriorityQueue} since that class does
 * not (in fact, cannot) provide the {@code decrease-key} operation, which is a key part of the
 * abstract data type and required by many graph traversal algorithms. For most implementations,
 * removing an element and re-inserting with the new priority would be much much slower than just
 * changing an element's priority using this operation.
 * 
 * <p>This class does not extend the {@link java.util.Collection} interface due to incompatibilities
 * with the {@link #add} method's signature. Also, this container class has some methods where it
 * acts as a container of elements and other methods where it acts as a container of entries
 * (element + priority), a dichotomy that is not had with standard collections. If you really need a
 * standard collection backed by a priority queue, use
 * {@link PriorityQueues#asQueue(PriorityQueue)}. 
 * 
 * <p>Generally, implementations of this interface do not provide iterators that support
 * {@link Iterator#remove()}. This is because removal of an element often has significant impact on
 * the structure of the queue, so proceeding to iterate through subsequent items after a removal is
 * not feasible.
 * 
 * <p>To remove an element or change its priority (as in the {@code decrease-key} operation), use
 * methods on the {@link Entry} associated with that element.
 *
 * @param <E> the type of the elements in the queue
 * @param <P> the type of the priority associated with each element
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PriorityQueue<E, P> extends SizedIterable<PriorityQueue.Entry<E, P>> {

   /**
    * An entry in a priority queue. This represents an element in the queue and its associated
    * priority.
    *
    * @param <E> the type of element in this queue entry
    * @param <P> the type of the priority associated with each element
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Entry<E, P> {
      /**
       * Gets the element in this entry.
       *
       * @return the element
       */
      E getElement();
      
      /**
       * Changes the value of the element in this entry
       *
       * @param newElement the new element value
       */
      void setElement(E newElement);

      /**
       * Gets the priority associated with the element in this entry.
       *
       * @return the priority
       */
      P getPriority();
      
      /**
       * Changes the priority associated with the element in this entry.
       *
       * @param newPriority the new priority
       * @throws IllegalArgumentException if the queue does not support this type of change (some
       *       queues, for example, may not allow increasing the priority, only decreasing)
       * @throws IllegalStateException if this entry has been removed from its queue or otherwise
       *       has no associated queue
       */
      void setPriority(P newPriority);
      
      /**
       * Removes this entry from the queue.
       * 
       * @throws IllegalStateException if this entry has already been removed or otherwise has no
       *       associated queue
       */
      void remove();
      
      /**
       * Determines if this entry is equal to another. This returns true if and only if the
       * specified object is also an entry, its priority is equal to this entry's priority, and its
       * element is equal to this entry's element.
       *
       * @param o an object
       * @return true if this entry is equal to the specified object; false otherwise
       */
      @Override boolean equals(Object o);
      
      /**
       * Computes the hash code for this entry. The entry's hash code is computed as
       * {@code priority.hashCode() ^ element.hashCode()}. If either priority or element is
       * {@code null} then its hash code is considered to be zero.
       *
       * @return the has code for this entry, computed as described above
       */
      @Override int hashCode();
   }
   
   /**
    * Gets the comparator for this queue. This determines the order of extracting elements from the
    * queue. If priorities are compared using their {@linkplain Comparable natural ordering} then
    * this method may return {@code null}.
    *
    * @return the comparator for this queue, or null if priorities are compared using their
    *       {@linkplain Comparable natural ordering}
    */
   Comparator<? super P> comparator();
   
   /**
    * Returns an array with the same entries as this queue. The order of entries in the returned
    * array is the same as the order that entries are returned by the queue's {@link #iterator()}.
    * 
    * @return an array with the same entries as this queue
    * 
    * @see Collection#toArray()
    */
   Entry<E, P>[] toArray();
   
   /**
    * Returns an array with the same entries as this queue, filling the specified array if it has
    * sufficient capacity. If the array has sufficient capacity, the entries are copied there
    * and a {@code null} terminator is stored after the entries if the array can hold more.
    * Otherwise, a new array is allocated, and the elements are copied into it.
    *
    * @param array an array 
    * @return an array with the same entries as this queue (possibly the same instance as the one
    *       specified)
    * @throws ArrayStoreException if any of the entries are not assignable to the runtime component
    *       type of the specified array
    *       
    * @see Collection#toArray(Object[])
    */
   Entry<E, P>[] toArray(Entry<E, P>[] array);
   
   /**
    * Determines if the specified object is an element in this queue.
    *
    * @param o an object
    * @return true if the specified object is an element in this queue, false otherwise
    */
   boolean containsElement(Object o);
   
   /**
    * Determines if the specified object matches the priority of any element in this queue.
    *
    * @param o an object
    * @return true if the specified object matches the priority of any element in this queue, false
    *       otherwise
    */
   boolean containsPriority(Object o);
   
   /**
    * Determines if this queue contains all of the specified items.
    *
    * @param items a sequence of items
    * @return true if all of the specified items are elements in this queue
    */
   boolean containsAll(Iterable<?> items);
   
   /**
    * Determines if this queue contains any one of the specified items.
    *
    * @param items a sequence of items
    * @return true if any one of the specified items is an element in this queue
    */
   boolean containsAny(Iterable<?> items);
   
   /**
    * Offers an element for insertion into this queue with the specified priority.
    *
    * @param element the new element
    * @param priority the new element's priority
    * @return the entry that corresponds to the newly added element or {@code null} if an entry
    *       could not be added (like due to capacity constraints)
    *       
    * @see Queue#offer(Object)
    */
   Entry<E, P> offer(E element, P priority);
   
   /**
    * Adds an element to the queue with the specified priority. 
    *
    * @param element the new element
    * @param priority the new element's priority
    * @return the entry that corresponds to the newly added element
    * @throws IllegalStateException if an entry could not be added to the queue (like due to
    *       capacity constraints)
    *       
    * @see Queue#add(Object)
    */
   Entry<E, P> add(E element, P priority);
   
   /**
    * Adds all of the specified entries to the this queue. The entry instances themselves will not
    * be associated with this queue. Instead, new entries will be created and added to this queue
    * that have the same elements and corresponding priorities.
    *
    * @param entries the entries whose elements and priorities are to be added to this queue
    */
   void addAll(Iterable<Entry<E, P>> entries);
   
   /**
    * Finds an entry for the specified element.
    *
    * @param element the element to find
    * @return an entry in this queue whose element equals the one specified or {@code null} if no
    *       such element exists in this queue
    */
   Entry<E, P> getEntry(E element);
   
   /**
    * Queries for the minimum entry. The returned entry's priority will be less than or equal to
    * the priority of every other element in the queue.
    *
    * @return the minimum entry in the queue or {@code null} if the queue is empty
    * 
    * @see Queue#peek()
    */
   Entry<E, P> peek();
   
   /**
    * Returns the minimum entry in the queue. The returned entry's priority will be less than or
    * equal to the priority of every other element in the queue.
    *
    * @return the minimum entry in the queue
    * @throws NoSuchElementException if the queue is empty
    * 
    * @see Queue#element()
    */
   Entry<E, P> firstEntry();
   
   /**
    * Removes the minimum entry from the queue if present. The returned entry's priority will be
    * less than or equal to the priority of every other element in the queue.
    *
    * @return the minimum entry in the queue or {@code null} if the queue is empty
    * 
    * @see Queue#poll()
    */
   Entry<E, P> poll();
   
   /**
    * Removes the minimum entry from the queue. The returned entry's priority will be less than or
    * equal to the priority of every other element in the queue.
    *
    * @return the minimum entry in the queue
    * @throws NoSuchElementException if the queue is empty
    * 
    * @see Queue#remove()
    */
   Entry<E, P> remove();
   
   /**
    * Finds and removes an entry from this queue that matches the given element.
    *
    * @param element the element to find
    * @return an entry in this queue whose element is equal to the specified one or {@code null} if
    *       the given element is not in this queue
    */
   Entry<E, P> removeEntry(E element);
   
   /**
    * Finds and removes all entries from this queue whose elements match the given elements.
    *
    * @param objects elements to find
    * @return true if any of the specified objects were found and removed; otherwise false
    */
   boolean removeAll(Iterable<?> objects);

   /**
    * Removes all entries from this queue whose elements do not match the given objects. This
    * mutates this queue into the intersection of its current contents and the specified objects.
    *
    * @param objects elements to find
    * @return true if any of the specified objects were found and removed; otherwise false
    */
   boolean retainAll(Iterable<?> objects);
   
   /**
    * Removes all entries from this queue. On return, the queue will be empty.
    */
   void clear();
   
   /**
    * Finds all entries with the given priority.
    *
    * @param priority the priority to find
    * @return all entries whose elements have the give priority; an empty list if no such entries
    *       are present in the queue
    */
   Collection<Entry<E, P>> getAll(P priority);
}
