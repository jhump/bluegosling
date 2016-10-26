package com.bluegosling.collections.queues;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.bluegosling.collections.CollectionUtils;

/**
 * A priority queue that supports merging of two queues and is backed by a Fibonacci heap.
 * Insertion, merging, and finding the minimum (or maximum) value all run in constant time. Removing
 * the minimum element runs in amortized logarithmic time (amortized because remove operations incur
 * heap maintenance work, which will be logarithmic incremental work when averaged over many remove
 * operations but can cause worst case linear performance for a single operation). Other operations,
 * like finding or removing arbitrary elements run in linear time.
 * 
 * <p>This collection supports all optional operations of the {@link Queue} interface. Removal of
 * an element fetched by an iterator runs in logarithmic time worst case, constant time best case.
 * 
 * <p>Iteration order for this collection is not defined other than the minimum element in the queue
 * will be retrieved first.
 * 
 * <p>Since the {@link OrderedQueue} interface does not include the "decrease key"
 * operation, this implementation does not offer it. Removing an arbitrary element from a Fibonacci
 * heap would usually be done using the "decrease key" operation followed by a "remove minimum". But
 * arbitrary removal of elements in this heap is only done via its {@link #iterator()}, and that
 * standard remove algorithm would make it very difficult to continue with iteration and still
 * guarantee that each element is traversed once and only once. So a modified remove operation is
 * used instead:
 * <ol>
 * <li>Remove the node from its list of siblings.</li>
 * <li>Splice the removed node's children (if any) into its place in the list of siblings.</li>
 * <li>Update the parent's pointer to children so it is pointing at the smallest child (now that
 *    its children have been augmented with grandchildren, from the removed child).</li>
 * <li>Decrement the sub-tree node count for the parent node and all ancestor nodes, up to a root.
 *    If the decreased count falls below a threshold, also decrement the node's order.</li> 
 * </ol>
 * This revised removal algorithm means that we don't need to keep track of a "mark" on each node.
 * Also, since we only do removals during iteration, and the iterator maintains a stack that
 * implicitly identifies the path to the current node, we do not need to keep track of back pointers
 * to nodes' parents. Instead, the parent can be found by examining the iteration stack.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the queue
 */
// TODO: tests that exercise all code paths in iterator.remove() and can verify integrity of
// structure after removing items that way
// TODO: OR throw UnsupportedOperationException from iterator.remove() and implement remove the
// standard way, using decrease-key and extract-min
public class FibonacciHeapOrderedQueue<E> extends AbstractCollection<E>
      implements MeldableOrderedQueue<E, FibonacciHeapOrderedQueue<? extends E>>,
            Serializable, Cloneable {
   
   private static final long serialVersionUID = -7500815513249839345L;
   
   private static final int MAX_DEGREE = 31;
   static final int DEGREE_THRESHOLDS[] = new int[MAX_DEGREE];
   static {
      int threshold = 1;
      for (int i = 0; i < MAX_DEGREE; i++) {
         DEGREE_THRESHOLDS[i] = threshold;
         threshold <<= 1;
      }
   }

   /**
    * A node in a Fibonacci tree. In addition to having references to child nodes, there are also
    * next and previous nodes since child nodes and tree roots are stored as circular doubly-linked
    * lists.
    * 
    * <p>We store the actual tree size, in addition to its degree, since we support arbitrary
    * removal of nodes via the {@link Iterator} interface. So we know to decrement a tree's degree
    * if enough items are arbitrarily removed from it. With only normal enqueue and dequeue
    * operations, the size would be unnecessary and the degree alone would suffice.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <E> the type of element in the queue
    */
   private static class Node<E> {
      final E element;
      int degree;
      int size;
      Node<E> next;
      Node<E> previous;
      Node<E> children;
      
      Node(E element) {
         this.element = element;
         this.size = 1;
         this.degree = 0;
         this.previous = this;
         this.next = this;
      }
   }
   
   transient Comparator<? super E> comp;
   transient Node<E> minRoot;
   transient int size;
   transient int modCount;
   
   /**
    * Constructs a new empty queue. Elements will be compared using their {@linkplain Comparable
    * natural ordering}.
    */
   public FibonacciHeapOrderedQueue() {
      this((Comparator<? super E>) null);
   }
   
   /**
    * Constructs a new empty queue. Elements will be compared using the specified comparator.
    * 
    * @param comp a comparator
    */
   public FibonacciHeapOrderedQueue(Comparator<? super E> comp) {
      if (comp == null) {
         this.comp = CollectionUtils.naturalOrder();
      } else {
         this.comp = comp;
      }
   }

   /**
    * Constructs a new queue whose contents are initialized to the elements in the specified
    * collection. The elements will be compared using their {@linkplain Comparable natural ordering}.
    * 
    * @param coll a collection of elements that will be in the constructed queue
    */
   public FibonacciHeapOrderedQueue(Collection<? extends E> coll) {
      this(coll, null);
   }

   public FibonacciHeapOrderedQueue(Collection<? extends E> coll, Comparator<? super E> comp) {
      this(comp);
      addAll(coll);
   }

   /**
    * Constructs a new queue whose contents and comparator are initialized to those of the specified
    * queue.
    * 
    * @param queue a queue
    */
   @SuppressWarnings("unchecked") // this could be unsafe if specified collection has incompatible
                                 // comparator. we'll trust user to specify a valid source
   public FibonacciHeapOrderedQueue(FibonacciHeapOrderedQueue<? extends E> queue) {
      this(queue, (Comparator<? super E>) queue.comparator());
   }

   /**
    * Constructs a new queue whose contents and comparator are initialized to those of the specified
    * queue.
    * 
    * @param queue a queue
    */
   @SuppressWarnings("unchecked") // this could be unsafe if specified collection has incompatible
                                 // comparator. we'll trust user to specify a valid source
   public FibonacciHeapOrderedQueue(PriorityQueue<? extends E> queue) {
      this(queue, (Comparator<? super E>) queue.comparator());
   }

   /**
    * Constructs a new queue whose contents and comparator are initialized to those of the specified
    * set.
    * 
    * @param set a set
    */
   @SuppressWarnings("unchecked") // this could be unsafe if specified collection has incompatible
                                 // comparator. we'll trust user to specify a valid source
   public FibonacciHeapOrderedQueue(SortedSet<? extends E> set) {
      this(set, (Comparator<? super E>) set.comparator());
   }
   
   /**
    * Returns the comparator used to sort elements in the queue. If no comparator is used and
    * elements are sorted according to their {@linkplain Comparable natural ordering} then
    * {@code null} is returned.
    * 
    * @return the comparator used to sort elements or {@code null}
    */
   @Override public Comparator<? super E> comparator() {
      return comp == Comparator.naturalOrder() ? null : comp;
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public Iterator<E> iterator() {
      return new IteratorImpl();
   }

   @Override
   public void clear() {
      size = 0;
      minRoot = null;
      modCount++;
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
      modCount++;
      size++;
      minRoot = merge(minRoot, new Node<E>(e));
      return true;
   }
   
   private void consolidate() {
      // The degree of a tree is log-base-2 of its size. Largest possible heap has MAX_INTEGER
      // elements, and log(MAX_INTEGER) == 31. So that's the largest possible degree for a tree.
      
      // can't create generic array, so we must cast
      @SuppressWarnings("unchecked")
      Node<E> degrees[] = (Node<E>[]) new Node<?>[MAX_DEGREE + 1];
      
      Node<E> done = minRoot;
      Node<E> current = minRoot;
      do {
         // consolidation might cause current to get moved into other trees, so record this now
         // so we know the next item to examine
         Node<E> nextItem = current.next;
         while (degrees[current.degree] != null) {
            // merge current with existing tree for this degree
            Node<E> existing = degrees[current.degree];
            degrees[current.degree] = null; 
            Node<E> min, max;
            if (comp.compare(current.element, existing.element) < 0) {
               min = current;
               max = existing;
            } else {
               min = existing;
               max = current;
            }
            // if we modified the first root, make sure we'll still know when we hit the end of
            // the circular list
            if (done == max) {
               done = min;
            }
            if (minRoot == max) {
               // we don't want minRoot pointing at something that's no longer a root
               minRoot = min;
            }
            // remove max from the list
            max.previous.next = max.next;
            max.next.previous = max.previous;
            max.next = max.previous = max; // max is now a singleton
            // and merge it into min tree
            min.children = merge(min.children, max);
            min.size += max.size;
            min.degree++;
            current = min;
         }
         degrees[current.degree] = current;
         // Keep us pointed at the minimum element. This is necessary because when we merge
         // trees in extractMin(), just before consolidate() is called, we may no longer be
         // pointing at the min element since we just grabbed the min element's neighbor.
         if (comp.compare(current.element, minRoot.element) < 0) {
            minRoot = current;
         }
         current = nextItem;
      } while (current != done);
   }
   
   private Node<E> extractMin() {
      if (size == 0) {
         return null;
      }
      Node<E> ret = minRoot;
      if (minRoot == minRoot.next) {
         minRoot = minRoot.children;
      } else {
         // remove this root from the list
         minRoot.previous.next = minRoot.next;
         minRoot.next.previous = minRoot.previous;
         // merge the root's children into the list. minRoot at this point might not be pointing
         // at minimum element since we just grabbed minRoot's neighbor, but that will be fixed
         // when we consolidate
         minRoot = merge(minRoot.next, minRoot.children);
         consolidate();
      }
      modCount++;
      size--;
      return ret;
   }

   @Override
   public E remove() {
      Node<E> min = extractMin();
      if (min != null) {
         return min.element;
      }
      throw new NoSuchElementException();
   }

   @Override
   public E poll() {
      Node<E> min = extractMin();
      return min == null ? null : min.element;
   }
   
   private Node<E> findMin() {
      return minRoot;
   }

   @Override
   public E element() {
      Node<E> min = findMin();
      if (min != null) {
         return min.element;
      }
      throw new NoSuchElementException();
   }

   @Override
   public E peek() {
      Node<E> min = findMin();
      return min == null ? null : min.element;
   }
   
   @Override
   public boolean mergeFrom(FibonacciHeapOrderedQueue<? extends E> other) {
      if (other.minRoot == null) {
         return false; // other queue is empty, nothing to do
      }
      
      @SuppressWarnings("unchecked") // we're taking ownership of roots
      Node<E> otherMinRoot = (Node<E>) other.minRoot;
      size += other.size;
      other.clear(); // we're taking all elements
      modCount++;
      minRoot = merge(minRoot, otherMinRoot);
      return true;
   }
   
   Node<E> merge(Node<E> list1, Node<E> list2) {
      if (list1 == null) {
         return list2;
      } else if (list2 == null) {
         return list1;
      }
      
      // otherwise, we just append the nodes to this list
      list1.previous.next = list2;
      list2.previous.next = list1;
      Node<E> list1End = list1.previous;
      list1.previous = list2.previous;
      list2.previous = list1End;
      // and update our minimum if necessary
      if (comp.compare(list1.element, list2.element) < 0) {
         return list1;
      } else {
         return list2;
      }
   }
   
   /**
    * Customizes de-serialization to read elements the same way as written by
    * {@link #writeObject(ObjectOutputStream)}.
    * 
    * @param in the stream from which the set is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      comp = (Comparator<? super E>) in.readObject();
      if (comp == null) {
         comp = CollectionUtils.naturalOrder();
      }
      size = in.readInt();
      for (int i = 0; i < size; i++) {
         minRoot = merge(minRoot, new Node<E>((E) in.readObject()));
      }
   }
   
   /**
    * Customizes serialization by just writing the queue contents.
    * 
    * @param out the stream to which to serialize this list
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeObject(comparator());
      out.writeInt(size);
      for (E e : this) {
         out.writeObject(e);
      }
   }

   @Override
   public FibonacciHeapOrderedQueue<E> clone() {
      if (this.getClass() == FibonacciHeapOrderedQueue.class) {
         return new FibonacciHeapOrderedQueue<E>(this);
      } else {
         try {
            @SuppressWarnings("unchecked")
            FibonacciHeapOrderedQueue<E> clone = (FibonacciHeapOrderedQueue<E>) super.clone();
            clone.minRoot = null;
            clone.modCount = 0;
            // clone shouldn't share any nodes, so we need to re-create the other heap
            for (E e : this) {
               clone.minRoot = merge(clone.minRoot, new Node<E>(e));
            }
            return clone;
         } catch (CloneNotSupportedException e) {
            throw new AssertionError();
         }
      }
   }

   /**
    * A stack-frame used to track state of tree traversal when iterating through the queue.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <E> the type of element in the queue
    */
   private static class IterationFrame<E> {
      final Node<E> done;
      Node<E> current;
      
      IterationFrame(Node<E> current) {
         this.done = this.current = current;
      }
   }
   
   /**
    * Iterator implementation for the queue. This supports the optional remove operation.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class IteratorImpl implements Iterator<E> {
      private int myModCount = modCount;
      private ArrayDeque<IterationFrame<E>> stack;
      private boolean removed;
      
      IteratorImpl() {
      }
      
      private void checkModCount() {
         if (myModCount != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         if (stack == null) {
            // not yet started? we have a next if and only if queue is not empty
            return minRoot != null;
         }
         // if top of the stack has children or any frame on the stack has more siblings then we
         // have a next element
         boolean top = true;
         for (IterationFrame<E> frame : stack) {
            if ((top && frame.current.children != null) || frame.current.next != frame.done) {
               return true;
            }
            top = false;
         }
         return false;
      }

      @Override
      public E next() {
         checkModCount();
         IterationFrame<E> frame;
         if (stack == null) {
            // haven't yet started so we need to initialize the stack
            if (minRoot == null) {
               throw new NoSuchElementException();
            }
            frame = new IterationFrame<E>(minRoot);
            stack = new ArrayDeque<IterationFrame<E>>();
            stack.push(frame);
         } else {
            // depth-first, pre-order traversal of each tree
            IterationFrame<E> last = stack.peek();
            if (last.current.children != null) {
               // we have visited current, so now move on to its descendants
               frame = new IterationFrame<E>(last.current.children);
               stack.push(frame);
            } else {
               // after exhausting sub-tree, we advance to sibling node or (when none), pop frames
               // from the stack and advance to ancestors' sibling nodes.
               frame = null;
               while (last != null) {
                  if (last.current.next != last.done) {
                     // re-use last stack frame, but advance current node to next one in list
                     last.current = last.current.next;
                     frame = last;
                     break;
                  } else {
                     // pop the last frame off and examine predecessor frame
                     stack.pop();
                     last = stack.peek();
                  }
               }
            }
         }
         
         if (frame == null) {
            throw new NoSuchElementException();
         }

         removed = false; // reset
         return stack.peek().current.element;
      }

      @Override
      public void remove() {
         checkModCount();
         if (stack == null) {
            // not yet started? no element to remove
            throw new IllegalStateException("No call to next() so no element to remove");
         }
         if (removed) {
            throw new IllegalStateException("Last fetched item already removed");
         }
         Iterator<IterationFrame<E>> iter = stack.iterator();
         IterationFrame<E> last = iter.next();
         boolean firstItemRemoved = last.current == last.done;
         Node<E> siblings;
         if (last.current == last.current.next) {
            // only child? current node's children becomes its siblings
            siblings = last.current.children;
         } else {
            // otherwise, remove current node and merge in its children
            last.current.next.previous = last.current.previous;
            last.current.previous.next = last.current.next;
            Node<E> minSibling;
            if (firstItemRemoved) {
               // we are removing the min sibling, so now we need to find the next min
               minSibling = last.current.next;
               for (Node<E> node = minSibling.next; node != last.current.next;
                     node = node.next) {
                  if (comp.compare(node.element, minSibling.element) <= 0) {
                     minSibling = node;
                  }
               }
            } else {
               minSibling = last.done;
            }
            siblings = merge(minSibling, last.current.children);
         }
         // now we need to update pointer from parent node with new sibling list
         boolean siblingsUpdated = false;
         while (iter.hasNext()) {
            Node<E> ancestor = iter.next().current;
            if (!siblingsUpdated) {
               ancestor.children = siblings;
               siblingsUpdated = true;
            }
            if (--ancestor.size <= DEGREE_THRESHOLDS[ancestor.degree - 1]) {
               // we've removed enough nodes to knock the degree down
               ancestor.degree--;
            }
         }
         if (!siblingsUpdated) {
            // no other level in stack? then children were merged into root nodes
            minRoot = siblings;
         }
         // If we removed an item that was not the first, its children were appended to the end
         // of the list, so iteration can proceed and will get to all nodes. But if we did remove
         // the first, we had to find a new minimum sibling before the merge, so order is not known.
         // If that is the case, we need to "start over" for this level, which is done by popping
         // the frame from the stack so that a subsequent call to next() will push it back on the
         // stack and start it over.
         if (firstItemRemoved) {
            stack.pop();
            if (stack.isEmpty()) {
               stack = null;
            }
         }
         // final book keeping
         size--;
         myModCount = ++modCount;
         removed = true;
      }
   }   
   
   
   
   
   
   
   
   
   private static class Record<E> {
      Node<E> parent;
      Node<E> node;
      int level;
      
      Record(Node<E> node, Node<E> parent, int level) {
         this.node = node;
         this.parent = parent;
         this.level = level;
      }
   }
   
   private static <E> void addAll(Queue<Record<E>> queue, Node<E> first, Node<E> parent, int level) {
      if (first == null) {
         return;
      }
      Node<E> current = first;
      do {
         queue.add(new Record<E>(current, parent, level));
         current = current.next;
      } while (current != first);
   }

   private static <E> String getName(Node<E> node, Map<Node<E>, String> names, AtomicInteger id) {
      if (node == null) {
         return null;
      }
      String name = names.get(node);
      if (name == null) {
         name = "Node #" + id.incrementAndGet();
         names.put(node, name);
      }
      return name;
   }

   
   @SuppressWarnings("unused") // was used in some testing
   // TODO: remove this and write real unit test
   private void printState(String title) {
      System.out.println("*** " + title.toUpperCase() + " ***");
      AtomicInteger id = new AtomicInteger();
      Map<Node<E>, String> nodeNames = new HashMap<Node<E>, String>();
      Queue<Record<E>> bfsNodes = new LinkedList<Record<E>>();
      addAll(bfsNodes, minRoot, null, 1);
      System.out.println("Level 1 (Roots)");
      String prevParent = null;
      while (!bfsNodes.isEmpty()) {
         Record<E> rec = bfsNodes.remove();
         if (nodeNames.containsKey(rec.node)) {
            System.out.println("ERROR: Cycle detected. Already processed " + nodeNames.get(rec.node));
            break;
         }
         String name = getName(rec.node, nodeNames, id);
         String parentName = getName(rec.parent, nodeNames, id);
         if (parentName != prevParent) {
            System.out.println();
            System.out.println("Level " + rec.level + ", Parent: " + parentName);
            prevParent = parentName;
         }
         System.out.println("   " + name + ": \"" + rec.node.element + "\" size=" + rec.node.size + ", degree=" + rec.node.degree);
         addAll(bfsNodes, rec.node.children, rec.node, rec.level + 1);
      }
      System.out.println();
   }
}
