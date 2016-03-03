package com.bluegosling.collections.views;

import com.bluegosling.collections.ArrayUtils;
import com.bluegosling.collections.BidiIterator;
import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.Cycle;

import java.util.Collection;

// TODO: javadoc
// TODO: tests?
public abstract class ReversedCycle<E> implements Cycle<E> {
   private final Cycle<E> delegate;
   
   public ReversedCycle(Cycle<E> delegate) {
      this.delegate = delegate;
   }

   @Override
   public int size() {
      return delegate.size();
   }

   @Override
   public boolean isEmpty() {
      return delegate.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return delegate.contains(o);
   }

   @Override
   public Object[] toArray() {
      Object array[] = delegate.toArray();
      ArrayUtils.reverse(array);
      return array;
   }

   @Override
   public <T> T[] toArray(T[] a) {
      T array[] = delegate.toArray(a);
      ArrayUtils.reverse(array);
      return array;
   }

   @Override
   public boolean add(E e) {
      addLast(e);
      return true;
   }

   @Override
   public boolean remove(Object o) {
      return CollectionUtils.removeObject(o, iterator(), true);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return delegate.containsAll(c);
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      // with O(n) more memory, we might be able to do this more efficiently (e.g. reverse the
      // collection and then use underlying addAll()), but that doesn't feel like a good trade-off
      // TODO: I think this actually needs to be reversed in order to be correct.
      for (E e : c) {
         addLast(e);
      }
      return true;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return delegate.removeAll(c);
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return delegate.retainAll(c);
   }

   @Override
   public void clear() {
      delegate.clear();
   }

   @Override
   public void addFirst(E e) {
      if (delegate.isEmpty()) {
         delegate.addFirst(e);
      } else {
         delegate.advance();
         delegate.addFirst(e);
      }
   }

   @Override
   public void addLast(E e) {
      if (delegate.isEmpty()) {
         delegate.addLast(e);
      } else {
         delegate.advance();
         delegate.addFirst(e);
         delegate.retreat();
      }
   }

   @Override
   public E set(E e) {
      return delegate.set(e);
   }

   @Override
   public E current() {
      return delegate.current();
   }

   @Override
   public int advance() {
      return delegate.retreat();
   }

   @Override
   public int advanceBy(int distance) {
      return delegate.retreatBy(distance);
   }

   @Override
   public int retreat() {
      return delegate.advance();
   }

   @Override
   public int retreatBy(int distance) {
      return delegate.advanceBy(distance);
   }

   @Override
   public void reset() {
      delegate.reset();
   }

   @Override
   public E remove() {
      E ret = delegate.remove();
      if (!delegate.isEmpty()) {
         delegate.retreat();
      }
      return ret;
   }

   @Override
   public BidiIterator<E> cycle() {
      return delegate.cycle().reverse();
   }

   @Override
   public Cycle<E> reverse() {
      return delegate;
   }

   @Override
   public boolean isCyclicOrder(E b, E c) {
      return delegate.isCyclicOrder(c, b);
   }

   @Override
   public boolean isCyclicOrder(E a, E b, E c) {
      return delegate.isCyclicOrder(a, c, b);
   }

}
