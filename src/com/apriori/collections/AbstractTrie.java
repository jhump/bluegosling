package com.apriori.collections;

import com.apriori.possible.Reference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

// TODO: javadoc
abstract class AbstractTrie<K, X, V, N extends AbstractTrie.Node<K, X, V, N>> {
   
   interface Node<K, X, V, N extends Node<K, X, V, N>> {
      // operations for the key/value at this node
      K getKey();
      boolean valuePresent();
      V getValue();
      void setValue(V newValue);
      void clearValue();

      // optional methods for tries with alternate representations of keys
      X getLeafKey();
      void setLeafKey(X leafKey);

      // tracking number of elements in this node's sub-tree
      int elementCount();
      void incrementCount();
      void decrementCount();
      void addToCount(int delta);

      N getParent();
      Iterator<N> childIterator();

      // map operations, for working with direct children of this node
      N get(Object key);
      N put(K key, N node);
      N remove(Object key);
      boolean isEmpty();
      void clear();
   }
   
   protected N root;
   protected int generation;
   
   protected AbstractTrie() {
      this.root = newNode(null, null);
   }
   
   protected AbstractTrie(N root) {
      this.root = root;
   }
   
   protected abstract N newNode(K key, N parent);
   
   protected N get(Iterable<K> keys) {
      Iterator<K> iter = keys.iterator();
      N node = root;
      while (iter.hasNext() && node != null) {
         K k = iter.next();
         node = node.get(k);
      }
      return node;
   }
   
   protected N ensurePath(Iterable<K> keys) {
      return ensurePath(root, keys);
   }
   
   protected final N ensurePath(N start, Iterable<K> keys) {
      Iterator<K> iter = keys.iterator();
      N node = start;
      while (iter.hasNext()) {
         K k = iter.next();
         N next = node.get(k);
         if (next == null) {
            next = newNode(k, node);
            node.put(k, next);
         }
         node = next;
      }
      return node;
   }
   
   protected V put(Iterable<K> keys, X leafKey, V value) {
      N node = ensurePath(keys);
      V ret;
      if (node.valuePresent()) {
         ret = node.getValue();
      } else {
         ret = null;
         for (N n = node; n != null; n = n.getParent()) {
            n.incrementCount();
         }
      }
      node.setLeafKey(leafKey);
      node.setValue(value);
      return ret;
   }
   
   protected Reference<V> remove(Iterable<K> keys) {
      Iterator<K> iter = keys.iterator();
      N node = root;
      while (iter.hasNext()) {
         K k = iter.next();
         node = node.get(k);
         if (node == null) {
            return Reference.unset();
         }
      }
      if (!node.valuePresent()) {
         return Reference.unset();
      }
      V v = node.getValue();
      remove(node);
      return Reference.setTo(v);
   }
   
   protected void remove(N node) {
      assert node.valuePresent();
      node.clearValue();
      for (N n = node; n != null; n = n.getParent()) {
         n.decrementCount();
      }
      while (node != null && !node.valuePresent() && node.isEmpty()) {
         N parent = node.getParent();
         if (parent != null) {
            parent.remove(node.getKey());
         }
         node = parent;
      }
   }
   
   public void clear() {
      root.clear();
      root.clearValue();
      generation++;
   }
   
   public boolean containsValue(Object value) {
      ArrayDeque<Iterator<N>> stack = new ArrayDeque<>();
      N node = root;
      while (true) {
         if (node.valuePresent() && Objects.equals(node.getValue(), value)) {
            return true;
         }
         Iterator<N> iter = node.childIterator();
         while (!iter.hasNext()) {
            if (stack.isEmpty()) {
               return false;
            }
            iter = stack.pop();
         }
         node = iter.next();
         stack.push(iter);
      }
   }
   
   public int size() {
      return root.elementCount();
   }
   
   public boolean isEmpty() {
      return size() == 0;
   }
   
   protected <T> Iterator<T> entryIterator(
         BiFunction<EntryIterator<T, K, X, V, N>, N, T> producer) {
      return new EntryIterator<>(root, producer);
   }
   
   protected <T> Iterator<T> entryIteratorFrom(
         BiFunction<EntryIterator<T, K, X, V, N>, N, T> producer, N startAt) {
      return new EntryIterator<>(root, producer, startAt);
   }

   protected List<K> createKeyList(N start, N end) {
      List<K> ret = new ArrayList<>();
      N node = start;
      while (node != null && node != end) {
         ret.add(node.getKey());
         node = node.getParent();
      }
      assert node == end;
      Collections.reverse(ret);
      return Collections.unmodifiableList(ret);
   }
   
   protected static class EntryImpl<T, K, X, V, N extends Node<K, X, V, N>> implements Entry<T, V> {
      final T key;
      final N node;
      
      EntryImpl(T key, N node) {
         this.key = key;
         this.node = node;
      }

      @Override
      public T getKey() {
         return key;
      }

      @Override
      public V getValue() {
         if (!node.valuePresent()) {
            throw new ConcurrentModificationException();
         }
         return node.getValue();
      }

      @Override
      public V setValue(V value) {
         if (!node.valuePresent()) {
            throw new ConcurrentModificationException();
         }
         V ret = node.getValue();
         node.setValue(value);
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
   }
   
   protected static class EntryIterator<T, K, X, V, N extends Node<K, X, V, N>>
         implements Iterator<T> {
      
      final ArrayDeque<StackFrame<K, X, V, N>> frames = new ArrayDeque<>();
      final BiFunction<EntryIterator<T, K, X, V, N>, N, T> producer;
      boolean first = true;
      N lastFetched;
      
      protected EntryIterator(N root, BiFunction<EntryIterator<T, K, X, V, N>, N, T> producer) {
         this(root, producer, root);
      }

      protected EntryIterator(N root, BiFunction<EntryIterator<T, K, X, V, N>, N, T> producer,
            N startAt) {
         while(true) {
            assert startAt != null;
            frames.push(newStackFrame(startAt));
            if (startAt == root) {
               break;
            }
            startAt = startAt.getParent();
         }
         this.producer = producer;
      }

      private StackFrame<K, X, V, N> newStackFrame(N node) {
         return new StackFrame<>(node, node.childIterator());
      }
      
      @Override
      public boolean hasNext() {
         for (StackFrame<K, X, V, N> frame : frames) {
            if (first && frame.node.valuePresent()) {
               // has initial element in the root node 
               return true;
            }
            if (frame.iter.hasNext()) {
               return true;
            }
         }
         return false;
      }

      @Override
      public T next() {
         if (frames.isEmpty()) {
            throw new NoSuchElementException();
         }
         StackFrame<K, X, V, N> top = frames.peek();
         if (first) {
            // have to special case initial conditions
            first = false;
            if (top.node.valuePresent()) {
               return producer.apply(this, lastFetched = top.node);
            }
         }
         // Find next node:
         // First, pop off any nodes whose ancestors have been exhausted.
         while (!top.iter.hasNext()) {
            StackFrame<K, X, V, N> frame = frames.pop();
            assert frame == top;
            if (frames.isEmpty()) {
               throw new NoSuchElementException();
            }
            top = frames.peek();
         }
         // Then drill down to find next node that has a value.
         N node;
         while (true) {
            node = top.iter.next();
            frames.push(top = newStackFrame(node));
            if (node.valuePresent()) {
               break;
            }
            assert top.iter.hasNext();
         }
         return producer.apply(this, lastFetched = node);
      }
      
      public List<K> createKeyList() {
         // TODO: Use AmtPersistentList instead of ArrayDeque so we don't need to create a new
         // list every time? Then we could instead just return a "view" of the persistent list.
         List<K> ret = new ArrayList<>(frames.size() - 1);
         boolean isFirst = true;
         for (Iterator<StackFrame<K, X, V, N>> iter = frames.descendingIterator();
               iter.hasNext(); ) {
            if (isFirst) {
               // skip the first
               isFirst = false;
               continue;
            }
            ret.add(iter.next().node.getKey());
         }
         return Collections.unmodifiableList(ret);
      }
      
      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         if (!lastFetched.valuePresent()) {
            throw new ConcurrentModificationException();
         }
         lastFetched.clearValue();
         for (N n = lastFetched; n != null; n = n.getParent()) {
            n.decrementCount();
         }
         if (lastFetched.isEmpty()) {
            // Remove dead sub-tries. Must use iterators' remove() method so we don't accidentally
            // cause ConcurrentModificationExceptions on subsequent use of this iterator
            boolean isFirst = true;
            for (Iterator<StackFrame<K, X, V, N>> iter = frames.descendingIterator();
                  iter.hasNext(); ) {
               StackFrame<K, X, V, N> frame = iter.next();
               if (isFirst) {
                  assert !frame.iter.hasNext();
                  isFirst = false;
               } else {
                  frame.iter.remove();
                  if (!frame.node.isEmpty()) {
                     // the rest of the ancestors have other content, so we're done pruning
                     break;
                  }
               }
            }
         }
         lastFetched = null;
      }
   }
   
   static class StackFrame<K, X, V, N extends Node<K, X, V, N>> {
      final N node;
      final Iterator<N> iter;
      
      StackFrame(N node, Iterator<N> iter) {
         this.node = node;
         this.iter = iter;
      }
   }
}
