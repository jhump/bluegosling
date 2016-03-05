package com.bluegosling.collections;

import com.bluegosling.collections.PriorityQueue.Entry;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;

/**
 * Utility methods for working with instances of {@link PriorityQueue} (not to be mistaken for
 * {@link java.util.PriorityQueue}).
 * 
 * <p>The meat of this class includes a {@linkplain AutoEntry useful base class} for value types
 * that are stored into priority queues, and methods for adapting a priority queue to the standard
 * {@link Queue} interface.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class PriorityQueues {
   private PriorityQueues() {
   }
   
   /**
    * A type of value that automatically associates itself with a queue entry when added to a
    * {@link PriorityQueue}. This only works when using the object's {@link #add} method.
    * 
    * <p>This can be very useful since sophisticated usage of a {@link PriorityQueue} requires
    * keeping track of an {@link Entry} after adding a value to the queue. A common pattern is to
    * store a reference to the associated entry in the actual value, after it is added to the queue.
    * This class encapsulates that pattern.
    * 
    * <p>To use, extend this class and add fields that represent the actual object/state being added
    * to a queue. Always use the object's {@link #add} method (instead of using the queue's
    * {@link PriorityQueue#add} method). This class also provides utility methods for interacting
    * with the queue, like for removing the entry or changing its priority.
    *
    * @param <P> the type of priority associated with this object
    * @param <E> the type of this object
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public abstract class AutoEntry<P, E extends AutoEntry<P, E>> {
      private Entry<E, P> entry;
      
      /**
       * Gets the element's associated queue entry. This will be {@code null} if the object has
       * never been {@linkplain #add(Object, PriorityQueue) added to a queue}.
       *
       * @return the element's associated queue entry
       */
      protected Entry<E, P> getEntry() {
         return entry;
      }
      
      /**
       * Adds this element to the given queue with the given priority. The element can only be
       * associated with one queue at any given time. If the object needs to be added to a different
       * queue, it must first be {@linkplain #remove() removed}.
       *
       * @param priority the priority for this object
       * @param queue the queue into which the object will be added
       * @throws IllegalStateException if the object is already associated with a queue
       */
      public void add(P priority, PriorityQueue<E, P> queue) {
         if (entry != null) {
            throw new IllegalStateException("This entry is already associated with another queue");
         }
         @SuppressWarnings("unchecked") // if this class is sub-classes correctly, this is safe
         E element = (E) this;
         entry = queue.add(element, priority);
      }
      
      /**
       * Removes the object from the queue that contains it.
       * 
       * @throws IllegalStateException if this object has never been {@linkplain
       *       #add(Object, PriorityQueue) added} to a queue or has already been removed
       */
      public void remove() {
         if (entry == null) {
            throw new IllegalStateException("This entry is not associated with a queue");
         }
         entry.remove();
         entry = null;
      }

      /**
       * Changes the priority associated with this object. The priority is initially assigned when
       * the element is added to a queue.
       * 
       * @throws IllegalStateException if this object has never been {@linkplain
       *       #add(Object, PriorityQueue) added} to a queue or has already been removed
       */
      public void setPriority(P priority) {
         if (entry == null) {
            throw new IllegalStateException("This entry is not associated with a queue");
         }
         entry.setPriority(priority);
      }
      
      /**
       * Gets the priority associated with this object. The priority is initially assigned when
       * the element is added to a queue.
       * 
       * @return the priority associated with this object
       * @throws IllegalStateException if this object has never been {@linkplain
       *       #add(Object, PriorityQueue) added} to a queue or has already been removed
       */
      public P getPriority() {
         if (entry == null) {
            throw new IllegalStateException("This entry is not associated with a queue");
         }
         return entry.getPriority();
      }
   }

   /**
    * Returns a view of a {@link PriorityQueue} as an {@link OrderedQueue}. The
    * {@link #entry(Object, Object)} method must be used to create objects that are added to the
    * returned queue. Otherwise, adding an entry will result in a {@link ClassCastException}.
    *
    * @param priorityQueue a priority queue
    * @return a view of the given priority queue as a {@link OrderedQueue}
    */
   public static <E> OrderedQueue<E> asQueue(PriorityQueue<E, E> priorityQueue) {
      return new QueueImpl<>(priorityQueue);
   }

   /**
    * A simple meldable ordered queue. You can attempt to meld it with any other instance of
    * {@link MeldableQueue}. A runtime exception will be thrown if the two queues are not actually
    * of compatible types for melding.
    *
    * @param <E> the type 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface MeldableQueue<E>
         extends MeldableOrderedQueue<E, MeldableQueue<? extends E>> {
   }
   
   /**
    * Returns a view of a {@link MeldablePriorityQueue} as a {@link MeldableOrderedQueue}. The
    * {@link #entry(Object, Object)} method must be used to create objects that are added to the
    * returned queue. Otherwise, adding an entry will result in a {@link ClassCastException}.
    * Attempting to meld the returned queue with another whose underlying priority queue is not
    * actually meldable will also result in a {@link ClassCastException}.
    *
    * @param priorityQueue a priority queue
    * @return a view of the given priority queue as a {@link MeldableOrderedQueue}
    */
   public static <E, Q extends MeldablePriorityQueue<E, E, Q>>
         MeldableQueue<E> asQueue(Q priorityQueue) {
      return new MeldableQueueImpl<>(priorityQueue);
   }

   /**
    * An {@link OrderedQueue}, implemented on top of a {@link PriorityQueue}.
    *
    * @param <E> the type of elements in the queue
    * @param <P> the type of priorities associated with elements in the queue
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class QueueImpl<E> extends AbstractQueue<E> 
         implements OrderedQueue<E> {
      
      final PriorityQueue<E, E> priorityQueue;
      
      QueueImpl(PriorityQueue<E, E> priorityQueue) {
         this.priorityQueue = priorityQueue;
      }
      
      @Override
      public boolean offer(E e) {
         return priorityQueue.offer(e, e) != null;
      }

      @Override
      public E poll() {
         Entry<E, E> entry = priorityQueue.poll();
         return entry == null ? null : entry.getElement();
      }

      @Override
      public E peek() {
         Entry<E, E> entry = priorityQueue.peek();
         return entry == null ? null : entry.getElement();
      }

      @Override
      public boolean contains(Object o) {
         return priorityQueue.elements().contains(o);
      }

      @Override
      public boolean remove(Object o) {
         return priorityQueue.elements().remove(o);
      }

      @Override
      public void clear() {
         priorityQueue.clear();
      }
      
      @Override
      public Comparator<? super E> comparator() {
         return priorityQueue.comparator();
      }
      
      @Override
      public String toString() {
         return CollectionUtils.toString(this);
      }

      @Override
      public Iterator<E> iterator() {
         return priorityQueue.elements().iterator();
      }

      @Override
      public int size() {
         return priorityQueue.size();
      }
   }
   
   /**
    * A {@linkplain MeldableOrderedQueue meldable} version of {@link QueueImpl}.
    *
    * @param <E> the type of elements in the queue
    * @param <P> the type of priorities associated with elements in the queue
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class MeldableQueueImpl<E> extends QueueImpl<E> implements MeldableQueue<E> {
      
      <Q extends MeldablePriorityQueue<E, E, Q>> MeldableQueueImpl(Q priorityQueue) {
         super(priorityQueue);
      }

      @Override
      public boolean mergeFrom(MeldableQueue<? extends E> other) {
         @SuppressWarnings("unchecked") // this isn't really unchecked, but confuses compiler...
         MeldablePriorityQueue<? extends E, ? extends E, ?> otherQueue =
               (MeldablePriorityQueue<? extends E, ? extends E, ?>)
               ((MeldableQueueImpl<? extends E>) other).priorityQueue;
         @SuppressWarnings("unchecked")
         MeldablePriorityQueue<E, E, MeldablePriorityQueue<? extends E, ? extends E, ?>> queue =
               (MeldablePriorityQueue<E, E, MeldablePriorityQueue<? extends E, ? extends E, ?>>)
               priorityQueue;
         
         return queue.mergeFrom(otherQueue);
      }
   }
}
