package com.apriori.collections;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An ordered queue backed by a <a href="https://en.wikipedia.org/wiki/Skew_heap">Skew Heap</a>.
 * This structure is a simple binary tree (a self-adjusting variant of a Leftist Heap) that allows
 * efficient and simple merging of two queues.
 *
 * @param <E> the type of element in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class SkewHeapOrderedQueue<E> extends AbstractQueue<E>
      implements MeldableOrderedQueue<E, SkewHeapOrderedQueue<? extends E>>,
            Serializable, Cloneable {

   private static final long serialVersionUID = 8878384608812601417L;

   /**
    * A basic binary tree node.
    *
    * @param <E> the type of value held in the node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Node<E> {
      Node<E> left;
      Node<E> right;
      E value;
      
      /**
       * Creates a new leaf node with the given value.
       *
       * @param value the node's value
       */
      Node(E value) {
         this.value = value;
      }
      
      /**
       * Performs a deep copy of the tree with the given root.
       *
       * @param other the root of the tree to copy
       */
      Node(Node<E> other) {
         this.value = other.value;
         this.left = other.left == null ? null : new Node<>(other.left);
         this.right = other.right == null ? null : new Node<>(other.right);
      }
   }
   
   final Comparator<? super E> comp;
   int size;
   Node<E> root;
   int modCount;

   /**
    * Constructs a new, empty queue that uses elements' {@linkplain Comparable natural order} to
    * determine order.
    */
   public SkewHeapOrderedQueue() {
      this(CollectionUtils.naturalOrder());
   }

   /**
    * Constructs a new, empty queue that uses the given comparator to determine order.
    *
    * @param comp a comparator
    */
   public SkewHeapOrderedQueue(Comparator<? super E> comp) {
      this.comp = requireNonNull(comp);
   }

   /**
    * Constructs a new queue that uses elements' {@linkplain Comparable natural order} to determine
    * order and contains the given elements.
    * 
    * @param coll the initial contents of the new queue
    */
   public SkewHeapOrderedQueue(Collection<? extends E> coll) {
      this(coll, CollectionUtils.naturalOrder());
   }
   
   /**
    * Constructs a new queue that uses the given comparator to determine order and contains the
    * given elements.
    * 
    * @param coll the initial contents of the new queue
    * @param comp a comparator
    */
   public SkewHeapOrderedQueue(Collection<? extends E> coll, Comparator<? super E> comp) {
      this(comp);
      addAll(coll);
   }

   /**
    * Creates a new queue that duplicates the given queue. The new queue will have the same contents
    * as the given queue and also use the same comparator.
    *
    * @param other a queue to be duplicated
    */
   public SkewHeapOrderedQueue(OrderedQueue<E> other) {
      this(other, getComparator(other));
   }
   
   private static <E> Comparator<? super E> getComparator(OrderedQueue<E> queue) {
      Comparator<? super E> comp = queue.comparator();
      return comp == null ? CollectionUtils.naturalOrder() : comp;
   }

   @Override
   public SkewHeapOrderedQueue<E> clone() {
      try {
         @SuppressWarnings("unchecked")
         SkewHeapOrderedQueue<E> clone = (SkewHeapOrderedQueue<E>) super.clone();
         // deep copy the tree nodes
         clone.root = new Node<>(root);
         return clone;
      } catch (CloneNotSupportedException e) {
         throw new AssertionError(e); // implements Cloneable, so this shouldn't ever happen
      }
   }
   
   @Override
   public Comparator<? super E> comparator() {
      return comp == CollectionUtils.naturalOrder() ? null : comp;
   }

   @Override
   public boolean offer(E e) {
      root = merge(root, new Node<E>(e));
      size++;
      modCount++;
      return true;
   }
   
   /**
    * Removes the root node from the binary tree that backs this heap. The root node always contains
    * the smallest value.
    *
    * @return the removed node or {@code null} if the queue is empty
    */
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
      other.clear(); // we're taking all elements
      if (root == null) {
         assert size == 0;
         root = otherRoot;
         size = other.size;
      } else {
         size += other.size;
         root = merge(root, otherRoot);
      }
      modCount++;
      return true;
   }

   /**
    * Merges two trees with the given roots. This modifies the given trees to combine them. This
    * operation can trivially be done recursively, but this version uses iteration to ensure that
    * a very large tree won't cause a stack overflow.
    *
    * @param p the root of a skew-heap tree
    * @param q the root of another skew-heap tree
    * @return the root of the merged tree
    */
   private Node<E> merge(Node<E> p, Node<E> q) {
      int count = 0;
      // measure lengths of right-most branches in the trees to be merged
      for (Node<E> n = p; n != null; count++, n = n.right);
      for (Node<E> n = q; n != null; count++, n = n.right);
      
      // now split tree by cutting each right-most path
      @SuppressWarnings("unchecked")
      Node<E> roots[] = new Node[count];
      int i = 0;
      while (p != null) {
         roots[i++] = p;
         Node<E> tmp = p.right;
         p.right = null;
         p = tmp;
      }
      while (q != null) {
         roots[i++] = q;
         Node<E> tmp = q.right;
         q.right = null;
         p = tmp;
      }
      
      // now we sort the trees and merge them, right-to-left
      Arrays.sort(roots, Comparator.comparing(n -> n.value, comp));
      while (--i > 0) {
         Node<E> n1 = roots[i];
         Node<E> n2 = roots[i - 1];
         assert n1.right == null;
         assert n2.right == null;
         if (n2.left != null) {
            n2.right = n2.left;
         }
         n2.left = n1;
      }
      return roots[0];
   }

   @Override
   public Iterator<E> iterator() {
      return new Iter();
   }

   @Override
   public int size() {
      return size;
   }
   
   /**
    * A persistent stack that is implemented via a linked list. 
    *
    * @param <T> the type of element in the stack
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Stack<T> {
      private final T element;
      private final Stack<T> next;
      
      Stack(T element) {
         this(element, null);
      }
      
      Stack(T element, Stack<T> next) {
         this.element = element;
         this.next = next;
      }
      
      Stack<T> push(T newElement) {
         return new Stack<>(newElement, this);
      }
      
      T peek() {
         return element;
      }
      
      Stack<T> pop() {
         return next;
      }
   }
   
   private class Iter implements Iterator<E> {
      private Stack<Node<E>> stack;
      private Stack<Node<E>> lastFetched;
      private int myModCount;
      
      Iter() {
         if (root != null) {
            stack = new Stack<>(root);
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
         return stack != null;
      }

      @Override
      public E next() {
         checkModCount();
         if (stack == null) {
            throw new NoSuchElementException();
         }
         Node<E> current = stack.peek();
         lastFetched = stack;
         E ret = current.value;
         if (current.left != null) {
            stack = stack.push(current.left);
         } else if (current.right != null) {
            stack = stack.push(current.right);
         } else {
            while (true) {
               Node<E> child = current;
               stack = stack.pop();
               if (stack == null) {
                  break;
               }
               current = stack.peek();
               if (child != current.right && current.right != null) {
                  stack = stack.push(current.right);
                  break;
               }
            }
         }
         return ret;
      }

      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         checkModCount();
         Node<E> n = lastFetched.peek();
         Stack<Node<E>> parentStack = lastFetched.pop();
         Node<E> parent = parentStack == null ? null : parentStack.peek();
         // remove n by replacing it, sifting up values from child node
         while (n != null) {
            if (n.left == null) {
               if (n.right == null) {
                  // node has no children, so we completely remove it
                  if (parent != null) {
                     if (parent.left == n) {
                        parent.left = null;
                     } else {
                        assert parent.right == n;
                        parent.right = n;
                     }
                  } else {
                     assert root == n;
                     assert size == 1;
                     root = null;
                  }
               } else {
                  // node has only a right child; sift it up
                  n.value = n.right.value;
                  parent = n;
                  n = n.right;
               }
            } else if (n.right != null) {
               // node has both children; sift up the smaller value
               if (comp.compare(n.left.value, n.right.value) < 0) {
                  // left is smaller
                  n.value = n.left.value;
                  parent = n;
                  n = n.left;
               } else {
                  // right is smaller
                  n.value = n.right.value;
                  parent = n;
                  n = n.right;
               }
            } else {
               // node has only a left child; sift it up
               n.value = n.left.value;
               parent = n;
               n = n.left;
            }
         }
         lastFetched = null;
         size--;
         myModCount = ++modCount;
      }
   }
}
