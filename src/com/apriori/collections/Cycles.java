package com.apriori.collections;

import com.apriori.util.Source;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// TODO: javadoc
// TODO: tests
public final class Cycles {
   private Cycles() {
   }
   
   static <E> Iterator<E> cycleIterator(final Source<Iterator<E>> source) {
      return new Iterator<E>() {
         Iterator<E> iter = source.get();

         private void checkAndReset() {
            if (!iter.hasNext()) {
               iter = source.get();
            }
         }
         
         @Override
         public boolean hasNext() {
            checkAndReset();
            return iter.hasNext();
         }

         @Override
         public E next() {
            checkAndReset();
            return iter.next();
         }

         @Override
         public void remove() {
            // TODO: implement me
         }
         
      };
   }
   
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
         public Iterator<E> cycle() {
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
            // TODO: should add to end of list
            current.add(e);
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
         public Iterator<E> cycle() {
            return cycleIterator(new Source<Iterator<E>>() {
               int pos = current.nextIndex();
               
               @Override
               public Iterator<E> get() {
                  // TODO: bounds check pos and adjust in case subsequent
                  // pass through sequence is after removals
                  return iterator(pos);
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
               
               @Override Source<Iterator<E>> pinnedIteratorSource() {
                  final int pos = current.nextIndex();
                  return new Source<Iterator<E>>() {
                     @Override public Iterator<E> get() {
                        // TODO: bounds check pos and adjust in case subsequent
                        // pass through sequence is after removals
                        // TODO: should be a descending iterator and must wrap from beginning to end
                        return list.listIterator(pos);
                     }
                  };
               }
            };
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
