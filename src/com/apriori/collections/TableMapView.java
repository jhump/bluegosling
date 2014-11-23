package com.apriori.collections;

import java.util.Map;

/**
 * A view of data in a table as a {@link Map}.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of views in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface TableMapView<K, V> extends Map<K, V> {

   /**
    * Returns a view of the value for a given key. If the value is itself a map or nested table and
    * the key is not present, this will return an empty such map or table, not {@code null}. The
    * returned view is modifiable. Changes to the returned view are visible via other operations on
    * the map and vice versa.
    * 
    * <p>Note that if this map is a view that does not allow adding elements then the returned view
    * will also not allow adding elements. In such a case this method and {@link #get(Object)} will
    * not actually behave differently.
    *
    * @param key the key
    * @return a modifiable view of the value for the given key, an empty map or table if no such key
    *       exists
    * @see #get(Object)
    */
   V getViewForKey(K key);

   /**
    * Returns the value mapped to the given key. Unlike normal maps, if the value is itself a map or
    * nested table and the key is not present, this will return an empty such map or table, not
    * {@code null}. The returned view is <strong>not</strong> fully modifiable. In particular, new
    * elements/mappings cannot be added through the returned view. But deletions are allowed.
    * Deletions on the returned view are visible via other operations on the map. Other changes made
    * via map operations are visible via the returned view.
    *
    * @param key the key
    * @return a view of the value for the given key
    * @see #getViewForKey(Object)
    */
   @Override
   V get(Object key);

   /**
    * Removes the value mapped to the given key and returns it. Unlike normal maps, if the value is
    * itself a map or nested table and the key is not present, this will return an empty such map
    * or table, not {@code null}.
    *
    * @param key the key of the mapping to remove
    * @return the value that was removed, possibly empty if the given key was not present
    * @see #get(Object)
    */
   @Override
   V remove(Object key);
}
