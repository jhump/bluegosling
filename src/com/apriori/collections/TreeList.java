package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * A list structure that is stored as a binary tree. Each node in the tree tracks the number of
 * child nodes beneath it so that random access can be done in O(log n) time. This implementation
 * uses a red-black tree to keep the tree roughly height-balanced.
 * 
 * <p>The main advantage this list has over other lists is that it supports removal of an element by
 * list index in logarithmic time whereas others require linear time. For example {@link
 * java.util.LinkedList} requires a linear seek followed by constant time removal and {@link
 * java.util.ArrayList} requires a constant time random access followed by a linear compaction that
 * shifts subsequent elements back to fill the blank left by the deleted element.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TreeList<E> extends AbstractList<E> implements RandomAccess, Serializable, Cloneable {
   
   private static final long serialVersionUID = 7578331319603545754L;

   /**
    * A node in the tree. This is how all items are stored. The trick to list-index access is the
    * size field, which indicates the size of a tree rooted at a node (which is thus equal to the
    * size of the right child tree plus the size of the left child tree plus one).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <E> the type of element stored in the node
    */
   private static class Node<E> {
      E value;
      int subTreeSize;
      Node<E> left, right, parent;
      boolean red;
      
      Node(E value) {
         this.value = value;
         red = true;
         subTreeSize = 1;
      }
      
      void addItem(Node<E> newNode, int index) {
         // Precondition: index has been range-checked
         int leftSize = left == null ? 0 : left.subTreeSize;
         if (index <= leftSize) {
            if (left == null) {
               left = newNode;
               newNode.parent = this;
            } else {
               left.addItem(newNode, index); 
            }
         } else {
            if (right == null) {
               right = newNode;
               newNode.parent = this;
            } else {
               right.addItem(newNode, index - leftSize - 1);
            }
         }
         subTreeSize++;
      }
      
      Node<E> getItem(int index) {
         // Precondition: index has been range-checked
         if (subTreeSize == 1) {
            return this;
         }
         int leftSize = left == null ? 0 : left.subTreeSize;
         if (index < leftSize) {
            return left.getItem(index); 
         } else if (index == leftSize) {
            return this;
         } else {
            return right.getItem(index - leftSize - 1);
         }
      }
      
      Node<E> deepCopy(Node<E> newParent) {
         Node<E> copy = new Node<E>(value);
         copy.red = red;
         copy.parent = newParent;
         copy.left = left.deepCopy(copy);
         copy.right = right.deepCopy(copy);
         copy.subTreeSize = subTreeSize;
         return copy;
      }
   }
   
   /**
    * Implementation of {@link Iterator} for this list.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class IteratorImpl implements ListIterator<E> {
      int myModCount;
      int nextIndex;
      private Node<E> nextNode;
      private Node<E> previousNode;
      private Node<E> lastFetched;
      private IteratorModifiedState modState = IteratorModifiedState.NONE;
      
      IteratorImpl(int index) {
         if (index == 0) {
            nextIndex = 0;
            nextNode = root == null ? null : root.getItem(0);
            previousNode = null;
         } else if (index == size) {
            nextIndex = size;
            nextNode = null;
            previousNode = root.getItem(size - 1);
         } else {
            nextIndex = index;
            nextNode = root.getItem(index);
            previousNode = predecessor(nextNode);            
         }
         myModCount = modCount;
      }
      
      private void checkCanModifyElement(String op) {
         if (lastFetched == null) {
            throw new IllegalStateException("No element to " + op + ": "
                  + "next() or previous() never called");
         }
         if (modState != IteratorModifiedState.NONE) {
            throw new IllegalStateException(
                  "Cannot "
                        + op
                        + " item after call to "
                        + (modState == IteratorModifiedState.REMOVED ? "remove()"
                              : "add()"));
         }
      }

      void contractAfterRemove() {
         // we use internal methods on the tree that modify it w/out incrementing modCount, so
         // increment it here and now
         myModCount = ++modCount;
      }
      
      void expandAfterAdd() {
         // we use internal methods on the tree that modify it w/out incrementing modCount, so
         // increment it here and now
         myModCount = ++modCount;
      }
      
      @Override
      public boolean hasNext() {
         checkMod(myModCount);
         return nextNode != null;
      }

      @Override
      public E next() {
         checkMod(myModCount);
         if (nextNode == null) {
            throw new NoSuchElementException("At end of list");
         }
         lastFetched = previousNode = nextNode;
         nextIndex++;
         nextNode = successor(nextNode);
         modState = IteratorModifiedState.NONE;
         return lastFetched.value;
      }

      @Override
      public boolean hasPrevious() {
         checkMod(myModCount);
         return previousNode != null;
      }

      @Override
      public E previous() {
         checkMod(myModCount);
         if (previousNode == null) {
            throw new NoSuchElementException("At beginning of list");
         }
         lastFetched = nextNode = previousNode;
         nextIndex--;
         previousNode = predecessor(previousNode);
         modState = IteratorModifiedState.NONE;
         return lastFetched.value;
      }

      @Override
      public int nextIndex() {
         checkMod(myModCount);
         return nextIndex;
      }

      @Override
      public int previousIndex() {
         checkMod(myModCount);
         return nextIndex - 1;
      }

      @Override
      public void remove() {
         checkMod(myModCount);
         checkCanModifyElement("remove");
         if (lastFetched == nextNode) {
            nextNode = successor(lastFetched);
         } else {
            previousNode = predecessor(lastFetched);
            nextIndex--;
         }
         removeAndRebalance(lastFetched);
         contractAfterRemove();
         modState = IteratorModifiedState.REMOVED;
      }

      @Override
      public void set(E e) {
         checkMod(myModCount);
         checkCanModifyElement("set");
         lastFetched.value = e;
      }

      @Override
      public void add(E e) {
         checkMod(myModCount);
         Node<E> newNode = addAndRebalance(nextIndex++, e);
         previousNode = newNode;
         expandAfterAdd();
         modState = IteratorModifiedState.ADDED;
      }
   }

   transient Node<E> root;
   transient int size;
   
   public TreeList() {
   }

   public TreeList(Collection<? extends E> coll) {
      addAll(0, coll);
   }

   void checkMod(int aModCount) {
      if (aModCount != modCount) {
         throw new ConcurrentModificationException();
      }
   }

   private void check(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      }
      if (index >= size) {
         throw new IndexOutOfBoundsException("" + index + " >= " + size);
      }
   }
   
   private void checkWide(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      }
      if (index > size) {
         throw new IndexOutOfBoundsException("" + index + " > " + size);
      }
   }
   
   Node<E> predecessor(Node<E> node) {
      if (node.left == null) {
         // find nearest ancestor that is less than (left of) this node
         while (node.parent != null && node != node.parent.right) {
            node = node.parent;
         }
         return node.parent;
      } else {
         // find greatest (right-most) element in left sub-tree
         node = node.left;
         while (node.right != null) {
            node = node.right;
         }
         return node;
      }
   }

   Node<E> successor(Node<E> node) {
      if (node.right == null) {
         // find nearest ancestor that is greater than (right of) this node
         while (node.parent != null && node != node.parent.left) {
            node = node.parent;
         }
         return node.parent;
      } else {
         // find smallest (left-most) element in right sub-tree
         node = node.right;
         while (node.left != null) {
            node = node.left;
         }
         return node;
      }
   }

   Node<E> addAndRebalance(int index, E element) {
      checkWide(index);
      Node<E> newNode = new Node<E>(element);
      if (root == null) {
         root = newNode;
         newNode.red = false;
      } else {
         root.addItem(newNode, index);
      }
      // and rebalance
      insertionRebalance(newNode);
      
      size++;
      return newNode;
   }
   
   void rotateLeft(Node<E> p) {
      Node<E> q = p.right;
      if (p.parent == null) {
         root = q;
      } else if (p.parent.left == p) {
         p.parent.left = q;
      } else {
         p.parent.right = q;
      }
      q.parent = p.parent;
      p.right = q.left;
      if (q.left != null) {
         q.left.parent = p;
      }
      q.left = p;
      p.parent = q;
      // fix sub-tree sizes
      q.subTreeSize = p.subTreeSize;
      p.subTreeSize -= 1 + (q.right == null ? 0 : q.right.subTreeSize);
   }

   void rotateRight(Node<E> q) {
      Node<E> p = q.left;
      if (q.parent == null) {
         root = p;
      } else if (q.parent.left == q) {
         q.parent.left = p;
      } else {
         q.parent.right = p;
      }
      p.parent = q.parent;
      q.left = p.right;
      if (p.right != null) {
         p.right.parent = q;
      }
      p.right = q;
      q.parent = p;
      // fix sub-tree sizes
      p.subTreeSize = q.subTreeSize;
      q.subTreeSize -= 1 + (p.left == null ? 0 : p.left.subTreeSize);
   }

   void insertionRebalance(Node<E> node) {
      while (node != null && node.parent != null && node.parent.red) {
         Node<E> g = node.parent.parent;
         if (g != null && node.parent == g.left) {
            Node<E> uncle = g.right;
            if (uncle != null && uncle.red) {
               node.parent.red = uncle.red = false;
               g.red = true;
               node = g;
            } else {
               if (node == node.parent.right) {
                  node = node.parent;
                  rotateLeft(node);
               }
               if (node.parent != null) {
                  node.parent.red = false;
                  if (node.parent.parent != null) {
                     node.parent.parent.red = true;
                     rotateRight(node.parent.parent);
                  }
               }
            }
         } else {
            Node<E> uncle = g == null ? null : g.left;
            if (uncle != null && uncle.red) {
               node.parent.red = uncle.red = false;
               g.red = true;
               node = g;
            } else {
               if (node == node.parent.left) {
                  node = node.parent;
                  rotateRight(node);
               }
               if (node.parent != null) {
                  node.parent.red = false;
                  if (node.parent.parent != null) {
                     node.parent.parent.red = true;
                     rotateLeft(node.parent.parent);
                  }
               }
            }            
         }
      }
      root.red = false;
   }
   
   void removeAndRebalance(Node<E> node) {
      size--;
      if (node.left != null && node.right != null) {
         // both sides present? replace value with that of predecessor
         Node<E> predecessor = node.left;
         while (predecessor.right != null) {
            predecessor = predecessor.right;
         }
         node.value = predecessor.value;
         // then proceed with removal of now obselete predecessor
         node = predecessor;
      }
      // clean up sub-tree sizes
      for (Node<E> ancestor = node.parent; ancestor != null; ancestor = ancestor.parent) {
         ancestor.subTreeSize--;
      }
      Node<E> child = node.left == null ? node.right : node.left;
      if (child != null) {
         // there is a child that will take the place of removed node
         child.parent = node.parent;
         if (node.parent == null) {
            root = child;
         } else if (node == node.parent.left) {
            node.parent.left = child;
         } else {
            node.parent.right = child;
         }
         deletionRebalance(child);
      } else {
         // no children? adjust balance of tree before unlinking
         deletionRebalance(node);
         if (node.parent == null) {
            root = child;
         } else if (node == node.parent.left) {
            node.parent.left = child;
         } else {
            node.parent.right = child;
         }
      }
   }
   
   void deletionRebalance(Node<E> node) {
      if (node.red) {
         return;
      }
      while (node.parent != null && !node.red) {
         if (node == node.parent.left) {
            Node<E> sibling = node.parent.right;
            if (sibling != null && sibling.red) {
               sibling.red = false;
               node.parent.red = true;
               rotateLeft(node.parent);
               sibling = node.parent == null ? null : node.parent.right;
            }
            if (sibling == null) {
               node = node.parent;
            } else if ((sibling.left == null || !sibling.left.red)
                  && (sibling.right == null || !sibling.right.red)) {
               sibling.red = true;
               node = node.parent;
            } else {
               if (sibling.right == null || !sibling.right.red) {
                  if (sibling.left != null) {
                     sibling.left.red = false;
                  }
                  sibling.red = true;
                  rotateRight(sibling);
                  sibling = node.parent == null ? null : node.parent.right;
               }
               if (sibling != null) {
                  sibling.red = node.parent == null ? false : node.parent.red;
                  if (sibling.right != null) {
                     sibling.right.red = false;
                  }
               }
               if (node.parent != null) {
                  node.parent.red = false;
                  rotateLeft(node.parent);
               }
               break;
            }
         } else {
            Node<E> sibling = node.parent.left;
            if (sibling != null && sibling.red) {
               sibling.red = false;
               node.parent.red = true;
               rotateRight(node.parent);
               sibling = node.parent == null ? null : node.parent.left;
            }
            if (sibling == null) {
               node = node.parent;
            } else if ((sibling.left == null || !sibling.left.red)
                  && (sibling.right == null || !sibling.right.red)) {
               sibling.red = true;
               node = node.parent;
            } else {
               if (sibling.left == null || !sibling.left.red) {
                  if (sibling.right != null) {
                     sibling.right.red = false;
                  }
                  sibling.red = true;
                  rotateLeft(sibling);
                  sibling = node.parent == null ? null : node.parent.left;
               }
               if (sibling != null) {
                  sibling.red = node.parent == null ? false : node.parent.red;
                  if (sibling.left != null) {
                     sibling.left.red = false;
                  }
               }
               if (node.parent != null) {
                  node.parent.red = false;
                  rotateRight(node.parent);
               }
               break;
            }
         }
      }
      node.red = false;
   }
   
   @Override
   public int size() {
      return size;
   }

   @Override
   public Iterator<E> iterator() {
      return new IteratorImpl(0);
   }

   @Override
   public boolean add(E e) {
      addAndRebalance(size, e);
      modCount++;
      return true;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      return addAll(size, c);
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      if (c.isEmpty()) {
         return false;
      }
      if (size == 0) {
         checkWide(index);
         // we can bulk add in a way that will result in a balanced tree w/out having to do
         // any rebalancing/rotation
         Object items[] = c.toArray();
         root = balancedTreeFromArray(items, 0, items.length, false);
         size = items.length;
      } else {
         for (E e : c) {
            addAndRebalance(index++, e);
         }
      }
      modCount++;
      return true;
   }
   
   private Node<E> balancedTreeFromArray(Object items[], int start, int len, boolean red) {
      int mid = start + (len >> 1);
      @SuppressWarnings("unchecked") // caller always passes array with E instances
      E value = (E) (items == null ? null : items[mid]);
      Node<E> node = new Node<E>(value);
      node.red = red;
      node.subTreeSize = len;
      int left = mid - start;
      if (left > 0) {
         node.left = balancedTreeFromArray(items, start, left, !red);
         node.left.parent = node;
      }
      int right = len - left - 1;
      if (right > 0) {
         node.right = balancedTreeFromArray(items, mid + 1, right, !red);
         node.right.parent = node;
      }
      return node;
   }

   @Override
   public void clear() {
      size = 0;
      root = null;
      modCount++;
   }

   @Override
   public E get(int index) {
      check(index);
      return root.getItem(index).value;
   }

   @Override
   public E set(int index, E element) {
      check(index);
      Node<E> node = root.getItem(index);
      E ret = node.value;
      node.value = element;
      return ret;
   }

   @Override
   public void add(int index, E element) {
      addAndRebalance(index, element);
      modCount++;
   }

   @Override
   public E remove(int index) {
      check(index);
      Node<E> node = root.getItem(index);
      E ret = node.value; // save it here since removal could overwrite it
      removeAndRebalance(node);
      modCount++;
      return ret;
   }

   @Override
   public ListIterator<E> listIterator() {
      return new IteratorImpl(0);
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      checkWide(index);
      return new IteratorImpl(index);
   }
   
   @Override
   public TreeList<E> clone() {
      if (this.getClass() == TreeList.class) {
         TreeList<E> copy = new TreeList<E>();
         copy.size = size;
         copy.root = root.deepCopy(null);
         return copy;
      }
      try {
         @SuppressWarnings("unchecked")
         TreeList<E> copy = (TreeList<E>) super.clone();
         // deep copy the tree
         copy.root = root.deepCopy(null);
         // now sub-class can do whatever else with this...
         return copy;
      }
      catch (CloneNotSupportedException e) {
         // should never happen since we implement Cloneable -- but just in
         // case, wrap in a runtime exception that sort of makes sense...
         throw new ClassCastException(Cloneable.class.getName());
      }
   }

   /**
    * Customizes de-serialization to read list of elements same way as written by
    * {@link #writeObject(ObjectOutputStream)}.
    * 
    * @param in the stream from which the list is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      size = in.readInt();
      // fill with null nodes
      if (size > 0) {
         // TODO: can do this all in one pass instead of creating structure in one pass and filling
         // with a second
         root = balancedTreeFromArray(null, 0, size, false);
         // and then set the values by reading them in from stream
         ListIterator<E> iter = listIterator();
         for (int index = 0; index < size; index++) {
            iter.next();
            // this cast should be safe while a user's cast on reading the collection from the
            // stream (casting from Object to List<E>) is the unsafe one
            @SuppressWarnings("unchecked")
            E item = (E) in.readObject();
            iter.set(item);
         }
      } else {
         root = null;
      }
   }
   
   /**
    * Customizes serialization by just writing the list contents in order.
    * 
    * @param out the stream to which to serialize this list
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeInt(size);
      for (E e : this) {
         out.writeObject(e);
      }
   }
}