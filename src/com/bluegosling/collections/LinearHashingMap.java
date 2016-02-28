package com.bluegosling.collections;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

// TODO: javadoc
// TODO: serialization, cloning
// TODO: more efficient compute*() and merge() implementations
public class LinearHashingMap<K, V> extends AbstractMap<K, V> implements Serializable, Cloneable {
   
   private static final long serialVersionUID = 1840670418748277196L;

   private static class TableEntry<K, V> implements Entry<K, V> {
      private static final TableEntry<?, ?> REMOVED =
            new TableEntry<Object, Object>(null, null, -1, null);
      
      final int hashCode;
      final K key;
      V value;
      TableEntry<K, V> next;
      TableEntry<K, V> iterPrev, iterNext;
      
      public TableEntry(K key, V value, int hashCode, TableEntry<K, V> iterPrev) {
         this.key = key;
         this.value = value;
         this.hashCode = hashCode;
         this.iterPrev = iterPrev;
      }
      
      @Override public K getKey() {
         return key;
      }
      
      @Override public V getValue() {
         return value;
      }
      
      @Override public V setValue(V newValue) {
         if (next == REMOVED) {
            throw new ConcurrentModificationException();
         }
         V ret = value;
         value = newValue;
         return ret;
      }
      
      @SuppressWarnings("unchecked")
      void markRemoved() {
         next = (TableEntry<K, V>) REMOVED;
         iterPrev = iterNext = null;
      }
      
      @Override public boolean equals(Object o) {
         return MapUtils.equals(this, o);
      }
      
      @Override public int hashCode() {
         return MapUtils.hashCode(this);
      }
      
      @Override public String toString() {
         return MapUtils.toString(this);
      }
   }
   
   private static int computeThreshold(int size) {
      return (size >> 1) + (size >> 3); // 62.5%
   }

   private static final int DEFAULT_INITIAL_SIZE = 16;
   private static final int DEFAULT_INITIAL_MASK = DEFAULT_INITIAL_SIZE - 1;
   private static final int DEFAULT_INITIAL_THRESHOLD = computeThreshold(DEFAULT_INITIAL_SIZE);
   
   private transient GrowableArray<TableEntry<K, V>> table;
   private transient TableEntry<K, V> first;
   private transient TableEntry<K, V> last;
   private transient int mask = DEFAULT_INITIAL_MASK;
   private transient int nextSplitThreshold = DEFAULT_INITIAL_THRESHOLD;
   private transient int nextSplitIndex;
   private transient int modCount;
   private int size;
   
   public LinearHashingMap() {
      table = new NoCopyGrowableArray<>(DEFAULT_INITIAL_SIZE);
      mask = DEFAULT_INITIAL_MASK;
      nextSplitThreshold = DEFAULT_INITIAL_THRESHOLD;
   }

   public LinearHashingMap(int expectedSize) {
      if (expectedSize < 0) {
         throw new IllegalArgumentException();
      }
      int initialCapacity = (expectedSize << 4) / 10; // inverse of computeThreshold
      if (initialCapacity < 2) {
         initialCapacity = 2;
      }
      table = new NoCopyGrowableArray<>(initialCapacity);
      nextSplitThreshold = computeThreshold(initialCapacity);
      int highBit = Integer.highestOneBit(initialCapacity);
      if (highBit < initialCapacity) {
         highBit <<= 1;
      }
      mask = highBit - 1;
      nextSplitIndex = initialCapacity & ((highBit >> 1) - 1); 
   }

   public LinearHashingMap(Map<? extends K, ? extends V> other) {
      this(other.size());
      putAll(other);
   }

   @Override
   public int size() {
      return size;
   }

   private int tableIndex(int hash, int sz) {
      int bits = hash & mask;
      if (bits < sz) {
         return bits;
      } else {
         return bits & ~((mask + 1) >> 1);
      }
   }
   
   @Override
   public boolean containsKey(Object key) {
      return getEntry(key) != null;
   }
   
   @Override
   public V get(Object key) {
      TableEntry<K, V> entry = getEntry(key);
      return entry != null ? entry.value : null;
   }

   @Override
   public V getOrDefault(Object key, V defaultValue) {
      TableEntry<K, V> entry = getEntry(key);
      return entry != null ? entry.value : defaultValue;
   }

   private TableEntry<K, V> getEntry(Object key) {
      int hash = Objects.hashCode(key);
      TableEntry<K, V> entry = table.get(tableIndex(hash, table.size()));
      while (entry != null) {
         if (entry.hashCode == hash && Objects.equals(entry.key, key)) {
            return entry;
         }
         entry = entry.next;
      }
      return null;
   }
   
   @Override
   public V put(K key, V value) {
      return put(key, value, false);
   }
   
   @Override
   public V putIfAbsent(K key, V value) {
      return put(key, value, true);
   }

   private V put(K key, V value, boolean onlyIfAbsent) {
      int hash = Objects.hashCode(key);
      int tableIndex = tableIndex(hash, table.size());
      TableEntry<K, V> entry = table.get(tableIndex);
      if (entry == null) {
         table.set(tableIndex, newEntry(key, value, hash));
      } else {
         while (true) {
            if (entry.hashCode == hash && Objects.equals(entry.key, key)) {
               if (!onlyIfAbsent || entry.value == null) {
                  return entry.setValue(value);
               } else {
                  return entry.value;
               }
            }
            TableEntry<K, V> nextEntry = entry.next;
            if (nextEntry == null) {
               entry.next = newEntry(key, value, hash);
               break;
            }
            entry = nextEntry;
         }
      }
      if (size > nextSplitThreshold) {
         split();
      }
      return null;
   }

   private TableEntry<K, V> newEntry(K key, V value, int hash) {
      TableEntry<K, V> ret = new TableEntry<K, V>(key, value, hash, last);
      if (last != null) {
         assert first != null;
         last.iterNext = ret;
      } else {
         assert first == null;
         first = ret;
      }
      last = ret;
      size++;
      return ret;
   }
   
   private void split() {
      TableEntry<K, V> toSplit = table.get(nextSplitIndex);
      TableEntry<K, V> newEntry1 = null;
      TableEntry<K, V> newEntry2 = null;
      int wrapLimit = mask >> 1;
      int newSz = table.size() + 1;
      if (nextSplitIndex == 0) {
         mask = (mask << 1) | 1;
      }
      while (toSplit != null) {
         int index = tableIndex(toSplit.hashCode, newSz);
         if (index == nextSplitIndex) {
            if (newEntry1 == null) {
               newEntry1 = toSplit;
               toSplit = toSplit.next;
               newEntry1.next = null;
            } else {
               TableEntry<K, V> tmp = newEntry1;
               newEntry1 = toSplit;
               toSplit = toSplit.next;
               newEntry1.next = tmp;
            }
         } else {
            if (newEntry2 == null) {
               newEntry2 = toSplit;
               toSplit = toSplit.next;
               newEntry2.next = null;
            } else {
               TableEntry<K, V> tmp = newEntry2;
               newEntry2 = toSplit;
               toSplit = toSplit.next;
               newEntry2.next = tmp;
            }
         }
      }
      if (newEntry2 != null) {
         // structural change if re-hash resulted in re-distribution of keys
         modCount++;
      }
      table.set(nextSplitIndex, newEntry1);
      table.push(newEntry2);
      nextSplitThreshold = computeThreshold(table.size());
      if (++nextSplitIndex > wrapLimit) {
         nextSplitIndex = 0;
      }
   }
   
   @Override
   public V replace(K key, V value) {
      int hash = Objects.hashCode(key);
      TableEntry<K, V> entry = table.get(tableIndex(hash, table.size()));
      while (entry != null) {
         if (entry.hashCode == hash && Objects.equals(entry.key, key)) {
            return entry.setValue(value);
         }
         entry = entry.next;
      }
      return null;
   }

   @Override
   public boolean replace(K key, V expectedValue, V newValue) {
      int hash = Objects.hashCode(key);
      TableEntry<K, V> entry = table.get(tableIndex(hash, table.size()));
      while (entry != null) {
         if (entry.hashCode == hash && Objects.equals(entry.key, key)) {
            if (Objects.equals(entry.value, expectedValue)) {
               entry.value = newValue;
               return true;
            }
            return false;
         }
         entry = entry.next;
      }
      return false;
   }

   @Override
   public V remove(Object key) {
      TableEntry<K, V> entry = removeEntry(key, null, false);
      return entry != null ? entry.value : null;
   }

   @Override
   public boolean remove(Object key, Object value) {
      TableEntry<K, V> entry = removeEntry(key, value, true);
      return entry != null;
   }

   private TableEntry<K, V> removeEntry(Object key, Object value, boolean matchValue) {
      int hash = Objects.hashCode(key);
      int tableIndex = tableIndex(hash, table.size());
      TableEntry<K, V> entry = table.get(tableIndex);
      TableEntry<K, V> prev = null;
      while (entry != null) {
         if (entry.hashCode == hash && Objects.equals(entry.key, key)) {
            if (matchValue && !Objects.equals(entry.value, value)) {
               return null;
            }
            removeEntry(entry, prev, tableIndex);
            return entry;
         }
         prev = entry;
         entry = entry.next;
      }
      return null;
   }
   
   private void removeEntry(TableEntry<K, V> entry, TableEntry<K, V> prev, int tableIndex) {
      // remove from table entry linked-list
      if (prev == null) {
         table.set(tableIndex, entry.next);
      } else {
         prev.next = entry.next;
      }
      // and also remove from doubly-linked-list of iteration order
      if (first == entry) {
         assert entry.iterPrev == null;
         first = entry.iterNext;
      } else {
         entry.iterPrev.iterNext = entry.iterNext;
      }
      if (last == entry) {
         assert entry.iterNext == null;
         last = entry.iterPrev;
      } else {
         entry.iterNext.iterPrev = entry.iterPrev;
      }
      // mark it and then done
      entry.markRemoved();
      size--;
   }
   
   @Override
   public void clear() {
      modCount++;
      table.adjustSizeTo(DEFAULT_INITIAL_SIZE);
      for (int i = 0; i < DEFAULT_INITIAL_SIZE; i++) {
         table.set(i, null);
      }
      size = nextSplitIndex = 0;
      nextSplitThreshold = DEFAULT_INITIAL_THRESHOLD;
      mask = DEFAULT_INITIAL_MASK;
      first = last = null;
   }
   
   @Override
   public void forEach(BiConsumer<? super K, ? super V> action) {
      if (action == null) {
         throw new NullPointerException();
      }
      int mc = modCount;
      for (TableEntry<K,V> current = first; current != null; current = current.iterNext) {
         action.accept(current.key, current.value);
         if (modCount != mc) {
            throw new ConcurrentModificationException();
         }
      }
   }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
     if (function == null) {
        throw new NullPointerException();
     }
     int mc = modCount;
     for (TableEntry<K,V> current = first; current != null; current = current.iterNext) {
        current.value = function.apply(current.key, current.value);
        if (modCount != mc) {
           throw new ConcurrentModificationException();
        }
     }
  }
   
   @Override
   public Set<K> keySet() {
      return new AbstractSet<K>() {
         @Override public Iterator<K> iterator() {
            return new Iterator<K>() {
               private final Iterator<Entry<K,V>> iter = entrySet().iterator();
      
               @Override public boolean hasNext() {
                  return iter.hasNext();
               }

               @Override public K next() {
                  return iter.next().getKey();
               }
      
               @Override public void remove() {
                  iter.remove();
               }
            };
         }
      
         @Override public int size() {
            return LinearHashingMap.this.size();
         }
      
         @Override public boolean isEmpty() {
            return LinearHashingMap.this.isEmpty();
         }
      
         @Override public void clear() {
            LinearHashingMap.this.clear();
         }
      
         @Override public boolean contains(Object k) {
            return containsKey(k);
         }
         
         @SuppressWarnings("synthetic-access")
         @Override public boolean remove(Object k) {
            return removeEntry(k, null, false) != null;
         }
      };
   }
   
   @Override
   public Set<Entry<K, V>> entrySet() {
      return new EntrySet();
   }

   private class EntrySet extends AbstractSet<Entry<K, V>> {
      
      EntrySet() {
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         return new EntryIterator();
      }

      @Override
      public int size() {
         return LinearHashingMap.this.size();
      }
      
      @Override
      public boolean isEmpty() {
         return LinearHashingMap.this.isEmpty();
      }
      
      @Override
      public void clear() {
         LinearHashingMap.this.clear();
      }
      
      @Override
      public boolean contains(Object o) {
         if (o instanceof Entry) {
            Entry<?, ?> other = (Entry<?, ?>) o;
            @SuppressWarnings("synthetic-access")
            TableEntry<K, V> entry = getEntry(other.getKey());
            return entry != null && Objects.equals(entry.value, other.getValue());
         }
         return false;
      }
      
      @Override
      public boolean remove(Object o) {
         if (o instanceof Entry) {
            Entry<?, ?> other = (Entry<?, ?>) o;
            return LinearHashingMap.this.remove(other.getKey(), other.getValue());
         }
         return false;
      }
   }
   
   @SuppressWarnings("synthetic-access")
   private class EntryIterator implements Iterator<Entry<K, V>> {

      int mc = modCount;
      TableEntry<K, V> next = first;
      TableEntry<K, V> lastFetched;
      
      EntryIterator() {
      }

      void checkModCount() {
         if (mc != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         return next != null;
      }

      @Override
      public Entry<K, V> next() {
         if (next == null) {
            throw new NoSuchElementException();
         }
         checkModCount();
         lastFetched = next;
         next = next.iterNext;
         return lastFetched;
      }
      
      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         checkModCount();
         // find the node's predecessor
         int tableIndex = tableIndex(lastFetched.hashCode, table.size());
         TableEntry<K, V> predecessor = null;
         TableEntry<K, V> entry = table.get(tableIndex);
         boolean found = false;
         while (entry != null) {
            if (entry == lastFetched) {
               found = true;
               break;
            }
            predecessor = entry;
            entry = entry.next;
         }
         if (!found) {
            throw new ConcurrentModificationException();
         }
         removeEntry(lastFetched, predecessor, tableIndex);
         lastFetched = null;
      }
   }
   
   
   
   // TODO: remove and move anything useful into unit tests
   public static void main(String args[]) {
      Random r = new Random(0);
      HashMap<String, Integer> benchmark = new HashMap<>();
      LinearHashingMap<String, Integer> map = new LinearHashingMap<>(500);
      for (int i = 0; i < 26; i++) {
         for (int j = 0; j < 26; j++) {
            int len = r.nextInt(5) + 2;
            StringBuilder sb = new StringBuilder(len);
            for (int x = 0; x < len; x++) {
               sb.append((char)((x == 0 ? 'A' : 'a') + r.nextInt(26)));
            }
            String k = sb.toString();
            if (map.put(k, j) != benchmark.put(k, j)) {
               throw new IllegalStateException();
            }
            if (map.get(k) != j) {
               throw new IllegalStateException();
            }
            if (map.remove(k) != j) {
               throw new IllegalStateException();
            }
            if (map.get(k) != null) {
               throw new IllegalStateException();
            }
            if (map.putIfAbsent(k, j) != null) {
               throw new IllegalStateException();
            }
            if (map.putIfAbsent(k, j + 100) != j) {
               throw new IllegalStateException();
            }
            if (map.get(k) != j) {
               throw new IllegalStateException();
            }
            if (map.replace(k, j + 100) != j) {
               throw new IllegalStateException();
            }
            if (map.replace(k, j) != j + 100) {
               throw new IllegalStateException();
            }
            if (!map.replace(k, j, j + 1)) {
               throw new IllegalStateException();
            }
            if (map.replace(k, j, j + 100)) {
               throw new IllegalStateException();
            }
            if (map.put(k, j) != j + 1) {
               throw new IllegalStateException();
            }
         }
      }
      for (Entry<String, Integer> entry : map.entrySet()) {
         if (entry.getValue() != map.get(entry.getKey())) {
            throw new IllegalStateException();
         }
      }
      if (map.hashCode() != benchmark.hashCode()) {
         throw new IllegalStateException();
      }
      if (!(map.equals(benchmark) && benchmark.equals(map))) {
         throw new IllegalStateException();
      }
      System.out.println(map.entrySet().stream()
            .map(entry -> entry.getKey() + " -> " + entry.getValue())
            .collect(Collectors.joining("\n")));
      System.out.println(map.size());
   }
}
