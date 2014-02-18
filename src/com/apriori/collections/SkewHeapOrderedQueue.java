package com.apriori.collections;

import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

// TODO: implement me (don't forget serialization and cloning)
// TODO: javadoc
// TODO: tests
public class SkewHeapOrderedQueue<E> extends AbstractCollection<E>
      implements MeldableOrderedQueue<E, SkewHeapOrderedQueue<? extends E>> {

   private static class Node<E> {
      Node<E> left;
      Node<E> right;
      final E value;
      
      Node(E value) {
         this.value = value;
      }
   }
   
   private Comparator<? super E> comp;
   private int size;
   Node<E> root;
   int modCount;
   
   public SkewHeapOrderedQueue() {
   }

   public SkewHeapOrderedQueue(Comparator<? super E> comp) {
      this.comp = comp;
   }

   public SkewHeapOrderedQueue(Collection<? extends E> coll) {
      addAll(coll);
   }
   
   public SkewHeapOrderedQueue(Collection<? extends E> coll, Comparator<? super E> comp) {
      this.comp = comp;
      addAll(coll);
   }

   public SkewHeapOrderedQueue(OrderedQueue<E> other) {
      this(other, other.comparator());
   }

   @Override
   public Comparator<? super E> comparator() {
      return comp;
   }

   @Override
   public boolean add(E e) {
      if (offer(e)) {
         return true;
      }
      throw new IllegalStateException();
   }

   @Override
   public boolean offer(E e) {
      Node<E> newNode = new Node<E>(e);
      root = merge(root, newNode);
      size++;
      modCount++;
      return true;
   }
   
   private Node<E> removeMin() {
      if (root == null) {
         return null;
      }
      Node<E> min = root;
      root = merge(root.left, root.right);
      size--;
      modCount++;
      return min;
   }

   @Override
   public E remove() {
      Node<E> min = removeMin();
      if (min == null) {
         throw new NoSuchElementException();
      }
      return min.value;
   }

   @Override
   public E poll() {
      Node<E> min = removeMin();
      return min == null ? null : min.value;
   }

   @Override
   public E element() {
      if (root == null) {
         throw new NoSuchElementException();
      }
      return root.value;
   }

   @Override
   public E peek() {
      return root == null ? null : root.value;
   }
   
   @Override
   public void clear() {
      root = null;
      size = 0;
      modCount++;
   }

   @Override
   public boolean mergeFrom(SkewHeapOrderedQueue<? extends E> other) {
      if (other.root == null) {
         return false; // other queue is empty, nothing to do
      }
      
      @SuppressWarnings("unchecked") // we're taking ownership of roots
      Node<E> otherRoot = (Node<E>) other.root;
      size += other.size;
      other.clear(); // we're taking all elements
      modCount++;
      root = merge(root, otherRoot);
      return true;
   }
   
   private Node<E> merge(Node<E> p, Node<E> q) {
      if (p == null) {
         return q;
      } else if (q == null) {
         return p;
      }
      if (comp.compare(p.value, q.value) > 0) {
         Node<E> tmp = p;
         p = q;
         q = tmp;
      }
      Node<E> tmp = p.right;
      p.right = p.left;
      p.left = merge(q, tmp);
      return p;
   }

   @Override
   public Iterator<E> iterator() {
      return new Iter();
   }

   @Override
   public int size() {
      return size;
   }
   
   private class Iter implements Iterator<E> {
      private final ArrayDeque<Node<E>> stack = new ArrayDeque<Node<E>>();
      private Node<E> lastFetched;
      private int myModCount;
      
      Iter() {
         if (root != null) {
            stack.push(root);
         }
         myModCount = modCount;
      }
      
      private void checkModCount() {
         if (myModCount != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         return !stack.isEmpty();
      }

      @Override
      public E next() {
         checkModCount();
         if (stack.isEmpty()) {
            throw new NoSuchElementException();
         }
         Node<E> current = stack.peek();
         lastFetched = current;
         E ret = current.value;
         if (current.left != null) {
            stack.push(current.left);
         } else if (current.right != null) {
            stack.push(current.right);
         } else {
            while (true) {
               Node<E> child = stack.pop();
               if (stack.isEmpty()) {
                  break;
               }
               current = stack.peek();
               if (child != current.right && current.right != null) {
                  stack.push(current.right);
                  break;
               }
            }
         }
         return ret;
      }

      @Override
      public void remove() {
         checkModCount();
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         // TODO: implement me
         lastFetched = null;
         myModCount = ++modCount;
      }
      
   }
}
