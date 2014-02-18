package com.apriori.collections;

import java.util.Map;

/**
 * An immutable, read-only set of key+value mappings. This interface is similar to the standard
 * {@link Map} interface except that it defines no mutation operations.
 * 
 * <p>The {@link java.util.Iterator#remove() remove()} operation of an immutable map's
 * {@link #iterator()} always throws {@link UnsupportedOperationException}. Similarly the
 * {@link #iterator()} for any of the collection views of the map will also always throw
 * {@link UnsupportedOperationException}.

 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public interface ImmutableMap<K, V> extends Iterable<ImmutableMap.Entry<K, V>> {
   
   /** 
    * A key-value-pair entry in an immutable map.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Entry<K, V> {
      /**
       * Retrieves the key associated with this entry.
       *
       * @return the key for this entry
       */
      K key();

      /**
       * Retrieves the value associated with this entry.
       *
       * @return the value for this entry
       */
      V value();
      
      /**
       * Determines if the given object is equal to this entry. This returns true if the given
       * object is also an {@link ImmutableMap.Entry} and has an equal key and equal value. Two
       * objects are considered equal if the following expression is true:<br/>
       * {@code o1 == null ? o2 == null : o1.equals(o2)}.
       *
       * @param o the object to test
       * @return true if the given object is equal to this entry
       */
      @Override boolean equals(Object o);
      
      /**
       * Computes the hash code for this entry. The hash code should be the result of XOR'ing the
       * hash code of the key with the hash code of the value. If the key or value is null, its hash
       * code is considered zero for the purpose of this computation.
       *
       * @return the hash code for this entry
       */
      @Override int hashCode();
   }
   
   /**
    * Determines the number of entries in this map.
    *
    * @return the number of entries in this map
    */
   int size();
   
   /**
    * Determines whether this map is empty or not. The map is empty if and only if it contains zero
    * entries.
    *
    * @return true if this map is entry, false otherwise
    */
   boolean isEmpty();
   
   boolean containsKey(Object o);
   
   boolean containsAllKeys(Iterable<?> keys);

   boolean containsAnyKey(Iterable<?> keys);
   
   boolean containsValue(Object o);
   
   boolean containsAllValues(Iterable<?> values);
   
   boolean containsAnyValue(Iterable<?> values);
   
   V get(Object key);
   
   ImmutableSet<K> keySet();
   
   ImmutableCollection<V> values();
   
   ImmutableSet<Entry<K, V>> entrySet();
   
   @Override boolean equals(Object o);
   
   @Override int hashCode();
}
