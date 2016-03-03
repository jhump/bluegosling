package com.bluegosling.collections.tries;

import com.bluegosling.collections.SortedArrayMap;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//TODO: javadoc
//TODO: tests
//TODO: don't forget serialization and cloning
//TODO: also add a CompactSortedArraySequenceTrie?
public class SortedArraySequenceTrie<K, V>
      extends AbstractNavigableSequenceTrie<K, V, SortedArraySequenceTrie.TrieNode<K, V>> {

   static class TrieNode<K, V> extends SortedArrayMap<K, TrieNode<K, V>>
         implements AbstractNavigableTrie.NavigableNode<K, Void, V, TrieNode<K, V>> {

      private static final long serialVersionUID = -1924824497591904579L;
      
      final TrieNode<K, V> parent;
      final K key;
      V value;
      boolean present;
      int count;

      TrieNode(K key, TrieNode<K, V> parent) {
         this.key = key;
         this.parent = parent;
      }
      
      @Override
      public K getKey() {
         return key;
      }

      @Override
      public boolean valuePresent() {
         return present;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public void setValue(V newValue) {
         this.value = newValue;
         this.present = true;
      }

      @Override
      public void clearValue() {
         this.value = null;
         this.present = false;
      }

      @Override
      public Void getLeafKey() {
         return null;
      }

      @Override
      public void setLeafKey(Void leafKey) {
      }

      @Override
      public int elementCount() {
         return count;
      }

      @Override
      public void incrementCount() {
         count++;
      }

      @Override
      public void decrementCount() {
         count--;
      }

      @Override
      public void addToCount(int delta) {
         count += delta;
      }

      @Override
      public TrieNode<K, V> getParent() {
         return parent;
      }

      @Override
      public Iterator<TrieNode<K, V>> childIterator() {
         return values().iterator();
      }

      @Override
      public Iterator<TrieNode<K, V>> descendingChildIterator() {
         return descendingMap().values().iterator();
      }
   }

   public SortedArraySequenceTrie() {
      super(null);
   }

   public SortedArraySequenceTrie(Comparator<? super K> componentComparator) {
      super(componentComparator);
   }

   public SortedArraySequenceTrie(Map<? extends List<K>, ? extends V> map) {
      super(null);
      putAll(map);
   }

   public SortedArraySequenceTrie(Map<? extends List<K>, ? extends V> map,
         Comparator<? super K> componentComparator) {
      super(componentComparator);
      putAll(map);
   }

   @Override
   protected TrieNode<K, V> newNode(K key, TrieNode<K, V> parent) {
      return new TrieNode<>(key, parent);
   }
}
