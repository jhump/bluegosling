package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;

import static com.apriori.collections.ImmutableCollectionWrapper.fromIterable;

abstract class PersistentCollectionWrapper<E, C extends Collection<E>,
                                           I extends ImmutableCollectionWrapper<E, C>,
                                           P extends PersistentCollectionWrapper<E, C, I, P>>
      implements PersistentCollection<E> {
   
   protected final I wrapper;
   
   PersistentCollectionWrapper(C collection) {
      this.wrapper = wrapImmutable(collection);
   }
   
   protected abstract C copy(C original);
   protected abstract I wrapImmutable(C collection);
   protected abstract P wrapPersistent(C collection);
   
   protected C wrapped() {
      return wrapper.wrapped();
   }

   @Override
   public int size() {
      return wrapper.size();
   }

   @Override
   public boolean isEmpty() {
      return wrapper.isEmpty();
   }

   @Override
   public Object[] toArray() {
      return wrapper.toArray();
   }

   @Override
   public <T> T[] toArray(T[] array) {
      return wrapper.toArray(array);
   }

   @Override
   public boolean contains(Object o) {
      return wrapper.contains(o);
   }

   @Override
   public boolean containsAll(Iterable<?> items) {
      return wrapper.containsAll(items);
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      return wrapper.containsAny(items);
   }

   @Override
   public Iterator<E> iterator() {
      return wrapper.iterator();
   }

   @Override
   public P add(E e) {
      C newCollection = copy(wrapped());
      newCollection.add(e);
      return wrapPersistent(newCollection);
   }

   @Override
   public P remove(Object o) {
      C newCollection = copy(wrapped());
      newCollection.remove(o);
      return wrapPersistent(newCollection);
   }

   @Override
   public P removeAll(Object o) {
      C newCollection = copy(wrapped());
      for (Iterator<E> iter = newCollection.iterator(); iter.hasNext();) {
         E e = iter.next();
         if (o == null ? e == null : o.equals(e)) {
            iter.remove();
         }
      }
      return wrapPersistent(newCollection);
   }

   @Override
   public P removeAll(Iterable<?> items) {
      C newCollection = copy(wrapped());
      newCollection.removeAll(fromIterable(items));
      return wrapPersistent(newCollection);
   }

   @Override
   public P retainAll(Iterable<?> items) {
      C newCollection = copy(wrapped());
      newCollection.retainAll(fromIterable(items));
      return wrapPersistent(newCollection);
   }

   @Override
   public P addAll(Iterable<? extends E> items) {
      C newCollection = copy(wrapped());
      newCollection.addAll(fromIterable(items));
      return wrapPersistent(newCollection);
   }
}
