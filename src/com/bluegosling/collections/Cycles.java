package com.bluegosling.collections;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility methods for creating instances of {@link Cycle}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc?
// TODO: tests
public final class Cycles {
   private Cycles() {
   }

   /**
    * Creates a cycle iterator from the specified initial iterator and sources for iterators from
    * the beginning of the sequence and from the end of the sequence. The sources are used to
    * provide entries in the cycle when iteration wraps from end back to beginning and vice versa.
    *
    * @param initial initial iterator
    * @param sourceFromBeginning source for an iterator when wrapping from end back to beginning
    * @param sourceFromEnd source for an iterator when wrapping from beginning to end
    * @return a iterator that endlessly cycles through the sequences of elements in the specified
    *       iterators and iterator sources
    */
   static <E> BidiIterator<E> cycleIterator(final BidiIterator<E> initial,
         final Supplier<BidiIterator<E>> sourceFromBeginning,
         final Supplier<BidiIterator<E>> sourceFromEnd) {
      return new BidiIterator<E>() {
         BidiIterator<E> iter = initial;

         private void checkNextAndReset() {
            if (!iter.hasNext()) {
               iter = sourceFromBeginning.get();
            }
         }
         
         private void checkPreviousAndReset() {
            if (!iter.hasPrevious()) {
               iter = sourceFromEnd.get();
            }
         }
         
         @Override
         public boolean hasNext() {
            checkNextAndReset();
            return iter.hasNext();
         }

         @Override
         public E next() {
            checkNextAndReset();
            return iter.next();
         }

         @Override
         public boolean hasPrevious() {
            checkPreviousAndReset();
            return iter.hasPrevious();
         }

         @Override
         public E previous() {
            checkPreviousAndReset();
            return iter.previous();
         }

         @Override
         public void remove() {
            iter.remove();
         }

         @Override
         public BidiIterator<E> reverse() {
            return new ReversedBidiIterator<E>(this);
         }
      };
   }
   
   /**
    * Creates a cycle iterator from uni-directional iterators by persisting iteration history in
    * linked lists. The lists allow moving backwards to already visited elements. Since a
    * uni-directional iterator, by definition, cannot go backwards, we require two iterators as
    * inputs: one that advances forward through the sequence, from the beginning; a second that
    * moves backwards through the sequence, from the end.
    * 
    * <p>Due to the persistent nature of iteration, the returned iterator is strongly consistent
    * with the point in time at which is was created.
    *
    * @param forwardFromHead an iterator that advances forward through the sequeunce, from the start
    * @param reverseFromTail an iterator that moves backwards through the sequence, from the end
    * @return a iterator that endlessly cycles through the sequences of elements in the specified
    *       iterators and iterator sources
    */
   static <E> BidiIterator<E> persistentCycle(Iterator<E> forwardFromHead,
         Iterator<E> reverseFromTail) {
      if (!forwardFromHead.hasNext()) {
         assert !reverseFromTail.hasNext();
         return BidiIterators.emptyIterator();
      }
      assert reverseFromTail.hasNext();
      Node<E> forward = nodeFromIter(forwardFromHead);
      Node<E> backward = nodeFromIter(reverseFromTail);
      return cycleIterator(iterFromNode(forward),
            () -> iterFromNode(forward), () -> iterFromNode(backward).reverse());
   }
   
   /**
    * A node in a self-generating linked list. The value for the node and its predecessors are
    * given. The successor is lazily generated using a given function.
    *
    * @param <E> the type of element in the linked list
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Node<E> {
      private final E val;
      private final Node<E> prev;
      private Node<E> next;
      private Function<Node<E>, Node<E>> nextFn;
      
      Node(E val, Node<E> prev, Function<Node<E>, Node<E>> nextFn) {
         this.val = val;
         this.prev = prev;
         this.nextFn = nextFn;
      }
      
      E value() {
         return val;
      }
      
      Node<E> next() {
         if (nextFn != null) {
            assert next == null;
            next = nextFn.apply(this);
            nextFn = null;
         }
         return next;
      }
      
      Node<E> previous() {
         return prev;
      }
   };
   
   /**
    * A sentinel type that represents the end of a sequence. A node of this kind is used as a "cap"
    * to denote the end of the sequence. This is used instead of, for example, {@code null} so that
    * if we navigate past the end of the list, we can still navigate back to the list via this
    * node's {@link #previous()} method.
    *
    * @param <E> the type of element in the linked list
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class End<E> extends Node<E> {
      End(Node<E> prev) {
         super(null, prev, null);
      }
   };
   
   /**
    * Returns the head of a linked list that generates itself from the iterator. Successor nodes are
    * created using the iterator's {@link Iterator#next() next()} method.
    *
    * @param iter an iterator
    * @return the head of a linked list that is generated from the iterator's contents
    */
   static <E> Node<E> nodeFromIter(Iterator<E> iter) {
      Function<Node<E>, Node<E>> fn = new Function<Node<E>, Node<E>>() {
         // can't use lambda because we reference "this" (recursion)
         @Override public Node<E> apply(Node<E> n) {
            return iter.hasNext() ? new Node<E>(iter.next(), n, this) : new End<E>(n);
         }
      };
      return fn.apply(null);
   }
   
   /**
    * Returns a view of the given linked list node as a {@link BidiIterator}.
    *
    * @param node a node in a linked list
    * @return a bi-directional iterator for navigating the list
    */
   static <E> BidiIterator<E> iterFromNode(Node<E> node) {
      return new BidiIterator<E>() {
         Node<E> curr = node;
         
         @Override
         public boolean hasNext() {
            return !(curr instanceof End);
         }

         @Override
         public E next() {
            if (curr instanceof End) {
               throw new NoSuchElementException();
            }
            E ret = curr.value();
            curr = curr.next();
            return ret;
         }

         @Override
         public BidiIterator<E> reverse() {
            return new ReversedBidiIterator<>(this);
         }

         @Override
         public boolean hasPrevious() {
            return curr.previous() != null;
         }

         @Override
         public E previous() {
            Node<E> prev = curr.previous();
            if (prev == null) {
               throw new NoSuchElementException();
            }
            curr = prev;
            return curr.value();
         }
      };
   }
   
   /**
    * Returns a view of the specified double-ended queue as a cycle. Modifications to the cycle end
    * up also modifying the deque. Navigating through the cycle modifies the underlying deque by
    * shifting elements from the beginning to end and vice versa. That way, the cycle's "current"
    * element is always the first in the deque.
    *
    * @param deque a deque
    * @return a view of the specified deque as a cycle
    */
   public static <E> Cycle<E> fromDeque(final Deque<E> deque) {
      return new Cycle<E>() {
         @Override
         public int size() {
            return deque.size();
         }

         @Override
         public boolean isEmpty() {
            return deque.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return deque.contains(o);
         }

         @Override
         public Object[] toArray() {
            return deque.toArray();
         }

         @Override
         public <T> T[] toArray(T[] a) {
            return deque.toArray(a);
         }

         @Override
         public boolean remove(Object o) {
            return deque.remove(o);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return deque.containsAll(c);
         }

         @Override
         public boolean add(E e) {
            return deque.add(e);
         }

         @Override
         public boolean addAll(Collection<? extends E> c) {
            return deque.addAll(c);
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            return deque.removeAll(c);
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            return deque.retainAll(c);
         }

         @Override
         public void clear() {
            deque.clear();
         }

         @Override
         public void addFirst(E e) {
            deque.addFirst(e);
         }

         @Override
         public void addLast(E e) {
            deque.addLast(e);
         }

         @Override
         public E set(E e) {
            E ret = deque.removeFirst();
            deque.addFirst(e);
            return ret;
         }

         @Override
         public E current() {
            return deque.peekFirst();
         }

         @Override
         public int advance() {
            if (deque.size() <= 1) {
               return 0;
            }
            E e = deque.removeFirst();
            deque.addLast(e);
            return 1;
         }

         @Override
         public int advanceBy(int distance) {
            int sz = deque.size();
            if (sz <= 1) {
               return 0;
            }
            distance = distance % sz;
            for (int i = 0; i < distance; i++) {
               E e = deque.removeFirst();
               deque.addLast(e);
            }
            return distance;
         }

         @Override
         public int retreat() {
            if (deque.size() <= 1) {
               return 0;
            }
            E e = deque.removeLast();
            deque.addFirst(e);
            return 1;
         }

         @Override
         public int retreatBy(int distance) {
            int sz = deque.size();
            if (sz <= 1) {
               return 0;
            }
            distance = distance % sz;
            for (int i = 0; i < distance; i++) {
               E e = deque.removeLast();
               deque.addFirst(e);
            }
            return distance;
         }

         @Override
         public void reset() {
            // TODO: keep track of our distance from origin, so we can implement this
         }

         @Override
         public E remove() {
            return deque.removeFirst();
         }

         @Override
         public Iterator<E> iterator() {
            return deque.iterator();
         }

         @Override
         public BidiIterator<E> cycle() {
            return persistentCycle(deque.iterator(), deque.descendingIterator());
         }

         @Override
         public Cycle<E> reverse() {
            return new ReversedCycle<E>(this) {
               @Override
               public Iterator<E> iterator() {
                  return deque.descendingIterator();
               }
            };
         }

         @Override
         public boolean isCyclicOrder(E a, E b, E c) {
            // TODO: implement me
            return false;
         }
      };
   }
   
   /**
    * Returns a view of the specified list as a cycle. Modifications to the cycle end up also
    * modifying the deque. Navigating through the cycle simply navigates using a
    * {@link ListIterator} and does not modify the underlying list.
    *
    * @param list a list
    * @return a view of the specified list as a cycle
    */
   public static <E> Cycle<E> fromList(final List<E> list) {
      return new Cycle<E>() {
         ListIterator<E> current = list.listIterator();

         @Override
         public int size() {
            return list.size();
         }

         @Override
         public boolean isEmpty() {
            return list.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return list.contains(o);
         }

         @Override
         public Object[] toArray() {
            return CollectionUtils.toArray(this);
         }

         @Override
         public <T> T[] toArray(T[] a) {
            return CollectionUtils.toArray(this, a);
         }

         @Override
         public boolean add(E e) {
            addLast(e);
            return true;
         }

         @Override
         public boolean remove(Object o) {
            // TODO: re-implement so we can track current
            return CollectionUtils.removeObject(o, iterator(), true);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return list.containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends E> c) {
            int idx = current.nextIndex();
            boolean ret = false;
            for (E e : c) {
               list.add(idx++, e);
               ret = true;
            }
            if (ret) {
               current = idx == list.size() ? list.listIterator() : list.listIterator(idx);
            }
            return ret;
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            // TODO: re-implement so we can track current
            return CollectionUtils.filter(c, iterator(), true);
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            // TODO: re-implement so we can track current
            return CollectionUtils.filter(c, iterator(), false);
         }

         @Override
         public void clear() {
            list.clear();
            current = list.listIterator();
         }

         @Override
         public void addFirst(E e) {
            current.add(e);
            current.previous();
         }

         @Override
         public void addLast(E e) {
            int index = current.nextIndex();
            list.add(e);
            current = list.listIterator(index);
         }

         @Override
         public E set(E e) {
            E ret = current.next();
            current.set(e);
            current.previous();
            return ret;
         }

         @Override
         public E current() {
            E ret = current.next();
            current.previous();
            return ret;
         }

         @Override
         public int advance() {
            return advanceBy(1);
         }

         @Override
         public int advanceBy(int distance) {
            int sz = list.size();
            if (sz <= 1) {
               return 0;
            }
            distance = distance % sz;
            int curr = current.nextIndex() + distance;
            if (curr >= sz) {
               curr -= sz;
               assert curr < sz;
            }
            current = list.listIterator(curr);
            return distance;
         }

         @Override
         public int retreat() {
            return retreatBy(1);
         }

         @Override
         public int retreatBy(int distance) {
            int sz = list.size();
            if (sz <= 1) {
               return 0;
            }
            distance = distance % sz;
            int curr = current.nextIndex() - distance;
            if (curr < 0) {
               curr += sz;
               assert curr >= 0;
            }
            current = list.listIterator(curr);
            return distance;
         }

         @Override
         public void reset() {
            current = list.listIterator();
         }

         @Override
         public E remove() {
            E ret = current.next();
            current.remove();
            return ret;
         }
         
         Iterator<E> iterator(int startingAt) {
            // TODO: wrap if startingAt > 0 
            return list.listIterator(startingAt);
         }

         @Override
         public Iterator<E> iterator() {
            return iterator(current.nextIndex());
         }

         @Override
         public BidiIterator<E> cycle() {
            return cycleIterator(
                  BidiIterators.fromListIterator(list.listIterator(current.nextIndex())),
                  () -> BidiIterators.fromListIterator(list.listIterator()),
                  () -> BidiIterators.fromListIterator(list.listIterator(list.size())));
         }

         @Override
         public Cycle<E> reverse() {
            return new ReversedCycle<E>(this) {
               @Override public Iterator<E> iterator() {
                  // TODO: should be a descending iterator and must wrap from beginning to end
                  return list.listIterator(current.nextIndex());
               }
            };
         }

         @Override
         public boolean isCyclicOrder(E a, E b, E c) {
            // TODO: implement me
            return false;
         } 
      };
   }
   
   /**
    * Returns a view of the specified cycle as a {@link ListIterator}. Invoking {@link
    * ListIterator#next()} is equivalent to calling {@link Cycle#current()} followed by
    * {@link Cycle#advance()}. Invoking {@link ListIterator#previous()} is equivalent to calling
    * {@link Cycle#retreat()} followed by {@link Cycle#current()}.
    * 
    * <p>The list indices returned by this iterator do not have much meaning. The initial element
    * is considered to be list index zero. However, little attempt is made to keep this consistent,
    * especially in the face of concurrent modifications being made to the sequence through the
    * standard {@link Cycle} interface, or through other {@link ListIterator} views. Specifically,
    * the first call to {@link ListIterator#next()} will be the item at list index zero. Each call
    * to {@link ListIterator#next()} increments the current list index. Each call to
    * {@link ListIterator#previous()} decrements it. If the index would be become negative, it is
    * set to {@code size() - 1}. If it would become greater than or equal to {@code size()}, it is
    * set to zero. Unless the sequence is empty, {@link ListIterator#hasNext()} and
    * {@link ListIterator#hasPrevious()} always return true, and movement through the sequence is
    * cyclic and unending.
    *
    * @return a view of this cycle as a list iterator
    */
   public static <E> ListIterator<E> asListIterator(Cycle<E> cycle) {
      // TODO
      return null;
   }
}
