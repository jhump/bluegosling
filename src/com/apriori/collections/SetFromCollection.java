package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

// TODO: javadoc
// TODO: tests
public class SetFromCollection<E> implements Set<E> {
   
   public static <E> Set<E> make(Collection<E> coll) {
      return coll instanceof Set ? (Set<E>) coll : new SetFromCollection<>(coll);
   }
   
   private final Collection<E> coll;
   
   private SetFromCollection(Collection<E> coll) {
      this.coll = coll;
   }

   @Override
   public int size() {
      return Iterables.size(iterator());
   }

   @Override
   public boolean isEmpty() {
      return coll.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return coll.contains(o);
   }

   @Override
   public Iterator<E> iterator() {
      return Iterables.unique(coll.iterator());
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
      if (contains(e)) {
         return false;
      } else {
         return coll.add(e);
      }
   }

   @Override
   public boolean remove(Object o) {
      boolean ret = false;
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         E e = iter.next();
         if (Objects.equals(e, o)) {
            iter.remove();
            ret = true;
         }
      }
      return ret;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return coll.containsAll(c);
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      boolean ret = false;
      for (E e : c) {
         if (add(e)) {
            ret = true;
         }
      }
      return ret;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return coll.retainAll(c);
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return coll.removeAll(c);
   }

   @Override
   public void clear() {
      coll.clear();
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}
