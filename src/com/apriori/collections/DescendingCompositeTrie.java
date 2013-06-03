package com.apriori.collections;

import java.util.Comparator;

//TODO: javadoc
class DescendingCompositeTrie<K, C, V> extends DescendingMap<K, V>
      implements NavigableCompositeTrie<K, C, V> {

   DescendingCompositeTrie(NavigableCompositeTrie<K, C, V> base) {
      super(base);
   }
   
   @SuppressWarnings("unchecked")
   NavigableCompositeTrie<K, C, V> base() {
      return (NavigableCompositeTrie<K, C, V>) base;
   }

   @Override
   public CompositeTrie.Componentizer<? super K, ? extends C> componentizer() {
      return base().componentizer();
   }
   
   @Override 
   public Comparator<? extends C> componentComparator() {
      return base().componentComparator();
   }
   
   @Override
   public NavigableCompositeTrie<K, C, V> prefixMap(K prefix) {
      return new DescendingCompositeTrie<K, C, V>(base().prefixMap(prefix));
   }
   
   @Override 
   public NavigableCompositeTrie<K, C, V> prefixMap(K prefix, int numComponents) {
      return new DescendingCompositeTrie<K, C, V>(base().prefixMap(prefix, numComponents));
   }
   
   @Override 
   public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix) {
      return new DescendingCompositeTrie<K, C, V>(base().prefixMap(prefix));
   }
   
   @Override 
   public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents) {
      return new DescendingCompositeTrie<K, C, V>(base().prefixMap(prefix, numComponents));
   }

   @Override 
   public NavigableCompositeTrie<K, C, V> descendingMap() {
      return base();
   }

   @Override 
   public NavigableCompositeTrie<K, C, V> subMap(K fromKey, boolean fromInclusive,
         K toKey, boolean toInclusive) {
      return new DescendingCompositeTrie<K, C, V>(base().subMap(fromKey, fromInclusive, toKey, toInclusive));
   }

   @Override
   public NavigableCompositeTrie<K, C, V> headMap(K toKey, boolean inclusive) {
      return new DescendingCompositeTrie<K, C, V>(base().headMap(toKey, inclusive));
   }

   @Override 
   public NavigableCompositeTrie<K, C, V> tailMap(K fromKey, boolean inclusive) {
      return new DescendingCompositeTrie<K, C, V>(base().tailMap(fromKey, inclusive));
   }

   @Override 
   public NavigableCompositeTrie<K, C, V> subMap(K fromKey, K toKey) {
      return new DescendingCompositeTrie<K, C, V>(base().subMap(fromKey, toKey));
   }

   @Override 
   public NavigableCompositeTrie<K, C, V> headMap(K toKey) {
      return new DescendingCompositeTrie<K, C, V>(base().headMap(toKey));
   }

   @Override 
   public NavigableCompositeTrie<K, C, V> tailMap(K fromKey) {
      return new DescendingCompositeTrie<K, C, V>(base().tailMap(fromKey));
   }
}
