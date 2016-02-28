package com.bluegosling.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.function.BiPredicate;

// TODO: javadoc
// TODO: tests
public class FilteringNavigableMap<K, V> extends FilteringSortedMap<K, V>
      implements NavigableMap<K, V> {

   public FilteringNavigableMap(NavigableMap<K, V> internal,
         BiPredicate<? super K, ? super V> predicate) {
      super(internal, predicate);
   }

   @Override
   protected NavigableMap<K, V> internal() {
      return (NavigableMap<K, V>) super.internal();
   }
   
   @Override
   public NavigableMap<K, V> capture() {
      return Collections.unmodifiableNavigableMap(new SortedArrayMap<>(this));
   }
   
   @Override
   public Entry<K, V> lowerEntry(K key) {
      Iterator<K> iter = headMap(key, false).navigableKeySet().descendingIterator(); 
      return iter.hasNext() ? internal().floorEntry(iter.next()) : null;
   }

   @Override
   public K lowerKey(K key) {
      Entry<K, V> entry = lowerEntry(key);
      return entry != null ? entry.getKey() : null;
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
      Iterator<K> iter = headMap(key, true).navigableKeySet().descendingIterator(); 
      return iter.hasNext() ? internal().floorEntry(iter.next()) : null;
   }

   @Override
   public K floorKey(K key) {
      Entry<K, V> entry = floorEntry(key);
      return entry != null ? entry.getKey() : null;
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
      Iterator<Entry<K, V>> iter = tailMap(key, true).entrySet().iterator(); 
      return iter.hasNext() ? iter.next() : null;
   }

   @Override
   public K ceilingKey(K key) {
      Entry<K, V> entry = ceilingEntry(key);
      return entry != null ? entry.getKey() : null;
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
      Iterator<Entry<K, V>> iter = tailMap(key, false).entrySet().iterator(); 
      return iter.hasNext() ? iter.next() : null;
   }

   @Override
   public K higherKey(K key) {
      Entry<K, V> entry = higherEntry(key);
      return entry != null ? entry.getKey() : null;
   }

   @Override
   public Entry<K, V> firstEntry() {
      Iterator<Entry<K, V>> iter = entrySet().iterator();
      return iter.hasNext() ? iter.next() : null;
   }
   
   @Override
   public K firstKey() {
      Entry<K, V> entry = firstEntry();
      if (entry == null) {
         throw new NoSuchElementException();
      }
      return entry.getKey();
   }

   @Override
   public Entry<K, V> lastEntry() {
      Iterator<K> iter = internal().navigableKeySet().descendingIterator();
      return iter.hasNext() ? internal().floorEntry(iter.next()) : null;
   }

   @Override
   public K lastKey() {
      Entry<K, V> entry = lastEntry();
      if (entry == null) {
         throw new NoSuchElementException();
      }
      return entry.getKey();
   }

   @Override
   public Entry<K, V> pollFirstEntry() {
      Entry<K, V> ret = firstEntry();
      if (ret != null) {
         internal().remove(ret.getKey());
      }
      return ret;
   }

   @Override
   public Entry<K, V> pollLastEntry() {
      Entry<K, V> ret = lastEntry();
      if (ret != null) {
         internal().remove(ret.getKey());
      }
      return ret;
   }

   @Override
   public NavigableMap<K, V> descendingMap() {
      return new DescendingMap<>(this);
   }

   @Override
   public NavigableSet<K> keySet() {
      return navigableKeySet();
   }
   
   @Override
   public NavigableSet<K> navigableKeySet() {
      return new NavigableKeySet();
   }

   @Override
   public NavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }

   @Override
   public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
         K toKey, boolean toInclusive) {
      return new FilteringNavigableMap<>(
            internal().subMap(fromKey, fromInclusive, toKey, toInclusive),
            predicate());
   }

   @Override
   public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return new FilteringNavigableMap<>(internal().headMap(toKey, inclusive),
            predicate());
   }

   @Override
   public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new FilteringNavigableMap<>(internal().tailMap(fromKey, inclusive),
            predicate());
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
   
   private class NavigableKeySet implements NavigableSet<K> {
      NavigableKeySet() {
      }

      @Override
      public Comparator<? super K> comparator() {
         return FilteringNavigableMap.this.comparator();
      }

      @Override
      public K first() {
         return FilteringNavigableMap.this.firstKey();
      }

      @Override
      public K last() {
         return FilteringNavigableMap.this.lastKey();
      }

      @Override
      public int size() {
         return FilteringNavigableMap.this.size();
      }

      @Override
      public boolean isEmpty() {
         return FilteringNavigableMap.this.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return FilteringNavigableMap.this.containsKey(o);
      }

      @Override
      public Object[] toArray() {
         return Iterables.toArray(this);
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return Iterables.toArray(this, a);
      }

      @Override
      public boolean add(K e) {
         throw new UnsupportedOperationException("add");
      }

      @Override
      public boolean remove(Object o) {
         if (contains(o)) {
            FilteringNavigableMap.this.remove(o);
            return true;
         }
         return false;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         for (Object o : c) {
            if (!contains(o)) {
               return false;
            }
         }
         return true;
      }

      @Override
      public boolean addAll(Collection<? extends K> c) {
         throw new UnsupportedOperationException("addAll");
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), false);
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         boolean ret = false;
         for (Object o : c) {
            if (remove(o)) {
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public void clear() {
         FilteringNavigableMap.this.clear();
      }

      @Override
      public K lower(K e) {
         return FilteringNavigableMap.this.lowerKey(e);
      }

      @Override
      public K floor(K e) {
         return FilteringNavigableMap.this.floorKey(e);
      }

      @Override
      public K ceiling(K e) {
         return FilteringNavigableMap.this.ceilingKey(e);
      }

      @Override
      public K higher(K e) {
         return FilteringNavigableMap.this.higherKey(e);
      }

      @Override
      public K pollFirst() {
         Entry<K, V> entry = FilteringNavigableMap.this.pollFirstEntry();
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public K pollLast() {
         Entry<K, V> entry = FilteringNavigableMap.this.pollLastEntry();
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Iterator<K> iterator() {
         return new TransformingIterator<>(FilteringNavigableMap.this.entrySet().iterator(),
               e -> e.getKey());
      }

      @Override
      public NavigableSet<K> descendingSet() {
         return new DescendingSet<>(this);
      }

      @Override
      public Iterator<K> descendingIterator() {
         return new TransformingIterator<>(
               FilteringNavigableMap.this.descendingMap().entrySet().iterator(), e -> e.getKey());
      }

      @Override
      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement,
            boolean toInclusive) {
         return FilteringNavigableMap.this.subMap(
               fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         return FilteringNavigableMap.this.headMap(toElement, inclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return FilteringNavigableMap.this.tailMap(fromElement, inclusive).navigableKeySet();
      }

      @Override
      public SortedSet<K> subSet(K fromElement, K toElement) {
         return FilteringNavigableMap.this.subMap(fromElement, toElement).navigableKeySet();
      }

      @Override
      public SortedSet<K> headSet(K toElement) {
         return FilteringNavigableMap.this.headMap(toElement).navigableKeySet();
      }

      @Override
      public SortedSet<K> tailSet(K fromElement) {
         return FilteringNavigableMap.this.tailMap(fromElement).navigableKeySet();
      }
   }
}
