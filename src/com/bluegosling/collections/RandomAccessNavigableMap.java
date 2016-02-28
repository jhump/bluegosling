package com.bluegosling.collections;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableMap;
import java.util.RandomAccess;

/**
 * A navigable map that provides random access over its elements. The entries in the map are
 * ordered according to the map's {@link Comparator} or (if there is none) according to the
 * keys' {@linkplain Comparable natural ordering}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @see RandomAccessNavigableSet
 */
public interface RandomAccessNavigableMap<K, V> extends NavigableMap<K, V> {

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access set.
    */
   @Override default RandomAccessSet<K> keySet() {
      return navigableKeySet();
   }

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a list (not just a collection). The returned list should
    * also implement {@link RandomAccess}.
    */
   @Override List<V> values(); // returned List also implements RandomAccess

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access set.
    */
   @Override RandomAccessSet<Entry<K, V>> entrySet();
   
   /**
    * Returns a list iterator that can iterator backwards and forwards over the set of mappings.
    * Being a list iterator, it also provides random access indices for the mappings.
    *
    * @return a list iterator over the mappings in this map
    */
   ListIterator<Entry<K, V>> listIterator();

   /**
    * Returns a list iterator that can iterator backwards and forwards over the set of mappings,
    * starting at a given index into the set. Indices are zero-based, so an index of zero starts
    * with the first mapping, an index of one starts with the second, and so on.
    *
    * @param index a random access index
    * @return a list iterator over the mappings in this map, starting from the given index
    * @throws IndexOutOfBoundsException if the given index is less than zero or greater than or
    *       equal to the size of this map
    */
   ListIterator<Entry<K, V>> listIterator(int index);
   
   /**
    * Gets an entry from the map by random access index. Indices are zero-based, so an index of zero
    * starts with the first mapping, an index of one starts with the second, and so on.
    *
    * @param index a random access index
    * @return the mapping at the given index
    * @throws IndexOutOfBoundsException if the given index is less than zero or greater than or
    *       equal to the size of this map
    */
   Entry<K, V> getEntry(int index);
   
   /**
    * Removes an entry from the map by random access index. Indices are zero-based, so an index of
    * zero starts with the first mapping, an index of one starts with the second, and so on.
    *
    * @param index a random access index
    * @return the mapping that was removed from the given index
    * @throws IndexOutOfBoundsException if the given index is less than zero or greater than or
    *       equal to the size of this map
    */
   Entry<K, V> removeEntry(int index);
   
   /**
    * Determines the random access index at which the given key is found in the map.
    *
    * @param key a key
    * @return the index at which the given key is found or -1 if the key is not present
    */
   int indexOfKey(Object key);
   
   /**
    * Returns a view of a region of this map. Unlike {@link #subMap}, this bounds of this region are
    * defined by random access indices instead of key values.
    *
    * @param startIndex the starting index, inclusive, of the region
    * @param endIndex the ending index, exclusive, of the region
    * @return a view of a region of this map between the given indices
    */
   RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex);
   
   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> descendingMap();

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access set.
    */
   @Override RandomAccessNavigableSet<K> navigableKeySet();

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access set.
    */
   @Override RandomAccessNavigableSet<K> descendingKeySet();

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
         K toKey, boolean toInclusive);

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive);

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive);

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey);

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> headMap(K toKey);

   /**
    * {@inheritDoc}
    * <p>Overridden to co-variantly return a random access map.
    */
   @Override RandomAccessNavigableMap<K, V> tailMap(K fromKey);
}
