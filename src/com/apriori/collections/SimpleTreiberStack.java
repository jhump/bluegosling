package com.apriori.collections;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A very simple, thread-safe stack. This is simple and lighter weight version than
 * {@link TreiberStack}. It does not expose arbitrary remove operations, only {@link #pop}. All
 * operations are atomic.
 * 
 * <p>This structure is safe to use concurrently from multiple threads. The iterator will never
 * throw a {@link ConcurrentModificationException}. However, each single iterator is meant to be
 * used from a single thread and is thus not thread safe. Iterators do not support the
 * {@link Iterator#remove} operation. Iterators are strongly consistent, yielding elements in the
 * stack at the time the iterator was created.
 *
 * @param <T> the type of values stored in the stack
 * 
 * @see TreiberStack
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SimpleTreiberStack<T> implements Stack<T> {
   
   @SuppressWarnings("rawtypes")
   private static final AtomicReferenceFieldUpdater<SimpleTreiberStack, Node> headUpdater =
         AtomicReferenceFieldUpdater.newUpdater(SimpleTreiberStack.class, Node.class, "head");
   
   /**
    * A linked list node that comprises the stack. Due to the limited API (no internal removes),
    * the list is also a persistent data structure, similar to {@link LinkedPersistentList}.
    *
    * @param <T> the type of value in the node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Node<T> {
      final T value;
      final Node<T> next;
      final int size;
      
      Node(T value, Node<T> next, int size) {
         this.value = value;
         this.next = next;
         this.size = size;
      }
   }
   
   volatile Node<T> head;
   
   public SimpleTreiberStack() {
   }

   SimpleTreiberStack(Node<T> head) {
      this.head = head;
   }

   @Override
   public Iterator<T> iterator() {
      return new Iterator<T>() {
         private Node<T> next = head;
         
         @Override
         public boolean hasNext() {
            return next != null;
         }

         @Override
         public T next() {
            if (next != null) {
               T ret = next.value;
               next = next.next;
               return ret;
            }
            throw new NoSuchElementException();
         }
      };
   }

   @Override
   public int size() {
      Node<T> node = head;
      return node != null ? node.size : 0;
   }

   @Override
   public void push(T value) {
      while (true) {
         Node<T> oldNode = head;
         Node<T> newNode = new Node<T>(value, oldNode, oldNode != null ? oldNode.size + 1 : 1);
         if (headUpdater.compareAndSet(this, oldNode, newNode)) {
            break;
         }
      }
   }

   @Override
   public T peek() {
      Node<T> node = head;
      return node != null ? node.value : null;
   }

   @Override
   public T element() {
      Node<T> node = head;
      if (node != null) {
         return node.value;
      }
      throw new NoSuchElementException();
   }

   @Override
   public T pop() {
      Node<T> node;
      while (true) {
         node = head;
         if (node == null) {
            throw new NoSuchElementException();
         }
         if (headUpdater.compareAndSet(this, node, node.next)) {
            return node.value;
         }
      }
   }

   @Override
   public T poll() {
      Node<T> node;
      while (true) {
         node = head;
         if (node == null) {
            return null;
         }
         if (headUpdater.compareAndSet(this, node, node.next)) {
            return node.value;
         }
      }
   }

   @Override
   public void drainTo(Collection<? super T> coll) {
      @SuppressWarnings("unchecked")
      Node<T> node = headUpdater.getAndSet(this, null);
      while (node != null) {
         coll.add(node.value);
         node = node.next;
      }
   }

   @Override
   public SimpleTreiberStack<T> removeAll() {
      @SuppressWarnings("unchecked")
      Node<T> node = headUpdater.getAndSet(this, null);
      return new SimpleTreiberStack<>(node);
   }

   @Override
   public void clear() {
      head = null;
   }
}
