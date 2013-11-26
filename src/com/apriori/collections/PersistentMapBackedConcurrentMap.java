package com.apriori.collections;

import com.apriori.util.Function;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

//TODO: javadoc
//TODO: tests
public class PersistentMapBackedConcurrentMap<K, V> extends AbstractMap<K, V>
      implements ConcurrentMap<K, V> {
   
   private final AtomicReference<PersistentMap<K, V>> underlying;
   
   public PersistentMapBackedConcurrentMap(PersistentMap<K, V> underlying) {
      this.underlying = new AtomicReference<PersistentMap<K, V>>(underlying);
   }

   @Override
   public boolean containsKey(Object o) {
      return underlying.get().containsKey(o);
   }
   
   @Override
   public boolean containsValue(Object o) {
      return underlying.get().containsValue(o);
   }
   
   @Override
   public V get(Object key) {
      return underlying.get().get(key);
   }
   
   @Override
   public V put(K key, V value) {
      while (true) {
         PersistentMap<K, V> original = underlying.get();
         PersistentMap<K, V> modified = original.put(key, value);
         if (original == modified) {
            return value;
         }
         if (underlying.compareAndSet(original, modified)) {
            return original.get(key);
         }
      }
   }

   @Override
   public V putIfAbsent(K key, V value) {
      while (true) {
         PersistentMap<K, V> original = underlying.get();
         V prior = original.get(key);
         if (prior != null || original.containsKey(key)) {
            return prior;
         }
         PersistentMap<K, V> modified = original.put(key, value);
         if (underlying.compareAndSet(original, modified)) {
            return null;
         }
      }
   }

   @Override
   public V remove(Object key) {
      while (true) {
         PersistentMap<K, V> original = underlying.get();
         PersistentMap<K, V> modified = original.remove(key);
         if (original == modified) {
            return null;
         }
         if (underlying.compareAndSet(original, modified)) {
            return original.get(key);
         }
      }
   }

   @Override
   public boolean remove(Object key, Object value) {
      while (true) {
         PersistentMap<K, V> original = underlying.get();
         V actual = original.get(key);
         if (actual != null || original.containsKey(key)) {
            if (value == null ? actual != null : !value.equals(actual)) {
               return false;
            }
         }
         PersistentMap<K, V> modified = original.remove(key);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      while (true) {
         PersistentMap<K, V> original = underlying.get();
         V actual = original.get(key);
         if (actual != null || original.containsKey(key)) {
            if (oldValue == null ? actual != null : !oldValue.equals(actual)) {
               return false;
            }
         }
         PersistentMap<K, V> modified = original.put(key, newValue);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public V replace(K key, V value) {
      while (true) {
         PersistentMap<K, V> original = underlying.get();
         V prior = original.get(key);
         if (prior == null && !original.containsKey(key)) {
            return null;
         }
         PersistentMap<K, V> modified = original.put(key, value);
         if (original == modified) {
            return null;
         }
         if (underlying.compareAndSet(original, modified)) {
            return prior;
         }
      }
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new EntrySet();
   }

   @Override
   public Set<K> keySet() {
      return new TransformingSet<Entry<K, V>, K>(entrySet(), new Function<Entry<K, V>, K>() {
         @Override
         public K apply(Entry<K, V> input) {
            return input.getKey();
         }
      });
   }
   
   @Override
   public Collection<V> values() {
      return new TransformingCollection<Entry<K, V>, V>(entrySet(), new Function<Entry<K, V>, V>() {
         @Override
         public V apply(Entry<K, V> input) {
            return input.getValue();
         }
      });
   }
   
   Entry<K, V> asMutableEntry(final ImmutableMap.Entry<K, V> entry) {
      return new Entry<K, V>() {
         V value = entry.value();
         
         @Override
         public K getKey() {
            return entry.key();
         }

         @Override
         public V getValue() {
            return value;
         }

         @Override
         public V setValue(V value) {
            replace(entry.key(), value);
            V ret = this.value;
            this.value = value;
            return ret;
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
      };
   }
   
   private class EntrySet extends AbstractSet<Entry<K, V>> {
      EntrySet() {
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         @SuppressWarnings("synthetic-access")
         final Iterator<ImmutableMap.Entry<K, V>> iter = underlying.get().iterator();
         return new Iterator<Entry<K, V>>() {
            private ImmutableMap.Entry<K, V> last = null;
            
            @Override
            public boolean hasNext() {
               return iter.hasNext();
            }

            @Override
            public Entry<K, V> next() {
               last = iter.next();
               return asMutableEntry(last);
            }

            @Override
            public void remove() {
               if (last == null) {
                  throw new IllegalStateException();
               }
               PersistentMapBackedConcurrentMap.this.remove(last.key(), last.value());
               last = null;
            }
            
         };
      }

      @Override
      public int size() {
         return PersistentMapBackedConcurrentMap.this.size();
      }
   }
}
