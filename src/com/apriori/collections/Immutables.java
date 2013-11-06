package com.apriori.collections;

import com.apriori.util.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;

// TODO: javadoc
// TODO: tests
public final class Immutables {
   private Immutables() {
   }
   
   public static <E> Collection<E> asIfMutable(ImmutableCollection<? extends E> collection) {
      return new CollectionFromImmutable<E>(collection);
   }

   public static <E> List<E> asIfMutable(ImmutableList<? extends E> list) {
      return list instanceof RandomAccess
            ? new RandomAccessListFromImmutable<E>(list)
            : new ListFromImmutable<E>(list);
   }

   public static <E> Set<E> asIfMutable(ImmutableSet<? extends E> set) {
      return new SetFromImmutable<E>(set);
   }

   public static <K, V> Map<K, V> asIfMutable(final ImmutableMap<? extends K, ? extends V> map) {
      return new Map<K, V>() {
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
            return map.values().contains(value);
         }

         @Override
         public V get(Object key) {
            return map.get(key);
         }

         @Override
         public V put(K key, V value) {
            throw new UnsupportedOperationException();
         }

         @Override
         public V remove(Object key) {
            throw new UnsupportedOperationException();
         }

         @Override
         public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
         }

         @Override
         public void clear() {
            throw new UnsupportedOperationException();
         }

         @Override
         public Set<K> keySet() {
            return asIfMutable(map.keySet());
         }

         @Override
         public Collection<V> values() {
            return asIfMutable(map.values());
         }

         @Override
         public Set<Map.Entry<K, V>> entrySet() {
            @SuppressWarnings("unchecked") // safe because it's immutable
            ImmutableMap<K, V> castMap = (ImmutableMap<K, V>) map;
            
            return new TransformingSet<ImmutableMap.Entry<K, V>, Map.Entry<K,V>>(
                  asIfMutable(castMap.entrySet()),
                  new Function<ImmutableMap.Entry<K, V>, Map.Entry<K,V>>() {
                     @Override
                     public Map.Entry<K, V> apply(ImmutableMap.Entry<K, V> input) {
                        return asIfMutable(input);
                     }
                  });
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
      };
   }

   public static <K, V> Map.Entry<K, V> asIfMutable(
         final ImmutableMap.Entry<? extends K, ? extends V> entry) {
      return new Map.Entry<K, V>() {
         @Override
         public K getKey() {
            return entry.key();
         }

         @Override
         public V getValue() {
            return entry.value();
         }

         @Override
         public V setValue(V value) {
            throw new UnsupportedOperationException();
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
      };
   }

   public static <E> ImmutableCollection<E> toImmutableCollection(E... elements) {
      return toImmutableList(elements);
   }
   
   public static <E> ImmutableCollection<E> toImmutableCollection(Iterable<? extends E> iterable) {
      return toImmutableList(iterable);
   }

   public static <E> ImmutableList<E> toImmutableList(E... elements) {
      return toImmutableList(Arrays.asList(elements));
   }

   public static <E> ImmutableList<E> toImmutableList(Iterable<? extends E> iterable) {
      // TODO
      return null;
   }

   public static <E> ImmutableSet<E> toImmutableSet(E... elements) {
      return toImmutableSet(Arrays.asList(elements));
   }

   public static <E> ImmutableSet<E> toImmutableSet(Iterable<? extends E> iterable) {
      
      // TODO
      return null;
   }

   public static <E extends Comparable<E>> ImmutableSortedSet<E> toImmutableSortedSet(
         E... elements) {
      return toImmutableSortedSet(Arrays.asList(elements));
   }

   public static <E extends Comparable<E>> ImmutableSortedSet<E> toImmutableSortedSet(
         Iterable<? extends E> iterable) {
      // TODO
      return null;
   }

   public static <E> ImmutableSortedSet<E> toImmutableSortedSet(Comparator<E> comparator,
         E... elements) {
      return toImmutableSortedSet(comparator, Arrays.asList(elements));
   }

   public static <E> ImmutableSortedSet<E> toImmutableSortedSet(Comparator<E> comparator,
         Iterable<? extends E> iterable) {
      // TODO
      return null;
   }

   // can't use var-args and get type safety for alternating K-then-V types, so we "simulate"
   // using numerous overrides and support up to 5 key-value pairs
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap() {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key, V value) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2,
         K key3, V value3) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2,
         K key3, V value3, K key4, V value4) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2,
         K key3, V value3, K key4, V value4, K key5, V value5) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(Map<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap() {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(K key, V value) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(K key1, V value1,
         K key2, V value2) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(K key1, V value1,
         K key2, V value2, K key3, V value3) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(K key1, V value1,
         K key2, V value2, K key3, V value3, K key4, V value4) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(K key1, V value1,
         K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Map<? extends K, ? extends V> map) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap.Entry<K, V> toImmutableMapEntry(
         Map.Entry<? extends K, ? extends V> entry) {
      // TODO
      return null;
   }
   
   private static class CollectionFromImmutable<E> implements Collection<E> {
      final ImmutableCollection<? extends E> collection;
      
      CollectionFromImmutable(ImmutableCollection<? extends E> collection) {
         this.collection = collection;
      }
      
      @Override
      public int size() {
         return collection.size();
      }

      @Override
      public boolean isEmpty() {
         return collection.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return collection.contains(o);
      }

      @SuppressWarnings("unchecked")
      @Override
      public Iterator<E> iterator() {
         return (Iterator<E>) collection.iterator();
      }

      @Override
      public Object[] toArray() {
         return collection.toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return collection.toArray(a);
      }

      @Override
      public boolean add(E e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(Object o) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return collection.containsAll(c);
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }
      
      @Override public String toString() {
         return CollectionUtils.toString(this);
      }
   }
   
   private static class ListFromImmutable<E> extends CollectionFromImmutable<E> implements List<E> {
      ListFromImmutable(ImmutableList<? extends E> list) {
         super(list);
      }
      
      private ImmutableList<? extends E> getList() {
         return (ImmutableList<? extends E>) collection;
      }

      @Override
      public boolean addAll(int index, Collection<? extends E> c) {
         throw new UnsupportedOperationException();
      }

      @Override
      public E get(int index) {
         return getList().get(index);
      }

      @Override
      public E set(int index, E element) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(int index, E element) {
         throw new UnsupportedOperationException();
      }

      @Override
      public E remove(int index) {
         throw new UnsupportedOperationException();
      }

      @Override
      public int indexOf(Object o) {
         return getList().indexOf(o);
      }

      @Override
      public int lastIndexOf(Object o) {
         return getList().lastIndexOf(o);
      }

      @Override
      public ListIterator<E> listIterator() {
         Iterator<? extends E> iterator = collection.iterator();
         if (iterator instanceof BidiIterator) {
            return new ListIteratorFromBidiIterator<E>(
                  (BidiIterator<? extends E>) iterator);
         } else {
            return new ListIteratorFromIterator<E>(iterator);
         }
      }

      @Override
      public ListIterator<E> listIterator(int index) {
         if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("" + index);
         }
         Iterator<? extends E> iterator = collection.iterator();
         if (iterator instanceof BidiIterator) {
            return new ListIteratorFromBidiIterator<E>(
                  (BidiIterator<? extends E>) iterator, index);
         } else {
            return new ListIteratorFromIterator<E>(iterator, index);
         }
      }

      @Override
      public List<E> subList(int fromIndex, int toIndex) {
         return new ListFromImmutable<E>(getList().subList(fromIndex, toIndex));
      }
      
      @Override public boolean equals(Object o) {
         return CollectionUtils.equals(this, o);
      }
      
      @Override public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
   }
   
   private static class ListIteratorFromIterator<E> implements ListIterator<E> {
      private final Iterator<? extends E> iterator;
      private final ArrayList<E> items = new ArrayList<E>();
      private int index;
      
      ListIteratorFromIterator(Iterator<? extends E> iterator) {
         this.iterator = iterator;
      }
      
      ListIteratorFromIterator(Iterator<? extends E> iterator, int index) {
         this.iterator = iterator;
         for (int i = 0; i < index; i++) {
            items.add(iterator.next());
         }
         this.index = index;
      }

      @Override
      public boolean hasNext() {
         return index < items.size() || iterator.hasNext();
      }

      @Override
      public E next() {
         if (index == items.size()) {
            if (!iterator.hasNext()) {
               throw new NoSuchElementException();
            }
            items.add(iterator.next());
         }
         return items.get(index++);
      }

      @Override
      public boolean hasPrevious() {
         return index > 0;
      }

      @Override
      public E previous() {
         if (index <= 0) {
            throw new NoSuchElementException();
         }
         return items.get(--index);
      }

      @Override
      public int nextIndex() {
         return index;
      }

      @Override
      public int previousIndex() {
         return index - 1;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void set(E e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(E e) {
         throw new UnsupportedOperationException();
      }
   }
   
   private static class ListIteratorFromBidiIterator<E> implements ListIterator<E> {
      private final BidiIterator<? extends E> iterator;
      private int index;

      ListIteratorFromBidiIterator(BidiIterator<? extends E> iterator) {
         this.iterator = iterator;
      }
      
      ListIteratorFromBidiIterator(BidiIterator<? extends E> iterator, int index) {
         this.iterator = iterator;
         for (int i = 0; i < index; i++) {
            iterator.next();
         }
         this.index = index;
      }

      @Override
      public boolean hasNext() {
         return iterator.hasNext();
      }

      @Override
      public E next() {
         E ret = iterator.next();
         index++;
         return ret;
      }

      @Override
      public boolean hasPrevious() {
         return iterator.hasPrevious();
      }

      @Override
      public E previous() {
         E ret = iterator.previous();
         index--;
         return ret;
      }

      @Override
      public int nextIndex() {
         return index;
      }

      @Override
      public int previousIndex() {
         return index - 1;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void set(E e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(E e) {
         throw new UnsupportedOperationException();
      }
   }

   private static class RandomAccessListFromImmutable<E> extends ListFromImmutable<E>
         implements RandomAccess {
      RandomAccessListFromImmutable(ImmutableList<? extends E> list) {
         super(list);
         assert list instanceof RandomAccess;
      }
   }

   private static class SetFromImmutable<E> extends CollectionFromImmutable<E> implements Set<E> {
      SetFromImmutable(ImmutableSet<? extends E> set) {
         super(set);
      }
      
      @Override public boolean equals(Object o) {
         return CollectionUtils.equals(this, o);
      }
      
      @Override public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
   }
}