package com.apriori.collections;

// TODO: implement me!
public abstract class HashBiMap<K, V> implements BiMap<K, V>  {
   
   private static class EntryImpl<K, V> {
      final Object key;
      boolean has1;
      V value1;
      boolean has2;
      K value2;
      
      EntryImpl(Object key) {
         this.key = key;
      }
   }
}
