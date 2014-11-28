package com.apriori.collections;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * An ordered queue that is backed by a navigable map and breaks ties in FIFO manner.
 *
 * @param <E> the type of elements in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MapBackedOrderedQueue<E> extends AbstractQueue<E> implements OrderedQueue<E> {
   
   private final NavigableMap<E, Queue<E>> map;
   private final Supplier<Queue<E>> queueMaker;
   private int size;
   
   /**
    * Creates a new ordered queue backed by the given map. When entries in the map are created, new
    * FIFO queues are constructed using {@code new ArrayDeque<E>(2)}.
    *
    * @param map the map that backs the ordered queue
    */
   public MapBackedOrderedQueue(NavigableMap<E, Queue<E>> map) {
      this(map, () -> new ArrayDeque<>(2));
   }

   /**
    * Creates a new ordered queue backed by the given map. When entries in the map are created, new
    * FIFO queues are constructed using the given supplier.
    *
    * @param map the map that backs the ordered queue
    * @param queueMaker a supplier of new, empty FIFO queues
    */
   public MapBackedOrderedQueue(NavigableMap<E, Queue<E>> map, Supplier<Queue<E>> queueMaker) {
      this.map = map;
      this.queueMaker = queueMaker;
   }

   @Override
   public boolean offer(E e) {
      map.compute(e, (k, q) -> {
         if (q == null) {
            q = queueMaker.get();
         }
         q.add(e);
         return q;
      });
      size++;
      return true;
   }

   @Override
   public E poll() {
      Entry<E, Queue<E>> entry = map.firstEntry();
      if (entry == null) {
         return null;
      }
      Queue<E> q = entry.getValue();
      E e = q.remove();
      if (q.isEmpty()) {
         map.remove(entry.getKey());
      }
      size--;
      return e;
   }

   @Override
   public E peek() {
      Entry<E, Queue<E>> entry = map.firstEntry();
      if (entry == null) {
         return null;
      }
      Queue<E> q = entry.getValue();
      return q.peek();
   }

   @Override
   public Comparator<? super E> comparator() {
      return map.comparator();
   }

   @Override
   public Iterator<E> iterator() {
      return new Iter<E>(map.values().iterator());
   }

   @Override
   public int size() {
      return size;
   }
   
   private static class Iter<E> implements Iterator<E> {
      private final Iterator<Queue<E>> queues;
      private Queue<E> lastFetchedQueue;
      private Queue<E> currentQueue;
      private Iterator<E> current;
      
      Iter(Iterator<Queue<E>> queues) {
         this.queues = queues;
      }
      
      private void findNext() {
         while (queues.hasNext()) {
            currentQueue = queues.next();
            current = currentQueue.iterator();
            if (current.hasNext()) {
               return;
            } else {
               assert currentQueue.isEmpty();
               queues.remove();
            }
         }
      }
      
      @Override
      public boolean hasNext() {
         return current.hasNext() || queues.hasNext();
      }

      @Override
      public E next() {
         if (current == null || !current.hasNext()) {
            findNext();
            if (current == null || !current.hasNext()) {
               throw new NoSuchElementException();
            }
         }
         E e = current.next();
         lastFetchedQueue = currentQueue;
         return e;
      }
      
      @Override
      public void remove() {
         if (lastFetchedQueue == null) {
            throw new IllegalStateException();
         }
         current.remove();
         // purge queue if it becomes empty
         if (currentQueue.isEmpty()) {
            queues.remove();
         }
         lastFetchedQueue = null;
      }
   }
}
