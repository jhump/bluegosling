package com.bluegosling.collections;

import com.bluegosling.collections.views.TransformingSet;

import java.util.AbstractCollection;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: doc
// TODO: split into several top-level interfaces/classes?
// TODO: tests
final class Multimaps {
   
   static final Object EMPTY_ARRAY[] = new Object[0];
   
   public interface Multimap<K, V> {
      int size();
      default boolean isEmpty() {
         return size() == 0;
      }
      Map<K, ? extends Collection<V>> asMap();
      Collection<V> read(Object key);
      Collection<V> get(K key);
      boolean put(K key, V value);
      boolean putAll(K key, Iterable<? extends V> values);
      
      default boolean putAll(Map<? extends K, ? extends Iterable<? extends V>> mappings) {
         boolean ret = false;
         for (Entry<? extends K, ? extends Iterable<? extends V>> entry : mappings.entrySet()) {
            if (putAll(entry.getKey(), entry.getValue())) {
               ret = true;
            }
         }
         return ret;
      }
      
      default boolean putAll(Multimap<? extends K, ? extends V> mappings) {
         return putAll(mappings.asMap());
      }
      
      boolean containsKey(Object key);
      boolean containsValue(Object value);
      Collection<V> removeAll(Object key);
      void clear();
      boolean remove(Object key, Object value);
      Collection<V> replaceAll(K key, Iterable<? extends V> values);
      Set<K> keySet();
      Collection<Entry<K, V>> entries();
      Collection<V> values();
      @Override boolean equals(Object o);
      @Override int hashCode();
   }
   
   static abstract class MapBackedMultimap<K, V, C extends Collection<V>, M extends Map<K, C>>
       implements Multimap<K, V> {
       
      final M map;
      final Supplier<C> valueMaker;
      final Function<Iterable<? extends V>, C> valueConverter; 
      int size;
      transient Map<K, Collection<V>> mapView;
      transient Set<K> keysView;
      transient Collection<V> valuesView;
      transient Collection<Entry<K, V>> entriesView;

      MapBackedMultimap(M map, Supplier<C> valueMaker) {
         this(map, valueMaker, iter -> {
            C coll = valueMaker.get();
            Iterables.addTo(iter, coll);
            return coll;
         });
      }
      
      MapBackedMultimap(M map, Supplier<C> valueMaker,
            Function<Iterable<? extends V>, C> valueConverter) {
         this.map = map;
         this.valueMaker = valueMaker;
         this.valueConverter = valueConverter;
      }
      
      protected void adjustSize(int adjustBy) {
         size += adjustBy;
      }
       
      @Override
      public int size() {
         return size;
      }
      
      @Override
      public Map<K, Collection<V>> asMap() {
         if (mapView == null) {
            mapView = new MapView();
         }
         return mapView;
      }
      
      @Override
      @SuppressWarnings("unchecked") // cast is safe because collection is immutable
      public Collection<V> read(Object key) {
         return Collections.unmodifiableCollection(get((K) key));
      }
      
      @Override
      public Collection<V> get(K key) {
         return new ValuesForKeyView(key);
      }
      
      @Override
      public boolean put(K key, V value) {
         boolean ret[] = new boolean[0];
         map.compute(key, (k, existing) -> {
            if (existing == null) {
               C newValue = valueMaker.get();
               newValue.add(value);
               ret[0] = true;
               return newValue;
            } else {
               ret[0] = existing.add(value);
               return existing;
            }
         });
         if (ret[0]) {
            adjustSize(1);
            return true;
         }
         return false;
      }
      
      @Override
      public boolean putAll(K key, Iterable<? extends V> c) {
         int added[] = new int[0];
         map.compute(key, (k, existing) -> {
            if (existing == null) {
               C newValue = valueConverter.apply(c);
               added[0] = newValue.size();
               return newValue;
            } else {
               int before = existing.size();
               Iterables.addTo(c, existing);
               int after = existing.size();
               added[0] = after - before;
               return existing;
            }
         });
         adjustSize(added[0]);
         return added[0] != 0;
      }
      
      @Override
      public boolean containsKey(Object key) {
         return map.containsKey(key);
      }
      
      @Override
      public boolean containsValue(Object value) {
         return map.values().stream().anyMatch(c -> c.contains(value));
      }
      
      @Override
      public C removeAll(Object key) {
         C ret = map.remove(key);
         adjustSize(-ret.size());
         return ret;
      }
      
      @Override
      public void clear() {
         map.clear();
         size = 0;
      }
      
      @Override
      public boolean remove(Object key, Object value) {
         boolean ret[] = new boolean[0];
         @SuppressWarnings("unchecked") // if no mapping exists, we don't add one, so if the given
                                        // key is the wrong type, it's okay because we won't add it
                                        // and thus won't pollute the heap.
         K kkey = (K) key;
         map.compute(kkey, (k, existing) -> {
            if (existing == null) {
               ret[0] = false;
               return null;
            }
            ret[0] = existing.remove(value);
            return existing.isEmpty() ? null : existing;
         });
         if (ret[0]) {
            adjustSize(-1);
            return true;
         }
         return false;
      }
      
      @Override
      public C replaceAll(K key, Iterable<? extends V> values) {
         C newColl = valueConverter.apply(values);
         C ret;
         if (newColl.isEmpty()) {
            ret = map.put(key, newColl);
         } else {
            ret = map.remove(key);
         }
         adjustSize(newColl.size() - (ret != null ? ret.size() : 0));
         return ret;
      }
      
      @Override
      public Set<K> keySet() {
         if (keysView == null) {
            keysView = new KeysView();
         }
         return keysView;
      }
      
      @Override
      public Collection<Entry<K, V>> entries() {
         if (entriesView == null) {
            entriesView = new EntriesView();
         }
         return entriesView;
      }
      
      @Override
      public Collection<V> values() {
         if (valuesView == null) {
            valuesView = new ValuesView();
         }
         return valuesView;
      }

      protected class MapView implements Map<K, Collection<V>> {
         @Override
         public int size() {
            return map.size();
         }

         @Override
         public boolean isEmpty() {
            return map.isEmpty();
         }

         @Override
         public boolean containsKey(Object key) {
            return map.containsKey(key);
         }

         @Override
         public boolean containsValue(Object value) {
            return map.containsValue(value);
         }

         @Override
         public Collection<V> get(Object key) {
            C coll = map.get(key);
            return coll != null ? new ValuesForExistingKeyView(key, coll) : null;
         }

         @Override
         public Collection<V> put(K key, Collection<V> values) {
            return MapBackedMultimap.this.replaceAll(key, values);
         }

         @Override
         public Collection<V> remove(Object key) {
            return MapBackedMultimap.this.removeAll(key);
         }

         @Override
         public void putAll(Map<? extends K, ? extends Collection<V>> m) {
            for (Entry<? extends K, ? extends Collection<V>> entry : m.entrySet()) {
               put(entry.getKey(), entry.getValue());
            }
         }

         @Override
         public void clear() {
            MapBackedMultimap.this.clear();
         }

         @Override
         public Set<K> keySet() {
            return MapBackedMultimap.this.keySet();
         }

         @Override
         public Collection<Collection<V>> values() {
            return new AbstractCollection<Collection<V>>() {
               @Override
               public Iterator<Collection<V>> iterator() {
                  return new Iter<Collection<V>>((k, c) -> MapBackedMultimap.this.get(k));
               }

               @Override
               public int size() {
                  return map.size();
               }
            };
         }

         @Override
         public Set<Entry<K, Collection<V>>> entrySet() {
            return new TransformingSet<K, Entry<K, Collection<V>>>(keySet(),
                  k -> new Entry<K, Collection<V>>() {
                     @Override
                     public K getKey() {
                        return k;
                     }

                     @Override
                     public Collection<V> getValue() {
                        return MapBackedMultimap.this.get(k);
                     }

                     @Override
                     public Collection<V> setValue(Collection<V> value) {
                        return MapBackedMultimap.this.replaceAll(k, value);
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
                  });
         }
      }

      protected class KeysView implements Set<K> {
         @Override
         public int size() {
            return map.size();
         }

         @Override
         public boolean isEmpty() {
            return map.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return map.containsKey(o);
         }

         @Override
         public Iterator<K> iterator() {
            return new Iter<K>((k, v) -> k);
         }

         @Override
         public Object[] toArray() {
            return map.keySet().toArray();
         }

         @Override
         public <T> T[] toArray(T[] a) {
            return map.keySet().toArray(a);
         }

         @Override
         public boolean add(K e) {
            throw new UnsupportedOperationException();
         }

         @Override
         public boolean remove(Object o) {
            return MapBackedMultimap.this.removeAll(o) != null;
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return map.keySet().containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends K> c) {
            throw new UnsupportedOperationException();
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            boolean ret = false;
            for (Iterator<K> iter = iterator(); iter.hasNext();) {
               K k = iter.next();
               if (!c.contains(k)) {
                  iter.remove();
                  ret = true;
               }
            }
            return ret;
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            boolean ret = false;
            if (c instanceof Set && c.size() > map.size()) {
               for (Iterator<K> iter = iterator(); iter.hasNext();) {
                  K k = iter.next();
                  if (c.contains(k)) {
                     iter.remove();
                     ret = true;
                  }
               }
            } else {
               for (Object o : c) {
                  ret |= remove(o);
               }
            }
            return ret;
         }

         @Override
         public void clear() {
            MapBackedMultimap.this.clear();
         }
      }

      protected class EntriesView extends AbstractCollection<Entry<K, V>> {
         @Override
         public Iterator<Entry<K, V>> iterator() {
            return new FlattenedIter<Entry<K, V>>((k, v) -> new SimpleImmutableEntry<>(k, v));
         }

         @Override
         public boolean contains(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?, ?> entry = (Entry<?, ?>) o;
            C coll = map.get(entry.getKey());
            return coll != null && coll.contains(entry.getValue());
         }

         @Override
         public boolean remove(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?, ?> entry = (Entry<?, ?>) o;
            return MapBackedMultimap.this.remove(entry.getKey(), entry.getValue());
         }

         @Override
         public int size() {
            return MapBackedMultimap.this.size();
         }
         
         @Override
         public boolean isEmpty() {
            return map.isEmpty();
         }
      }
      
      protected class ValuesView extends AbstractCollection<V> {
         @Override
         public Iterator<V> iterator() {
            return new FlattenedIter<V>((k, v) -> v);
         }
         
         @Override
         public boolean contains(Object o) {
            return MapBackedMultimap.this.containsValue(o);
         }

         @Override
         public int size() {
            return MapBackedMultimap.this.size();
         }
         
         @Override
         public boolean isEmpty() {
            return map.isEmpty();
         }
      }
      
      protected class ValuesForKeyView implements Collection<V> {
         
         final K key;
         
         ValuesForKeyView(K key) {
            this.key = key;
         }

         @Override
         public Iterator<V> iterator() {
            C coll = map.get(key);
            if (coll == null) {
               return Collections.emptyIterator();
            }
            Iterator<V> iter = coll.iterator();
            return new Iterator<V>() {
               @Override
               public boolean hasNext() {
                  return iter.hasNext();
               }

               @Override
               public V next() {
                  return iter.next();
               }
               
               @Override
               public void remove() {
                  iter.remove();
                  if (map.get(key) != coll) {
                     // value has been replaced, so removing from this iterator no
                     // longer has any bearing on multimap
                     return;
                  }
                  adjustSize(-1);
                  if (coll.isEmpty()) {
                     assert !iter.hasNext();
                     map.remove(key);
                  }
               }
            };
         }

         @Override
         public int size() {
            C coll = map.get(key);
            return coll != null ? coll.size() : 0;
         }
         
         @Override
         public boolean isEmpty() {
            C coll = map.get(key);
            return coll == null || coll.isEmpty();
         }
         
         @Override
         public boolean contains(Object o) {
            C coll = map.get(key);
            return coll != null && coll.contains(o);
         }
         
         @Override
         public boolean remove(Object o) {
            return MapBackedMultimap.this.remove(key, o);
         }
         
         @Override
         public boolean add(V v) {
            return MapBackedMultimap.this.put(key, v);
         }

         @Override
         public Object[] toArray() {
            C coll = map.get(key);
            return coll != null ? coll.toArray() : EMPTY_ARRAY;
         }

         @Override
         public <T> T[] toArray(T[] a) {
            C coll = map.get(key);
            if (coll != null) {
               return coll.toArray(a);
            }
            if (a.length > 0) {
               a[0] = null;
            }
            return a;
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            C coll = map.get(key);
            return coll != null ? coll.containsAll(c) : false;
         }

         @Override
         public boolean addAll(Collection<? extends V> c) {
            return MapBackedMultimap.this.putAll(key, c);
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            boolean ret[] = new boolean[1];
            int sizeAdjust[] = new int[1];
            map.compute(key, (k, existing) -> {
               if (existing == null) {
                  ret[0] = false;
                  return null;
               }
               int before = existing.size();
               ret[0] = existing.removeAll(c);
               int after = existing.size();
               sizeAdjust[0] = after - before;
               return existing.isEmpty() ? null : existing;
            });
            assert ret[0] == (sizeAdjust[0] < 0);
            if (ret[0]) {
               adjustSize(sizeAdjust[0]);
            }
            return ret[0];
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            boolean ret[] = new boolean[1];
            int sizeAdjust[] = new int[1];
            map.compute(key, (k, existing) -> {
               if (existing == null) {
                  ret[0] = false;
                  return null;
               }
               int before = existing.size();
               ret[0] = existing.retainAll(c);
               int after = existing.size();
               sizeAdjust[0] = after - before;
               return existing.isEmpty() ? null : existing;
            });
            assert ret[0] == (sizeAdjust[0] < 0);
            if (ret[0]) {
               adjustSize(sizeAdjust[0]);
            }
            return ret[0];
         }

         @Override
         public void clear() {
            MapBackedMultimap.this.removeAll(key);
         }
      }

      protected class ValuesForExistingKeyView implements Collection<V> {
         final Object key;
         final Collection<V> coll;

         ValuesForExistingKeyView(Object key, Collection<V> coll) {
            this.key = key;
            this.coll = coll;
         }

         @Override
         public Iterator<V> iterator() {
            Iterator<V> iter = coll.iterator();
            if (map.get(key) != coll) {
               return iter;
            }
            return new Iterator<V>() {
               @Override
               public boolean hasNext() {
                  return iter.hasNext();
               }

               @Override
               public V next() {
                  return iter.next();
               }
               
               @Override
               public void remove() {
                  iter.remove();
                  if (map.get(key) != coll) {
                     // value has been replaced, so removing from this iterator no
                     // longer has any bearing on multimap
                     return;
                  }
                  adjustSize(-1);
                  if (coll.isEmpty()) {
                     assert !iter.hasNext();
                     map.remove(key);
                  }
               }
            };
         }

         @Override
         public int size() {
            return coll.size();
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
         public boolean remove(Object o) {
            return coll.remove(o);
         }
         
         @Override
         public boolean add(V v) {
            boolean ret = coll.add(v);
            if (ret && map.get(key) == coll) {
               adjustSize(1);
            }
            return ret;
         }

         @Override
         public Object[] toArray() {
            return coll.toArray();
         }

         @Override
         public <T> T[] toArray(T[] a) {
            return coll.toArray(a);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return coll.containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends V> c) {
            boolean ret = false;
            for (V v : c) {
               ret |= add(v);
            }
            return ret;
         }

         @Override
         @SuppressWarnings("unchecked") // when just removing (not adding) cast will be safe
         public boolean removeAll(Collection<?> c) {
            if (map.get(key) == coll) {
               return MapBackedMultimap.this.get((K) key).removeAll(c);
            } else {
               return coll.removeAll(c);
            }
         }

         @Override
         @SuppressWarnings("unchecked") // when just removing (not adding) cast will be safe
         public boolean retainAll(Collection<?> c) {
            if (map.get(key) == coll) {
               return MapBackedMultimap.this.get((K) key).retainAll(c);
            } else {
               return coll.removeAll(c);
            }
         }

         @Override
         public void clear() {
            map.remove(key, coll);
            coll.clear();
         }
      }

      /**
       * An iterator over map entries that performs proper size accounting when an element is
       * removed.
       *
       * @param <E> the type of value returned by the iterator, computed from a key and its
       *       corresponding collection of values
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class Iter<E> implements Iterator<E> {
         final BiFunction<K, C, E> fn;
         final Iterator<Entry<K, C>> entriesIterator;
         Entry<K, C> lastFetched;
         
         Iter(BiFunction<K, C, E> fn) {
            this.fn = fn;
            this.entriesIterator = map.entrySet().iterator();
         }

         @Override
         public boolean hasNext() {
            return entriesIterator.hasNext();
         }

         @Override
         public E next() {
            lastFetched = entriesIterator.next();
            return fn.apply(lastFetched.getKey(), lastFetched.getValue());
         }
         
         @Override
         public void remove() {
            if (lastFetched == null) {
               throw new IllegalStateException();
            }
            C coll = lastFetched.getValue();
            entriesIterator.remove();
            adjustSize(-coll.size());
            lastFetched = null;
         }
      }

      /**
       * An iterator over multimap entries, where the mapped collections are flattened into multiple
       * emitted entries. This iterator performs proper size accounting when elements are removed.
       *
       * @param <E> the type of value returned by the iterator, computed from a key and a value
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class FlattenedIter<E> implements Iterator<E> {
         final BiFunction<K, V, E> fn;
         final Iterator<Entry<K, C>> entriesIterator;
         K currentKey;
         C currentColl;
         Iterator<V> currentIterator;
         
         FlattenedIter(BiFunction<K, V, E> fn) {
            this.fn = fn;
            this.entriesIterator = map.entrySet().iterator();
         }
         
         private void findNext() {
            if (currentIterator != null && currentIterator.hasNext()) {
               return;
            }
            while (entriesIterator.hasNext()) {
               Entry<K, C> entry = entriesIterator.next();
               C coll = entry.getValue();
               Iterator<V> iter = coll.iterator();
               if (iter.hasNext()) {
                  currentKey = entry.getKey();
                  currentColl = coll;
                  currentIterator = iter;
                  return;
               }
            }
            currentKey = null;
            currentColl = null;
            currentIterator = null;
         }

         @Override
         public boolean hasNext() {
            return entriesIterator.hasNext() || currentIterator.hasNext();
         }

         @Override
         public E next() {
            findNext();
            if (currentIterator == null) {
               throw new NoSuchElementException();
            }
            V v = currentIterator.next();
            return fn.apply(currentKey, v);
         }
         
         @Override
         public void remove() {
            if (currentIterator == null) {
               throw new IllegalStateException();
            }
            currentIterator.remove();
            if (map.get(currentKey) != currentColl) {
               // value has been replaced, so removing from this iterator no
               // longer has any bearing on multimap
               return;
            }
            adjustSize(-1);
            // clean-up if we deleted last item in the collection
            if (currentColl.isEmpty()) {
               assert !currentIterator.hasNext();
               entriesIterator.remove();
            }
         }
      }
   }
}
