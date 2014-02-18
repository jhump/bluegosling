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
import java.util.SortedMap;

// TODO: javadoc
// TODO: tests
public final class Immutables {
   private Immutables() {
   }
   
   private static final Object EMPTY[] = new Object[0];
   
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
            ImmutableMap<K, V> castMap = cast(map);
            
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
   
   public static <E> ImmutableCollection<E> cast(ImmutableCollection<? extends E> coll) {
      @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
      ImmutableCollection<E> cast = (ImmutableCollection<E>) coll;
      return cast;
   }

   public static <E> ImmutableList<E> cast(ImmutableList<? extends E> list) {
      @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
      ImmutableList<E> cast = (ImmutableList<E>) list;
      return cast;
   }

   public static <E> ImmutableSet<E> cast(ImmutableSet<? extends E> set) {
      @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
      ImmutableSet<E> cast = (ImmutableSet<E>) set;
      return cast;
   }

   public static <K, V> ImmutableMap<K, V> cast(ImmutableMap<? extends K, ? extends V> map) {
      @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
      ImmutableMap<K, V> cast = (ImmutableMap<K, V>) map;
      return cast;
   }

   public static <K, V> ImmutableSortedMap<K, V> cast(
         ImmutableSortedMap<? extends K, ? extends V> map) {
      @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
      ImmutableSortedMap<K, V> cast = (ImmutableSortedMap<K, V>) map;
      return cast;
   }

   public static <K, V> ImmutableMap.Entry<K, V> cast(
         ImmutableMap.Entry<? extends K, ? extends V> entry) {
      @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
      ImmutableMap.Entry<K, V> cast = (ImmutableMap.Entry<K, V>) entry;
      return cast;
   }
   
   public static <E> ImmutableCollection<E> emptyImmutableCollection() {
      @SuppressWarnings("unchecked")
      ImmutableCollection<E> ret = (ImmutableCollection<E>) EmptyCollection.INSTANCE;
      return ret;
   }
   
   public static <E> ImmutableCollection<E> singletonImmutableCollection(E element) {
      return new SingletonCollection<E>(element);
   }
   
   public static <E> ImmutableCollection<E> toImmutableCollection(E... elements) {
      return toImmutableList(elements);
   }
   
   public static <E> ImmutableCollection<E> toImmutableCollection(Iterable<? extends E> iterable) {
      return toImmutableList(iterable);
   }

   public static <E> ImmutableList<E> emptyImmutableList() {
      @SuppressWarnings("unchecked")
      ImmutableList<E> ret = (ImmutableList<E>) EmptyList.INSTANCE;
      return ret;
   }
   
   public static <E> ImmutableList<E> singletonImmutableList(E element) {
      return new SingletonList<E>(element);
   }
   
   private static Object[] toArray(Iterable<?> iterable) {
      Iterator<?> iter = iterable.iterator();
      if (!iter.hasNext()) {
         return EMPTY;
      } else if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).toArray();
      } else if (iterable instanceof ImmutableCollection) {
         return ((ImmutableCollection<?>) iterable).toArray();
      } else {
         class Chunk {
            final Object contents[];
            Chunk next;
            Chunk(int limit) {
               contents = new Object[limit];
            }
         }
         int size = 0;
         int chunkLimit = 16;
         Chunk head = new Chunk(chunkLimit);
         Chunk current = head;
         int currentIndex = 0;
         for (Object o : iterable) {
            size++;
            current.contents[currentIndex++] = o;
            if (currentIndex == chunkLimit) {
               chunkLimit = size;
               current.next = new Chunk(chunkLimit);
               current = current.next;
               currentIndex = 0;
            }
         }
         Object elements[] = new Object[size];
         currentIndex = 0;
         for (current = head; current != null; current = current.next) {
            for (int i = 0, len = current.contents.length; i < len && size > 0; i++) {
               elements[currentIndex++] = current.contents[i];
               size--;
            }
         }
         return elements;
      }
   }
   
   @SuppressWarnings("unchecked")
   private static <E> ImmutableList<E> makeImmutableList(final Object elements[]) {
      switch (elements.length) {
         case 0:
            return emptyImmutableList();
         case 1:
            return singletonImmutableList((E) elements[0]);
         default:
            return new ImmutableListImpl<E>(elements);
      }
   }
   
   public static <E> ImmutableList<E> toImmutableList(Iterable<? extends E> iterable) {
      if (iterable instanceof ImmutableList) {
         return cast((ImmutableList<? extends E>) iterable);
      }
      return makeImmutableList(toArray(iterable));
   }

   public static <E> ImmutableList<E> toImmutableList(E... elements) {
      return makeImmutableList(elements.clone());
   }

   public static <E> ImmutableSet<E> emptyImmutableSet() {
      @SuppressWarnings("unchecked")
      ImmutableSet<E> ret = (ImmutableSet<E>) EmptySet.INSTANCE;
      return ret;
   }
   
   public static <E> ImmutableSet<E> toImmutableSet(E... elements) {
      return toImmutableSet(Arrays.asList(elements));
   }

   public static <E> ImmutableSet<E> singletonImmutableSet(E element) {
      return new SingletonSet<E>(element);
   }
   
   public static <E> ImmutableSet<E> toImmutableSet(Iterable<? extends E> iterable) {
      if (iterable instanceof ImmutableSet) {
         return cast((ImmutableSet<? extends E>) iterable);
      }
      // TODO
      return null;
   }

   public static <E extends Comparable<E>> ImmutableSortedSet<E> toImmutableSortedSet(
         E... elements) {
      return toImmutableSortedSet(Arrays.asList(elements));
   }

   public static <E extends Comparable<E>> ImmutableSortedSet<E> toImmutableSortedSet(
         Iterable<? extends E> iterable) {
      return toImmutableSortedSet(CollectionUtils.NATURAL_ORDERING, iterable);
   }

   public static <E> ImmutableSortedSet<E> toImmutableSortedSet(Comparator<? super E> comparator,
         E... elements) {
      return toImmutableSortedSet(comparator, Arrays.asList(elements));
   }

   public static <E> ImmutableSortedSet<E> toImmutableSortedSet(Comparator<? super E> comparator,
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
   
   public static <E> ImmutableMap<E, E> toImmutableMap(E... keysAndValues) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> toImmutableMap(Map<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap() {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key, V value) {
      // TODO
      return null;
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2, K key3, V value3) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2, K key3, V value3) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2, K key3, V value3,
         K key4, V value4) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2, K key3, V value3,
         K key4, V value4, K key5, V value5) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Map<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         SortedMap<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, Map<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   public static <K, V> ImmutableMap.Entry<K, V> toImmutableMapEntry(
         Map.Entry<? extends K, ? extends V> entry) {
      return Immutables.<K, V>toImmutableMapEntry(entry.getKey(), entry.getValue());
   }

   public static <K, V> ImmutableMap.Entry<K, V> toImmutableMapEntry(K key, V value) {
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
   
   private static class EmptyCollection extends AbstractImmutableCollection<Object> {
      static final EmptyCollection INSTANCE = new EmptyCollection();
      
      @Override
      public int size() {
         return 0;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Object[] toArray() {
         return EMPTY;
      }

      @Override
      public <T> T[] toArray(T[] array) {
         if (array.length > 0) {
            array[0] = null;
         }
         return array;
      }

      @Override
      public boolean contains(Object o) {
         return false;
      }

      @Override
      public boolean containsAll(Iterable<?> items) {
         return false;
      }

      @Override
      public boolean containsAny(Iterable<?> items) {
         return false;
      }

      @Override
      public Iterator<Object> iterator() {
         return Iterators.emptyIterator();
      }
   }

   private static class EmptyList extends EmptyCollection implements ImmutableList<Object> {
      static final EmptyList INSTANCE = new EmptyList();

      @Override
      public Object get(int i) {
         throw new IndexOutOfBoundsException(String.valueOf(i));
      }

      @Override
      public int indexOf(Object o) {
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return -1;
      }

      @Override
      public ImmutableList<Object> subList(int from, int to) {
         return INSTANCE;
      }

      @Override
      public Object first() {
         throw new NoSuchElementException();
      }

      @Override
      public ImmutableList<Object> rest() {
         return INSTANCE;
      }
      
      @Override
      public boolean equals(Object o) {
         return CollectionUtils.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
   }
   
   private static class EmptySet extends EmptyCollection implements ImmutableSet<Object> {
      static final EmptySet INSTANCE = new EmptySet();
      
      @Override
      public boolean equals(Object o) {
         return CollectionUtils.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
   }

   private static class EmptySortedSet extends EmptySet implements ImmutableSortedSet<Object> {
      static final EmptySortedSet INSTANCE = new EmptySortedSet();
      
      @Override
      public Object first() {
         throw new NoSuchElementException();
      }


      @Override
      public ImmutableSortedSet<Object> rest() {
         return INSTANCE;
      }
      
      @Override
      public Object last() {
         throw new NoSuchElementException();
      }

      @Override
      public Object floor(Object e) {
         return null;
      }

      @Override
      public Object higher(Object e) {
         return null;
      }

      @Override
      public Object ceil(Object e) {
         return null;
      }

      @Override
      public Object lower(Object e) {
         return null;
      }
      
      @Override
      public ImmutableSortedSet<Object> subSet(Object from, Object to) {
         return INSTANCE;
      }

      @Override
      public ImmutableSortedSet<Object> subSet(Object from, boolean fromInclusive, Object to,
            boolean toInclusive) {
         return INSTANCE;
      }

      @Override
      public ImmutableSortedSet<Object> headSet(Object to) {
         return INSTANCE;
      }

      @Override
      public ImmutableSortedSet<Object> headSet(Object to, boolean inclusive) {
         return INSTANCE;
      }

      @Override
      public ImmutableSortedSet<Object> tailSet(Object from) {
         return INSTANCE;
      }

      @Override
      public ImmutableSortedSet<Object> tailSet(Object from, boolean inclusive) {
         return INSTANCE;
      }
   }
   
   private static class SingletonCollection<E> extends AbstractImmutableCollection<E> {
      final E element;
      
      SingletonCollection(E element) {
         this.element = element;
      }
      
      @Override
      public int size() {
         return 1;
      }

      @Override
      public boolean isEmpty() {
         return false;
      }

      @Override
      public Object[] toArray() {
         return new Object[] { element };
      }

      @Override
      public <T> T[] toArray(T[] array) {
         array = ArrayUtils.newArrayIfTooSmall(array, 1);
         array[0] = (T) element;
         if (array.length > 1) {
            array[1] = null;
         }
         return array;
      }

      @Override
      public boolean contains(Object o) {
         return o == null ? element == null : o.equals(element);
      }

      @Override
      public Iterator<E> iterator() {
         return Iterators.singletonIterator(element);
      }
   }

   private static class SingletonList<E> extends SingletonCollection<E>
         implements ImmutableList<E> {

      SingletonList(E e) {
         super(e);
      }
      
      @Override
      public E get(int i) {
         if (i != 0) {
            throw new IndexOutOfBoundsException(String.valueOf(i));
         }
         return element;
      }

      @Override
      public int indexOf(Object o) {
         return contains(o) ? 0 : -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return contains(o) ? 0 : -1;
      }

      @Override
      public ImmutableList<E> subList(int from, int to) {
         if (from < 0 || from > 1) {
            throw new IndexOutOfBoundsException(String.valueOf(from));
         }
         if (to < 0 || to > 1) {
            throw new IndexOutOfBoundsException(String.valueOf(to));
         }
         if (from > to) {
            throw new IndexOutOfBoundsException(String.valueOf(from) + " > " + String.valueOf(to));
         }
         ImmutableList<E> ret = from == to ? (ImmutableList<E>) EmptyList.INSTANCE : this;
         return ret;
      }

      @Override
      public E first() {
         return element;
      }

      @Override
      public ImmutableList<E> rest() {
         return (ImmutableList<E>) EmptyList.INSTANCE;
      }
      
      @Override
      public boolean equals(Object o) {
         return CollectionUtils.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
   }
   
   private static class SingletonSet<E> extends SingletonCollection<E> implements ImmutableSet<E> {
      
      SingletonSet(E e) {
         super(e);
      }
      
      @Override
      public boolean equals(Object o) {
         return CollectionUtils.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
   }
   
   private static class ImmutableListImpl<E> extends AbstractRandomAccessImmutableList<E> {
      private final Object elements[];
      
      ImmutableListImpl(Object elements[]) {
         this.elements = elements;
      }
      
      @Override
      public int size() {
         return elements.length;
      }

      @Override
      public boolean isEmpty() {
         return elements.length > 0;
      }

      @Override
      public Object[] toArray() {
         return elements.clone();
      }

      @Override
      public <T> T[] toArray(T[] array) {
         array = ArrayUtils.newArrayIfTooSmall(array, elements.length);
         System.arraycopy(elements, 0, array, 0, elements.length);
         if (array.length > elements.length) {
            array[elements.length] = null;
         }
         return array;
      }

      @Override
      public E get(int i) {
         if (i < 0 || i >= elements.length) {
            throw new IndexOutOfBoundsException(String.valueOf(i));
         }
         return (E) elements[i];
      }
   }

   private static class ImmutableSetImpl<E> extends AbstractImmutableSet<E> {
      private final Object table[];
      private final int hashCodes[];
      private final int size;

      ImmutableSetImpl(Object elements[]) {
         assert elements.length > 0;
         this.size = elements.length;
         // compute desired size based on a load factor of 0.75
         int tableSize = (this.size << 2) / 3;
         // use smallest power of 2 that is >= desired size
         int bit = Integer.highestOneBit(tableSize);
         if (bit != tableSize) {
            tableSize = bit << 1;
         }
         if (this.size > tableSize) {
            // could happen on overflow, for super-huge set
            tableSize = Integer.MAX_VALUE;
         }
         int mask = tableSize - 1;
         table = new Object[tableSize];
         hashCodes = new int[tableSize];
         for (Object e : elements) {
            int h = hash(e);
            int idx = h;
            while (true) {
               idx = h & mask;
               int hash = hashCodes[idx];
               Object obj = table[idx];
               if (hash == 0 && obj == null) {
                  // found an empty slot
                  hashCodes[idx] = h;
                  table[idx] = e;
                  break;
               } else
               if (hash == h && (obj == e || (obj != null && obj.equals(e)))) {
                  // item already exists in the table
                  continue;
               }
               // re-probe
               idx++;
            }
         }
      }
      
      private int hash(Object e) {
         int h = e == null ? 0 : e.hashCode();
         // Spread bits to regularize both segment and index locations,
         // using variant of single-word Wang/Jenkins hash.
         h += (h <<  15) ^ 0xffffcd7d;
         h ^= (h >>> 10);
         h += (h <<   3);
         h ^= (h >>>  6);
         h += (h <<   2) + (h << 14);
         return h ^ (h >>> 16);
      }

      @Override
      public int size() {
         return size;
      }

      @Override
      public boolean contains(Object o) {
         int mask = table.length - 1;
         int h = hash(o);
         int idx = h;
         while (true) {
            idx = h & mask;
            int hash = hashCodes[idx];
            Object obj = table[idx];
            if (hash == 0 && obj == null) {
               // if we find empty slot, object is not in the table
               return false;
            } else
            if (hash == h && (obj == o || (obj != null && obj.equals(o)))) {
               // found the item
               return true;
            }
            // re-probe
            idx++;
         }
      }
      
      @Override
      public Iterator<E> iterator() {
         return new ReadOnlyIterator<E>() {
            private boolean needNext = true;
            private int next;

            private void findNext() {
               if (needNext) {
                  needNext = false;
                  for (int len = table.length; next < len; next++) {
                     if (hashCodes[next] != 0 || table[next] != null) {
                        return;
                     }
                  }
                  next = -1;
               }
            }
            
            @Override
            public boolean hasNext() {
               findNext();
               return next != -1;
            }

            @Override
            public E next() {
               findNext();
               if (next == -1) {
                  throw new NoSuchElementException();
               }
               needNext = true;
               return (E) table[next++];
            }
         };
      }
   }
}