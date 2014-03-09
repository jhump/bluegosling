package com.apriori.collections;

import com.apriori.collections.PriorityQueue.Entry;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Utility methods for working with instances of {@link PriorityQueue} (not to be mistaken for
 * {@link java.util.PriorityQueue}).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: test
public final class PriorityQueues {
   private PriorityQueues() {
   }
   
   /**
    * A type of value that automatically associates itself with a queue entry when added to a
    * {@link PriorityQueue}. This only works when using the object's {@link
    * #add(Object, PriorityQueue) method}.
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
    * A simple meldable ordered queue. You can attempt to meld it with any other instance of
    * {@link MeldableQueue}. A runtime exception will be thrown if the two queues are not actually
    * of compatible types for melding.
    *
    * @param <E> the type 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface MeldableQueue<E>
         extends MeldableOrderedQueue<E, MeldableOrderedQueue<? extends E, MeldableQueue<E>>> {
   }
   
   /**
    * Creates a priority queue entry. This method should be used when adding items to a queue
    * returned by {@link #asQueue}.
    * 
    * <p>If the same element and priority are to be added to multiple queues, create multiple
    * entries. Adding the returned entry to more than one queue will result in an
    * {@link IllegalStateException} being thrown. Trying to {@linkplain Entry#remove() remove} the
    * entry before it has actually been added to a queue will also result in an
    * {@link IllegalStateException} being thrown.
    *
    * @param element the element
    * @param priority the priority for the given element
    * @return an entry that represents the given element at the given priority
    */
   public static <E, P> Entry<E, P> entry(E element, P priority) {
      return new AssociatingEntry<E, P>(element, priority);
   }

   /**
    * Returns a view of a {@link PriorityQueue} as an {@link OrderedQueue}. The
    * {@link #entry(Object, Object)} method must be used to create objects that are added to the
    * returned queue. Otherwise, adding an entry will result in a {@link ClassCastException}.
    *
    * @param priorityQueue a priority queue
    * @return a view of the given priority queue as a {@link OrderedQueue}
    */
   public static <E, P> OrderedQueue<Entry<E, P>> asQueue(PriorityQueue<E, P> priorityQueue) {
      return new QueueImpl<E, P>(priorityQueue);
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
   public static <E, P, Q extends MeldablePriorityQueue<E, P, Q>>
         MeldableQueue<Entry<E, P>> asQueue(Q priorityQueue) {
      return new MeldableQueueImpl<E, P>(priorityQueue);
   }
   
   /**
    * A simple entry that becomes associated with another after being added to a queue.
    *
    * @param <E> the type of the element
    * @param <P> the type of the element's priority
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AssociatingEntry<E, P> implements Entry<E, P> {
      private E element;
      private P priority;
      private Entry<E, P> associate;
      
      AssociatingEntry(E element, P priority) {
         this.priority = priority;
         this.element = element;
      }
      
      @Override
      public P getPriority() {
         return associate == null ? priority : associate.getPriority();
      }

      @Override
      public E getElement() {
         return associate == null ? element : associate.getElement();
      }

      @Override
      public void setPriority(P newPriority) {
         if (associate == null) {
            this.priority = newPriority;
         } else {
            associate.setPriority(newPriority);
         }
      }
      
      void setAssociate(Entry<E, P> associate) {
         if (this.associate != null) {
            throw new IllegalStateException(
                  "This entry can only be used once and has already been added to a priority queue");
         }
         this.associate = associate;
      }

      @Override
      public void setElement(E newElement) {
         if (associate == null) {
            this.element = newElement;
         } else {
            associate.setElement(newElement);
         }
      }

      @Override
      public void remove() {
         if (associate == null) {
            throw new IllegalStateException("This entry must first be added to a priority queue");
         }
         associate.remove();
      }
      
      @Override
      public boolean equals(Object o) {
         return PriorityQueueUtils.equals(this,  o);
      }
      
      @Override
      public int hashCode() {
         return PriorityQueueUtils.hashCode(this);
      }
      
      @Override
      public String toString() {
         return PriorityQueueUtils.toString(this);
      }
   }
   
   /**
    * An {@link OrderedQueue}, implemented on top of a {@link PriorityQueue}.
    *
    * @param <E> the type of elements in the queue
    * @param <P> the type of priorities associated with elements in the queue
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class QueueImpl<E, P> extends AbstractQueue<Entry<E, P>> 
         implements OrderedQueue<Entry<E, P>> {
      
      final PriorityQueue<E, P> priorityQueue;
      
      QueueImpl(PriorityQueue<E, P> priorityQueue) {
         this.priorityQueue = priorityQueue;
      }
      
      @Override
      public boolean offer(Entry<E, P> e) {
         AssociatingEntry<E, P> entry = (AssociatingEntry<E, P>) e;
         Entry<E, P> associate = priorityQueue.offer(e.getElement(), e.getPriority());
         if (associate == null) {
            return false;
         }
         entry.setAssociate(associate);
         return true;
      }

      @Override
      public Entry<E, P> poll() {
         return priorityQueue.poll();
      }

      @Override
      public Entry<E, P> peek() {
         return priorityQueue.peek();
      }

      @Override
      public int size() {
         return priorityQueue.size();
      }

      @Override
      public boolean isEmpty() {
         return priorityQueue.isEmpty();
      }

      @Override
      public Iterator<Entry<E, P>> iterator() {
         return priorityQueue.iterator();
      }

      @Override
      public Object[] toArray() {
         return priorityQueue.toArray();
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> T[] toArray(T[] a) {
         if (a.getClass().getComponentType() != Entry.class) {
            throw new ArrayStoreException();
         }
         Entry<E, P> entryArray[] = (Entry<E, P>[]) a;
         return (T[]) priorityQueue.toArray(entryArray);
      }

      @Override
      public boolean remove(Object o) {
         if (o instanceof Entry) {
            for (Entry<E, P> entry : this) {
               if (o.equals(entry)) {
                  entry.remove();
                  return true;
               }
            }
         }
         return false;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return priorityQueue.containsAll(c);
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         boolean ret = false;
         for (Object o : c) {
            if (remove(o)) {
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         ArrayList<Entry<E, P>> toRemove = new ArrayList<Entry<E, P>>();
         for (Entry<E, P> entry : this) {
            if (!c.contains(entry)) {
               toRemove.add(entry);
            }
         }
         if (toRemove.isEmpty()) {
            return false;
         } else {
            removeAll(toRemove);
            return true;
         }
      }

      @Override
      public void clear() {
         priorityQueue.clear();
      }
      
      @Override
      public Comparator<? super Entry<E, P>> comparator() {
         return new Comparator<Entry<E, P>>() {
            @Override
            public int compare(Entry<E, P> o1, Entry<E, P> o2) {
               Comparator<? super P> comp = priorityQueue.comparator();
               if (comp == null) {
                  comp = CollectionUtils.NATURAL_ORDERING;
               }
               return comp.compare(o1.getPriority(), o2.getPriority());
            }
         };
      }
      
      @Override
      public String toString() {
         return CollectionUtils.toString(this);
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
   private static class MeldableQueueImpl<E, P> extends QueueImpl<E, P>
         implements MeldableQueue<Entry<E, P>> {
      
      <Q extends MeldablePriorityQueue<E, P, Q>> MeldableQueueImpl(Q priorityQueue) {
         super(priorityQueue);
      }

      @Override
      public boolean mergeFrom(
            MeldableOrderedQueue<? extends Entry<E, P>, MeldableQueue<Entry<E, P>>> other) {
         @SuppressWarnings({"rawtypes", "unchecked"})
         MeldablePriorityQueue<E, P, ?> otherQueue =
               (MeldablePriorityQueue<E, P, ?>)((MeldableQueueImpl) other).priorityQueue;
         @SuppressWarnings("unchecked")
         MeldablePriorityQueue<E, P, MeldablePriorityQueue<? extends E, ? extends P, ?>> queue =
               (MeldablePriorityQueue<E, P, MeldablePriorityQueue<? extends E, ? extends P, ?>>) priorityQueue;
         
         return queue.mergeFrom(otherQueue);
      }
   }
}
