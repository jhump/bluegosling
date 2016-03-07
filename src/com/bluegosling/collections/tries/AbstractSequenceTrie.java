package com.bluegosling.collections.tries;

import com.bluegosling.collections.MapUtils;
import com.bluegosling.collections.MoreIterables;
import com.bluegosling.possible.Reference;
import com.google.common.collect.Iterables;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * An abstract base class for {@link SequenceTrie} implementations. Concrete sub-classes need only
 * provide a concrete implementation of {@link Node} and override the {@link #newNode} method. This
 * is ideal for tries where each node uses some sort of map to store the edges (which map key
 * elements to sub-tries).
 *
 * @param <K> the component type of keys in the map (a key will be a sequence of these components)  
 * @param <V> the type of values in the map
 * @param <N> the concrete type of trie node
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractSequenceTrie<K, V, N extends AbstractTrie.Node<K, Void, V, N>>
      extends AbstractTrie<K, Void, V, N> implements SequenceTrie<K, V> {

   @SuppressWarnings("unchecked")
   Iterable<K> asIterable(Object key) {
      return (Iterable<K>) key;
   }

   @Override
   public boolean containsKey(Object key) {
      if (!(key instanceof Iterable)) {
         return false;
      }
      N node = get(asIterable(key));
      return node != null && node.valuePresent();
   }

   @Override
   public V get(Object key) {
      if (!(key instanceof Iterable)) {
         return null;
      }
      N node = get(asIterable(key));
      return node != null ? node.getValue() : null;
   }

   @Override
   public V put(Iterable<K> key, V value) {
      return put(key, null, value);
   }
   
   @Override
   public V put(List<K> key, V value) {
      return put(key, null, value);
   }

   @Override
   public V remove(Object key) {
      if (!(key instanceof Iterable)) {
         return null;
      }
      return remove(asIterable(key)).orElse(null);
   }

   @Override
   public void putAll(Map<? extends List<K>, ? extends V> m) {
      for (Entry<? extends List<K>, ? extends V> entry : m.entrySet()) {
         put(entry.getKey(), entry.getValue());
      }
   }
   
   @Override
   public Set<List<K>> keySet() {
      return new AbstractSet<List<K>>() {
         @Override
         public Iterator<List<K>> iterator() {
            return entryIterator((supplier, node) -> supplier.get());
         }

         @Override
         public int size() {
            return AbstractSequenceTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            return containsKey(o);
         }
         
         @Override
         public boolean remove(Object o) {
            if (!(o instanceof Iterable)) {
               return false;
            }
            return AbstractSequenceTrie.this.remove(asIterable(o)).isPresent();
         }

         @Override
         public void clear() {
            AbstractSequenceTrie.this.clear();
         }
      };
   }

   @Override
   public Collection<V> values() {
      return new AbstractCollection<V>() {
         @Override
         public Iterator<V> iterator() {
            return entryIterator((iter, node) -> node.getValue());
         }

         @Override
         public int size() {
            return AbstractSequenceTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            return containsValue(o);
         }

         @Override
         public void clear() {
            AbstractSequenceTrie.this.clear();
         }
      };
   }

   @Override
   public Set<Entry<List<K>, V>> entrySet() {
      return new AbstractSet<Entry<List<K>, V>>() {
         @Override
         public Iterator<Entry<List<K>, V>> iterator() {
            return entryIterator((supplier, node) -> new EntryImpl<>(supplier.get(), node));
         }

         @Override
         public int size() {
            return AbstractSequenceTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
               return false;
            }
            Entry<?, ?> other = (Entry<?, ?>) o;
            Object key = other.getKey();
            if (!(key instanceof Iterable)) {
               return false;
            }
            N node = get(asIterable(key));
            return node != null && node.valuePresent()
                  && Objects.equals(node.getValue(), other.getValue());
         }

         @Override
         public boolean remove(Object o) {
            if (!(o instanceof Entry)) {
               return false;
            }
            Entry<?, ?> other = (Entry<?, ?>) o;
            Object key = other.getKey();
            if (!(key instanceof Iterable)) {
               return false;
            }
            N node = get(asIterable(key));
            if (node == null || !node.valuePresent()
                  || !Objects.equals(node.getValue(), other.getValue())) {
               return false;
            }
            AbstractSequenceTrie.this.remove(node);
            return true;
         }
         
         @Override
         public void clear() {
            AbstractSequenceTrie.this.clear();
         }
      };
   }

   @Override
   public SequenceTrie<K, V> prefixMap(K prefix) {
      return prefixMap(Collections.singleton(prefix));
   }

   @Override
   public SequenceTrie<K, V> prefixMap(Iterable<K> prefix) {
      Collection<K> snapshot = MoreIterables.snapshot(prefix);
      return snapshot.isEmpty() ? this : new PrefixSequenceTrie<>(snapshot, this);
   }

   @Override
   public SequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents) {
      return prefixMap(Iterables.limit(prefix, numComponents));
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
   
   static class PrefixSequenceTrie<K, V, N extends Node<K, Void, V, N>>
         extends AbstractSequenceTrie<K, V, N> {
      final Iterable<K> prefix;
      final AbstractSequenceTrie<K, V, N> parent;

      PrefixSequenceTrie(Iterable<K> prefix, AbstractSequenceTrie<K, V, N> parent) {
         this.prefix = prefix;
         this.parent = parent;
         this.generation = parent.generation;
         this.root = parent.get(prefix);
      }
   
      @Override
      public SequenceTrie<K, V> prefixMap(Iterable<K> newPrefix) {
         Collection<K> snapshot = MoreIterables.snapshot(newPrefix);
         return snapshot.isEmpty()
               ? this
               : new PrefixSequenceTrie<>(Iterables.concat(this.prefix, snapshot), parent);
      }
      
      N getRoot() {
         if (root == null || (root.isEmpty() && !root.valuePresent())
               || this.generation != parent.generation) {
            // root needs to be recomputed
            root = parent.get(prefix);
         }
         return root;
      }

      @Override
      protected N get(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.get(keys) : null;
      }
      
      // don't need to override put() since it uses ensurePath, which is overridden below
      
      @Override
      protected N ensurePath(Iterable<K> path) {
         // make sure we have a path to this prefix trie's root
         if (root == null || (root.isEmpty() && !root.valuePresent())
               || this.generation != parent.generation) {
            // root needs to be recomputed
            root = parent.ensurePath(prefix);
         }
         return ensurePath(root, path);
      }

      @Override
      protected Reference<V> remove(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.remove(keys) : Reference.unset();
      }

      @Override
      protected N newNode(K key, N p) {
         if (parent == null) {
            // This can only happen during initialization. Super-class constructor invokes newNode
            // to initialize root, but we haven't yet had the chance to initialize parent.
            assert Arrays.stream(Thread.currentThread().getStackTrace())
                  .anyMatch(ste -> {
                     return ste.getClassName().equals(getClass().getName())
                           && ste.getMethodName().equals("<init>");
                  });
            return null;
         }
         return parent.newNode(key, p);
      }
      
      @Override
      public void clear() {
         N node = getRoot();
         if (node == null) {
            // nothing to do
            return;
         }
         int removed = node.elementCount();
         assert removed > 0;
         node.clear();
         node.clearValue();
         assert node.getParent() != null;
         while (node != null) {
            N p = node.getParent();
            if (p != null) {
               p.addToCount(-removed);
               if (node.isEmpty() && !node.valuePresent()) {
                  p.remove(node.getKey());
               }
            }
            node = p;
         }
         this.root = null;
         parent.generation++;
      }
      
      @Override
      public int size() {
         N node = getRoot();
         return node != null ? node.elementCount() : 0;
      }
      
      @Override
      protected <T> Iterator<T> entryIterator(BiFunction<Supplier<List<K>>, N, T> producer) {
         N node = getRoot();
         if (node == null) {
            return Collections.emptyIterator();
         }
         return new EntryIterator<T, K, Void, V, N>(node, producer) {
            @Override public void remove() {
               super.remove();
               // if this removal caused prefix trie to become empty, we may need to
               // propagate removal up to parent trie to prune dead sub-tries
               if (!node.valuePresent() && node.isEmpty()) {
                  for (N n = node, p = node.getParent(); p != null; n = p, p = p.getParent()) {
                     p.remove(n.getKey());
                  }
               }
            }
         };
      }
   }
}
