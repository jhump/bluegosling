package com.apriori.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A thread-safe stack. New elements are CAS'ed onto the head of a linked list. Elements are removed
 * similarly: CAS'ing their predecessor to point at their successor.
 * 
 * <p>This structure exposes the operations of a stack, and iteration yields the newest elements
 * first. But it can be useful as a general-purpose thread-safe collection. For example, it can be
 * used for waiter queues, where each element added is a parked thread.
 * 
 * <p>This structure is safe to use concurrently from multiple threads. Iterators will never throw
 * {@link ConcurrentModificationException}s. However, a single iterator is not meant to be used by
 * multiple concurrent threads and is thus not thread-safe. This collection supports all optional
 * operations.
 * 
 * <p>Bulk operations are not atomic. So reads that execute concurrently with bulk operations may
 * see partial results from in-progress the bulk operations.
 *
 * @param <T> the type of values stored in the stack
 * 
 * @see SimpleTreiberStack
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class TreiberStack<T> extends AbstractCollection<T> implements Stack<T> {

   @SuppressWarnings("rawtypes")
   private static final AtomicReferenceFieldUpdater<TreiberStack, Node> headUpdater =
         AtomicReferenceFieldUpdater.newUpdater(TreiberStack.class, Node.class, "head");

   @SuppressWarnings("rawtypes")
   private static final AtomicIntegerFieldUpdater<TreiberStack> sizeUpdater =
         AtomicIntegerFieldUpdater.newUpdater(TreiberStack.class, "size");

   /**
    * A node in a linked list. The node has a value, but it's also an atomic markable reference to
    * its next node. The mark is false on creation and then set to true when the node is removed
    * from the list.
    *
    * @param <T> the type of value stored in the node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Node<T> extends AtomicMarkableReference<Node<T>> {
      final T value;
      
      Node(T value) {
         this(value, null);
      }

      Node(T value, Node<T> next) {
         super(next, false);
         this.value = value;
      }
}
   
   volatile Node<T> head;
   volatile int size;
   
   /**
    * Constructs a new collection.
    */
   public TreiberStack() {
   }

   /**
    * Constructs a new collection with the specified initial contents.
    *
    * @param coll the initial contents of the constructed collection
    */
   public TreiberStack(Collection<? extends T> coll) {
      addAll(coll);
   }
   
   /**
    * Constructs a new collection with the given size and linked list.
    *
    * @param head a linked list of nodes
    * @param size the number of nodes in the given list
    */
   private TreiberStack(Node<T> head, int size) {
      this.head = head;
      this.size = size;
   }

   @Override
   public Iterator<T> iterator() {
      return new Iterator<T>() {
         Node<T> next = head;
         Node<T> current;
         Node<T> previous;
         boolean removed[] = new boolean[1];
         boolean lastFetchedWasRemoved;
         
         @Override
         public boolean hasNext() {
            return next != null;
         }

         @Override
         public T next() {
            if (next == null) {
               throw new NoSuchElementException();
            }
            previous = current;
            current = next;
            do {
               next = next.get(removed);
            } while (next != null && removed[0]);
            lastFetchedWasRemoved = false;
            return current.value;
         }
         
         @Override
         public void remove() {
            if (current == null || lastFetchedWasRemoved) {
               throw new IllegalStateException();
            }
            lastFetchedWasRemoved = true;
            removeNode(current, previous, removed);
         }
      };
   }
   
   @Override
   public boolean add(T value) {
      Node<T> newHead = new Node<>(value);
      while (true) {
         Node<T> oldHead = head;
         newHead.set(oldHead, false);
         if (headUpdater.compareAndSet(this, oldHead, newHead)) {
            sizeUpdater.incrementAndGet(this);
            return true;
         }
      }
   }

   @Override
   public boolean remove(Object value) {
      boolean removed[] = new boolean[1];
      Node<T> predecessor = null;
      Node<T> current = head;
      while (true) {
         while (true) {
            if (current == null) {
               // not found
               return false;
            }
            T t = current.value;
            if (value == null ? t == null : value.equals(t)) {
               // found it
               break;
            }
            Node<T> successor = current.get(removed);
            // we want to ignore removed nodes we encounter, so only update
            // predecessor to a not-removed node
            if (!removed[0]) {
               predecessor = current;
            }
            current = successor;
         }
         if (removeNode(current, predecessor, removed)) {
            return true;
         }
         // at this point, we found a node with the right value but it was concurrently deleted,
         // so let's look for another matching value
      }
   }
   
   boolean removeNode(Node<T> node, Node<T> predecessor, boolean removed[]) {
      // first let's mark the node as removed
      Node<T> next;
      while (true) {
         next = node.get(removed);
         if (removed[0]) {
            // concurrently removed
            return false;
         }
         if (node.compareAndSet(next, next, false, true)) {
            sizeUpdater.decrementAndGet(this);
            break;
         }
      }
      
      // now we try to repair link from previous node to next to route around deleted node
      Node<T> replaced = node;
      while (true) {
         if (predecessor == null) {
            if (headUpdater.compareAndSet(TreiberStack.this, replaced, next)) {
               break;
            }
         } else if (predecessor.compareAndSet(replaced, next, false, false)) {
            break;
         }
         // predecessor node has changed concurrently, so we have to search through
         // the linked list for it again
         predecessor = null;
         removed[0] = false;
         Node<T> current = head;
         while (current != null) {
            if (current == node) {
               // found it
               break;
            }
            // we want to ignore removed nodes we encounter, so only update
            // predecessor to a not-removed node
            Node<T> successor = current.get(removed);
            if (!removed[0]) {
               predecessor = current;
               // since the predecessor we update may not be pointing at
               // the node to remove (it may instead point at the start
               // of a chain of removed nodes), so we need to keep track
               // of what it's pointing at so we can do a CAS
               replaced = successor;
            }
            current = successor;
         }
         if (current == null) {
            // we never found the node, so a concurrent remove saw the node was deleted
            // and routed the links around it... nothing more to do
            break;
         }
      }
      return true;
   }
   
   @Override
   public int size() {
      return size;
   }
   
   /**
    * Pushes a new value onto the stack. This is equivalent to {@code stack.add(value)} but uses the
    * typical vocabulary of the stack ADT.
    *
    * @param value the value to push
    */
   @Override
   public void push(T value) {
      add(value);
   }
   
   /**
    * Peeks at the value on the top of the stack. If the stack is empty, null is returned. The stack
    * is unchanged by this method.
    *
    * @return the most recently added item, or null if the stack is empty
    */
   @Override
   public T peek() {
      return doPeek(() -> null);
   }
   
   /**
    * Queries for the element on the top of the stack. The stack is unchanged by this method.
    *
    * @return the most recently added item
    * @throws NoSuchElementException if the stack is empty
    */
   @Override
   public T element() {
      return doPeek(() -> { throw new NoSuchElementException(); });
   }
   
   private T doPeek(Supplier<T> ifEmpty) {
      boolean removed[] = new boolean[1];
      while (true) {
         Node<T> node = head;
         Node<T> next;
         // find a successor that isn't already removed
         while (true) {
            if (node == null) {
               return ifEmpty.get();
            }
            next = node.get(removed);
            if (!removed[0]) {
               return node.value; 
            }
            node = next;
         }
      }
   }
   
   /**
    * Pops a value off of the stack. The item returned will be the most recently added/pushed item.
    *
    * @return the most recently added item, removed from the stack
    * @throws NoSuchElementException if the stack is empty
    */
   @Override
   public T pop() {
      return doPop(() -> { throw new NoSuchElementException(); });
   }

   /**
    * Polls for the value at the top of the stack. If the stack is empty, null is returned.
    * Otherwise, the item at the top of the stack is removed and returned.
    *
    * @return the most recently added item, removed from the stack; or null if the stack is empty
    */
   @Override
   public T poll() {
      return doPop(() -> null);
   }

   private T doPop(Supplier<T> ifEmpty) {
      boolean removed[] = new boolean[1];
      while (true) {
         Node<T> node = head;
         Node<T> next;
         // find a successor that isn't already removed
         while (true) {
            if (node == null) {
               return ifEmpty.get();
            }
            next = node.get(removed);
            if (!removed[0]) {
               break;
            }
            node = next;
         }
         if (headUpdater.compareAndSet(this, node, next)) {
            sizeUpdater.decrementAndGet(this);
            return node.value;
         }
      }
   }
   
   /**
    * Drains all elements from the stack and adds them to the given collection. 
    *
    * @param coll the collection to which the removed items are added
    */
   @Override
   public void drainTo(Collection<? super T> coll) {
      doClear(null, (v, n) -> { coll.add(n.value); return null; });
   }
   
   /**
    * Clears all elements from the stack and returns them in a new stack.
    *
    * @return a new stack that contains all of the items removed from this stack
    */
   @Override
   public TreiberStack<T> removeAll() {
      int sz[] = new int[1];
      @SuppressWarnings("unchecked")
      Node<T> list[] = (Node<T>[]) new Node<?>[1];
      // We want the new stack to be in the same order as the one we're clearing. Simply pushing
      // the removed items to new list would end up reversing the order. So this is a little
      // complicated to get the order right.
      doClear(null, (Node<T> tail, Node<T> removedNode) -> {
         sz[0]++;
         Node<T> newTail = new Node<T>(removedNode.value);
         if (tail == null) {
            assert list[0] == null;
            list[0] = newTail; 
         } else {
            tail.set(newTail, false);
         }
         return newTail;
      });
      return new TreiberStack<>(list[0], sz[0]);
   }
   
   @Override
   public void clear() {
      doClear(null, (v, n) -> null);
   }
   
   private <R> R doClear(R start, BiFunction<R, Node<T>, R> action) {
      R r = start;
      @SuppressWarnings("unchecked")
      Node<T> node = headUpdater.getAndSet(this, null);
      // in case there are concurrent remove operations in progress, we
      // must go through the linked list and count the actual items we're
      // removing and decrement the size as we go
      boolean removed[] = new boolean[1];
      while (node != null) {
         Node<T> next;
         while (true) {
            next = node.get(removed);
            if (removed[0]) {
               break;
            }
            if (node.compareAndSet(next, next, false, true)) {
               sizeUpdater.decrementAndGet(this);
               r = action.apply(r, node);
               break;
            }
         }
         node = next;
      }
      return r;
   }
}
