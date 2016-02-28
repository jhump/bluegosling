package com.bluegosling.collections;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A {@link List} that can have associative keys and can be viewed as a {@link Map}. Each item in
 * the list can have an optional associative key and can be queried either by its position or by
 * that associative key.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the list
 * @param <K> the type of optional associative keys
 */
public interface AssociativeList<E, K> extends List<E> {
   /**
    * Adds an element with an associative key. If the specified key was previously associated with
    * a different element, the index of that element is returned.
    * 
    * @param element the new element to add to the end of the list
    * @param key an associative key for the new element
    * @return the index previously associated with the specified key or -1 if it was not previously
    *       mapped
    */
   int add(E element, K key);
   
   /**
    * Adds an element with an associative key. If the specified key was previously associated with
    * a different element, the index of that element is returned.
    * 
    * @param index the index at which the new element is inserted
    * @param element the new element to add to the end of the list
    * @param key an associative key for the new element
    * @return the index previously associated with the specified key or -1 if it was not previously
    *       mapped
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       size of this list
    */
   int add(int index, E element, K key);
   
   /**
    * Adds a collection of mapped entries from a map. The elements are added in whatever iteration
    * order is for the specified map. They are added to the end of the list. Any key that was
    * already associated with an element in the list will instead be associated with its
    * corresponding value in the specified map. The returned map, if not empty, contains any such
    * overwritten keys along with their previously associated list index.
    * 
    * @param mappedElements the values and associated keys to add to the list
    * @return a map of keys to previously associated indices
    */
   Map<K, Integer> addAll(Map<? extends K, ? extends E> mappedElements);

   /**
    * Adds a collection of mapped entries from a map. The elements are added in whatever iteration
    * order is for the specified map. Any key that was already associated with an element in the
    * list will instead be associated with its corresponding value in the specified map. The
    * returned map, if not empty, contains any such overwritten keys along with their previously
    * associated list index.
    * 
    * @param index the index at which the new elements are inserted
    * @param mappedElements the values and associated keys to add to the list
    * @return a map of keys to previously associated indices
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       size of this list
    */
   Map<K, Integer> addAll(int index, Map<? extends K, ? extends E> mappedElements);

   /**
    * Adds a collection of mapped entries from another {@link AssociativeList}. They are added to
    * the end of the list. Any key that was already associated with an element in the list will
    * instead be associated with its corresponding value in the specified map. The returned map, if
    * not empty, contains any such overwritten keys along with their previously associated list
    * index.
    * 
    * @param mappedElements the values and associated keys to add to the list
    * @return a map of keys to previously associated indices
    */
   Map<K, Integer> addAll(AssociativeList<? extends E, ? extends K> mappedElements);

   /**
    * Adds a collection of mapped entries from another {@link AssociativeList}. Any key that was
    * already associated with an element in the list will instead be associated with its
    * corresponding value in the specified map. The returned map, if not empty, contains any such
    * overwritten keys along with their previously associated list index.
    * 
    * @param index the index at which the new elements are inserted
    * @param mappedElements the values and associated keys to add to the list
    * @return a map of keys to previously associated indices
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       size of this list
    */
   Map<K, Integer> addAll(int index, AssociativeList<? extends E, ? extends K> mappedElements);

   /**
    * Adds a key for an existing list element. If the key was already associated with an element,
    * that previous list index is returned.
    * 
    * @param key the associative key
    * @param index the list index to associate with the key
    * @return the index previously associated with the specified key or -1 if it was not previously
    *       mapped
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       largest valid index in this list
    */
   int createKey(K key, int index);
   
   /**
    * Retrieves a list element via its associative key.
    * 
    * @param key the associative key
    * @return the associated list element or {@code null} if no such associative key exists
    */
   E getByKey(K key);
   
   /**
    * Finds the list index associated with a given key.
    * 
    * @param key the associative key
    * @return the associated list index or -1 if no such associative key exists
    */
   int getKeyIndex(K key);
   
   /**
    * Removes an element via its associative key.
    * 
    * @param key the associative key
    * @return both the value and the index at which it was located for the removed element or
    *       {@code null} if no such associative key exists
    */
   Map.Entry<Integer, E> removeByKey(K key);
   
   /**
    * Removes an associative key but leaves the list and associated element in tact. This just
    * removes the association of the key and the element.
    * 
    * @param key the associative key
    * @return the index that was associated with the specified key or -1 if it was not previously
    *       mapped
    */
   int forgetKey(K key);

   /**
    * Clears an associative key from a given list index. The list and associated element is left in
    * tact. This just removes the association of the key and the element.
    * 
    * @param index the index
    * @return the key that was associated with the specified index or {@code null} if it was not
    *       previously mapped
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       largest valid index in this list
    */
   K clearKey(int index);

   /**
    * Determines if the specified list index is associated with a key.
    * 
    * @param index index into the list
    * @return true if the specified index has an associative key; false otherwise
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       largest valid index in this list
    */
   boolean hasKey(int index);
   
   /**
    * Determines if the specified key is associated with an element in the list.
    * 
    * @param key the associative key
    * @return true if the associative key exists; false otherwise
    */
   boolean containsKey(K key);
   
   /**
    * Returns an iterator over the associative keys in the list in proper order. The iterator
    * skips over list indexes that are not mapped.
    * 
    * @return an iterator over the associative keys in the list
    */
   ListIterator<K> keyIterator();

   /**
    * Returns an iterator over the associative mappings in the list in proper order. The iterator
    * skips over list indexes that are not mapped.
    * 
    * @return an iterator over the associative mappings in the list
    */
   ListIterator<Map.Entry<K, E>> keyedEntryIterator();
   
   /**
    * Returns the next index in the list, that is greater than or equal to the specified index, that
    * has an associated key. The following code construct can be used to iterate through the
    * indexes with associative keys:
    * <pre>
    * for (int i = list.nextKeyedIndex(0); i &gt;= 0; i = list.nextKeyedIndex(i + 1)) {
    *   // i is an index associated with a key
    * }
    * </pre>
    * 
    * @param start the starting index for searching for an index with a key
    * @return the next index with a key that is greater than or equal to the specified starting
    *       index or -1 if no more keys exist
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than the
    *       size of the list
    */
   int nextKeyedIndex(int start);
   
   /**
    * Returns a view of the associative mappings in this list. The returned map is a read-only
    * view. All mutation operations will throw a {@link UnsupportedOperationException}.
    * 
    * @return a view of the mappings in this list
    */
   Map<K, E> asMap();
   
   /**
    * Returns a view of a subset of this list. The returned view is also an associative list.
    */
   @Override AssociativeList<E, K> subList(int fromIndex, int toIndex);
}
