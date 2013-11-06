package com.apriori.collections;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

//TODO: javadoc
//TODO: tests
//TODO: implement serialization and cloning
public class HashSequenceTrie<K, V> extends AbstractMap<List<K>, V>
      implements SequenceTrie<K, V> {

   @SuppressWarnings("serial")
   private static class Node<K, V> extends HashMap<K, Node<K, V>> {
      V value;
      boolean present;
      
      Node() {
      }
   }
   
   int size;
   final Node<K, V> root = new Node<K, V>();
   int modCount;
   
   public HashSequenceTrie() {
   }
   
   public HashSequenceTrie(Map<? extends List<K>, ? extends V> map) {
      putAll(map);
   }
   
   @Override
   public SequenceTrie<K, V> prefixMap(K prefix) {
      return new PrefixMap(toArray(prefix));
   }
   
   @Override
   public SequenceTrie<K, V> prefixMap(List<K> prefix) {
      return new PrefixMap(toArray(prefix));
   }
   
   @Override
   public SequenceTrie<K, V> prefixMap(List<K> prefix, int numComponents) {
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

   Node<K, V> get(Iterator<?> iter, Node<K, V> node) {
      if (iter == null) {
         return null;
      }
      while (iter.hasNext()) {
         Object item = iter.next();
         node = node.get(item);
         if (node == null) {
            break;
         }
      }
      return node;
   }
   
   V remove(Iterator<?> iter, Node<K, V> node) {
      if (iter == null || node == null) {
         return null;
      } else if (!iter.hasNext()) {
         V ret = node.value;
         node.value = null;
         if (node.present) {
            node.present = false;
            size--;
         }
         return ret;
      } else {
         Object item = iter.next();
         Node<K, V> successor = node.get(item);
         V ret = remove(iter, successor);
         if (successor != null && !successor.present && successor.isEmpty()) {
            // prune empty node from the trie
            node.remove(item);
         }
         return ret;
      }
   }
   
   V put(Iterator<K> iter, V value, Node<K, V> node) {
      while (iter.hasNext()) {
         K k = iter.next();
         Node<K, V> next = node.get(k);
         if (next == null) {
            next = new Node<K, V>();
            node.put(k, next);
         }
         node = next;
      }
      V ret = node.value;
      node.value = value;
      if (!node.present) {
         node.present = true;
         size++;
      }
      return ret;
   }
   
   @Override
   public V get(Object o) {
      Node<K, V> node = get(iterator(o), root);
      return node == null ? null : node.value;
   }
   
   @Override
   public V remove(Object o) {
      return remove(iterator(o), root);
   }
   
   @Override
   public boolean containsKey(Object o) {
      Node<K, V> node = get(iterator(o), root);
      return node == null ? false : node.present;
   }

   @Override
   public V put(List<K> key, V value) {
      return put(key.iterator(), value, root);
   }
   
   @Override
   public Set<Entry<List<K>, V>> entrySet() {
      return new EntrySet();
   }
   
   private class PrefixMap extends AbstractMap<List<K>, V> implements SequenceTrie<K, V> {

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
            node = node.get(key);
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
      public SequenceTrie<K, V> prefixMap(List<K> prefix) {
         return new PrefixMap(toArray(prefix), this);
      }

      @Override
      public SequenceTrie<K, V> prefixMap(List<K> prefix, int numComponents) {
         return new PrefixMap(toArray(prefix, numComponents), this);
      }

      @Override
      public V get(Object o) {
         Node<K, V> node = HashSequenceTrie.this.get(iterator(o), getPrefixRoot());
         return node == null ? null : node.value;
      }
      
      @Override
      public V remove(Object o) {
         return HashSequenceTrie.this.remove(iterator(o), getPrefixRoot());
      }
      
      @Override
      public boolean containsKey(Object o) {
         Node<K, V> node = HashSequenceTrie.this.get(iterator(o), getPrefixRoot());
         return node == null ? false : node.present;
      }

      @Override
      public V put(List<K> key, V value) {
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
      public Set<Entry<List<K>, V>> entrySet() {
         return new PrefixEntrySet(this);
      }
   }
   
   private class EntrySet extends AbstractSet<Entry<List<K>, V>> {
      EntrySet() {
      }
      
      @Override
      public Iterator<Entry<List<K>, V>> iterator() {
         return new EntryIterator(root);
      }

      @Override
      public int size() {
         return size;
      }
   }
   
   private class PrefixEntrySet extends AbstractSet<Entry<List<K>, V>> {
      private final PrefixMap prefixMap;
      
      PrefixEntrySet(PrefixMap prefixMap) {
         this.prefixMap = prefixMap;
      }
      
      @SuppressWarnings("synthetic-access")
      @Override
      public Iterator<Entry<List<K>, V>> iterator() {
         return new EntryIterator(prefixMap.getPrefixRoot(), prefixMap.path);
      }

      @Override
      public int size() {
         return prefixMap.size();
      }
   }
   
   private static class StackFrame<K, V> {
      Node<K, V> node;
      private Iterator<Entry<K, Node<K, V>>> childIterator;
      Entry<K, Node<K, V>> currentChildEntry;
      
      StackFrame(Node<K, V> node) {
         this.node = node;
         childIterator = node.entrySet().iterator();
      }
      
      boolean hasMoreChildren() {
         return childIterator.hasNext();
      }
      
      Node<K, V> nextChild() {
         currentChildEntry = childIterator.next();
         return currentChildEntry.getValue();
      }
      
      Node<K, V> currentChild() {
         return currentChildEntry.getValue();
      }
      
      K currentChildKey() {
         return currentChildEntry.getKey();
      }
      
      void removeCurrentChild() {
         childIterator.remove();
      }
   }
   
   static final Object[] EMPTY_ARRAY = new Object[0];
   
   Entry<List<K>, V> entry(final List<K> key, final Node<K, V> node) {
      // TODO: check modification count; confirm value is present before setting
      return new Entry<List<K>, V>() {
         @Override
         public List<K> getKey() {
            return key;
         }

         @Override
         public V getValue() {
            return node.value;
         }

         @Override
         public V setValue(V value) {
            V ret = node.value;
            node.value = value;
            return ret;
         }
         
         @Override
         public boolean equals(Object o) {
            return MapUtils.equals(this, o);
         }
         
         @Override
         public int hashCode() {
            return MapUtils.hashCode(this);
         }
         
         @Override
         public String toString() {
            return MapUtils.toString(this);
         }
      };
   }
   
   private class EntryIterator implements Iterator<Entry<List<K>, V>> {
      // TODO: fail fast for concurrent modification exception

      private int hasNext;
      final K[] prefix;
      private final ArrayDeque<StackFrame<K, V>> stack = new ArrayDeque<StackFrame<K, V>>();
      private boolean hasFetched;
      private boolean hasRemoved;
      
      @SuppressWarnings("unchecked")
      EntryIterator(Node<K, V> start) {
         this(start, (K[]) EMPTY_ARRAY);
      }
      
      EntryIterator(Node<K, V> start, K[] prefix) {
         this.prefix = prefix;
         if (!isEmpty()) {
            StackFrame<K, V> first = new StackFrame<K, V>(start); 
            stack.push(first);
            hasNext = first.hasMoreChildren() || first.node.present ? 1 : 0;
         } else {
            hasNext = 0; 
         }
      }
      
      @Override
      public boolean hasNext() {
         if (hasNext == -1) {
            hasNext = 0;
            for (StackFrame<K, V> frame : stack) {
               if (frame.hasMoreChildren()) {
                  hasNext = 1;
                  break;
               }
            }
         }
         return hasNext == 1;
      }

      @Override
      public Entry<List<K>, V> next() {
         if (hasNext == 0) {
            throw new NoSuchElementException();
         }
         StackFrame<K, V> frame = stack.peek();
         if (hasFetched || !frame.node.present) {
            while (!frame.hasMoreChildren()) {
               stack.pop(); // remove the frame with no more elements
               if (stack.isEmpty()) {
                  assert hasNext == -1;
                  hasNext = 0;
                  throw new NoSuchElementException();
               }
               frame = stack.peek();
            }
            do {
               assert frame.hasMoreChildren();
               frame = new StackFrame<K, V>(frame.nextChild());
               stack.push(frame);
            } while (!frame.node.present);
         }
         hasNext = -1;
         hasFetched = true;
         hasRemoved = false;
         return entry(keySequence(), frame.node);
      }
      
      private List<K> keySequence() {
         @SuppressWarnings("unchecked")
         final K current[] = (K[]) new Object[stack.size() - 1];
         final int length = prefix.length + current.length;
         Iterator<StackFrame<K, V>> iter = stack.descendingIterator();
         for (int i = 0; i < current.length; i++) {
            assert iter.hasNext();
            current[i] = iter.next().currentChildKey();
         }
         // last item is end of sequence (but has no key information)
         assert iter.hasNext();
         return new AbstractList<K>() {
            @Override
            public K get(int index) {
               if (index < 0) {
                  throw new IndexOutOfBoundsException(index + " < 0");
               } else if (index >= length) {
                  throw new IndexOutOfBoundsException(index + " >= " + length);
               }
               return index >= prefix.length
                     ? current[index - prefix.length]
                     : prefix[index];
            }

            @Override
            public int size() {
               return length;
            }
         };
      }

      @Override
      public void remove() {
         if (!hasFetched || hasRemoved) {
            throw new IllegalStateException();
         }
         hasRemoved = true;
         Iterator<StackFrame<K, V>> iter = stack.iterator();
         StackFrame<K, V> frame = iter.next();
         assert frame.node.present;
         // clear value and we're done
         frame.node.present = false;
         frame.node.value = null;
         size--;
         if (frame.node.isEmpty()) {
            // if the node is now empty, we should remove it
            while (iter.hasNext()) {
               StackFrame<K, V> parent = iter.next();
               assert parent.currentChild() == frame.node;
               parent.removeCurrentChild();
               if (parent.node.isEmpty() && !parent.node.present) {
                  // parent is empty and not present? then we need to delete it, too
                  frame = parent;
               } else {
                  break;
               }
            }
         }
      }
   }
}
