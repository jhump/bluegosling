package com.bluegosling.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A concurrent map that is backed by a persistent map. Modifications are made by atomically
 * swapping one map for the modified one. Since persistent maps are immutable, the map itself isn't
 * updated, only the reference to it. Modified maps are generally computed via limited copy-on-write
 * (path-copying for tree-based map structures). 
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: javadoc
//TODO: tests
public class PersistentMapBackedConcurrentMap<K, V> extends AbstractMap<K, V>
      implements ConcurrentMap<K, V> {
   
   private final AtomicReference<PersistentMap<K, V>> underlying;
   
   /**
    * Constructs a new concurrent map whose initial state is the given persistent map. The given
    * persistent map is also used to compute new maps during modifications.
    *
    * @param underlying the persistent map that initial backs the concurrent map
    */
   public PersistentMapBackedConcurrentMap(PersistentMap<K, V> underlying) {
      this.underlying = new AtomicReference<PersistentMap<K, V>>(underlying);
   }

   @Override
   public int size() {
      return underlying.get().size();
   }
   
   @Override
   public boolean isEmpty() {
      return underlying.get().isEmpty();
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
         boolean match = value != null ? value.equals(actual)
               : (actual == null && original.containsKey(key));
         if (!match) {
            return false;
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
         boolean match = oldValue != null ? oldValue.equals(actual)
               : (actual == null && original.containsKey(key));
         if (!match) {
            return false;
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
            assert Objects.equals(value, prior);
            return value;
         }
         if (underlying.compareAndSet(original, modified)) {
            return prior;
         }
      }
   }

   @Override
   public void clear() {
      underlying.set(underlying.get().clear());
   }
   
   @Override
   public Set<Entry<K, V>> entrySet() {
      return new EntrySet();
   }

   @Override
   public Set<K> keySet() {
      return new TransformingSet<Entry<K, V>, K>(entrySet(), (entry) -> entry.getKey());
   }
   
   @Override
   public Collection<V> values() {
      return new TransformingCollection<Entry<K, V>, V>(entrySet(), (entry) -> entry.getValue());
   }
   
   @Override
   public String toString() {
      return underlying.get().toString();
   }

   @Override
   public boolean equals(Object o) {
      return MapUtils.equals(this, o);
   }


   @Override
   public int hashCode() {
      return underlying.get().hashCode();
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
            // TODO: throw if replace fails?
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
   
   // TODO: KeySet, to provide more efficient Set.contains and Set.remove
   
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
      public boolean contains(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         }
         Entry<?, ?> e = (Entry<?, ?>) o;
         Object v = PersistentMapBackedConcurrentMap.this.get(e.getKey());
         return v != null
               ? v.equals(e.getValue())
               : PersistentMapBackedConcurrentMap.this.containsKey(e.getKey())
                     && e.getValue() == null;
      }
      
      @Override
      public boolean remove(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         }
         Entry<?, ?> e = (Entry<?, ?>) o;
         return PersistentMapBackedConcurrentMap.this.remove(e.getKey(), e.getValue());
      }
      
      @Override
      public int size() {
         return PersistentMapBackedConcurrentMap.this.size();
      }
      
      @Override
      public boolean isEmpty() {
         return PersistentMapBackedConcurrentMap.this.isEmpty();
      }
      
      @Override
      public void clear() {
         PersistentMapBackedConcurrentMap.this.clear();
      }
   }
}
