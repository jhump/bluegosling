package com.apriori.collections;

import java.util.Iterator;

// TODO: javadoc
// TODO: tests
public abstract class AbstractImmutableMap<K, V> implements ImmutableMap<K, V> {

   @Override
   public boolean isEmpty() {
      return size() == 0;
   }

   @Override
   public boolean containsKey(Object o) {
      if (o == null) {
         for (Entry<K, V> entry : this) {
            if (entry.key() == null) {
               return true;
            }
         }
      } else {
         for (Entry<K, V> entry : this) {
            if (o.equals(entry.key())) {
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public boolean containsAllKeys(Iterable<?> keys) {
      for (Object o : keys) {
         if (!containsKey(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean containsAnyKey(Iterable<?> keys) {
      for (Object o : keys) {
         if (containsKey(o)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean containsValue(Object o) {
      if (o == null) {
         for (Entry<K, V> entry : this) {
            if (entry.value() == null) {
               return true;
            }
         }
      } else {
         for (Entry<K, V> entry : this) {
            if (o.equals(entry.value())) {
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public boolean containsAllValues(Iterable<?> values) {
      for (Object o : values) {
         if (!containsValue(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean containsAnyValue(Iterable<?> values) {
      for (Object o : values) {
         if (containsValue(o)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public V get(Object key) {
      if (key == null) {
         for (Entry<K, V> entry : this) {
            if (entry.key() == null) {
               return entry.value();
            }
         }
      } else {
         for (Entry<K, V> entry : this) {
            if (key.equals(entry.key())) {
               return entry.value();
            }
         }
      }
      return null;
   }

   @Override
   public ImmutableSet<K> keySet() {
      return new KeySet();
   }

   @Override
   public ImmutableCollection<V> values() {
      return new ValueCollection();
   }

   @Override
   public ImmutableSet<Entry<K, V>> entrySet() {
      return new EntrySet();
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

   private class KeySet extends AbstractImmutableSet<K> {
      KeySet() {
      }
      
      @Override
      public int size() {
         return AbstractImmutableMap.this.size();
      }

      @Override
      public boolean contains(Object o) {
         return containsKey(o);
      }
      
      @Override
      public Iterator<K> iterator() {
         return new TransformingIterator<Entry<K, V>, K>(AbstractImmutableMap.this.iterator(),
               (entry) -> entry.key());
      }
   }

   private class EntrySet extends AbstractImmutableSet<Entry<K, V>> {
      EntrySet() {
      }
      
      @Override
      public int size() {
         return AbstractImmutableMap.this.size();
      }

      @Override
      public boolean contains(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         }
         Entry<?, ?> other = (Entry<?, ?>) o;
         Object otherKey = other.key();
         V value = get(otherKey);
         if (value == null) {
            return other.value() == null && containsKey(otherKey);
         } else {
            return value.equals(other.value());
         }
      }
      
      @Override
      public Iterator<Entry<K, V>> iterator() {
         return AbstractImmutableMap.this.iterator();
      }
   }
   
   private class ValueCollection extends AbstractImmutableCollection<V> {
      ValueCollection() {
      }
      
      @Override
      public int size() {
         return AbstractImmutableMap.this.size();
      }

      @Override
      public boolean contains(Object o) {
         return containsValue(o);
      }
      
      @Override
      public Iterator<V> iterator() {
         return new TransformingIterator<Entry<K, V>, V>(AbstractImmutableMap.this.iterator(),
               (entry) -> entry.value());
      }
   }
   
   public static class SimpleEntry<K, V> implements Entry<K, V> {
      private final K key;
      private final V value;
      
      SimpleEntry(K key, V value) {
         this.key = key;
         this.value = value;
      }

      @Override
      public K key() {
         return key;
      }

      @Override
      public V value() {
         return value;
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
}
