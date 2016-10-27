package com.bluegosling.collections.tries;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.MapUtils;
import com.bluegosling.collections.MoreIterables;
import com.bluegosling.possible.Reference;
import com.bluegosling.tuples.Triple;
import com.google.common.collect.Iterables;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
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
 * An abstract base class for {@link CompositeTrie} implementations. Concrete sub-classes need only
 * provide a concrete implementation of {@link Node} and override the {@link #newNode} method. This
 * is ideal for tries where each node uses some sort of map to store the edges (which map key
 * components to sub-tries).
 *
 * @param <K> the type of keys in the map
 * @param <C> the component type of keys in the map (each key represents a sequence of components)  
 * @param <V> the type of values in the map
 * @param <N> the concrete type of trie node
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractCompositeTrie<K, C, V, N extends AbstractTrie.Node<C, K, V, N>>
      extends AbstractTrie<C, K, V, N> implements CompositeTrie<K, C, V> {

   final Componentizer<? super K, ? extends C> componentizer;
   
   public AbstractCompositeTrie(Componentizer<? super K, ? extends C> componentizer) {
      this.componentizer = componentizer;
   }
   
   @Override
   public Componentizer<? super K, ? extends C> componentizer() {
      return componentizer;
   }
   
   @SuppressWarnings("unchecked")
   Iterable<C> tryAsIterable(Object key) {
      try {
         return MoreIterables.cast(componentizer.getComponents((K) key));
      } catch (ClassCastException e) {
         return null;
      }
   }

   Iterable<C> asIterable(K key) {
      return MoreIterables.cast(componentizer.getComponents(key));
   }

   @Override
   public boolean containsKey(Object key) {
      Iterable<C> iter = tryAsIterable(key);
      if (iter == null) {
         return false;
      }
      N node = get(iter);
      return node != null && node.valuePresent();
   }

   @Override
   public V get(Object key) {
      Iterable<C> iter = tryAsIterable(key);
      if (iter == null) {
         return null;
      }
      N node = get(iter);
      return node != null ? node.getValue() : null;
   }

   @Override
   public V put(K key, V value) {
      return put(asIterable(key), key, value);
   }

   @Override
   public V remove(Object key) {
      Iterable<C> iter = tryAsIterable(key);
      if (iter == null) {
         return null;
      }
      return remove(iter).orElse(null);
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
         put(entry.getKey(), entry.getValue());
      }
   }

   @Override
   public Set<K> keySet() {
      return new AbstractSet<K>() {
         @Override
         public Iterator<K> iterator() {
            return entryIterator((iter, node) -> node.getLeafKey());
         }

         @Override
         public int size() {
            return AbstractCompositeTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            return containsKey(o);
         }
         
         @Override
         public boolean remove(Object o) {
            Iterable<C> iter = tryAsIterable(o);
            if (iter == null) {
               return false;
            }
            return AbstractCompositeTrie.this.remove(iter).isPresent();
         }

         @Override
         public void clear() {
            AbstractCompositeTrie.this.clear();
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
            return AbstractCompositeTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            return containsValue(o);
         }

         @Override
         public void clear() {
            AbstractCompositeTrie.this.clear();
         }
      };
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new AbstractSet<Entry<K, V>>() {
         @Override
         public Iterator<Entry<K, V>> iterator() {
            return entryIterator((iter, node) -> new EntryImpl<>(node.getLeafKey(), node));
         }

         @Override
         public int size() {
            return AbstractCompositeTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
               return false;
            }
            Entry<?, ?> other = (Entry<?, ?>) o;
            Object key = other.getKey();
            Iterable<C> iter = tryAsIterable(key);
            if (iter == null) {
               return false;
            }
            N node = get(iter);
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
            Iterable<C> iter = tryAsIterable(key);
            if (iter == null) {
               return false;
            }
            N node = get(iter);
            if (node == null || !node.valuePresent()
                  || !Objects.equals(node.getValue(), other.getValue())) {
               return false;
            }
            AbstractCompositeTrie.this.remove(node);
            return true;
         }
         
         @Override
         public void clear() {
            AbstractCompositeTrie.this.clear();
         }
      };
   }

   @Override
   public CompositeTrie<K, C, V> prefixMapByKey(K prefix) {
      return prefixMap(asIterable(prefix));
   }

   @Override
   public CompositeTrie<K, C, V> prefixMapByKey(K prefix, int numComponents) {
      return prefixMap(asIterable(prefix), numComponents);
   }
   
   @Override
   public CompositeTrie<K, C, V> prefixMap(C prefix) {
      return prefixMap(Collections.singleton(prefix));
   }

   @Override
   public CompositeTrie<K, C, V> prefixMap(Iterable<C> prefix) {
      Collection<C> snapshot = MoreIterables.snapshot(prefix);
      return snapshot.isEmpty() ? this : new PrefixCompositeTrie<>(snapshot, this);
   }

   @Override
   public CompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents) {
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

   static class PrefixCompositeTrie<K, C, V, N extends Node<C, K, V, N>>
         extends AbstractCompositeTrie<K, C, V, N> {
      final Iterable<C> prefix;
      final AbstractCompositeTrie<K, C, V, N> parent;

      PrefixCompositeTrie(Iterable<C> prefix, AbstractCompositeTrie<K, C, V, N> parent) {
         super(parent.componentizer);
         this.prefix = prefix;
         this.parent = parent;
         this.generation = parent.generation;
         this.root = parent.get(prefix);
      }
      
      @Override
      public CompositeTrie<K, C, V> prefixMap(Iterable<C> newPrefix) {
         Collection<C> snapshot = MoreIterables.snapshot(newPrefix);
         return snapshot.isEmpty() ? this : new PrefixCompositeTrie<>(Iterables.concat(
               this.prefix, snapshot), parent);
      }
      
      Iterable<C> checkPrefix(Iterable<C> newKey) {
         Iterator<C> iter = newKey.iterator();
         for (C prefixComponent : prefix) {
            if (!iter.hasNext()) {
               // new key is too short!
               return null;
            }
            C keyComponent = iter.next();
            if (!Objects.equals(prefixComponent, keyComponent)) {
               // new key doesn't match prefix!
               return null;
            }
         }
         return () -> iter;
      }
      
      @Override
      public V put(K key, V value) {
         Iterable<C> k = checkPrefix(asIterable(key));
         if (k == null) {
            throw new IllegalArgumentException(
                  "Key, " + key + ", does not have prefix " + CollectionUtils.toString(prefix));
         }
         return put(k, key, value);
      }
      
      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         List<Triple<Iterable<C>, K, V>> entries = new ArrayList<>(m.size());
         // first pass, check all prefixes and create list of new entries w/ suffixes
         for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            Iterable<C> k = checkPrefix(asIterable(key));
            if (k == null) {
               throw new IllegalArgumentException(
                     "Key, " + key + ", does not have prefix " + CollectionUtils.toString(prefix));
            }
            entries.add(Triple.of(k, key, entry.getValue()));
         }
         // they've all been checked for correct prefix; add 'em
         for (Triple<Iterable<C>, K, V> entry : entries) {
            put(entry.getFirst(), entry.getSecond(), entry.getThird());
         }
      }
      
      N getRoot() {
         if (root == null || (root.isEmpty() && !root.valuePresent())
               || this.generation != parent.generation) {
            // root needs to be recomputed
            root = parent.get(prefix);
            assert root != parent.root;
         }
         return root;
      }
      
      @Override
      N get(Iterable<C> keys) {
         keys = checkPrefix(keys);
         if (keys == null) {
            return null;
         }
         N node = getRoot();
         return node != null ? super.get(keys) : null;
      }
      
      // don't need to override put() since it uses ensurePath, which is overridden below
      
      @Override
      N ensurePath(Iterable<C> path) {
         // make sure we have a path to this prefix trie's root
         if (root == null || (root.isEmpty() && !root.valuePresent())
               || this.generation != parent.generation) {
            // root needs to be recomputed
            root = parent.ensurePath(prefix);
            assert root != parent.root;
         }
         return ensurePath(root, path);
      }

      @Override
      Reference<V> remove(Iterable<C> keys) {
         keys = checkPrefix(keys);
         if (keys == null) {
            return Reference.unset();
         }
         N node = getRoot();
         return node != null ? super.remove(keys) : Reference.unset();
      }

      @Override
      N newNode(C key, N p) {
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
      <T> Iterator<T> entryIterator(BiFunction<Supplier<List<C>>, N, T> producer) {
         N node = getRoot();
         if (node == null) {
            return Collections.emptyIterator();
         }
         return new EntryIterator<T, C, K, V, N>(node, producer) {
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
