package com.bluegosling.collections.tables;

import com.bluegosling.collections.FilteringIterator;
import com.google.common.collect.Iterators;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * A nested map structure, like a multi-dimensional index. This class provides base operations for
 * structures that represent maps of maps, like {@code Map<Type, Map<Key, Value>>} or even nested
 * further like {@code Map<Type, Map<AnotherType, Map<Key, Value>>>}. This can be much easier to
 * work with than using multiple levels of {@link Map}s due to the extra book-keeping to initialize
 * maps as new keys and levels are created and remove them when keys and levels are removed.
 *
 * @param <K> the type for all keys in the map (must be a common super-type for keys at all levels,
 *       can be {@code Object})
 * @param <V> the type of values in the map
 * 
 * @see Table2D
 * @see Table3D
 * @see Table4D
 * @see Table5D
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
class NestedMap<K, V> {
   
   /**
    * A sentinel value that is used as a wildcard with {@link Level#containsKey(Object[])}.
    */
   static final Object ANY = new Object();
   
   /**
    * A level in the nested map. Each level is a map. The values in this map could be a subsequent
    * nesting level. The leaf level of a nested map has values in the map, not further levels.
    *
    * @param <K> the type of keys in the map
    * @param <V> the type of values in the leaf level
    * 
    * @see NonLeafLevel
    * @see LeafLevel
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static abstract class Level<K, V> {
      private final Map<K, ?> map;
      
      /**
       * Constructs a new level backed by the given map.
       *
       * @param map a map that stores this level's children
       */
      Level(Map<K, ?> map) {
         this.map = map;
      }
      
      /**
       * Returns the map that backs this level. The value type is not known because it depends on
       * whether this level is a leaf level or not. Leaf level maps have values of type {@code V}
       * (actual map values). Non-leaf level maps have values of type {@code Level<K, V>} (the next
       * level in the nested map).
       *
       * @return the map that backs this level
       */
      Map<K, ?> map() {
         return map;
      }
      
      /**
       * Returns the number of entries in the map that backs this level. For leaf levels, this can
       * be used to compute the total number of mappings in a nested map as the size is the number
       * of mappings. For non-leaf levels, the size is the number of child levels that are nested
       * below this one.
       *
       * @return the number of entries in the map that backs this level
       */
      int size() {
         return map.size();
      }

      /**
       * Determines whether the map that backs this level is empty.
       *
       * @return true if the map that backs this level is empty
       */
      boolean isEmpty() {
         return map.isEmpty();
      }

      /**
       * Returns the height of this level in the nested map. The leaf level has a height of zero.
       * The root of a 2-D nested map has a height of two.
       *
       * @return the height of this level in the nested map
       */
      abstract int height();
      
      /**
       * Stores a value in this level. The given array of keys is indexed using a given level's
       * height. For example, the key at index zero corresponds to the key in the inner-most (e.g.
       * leaf) level. The last element in the array corresponds to the first key in the path. This
       * method is recursive: calling it on the root node will also invoke it for subsequent nested
       * levels, finally storing the value in the leaf entry that corresponds to the full path.
       *
       * @param keys an array of keys for the new mapping
       * @param value the value for the new mapping
       * @return the old value associated with this path of keys or {@code null} if there was no
       *       such previous mapping
       */
      abstract V put(K keys[], V value);
      
      /**
       * Gets a value from this level. The given array of keys is indexed using a given level's
       * height. For example, the key at index zero corresponds to the key in the inner-most (e.g.
       * leaf) level. The last element in the array corresponds to the first key in the path. This
       * method is recursive: calling it on the root node will also invoke it for subsequent nested
       * levels, finally returning the value from the leaf entry that corresponds to the full path.
       *
       * @param keys an array of keys for the mapping being queried
       * @return the value for the given keys or {@code null} if no such mapping exists
       */
      abstract V get(K keys[]);
      
      /**
       * Removes a value from this level. The given array of keys is indexed using a given level's
       * height. For example, the key at index zero corresponds to the key in the inner-most (e.g.
       * leaf) level. The last element in the array corresponds to the first key in the path. This
       * method is recursive: calling it on the root node will also invoke it for subsequent nested
       * levels, finally removing the value from the leaf entry that corresponds to the full path.
       *
       * @param keys an array of keys for the mapping to remove
       * @return the value that was associated with this path of keys and now removed, or
       *       {@code null} if there was no mapping to remove       
       */
      abstract V remove(K keys[]);
      
      /**
       * Determines if the given keys exist in this level. The given array of keys is indexed using
       * a given level's height. For example, the key at index zero corresponds to the key in the
       * inner-most (e.g. leaf) level. The last element in the array corresponds to the first key in
       * the path. This method is recursive: calling it on the root node will also invoke it for
       * subsequent nested levels, finally querying the existence of the value from the leaf entry
       * that corresponds to the full path.
       *
       * @param keys an array of keys for the mapping being queried
       * @return true if this nested map contains an entry for the given keys; false otherwise
       */
      abstract boolean containsKey(K keys[]);
      
      /**
       * An iterator over entries in this level of the map. If this is a leaf level, the returned
       * iterator has no elements since there are no further nested levels.
       *
       * @return an iterator over nested entries in this level of the map
       */
      abstract Iterator<Entry<K, Level<K, V>>> entryIterator();
      
      /**
       * An iterator over values in this level of the map. If this is not a leaf level, the returned
       * iterator has no elements since there are no values in internal levels.
       *
       * @return an iterator over values in this level of the map
       */
      abstract Iterator<Entry<K, V>> valueIterator();
   }
   
   private final IntFunction<Map<K, Object>> mapMaker;
   private final NonLeafLevel<K, V> root;
   final int keyOrder[];
   final K filterKeysByNesting[];
   final boolean filterKeyPresentByNesting[];
   private final int numFilterKeys;
   
   /**
    * Constructs a nested map. The given function is provided, as input, the height for a level and
    * should produce an empty map, used to create a new node for that level.
    *
    * @param mapMaker a function that produces an empty map for a given level
    * @param height the height of this nested map
    */
   NestedMap(IntFunction<Map<K, Object>> mapMaker, int height) {
      if (height < 1) {
         throw new IllegalArgumentException();
      }
      this.mapMaker = mapMaker;
      this.root = new NonLeafLevel<K, V>(height, mapMaker);
      this.keyOrder = IntStream.rangeClosed(0, height).toArray();
      @SuppressWarnings("unchecked")
      K[] k = (K[]) new Object[++height];
      this.filterKeysByNesting = k;
      this.filterKeyPresentByNesting = new boolean[height];
      this.numFilterKeys = 0;
      // check invariants:
      assert validKeyOrder();
      assert root.height() + 1 == keyOrder.length;
      assert root.height() >= this.height() + numFilterKeys;
   }

   /**
    * Constructs a view of the given map with a filter applied. All keys other than the given value
    * at the given level are excluded from the view. This view has one fewer level than the given
    * nested map; in other words, it's height is one less.
    * 
    * <p>To demonstrate how the view works, let's take a 3-D nested map. We construct a filtered
    * view like so:<pre>
    * NestedMap<K, V> filtered = new NestedMap<>(other, "a", 1);</pre>
    * In this case, the second level of the tree is fixed with a value of "a" (the first is at
    * height == 0; the last and third level is at height == 2). So querying this view with a key
    * path of "x", "y" (since the filtered map has but two dimensions) is effectively the same as
    * querying the original map with a path of "x", "a", "y".
    *
    * @param other a nested map
    * @param filterKey the value used to filter a level of the tree
    * @param filterHeight the height of the level being filtered
    */
   NestedMap(NestedMap<K, V> other, K filterKey, int filterHeight) {
      this.mapMaker = other.mapMaker;
      this.root = other.root;
      this.keyOrder = other.keyOrderWithNewFilter(filterHeight);
      this.filterKeysByNesting = other.filterKeysByNesting.clone();
      this.filterKeyPresentByNesting = other.filterKeyPresentByNesting.clone();
      this.numFilterKeys = other.numFilterKeys + 1;
      int h = keyOrder[keyOrder.length - numFilterKeys];
      this.filterKeyPresentByNesting[h] = true;
      this.filterKeysByNesting[h] = filterKey;
      // check invariants:
      assert validKeyOrder();
      assert root.height() + 1 == keyOrder.length;
      assert root.height() >= this.height() + numFilterKeys;
   }
   
   /**
    * Constructs a rotated or transposed view of the given map. A transposed view swaps the two
    * inner-most levels. A rotation shifts all levels one to the right, displacing the right-most
    * (e.g. inner-most) level into the left-most level. If the nested map has a height of just two,
    * transposition and rotation are effectively the same. 
    *
    * <p>To demonstrate how the view works, let's take a 3-D nested map. If we created a transposed
    * view, then querying the transposed view with a key path of "x", "y", "z" is effectively the
    * same as querying the original map with a path of "x", "z", "y".
    * 
    * <p>Similarly, if we constructed a rotated view, querying the rotated view with a key path of
    * "x", "y", "z" is effectively the same as querying the original map with a path of "y", "z",
    * "x".
    * 
    * @param other a nested map
    * @param transposed if true, this view is transposition of the given map; if false, this view is
    *       a rotation of the given map
    */
   NestedMap(NestedMap<K, V> other, boolean transposed) {
      this.mapMaker = other.mapMaker;
      this.root = other.root;
      this.keyOrder = transposed ? other.transposeKeyOrder() : other.rotateKeyOrder();
      this.filterKeysByNesting = other.filterKeysByNesting;
      this.filterKeyPresentByNesting = other.filterKeyPresentByNesting;
      this.numFilterKeys = other.numFilterKeys;
      // check invariants:
      assert validKeyOrder();
      assert root.height() + 1 == keyOrder.length;
      assert root.height() >= this.height() + numFilterKeys;
   }

   /**
    * Returns the root level of this nested map.
    *
    * @return the root level of this nested map
    */
   NonLeafLevel<K, V> root() {
      return root;
   }
   
   /**
    * Returns the height of this nested map.
    *
    * @return the height of this nested map
    */
   int height() {
      return keyOrder.length - numFilterKeys - 1;
   }
   
   private boolean validKeyOrder() {
      int len = keyOrder.length;
      boolean seen[] = new boolean[len];
      for (int i = 0; i < len; i++) {
         int o = keyOrder[i];
         if (o < 0 || o >= keyOrder.length || seen[o]) {
            return false;
         }
         seen[o] = true;
      }
      return true;
   }
   
   K[] newArray(int n) {
      @SuppressWarnings("unchecked")
      K ret[] = (K[]) new Object[n];
      return ret;
   }
   
   K[] makeKeys(K key, int height) {
      assert height < keyOrder.length - numFilterKeys;
      assert numFilterKeys + 1 <= keyOrder.length;
      int actualHeight = keyOrder[height];
      K ret[] = filterKeysByNesting.clone();
      for (int i = 0, len = ret.length; i < len; i++) {
         if (i == actualHeight) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key;
         } else if (!filterKeyPresentByNesting[i]) {
            @SuppressWarnings("unchecked")
            K anyK = (K) ANY;
            ret[i] = anyK;
         }
      }
      return ret;
   }

   K[] makeKeys(K key1, int height1, K key2, int height2) {
      assert height1 < keyOrder.length - numFilterKeys;
      assert height2 < keyOrder.length - numFilterKeys;
      assert numFilterKeys + 2 <= keyOrder.length;
      assert height1 != height2;
      int actualHeight1 = keyOrder[height1];
      int actualHeight2 = keyOrder[height2];
      K ret[] = filterKeysByNesting.clone();
      for (int i = 0, len = ret.length; i < len; i++) {
         if (i == actualHeight1) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key1;
         } else if (i == actualHeight2) {
               assert !filterKeyPresentByNesting[i];
               ret[i] = key2;
         } else if (!filterKeyPresentByNesting[i]) {
            @SuppressWarnings("unchecked")
            K anyK = (K) ANY;
            ret[i] = anyK;
         }
      }
      return ret;
   }

   K[] makeKeys(K key1, int height1, K key2, int height2, K key3, int height3) {
      assert height1 < keyOrder.length - numFilterKeys;
      assert height2 < keyOrder.length - numFilterKeys;
      assert height3 < keyOrder.length - numFilterKeys;
      assert numFilterKeys + 3 <= keyOrder.length;
      assert height1 != height2;
      assert height1 != height3;
      assert height2 != height3;
      int actualHeight1 = keyOrder[height1];
      int actualHeight2 = keyOrder[height2];
      int actualHeight3 = keyOrder[height3];
      K ret[] = filterKeysByNesting.clone();
      for (int i = 0, len = ret.length; i < len; i++) {
         if (i == actualHeight1) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key1;
         } else if (i == actualHeight2) {
               assert !filterKeyPresentByNesting[i];
               ret[i] = key2;
         } else if (i == actualHeight3) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key3;
         } else if (!filterKeyPresentByNesting[i]) {
            @SuppressWarnings("unchecked")
            K anyK = (K) ANY;
            ret[i] = anyK;
         }
      }
      return ret;
   }
   
   K[] makeKeys(K key1, int height1, K key2, int height2, K key3, int height3, K key4, int height4)
   {
      assert height1 < keyOrder.length - numFilterKeys;
      assert height2 < keyOrder.length - numFilterKeys;
      assert height3 < keyOrder.length - numFilterKeys;
      assert height4 < keyOrder.length - numFilterKeys;
      assert numFilterKeys + 4 <= keyOrder.length;
      assert height1 != height2;
      assert height1 != height3;
      assert height1 != height4;
      assert height2 != height3;
      assert height2 != height4;
      assert height3 != height4;
      int actualHeight1 = keyOrder[height1];
      int actualHeight2 = keyOrder[height2];
      int actualHeight3 = keyOrder[height3];
      int actualHeight4 = keyOrder[height4];
      K ret[] = filterKeysByNesting.clone();
      for (int i = 0, len = ret.length; i < len; i++) {
         if (i == actualHeight1) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key1;
         } else if (i == actualHeight2) {
               assert !filterKeyPresentByNesting[i];
               ret[i] = key2;
         } else if (i == actualHeight3) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key3;
         } else if (i == actualHeight4) {
            assert !filterKeyPresentByNesting[i];
            ret[i] = key4;
         } else if (!filterKeyPresentByNesting[i]) {
            @SuppressWarnings("unchecked")
            K anyK = (K) ANY;
            ret[i] = anyK;
         }
      }
      return ret;
   }
   
   K[] makeKeys(K key) {
      assert numFilterKeys + 1 == keyOrder.length;
      K ret[] = filterKeysByNesting.clone();
      ret[keyOrder[0]] = key;
      return ret;
   }
   
   K[] makeKeys(K k1, K k2) {
      assert numFilterKeys + 2 == keyOrder.length;
      K ret[] = filterKeysByNesting.clone();
      ret[keyOrder[1]] = k1;
      ret[keyOrder[0]] = k2;
      return ret;
   }

   K[] makeKeys(K k1, K k2, K k3) {
      assert numFilterKeys + 3 == keyOrder.length;
      K ret[] = filterKeysByNesting.clone();
      ret[keyOrder[2]] = k1;
      ret[keyOrder[1]] = k2;
      ret[keyOrder[0]] = k3;
      return ret;
   }

   K[] makeKeys(K k1, K k2, K k3, K k4) {
      assert numFilterKeys + 4 == keyOrder.length;
      K ret[] = filterKeysByNesting.clone();
      ret[keyOrder[3]] = k1;
      ret[keyOrder[2]] = k2;
      ret[keyOrder[1]] = k3;
      ret[keyOrder[0]] = k4;
      return ret;
   }

   K[] makeKeys(K k1, K k2, K k3, K k4, K k5) {
      assert numFilterKeys + 5 == keyOrder.length;
      K ret[] = filterKeysByNesting.clone();
      ret[keyOrder[4]] = k1;
      ret[keyOrder[3]] = k2;
      ret[keyOrder[2]] = k3;
      ret[keyOrder[1]] = k4;
      ret[keyOrder[0]] = k5;
      return ret;
   }

   // unused; to support possible future cases with nesting depth > 5
   K[] makeKeys(K keys[]) {
      assert numFilterKeys + keys.length == keyOrder.length;
      K ret[] = filterKeysByNesting.clone();
      for (int j = 0, i = height(); i >= 0; j++, i--) {
         if (j >= keys.length) {
            throw new IllegalStateException("too many keys provded: " + keys.length);
         }
         ret[keyOrder[i]] = keys[j];
      }
      return ret;
   }
   
   V put(K keys[], V value) {
      return root.put(keys, value);
   }
   
   V get(K keys[]) {
      return root.get(keys);
   }
   
   V remove(K keys[]) {
      return root.remove(keys);
   }
   
   boolean containsKey(K keys[]) {
      return root.containsKey(keys);
   }
   
   /**
    * Computes the total number of mappings in this nested map. This is not a constant time
    * operation. Instead, it is linear with the number of maps at the leaf level.
    *
    * @return the total number of mappings in this nested map
    */
   public int size() {
      int sz = 0;
      int filterHeight = getLowestFilteredLevel();
      if (filterHeight == 0) {
         // we are filtering leaf level, so we can't just measure size of leaf maps
         for (Iterator<?> iter = entryIterator((k, e) -> null); iter.hasNext();) {
            iter.next();
            sz++;
         }
      } else {
         for (Iterator<Level<K, V>> iter = levelIterator((k, e) -> e.getValue(), 1);
               iter.hasNext();) {
            Level<K, V> level = iter.next();
            sz += level.size();
         }
      }
      return sz;
   }
   
   /**
    * Determines if this nested map is empty or not. This is not a constant time operation. Instead,
    * it is linear with the height of this map. If this nested map is a {@linkplain
    * NestedMap#NestedMap(NestedMap, Object, int) filtered view}, then the runtime complexity is
    * proportional to the height of the base map (the underlying map, or its base map if the
    * underlying map is also a filtered view).
    *
    * @return true if the nested map is empty; false otherwise
    */
   public boolean isEmpty() {
      return !entryIterator((k, e) -> null).hasNext();
   }
   
   /**
    * Removes all mappings from this nested map.
    */
   public void clear() {
      if (numFilterKeys == 0) {
         root.map().clear();
      } else {
         int clearHeight = getLowestFilteredLevel();
         if (clearHeight == 0) {
            // we're filtering on keys at lowest level, so we can't clear entire maps but instead
            // must remove them using iterator
            Iterator<?> iter = entryIterator((k, e) -> null);
            while (iter.hasNext()) {
               iter.next();
               iter.remove();
            }
         } else {
            // otherwise, we can clear entire child maps
            levelIterator((k, e) -> e.getValue(), clearHeight)
                  .forEachRemaining(l -> l.map().clear());
         }
      }
   }
   
   private int getLowestFilteredLevel() {
      int len = filterKeyPresentByNesting.length;
      for (int i = 0; i < len; i++) {
         if (filterKeyPresentByNesting[i]) {
            return i;
         }
      }
      return len;
   }
   
   private int[] keyOrderWithNewFilter(int heightOfFilter) {
      // new filter goes at the end (but before other filter keys, if any)
      int l = keyOrder.length;
      int filterLocation = l - numFilterKeys - 1;
      if (filterLocation == heightOfFilter) {
         // already in the right spot
         return keyOrder;
      }
      assert heightOfFilter < filterLocation;
      int ret[] = new int[l];
      System.arraycopy(keyOrder, 0, ret, 0, heightOfFilter);
      System.arraycopy(keyOrder, heightOfFilter + 1, ret, heightOfFilter,
            filterLocation - heightOfFilter);
      ret[filterLocation] = keyOrder[heightOfFilter];
      return ret;
   }
   
   private int[] rotateKeyOrder() {
      int l = keyOrder.length;
      int ret[] = new int[l];
      l -= numFilterKeys;
      ret[0] = keyOrder[--l];
      System.arraycopy(keyOrder, 0, ret, 1, l);
      return ret;
   }

   private int[] transposeKeyOrder() {
      int ret[] = keyOrder.clone();
      int tmp = ret[0];
      ret[0] = ret[1];
      ret[1] = tmp;
      return ret;
   }

   Set<K> keysAtHeight(int height) {
      int actualHeight = keyOrder[height];
      if (actualHeight == root.height() && numFilterKeys == 0) {
         return root.map().keySet();
      } else {
         return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
               return keyIteratorAtHeight(height);
            }

            @Override
            public int size() {
               return Iterators.size(iterator());
            }
         };
      }
   }
   
   public Collection<V> values() {
      return new AbstractCollection<V>() {
         @Override
         public Iterator<V> iterator() {
            return entryIterator((k, e) -> e.getValue());
         }

         @Override
         public int size() {
            return Iterators.size(iterator());
         }
      };
   }

   Iterator<K> keyIteratorAtHeight(int height) {
      int actualHeight = keyOrder[height];
      Map<K, Object> seen = mapMaker.apply(actualHeight);
      if (actualHeight == root.height() && numFilterKeys == 0) {
         // grabbing top-level, no filters is the easiest and most efficient
         return root.map().keySet().iterator();
      } else if (actualHeight == 0) {
         // lowest-level means we have to iterate through every leaf value
         return new FilteringIterator<>(
               entryIterator((k, e) -> e.getKey()),
               k -> seen.put(k, Boolean.TRUE) == null);
      } else {
         int lowestFilterHeight = getLowestFilteredLevel();
         if (lowestFilterHeight < actualHeight) {
            // when there is a filter *below* the key height we want, we need an iterator at the
            // lower level, to make sure that the keys at the higher level match the filter
            // TODO: this could be more efficient by getting an iterator at the right height and
            // then doing a deep "containsKey" check to check all filters below that level
            return new FilteringIterator<>(
                  levelIterator((k, e) -> k.getKey(height), lowestFilterHeight),
                  k -> seen.put(k, Boolean.TRUE) == null);
         } else {
            // final case: just iterate through keys at the given level
            return new FilteringIterator<>(
                  levelIterator((k, e) -> e.getKey(), actualHeight),
                  k -> seen.put(k, Boolean.TRUE) == null);
         }
      }
   }

   <T> Iterator<T> entryIterator(BiFunction<NestedKey<K>, Entry<K, V>, T> fn) {
      return new EntryIterator<>(root, fn);
   }

   <T> Iterator<T> levelIterator(BiFunction<NestedKey<K>, Entry<K, Level<K, V>>, T> fn,
         int heightOfLevel) {
      return new LevelIterator<>(root, fn, keyOrder[heightOfLevel]);
   }

   /**
    * A non-leaf level in a nested map. The values in this level of the map are subsequent levels.
    *
    * @param <K> the type of keys in the level
    * @param <V> the type of values in the leaf level
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class NonLeafLevel<K, V> extends Level<K, V> {
      private final int height;
      private final IntFunction<Map<K, Object>> mapMaker;
      
      NonLeafLevel(int height, IntFunction<Map<K, Object>> mapMaker) {
         super(mapMaker.apply(height));
         this.height = height;
         this.mapMaker = mapMaker;
      }

      IntFunction<Map<K, Object>> mapMaker() {
         return mapMaker;
      }
      
      @Override
      Map<K, Level<K, V>> map() {
         @SuppressWarnings("unchecked")
         Map<K, Level<K, V>> ret = (Map<K, Level<K, V>>) super.map();
         return ret;
      }

      @Override
      public V put(K keys[], V value) {
         @SuppressWarnings("unchecked")
         V ret[] = (V[]) new Object[1];
         map().compute(keys[height], (k, v) -> {
            if (v == null) {
               if (height == 1) {
                  // next height is zero, so it's a leaf
                  v = new LeafLevel<K, V>(mapMaker.apply(0));
               } else {
                  v = new NonLeafLevel<K, V>(height - 1, mapMaker);
               }
            }
            ret[0] = v.put(keys, value);
            return v;
         });
         return ret[0];
      }

      @Override
      public V get(K keys[]) {
         Level<K, V> nextLevel = map().get(keys[height]);
         return nextLevel != null ? nextLevel.get(keys) : null;
      }

      @Override
      public V remove(K keys[]) {
         @SuppressWarnings("unchecked")
         V ret[] = (V[]) new Object[1];
         map().compute(keys[height], (k, l) -> {
            if (l == null) {
               return null;
            }
            ret[0] = l.remove(keys);
            return l.isEmpty() ? null : l;
         });
         return ret[0];
      }

      @Override
      public boolean containsKey(K keys[]) {
         K k = keys[height];
         if (k == ANY) {
            for (Level<K, V> nextLevel : map().values()) {
               if (nextLevel.containsKey(keys)) {
                  return true;
               }
            }
            return false;
         } else {
            Level<K, V> nextLevel = map().get(k);
            return nextLevel != null && nextLevel.containsKey(keys);
         }
      }

      @Override
      public Iterator<Entry<K, Level<K, V>>> entryIterator() {
         return map().entrySet().iterator();
      }
      
      @Override
      public Iterator<Entry<K, V>> valueIterator() {
         return Collections.emptyIterator();
      }

      @Override
      public int height() {
         return height;
      }
   }
   
   /**
    * A leaf level in a nested map. The values in this level of the map are values, not
    * subsequent levels.
    *
    * @param <K> the type of keys in this level
    * @param <V> the type of values in this level
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class LeafLevel<K, V> extends Level<K, V> {
      
      LeafLevel(Map<K, ?> map) {
         super(map);
      }
      
      @Override
      Map<K, V> map() {
         @SuppressWarnings("unchecked")
         Map<K, V> ret = (Map<K, V>) super.map();
         return ret;
      }

      @Override
      public V put(K keys[], V value) {
         return map().put(keys[0], value);
      }
      
      @Override
      public V remove(K keys[]) {
         return map().remove(keys[0]);
      }

      @Override
      public V get(K keys[]) {
         return map().get(keys[0]);
      }

      @Override
      public boolean containsKey(K keys[]) {
         K k = keys[0];
         return k == ANY ? !map().isEmpty() : map().containsKey(k);
      }

      @Override
      public Iterator<Entry<K, Level<K, V>>> entryIterator() {
         return Collections.emptyIterator();
      }

      @Override
      public Iterator<Entry<K, V>> valueIterator() {
         return map().entrySet().iterator();
      }

      @Override
      public int height() {
         return 0;
      }
   }
   
   /**
    * A simple interface that represents a key for a nested map. A single key for a nested map is a
    * path of keys, each element in the path representing a key for a level in the nested map.
    *
    * @param <K> the type of the keys
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface NestedKey<K> {
      /**
       * Gets the path element of this key corresponding to the given height, or level. A height of
       * zero represents the leaf level.
       *
       * @param height the height of the key to query
       * @return the value of the key element for the given level
       */
      K getKey(int height);
   }
   
   /**
    * A stack-frame, used to iteratively perform depth-first traversal when iterating through
    * mappings in the nested map.
    *
    * @param <T> the type of value fetched from the iterator
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class StackFrame<T> implements Iterator<T> {
      private final Iterator<? extends T> iter;
      private T lastFetched;
      
      StackFrame(Iterator<? extends T> iter) {
         this.iter = iter;
      }
      
      @Override
      public boolean hasNext() {
         return iter.hasNext();
      }
      
      @Override
      public T next() {
         return (lastFetched = iter.next());
      }
      
      @Override
      public void remove() {
         iter.remove();
      }
      
      T lastFetched() {
         return lastFetched;
      }
   }
   
   /**
    * An iterator through all entries in a nested map. This iterates through all entries in all leaf
    * levels of the nested map.
    *
    * @param <T> the type of value fetched from the iterator
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class EntryIterator<T> implements Iterator<T>, NestedKey<K> {
      private final BiFunction<NestedKey<K>, Entry<K, V>, T> fn;
      private final StackFrame<Entry<K, Level<K, V>>> stack[];
      private StackFrame<Entry<K, V>> top;
      private boolean needNext;
      
      @SuppressWarnings("unchecked")
      EntryIterator(Level<K, V> level, BiFunction<NestedKey<K>, Entry<K, V>, T> fn) {
         this.fn = fn;
         this.stack = (StackFrame<Entry<K, Level<K, V>>>[]) new StackFrame<?>[level.height()];
         findNext(level.entryIterator());
      }
      
      private void findNext(Iterator<Entry<K, Level<K, V>>> initialIter) {
         int len = stack.length;
         int i;
         Iterator<Entry<K, Level<K, V>>> iter;
         boolean pushing;
         if (initialIter == null) {
            i = -1; // -1 is "top", >=0 is in "stack"
            iter = null;
            pushing = false;
         } else {
            i = len - 1;
            iter = stack[i] = new StackFrame<>(initialIter);
            pushing = true;
         }
         // as we navigate iterators, we can either be pushing iterators onto the stack, finding
         // the next leaf value, or (when a level has no more descendants) popping back up the stack
         // to find an ancestor with more values (and then we push back down...)
         while (true) {
            if (pushing) {
               assert iter != null;
               while (i >= 0 && iter.hasNext()) {
                  Entry<K, Level<K, V>> entry = iter.next();
                  iter = entry.getValue().entryIterator();
                  if (i >= 0) {
                     stack[--i] = new StackFrame<>(iter);
                  } else {
                     assert !iter.hasNext();
                     top = new StackFrame<>(entry.getValue().valueIterator());
                     if (top.hasNext()) {
                        // found a leaf
                        return;
                     }
                  }
               }
               // found a dead end, need to pop back up
               pushing = false;
            }
            while (true) {
               if (++i >= len) {
                  // we reached end of iteration; no more
                  return;
               }
               iter = stack[i];
               if (iter.hasNext()) {
                  // found a level with more values, so push down to a leaf
                  pushing = true;
                  break;
               }
            }
         }
      }

      private void maybeFindNext() {
         if (needNext) {
            findNext(null);
         }
      }
      
      @Override
      public boolean hasNext() {
         maybeFindNext();
         return top != null && top.hasNext();
      }

      @Override
      public T next() {
         maybeFindNext();
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         T ret = fn.apply(this, top.next());
         needNext = true;
         return ret;
      }

      @Override
      public void remove() {
         if (top == null) {
            throw new IllegalStateException();
         }
         top.remove();
         if (!top.hasNext()) {
            // recursively remove parent levels if they are now empty
            for (int i = 0, len = stack.length; i < len; i++) {
               StackFrame<Entry<K, Level<K, V>>> frame = stack[i];
               if (frame.lastFetched().getValue().isEmpty()) {
                  frame.remove();
               } else {
                  break;
               }
            }
         }
      }

      @Override
      public K getKey(int height) {
         int actualHeight = keyOrder[height];
         return (actualHeight == 0 ? top : stack[actualHeight - 1]).lastFetched().getKey();
      }
   }

   /**
    * An iterator through nested entries at a given <strong>non-leaf</strong> level of the map.
    *
    * @param <T> the type of value fetched from the iterator
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class LevelIterator<T> implements Iterator<T>, NestedKey<K> {
      private final int offset;
      private final StackFrame<Entry<K, Level<K, V>>> stack[];
      private final BiFunction<NestedKey<K>, Entry<K, Level<K, V>>, T> fn;

      @SuppressWarnings("unchecked")
      LevelIterator(Level<K, V> level, BiFunction<NestedKey<K>, Entry<K, Level<K, V>>, T> fn,
            int heightOfLevel) {
         assert heightOfLevel >= 1 && heightOfLevel <= level.height();
         offset = heightOfLevel;
         stack = (StackFrame<Entry<K, Level<K, V>>>[])
               new StackFrame<?>[level.height() - offset + 1];
         this.fn = fn;
         findNext(iteratorFor(level));
      }

      private void findNext(Iterator<Entry<K, Level<K, V>>> initialIter) {
         int len = stack.length;
         int i;
         Iterator<Entry<K, Level<K, V>>> iter;
         boolean pushing;
         if (initialIter == null) {
            i = 0;
            iter = null;
            pushing = false;
         } else {
            i = len - 1;
            iter = stack[i] = new StackFrame<>(initialIter);
            pushing = true;
         }
         // as we navigate iterators, we can either be pushing iterators onto the stack, finding
         // the next leaf value, or (when a level has no more descendants) popping back up the stack
         // to find an ancestor with more values (and then we push back down...)
         while (true) {
            if (pushing) {
               assert iter != null;
               while (iter.hasNext()) {
                  if (i == 0) {
                     // reached a leaf
                     return;
                  }
                  Entry<K, Level<K, V>> entry = iter.next();
                  iter = iteratorFor(entry.getValue());
                  stack[i] = new StackFrame<>(iter);
               }
               // otherwise: found a dead end, need to pop back up
               pushing = false;
            }
            while (true) {
               if (++i >= len) {
                  // we reached end of iteration; no more
                  return;
               }
               iter = stack[i];
               if (iter.hasNext()) {
                  // found a level with more values, so push down to a leaf
                  pushing = true;
                  break;
               }
            }
         }
      }
      
      private Iterator<Entry<K, Level<K, V>>> iteratorFor(Level<K, V> level) {
         int l = level.height();
         assert l > 0;
         if (!filterKeyPresentByNesting[l]) {
            return level.entryIterator();
         } else {
            // must filter this level
            K k = filterKeysByNesting[l];
            @SuppressWarnings("unchecked")
            Map<K, Level<K, V>> map = (Map<K, Level<K, V>>) level.map(); 
            Level<K, V> child = map.get(k);
            return child == null && !map.containsKey(k)
                  ? Collections.emptyIterator()
                  : new Iterator<Entry<K, Level<K, V>>>() {
                     // an iterator that returns just the single entry for k
                     int state;

                     @Override
                     public boolean hasNext() {
                        return state == 0;
                     }

                     @Override
                     public Entry<K, Level<K, V>> next() {
                        if (state != 0) {
                           throw new NoSuchElementException();
                        }
                        state = 1;
                        return new AbstractMap.SimpleImmutableEntry<>(k, child);
                     }
                     
                     @Override
                     public void remove() {
                        if (state != 1) {
                           throw new IllegalStateException();
                        }
                        state = 2;
                        map.remove(k);
                     }
                  };
         }
      }

      @Override
      public boolean hasNext() {
         return stack[0] != null && stack[0].hasNext();
      }
      
      @Override
      public T next() {
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         T ret = fn.apply(this, stack[0].next());
         if (!stack[0].hasNext()) {
            findNext(null);
         }
         return ret;
      }
      
      @Override
      public void remove() {
         StackFrame<Entry<K, Level<K, V>>> top = stack[0];
         if (top == null) {
            throw new IllegalStateException();
         }
         top.remove();
         if (!top.hasNext()) {
            // recursively remove parent levels if they are now empty
            for (int i = 1, len = stack.length; i < len; i++) {
               StackFrame<Entry<K, Level<K, V>>> frame = stack[i];
               if (frame.lastFetched().getValue().isEmpty()) {
                  frame.remove();
               } else {
                  break;
               }
            }
         }
      }

      @Override
      public K getKey(int height) {
         int actualHeight = keyOrder[height];
         return stack[actualHeight - offset].lastFetched().getKey();
      }
   }
}
