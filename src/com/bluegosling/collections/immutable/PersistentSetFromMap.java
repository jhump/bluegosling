package com.bluegosling.collections.immutable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.function.Predicate;

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
class PersistentSetFromMap<E> extends AbstractSet<E> implements PersistentSet<E> {
   
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
   public PersistentSet<E> withoutAny(Object o) {
      return new PersistentSetFromMap<>(map.withoutKey(o));
   }

   @Override
   public PersistentSet<E> with(E e) {
      return new PersistentSetFromMap<>(map.with(e, Boolean.TRUE));
   }

   @Override
   public PersistentSet<E> without(Object o) {
      return new PersistentSetFromMap<>(map.withoutKey(o));
   }

   @Override
   public PersistentSet<E> withoutAny(Iterable<?> items) {
      return new PersistentSetFromMap<>(map.withoutKeys(items));
   }
   
   @Override
   public PersistentSet<E> withoutAny(Predicate<? super E> predicate) {
      return new PersistentSetFromMap<>(map.withoutKeys(predicate));
   }

   @Override
   public PersistentSet<E> withOnly(Iterable<?> items) {
      return new PersistentSetFromMap<>(map.withOnlyKeys(items));
   }

   @Override
   public PersistentSet<E> withAll(Iterable<? extends E> items) {
      PersistentSet<E> ret = this;
      for (E e : items) {
         ret = ret.with(e);
      }
      return ret;
   }

   @Override
   public PersistentSet<E> removeAll() {
      return new PersistentSetFromMap<>(map.removeAll());
   }
}
