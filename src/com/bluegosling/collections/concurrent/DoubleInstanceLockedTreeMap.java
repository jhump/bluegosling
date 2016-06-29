package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.MapUtils;
import com.bluegosling.collections.views.DescendingMap;
import com.bluegosling.collections.views.DescendingSet;
import com.bluegosling.concurrent.locks.DoubleInstanceLock;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A concurrent navigable map backed by a {@link TreeMap} and made thread-safe via a
 * {@link DoubleInstanceLock}.
 * 
 * <p>This map, its collection views, and iterators support all operations. Iteration will never
 * throw {@link ConcurrentModificationException}. One thing that can throw this, however, is trying
 * to set an entry value via {@link Entry#setValue(Object)} after the entry has been removed.
 * 
 * <p>Iterators are strongly consistent and reflect the state of the map or collection view at the
 * time the iterator was created.
 * 
 * <p>Batch operations, like {@link #putAll(Map)} and {@link #replaceAll(BiFunction)}, are atomic.
 * Queries on the map will never reflect partial results of such operations.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: serialization? cloning?
public class DoubleInstanceLockedTreeMap<K, V> implements ConcurrentMap<K, V>, NavigableMap<K, V> {

   final DoubleInstanceLock<TreeMap<K, V>> lock;
   
   /**
    * Creates a new, empty map.
    */
   public DoubleInstanceLockedTreeMap() {
      this.lock = DoubleInstanceLock.newLock(new TreeMap<>());
   }

   /**
    * Creates a new map with the given mappings.
    *
    * @param map mappings used to initialize the new map
    */
   public DoubleInstanceLockedTreeMap(Map<? extends K, ? extends V> map) {
      this.lock = DoubleInstanceLock.newLock(new TreeMap<>(map));
   }

   @Override
   public int size() {
      return lock.read(Map::size);
   }

   @Override
   public boolean isEmpty() {
      return lock.read(Map::isEmpty);
   }

   @Override
   public boolean containsKey(Object key) {
      return lock.read(m -> m.containsKey(key));
   }

   @Override
   public boolean containsValue(Object value) {
      return lock.read(m -> m.containsValue(value));
   }

   @Override
   public V get(Object key) {
      return lock.read(m -> m.get(key));
   }

   @Override
   public V put(K key, V value) {
      return lock.write(m -> m.put(key, value));
   }

   @Override
   public V remove(Object key) {
      return lock.write(m -> m.remove(key));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      lock.writeWith(m -> m.putAll(map));
   }

   @Override
   public void clear() {
      lock.writeWith(Map::clear);
   }

   @Override
   public Set<K> keySet() {
      return navigableKeySet();
   }

   @Override
   public Collection<V> values() {
      return new Values(NavigableMap::values, NavigableMap::entrySet);
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new Entries(NavigableMap::entrySet);
   }

   @Override
   public Comparator<? super K> comparator() {
      return lock.read(NavigableMap::comparator);
   }

   @Override
   public K firstKey() {
      return lock.read(NavigableMap::firstKey);
   }

   @Override
   public K lastKey() {
      return lock.read(NavigableMap::lastKey);
   }

   @Override
   public Entry<K, V> lowerEntry(K key) {
      return wrap(lock.read(m -> m.lowerEntry(key)));
   }

   @Override
   public K lowerKey(K key) {
      return lock.read(m -> m.lowerKey(key));
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
      return wrap(lock.read(m -> m.floorEntry(key)));
   }

   @Override
   public K floorKey(K key) {
      return lock.read(m -> m.floorKey(key));
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
      return wrap(lock.read(m -> m.ceilingEntry(key)));
   }

   @Override
   public K ceilingKey(K key) {
      return lock.read(m -> m.ceilingKey(key));
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
      return wrap(lock.read(m -> m.higherEntry(key)));
   }

   @Override
   public K higherKey(K key) {
      return lock.read(m -> m.higherKey(key));
   }

   @Override
   public Entry<K, V> firstEntry() {
      return wrap(lock.read(NavigableMap::firstEntry));
   }

   @Override
   public Entry<K, V> lastEntry() {
      return wrap(lock.read(NavigableMap::lastEntry));
   }

   @Override
   public Entry<K, V> pollFirstEntry() {
      return wrap(lock.read(NavigableMap::pollFirstEntry));
   }

   @Override
   public Entry<K, V> pollLastEntry() {
      return wrap(lock.read(NavigableMap::pollLastEntry));
   }

   @Override
   public NavigableMap<K, V> descendingMap() {
      return new DescendingMap<>(this);
   }

   @Override
   public NavigableSet<K> navigableKeySet() {
      return new Keys(NavigableMap::navigableKeySet);
   }

   @Override
   public NavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }

   @Override
   public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
         boolean toInclusive) {
      // get submap now, even though we don't need it, to let underlying map verify arguments
      lock.readWith(m -> m.subMap(fromKey, fromInclusive, toKey, toInclusive));
      return new SubMap(m -> m.subMap(fromKey, fromInclusive, toKey, toInclusive));
   }

   @Override
   public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      // get submap now, even though we don't need it, to let underlying map verify arguments
      lock.readWith(m -> m.headMap(toKey, inclusive));
      return new SubMap(m -> m.headMap(toKey, inclusive));
   }

   @Override
   public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      // get submap now, even though we don't need it, to let underlying map verify arguments
      lock.readWith(m -> m.tailMap(fromKey, inclusive));
      return new SubMap(m -> m.tailMap(fromKey, inclusive));
   }

   @Override
   public NavigableMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   @Override
   public NavigableMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   @Override
   public NavigableMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
   }

   @Override
   public V putIfAbsent(K key, V value) {
      return lock.write(m -> m.putIfAbsent(key, value));
   }

   @Override
   public boolean remove(Object key, Object value) {
      return lock.write(m -> m.remove(key, value));
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return lock.write(m -> m.replace(key, oldValue, newValue));
   }

   @Override
   public V replace(K key, V value) {
      return lock.write(m -> m.replace(key, value));
   }
   
   @Override
   public V getOrDefault(Object key, V defaultValue) {
      return lock.read(m -> m.getOrDefault(key, defaultValue));
   }

   @Override
   public void forEach(BiConsumer<? super K, ? super V> action) {
      lock.snapshot().forEach(action);
   }

   @Override
   public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
      lock.writeWith(m -> m.replaceAll(function));
   }

   @Override
   public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
      return lock.write(m -> m.computeIfAbsent(key, mappingFunction));
   }

   @Override
   public V computeIfPresent(K key,
         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      return lock.write(m -> m.computeIfPresent(key, remappingFunction));
   }

   @Override
   public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      return lock.write(m -> m.compute(key, remappingFunction));
   }

   @Override
   public V merge(K key, V value,
         BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
      return lock.write(m -> m.merge(key, value, remappingFunction));
   }
   
   @Override
   public boolean equals(Object obj) {
      return lock.read(m -> m.equals(obj));
   }

   @Override
   public int hashCode() {
      return lock.read(Map::hashCode);
   }

   @Override
   public String toString() {
      return lock.read(Map::toString);
   }

   EntryImpl wrap(Entry<K, V> e) {
      return e == null ? null : new EntryImpl(e);
   }
   
   /**
    * Implements the {@link Entry} interface for entries in this map. This represents a snapshot of
    * an entry, but does support modification via {@link #setValue(Object)}. If that method is
    * called, but the entry has been concurrently deleted, a {@link ConcurrentModificationException}
    * will be thrown.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class EntryImpl implements Entry<K, V> {
      final K key;
      volatile V value;
      
      EntryImpl(Entry<K, V> e) {
         this.key = e.getKey();
         this.value = e.getValue();
      }
         
      @Override
      public K getKey() {
         return key;
      }

      @Override
      public V getValue() {
         return lock.read(m -> {
            V v = m.get(key);
            if (v == null && !m.containsKey(key)) {
               // use last known good value if the key has been concurrently removed
               return value;
            }
            return value = v;
         });
      }

      @Override
      public V setValue(V value) {
         return lock.write(m -> {
            V v = m.replace(key, value);
            if (v == null && !m.containsKey(key)) {
               throw new ConcurrentModificationException();
            }
            this.value = value;
            return v;
         });
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
   
   /**
    * A view of keys for a map or sub-map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class Keys extends AbstractSet<K> implements NavigableSet<K> {
      /**
       * The function used to extract the applicable key set from the base tree map.
       */
      private final Function<NavigableMap<K, V>, NavigableSet<K>> setFromMap;
      
      Keys(Function<NavigableMap<K, V>, NavigableSet<K>> setFromMap) {
         this.setFromMap = setFromMap;
      }
      
      @Override
      public int size() {
         return lock.read(m -> setFromMap.apply(m).size());
      }

      @Override
      public boolean isEmpty() {
         return lock.read(m -> setFromMap.apply(m).isEmpty());
      }

      @Override
      public boolean contains(Object o) {
         return lock.read(m -> setFromMap.apply(m).contains(o));
      }

      @Override
      public Iterator<K> iterator() {
         return new KeyIterator(setFromMap.apply(lock.snapshot()).iterator());
      }

      @Override
      public Object[] toArray() {
         return lock.read(m -> setFromMap.apply(m).toArray());
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return lock.read(m -> setFromMap.apply(m).toArray(a));
      }

      @Override
      public boolean remove(Object o) {
         return lock.write(m -> setFromMap.apply(m).remove(o));
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return lock.read(m -> setFromMap.apply(m).containsAll(c));
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return lock.write(m -> setFromMap.apply(m).retainAll(c));
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return lock.write(m -> setFromMap.apply(m).removeAll(c));
      }

      @Override
      public void clear() {
         lock.writeWith(m -> setFromMap.apply(m).clear());
      }

      @Override
      public Comparator<? super K> comparator() {
         return lock.read(m -> setFromMap.apply(m).comparator());
      }

      @Override
      public K first() {
         return lock.read(m -> setFromMap.apply(m).first());
      }

      @Override
      public K last() {
         return lock.read(m -> setFromMap.apply(m).last());
      }

      @Override
      public K lower(K e) {
         return lock.read(m -> setFromMap.apply(m).lower(e));
      }

      @Override
      public K floor(K e) {
         return lock.read(m -> setFromMap.apply(m).floor(e));
      }

      @Override
      public K ceiling(K e) {
         return lock.read(m -> setFromMap.apply(m).ceiling(e));
      }

      @Override
      public K higher(K e) {
         return lock.read(m -> setFromMap.apply(m).higher(e));
      }

      @Override
      public K pollFirst() {
         return lock.read(m -> setFromMap.apply(m).pollFirst());
      }

      @Override
      public K pollLast() {
         return lock.read(m -> setFromMap.apply(m).pollLast());
      }

      @Override
      public NavigableSet<K> descendingSet() {
         return new DescendingSet<>(this);
      }

      @Override
      public Iterator<K> descendingIterator() {
         return new KeyIterator(setFromMap.apply(lock.snapshot()).descendingIterator());
      }

      @Override
      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement,
            boolean toInclusive) {
         // get subset now, even though we don't need it, to let underlying map verify arguments
         lock.readWith(m ->
               setFromMap.apply(m).subSet(fromElement, fromInclusive, toElement, toInclusive));
         // return view
         return new Keys(m ->
               setFromMap.apply(m).subSet(fromElement, fromInclusive, toElement, toInclusive));
      }

      @Override
      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         // get subset now, even though we don't need it, to let underlying map verify arguments
         lock.readWith(m -> setFromMap.apply(m).headSet(toElement, inclusive));
         // return view
         return new Keys(m -> setFromMap.apply(m).headSet(toElement, inclusive));
      }

      @Override
      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         // get subset now, even though we don't need it, to let underlying map verify arguments
         lock.readWith(m -> setFromMap.apply(m).tailSet(fromElement, inclusive));
         // return view
         return new Keys(m -> setFromMap.apply(m).tailSet(fromElement, inclusive));
      }

      @Override
      public SortedSet<K> subSet(K fromElement, K toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      @Override
      public SortedSet<K> headSet(K toElement) {
         return headSet(toElement, false);
      }

      @Override
      public SortedSet<K> tailSet(K fromElement) {
         return tailSet(fromElement, true);
      }
   }
   
   /**
    * An iterator through keys in the map. Wraps the supplied iterator (which will be from a
    * snapshot) in order to correctly implement {@link Iterator#remove()}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class KeyIterator implements Iterator<K> {
      private final Iterator<K> iter;
      private boolean hasFetched;
      private K lastFetched;
      
      KeyIterator(Iterator<K> iter) {
         this.iter = iter;
      }

      @Override
      public boolean hasNext() {
         return iter.hasNext();
      }

      @Override
      public K next() {
         hasFetched = true;
         lastFetched = iter.next();
         return lastFetched;
      }
      
      @Override
      public void remove() {
         if (!hasFetched) {
            throw new IllegalStateException();
         }
         hasFetched = false;
         DoubleInstanceLockedTreeMap.this.remove(lastFetched);
         lastFetched = null;
      }
   }

   /**
    * A view of values for a map or sub-map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class Values extends AbstractCollection<V> {
      /**
       * The function used to extract the applicable value collection from the base tree map.
       */
      private final Function<NavigableMap<K, V>, Collection<V>> collFromMap;
      
      /**
       * The function used to extract the applicable entry set from the base tree map. The entry
       * set is needed to correctly implement the value iterator (particularly, the {@code remove}
       * method).
       */
      private final Function<NavigableMap<K, V>, Set<Entry<K, V>>> entriesFromMap;
      
      Values(Function<NavigableMap<K, V>, Collection<V>> collFromMap,
            Function<NavigableMap<K, V>, Set<Entry<K, V>>> entriesFromMap) {
         this.collFromMap = collFromMap;
         this.entriesFromMap = entriesFromMap;
      }
      
      @Override
      public int size() {
         return lock.read(m -> collFromMap.apply(m).size());
      }

      @Override
      public boolean isEmpty() {
         return lock.read(m -> collFromMap.apply(m).isEmpty());
      }

      @Override
      public boolean contains(Object o) {
         return lock.read(m -> collFromMap.apply(m).contains(o));
      }

      @Override
      public Iterator<V> iterator() {
         Iterator<Entry<K, V>> iter = entriesFromMap.apply(lock.snapshot()).iterator();
         return new Iterator<V>() {
            boolean hasFetched;
            Entry<K, V> lastFetched;
            
            @Override
            public boolean hasNext() {
               return iter.hasNext();
            }

            @Override
            public V next() {
               hasFetched = true;
               lastFetched = iter.next();
               return lastFetched.getValue();
            }
            
            @Override
            public void remove() {
               if (!hasFetched) {
                  throw new IllegalStateException();
               }
               hasFetched = false;
               DoubleInstanceLockedTreeMap.this.remove(lastFetched.getKey(),
                     lastFetched.getValue());
               lastFetched = null;
            }
         };
      }

      @Override
      public Object[] toArray() {
         return lock.read(m -> collFromMap.apply(m).toArray());
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return lock.read(m -> collFromMap.apply(m).toArray(a));
      }

      @Override
      public boolean remove(Object o) {
         return lock.write(m -> collFromMap.apply(m).remove(o));
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return lock.read(m -> collFromMap.apply(m).containsAll(c));
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return lock.write(m -> collFromMap.apply(m).retainAll(c));
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return lock.write(m -> collFromMap.apply(m).removeAll(c));
      }

      @Override
      public void clear() {
         lock.writeWith(m -> collFromMap.apply(m).clear());
      }
   }
   
   /**
    * A view of entries for a map or sub-map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class Entries extends AbstractSet<Entry<K, V>> {
      /**
       * The function used to extract the applicable entry set from the base tree map.
       */
      private final Function<NavigableMap<K, V>, Set<Entry<K, V>>> setFromMap;
      
      Entries(Function<NavigableMap<K, V>, Set<Entry<K, V>>> setFromMap) {
         this.setFromMap = setFromMap;
      }
      
      @Override
      public int size() {
         return lock.read(m -> setFromMap.apply(m).size());
      }

      @Override
      public boolean isEmpty() {
         return lock.read(m -> setFromMap.apply(m).isEmpty());
      }

      @Override
      public boolean contains(Object o) {
         return lock.read(m -> setFromMap.apply(m).contains(o));
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         Iterator<Entry<K, V>> iter = setFromMap.apply(lock.snapshot()).iterator();
         return new Iterator<Entry<K, V>>() {
            boolean hasFetched;
            Entry<K, V> lastFetched;
            
            @Override
            public boolean hasNext() {
               return iter.hasNext();
            }

            @Override
            public Entry<K, V> next() {
               hasFetched = true;
               lastFetched = wrap(iter.next());
               return lastFetched;
            }
            
            @Override
            public void remove() {
               if (!hasFetched) {
                  throw new IllegalStateException();
               }
               hasFetched = false;
               DoubleInstanceLockedTreeMap.this.remove(lastFetched.getKey(),
                     lastFetched.getValue());
               lastFetched = null;
            }
         };
      }

      @Override
      public Object[] toArray() {
         return lock.read(m -> setFromMap.apply(m).toArray());
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return lock.read(m -> setFromMap.apply(m).toArray(a));
      }

      @Override
      public boolean remove(Object o) {
         return lock.write(m -> setFromMap.apply(m).remove(o));
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return lock.read(m -> setFromMap.apply(m).containsAll(c));
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return lock.write(m -> setFromMap.apply(m).retainAll(c));
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return lock.write(m -> setFromMap.apply(m).removeAll(c));
      }

      @Override
      public void clear() {
         lock.writeWith(m -> setFromMap.apply(m).clear());
      }
   }

   /**
    * A sub-map view of the map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubMap implements NavigableMap<K, V> {
      /**
       * The function used to extract the sub-map from the base tree map.
       */
      private final Function<NavigableMap<K, V>, NavigableMap<K, V>> derive;
      
      SubMap(Function<NavigableMap<K, V>, NavigableMap<K, V>> derive) {
         this.derive = derive;
      }

      @Override
      public Comparator<? super K> comparator() {
         return lock.read(m -> derive.apply(m).comparator());
      }

      @Override
      public K firstKey() {
         return lock.read(m -> derive.apply(m).firstKey());
      }

      @Override
      public K lastKey() {
         return lock.read(m -> derive.apply(m).lastKey());
      }

      @Override
      public Set<K> keySet() {
         return navigableKeySet();
      }

      @Override
      public Collection<V> values() {
         return new Values(m -> derive.apply(m).values(), m -> derive.apply(m).entrySet());
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
         return new Entries(m -> derive.apply(m).entrySet());
      }

      @Override
      public int size() {
         return lock.read(m -> derive.apply(m).size());
      }

      @Override
      public boolean isEmpty() {
         return lock.read(m -> derive.apply(m).isEmpty());
      }

      @Override
      public boolean containsKey(Object key) {
         return lock.read(m -> derive.apply(m).containsKey(key));
      }

      @Override
      public boolean containsValue(Object value) {
         return lock.read(m -> derive.apply(m).containsValue(value));
      }

      @Override
      public V get(Object key) {
         return lock.read(m -> derive.apply(m).get(key));
      }

      @Override
      public V put(K key, V value) {
         return lock.write(m -> derive.apply(m).put(key, value));
      }

      @Override
      public V remove(Object key) {
         return lock.write(m -> derive.apply(m).remove(key));
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> map) {
         lock.writeWith(m -> derive.apply(m).putAll(map));
      }

      @Override
      public void clear() {
         lock.writeWith(m -> derive.apply(m).clear());
      }

      @Override
      public Entry<K, V> lowerEntry(K key) {
         return wrap(lock.read(m -> derive.apply(m).lowerEntry(key)));
      }

      @Override
      public K lowerKey(K key) {
         return lock.read(m -> derive.apply(m).lowerKey(key));
      }

      @Override
      public Entry<K, V> floorEntry(K key) {
         return wrap(lock.read(m -> derive.apply(m).floorEntry(key)));
      }

      @Override
      public K floorKey(K key) {
         return lock.read(m -> derive.apply(m).floorKey(key));
      }

      @Override
      public Entry<K, V> ceilingEntry(K key) {
         return wrap(lock.read(m -> derive.apply(m).ceilingEntry(key)));
      }

      @Override
      public K ceilingKey(K key) {
         return lock.read(m -> derive.apply(m).ceilingKey(key));
      }

      @Override
      public Entry<K, V> higherEntry(K key) {
         return wrap(lock.read(m -> derive.apply(m).higherEntry(key)));
      }

      @Override
      public K higherKey(K key) {
         return lock.read(m -> derive.apply(m).higherKey(key));
      }

      @Override
      public Entry<K, V> firstEntry() {
         return wrap(lock.read(m -> derive.apply(m).firstEntry()));
      }

      @Override
      public Entry<K, V> lastEntry() {
         return wrap(lock.read(m -> derive.apply(m).lastEntry()));
      }

      @Override
      public Entry<K, V> pollFirstEntry() {
         return wrap(lock.read(m -> derive.apply(m).pollFirstEntry()));
      }

      @Override
      public Entry<K, V> pollLastEntry() {
         return wrap(lock.read(m -> derive.apply(m).pollLastEntry()));
      }

      @Override
      public NavigableMap<K, V> descendingMap() {
         return new DescendingMap<>(this);
      }

      @Override
      public NavigableSet<K> navigableKeySet() {
         return new Keys(m -> derive.apply(m).navigableKeySet());
      }

      @Override
      public NavigableSet<K> descendingKeySet() {
         return navigableKeySet().descendingSet();
      }

      @Override
      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         // get submap now, even though we don't need it, to let underlying map verify arguments
         lock.readWith(m -> derive.apply(m).subMap(fromKey, fromInclusive, toKey, toInclusive));
         return new SubMap(m -> derive.apply(m).subMap(fromKey, fromInclusive, toKey, toInclusive));
      }

      @Override
      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         // get submap now, even though we don't need it, to let underlying map verify arguments
         lock.readWith(m -> derive.apply(m).headMap(toKey, inclusive));
         return new SubMap(m -> derive.apply(m).headMap(toKey, inclusive));
      }

      @Override
      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         // get submap now, even though we don't need it, to let underlying map verify arguments
         lock.readWith(m -> derive.apply(m).tailMap(fromKey, inclusive));
         return new SubMap(m -> derive.apply(m).tailMap(fromKey, inclusive));
      }

      @Override
      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return subMap(fromKey, true, toKey, false);
      }

      @Override
      public SortedMap<K, V> headMap(K toKey) {
         return headMap(toKey, false);
      }

      @Override
      public SortedMap<K, V> tailMap(K fromKey) {
         return tailMap(fromKey, true);
      }
      
      @Override
      public V getOrDefault(Object key, V defaultValue) {
         return lock.read(m -> derive.apply(m).getOrDefault(key, defaultValue));
      }

      @Override
      public void forEach(BiConsumer<? super K, ? super V> action) {
         derive.apply(lock.snapshot()).forEach(action);
      }

      @Override
      public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
         lock.writeWith(m -> derive.apply(m).replaceAll(function));
      }

      @Override
      public V putIfAbsent(K key, V value) {
         return lock.write(m -> derive.apply(m).putIfAbsent(key, value));
      }

      @Override
      public boolean remove(Object key, Object value) {
         return lock.write(m -> derive.apply(m).remove(key, value));
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         return lock.write(m -> derive.apply(m).replace(key, oldValue, newValue));
      }

      @Override
      public V replace(K key, V value) {
         return lock.write(m -> derive.apply(m).replace(key, value));
      }

      @Override
      public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
         return lock.write(m -> derive.apply(m).computeIfAbsent(key, mappingFunction));
      }

      @Override
      public V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         return lock.write(m -> derive.apply(m).computeIfPresent(key, remappingFunction));
      }

      @Override
      public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         return lock.write(m -> derive.apply(m).compute(key, remappingFunction));
      }

      @Override
      public V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
         return lock.write(m -> derive.apply(m).merge(key, value, remappingFunction));
      }

      @Override
      public boolean equals(Object o) {
         return lock.read(m -> derive.apply(m).equals(o));
      }

      @Override
      public int hashCode() {
         return lock.read(m -> derive.apply(m).hashCode());
      }

      @Override
      public String toString() {
         return lock.read(m -> derive.apply(m).toString());
      }
   }
}
