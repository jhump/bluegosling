package com.bluegosling.collections.views;

import com.bluegosling.collections.tries.NavigableSequenceTrie;

import java.util.Comparator;
import java.util.List;


// TODO javadoc
public class DescendingSequenceTrie<K, V> extends DescendingMap<List<K>, V>
      implements NavigableSequenceTrie<K, V> {

   public DescendingSequenceTrie(NavigableSequenceTrie<K, V> base) {
      super(base);
   }
   
   NavigableSequenceTrie<K, V> base() {
      return (NavigableSequenceTrie<K, V>) base;
   }
   
   @Override
   public V put(Iterable<K> key, V value) {
      return base().put(key, value);
   }

   @Override
   public Comparator<? super K> componentComparator() {
      return base().componentComparator();
   }
   
   @Override 
   public NavigableSequenceTrie<K, V> prefixMap(K prefix) {
      return new DescendingSequenceTrie<K, V>(base().prefixMap(prefix));
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
   public NavigableSequenceTrie<K, V> subMap(List<K> fromKey, boolean fromInclusive,
         List<K> toKey, boolean toInclusive) {
      return new DescendingSequenceTrie<>(
            base().subMap(fromKey, fromInclusive, toKey, toInclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(List<K> toKey, boolean inclusive) {
      return new DescendingSequenceTrie<>(base().headMap(toKey, inclusive));
      
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(List<K> fromKey, boolean inclusive) {
      return new DescendingSequenceTrie<>(base().tailMap(fromKey, inclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> subMap(List<K> fromKey, List<K> toKey) {
      return new DescendingSequenceTrie<>(base().subMap(fromKey, toKey));
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(List<K> toKey) {
      return new DescendingSequenceTrie<>(base().headMap(toKey));
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(List<K> fromKey) {
      return new DescendingSequenceTrie<>(base().tailMap(fromKey));
   }
   
   @Override
   public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, boolean fromInclusive,
         Iterable<K> toKey, boolean toInclusive) {
      return new DescendingSequenceTrie<>(
            base().subMap(fromKey, fromInclusive, toKey, toInclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey, boolean inclusive) {
      return new DescendingSequenceTrie<>(base().headMap(toKey, inclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey, boolean inclusive) {
      return new DescendingSequenceTrie<>(base().tailMap(fromKey, inclusive));
   }

   @Override
   public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, Iterable<K> toKey) {
      return new DescendingSequenceTrie<>(base().subMap(fromKey, toKey));
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey) {
      return new DescendingSequenceTrie<>(base().headMap(toKey));
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey) {
      return new DescendingSequenceTrie<>(base().tailMap(fromKey));
   }
   
   @Override
   public Entry<List<K>, V> lowerEntry(Iterable<K> key) {
      return base().higherEntry(key);
   }

   @Override
   public List<K> lowerKey(Iterable<K> key) {
      return base().higherKey(key);
   }

   @Override
   public Entry<List<K>, V> floorEntry(Iterable<K> key) {
      return base().ceilingEntry(key);
   }

   @Override
   public List<K> floorKey(Iterable<K> key) {
      return base().ceilingKey(key);
   }

   @Override
   public Entry<List<K>, V> ceilingEntry(Iterable<K> key) {
      return base().floorEntry(key);
   }

   @Override
   public List<K> ceilingKey(Iterable<K> key) {
      return base().floorKey(key);
   }

   @Override
   public Entry<List<K>, V> higherEntry(Iterable<K> key) {
      return base().lowerEntry(key);
   }

   @Override
   public List<K> higherKey(Iterable<K> key) {
      return base().lowerKey(key);
   }
}
