package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.OrderedDeque;
import com.bluegosling.collections.views.TransformingIterator;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;


public class ConcurrentSkipListOrderedQueue<E> extends AbstractQueue<E> implements OrderedDeque<E> {
   
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
   
   private final ConcurrentSkipListMap<Entry<E>, Boolean> skipList;
   private final Comparator<? super E> comparator;
   
   public ConcurrentSkipListOrderedQueue() {
      this.skipList = new ConcurrentSkipListMap<>();
      this.comparator = null;
   }
   
   public ConcurrentSkipListOrderedQueue(Comparator<? super E> comparator) {
      this.skipList = new ConcurrentSkipListMap<>(Entry.comparator(comparator));
      this.comparator = comparator;
   }

   @Override
   public boolean offer(E e) {
      skipList.put(new Entry<>(e), Boolean.TRUE);
      return true;
   }

   @Override
   public Comparator<? super E> comparator() {
      return comparator;
   }

   @Override
   public E pollFirst() {
      Map.Entry<Entry<E>, Boolean> e = skipList.pollFirstEntry();
      Entry<E> entry = e == null ? null : e.getKey();
      return entry == null ? null : entry.value;
   }

   @Override
   public E peekFirst() {
      Map.Entry<Entry<E>, Boolean> e = skipList.firstEntry();
      Entry<E> entry = e == null ? null : e.getKey();
      return entry == null ? null : entry.value;
   }

   @Override
   public E pollLast() {
      Map.Entry<Entry<E>, Boolean> e = skipList.pollLastEntry();
      Entry<E> entry = e == null ? null : e.getKey();
      return entry == null ? null : entry.value;
   }

   @Override
   public E peekLast() {
      Map.Entry<Entry<E>, Boolean> e = skipList.lastEntry();
      Entry<E> entry = e == null ? null : e.getKey();
      return entry == null ? null : entry.value;
   }

   @Override
   public Iterator<E> iterator() {
      return new TransformingIterator<>(skipList.keySet().iterator(), e -> e.value);
   }

   @Override
   public int size() {
      return skipList.size();
   }
}
