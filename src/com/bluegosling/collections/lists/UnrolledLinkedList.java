package com.bluegosling.collections.lists;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.MoreIterators;
import com.bluegosling.tuples.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A list implementation that is a hybrid between a linked list and an array. To mitigate some of
 * the performance impacts of a linked list (memory overhead, poor cache locality during
 * traversal), this structure uses a linked list of arrays. Each node in the list represents a fixed
 * number of list elements, stored in an array. Essentially, some number of list nodes are
 * "unrolled" into an array.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Unrolled_linked_list">Wikipedia - Unrolled Linked Lists</a>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element stored in the list
 */
//TODO: javadoc
//TODO: more efficient implementation of addAll(int,Collection)
public class UnrolledLinkedList<E> extends AbstractSequentialList<E> implements Deque<E>, Cloneable,
      Serializable {

   private static final long serialVersionUID = 2966626995282831346L;

   /**
    * A node in the list.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <E> the type of element stored in the node
    */
   // TODO: extend CircularBuffer so moving elements around is both simpler and faster
   private static class Node<E> {
      final E elements[];
      int count;
      Node<E> next;
      Node<E> previous;
      
      @SuppressWarnings("unchecked")
      Node(int unrollCount) {
         elements = (E[]) new Object[unrollCount];
      }
   }
   
   /**
    * The default number of elements to store in each linked list node.
    */
   private static final int DEFAULT_UNROLL_COUNT = 16;
   
   int unrollCount;
   int size;
   private transient Node<E> head;
   private transient Node<E> tail;
   
   /**
    * Constructs a new, empty unrolled linked list with a default unroll count of 16.
    */
   public UnrolledLinkedList() {
      this(DEFAULT_UNROLL_COUNT);
   }
   
   /**
    * Constructs a new unrolled linked list with a default unroll count of 16, populated with the
    * elements of the specified collection.
    * 
    * @param coll the collection of elements which will be present in the newly constructed list
    */
   public UnrolledLinkedList(Collection<? extends E> coll) {
      this(DEFAULT_UNROLL_COUNT, coll);
   }
   
   /**
    * Constructs a new, empty unrolled linked list with the specified unroll count.
    * 
    * @param unrollCount the number of elements "unrolled" into each linked list node
    */
   public UnrolledLinkedList(int unrollCount) {
      if (unrollCount < 2) {
         throw new IllegalArgumentException();
      }
      this.unrollCount = unrollCount;
   }

   /**
    * Constructs a new unrolled linked list with the specified unroll count, populated with the
    * elements of the specified collection.
    * 
    * @param unrollCount the number of elements "unrolled" into each linked list node
    * @param coll the collection of elements which will be present in the newly constructed list
    */
   public UnrolledLinkedList(int unrollCount, Collection<? extends E> coll) {
      this(unrollCount);
      addAll(coll);
   }

   /**
    * Finds the node and index where the specified list index exists in the list.
    * 
    * @param index the list index to find
    * @return the pair of list node and node offset where the object at the specified index is found
    */
   private Pair<Node<E>, Integer> find(int index) {
      Node<E> node;
      int offset;
      int pos;
      if (index >= size >> 1) {
         // search from the end
         node = tail;
         pos = size - node.count;
         while (true) {
            if (index >= pos) {
               offset = index - pos;
               break;
            }
            node = node.previous;
            pos -= node.count;
         }
      } else {
         node = head;
         pos = 0;
         while (true) {
            if (index < pos + node.count) {
               offset = index - pos;
               break;
            }
            pos += node.count;
            node = node.next;
         }
      }
      return Pair.create(node, offset);
   }
   
   E remove(Node<E> node, int offset) {
      E ret = node.elements[offset];
      if (node.count == 1 && node.next == null) {
         // just drop the node
         assert node == tail;
         if (head == tail) {
            // remove last item in the list
            head = tail = null;
         } else {
            tail = node.previous;
            tail.next = null;
         }
      } else {
         if (offset < node.count - 1) {
            // shift elements back one position
            System.arraycopy(node.elements, offset + 1, node.elements, offset,
                  node.count - offset - 1);
         }
         if (node.count > (unrollCount + 1) >> 1 || node.next == null) {
            // just clear trailing ref
            node.elements[--node.count] = null;
         } else if (node.count + node.next.count - 1 <= unrollCount) {
            // merge nodes into one
            System.arraycopy(node.next.elements, 0, node.elements, node.count - 1,
                  node.next.count);
            node.count += node.next.count - 1;
            // node that was merged can now be removed
            if (node.next == tail) {
               tail = node;
               node.next = null;
            } else {
               node.next = node.next.next;
               node.next.previous = node;
            }
         } else {
            // shuffle elements so neither node has fewer than unrollCount/2
            int p = (node.count + node.next.count - 1) >> 1;
            int numSpill = p - node.count + 1;
            System.arraycopy(node.next.elements, 0, node.elements, node.count - 1, numSpill);
            System.arraycopy(node.next.elements, numSpill, node.next.elements, 0,
                  node.next.count - numSpill); // TODO: clear trailing refs in node.next
            node.count = p;
            node.next.count -= numSpill;
         }
      }
      size--;
      modCount++;
      return ret;
   }

   void add(Node<E> node, int offset, E element) {
      if (node.count != unrollCount) {
         if (offset < node.count) {
            System.arraycopy(node.elements, offset, node.elements, offset + 1, node.count - offset);
         }
         node.elements[offset] = element;
         node.count++;
      } else if (node == tail) {
         // append a new node
         tail.next = new Node<E>(unrollCount);
         tail.next.previous = tail;
         tail = tail.next;
         if (offset < node.count) {
            tail.elements[0] = node.elements[node.count - 1];
            if (offset < node.count - 1) {
               System.arraycopy(node.elements, offset, node.elements, offset + 1, node.count - offset - 1);
            }
            node.elements[offset] = element;
         } else {
            tail.elements[0] = element;
         }
         tail.count = 1;
      } else {
         // have to split elements into two nodes
         Node<E> next;
         int p;
         int numSpill;
         // first see if we can just spill into existing next node
         if (node.next != null && node.next.count < unrollCount) {
            next = node.next;
            p = (next.count + unrollCount + 1) >> 1;
            // shift existing elements to make room for newly spilled ones
            numSpill = unrollCount + 1 - p;
            System.arraycopy(next.elements, 0, next.elements, numSpill, next.count);
            next.count += numSpill;
         } else {
            // nope, won't fit. so we have to create a new node
            next = new Node<E>(unrollCount);
            if (node == tail) {
               tail = next;
            } else {
               next.next = node.next;
               next.next.previous = next;
            }
            node.next = next;
            next.previous = node;
            p = (unrollCount + 1) >> 1;
            numSpill = next.count = unrollCount + 1 - p;
         }
         node.count = p;
         if (offset >= p) {
            // new item is in the new next node
            offset -= p;
            if (offset > 0) {
               System.arraycopy(node.elements, p, next.elements, 0, offset);
            }
            next.elements[offset] = element;
            if (offset < numSpill - 1) {
               System.arraycopy(node.elements, p + offset, next.elements, offset + 1,
                     numSpill - 1 - offset);
            }
         } else {
            // new item is in current node
            System.arraycopy(node.elements, unrollCount - numSpill, next.elements, 0, numSpill);
            if (offset < node.count - 1) {
               System.arraycopy(node.elements, offset, node.elements, offset + 1,
                     node.count - 1 - offset);
            }
            node.elements[offset] = element;
         }
         // clear out no-longer-needed trailing references
         Arrays.fill(node.elements, p, unrollCount, null);
      }
      size++;
      modCount++;
   }

   @Override
   public boolean add(E e) {
      addLast(e);
      return true;
   }
   
   private void addInitial(E e) {
      head = tail = new Node<E>(unrollCount);
      head.elements[0] = e;
      head.count = 1;
      size = 1;
      modCount++;
   }
   
   @Override
   public void add(int index, E e) {
      if (index < 0 || index > size) {
         throw new IndexOutOfBoundsException("" + index);
      }
      if (isEmpty()) {
         addInitial(e);
      } else {
         Pair<Node<E>, Integer> loc = find(index);
         add(loc.getFirst(), loc.getSecond(), e);
      }
   }
   
   @Override
   public E set(int index, E e) {
      if (index < 0 || index >= size) {
         throw new IndexOutOfBoundsException("" + index);
      }
      Pair<Node<E>, Integer> loc = find(index);
      Node<E> node = loc.getFirst();
      int offset = loc.getSecond();
      E ret = node.elements[offset];
      node.elements[offset] = e;
      return ret;
   }
   
   @Override
   public E get(int index) {
      if (index < 0 || index >= size) {
         throw new IndexOutOfBoundsException("" + index);
      }
      Pair<Node<E>, Integer> loc = find(index);
      return loc.getFirst().elements[loc.getSecond()];
   }
   
   @Override
   public E remove(int index) {
      if (index < 0 || index >= size) {
         throw new IndexOutOfBoundsException("" + index);
      }
      Pair<Node<E>, Integer> loc = find(index);
      return remove(loc.getFirst(), loc.getSecond());
   }
   
   @Override
   public void clear() {
      head = tail = null;
      size = 0;
      modCount++;
   }
   
   @Override
   public void addFirst(E e) {
      if (isEmpty()) {
         addInitial(e);
      } else {
         add(head, 0, e);
      }
   }

   @Override
   public void addLast(E e) {
      if (isEmpty()) {
         addInitial(e);
      } else {
         add(tail, tail.count, e);
      }
   }

   @Override
   public boolean offerFirst(E e) {
      addFirst(e);
      return true;
   }

   @Override
   public boolean offerLast(E e) {
      addLast(e);
      return true;
   }

   @Override
   public E removeFirst() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return remove(head, 0);
   }

   @Override
   public E removeLast() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return remove(tail, tail.count - 1);
   }

   @Override
   public E pollFirst() {
      return isEmpty() ? null : removeFirst();
   }

   @Override
   public E pollLast() {
      return isEmpty() ? null : removeLast();
   }

   @Override
   public E getFirst() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return head.elements[0];
   }

   @Override
   public E getLast() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return tail.elements[tail.count - 1];
   }

   @Override
   public E peekFirst() {
      return isEmpty() ? null : head.elements[0];
   }

   @Override
   public E peekLast() {
      return isEmpty() ? null : tail.elements[tail.count - 1];
   }

   @Override
   public boolean removeFirstOccurrence(Object o) {
      return remove(o);
   }

   @Override
   public boolean removeLastOccurrence(Object o) {
      return CollectionUtils.removeObject(o, descendingIterator(), true);
   }

   @Override
   public boolean offer(E e) {
      return offerLast(e);
   }

   @Override
   public E remove() {
      return removeFirst();
   }

   @Override
   public E poll() {
      return pollFirst();
   }

   @Override
   public E element() {
      return getFirst();
   }

   @Override
   public E peek() {
      return peekFirst();
   }

   @Override
   public void push(E e) {
      addFirst(e);
   }

   @Override
   public E pop() {
      return removeFirst();
   }

   @Override
   public Iterator<E> descendingIterator() {
      return MoreIterators.reverseListIterator(listIterator(size()));
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      if (index < 0 || index > size) {
         throw new IndexOutOfBoundsException("" + index);
      }
      if (index == 0) {
         return new IteratorImpl(head, 0, 0);
      } else if (index == size) {
         return new IteratorImpl(tail, tail.count, index);
      }
      Pair<Node<E>, Integer> loc = find(index);
      return new IteratorImpl(loc.getFirst(), loc.getSecond(), index);
   }

   @Override
   public int size() {
      return size;
   }
   
   private class IteratorImpl implements ListIterator<E> {
      private int myModCount;
      private Node<E> node;
      private int offset;
      private int nextIndex;
      private int lastFetched;
      
      IteratorImpl(Node<E> node, int offset, int nextIndex) {
         this.node = node;
         this.offset = offset;
         this.nextIndex = nextIndex;
         resetModCount();
      }
      
      @SuppressWarnings("synthetic-access")
      private void resetModCount() {
         myModCount = modCount;
      }
      
      @SuppressWarnings("synthetic-access")
      private void checkModCount() {
         if (myModCount != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         return nextIndex < size;
      }

      @Override
      public E next() {
         checkModCount();
         if (nextIndex >= size) {
            throw new NoSuchElementException();
         }
         E ret = node.elements[offset];
         offset++;
         if (offset == node.count && node.next != null) {
            offset = 0;
            node = node.next;
         }
         nextIndex++;
         lastFetched = -1;
         return ret;
      }

      @Override
      public boolean hasPrevious() {
         checkModCount();
         return nextIndex > 0;
      }

      @Override
      public E previous() {
         checkModCount();
         if (nextIndex == 0) {
            throw new NoSuchElementException();
         }
         if (offset == 0) {
            node = node.previous;
            offset = node.count - 1;
         } else {
            offset--;
         }
         nextIndex--;
         lastFetched = 1;
         return node.elements[offset];
      }

      @Override
      public int nextIndex() {
         checkModCount();
         return nextIndex;
      }

      @Override
      public int previousIndex() {
         checkModCount();
         return nextIndex - 1;
      }

      @Override
      public void remove() {
         checkModCount();
         if (lastFetched == 0) {
            throw new IllegalStateException();
         } 
         int offsetOfRemoval;
         Node<E> nodeOfRemoval;
         if (lastFetched == -1) {
            if (offset == 0) {
               nodeOfRemoval = node.previous;
               offsetOfRemoval = nodeOfRemoval.count - 1;
            } else {
               nodeOfRemoval = node;
               offsetOfRemoval = offset - 1;
            }
            nextIndex--;
         } else { // lastFetched == 1
            nodeOfRemoval = node;
            offsetOfRemoval = offset;
         }
         
         Node<E> prevNode;
         int offsetFromPrevNode;
         if (nodeOfRemoval.previous == null) {
            prevNode = nodeOfRemoval;
            offsetFromPrevNode = offsetOfRemoval;
         } else {
            prevNode = nodeOfRemoval.previous;
            offsetFromPrevNode = offsetOfRemoval + prevNode.count;
         }
         UnrolledLinkedList.this.remove(nodeOfRemoval, offsetOfRemoval);
         // re-establish node and offset in case we ended up removing the whole node
         while (prevNode.next != null && offsetFromPrevNode >= prevNode.count) {
            offsetFromPrevNode -= prevNode.count;
            prevNode = prevNode.next;
         }
         node = prevNode;
         offset = offsetFromPrevNode;
         lastFetched = 0;
         resetModCount();
      }

      @Override
      public void set(E e) {
         checkModCount();
         if (lastFetched == 0) {
            throw new IllegalStateException();
         } else if (lastFetched == -1) {
            Node<E> nodeToSet;
            int offsetToSet;
            if (offset == 0) {
               nodeToSet = node.previous;
               offsetToSet = nodeToSet.count - 1;
            } else {
               nodeToSet = node;
               offsetToSet = offset - 1;
            }
            nodeToSet.elements[offsetToSet] = e;
         } else { // lastFetched == 1
            node.elements[offset] = e;
         }
      }

      @SuppressWarnings("synthetic-access") // accesses head, private member of enclosing class
      @Override
      public void add(E e) {
         checkModCount();
         if (nextIndex == size) {
            UnrolledLinkedList.this.addLast(e);
         } else {
            UnrolledLinkedList.this.add(node, offset, e);
         }
         if (node == null) {
            node = head;
         }
         // offset could be > node.count if we split the node during add operation
         if (++offset >= node.count && node.next != null) {
            offset -= node.count;
            node = node.next;
         }
         nextIndex++;
         lastFetched = 0;
         resetModCount();
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
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      if (size > 0) {
         Node<E> node = null;
         int numLeft = size;
         while (numLeft > 0) {
            Node<E> nextNode = new Node<E>(unrollCount);
            if (node == null) {
               head = node = nextNode;
            } else {
               node.next = nextNode;
               nextNode.previous = node;
               node = nextNode;
            }
            int elements = Math.min(unrollCount, numLeft);
            for (int j = 0; j < elements; j++) {
               node.elements[j] = (E) in.readObject();
            }
            node.count = elements;
            numLeft -= elements;
         }
         tail = node;
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
      for (E e : this) {
         out.writeObject(e);
      }
   }
   
   @Override
   public UnrolledLinkedList<E> clone() {
      if (getClass() == UnrolledLinkedList.class) {
         return new UnrolledLinkedList<E>(unrollCount, this);
      }
      try {
         @SuppressWarnings("unchecked")
         UnrolledLinkedList<E> clone = (UnrolledLinkedList<E>) super.clone();
         // deep copy all linked list nodes
         Node<E> curr = head;
         Node<E> currClone = null;
         while (curr != null) {
            Node<E> nextNode = new Node<E>(unrollCount);
            if (currClone == null) {
               clone.head = currClone = nextNode;
            } else {
               nextNode.previous = currClone;
               currClone.next = nextNode;
               currClone = nextNode;
            }
            currClone.count = curr.count;
            System.arraycopy(curr.elements, 0, currClone.elements, 0, curr.count);
            curr = curr.next;
         }
         clone.tail = currClone;
         return clone;
      } catch (CloneNotSupportedException e) {
         // should never happen since we implement Cloneable -- but just in
         // case, wrap in a runtime exception that sort of makes sense...
         throw new ClassCastException(Cloneable.class.getName());
      }
   }
}
