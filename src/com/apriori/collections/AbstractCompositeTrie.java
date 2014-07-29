package com.apriori.collections;

import static com.apriori.collections.Iterables.upToN;

import com.apriori.possible.Reference;
import com.apriori.tuples.Trio;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

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
   Iterable<C> asIterable(Object key) {
      return Iterables.cast(componentizer.getComponents((K) key));
   }

   @Override
   public boolean containsKey(Object key) {
      N node = get(asIterable(key));
      return node != null && node.valuePresent();
   }

   @Override
   public V get(Object key) {
      N node = get(asIterable(key));
      return node != null ? node.getValue() : null;
   }

   @Override
   public V put(K key, V value) {
      return put(asIterable(key), key, value);
   }

   @Override
   public V remove(Object key) {
      return remove(asIterable(key)).orElse(null);
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
            return AbstractCompositeTrie.this.remove(asIterable(o)).isPresent();
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
      Collection<C> snapshot = Iterables.snapshot(prefix);
      return snapshot.isEmpty() ? this : new PrefixCompositeTrie<>(snapshot, this);
   }

   @Override
   public CompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents) {
      return prefixMap(upToN(prefix, numComponents));
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
         Collection<C> snapshot = Iterables.snapshot(newPrefix);
         return snapshot.isEmpty()
               ? this
               : new PrefixCompositeTrie<>(Iterables.concat(this.prefix, snapshot), parent);
      }
      
      protected Iterable<C> checkPrefix(Iterable<C> newKey) {
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
            throw new IllegalArgumentException();
         }
         return put(k, key, value);
      }
      
      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         List<Trio<Iterable<C>, K, V>> entries = new ArrayList<>(m.size());
         // first pass, check all prefixes and create list of new entries w/ suffixes
         for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            Iterable<C> k = checkPrefix(asIterable(key));
            if (k == null) {
               throw new IllegalArgumentException();
            }
            entries.add(Trio.create(k, key, entry.getValue()));
         }
         // they've all been checked for correct prefix; add 'em
         for (Trio<Iterable<C>, K, V> entry : entries) {
            put(entry.getFirst(), entry.getSecond(), entry.getThird());
         }
      }
      
      protected N getRoot() {
         N node = this.root;
         if (node == null || (node.isEmpty() && !node.valuePresent())
               || this.generation != parent.generation) {
            // node needs to be recomputed
            node = parent.get(prefix);
            this.root = node;
            assert this.root != parent.root;
         }
         return node;
      }

      @Override
      protected N get(Iterable<C> keys) {
         N node = getRoot();
         return node != null ? super.get(keys) : null;
      }
      
      // don't need to override put() since it uses ensurePath, which is overridden below
      
      @Override
      protected N ensurePath(Iterable<C> path) {
         // make sure we have a path to this prefix trie's root
         N node = this.root;
         if (node == null || (node.isEmpty() && !node.valuePresent())
               || this.generation != parent.generation) {
            // node needs to be recomputed
            this.root = node = parent.ensurePath(prefix);
            assert this.root != parent.root;
         }
         return ensurePath(node, path);
      }

      @Override
      protected Reference<V> remove(Iterable<C> keys) {
         N node = getRoot();
         return node != null ? super.remove(keys) : Reference.unset();
      }

      @Override
      protected N newNode(C key, N p) {
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
      protected <T> Iterator<T> entryIterator(
            BiFunction<EntryIterator<T, C, K, V, N>, N, T> producer) {
         N node = getRoot();
         if (node == null) {
            return Iterables.emptyIterator();
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
