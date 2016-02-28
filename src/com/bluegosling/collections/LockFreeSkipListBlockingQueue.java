package com.bluegosling.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A blocking ordered queue that uses a concurrent skip list to store elements. This queue is not
 * bounded.
 *
 * @param <E> the type of element held in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class LockFreeSkipListBlockingQueue<E> extends AbstractLockFreeBlockingQueue<E>
      implements OrderedQueue<E> {
   
   /**
    * An entry in the queue. The underlying storage is a set, but an ordered queue allows
    * duplicates. So we make the values unique by associating each with a unique ID.
    *
    * @param <E> the type value in the entry
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Entry<E> implements Comparable<Entry<E>> {
      /**
       * Returns a comparator of entries that uses the given comparator to compare values.
       *
       * @param comparator a comparator of values
       * @return a comparator of entries
       */
      static <E> Comparator<Entry<E>> comparator(Comparator<? super E> comparator) {
         return (e1, e2) -> {
            int c = comparator.compare(e1.value, e2.value);
            return c == 0 ? Long.compare(e1.id, e2.id) : c;
         };
      }

      private static final AtomicLong idGenerator = new AtomicLong();

      final E value;
      final long id;
      
      Entry(E value) {
         this.value = value;
         this.id = idGenerator.getAndIncrement();
      }

      @Override
      public int compareTo(Entry<E> o) {
         @SuppressWarnings("unchecked")
         int c = ((Comparable<? super E>) value).compareTo(o.value);
         return c == 0 ? Long.compare(id, o.id) : c;
      }
   }
   
   private final ConcurrentSkipListSet<Entry<E>> skipList;
   private final Comparator<? super E> comparator; 
   
   public LockFreeSkipListBlockingQueue() {
      this.skipList = new ConcurrentSkipListSet<>();
      this.comparator = null;
   }
   
   public LockFreeSkipListBlockingQueue(Comparator<? super E> comparator) {
      this.skipList = new ConcurrentSkipListSet<>(Entry.comparator(comparator));
      this.comparator = comparator;
   }

   @Override
   public boolean offer(E e) {
      skipList.add(new Entry<>(e));
      // We have no correct way of knowing exactly when to signal, unless we introduced additional
      // book-keeping. Always signaling should be fine. When a no-op, it amounts to a couple of
      // volatile reads, which would typically be cheaper than the CAS we'd need for book-keeping.
      signalNotEmpty();
      return true;
   }

   @Override
   public E poll() {
      Entry<E> entry = skipList.pollFirst();
      return entry == null ? null : entry.value;
   }

   @Override
   public E peek() {
      Iterator<Entry<E>> iter = skipList.iterator();
      return iter.hasNext() ? iter.next().value : null;
   }

   @Override
   public Comparator<? super E> comparator() {
      return comparator;
   }

   @Override
   public Iterator<E> iterator() {
      return new TransformingIterator<>(skipList.iterator(), e -> e.value);
   }

   @Override
   public boolean isEmpty() {
      return skipList.isEmpty();
   }
   
   @Override
   public int size() {
      return skipList.size();
   }

   @Override
   public int remainingCapacity() {
      return Integer.MAX_VALUE;
   }
}
