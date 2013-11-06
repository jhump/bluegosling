package com.apriori.collections;

// TODO: javadoc
// TODO: tests
public abstract class AbstractImmutableCollection<E> implements ImmutableCollection<E> {
   @Override
   public boolean isEmpty() {
      return size() == 0;
   }

   @Override
   public Object[] toArray() {
      return CollectionUtils.toArray(this);
   }

   @Override
   public <T> T[] toArray(T[] array) {
      return CollectionUtils.toArray(this, array);
   }

   @Override
   public boolean contains(Object o) {
      return CollectionUtils.contains(iterator(), o);
   }

   @Override
   public boolean containsAll(Iterable<?> items) {
      for (Object o : items) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      for (Object o : items) {
         if (contains(o)) {
            return true;
         }
      }
      return false;
   }
}
