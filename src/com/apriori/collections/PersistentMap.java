
package com.apriori.collections;

import java.util.Map;

// TODO: javadoc
// TODO: other methods like Map's new default methods (putIfAbsent, computeIfAbsent, replace, etc)
public interface PersistentMap<K, V> extends ImmutableMap<K, V> {
   PersistentMap<K, V> put(K key, V value);
   PersistentMap<K, V> remove(Object o);
   PersistentMap<K, V> removeAll(Iterable<?> keys);
   PersistentMap<K, V> retainAll(Iterable<?> keys);
   PersistentMap<K, V> putAll(Map<? extends K, ? extends V> items);
   PersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items);
}
