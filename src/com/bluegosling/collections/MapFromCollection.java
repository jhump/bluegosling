package com.bluegosling.collections;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// TODO: javadoc
// TODO: tests
public class MapFromCollection<T> implements Map<T, T> {

   private final Collection<T> coll;
   private final Set<T> keySet;
   
   MapFromCollection(Collection<T> coll) {
      this.coll = coll;
      this.keySet = SetFromCollection.make(coll);
   }
   
   @Override
   public int size() {
      return keySet.size();
   }

   @Override
   public boolean isEmpty() {
      return coll.isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return coll.contains(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return coll.contains(value);
   }

   @Override
   public T get(Object key) {
      for (Iterator<T> iter = coll.iterator(); iter.hasNext();) {
         T t = iter.next();
         if (Objects.equals(t, key)) {
            return t;
         }
      }
      return null;
   }

   @Override
   public T put(T key, T value) {
      if (!Objects.equals(key, value)) {
         throw new IllegalArgumentException("mapped value must be equal to corresponding key");
      }
      return keySet.add(key) ? null : value;
   }

   @Override
   public T remove(Object key) {
      for (Iterator<T> iter = coll.iterator(); iter.hasNext();) {
         T t = iter.next();
         if (Objects.equals(t, key)) {
            iter.remove();
            return t;
         }
      }
      return null;
   }

   @Override
   public void putAll(Map<? extends T, ? extends T> m) {
      for (Entry<? extends T, ? extends T> e : m.entrySet()) {
         put(e.getKey(), e.getValue());
      }
   }

   @Override
   public void clear() {
      coll.clear();
   }

   @Override
   public Set<T> keySet() {
      return keySet;
   }

   @Override
   public Collection<T> values() {
      return keySet;
   }

   @Override
   public Set<Entry<T, T>> entrySet() {
      return new TransformingSet<>(keySet, t -> new AbstractMap.SimpleImmutableEntry<>(t, t));
   }
   
   @Override
   public boolean equals(Object o) {
      return MapUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return MapUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return MapUtils.toString(this);
   }
}
