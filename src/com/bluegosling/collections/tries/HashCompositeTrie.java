package com.bluegosling.collections.tries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//TODO: javadoc
//TODO: tests
//TODO: implement serialization and cloning
public class HashCompositeTrie<K, C, V>
      extends AbstractCompositeTrie<K, C, V, HashCompositeTrie.TrieNode<C, K, V>> {

   static class TrieNode<C, K, V> extends HashMap<C, TrieNode<C, K, V>>
         implements AbstractTrie.Node<C, K, V, TrieNode<C, K, V>> {
      
      private static final long serialVersionUID = -3245742966082088036L;
      
      final TrieNode<C, K, V> parent;
      final C key;
      K leafKey;
      V value;
      boolean present;
      int count;
      
      TrieNode(C key, TrieNode<C, K, V> parent) {
         this.key = key;
         this.parent = parent;
      }
      
      @Override
      public C getKey() {
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
         if (present) {
            value = null;
            present = false;
            count--;
         }
      }

      @Override
      public void clear() {
         super.clear();
         count = present ? 1 : 0;
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
      public TrieNode<C, K, V> getParent() {
         return parent;
      }
      
      @Override
      public Iterator<TrieNode<C, K, V>> childIterator() {
         return values().iterator();
      }
      
      @Override
      public K getLeafKey() {
         return leafKey;
      }
      
      @Override
      public void setLeafKey(K leafKey) {
         this.leafKey = leafKey;
      }
   }

   public HashCompositeTrie(Componentizer<? super K, ? extends C> componentizer) {
      super(componentizer);
   }
   
   public HashCompositeTrie(CompositeTrie<K, ? extends C, ? extends V> other) {
      this(other.componentizer());
      putAll(other);
   }

   public HashCompositeTrie(Componentizer<? super K, ? extends C> componentizer,
         Map<? extends K, ? extends V> map) {
      this(componentizer);
      putAll(map);
   }

   @Override
   protected TrieNode<C, K, V> newNode(C key, TrieNode<C, K, V> p) {
      return new TrieNode<>(key, p);
   }
}
