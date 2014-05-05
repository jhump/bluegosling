package com.apriori.collections;

import com.apriori.tuples.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Utility methods for creating immutable collections and maps and for viewing immutable collections
 * and maps using the standard JCF collection and map interfaces. Standard collection and map views
 * of immutable collections and maps throw {@link UnsupportedOperationException} from all mutation
 * methods.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public final class Immutables {
   private Immutables() {
   }
   
   static final Object EMPTY[] = new Object[0];
   
   /**
    * Returns a view of the given immutable collection as a standard {@link Collection}. The
    * returned collection is a view, not a copy, so it is inexpensive to create. But since the
    * input collection is immutable, the returned collection is indistinguishable from a snapshot.
    * The returned collection throws {@link UnsupportedOperationException} for every operation that
    * would otherwise mutate the collection.
    *
    * @param collection an immutable collection
    * @return a view of the given collection via the standard {@link Collection} interface
    */
   public static <E> Collection<E> asIfMutable(ImmutableCollection<? extends E> collection) {
      return new CollectionFromImmutable<E>(collection);
   }

   /**
    * Returns a view of the given immutable list as a standard {@link List}. The returned list is a
    * view, not a copy, so it is inexpensive to create. But since the input list is immutable, the
    * returned list is indistinguishable from a snapshot. The returned list throws
    * {@link UnsupportedOperationException} for every operation that would otherwise mutate the
    * list.
    *
    * @param list an immutable list
    * @return a view of the given list via the standard {@link List} interface
    */
   public static <E> List<E> asIfMutable(ImmutableList<? extends E> list) {
      return list instanceof RandomAccess
            ? new RandomAccessListFromImmutable<E>(list)
            : new ListFromImmutable<E>(list);
   }

   /**
    * Returns a view of the given immutable set as a standard {@link Set}. The returned set is a
    * view, not a copy, so it is inexpensive to create. But since the input set is immutable, the
    * returned set is indistinguishable from a snapshot. The returned set throws
    * {@link UnsupportedOperationException} for every operation that would otherwise mutate the
    * set.
    *
    * @param set an immutable set
    * @return a view of the given set via the standard {@link Set} interface
    */
   public static <E> Set<E> asIfMutable(ImmutableSet<? extends E> set) {
      return new SetFromImmutable<E>(set);
   }

   /**
    * Returns a view of the given immutable set as a standard {@link NavigableSet}. The returned set
    * is a view, not a copy, so it is inexpensive to create. But since the input set is immutable,
    * the returned set is indistinguishable from a snapshot. The returned set throws
    * {@link UnsupportedOperationException} for every operation that would otherwise mutate the
    * set.
    *
    * @param set an immutable sorted set
    * @return a view of the given set via the standard {@link NavigableSet} interface
    */
   public static <E> NavigableSet<E> asIfMutable(ImmutableSortedSet<? extends E> set) {
      return new NavigableSetFromImmutable<E>(set);
   }
   
   /**
    * Returns an iterator over the given immutable sorted set that visits elements in reverse order,
    * starting with the last (highest) element and ending with the first (lowest).
    *
    * @param set an immutable sorted set
    * @return an iterator that visits elements in descending order
    */
   public static <E> Iterator<E> descendingIterator(final ImmutableSortedSet<E> set) {
      return new Iterator<E>() {
         boolean hasNext = !set.isEmpty();
         E next = hasNext ? set.last() : null; 
         
         @Override
         public boolean hasNext() {
            return hasNext;
         }

         @Override
         public E next() {
            if (!hasNext) {
               throw new NoSuchElementException();
            }
            E ret = next;
            if (isFirst(ret)) {
               hasNext = false;
               next = null;
            } else {
               hasNext = true;
               next = set.lower(next);
            }
            return ret;
         }
         
         private boolean isFirst(E element) {
            E first = set.first();
            Comparator<? super E> comp = set.comparator();
            if (comp == null) {
               comp = CollectionUtils.NATURAL_ORDERING;
            }
            return comp.compare(element,  first) == 0;
         }
      };
   }

   /**
    * Returns a view of the given immutable map as a standard {@link Map}. The returned map is a
    * view, not a copy, so it is inexpensive to create. But since the input map is immutable, the
    * returned map is indistinguishable from a snapshot. The returned map throws
    * {@link UnsupportedOperationException} for every operation that would otherwise mutate the
    * map.
    *
    * @param map an immutable map
    * @return a view of the given map via the standard {@link Map} interface
    */
   public static <K, V> Map<K, V> asIfMutable(final ImmutableMap<? extends K, ? extends V> map) {
      return new MapFromImmutable<K, V>(map);
   }
   
   /**
    * Returns a view of the given immutable map as a standard {@link NavigableMap}. The returned map
    * is a view, not a copy, so it is inexpensive to create. But since the input map is immutable,
    * the returned map is indistinguishable from a snapshot. The returned map throws
    * {@link UnsupportedOperationException} for every operation that would otherwise mutate the
    * map.
    *
    * @param map an immutable sorted map
    * @return a view of the given map via the standard {@link NavigableMap} interface
    */
   public static <K, V> NavigableMap<K, V> asIfMutable(
         ImmutableSortedMap<? extends K, ? extends V> map) {
      return new NavigableMapFromImmutable<K, V>(map);
   }

   /**
    * Returns an iterator over the given immutable sorted map that visits entries in reverse order,
    * starting with the last (highest) key and ending with the first (lowest).
    *
    * @param map an immutable sorted map
    * @return an iterator that visits entries in descending order
    */
   public static <K, V> Iterator<ImmutableMap.Entry<K, V>> descendingIterator(
         ImmutableSortedMap<? extends K, ? extends V> map) {
      final ImmutableSortedMap<K, V> castMap = cast(map);
      return new Iterator<ImmutableMap.Entry<K, V>>() {
         boolean hasNext = !castMap.isEmpty();
         ImmutableMap.Entry<K, V> next = hasNext ? castMap.lastEntry() : null; 
         
         @Override
         public boolean hasNext() {
            return hasNext;
         }

         @Override
         public ImmutableMap.Entry<K, V> next() {
            if (!hasNext) {
               throw new NoSuchElementException();
            }
            ImmutableMap.Entry<K, V> ret = next;
            if (isFirst(ret)) {
               hasNext = false;
               next = null;
            } else {
               hasNext = true;
               next = castMap.lowerEntry(next.key());
            }
            return ret;
         }
         
         private boolean isFirst(ImmutableMap.Entry<K, V> element) {
            ImmutableMap.Entry<K, V> first = castMap.firstEntry();
            Comparator<? super K> comp = castMap.comparator();
            if (comp == null) {
               comp = CollectionUtils.NATURAL_ORDERING;
            }
            return comp.compare(element.key(),  first.key()) == 0;
         }
      };
   }

   /**
    * Returns a view of the given immutable map entry as a standard {@link java.util.Map.Entry}.
    * Since the input entry is immutable, the returned entry is indistinguishable from a snapshot.
    * The returned entry throws {@link UnsupportedOperationException} if
    * {@link java.util.Map.Entry#setValue(Object)} is used.
    *
    * @param entry an immutable map entry
    * @return a view of the given map entry via the standard {@link java.util.Map.Entry} interface
    */
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
   
   /**
    * Safely upcasts the element type of an immutable collection. This operation is safe thanks to
    * the input being immutable. This can help adapt instances of immutable collection to otherwise
    * invariant collection types, without the need for unchecked casts.
    *
    * @param collection an immutable collection
    * @param <E> the element type of the collection
    * @param <S> the source type of the collection being re-cast
    * @param <T> the target type to which the collection is re-cast
    * @return the input collection, but with its element type re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <E, S extends ImmutableCollection<? extends E>, T extends ImmutableCollection<E>>
   T cast(S collection) {
      return (T) collection;
   }

   /**
    * Safely upcasts the key and/or value types of an immutable map. This operation is safe thanks
    * to the input being immutable. This can help adapt instances of immutable map to otherwise
    * invariant map types, without the need for unchecked casts.
    *
    * @param map an immutable map
    * @param <K> the key type of the map
    * @param <V> the value type of the map
    * @param <S> the source type of the map being re-cast
    * @param <T> the target type to which the map is re-cast
    * @return the input map, but with its key or value type (or both) re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <K, V, S extends ImmutableMap<? extends K, ? extends V>, T extends ImmutableMap<K, V>>
   T cast(S map) {
      return (T) map;
   }

   /**
    * Safely upcasts the key and/or value types of an immutable map entry. This operation is safe
    * thanks to the input being immutable. This can help adapt instances of immutable map entry to
    * otherwise invariant entry types, without the need for unchecked casts.
    *
    * @param entry an immutable map entry
    * @param <K> the key type of the map entry
    * @param <V> the value type of the map entry
    * @param <S> the source type of the map entry being re-cast
    * @param <T> the target type to which the map entry is re-cast
    * @return the input entry, but with its key or value type (or both) re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <K, V, S extends ImmutableMap.Entry<? extends K, ? extends V>, T extends ImmutableMap.Entry<K, V>>
   T cast(S entry) {
      return (T) entry;
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
   
   /**
    * Creates a snapshot of the given iterable. For performance, the most compact representation is
    * an array. {@link Collection} and {@link ImmutableCollection} already have {@code toArray}
    * methods that can be used.
    * 
    * <p>Iterables that implement neither of these interfaces must resort to a custom mechanism.
    * Instead of using a growable array, where we have to copy contents on each resize, we use a
    * structure of linked nodes, where each node has an array whose length is the same as all nodes
    * before it (so adding a new node effectively doubles the capacity, just like in standard
    * growable array approaches). The last step is to consolidate the nodes into a single array once
    * the iterable's contents have been exhausted.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an array
    */
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
            current.contents[currentIndex++] = o;
            if (currentIndex == chunkLimit) {
               chunkLimit = size;
               size += currentIndex;
               current.next = new Chunk(chunkLimit);
               current = current.next;
               currentIndex = 0;
            }
         }
         size += currentIndex;
         int lastChunkSize = currentIndex;
         Object elements[] = new Object[size];
         currentIndex = 0;
         for (current = head; current != null; current = current.next) {
            if (current.next == null) {
               // last chunk
               if (lastChunkSize > 0) {
                  System.arraycopy(current.contents, 0, elements, currentIndex, lastChunkSize);
               }
            } else {
               int chunkLength = current.contents.length;
               System.arraycopy(current.contents, 0, elements, currentIndex, chunkLength);
               currentIndex += chunkLength;
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
   
   public static <E> ImmutableSet<E> singletonImmutableSet(E element) {
      return new SingletonSet<E>(element);
   }
   
   @SuppressWarnings("unchecked")
   private static <E> ImmutableSet<E> makeImmutableSet(final Object elements[]) {
      switch (elements.length) {
         case 0:
            return emptyImmutableSet();
         case 1:
            return singletonImmutableSet((E) elements[0]);
         default:
            return new ImmutableSetImpl<E>(elements);
      }
   }
   
   public static <E> ImmutableSet<E> toImmutableSet(Iterable<? extends E> iterable) {
      if (iterable instanceof ImmutableSet) {
         return cast((ImmutableSet<? extends E>) iterable);
      }
      return makeImmutableSet(toArray(iterable));
   }

   public static <E> ImmutableSet<E> toImmutableSet(E... elements) {
      return makeImmutableSet(elements.clone());
   }

   public static <E> ImmutableSortedSet<E> emptyImmutableSortedSet() {
      @SuppressWarnings("unchecked")
      ImmutableSortedSet<E> ret = (ImmutableSortedSet<E>) EmptySortedSet.INSTANCE;
      return ret;
   }

   public static <E> ImmutableSortedSet<E> emptyImmutableSortedSet(Comparator<? super E> comp) {
      @SuppressWarnings("unchecked")
      ImmutableSortedSet<E> ret = (ImmutableSortedSet<E>) new EmptySortedSet(comp);
      return ret;
   }
   
   public static <E> ImmutableSortedSet<E>singletonImmutableSortedSet(E e) {
      // TODO
      return null;
   }

   public static <E> ImmutableSortedSet<E>singletonImmutableSortedSet(Comparator<? super E> comp,
         E e) {
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

   public static <E> ImmutableSortedSet<E> toImmutableSortedSet(Comparator<? super E> comparator,
         SortedSet<? extends E> set) {
      // TODO
      return null;
   }

   public static <K, V> ImmutableMap<K, V> emptyImmutableMap() {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableMap<K, V> singletonImmutableMap(K key, V value) {
      // TODO
      return null;
   }
   
   // Can't use var-args and get type safety for alternating K-then-V types, so we "simulate"
   // using numerous overrides and support up to 5 key-value pairs.
   // (There are also overloads to accept Pair<K,V>s)
   
   @SuppressWarnings("unchecked")
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2) {
      return toImmutableMap(Arrays.asList(Pair.create(key1, value1), Pair.create(key2, value2)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2,
         K key3, V value3) {
      return toImmutableMap(Arrays.asList(Pair.create(key1, value1), Pair.create(key2, value2),
            Pair.create(key3, value3)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2,
         K key3, V value3, K key4, V value4) {
      return toImmutableMap(Arrays.asList(Pair.create(key1, value1), Pair.create(key2, value2),
            Pair.create(key3, value3), Pair.create(key4, value4)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K, V> ImmutableMap<K, V> toImmutableMap(K key1, V value1, K key2, V value2,
         K key3, V value3, K key4, V value4, K key5, V value5) {
      return toImmutableMap(Arrays.asList(Pair.create(key1, value1), Pair.create(key2, value2),
            Pair.create(key3, value3), Pair.create(key4, value4), Pair.create(key5, value5)));
   }
   
   private static <K, V> ImmutableMap<K, V> makeImmutableMap(Object keyValuePairs[]) {
      switch (keyValuePairs.length) {
         case 0:
            return emptyImmutableMap();
         case 1:
            @SuppressWarnings("unchecked")
            Pair<K, V> pair = (Pair<K, V>) keyValuePairs[0];
            return singletonImmutableMap(pair.getFirst(), pair.getSecond());
         default:
            return new ImmutableMapImpl<K, V>(keyValuePairs, true);
      }
   }
   
   public static <E> ImmutableMap<E, E> toImmutableMap(E... keysAndValues) {
      int len = keysAndValues.length;
      if ((len & 1) != 0) {
         throw new IllegalArgumentException("Must specify even number of keys/values");
      }
      @SuppressWarnings("unchecked")
      Pair<E, E> pairs[] = (Pair<E, E>[]) new Pair<?, ?>[len >> 1];
      for (int i = 0; i < len; i += 2) {
         pairs[i] = Pair.create(keysAndValues[i], keysAndValues[i+1]);
      }
      return makeImmutableMap(pairs);
   }

   public static <K, V> ImmutableMap<K, V> toImmutableMap(Pair<K, V>... keysAndValues) {
      return makeImmutableMap(keysAndValues.clone());
   }

   public static <K, V> ImmutableMap<K, V> toImmutableMap(Iterable<Pair<K, V>> keysAndValues) {
      return makeImmutableMap(toArray(keysAndValues));
   }

   public static <K, V> ImmutableMap<K, V> toImmutableMap(Map<? extends K, ? extends V> map) {
      Object entries[] = map.entrySet().toArray();
      switch (entries.length) {
         case 0:
            return emptyImmutableMap();
         case 1:
            @SuppressWarnings("unchecked")
            Map.Entry<K, V> entry = (Map.Entry<K, V>) entries[0];
            return singletonImmutableMap(entry.getKey(), entry.getValue());
         default:
            return new ImmutableMapImpl<K, V>(entries, false);
      }
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> emptyImmutableSortedMap() {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> emptyImmutableSortedMap(
         Comparator<? super K> comp) {
      // TODO
      return null;
   }
   
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> singletonImmutableSortedMap(
         K key, V value) {
      // TODO
      return null;
   }

   public static <K, V> ImmutableSortedMap<K, V> singletonImmutableSortedMap(
         Comparator<? super K> comp, K key, V value) {
      // TODO
      return null;
   }

   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2) {
      return toImmutableSortedMap(Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2, K key3, V value3) {
      return toImmutableSortedMap(Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2), Pair.create(key3, value3)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
      return toImmutableSortedMap(Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2), Pair.create(key3, value3), Pair.create(key4, value4)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
      return toImmutableSortedMap(Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2), Pair.create(key3, value3), Pair.create(key4, value4),
            Pair.create(key5, value5)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2) {
      return toImmutableSortedMap(comparator, Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2, K key3, V value3) {
      return toImmutableSortedMap(comparator, Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2), Pair.create(key3, value3)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2, K key3, V value3,
         K key4, V value4) {
      return toImmutableSortedMap(comparator, Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2), Pair.create(key3, value3), Pair.create(key4, value4)));
   }
   
   @SuppressWarnings("unchecked")
   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Comparator<? super K> comparator, K key1, V value1, K key2, V value2, K key3, V value3,
         K key4, V value4, K key5, V value5) {
      return toImmutableSortedMap(comparator, Arrays.asList(Pair.create(key1, value1),
            Pair.create(key2, value2), Pair.create(key3, value3), Pair.create(key4, value4),
            Pair.create(key5, value5)));
   }
   
   public static <E extends Comparable<E>> ImmutableSortedMap<E, E> toImmutableSortedMap(
         E... keysAndValues) {
      int len = keysAndValues.length;
      if ((len & 1) != 0) {
         throw new IllegalArgumentException("Must specify even number of keys/values");
      }
      ArrayList<Pair<E, E>> pairs = new ArrayList<Pair<E, E>>(len >> 1);
      for (int i = 0; i < len; i += 2) {
         pairs.add(Pair.create(keysAndValues[i], keysAndValues[i+1]));
      }
      // TODO
      return null;
   }
   
   public static <E> ImmutableSortedMap<E, E> toImmutableSortedMap(Comparator<? super E> comp,
         E... keysAndValues) {
      int len = keysAndValues.length;
      if ((len & 1) != 0) {
         throw new IllegalArgumentException("Must specify even number of keys/values");
      }
      ArrayList<Pair<E, E>> pairs = new ArrayList<Pair<E, E>>(len >> 1);
      for (int i = 0; i < len; i += 2) {
         pairs.add(Pair.create(keysAndValues[i], keysAndValues[i+1]));
      }
      // TODO
      return null;
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Pair<K, V>... keysAndValues) {
      return toImmutableSortedMap(Arrays.asList(keysAndValues));
   }

   public static <K extends Comparable<K>, V> ImmutableSortedMap<K, V> toImmutableSortedMap(
         Iterable<Pair<K, V>> keysAndValues) {
      // TODO
      return null;
   }
   
   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(Comparator<? super K> comp,
         Pair<K, V>... keysAndValues) {
      return toImmutableSortedMap(comp, Arrays.asList(keysAndValues));
   }

   public static <K, V> ImmutableSortedMap<K, V> toImmutableSortedMap(Comparator<? super K> comp,
         Iterable<Pair<K, V>> keysAndValues) {
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
      if (map instanceof SortedMap) {
         SortedMap<? extends K, ? extends V> sortedMap = (SortedMap<? extends K, ? extends V>) map;
         if (comparator == null ? sortedMap.comparator() == null
               : comparator.equals(sortedMap.comparator())) {
            return toImmutableSortedMap((SortedMap<? extends K, ? extends V>) map);
         }
      }
      // TODO
      return null;
   }

   public static <K, V> ImmutableMap.Entry<K, V> toImmutableMapEntry(
         Map.Entry<? extends K, ? extends V> entry) {
      return Immutables.<K, V>toImmutableMapEntry(entry.getKey(), entry.getValue());
   }

   public static <K, V> ImmutableMap.Entry<K, V> toImmutableMapEntry(K key, V value) {
      return new AbstractImmutableMap.SimpleEntry<K, V>(key, value);
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

   private static class NavigableSetFromImmutable<E> extends SetFromImmutable<E>
         implements NavigableSet<E> {
      NavigableSetFromImmutable(ImmutableSortedSet<? extends E> set) {
         super(set);
      }
      
      private ImmutableSortedSet<E> getSet() {
         return cast((ImmutableSortedSet<? extends E>) collection);
      }

      @Override
      public Comparator<? super E> comparator() {
         return getSet().comparator();
      }

      @Override
      public E first() {
         return getSet().first();
      }

      @Override
      public E last() {
         return getSet().last();
      }

      @Override
      public E lower(E e) {
         return getSet().lower(e);
      }

      @Override
      public E floor(E e) {
         return getSet().floor(e);
      }

      @Override
      public E ceiling(E e) {
         return getSet().ceil(e);
      }

      @Override
      public E higher(E e) {
         return getSet().higher(e);
      }

      @Override
      public E pollFirst() {
         throw new UnsupportedOperationException();
      }

      @Override
      public E pollLast() {
         throw new UnsupportedOperationException();
      }

      @Override
      public NavigableSet<E> descendingSet() {
         return new DescendingSet<E>(this);
      }

      @Override
      public Iterator<E> descendingIterator() {
         return Immutables.descendingIterator(getSet());
      }

      @Override
      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
            boolean toInclusive) {
         return new NavigableSetFromImmutable<E>(getSet().subSet(fromElement,  fromInclusive,
               toElement, toInclusive));
      }

      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return new NavigableSetFromImmutable<E>(getSet().headSet(toElement, inclusive));
      }

      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return new NavigableSetFromImmutable<E>(getSet().tailSet(fromElement, inclusive));
      }

      @Override
      public SortedSet<E> subSet(E fromElement, E toElement) {
         return new NavigableSetFromImmutable<E>(getSet().subSet(fromElement, toElement));
      }

      @Override
      public SortedSet<E> headSet(E toElement) {
         return new NavigableSetFromImmutable<E>(getSet().headSet(toElement));
      }

      @Override
      public SortedSet<E> tailSet(E fromElement) {
         return new NavigableSetFromImmutable<E>(getSet().tailSet(fromElement));
      }
   }

   private static class MapFromImmutable<K, V> implements Map<K, V> {

      final ImmutableMap<? extends K, ? extends V> map;

      MapFromImmutable(ImmutableMap<? extends K, ? extends V> map) {
         this.map = map;
      }

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
               asIfMutable(castMap.entrySet()), (entry) -> asIfMutable(entry));
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
   
   private static class NavigableMapFromImmutable<K, V> extends MapFromImmutable<K, V>
         implements NavigableMap<K, V> {

      NavigableMapFromImmutable(ImmutableSortedMap<? extends K, ? extends V> map) {
         super(map);
      }
      
      private ImmutableSortedMap<K, V> getMap() {
         return cast((ImmutableSortedMap<? extends K, ? extends V>) map);
      }

      @Override
      public Comparator<? super K> comparator() {
         return getMap().comparator();
      }

      private K key(ImmutableMap.Entry<K, V> entry) {
         return entry == null ? null : entry.key();
      }
      
      private Entry<K, V> entry(ImmutableMap.Entry<K, V> entry) {
         return entry == null ? null : asIfMutable(entry);
      }
      
      @Override
      public K firstKey() {
         return key(getMap().firstEntry());
      }

      @Override
      public K lastKey() {
         return key(getMap().lastEntry());
      }

      @Override
      public Map.Entry<K, V> lowerEntry(K key) {
         return entry(getMap().lowerEntry(key));
      }

      @Override
      public K lowerKey(K key) {
         return key(getMap().lowerEntry(key));
      }

      @Override
      public Entry<K, V> floorEntry(K key) {
         return entry(getMap().floorEntry(key));
      }

      @Override
      public K floorKey(K key) {
         return key(getMap().floorEntry(key));
      }

      @Override
      public Entry<K, V> ceilingEntry(K key) {
         return entry(getMap().ceilEntry(key));
      }

      @Override
      public K ceilingKey(K key) {
         return key(getMap().ceilEntry(key));
      }

      @Override
      public Entry<K, V> higherEntry(K key) {
         return entry(getMap().higherEntry(key));
      }

      @Override
      public K higherKey(K key) {
         return key(getMap().higherEntry(key));
      }

      @Override
      public Entry<K, V> firstEntry() {
         return entry(getMap().firstEntry());
      }

      @Override
      public Entry<K, V> lastEntry() {
         return entry(getMap().lastEntry());
      }

      @Override
      public Entry<K, V> pollFirstEntry() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Entry<K, V> pollLastEntry() {
         throw new UnsupportedOperationException();
      }

      @Override
      public NavigableMap<K, V> descendingMap() {
         return new DescendingMap<K, V>(this);
      }

      @Override
      public NavigableSet<K> navigableKeySet() {
         return asIfMutable(getMap().keySet());
      }

      @Override
      public NavigableSet<K> descendingKeySet() {
         return descendingMap().navigableKeySet();
      }

      @Override
      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         return new NavigableMapFromImmutable<K, V>(getMap().subMap(fromKey, fromInclusive, toKey,
               toInclusive));
      }

      @Override
      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         return new NavigableMapFromImmutable<K, V>(getMap().headMap(toKey, inclusive));
      }

      @Override
      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         return new NavigableMapFromImmutable<K, V>(getMap().tailMap(fromKey, inclusive));
      }

      @Override
      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return new NavigableMapFromImmutable<K, V>(getMap().subMap(fromKey, toKey));
      }

      @Override
      public SortedMap<K, V> headMap(K toKey) {
         return new NavigableMapFromImmutable<K, V>(getMap().headMap(toKey));
      }

      @Override
      public SortedMap<K, V> tailMap(K fromKey) {
         return new NavigableMapFromImmutable<K, V>(getMap().tailMap(fromKey));
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
      @SuppressWarnings("hiding")
      static final EmptyList INSTANCE = new EmptyList();

      @SuppressWarnings("synthetic-access")
      private EmptyList() {
      }
      
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
      @SuppressWarnings("hiding")
      static final EmptySet INSTANCE = new EmptySet();
      
      @SuppressWarnings("synthetic-access")
      private EmptySet() {
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

   private static class EmptySortedSet extends EmptySet implements ImmutableSortedSet<Object> {
      @SuppressWarnings("hiding")
      static final EmptySortedSet INSTANCE = new EmptySortedSet(null);
      
      private final Comparator<Object> comp;
      
      @SuppressWarnings({"synthetic-access", "unchecked"})
      EmptySortedSet(Comparator<?> comp) {
         this.comp = (Comparator<Object>) comp;
      }
      
      @Override
      public Comparator<? super Object> comparator() {
         return comp;
      }
      
      @Override
      public Object first() {
         throw new NoSuchElementException();
      }


      @Override
      public ImmutableSortedSet<Object> rest() {
         return this;
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
         return this;
      }

      @Override
      public ImmutableSortedSet<Object> subSet(Object from, boolean fromInclusive, Object to,
            boolean toInclusive) {
         return this;
      }

      @Override
      public ImmutableSortedSet<Object> headSet(Object to) {
         return this;
      }

      @Override
      public ImmutableSortedSet<Object> headSet(Object to, boolean inclusive) {
         return this;
      }

      @Override
      public ImmutableSortedSet<Object> tailSet(Object from) {
         return this;
      }

      @Override
      public ImmutableSortedSet<Object> tailSet(Object from, boolean inclusive) {
         return this;
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

      @SuppressWarnings("unchecked")
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
         @SuppressWarnings("unchecked")
         ImmutableList<E> ret = from == to ? (ImmutableList<E>) EmptyList.INSTANCE : this;
         return ret;
      }

      @Override
      public E first() {
         return element;
      }

      @Override
      public ImmutableList<E> rest() {
         return this;
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
         @SuppressWarnings("unchecked")
         E ret = (E) elements[i];
         return ret;
      }
   }
   
   static int hash(Object e) {
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

   private static class ImmutableSetImpl<E> extends AbstractImmutableSet<E> {
      // TODO: benchmark using arbitrary table sizes (e.g. 1.5 * num entries) and mod is too slow
      // compared to using power-of-2 table sizes and bitwise and. The former would be nice since
      // it means less memory bloat as the latter could yield a table with a low load factor because
      // the number of entries was just beyond the comfort level for the next smallest power of 2...
      
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
         return new Iterator<E>() {
            private boolean needNext = true;
            private int next;

            @SuppressWarnings("synthetic-access")
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

            @SuppressWarnings("synthetic-access")
            @Override
            public E next() {
               findNext();
               if (next == -1) {
                  throw new NoSuchElementException();
               }
               needNext = true;
               @SuppressWarnings("unchecked")
               E ret = (E) table[next++];
               return ret;
            }
         };
      }
   }

   private static class ImmutableMapImpl<K, V> extends AbstractImmutableMap<K, V> {
      private final Object keyTable[];
      private final Object valueTable[];
      private final int hashCodes[];
      private final int size;

      ImmutableMapImpl(Object entries[], boolean pairs) {
         assert entries.length > 0;
         this.size = entries.length;
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
         keyTable = new Object[tableSize];
         valueTable = new Object[tableSize];
         hashCodes = new int[tableSize];
         for (Object o : entries) {
            K k; V v;
            if (pairs) {
               @SuppressWarnings("unchecked")
               Pair<K, V> pair = (Pair<K, V>) o;
               k = pair.getFirst();
               v = pair.getSecond();
            } else {
               @SuppressWarnings("unchecked")
               Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
               k = entry.getKey();
               v = entry.getValue();
            }
            int h = hash(k);
            int idx = h;
            while (true) {
               idx = h & mask;
               int hash = hashCodes[idx];
               Object key = keyTable[idx];
               if (hash == 0 && key == null) {
                  // found an empty slot
                  hashCodes[idx] = h;
                  keyTable[idx] = k;
                  valueTable[idx] = v;
                  break;
               } else
               if (hash == h && (key == k || (key != null && key.equals(k)))) {
                  // item already exists in the table
                  continue;
               }
               // re-probe
               idx++;
            }
         }
      }
      
      @Override
      public int size() {
         return size;
      }

      @Override
      public boolean containsKey(Object o) {
         int mask = keyTable.length - 1;
         int h = hash(o);
         int idx = h;
         while (true) {
            idx = h & mask;
            int hash = hashCodes[idx];
            Object key = keyTable[idx];
            if (hash == 0 && key == null) {
               // if we find empty slot, object is not in the table
               return false;
            } else
            if (hash == h && (key == o || (key != null && key.equals(o)))) {
               // found the item
               return true;
            }
            // re-probe
            idx++;
         }
      }
      
      @Override
      public V get(Object o) {
         int mask = keyTable.length - 1;
         int h = hash(o);
         int idx = h;
         while (true) {
            idx = h & mask;
            int hash = hashCodes[idx];
            Object key = keyTable[idx];
            if (hash == 0 && key == null) {
               // if we find empty slot, object is not in the table
               return null;
            } else
            if (hash == h && (key == o || (key != null && key.equals(o)))) {
               // found the item
               @SuppressWarnings("unchecked")
               V ret = (V) valueTable[idx];
               return ret;
            }
            // re-probe
            idx++;
         }
      }
      
      @Override
      public Iterator<Entry<K, V>> iterator() {
         return new Iterator<Entry<K, V>>() {
            private boolean needNext = true;
            private int next;

            @SuppressWarnings("synthetic-access")
            private void findNext() {
               if (needNext) {
                  needNext = false;
                  for (int len = keyTable.length; next < len; next++) {
                     if (hashCodes[next] != 0 || keyTable[next] != null) {
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

            @SuppressWarnings("synthetic-access")
            @Override
            public Entry<K, V> next() {
               findNext();
               if (next == -1) {
                  throw new NoSuchElementException();
               }
               needNext = true;
               @SuppressWarnings("unchecked")
               K key = (K) keyTable[next];
               @SuppressWarnings("unchecked")
               V val = (V) valueTable[next++];
               return new SimpleEntry<K, V>(key, val);
            }
         };
      }
   }
}