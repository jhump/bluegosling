package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An abstract base class for {@link NavigableMap} implementations. Concrete classes only need to
 * implement the following methods, most of which perform search operations:
 * <ul>
 *    <li>{@link #size()}</li>
 *    <li>{@link #firstEntry()}</li>
 *    <li>{@link #lastEntry()}</li>
 *    <li>{@link #getEntry(Object) getEntry(K)}</li>
 *    <li>{@link #lowerEntry(Object) lowerEntry(K)}</li>
 *    <li>{@link #higherEntry(Object) higherEntry(K)}</li>
 *    <li>{@link #put(Object, Object) put(K, V)}</li>
 *    <li>{@link #removeEntry(Object)}</li>
 * </ul>
 * This abstract class then implements everything else, including {@link #keySet()} and
 * {@link #entrySet()}, in terms of those operations.
 * 
 * <p>This class is not thread-safe. So most of the method implementations herein cannot be used to
 * correctly implement a {@link java.util.concurrent.ConcurrentNavigableMap ConcurrentNavigableMap}.
 * 
 * <p>Because the {@link NavigableMap} interface is so much broader (and thus different) than the
 * plain {@link Map} interface, this class (and its usage by sub-classes) bear little resemblance to
 * the JRE's {@link java.util.AbstractMap AbstractMap} base class. As such, this class does
 * <em>not</em> extend {@link java.util.AbstractMap AbstractMap}.
 * 
 * <p>Although this base class does not implement {@link Serializable}, it does provide basic
 * support for serialization to sub-classes. Mainly: sub-classes do not need to worry about
 * serializing and deserializing the map's {@linkplain #comparator() comparator}.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests!
// TODO: test serialization support
public abstract class AbstractNavigableMap<K, V> implements NavigableMap<K, V> {
   
   /**
    * The map's current comparator. This will never be null. If no comparator is specified during
    * construction then this is set to a default implementation that compares objects using their
    * {@linkplain Comparable natural ordering}.
    */
   protected transient Comparator<? super K> comparator;
   
   /**
    * A monotonically increasing count of modifications. This is used to provide best effort
    * detection of concurrent modification (for failing fast).
    */
   protected transient int modCount;
   
   /**
    * Constructs a new, empty map that orders keys according to their {@linkplain Comparable natural
    * ordering}.
    */
   protected AbstractNavigableMap() {
      this(null);
   }
   
   /**
    * Constructs a new, empty map that orders keys using the specified comparator.
    * 
    * @param comparator determines ordering of keys in the map
    */
   protected AbstractNavigableMap(Comparator<? super K> comparator) {
      if (comparator == null) {
         this.comparator = CollectionUtils.NATURAL_ORDERING;
      } else {
         this.comparator = comparator;
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This implementation returns the comparator specified during construction or {@code null} if
    * elements are sorted according to their natural ordering.
    */
   @Override
   public Comparator<? super K> comparator() {
      return comparator == CollectionUtils.NATURAL_ORDERING ? null : comparator;
   }
   
   private K key(Entry<K, V> entry) {
      return entry == null ? null : entry.getKey();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #firstEntry()} and, if non-null, returns
    * the entry's key.
    */
   @Override
   public K firstKey() {
      return key(firstEntry());
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #lastEntry()} and, if non-null, returns
    * the entry's key.
    */
   @Override
   public K lastKey() {
      return key(lastEntry());
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation delegates to {@link #navigableKeySet()}.
    */
   @Override
   public Set<K> keySet() {
      return navigableKeySet();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns a {@link ValueCollection}.
    */
   @Override
   public Collection<V> values() {
      return new ValueCollection(); 
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns an {@link EntrySet}.
    */
   @Override
   public Set<Entry<K, V>> entrySet() {
      return new EntrySet();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns true when {@link #size()}{@code == 0}.
    */
   @Override
   public boolean isEmpty() {
      return size() == 0;
   }
   
   /**
    * Finds an entry for a specified key. If the key does not exist in the map then {@code null}
    * is returned.
    * 
    * @param key the key
    * @return the entry in the map for the specified key or {@code null} if one does not exist
    * @throws ClassCastException if the given key must be cast to compare to existing keys and
    *       is not of a valid type
    */
   protected abstract Entry<K, V> getEntry(Object key);

   /**
    * Removes a mapping for the specified key and returns the removed entry. If the key does not
    * exist in the map then {@code null} is returned.
    * 
    * @param key the key
    * @return the entry in the map that was removed or {@code null} if no such entry exists
    * @throws ClassCastException if the given key must be cast to compare to existing keys and
    *       is not of a valid type
    */
   protected abstract Entry<K, V> removeEntry(Object key);

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns true if {@link #getEntry(Object) getEntry(key)}
    * {@code != null}.
    */
   @Override
   public boolean containsKey(Object key) {
      return getEntry(key) != null;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns {@link #values()}{@code .contains(value)}. 
    */
   @Override
   public boolean containsValue(Object value) {
      return values().contains(value);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation uses {@link #getEntry(Object)} and returns the associated value
    * if an entry is found.
    */
   @Override
   public V get(Object key) {
      Entry<K, V> entry = getEntry(key);
      return entry == null ? null : entry.getValue();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation uses {@link #removeEntry(Object)} and returns the value of the
    * removed entry or {@code null} if the key did not exist in the map.
    */
   @Override
   public V remove(Object key) {
      Entry<K, V> entry = removeEntry(key);
      return entry == null ? null : entry.getValue();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply loops over all entries in the specified map and calls
    * {@link #put(Object, Object)} for each one.
    */
   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
         put(entry.getKey(), entry.getValue());
      }
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #pollFirstEntry()} until the map is empty.
    */
   @Override
   public void clear() {
      while (!isEmpty()) {
         pollFirstEntry();
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #lowerEntry(Object)} and, if non-null,
    * returns the entry's key.
    */
   @Override
   public K lowerKey(K key) {
      return key(lowerEntry(key));
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation uses {@link #getEntry(Object)}. If no such entry is found then
    * it calls {@link #lowerEntry(Object)} and returns that entry.
    */
   @Override
   public Entry<K, V> floorEntry(K key) {
      Entry<K, V> ret = getEntry(key);
      return ret == null ? lowerEntry(key) : ret;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #floorEntry(Object)} and, if non-null,
    * returns the entry's key.
    */
   @Override
   public K floorKey(K key) {
      return key(floorEntry(key));
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation uses {@link #getEntry(Object)}. If no such entry is found then
    * it calls {@link #higherEntry(Object)} and returns that entry.
    */
   @Override
   public Entry<K, V> ceilingEntry(K key) {
      Entry<K, V> ret = getEntry(key);
      return ret == null ? higherEntry(key) : ret;
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #ceilingEntry(Object)} and, if non-null,
    * returns the entry's key.
    */
   @Override
   public K ceilingKey(K key) {
      return key(ceilingEntry(key));
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #higherEntry(Object)} and, if non-null,
    * returns the entry's key.
    */
   @Override
   public K higherKey(K key) {
      return key(higherEntry(key));
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation queries for the first entry via {@link #firstEntry()} and
    * returns it. If non-null, it calls {@link #removeEntry(Object)} for the entry's key before
    * returning.
    */
   @Override
   public Entry<K, V> pollFirstEntry() {
      Entry<K, V> entry = firstEntry();
      if (entry == null) {
         return null;
      }
      removeEntry(entry.getKey());
      return entry;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation queries for the last entry via {@link #lastEntry()} and
    * returns it. If non-null, it calls {@link #removeEntry(Object)} for the entry's key before
    * returning.
    */
   @Override
   public Entry<K, V> pollLastEntry() {
      Entry<K, V> entry = lastEntry();
      if (entry == null) {
         return null;
      }
      removeEntry(entry.getKey());
      return entry;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation should suffice for any {@link NavigableMap} since the map
    * provides all necessary operations to trivially reverse the order. It simply uses inverse
    * operations to return the descending view of this map. For example, the inverse of
    * {@link #firstEntry()} is {@link #lastEntry()}, the inverse of the key set's
    * {@link NavigableSet#iterator() iterator()} is its {@link NavigableSet#descendingIterator() 
    * descendingIterator()}, etc.
    */
   @Override
   public NavigableMap<K, V> descendingMap() {
      return new DescendingMap<K, V>(this);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns a {@link KeySet}.
    */
   @Override
   public NavigableSet<K> navigableKeySet() {
      return new KeySet();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns {@link #navigableKeySet()}{@code .descendingSet()}.
    */
   @Override
   public NavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns a {@link SubMap} with the specified bounds.
    */
   @Override
   public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      if (comparator.compare(fromKey, toKey) > 0) {
         throw new IllegalArgumentException("fromKey (" + fromKey + ") > toKey (" + toKey + ")");
      }
      return new SubMap(fromKey, fromInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
            toKey, toInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns a {@link SubMap} with the specified upper bound and
    * {@linkplain BoundType#NO_BOUND no lower bound}.
    */
   @Override
   public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return new SubMap(null, BoundType.NO_BOUND, toKey,
            inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation returns a {@link SubMap} with the specified lower bound and
    * {@linkplain BoundType#NO_BOUND no upper bound}.
    */
   @Override
   public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new SubMap(fromKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE, null,
            BoundType.NO_BOUND);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #subMap(Object, boolean, Object, boolean)
    * subMap(fromKey, true, toKey, false)}. The return type is also overridden to provide access to
    * the full {@link NavigableMap} interface of the returned view.
    */
   @Override
   public NavigableMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #headMap(Object, boolean)
    * headMap(toKey, false)}. The return type is also overridden to provide access to
    * the full {@link NavigableMap} interface of the returned view.
    */
   @Override
   public NavigableMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation simply calls {@link #tailMap(Object, boolean)
    * tailMap(fromKey, true)}. The return type is also overridden to provide access to
    * the full {@link NavigableMap} interface of the returned view.
    */
   @Override
   public NavigableMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
   }
   
   /**
    * Returns the map's modification count. This is used by iterator implementations to detect
    * concurrent modification and fail fast. Sub-classes usually will access the protected field
    * {@link #modCount} instead of using this method. However, iterator implementations should
    * always use this method and <em>not</em> directly access the field. This is because sub-classes
    * ({@link SubMap#getModCount() SubMap} in particular) are allowed to override this method.
    * 
    * @return the map's modification count
    */
   protected int getModCount() {
      return modCount;
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

   /**
    * Customizes de-serialization to properly resolve the comparator. Though this class is not
    * serializable, this is provided for the benefit of sub-classes that are.
    * 
    * @param in the stream from which the object is being de-serialized
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serialization fails to locate a required type
    */
   @SuppressWarnings("unchecked") // no choice but to cast when reading from input stream (should be ok)
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      comparator = (Comparator<? super K>) in.readObject();
      if (comparator == null) {
         comparator = CollectionUtils.NATURAL_ORDERING;
      }
   }
   
   /**
    * Customizes serialization by writing the maps' comparator. Though this class is not
    * serializable, this is provided for the benefit of sub-classes that are.
    * 
    * @param out the stream to which the object is being serialized
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      // this will record null if using CollectionUtils.NATURAL_ORDERING
      out.writeObject(comparator());
   }
   
   /**
    * The type of bound for a {@link SubMap}. Sub-map views have both lower and upper bounds. These
    * bounds are optional and, if present, can be inclusive or exclusive.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected enum BoundType {
      /**
       * Indicates that a given bound is inclusive.
       */
      INCLUSIVE,

      /**
       * Indicates that a given bound is exclusive.
       */
      EXCLUSIVE,
      
      /**
       * Indicates that a given bound is absent.
       */
      NO_BOUND
   }

   /**
    * A sub-map view, with bounds on the range of included keys. This implementation defines its
    * operations in terms of operations on the main map. Sub-classes rarely need to extend this
    * class, but may do so if (for example) they can provide more efficient implementations for some
    * operations.
    * 
    * <p>For example, overriding the {@link Iterator} implementation for a map's {@link
    * #navigableKeySet()} can often be done more efficiently (depending on the exact data structures
    * used). If it is overridden in the main map implementation (likely by extending {@link KeySet}
    * and overriding the map's {@link #navigableKeySet()} method) then it is also prudent to extend
    * this class and override in the same way so that sub-map views can also take advantage of the
    * faster implementation.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class SubMap extends AbstractNavigableMap<K, V> {
      
      private final K lowerBound;
      private final BoundType lowerBoundType;
      private final K upperBound;
      private final BoundType upperBoundType;
      private int size = -1;
      
      /**
       * Constructs a new sub-map view with the given bounds.
       * 
       * @param lowerBound the lower bound, ignored when
       *       {@code lowerBoundType == }{@link BoundType#NO_BOUND}
       * @param lowerBoundType the lower bound type
       * @param upperBound the upper bound, ignored when
       *       {@code upperBoundType == }{@link BoundType#NO_BOUND}
       * @param upperBoundType the upper bound type
       */
      protected SubMap(K lowerBound, BoundType lowerBoundType, K upperBound,
            BoundType upperBoundType) {
         super(AbstractNavigableMap.this.comparator);
         this.lowerBound = lowerBound;
         this.lowerBoundType = lowerBoundType;
         this.upperBound = upperBound;
         this.upperBoundType = upperBoundType;
      }

      /**
       * Returns true if the specified key is within the sub-map's bounds.
       * 
       * @param key a key
       * @return true if the key is within the sub-map's bounds
       */
      protected boolean isInRange(Object key) {
         // unchecked cast may cause ClassCastException in comparisons but that's acceptable here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         
         if (lowerBoundType == BoundType.NO_BOUND) {
            return CollectionUtils.isInRangeHigh(k, true, upperBound,
                  upperBoundType == BoundType.INCLUSIVE, comparator);
         } else if (upperBoundType == BoundType.NO_BOUND) {
            return CollectionUtils.isInRangeLow(k, true, lowerBound,
                  lowerBoundType == BoundType.INCLUSIVE, comparator);
         } else {
            return CollectionUtils.isInRange(k, lowerBound, lowerBoundType == BoundType.INCLUSIVE,
                  upperBound, upperBoundType == BoundType.INCLUSIVE, comparator);
         }
      }
      
      /**
       * {@inheritDoc}
       * 
       * <p>This checks if the key is in range and returns {@code null} if not. If it is in range,
       * then the main map's {@link #getEntry(Object)} method is called.
       */
      @Override
      protected Entry<K, V> getEntry(Object key) {
         return isInRange(key) ? AbstractNavigableMap.this.getEntry(key) : null;
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This checks if the key is in range and returns {@code null} if not. If it is in range,
       * then the main map's {@link #removeEntry(Object)} method is called.
       */
      @Override
      protected Entry<K, V> removeEntry(Object key) {
         return isInRange(key) ? AbstractNavigableMap.this.removeEntry(key) : null;
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This implementation actually counts the number of entries within the sub-map's bounds
       * using the {@link #entrySet()}'s iterator. This runs in linear time, O(n), but the result
       * is memo-ized between calls. It will only count the number of entries again if it detects
       * that the main map's {@link #modCount} has changed since the last time the entries were
       * counted.
       */
      @Override
      public int size() {
         if (size == -1 || modCount != AbstractNavigableMap.this.modCount) {
            int sz = 0;
            for (Iterator<Entry<K, V>> iter = entrySet().iterator(); iter.hasNext(); ) {
               iter.next();
               sz++;
            }
            size = sz;
            modCount = AbstractNavigableMap.this.modCount;
         }
         return size;
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This checks if the key is in range and throws an {@link IllegalArgumentException} if
       * not. If it is in range, then the main map's {@link #put(Object, Object)} method is called.
       */
      @Override
      public V put(K key, V value) {
         if (!isInRange(key)) {
            throw new IllegalArgumentException("Key " + key + " outside of submap range");
         }
         return AbstractNavigableMap.this.put(key,  value);
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This checks that if <em>all</em> keys in the specified map are in range and throws an
       * {@link IllegalArgumentException} if any of them are not. If all keys are in range, then the
       * main map's {@link #putAll(Map)} method is called.
       */
      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         for (K k : m.keySet()) {
            if (!isInRange(k)) {
               throw new IllegalArgumentException("Key " + k + " outside of submap range");
            }
         }
         AbstractNavigableMap.this.putAll(m);
      }

      /**
       * Removes all elements within the sub-map's bounds.
       * 
       * <p>This uses the same implementation as the {@linkplain AbstractNavigableMap#clear()
       * default}. It also updates its memo-ized size to zero (so {@link #size()} will not need to
       * count elements unless items are actually added to the map).
       */
      @Override
      public void clear() {
         super.clear();
         size = 0;
         modCount = AbstractNavigableMap.this.modCount;
      }

      /**
       * Provides a new sub-map view (sub-sub-map?). If the specified bounds are not within this
       * sub-map's bounds then a {@link IllegalArgumentException} is thrown. This delegates to the
       * main map's implementation after range-checking the specified bounds.
       */
      @Override
      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         if (!isInRange(toKey)) {
            throw new IllegalArgumentException("to key " + toKey + " is outside sub-map range");
         }
         if (!isInRange(fromKey)) {
            throw new IllegalArgumentException("from key " + fromKey + " is outside sub-map range");
         }
         if (comparator.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("fromKey (" + fromKey + ") > toKey (" + toKey + ")");
         }
         return AbstractNavigableMap.this.subMap(fromKey, fromInclusive, toKey, toInclusive);
      }

      /**
       * Provides a new view of the head of this sub-map. If the specified upper bound is not within
       * this sub-map's bounds then a {@link IllegalArgumentException} is thrown. This delegates to
       * the main map's implementation after range-checking the specified bound.
       * 
       * <p>If this sub-map has no lower bound, then the main map's {@link #headMap(Object, boolean)}
       * method is used. If this sub-map <em>does</em> have a lower bound, then the main map's
       * {@link #subMap(Object, boolean, Object, boolean)} method is used and supplied this
       * sub-map's lower bound and the specified upper bound.
       */
      @Override
      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         if (!isInRange(toKey)) {
            throw new IllegalArgumentException("to key " + toKey + " is outside sub-map range");
         }
         return lowerBoundType == BoundType.NO_BOUND
               ? AbstractNavigableMap.this.headMap(toKey, inclusive)
               : AbstractNavigableMap.this.subMap(lowerBound, lowerBoundType == BoundType.INCLUSIVE,
                     toKey, inclusive);
      }

      /**
       * Provides a new view of the tail of this sub-map. If the specified lower bound is not within
       * this sub-map's bounds then a {@link IllegalArgumentException} is thrown. This delegates to
       * the main map's implementation after range-checking the specified bound.
       * 
       * <p>If this sub-map has no upper bound, then the main map's {@link #tailMap(Object, boolean)}
       * method is used. If this sub-map <em>does</em> have an upper bound, then the main map's
       * {@link #subMap(Object, boolean, Object, boolean)} method is used and supplied the specified
       * lower bound and this sub-map's upper bound.
       */
      @Override
      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         if (!isInRange(fromKey)) {
            throw new IllegalArgumentException("from key " + fromKey + " is outside sub-map range");
         }
         return upperBoundType == BoundType.NO_BOUND
               ? AbstractNavigableMap.this.tailMap(fromKey, inclusive)
               : AbstractNavigableMap.this.subMap(fromKey, inclusive, upperBound,
                     upperBoundType == BoundType.INCLUSIVE);
      }
      
      /**
       * Finds the entry in this sub-map with the greatest key that is less than the specified key.
       * This first calls the main map's implementation of {@link #lowerEntry(Object)}. If it is not
       * null then it is range checked and returned if within the sub-map's bounds. If it is less
       * than the sub-map's lower bound then null is returned. If it is greater than the sub-map's
       * upper bound, then the sub-map's {@link #lastEntry()} is returned.
       */
      @Override
      public Entry<K, V> lowerEntry(K key) {
         Entry<K, V> candidate = AbstractNavigableMap.this.lowerEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return lastEntry();
         }
         return candidate; 
      }

      /**
       * Finds the entry in this sub-map with the greatest key that is less than or equal to the
       * specified key. This first calls the main map's implementation of {@link #floorEntry(Object)}.
       * If it is not null then it is range checked and returned if within the sub-map's bounds. If
       * it is less than the sub-map's lower bound then null is returned. If it is greater than the
       * sub-map's upper bound, then the sub-map's {@link #lastEntry()} is returned.
       */
      @Override
      public Entry<K, V> floorEntry(K key) {
         Entry<K, V> candidate = AbstractNavigableMap.this.floorEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return lastEntry();
         }
         return candidate;
      }

      /**
       * Finds the entry in this sub-map with the smallest key that is greater than or equal to the
       * specified key. This first calls the main map's implementation of {@link
       * #ceilingEntry(Object)}. If it is not null then it is range checked and returned if within
       * the sub-map's bounds. If it is greater than the sub-map's upper bound then null is
       * returned. If it is less than the sub-map's lower bound, then the sub-map's {@link
       * #firstEntry()} is returned.
       */
      @Override
      public Entry<K, V> ceilingEntry(K key) {
         Entry<K, V> candidate = AbstractNavigableMap.this.ceilingEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return firstEntry();
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }

      /**
       * Finds the entry in this sub-map with the smallest key that is greater than the specified
       * key. This first calls the main map's implementation of {@link #higherEntry(Object)}. If it
       * is not null then it is range checked and returned if within the sub-map's bounds. If it is
       * greater than the sub-map's upper bound then null is returned. If it is less than the
       * sub-map's lower bound, then the sub-map's {@link #firstEntry()} is returned.
       */
      @Override
      public Entry<K, V> higherEntry(K key) {
         Entry<K, V> candidate = AbstractNavigableMap.this.higherEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return firstEntry();
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }

      /**
       * Finds the entry in the map with the smallest key or {@code null} if the sub-map is empty.
       * If the sub-map has no lower bound, the main map's first entry is queried. If the sub-map's
       * lower bound is inclusive, then this finds the entry using the main map's {@link
       * #ceilingEntry(Object)} method and passing the lower bound. If exclusive, the main map's
       * {@link #higherEntry(Object)} method is used instead. If the resulting entry is non-null and
       * within the sub-map's bounds then it is returned. If, however, it is null or greater than
       * the sub-map's upper bound, {@code null} is returned.
       */
      @Override
      public Entry<K, V> firstEntry() {
         Entry<K, V> candidate;
         switch (lowerBoundType) {
            case NO_BOUND:
               candidate = AbstractNavigableMap.this.firstEntry();
               break;
            case INCLUSIVE:
               candidate = AbstractNavigableMap.this.ceilingEntry(lowerBound);
               break;
            case EXCLUSIVE:
               candidate = AbstractNavigableMap.this.higherEntry(lowerBound);
               break;
            default:
               throw new AssertionError();
         }
         if (candidate == null) {
            return null;
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }

      /**
       * Finds the entry in the map with the greatest key or {@code null} if the sub-map is empty.
       * If the sub-map has no upper bound, the main map's last entry is queried. If the sub-map's
       * upper bound is inclusive, then this finds the entry using the main map's {@link
       * #floorEntry(Object)} method and passing the upper bound. If exclusive, the main map's
       * {@link #lowerEntry(Object)} method is used instead. If the resulting entry is non-null and
       * within the sub-map's bounds then it is returned. If, however, it is null or less than the
       * sub-map's lower bound, {@code null} is returned.
       */
      @Override
      public Entry<K, V> lastEntry() {
         Entry<K, V> candidate;
         switch (upperBoundType) {
            case NO_BOUND:
               candidate = AbstractNavigableMap.this.lastEntry();
               break;
            case INCLUSIVE:
               candidate = AbstractNavigableMap.this.floorEntry(upperBound);
               break;
            case EXCLUSIVE:
               candidate = AbstractNavigableMap.this.lowerEntry(upperBound);
               break;
            default:
               throw new AssertionError();
         }
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }
      
      /**
       * Returns the main map's modification count. This allows iterators over sub-map views to
       * properly fail-fast.
       * 
       * <p>Since a {@link SubMap} <em>extends</em> {@link AbstractNavigableMap}, it also inherits
       * a {@link AbstractNavigableMap#modCount} field. For sub-maps, that field is compared to the
       * main maps' value to detect when the sub-map's memo-ized size is stale and elements need to
       * be counted.
       * 
       * @return {@inheritDoc}
       */
      @Override protected int getModCount() {
         return AbstractNavigableMap.this.getModCount();
      }
   }
   
   /**
    * An iterator over the mappings in this map. This is the base class used for iterators over the
    * map's keys, its entries, and its values.
    * 
    * <p>This iterator implements fail-fast behavior.
    *
    * @param <T> the type of element fetched from the iterator
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected abstract class BaseIteratorImpl<T> implements Iterator<T> {
      protected Entry<K, V> next;
      protected Entry<K, V> lastFetched;
      protected int myModCount;
      
      /**
       * Constructs a new iterator.
       */
      protected BaseIteratorImpl() {
         next = start();
         resetModCount();
      }
      
      /**
       * Returns the first entry that will be visited during iteration. The default is the map's
       * {@linkplain NavigableMap#firstEntry() first entry}.
       *
       * @return the entry returned by the very first call to {@link #next()} or {@code null} if
       *       the map is empty
       */
      protected Entry<K, V> start() {
         return firstEntry();
      }

      /**
       * Advances the iterator and returns the next entry to be visited during iteration. The
       * default, for ascending iteration, returns the lowest key that is greater than the specified
       * previous key (using {@link NavigableMap#higherEntry(Object)}).
       *
       * @param previousKey the previous key visited during iteration
       * @return the next entry to be visited
       */
      protected Entry<K, V> advance(K previousKey) {
         return higherEntry(previousKey);
      }
      
      /**
       * Computes the value fetched from this iterator based on the given map entry.
       *
       * @param entry the visisted map entry
       * @return the value to return from {@link #next()}
       */
      protected abstract T compute(Entry<K, V> entry);

      /**
       * Checks for concurrent modification by examining the underlying map's
       * {@link AbstractNavigableMap#modCount modCount}. Throws an exception if concurrent
       * modification is detected.
       */
      protected void checkModCount() {
         if (myModCount != getModCount()) {
            throw new ConcurrentModificationException();
         }
      }
      
      /**
       * Resets the iterator to be in sync with the map's latest
       * {@link AbstractNavigableMap#modCount modCount}. This is used if this iterator makes a call
       * to modify the underlying map, so that subsequent operations don't mistake the change for
       * a concurrent modification.
       */
      protected void resetModCount() {
         myModCount = getModCount();
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         return next != null;
      }

      @Override
      public T next() {
         checkModCount();
         if (next == null) {
            throw new NoSuchElementException();
         }
         lastFetched = next;
         Entry<K, V> ret = next;
         next = advance(lastFetched.getKey());
         return compute(ret);
      }

      @Override
      public void remove() {
         checkModCount();
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         removeEntry(lastFetched);
         resetModCount();
         lastFetched = null;
      }
   }
   
   /**
    * An iterator over the map entries in this map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class EntryIteratorImpl extends BaseIteratorImpl<Entry<K, V>> {
      /**
       * {@inheritDoc}
       * 
       * <p>This implementation just returns the specified entry.
       */
      @Override
      protected Entry<K, V> compute(Entry<K, V> entry) {
         return entry;
      }
   }

   /**
    * An iterator over the keys in this map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class KeyIteratorImpl extends BaseIteratorImpl<K> {
      /**
       * {@inheritDoc}
       * 
       * <p>This implementation extracts the key from the specified entry.
       */
      @Override
      protected K compute(Entry<K, V> entry) {
         return entry.getKey();
      }
   }

   /**
    * An iterator over the values in this map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class ValueIteratorImpl extends BaseIteratorImpl<V> {
      /**
       * {@inheritDoc}
       * 
       * <p>This implementation extracts the key from the specified entry.
       */
      @Override
      protected V compute(Entry<K, V> entry) {
         return entry.getValue();
      }
   }

   /**
    * An iterator over the keys in this map that goes from highest to lowest key.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class DescendingKeyIteratorImpl extends BaseIteratorImpl<K> {
      /**
       * {@inheritDoc}
       * 
       * <p>This implementation extracts the key from the specified entry.
       */
      @Override
      protected K compute(Entry<K, V> entry) {
         return entry.getKey();
      }
      
      /**
       * Returns the first entry that will be visited during iteration. This implementation returns
       * the map's {@linkplain NavigableMap#lastEntry() last entry}
       * 
       * @return {@inheritDoc}
       */
      @Override
      protected Entry<K, V> start() {
         return lastEntry();
      }
      
      /**
       * Advances the iterator and returns the next entry to be visited during iteration. This
       * implementation returns the largest key that is less than the specified previous key (using
       * {@link NavigableMap#lowerEntry(Object)}).
       *
       * @param previousKey {@inheritDoc}
       * @return {@inheritDoc}
       */
      @Override
      protected Entry<K, V> advance(K previousKey) {
         return lowerEntry(previousKey);
      }
   }
   
   /**
    * The view of mappings as a set of {@linkplain java.util.Map.Entry map entries}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class EntrySet extends AbstractSet<Entry<K, V>> {
      @Override
      public int size() {
         return AbstractNavigableMap.this.size();
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         return new EntryIteratorImpl();
      }

      @Override
      public void clear() {
         AbstractNavigableMap.this.clear();
      }
   }

   /**
    * A view of the map's keys as a set.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class KeySet extends AbstractSet<K> implements NavigableSet<K> {
      
      @Override
      public K lower(K e) {
         return AbstractNavigableMap.this.lowerKey(e);
      }

      @Override
      public K floor(K e) {
         return AbstractNavigableMap.this.floorKey(e);
      }

      @Override
      public K ceiling(K e) {
         return AbstractNavigableMap.this.ceilingKey(e);
      }

      @Override
      public K higher(K e) {
         return AbstractNavigableMap.this.higherKey(e);
      }

      @Override
      public K pollFirst() {
         Entry<K, V> entry = AbstractNavigableMap.this.pollFirstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K pollLast() {
         Entry<K, V> entry = AbstractNavigableMap.this.pollLastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Iterator<K> iterator() {
         return new KeyIteratorImpl();
      }

      @Override
      public Iterator<K> descendingIterator() {
         return new DescendingKeyIteratorImpl();
      }

      @Override
      public Comparator<? super K> comparator() {
         return AbstractNavigableMap.this.comparator();
      }

      @Override
      public K first() {
         return AbstractNavigableMap.this.firstKey();
      }

      @Override
      public K last() {
         return AbstractNavigableMap.this.lastKey();
      }

      @Override
      public int size() {
         return AbstractNavigableMap.this.size();
      }

      @Override
      public boolean contains(Object o) {
         return AbstractNavigableMap.this.containsKey(o);
      }

      @Override
      public boolean remove(Object o) {
         return AbstractNavigableMap.this.removeEntry(o) != null;
      }

      @Override
      public void clear() {
         AbstractNavigableMap.this.clear();
      }

      @Override
      public NavigableSet<K> descendingSet() {
         return new DescendingSet<K>(this);
      }

      @Override
      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement,
            boolean toInclusive) {
         return AbstractNavigableMap.this.subMap(fromElement, fromInclusive, toElement, toInclusive)
               .navigableKeySet();
      }

      @Override
      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         return AbstractNavigableMap.this.headMap(toElement, inclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return AbstractNavigableMap.this.tailMap(fromElement, inclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> subSet(K fromElement, K toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      @Override
      public NavigableSet<K> headSet(K toElement) {
         return headSet(toElement, false);
      }

      @Override
      public NavigableSet<K> tailSet(K fromElement) {
         return tailSet(fromElement, true);
      }
   }
   
   /**
    * A view of the map's values as a collection.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class ValueCollection extends AbstractCollection<V> {
      @Override
      public Iterator<V> iterator() {
         return new ValueIteratorImpl();
      }

      @Override
      public int size() {
         return AbstractNavigableMap.this.size();
      }
   };
}
