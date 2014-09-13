package com.apriori.collections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//TODO: javadoc
//TODO: tests
//TODO: implement serialization and cloning
public class HashSequenceTrie<K, V>
      extends AbstractSequenceTrie<K, V, HashSequenceTrie.TrieNode<K, V>> {

   static class TrieNode<K, V> extends HashMap<K, TrieNode<K, V>>
         implements AbstractTrie.Node<K, Void, V, TrieNode<K, V>> {

      private static final long serialVersionUID = -681414287890324281L;
      
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
      public Void getLeafKey() {
         return null;
      }

      @Override
      public void setLeafKey(Void leafKey) {
      }
   }

   public HashSequenceTrie() {
   }
   
   public HashSequenceTrie(Map<? extends List<K>, ? extends V> map) {
      putAll(map);
   }
   
   @Override
   protected TrieNode<K, V> newNode(K key, TrieNode<K, V> p) {
      return new TrieNode<>(key, p);
   }
}
