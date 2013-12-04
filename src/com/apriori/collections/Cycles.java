package com.apriori.collections;

import com.apriori.util.Source;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
    * TODO: document me!
    *
    * @param initial initial iterator
    * @param sourceFromBeginning source for an iterator when wrapping from end back to beginning
    * @param sourceFromEnd source for an iterator when wrapping from beginning to end
    * @return a iterator that endlessly cycles through the sequenes of elements in the specified
    *       iterators and iterator sources
    */
   static <E> BidiIterator<E> cycleIterator(final BidiIterator<E> initial,
         final Source<BidiIterator<E>> sourceFromBeginning,
         final Source<BidiIterator<E>> sourceFromEnd) {
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
    * Returns a view of the specified double-ended queue as a cycle. Modifications to the cycle end
    * up also modifying the deque. Navigating through the cycle modifies the underlying deque by
    * shifting elements from the begining to end and vice versa. That way, the cycle's "current"
    * element is always the first in the deque.
    *
    * @param deque a deque
    * @return a view of the specified deque as a cycle
    */
   public static <E> Cycle<E> fromDeque(final Deque<E> deque) {
      return new Cycle<E>() {
         @Override
         public int size() {
            // TODO: implement me
            return 0;
         }

         @Override
         public boolean isEmpty() {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean contains(Object o) {
            // TODO: implement me
            return false;
         }

         @Override
         public Object[] toArray() {
            // TODO: implement me
            return null;
         }

         @Override
         public <T> T[] toArray(T[] a) {
            // TODO: implement me
            return null;
         }

         @Override
         public boolean add(E e) {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean remove(Object o) {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean addAll(Collection<? extends E> c) {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            // TODO: implement me
            return false;
         }

         @Override
         public void clear() {
            // TODO: implement me
            
         }

         @Override
         public void addFirst(E e) {
            // TODO: implement me
            
         }

         @Override
         public void addLast(E e) {
            // TODO: implement me
            
         }

         @Override
         public E set(E e) {
            // TODO: implement me
            return null;
         }

         @Override
         public E current() {
            // TODO: implement me
            return null;
         }

         @Override
         public int advance() {
            // TODO: implement me
            return 0;
         }

         @Override
         public int advanceBy(int distance) {
            // TODO: implement me
            return 0;
         }

         @Override
         public int retreat() {
            // TODO: implement me
            return 0;
         }

         @Override
         public int retreatBy(int distance) {
            // TODO: implement me
            return 0;
         }

         @Override
         public void reset() {
            // TODO: implement me
            
         }

         @Override
         public E remove() {
            // TODO: implement me
            return null;
         }

         @Override
         public Iterator<E> iterator() {
            // TODO: implement me
            return null;
         }

         @Override
         public BidiIterator<E> cycle() {
            // TODO: implement me
            return null;
         }

         @Override
         public Cycle<E> reverse() {
            // TODO: implement me
            return null;
         }

         @Override
         public boolean isCyclicOrder(E b, E c) {
            // TODO: implement me
            return false;
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
    * modifying the deque. Navigating through the cycle simply navigates using a {@link ListIterator}
    * and does not modify the underlying list.
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
            // TODO: update current?
            return CollectionUtils.removeObject(o, iterator(), true);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return list.containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends E> c) {
            // TODO: implement me
            return false;
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            // TODO: update current?
            return CollectionUtils.filter(c, iterator(), true);
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            // TODO: update current?
            return CollectionUtils.filter(c, iterator(), false);
         }

         @Override
         public void clear() {
            // TODO: update current?
            list.clear();
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
            // TODO: implement me
            return 0;
         }

         @Override
         public int retreat() {
            return retreatBy(1);
         }

         @Override
         public int retreatBy(int distance) {
            // TODO: implement me
            return 0;
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
            // TODO
            return null;
         }

         @Override
         public Iterator<E> iterator() {
            return iterator(current.nextIndex());
         }

         @Override
         public BidiIterator<E> cycle() {
            return cycleIterator(
                  BidiIterators.fromListIterator(list.listIterator(current.nextIndex())),
                  new Source<BidiIterator<E>>() {
                     @Override public BidiIterator<E> get() {
                        return BidiIterators.fromListIterator(list.listIterator());
                     }
                  },
                  new Source<BidiIterator<E>>() {
                     @Override public BidiIterator<E> get() {
                        return BidiIterators.fromListIterator(list.listIterator(list.size()));
                     }
                  });
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
         public boolean isCyclicOrder(E b, E c) {
            return isCyclicOrder(current(), b, c);
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
