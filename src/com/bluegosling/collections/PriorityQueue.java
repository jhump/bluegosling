package com.bluegosling.collections;

import com.bluegosling.collections.views.TransformingCollection;

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
// TODO: finish doc
public interface PriorityQueue<E, P> {

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
   
   int size();
   
   default boolean isEmpty() {
      return size() == 0;
   }

   Collection<Entry<E, P>> entries();

   default Collection<P> priorities() {
      return new TransformingCollection<>(entries(), Entry::getPriority);
   }
   
   default Collection<E> elements() {
      return new TransformingCollection<>(entries(), Entry::getElement);
   }
   
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
    * @param others a queue with entries to add to this queue
    * @throws IllegalStateException if an entry could not be added to the queue (like due to
    *       capacity constraints)
    */
   void addAll(PriorityQueue<? extends E, ? extends P> others);

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
    * Removes all entries from this queue. On return, the queue will be empty.
    */
   void clear();
   
   @Override
   boolean equals(Object o);
   
   @Override
   int hashCode();
}
