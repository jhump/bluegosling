package com.bluegosling.collections.tries;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import com.bluegosling.collections.maps.SortedArrayMap;

//TODO: javadoc
//TODO: don't forget serialization and cloning
//TODO: also add a CompactSortedArrayCompositeTrie?
public class SortedArrayCompositeTrie<K, C, V>
      extends AbstractNavigableCompositeTrie<K, C, V, SortedArrayCompositeTrie.TrieNode<K, C, V>> {

   static class TrieNode<K, C, V> extends SortedArrayMap<C, TrieNode<K, C, V>>
         implements AbstractNavigableTrie.NavigableNode<C, K, V, TrieNode<K, C, V>> {

      private static final long serialVersionUID = -1924824497591904579L;
      
      final TrieNode<K, C, V> parent;
      final C key;
      K leafKey;
      V value;
      boolean present;
      int count;

      TrieNode(C key, TrieNode<K, C, V> parent) {
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
      public K getLeafKey() {
         return leafKey;
      }

      @Override
      public void setLeafKey(K leafKey) {
         this.leafKey = leafKey;
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
      public TrieNode<K, C, V> getParent() {
         return parent;
      }

      @Override
      public Iterator<TrieNode<K, C, V>> childIterator() {
         return values().iterator();
      }

      @Override
      public Iterator<TrieNode<K, C, V>> descendingChildIterator() {
         return descendingMap().values().iterator();
      }
   }

   public SortedArrayCompositeTrie(Componentizer<? super K, ? extends C> componentizer) {
      super(componentizer);
   }
   
   public SortedArrayCompositeTrie(Componentizer<? super K, ? extends C> componentizer,
         Comparator<? super C> comparator) {
      super(componentizer, comparator);
   }
   
   public SortedArrayCompositeTrie(CompositeTrie<K, ? extends C, ? extends V> other) {
      this(other.componentizer());
      putAll(other);
   }

   public SortedArrayCompositeTrie(CompositeTrie<K, ? extends C, ? extends V> other,
         Comparator<? super C> comparator) {
      this(other.componentizer(), comparator);
      putAll(other);
   }

   public SortedArrayCompositeTrie(NavigableCompositeTrie<K, C, ? extends V> other) {
      this(other.componentizer(), other.componentComparator());
      putAll(other);
   }

   public SortedArrayCompositeTrie(Componentizer<? super K, ? extends C> componentizer,
         Map<? extends K, ? extends V> map) {
      this(componentizer);
      putAll(map);
   }

   public SortedArrayCompositeTrie(Componentizer<? super K, ? extends C> componentizer,
         Comparator<? super C> comparator, Map<? extends K, ? extends V> map) {
      this(componentizer, comparator);
      putAll(map);
   }

   @Override
   protected TrieNode<K, C, V> newNode(C key, TrieNode<K, C, V> parent) {
      return new TrieNode<>(key, parent);
   }
}
