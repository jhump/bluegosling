package com.bluegosling.collections;

import java.util.Iterator;

/**
 * A persistent set that is backed by a persistent map.
 *
 * @param <E> the type of elements in the set
 * 
 * @see PersistentSet#newSetFromMap(PersistentMap)
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
class PersistentSetFromMap<E> extends AbstractImmutableSet<E> implements PersistentSet<E> {
   
   private final PersistentMap<E, Boolean> map;

   /**
    * Creates a new set backed by the given map. 
    *
    * @param map the persistent map that backs the set
    */
   PersistentSetFromMap(PersistentMap<E, Boolean> map) {
      this.map = map;
   }

   @Override
   public int size() {
      return map.size();
   }

   @Override
   public Iterator<E> iterator() {
      return map.keySet().iterator();
   }

   @Override
   public PersistentSet<E> removeAll(Object o) {
      return new PersistentSetFromMap<>(map.remove(o));
   }

   @Override
   public PersistentSet<E> add(E e) {
      return new PersistentSetFromMap<>(map.put(e, Boolean.TRUE));
   }

   @Override
   public PersistentSet<E> remove(Object o) {
      return new PersistentSetFromMap<>(map.remove(o));
   }

   @Override
   public PersistentSet<E> removeAll(Iterable<?> items) {
      return new PersistentSetFromMap<>(map.removeAll(items));
   }

   @Override
   public PersistentSet<E> retainAll(Iterable<?> items) {
      return new PersistentSetFromMap<>(map.retainAll(items));
   }

   @Override
   public PersistentSet<E> addAll(Iterable<? extends E> items) {
      PersistentSet<E> ret = this;
      for (E e : items) {
         ret = ret.add(e);
      }
      return ret;
   }

   @Override
   public PersistentSet<E> clear() {
      return new PersistentSetFromMap<>(map.clear());
   }
}
