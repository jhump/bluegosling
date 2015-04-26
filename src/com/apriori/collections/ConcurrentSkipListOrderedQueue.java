package com.apriori.collections;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;


public class ConcurrentSkipListOrderedQueue<E> extends AbstractQueue<E> implements OrderedQueue<E> {
   
   private static class Entry<E> implements Comparable<Entry<E>> {
      private static final AtomicLong idGenerator = new AtomicLong();

      static <E> Comparator<Entry<E>> comparator(Comparator<? super E> comparator) {
         return (e1, e2) -> {
            int c = comparator.compare(e1.value, e2.value);
            return c == 0 ? Long.compare(e1.id, e2.id) : c;
         };
      }

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
   
   public ConcurrentSkipListOrderedQueue() {
      this.skipList = new ConcurrentSkipListSet<>();
      this.comparator = null;
   }
   
   public ConcurrentSkipListOrderedQueue(Comparator<? super E> comparator) {
      this.skipList = new ConcurrentSkipListSet<>(Entry.comparator(comparator));
      this.comparator = comparator;
   }

   @Override
   public boolean offer(E e) {
      skipList.add(new Entry<>(e));
      return true;
   }

   @Override
   public E poll() {
      Entry<E> entry = skipList.pollFirst();
      return entry == null ? null : entry.value;
   }

   @Override
   public E peek() {
      Entry<E> entry = skipList.first();
      return entry == null ? null : entry.value;
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
   public int size() {
      return skipList.size();
   }
}
