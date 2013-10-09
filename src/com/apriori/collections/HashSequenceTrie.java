package com.apriori.collections;

import com.apriori.possible.Holder;
import com.apriori.possible.Possible;
import com.apriori.possible.Reference;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

//TODO: javadoc
//TODO: tests
//TODO: implement serialization and cloning
//TODO: also add a CompactHashSequenceTrie?
public class HashSequenceTrie<K, V> extends AbstractMap<Iterable<K>, V>
      implements SequenceTrie<K, V> {

   private static class Node<K, V> {
      Holder<V> value;
      HashMap<K, Node<K, V>> successors = new HashMap<K, Node<K, V>>();
      
      Node() {
      }
   }
   
   int size;
   final Node<K, V> root = new Node<K, V>();
   int modCount;
   
   public HashSequenceTrie() {
   }
   
   public HashSequenceTrie(Map<? extends Iterable<K>, ? extends V> map) {
      putAll(map);
   }
   
   @Override
   public SequenceTrie<K, V> prefixMap(K prefix) {
      return new PrefixMap(toArray(prefix));
   }
   
   @Override
   public SequenceTrie<K, V> prefixMap(Iterable<K> prefix) {
      return new PrefixMap(toArray(prefix));
   }
   
   @Override
   public SequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents) {
      return new PrefixMap(toArray(prefix, numComponents));
   }
   
   static Iterator<?> iterator(Object o) {
      return o instanceof Iterable ? ((Iterable<?>) o).iterator() : null;
   }
   
   @SuppressWarnings("unchecked")
   static <K> K[] toArray(K key) {
      return (K[]) new Object[] { key };
   }

   @SuppressWarnings("unchecked")
   static <K> K[] toArray(Iterable<K> keys) {
      ArrayList<K> l = new ArrayList<K>();
      for (K k : keys) {
         l.add(k);
      }
      return (K[]) l.toArray();
   }

   static <K> K[] toArray(Iterable<K> keys, int num) {
      @SuppressWarnings("unchecked")
      K[] ret = (K[]) new Object[num];
      Iterator<K> iter = keys.iterator();
      for (int i = 0; i < num; i++) {
         if (iter.hasNext()) {
            K k = iter.next();
            ret[i] = k;
         } else {
            throw new IllegalArgumentException();
         }
      }
      return ret;
   }

   Possible<V> get(Iterator<?> iter, Node<K, V> node) {
      if (iter == null) {
         return null;
      }
      while (iter.hasNext()) {
         Object item = iter.next();
         node = node.successors.get(item);
         if (node == null) {
            break;
         }
      }
      return node == null ? Reference.<V>unset() : node.value;
   }
   
   V remove(Iterator<?> iter, Node<K, V> node) {
      if (iter == null || node == null) {
         return null;
      } else if (!iter.hasNext()) {
         return node.value.clear();
      } else {
         Object item = iter.next();
         Node<K, V> successor = node.successors.get(item);
         V ret = remove(iter, successor);
         if (!successor.value.isPresent() && successor.successors.isEmpty()) {
            // prune empty node from the trie
            node.successors.remove(item);
         }
         return ret;
      }
   }
   
   V put(Iterator<K> iter, V value, Node<K, V> node) {
      while (iter.hasNext()) {
         K k = iter.next();
         Node<K, V> next = node.successors.get(k);
         if (next == null) {
            next = new Node<K, V>();
            node.successors.put(k, next);
         }
         node = next;
      }
      return node.value.set(value);
   }
   
   @Override
   public V get(Object o) {
      return get(iterator(o), root).getOr(null);
   }
   
   @Override
   public V remove(Object o) {
      return remove(iterator(o), root);
   }
   
   @Override
   public boolean containsKey(Object o) {
      return get(iterator(o), root).isPresent();
   }

   @Override
   public V put(Iterable<K> key, V value) {
      return put(key.iterator(), value, root);
   }
   
   @Override
   public Set<Entry<Iterable<K>, V>> entrySet() {
      return new EntrySet();
   }
   
   private class PrefixMap extends AbstractMap<Iterable<K>, V> implements SequenceTrie<K, V> {

      private final K[] path;
      private int myModCount = modCount;
      private int prefixMapSize = -1;
      private Node<K, V> prefixRoot;
      private boolean hasRoot;
      
      PrefixMap(K path[]) {
         this.path = path;
      }
      
      @SuppressWarnings("unchecked")
      PrefixMap(K path[], PrefixMap start) {
         this.path = (K[]) new Object[path.length + start.path.length];
         System.arraycopy(start.path, 0, this.path, 0, start.path.length);
         System.arraycopy(path, 0, this.path, start.path.length, path.length);
         prefixRoot = findPrefixRoot(path, start.getPrefixRoot());
      }
      
      private Node<K, V> findPrefixRoot(K searchPath[], Node<K, V> start) {
         Node<K, V> node = start;
         for (K key : searchPath) {
            node = node.successors.get(key);
            if (node == null) {
               break;
            }
         }
         return node;
      }
      
      Node<K, V> getPrefixRoot() {
         if (hasRoot && myModCount == modCount) {
            return prefixRoot;
         }
         myModCount = modCount;
         prefixMapSize = -1;
         hasRoot = true;
         return prefixRoot = findPrefixRoot(path, root);
      }

      @Override
      public SequenceTrie<K, V> prefixMap(K prefix) {
         return new PrefixMap(toArray(prefix), this);
      }

      @Override
      public SequenceTrie<K, V> prefixMap(Iterable<K> prefix) {
         return new PrefixMap(toArray(prefix), this);
      }

      @Override
      public SequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents) {
         return new PrefixMap(toArray(prefix, numComponents), this);
      }

      @Override
      public V get(Object o) {
         return HashSequenceTrie.this.get(iterator(o), getPrefixRoot()).getOr(null);
      }
      
      @Override
      public V remove(Object o) {
         return HashSequenceTrie.this.remove(iterator(o), getPrefixRoot());
      }
      
      @Override
      public boolean containsKey(Object o) {
         return HashSequenceTrie.this.get(iterator(o), getPrefixRoot()).isPresent();
      }

      @Override
      public V put(Iterable<K> key, V value) {
         return HashSequenceTrie.this.put(key.iterator(), value, getPrefixRoot());
      }
      
      @Override
      public int size() {
         if (prefixMapSize != -1 && myModCount == modCount) {
            return prefixMapSize;
         }
         myModCount = modCount;
         hasRoot = false;
         prefixMapSize = 0;
         for (Iterator<?> iter = entrySet().iterator(); iter.hasNext();) {
            prefixMapSize++;
         }
         return prefixMapSize;
      }
      
      @Override
      public Set<Entry<Iterable<K>, V>> entrySet() {
         return new PrefixEntrySet(this);
      }
   }
   
   private class EntrySet extends AbstractSet<Entry<Iterable<K>, V>> {
      EntrySet() {
      }
      
      @Override
      public Iterator<Entry<Iterable<K>, V>> iterator() {
         return new EntryIterator(root);
      }

      @Override
      public int size() {
         return size;
      }
   }
   
   private class PrefixEntrySet extends AbstractSet<Entry<Iterable<K>, V>> {
      private final PrefixMap prefixMap;
      
      PrefixEntrySet(PrefixMap prefixMap) {
         this.prefixMap = prefixMap;
      }
      
      @SuppressWarnings("synthetic-access")
      @Override
      public Iterator<Entry<Iterable<K>, V>> iterator() {
         return new EntryIterator(prefixMap.getPrefixRoot(), prefixMap.path);
      }

      @Override
      public int size() {
         return prefixMap.size();
      }
   }
   
   private class EntryIterator implements Iterator<Entry<Iterable<K>, V>> {
      private final Node<K, V> start;
      private final K[] prefix;
      private final ArrayDeque<Node<K, V>> stack = new ArrayDeque<Node<K, V>>();
      private Node<K, V> lastFetched;
      
      @SuppressWarnings("unchecked")
      EntryIterator(Node<K, V> start) {
         this(start, (K[]) new Object[0]);
      }
      
      EntryIterator(Node<K, V> start, K[] prefix) {
         this.start = start;
         this.prefix = prefix;
      }
      
      @Override
      public boolean hasNext() {
         // TODO Auto-generated method stub
         return false;
      }

      @Override
      public Entry<Iterable<K>, V> next() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public void remove() {
         // TODO Auto-generated method stub
      }
   }
}
