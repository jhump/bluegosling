package com.apriori.collections;

import java.util.Comparator;


// TODO javadoc
class DescendingSequenceTrie<K, V> extends DescendingMap<Iterable<K>, V>
      implements NavigableSequenceTrie<K, V> {

   DescendingSequenceTrie(NavigableSequenceTrie<K, V> base) {
      super(base);
   }
   
   NavigableSequenceTrie<K, V> base() {
      return (NavigableSequenceTrie<K, V>) base;
   }

   @Override
   public Comparator<? extends K> componentComparator() {
      return base().componentComparator();
   }
   
   @Override 
   public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix) {
      return new DescendingSequenceTrie<K, V>(base().prefixMap(prefix));
   }

   @Override 
   public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents) {
      return new DescendingSequenceTrie<K, V>(base().prefixMap(prefix, numComponents));
   }
   
   @Override 
   public NavigableSequenceTrie<K, V> descendingMap() {
      return base();
   }

   @Override 
   public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, boolean fromInclusive,
         Iterable<K> toKey, boolean toInclusive) {
      return new DescendingSequenceTrie<K, V>(base().subMap(fromKey, fromInclusive, toKey, toInclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey, boolean inclusive) {
      return new DescendingSequenceTrie<K, V>(base().headMap(toKey, inclusive));
      
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey, boolean inclusive) {
      return new DescendingSequenceTrie<K, V>(base().tailMap(fromKey, inclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, Iterable<K> toKey) {
      return new DescendingSequenceTrie<K, V>(base().subMap(fromKey, toKey));
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey) {
      return new DescendingSequenceTrie<K, V>(base().headMap(toKey));
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey) {
      return new DescendingSequenceTrie<K, V>(base().tailMap(fromKey));
   }
}
