package com.apriori.collections;

import java.util.Iterator;
import java.util.Map;

// TODO: javadoc
// TODO: tests
public class HamtPersistentMap<K, V> implements PersistentMap<K, V> {

   @Override
   public Iterator<Entry<K, V>> iterator() {
      // TODO: implement me
      return null;
   }
   
   @Override
   public ImmutableIterator<Entry<K, V>> immutableIterator() {
      // TODO: implement me
      return null;
   }
   
   @Override
   public boolean containsKey(Object o) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsAllKeys(Iterable<?> keys) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsAnyKey(Iterable<?> keys) {
      // TODO: implement me
      return false;
   }

   @Override
   public V get(Object key) {
      // TODO: implement me
      return null;
   }

   @Override
   public ImmutableSet<K> keySet() {
      // TODO: implement me
      return null;
   }

   @Override
   public ImmutableCollection<V> values() {
      // TODO: implement me
      return null;
   }

   @Override
   public ImmutableSet<Entry<K, V>> entrySet() {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> put(K key, V value) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> remove(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> removeAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> retainAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> putAll(Map<? extends K, ? extends V> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public int size() {
      // TODO: implement me
      return 0;
   }

   @Override
   public boolean isEmpty() {
      // TODO: implement me
      return false;
   }
}
