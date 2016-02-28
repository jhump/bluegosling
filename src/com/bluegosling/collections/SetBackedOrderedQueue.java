package com.bluegosling.collections;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An ordered queue that is backed by a navigable set and breaks ties in FIFO manner.
 *
 * @param <E> the type of elements in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SetBackedOrderedQueue<E> extends AbstractQueue<E> implements OrderedQueue<E> {
   
   @FunctionalInterface
   public interface SetFactory {
      <T> NavigableSet<T> newSet(Comparator<? super T> comparator);
   }
   
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
   
   public static <E extends Comparable<E>> SetBackedOrderedQueue<E> create(SetFactory factory) {
      return create(factory, Comparator.naturalOrder());
   }

   public static <E> SetBackedOrderedQueue<E> create(SetFactory factory,
         Comparator<? super E> comparator) {
      return new SetBackedOrderedQueue<>(factory, comparator);
   }

   private final NavigableSet<Entry<E>> set;
   private final Comparator<? super E> comparator;
   
   /**
    * Creates a new ordered queue backed by a set constructed using the given factory.
    *
    * @param factory factory to construct the set that backs the ordered queue
    * @param comparator a comparator used to compare and order values
    */
   public SetBackedOrderedQueue(SetFactory factory, Comparator<? super E> comparator) {
      this.set = factory.newSet(Entry.comparator(comparator));
      this.comparator = comparator == Comparator.naturalOrder() ? null : comparator;
   }

   @Override
   public boolean offer(E e) {
      boolean added = set.add(new Entry<>(e));
      assert added;
      return true;
   }

   @Override
   public E poll() {
      Entry<E> entry = set.pollFirst();
      return entry == null ? null : entry.value;
   }

   @Override
   public E peek() {
      Iterator<Entry<E>> iter = set.iterator();
      return iter.hasNext() ? iter.next().value : null;
   }

   @Override
   public Comparator<? super E> comparator() {
      return comparator;
   }

   @Override
   public Iterator<E> iterator() {
      return new TransformingIterator<>(set.iterator(), e -> e.value);
   }

   @Override
   public int size() {
      return set.size();
   }
}
