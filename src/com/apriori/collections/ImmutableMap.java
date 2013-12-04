package com.apriori.collections;

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
      K key();
      V value();
      @Override boolean equals(Object o);
      @Override int hashCode();
   }
   
   int size();
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
