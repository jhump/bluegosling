package com.apriori.collections;

// TODO: javadoc
public interface ImmutableMap<K, V> extends Iterable<ImmutableMap.Entry<K, V>> {
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
