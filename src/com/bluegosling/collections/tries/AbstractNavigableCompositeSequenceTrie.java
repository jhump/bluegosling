package com.bluegosling.collections.tries;

import com.bluegosling.collections.AbstractNavigableMap;
import com.bluegosling.collections.BoundType;
import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.MapUtils;
import com.bluegosling.collections.MoreIterables;
import com.bluegosling.collections.views.DescendingCompositeTrie;
import com.bluegosling.collections.views.DescendingSet;
import com.bluegosling.possible.Reference;
import com.bluegosling.tuples.Trio;
import com.google.common.collect.Iterables;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Supplier;

abstract class AbstractNavigableCompositeTrie<K, C, V, N extends AbstractNavigableTrie.NavigableNode<C, K, V, N>>
      extends AbstractNavigableTrie<C, K, V, N> implements NavigableCompositeTrie<K, C, V> {

   final Componentizer<? super K, ? extends C> componentizer;
   
   public AbstractNavigableCompositeTrie(Componentizer<? super K, ? extends C> componentizer) {
      super(null);
      this.componentizer = componentizer;
   }
   
   public AbstractNavigableCompositeTrie(Componentizer<? super K, ? extends C> componentizer,
         Comparator<? super C> componentComparator) {
      super(componentComparator);
      this.componentizer = componentizer;
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
   public Componentizer<? super K, ? extends C> componentizer() {
      return componentizer;
   }
   
   @Override
   public Comparator<K> comparator() {
      return Comparator.comparing(componentizer::getComponents,
            MoreIterables.comparator(componentComparator));
   }

   @Override
   public Comparator<? super C> componentComparator() {
      return componentComparator == Comparator.naturalOrder()
            ? null
            : componentComparator;
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
   
   Entry<K, V> getEntry(Object key) {
      Iterable<C> iter = tryAsIterable(key);
      if (iter == null) {
         return null;
      }
      N node = get(iter);
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }

   @Override
   public Entry<K, V> lowerEntry(K key) {
      N node = lowerNode(asIterable(key));
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }
   
   @Override
   public K lowerKey(K key) {
      N node = lowerNode(asIterable(key));
      return node != null ? node.getLeafKey() : null;
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
      N node = floorNode(asIterable(key));
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }

   @Override
   public K floorKey(K key) {
      N node = floorNode(asIterable(key));
      return node != null ? node.getLeafKey() : null;
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
      N node = ceilingNode(asIterable(key));
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }

   @Override
   public K ceilingKey(K key) {
      N node = ceilingNode(asIterable(key));
      return node != null ? node.getLeafKey() : null;
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
      N node = higherNode(asIterable(key));
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }

   @Override
   public K higherKey(K key) {
      N node = higherNode(asIterable(key));
      return node != null ? node.getLeafKey() : null;
   }

   @Override
   public Entry<K, V> firstEntry() {
      N node = firstNode();
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }

   @Override
   public Entry<K, V> lastEntry() {
      N node = lastNode();
      return node != null ? new EntryImpl<>(node.getLeafKey(), node) : null;
   }

   @Override
   public Entry<K, V> pollFirstEntry() {
      N node = firstNode();
      if (node == null) {
         return null;
      }
      remove(node);
      return new EntryImpl<>(node.getLeafKey(), node);
   }

   @Override
   public Entry<K, V> pollLastEntry() {
      N node = lastNode();
      if (node == null) {
         return null;
      }
      remove(node);
      return new EntryImpl<>(node.getLeafKey(), node);
   }

   @Override
   public K firstKey() {
      N node = firstNode();
      if (node == null) {
         throw new NoSuchElementException();
      }
      return node.getLeafKey();
   }

   @Override
   public K lastKey() {
      N node = lastNode();
      if (node == null) {
         throw new NoSuchElementException();
      }
      return node.getLeafKey();
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
   public NavigableCompositeTrie<K, C, V> descendingMap() {
      return new DescendingCompositeTrie<>(this);
   }
   
   @Override
   public Set<K> keySet() {
      return navigableKeySet();
   }
   
   @Override
   public NavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }
   
   @Override
   public NavigableSet<K> navigableKeySet() {
      return new KeySet();
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
            return AbstractNavigableCompositeTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            return containsValue(o);
         }

         @Override
         public void clear() {
            AbstractNavigableCompositeTrie.this.clear();
         }
      };
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new AbstractSet<Entry<K, V>>() {
         @Override
         public Iterator<Entry<K, V>> iterator() {
            return entryIterator((supplier, node) -> new EntryImpl<>(node.getLeafKey(), node));
         }

         @Override
         public int size() {
            return AbstractNavigableCompositeTrie.this.size();
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
            AbstractNavigableCompositeTrie.this.remove(node);
            return true;
         }
         
         @Override
         public void clear() {
            AbstractNavigableCompositeTrie.this.clear();
         }
      };
   }

   @Override
   public NavigableCompositeTrie<K, C, V> subMap(K fromKey, boolean fromInclusive,
         K toKey, boolean toInclusive) {
      return new SubTrie<>(this, fromKey, fromInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
            toKey, toInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> headMap(K toKey, boolean inclusive) {
      return new SubTrie<>(this, null, BoundType.NO_BOUND, toKey,
            inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> tailMap(K fromKey, boolean inclusive) {
      return new SubTrie<>(this, fromKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
            null, BoundType.NO_BOUND);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
   }
   
   @Override
   public NavigableCompositeTrie<K, C, V> prefixMapByKey(K prefix) {
      return prefixMap(asIterable(prefix));
   }

   @Override
   public NavigableCompositeTrie<K, C, V> prefixMapByKey(K prefix, int numComponents) {
      return prefixMap(asIterable(prefix), numComponents);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> prefixMap(C prefix) {
      return prefixMap(Collections.singleton(prefix));
   }

   @Override
   public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix) {
      Collection<C> snapshot = MoreIterables.snapshot(prefix);
      return snapshot.isEmpty() ? this : new NavigablePrefixCompositeTrie<>(snapshot, this);
   }

   @Override
   public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents) {
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
   
   static class NavigablePrefixCompositeTrie<K, C, V, N extends NavigableNode<C, K, V, N>>
         extends AbstractNavigableCompositeTrie<K, C, V, N> {
      final Iterable<C> prefix;
      final AbstractNavigableCompositeTrie<K, C, V, N> parent;

      NavigablePrefixCompositeTrie(Iterable<C> prefix,
            AbstractNavigableCompositeTrie<K, C, V, N> parent) {
         super(parent.componentizer, parent.componentComparator);
         this.prefix = prefix;
         this.parent = parent;
         this.generation = parent.generation;
         this.root = parent.get(prefix);
      }
   
      @Override
      public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> newPrefix) {
         Collection<C> snapshot = MoreIterables.snapshot(newPrefix);
         return snapshot.isEmpty() ? this : new NavigablePrefixCompositeTrie<>(
               Iterables.concat(this.prefix, snapshot), parent);
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
         List<Trio<Iterable<C>, K, V>> entries = new ArrayList<>(m.size());
         // first pass, check all prefixes and create list of new entries w/ suffixes
         for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            Iterable<C> k = checkPrefix(asIterable(key));
            if (k == null) {
               throw new IllegalArgumentException(
                     "Key, " + key + ", does not have prefix " + CollectionUtils.toString(prefix));
            }
            entries.add(Trio.create(k, key, entry.getValue()));
         }
         // they've all been checked for correct prefix; add 'em
         for (Trio<Iterable<C>, K, V> entry : entries) {
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

      @Override
      N firstNode() {
         N node = getRoot();
         return node != null ? super.firstNode() : null;
      }
      
      @Override
      N lastNode() {
         N node = getRoot();
         return node != null ? super.lastNode() : null;
      }

      @Override
      N floorNode(Iterable<C> keys) {
         N node = getRoot();
         return node != null ? super.floorNode(keys) : null;
      }

      @Override
      N ceilingNode(Iterable<C> keys) {
         N node = getRoot();
         return node != null ? super.ceilingNode(keys) : null;
      }
      
      @Override
      N lowerNode(Iterable<C> keys) {
         N node = getRoot();
         return node != null ? super.lowerNode(keys) : null;
      }

      @Override
      N higherNode(Iterable<C> keys) {
         N node = getRoot();
         return node != null ? super.higherNode(keys) : null;
      }
      
      // don't need to override put() since it uses ensurePath, which is overridden below
      
      @Override
      N ensurePath(Iterable<C> path) {
         // make sure we have a path to this prefix trie's root
         N node = this.root;
         if (node == null || (node.isEmpty() && !node.valuePresent())
               || this.generation != parent.generation) {
            // node needs to be recomputed
            this.root = node = parent.ensurePath(prefix);
         }
         return ensurePath(node, path);
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
               for (N n = node, p = node.getParent();
                     !n.valuePresent() && n.isEmpty() && p != null; n = p, p = p.getParent()) {
                  p.remove(n.getKey());
               }
            }
         };
      }

      @Override
      <T> Iterator<T> descendingEntryIterator(BiFunction<Supplier<List<C>>, N, T> producer) {
         N node = getRoot();
         if (node == null) {
            return Collections.emptyIterator();
         }
         return new DescendingEntryIterator<T, C, K, V, N>(node, producer) {
            @Override public void remove() {
               super.remove();
               // if this removal caused prefix trie to become empty, we may need to
               // propagate removal up to parent trie to prune dead sub-tries
               for (N n = node, p = node.getParent();
                     !n.valuePresent() && n.isEmpty() && p != null; n = p, p = p.getParent()) {
                  p.remove(n.getKey());
               }
            }
         };
      }
   }
   
   class KeySet extends AbstractSet<K> implements NavigableSet<K> {
      @Override
      public K lower(K e) {
         return AbstractNavigableCompositeTrie.this.lowerKey(e);
      }

      @Override
      public K floor(K e) {
         return AbstractNavigableCompositeTrie.this.floorKey(e);
      }

      @Override
      public K ceiling(K e) {
         return AbstractNavigableCompositeTrie.this.ceilingKey(e);
      }

      @Override
      public K higher(K e) {
         return AbstractNavigableCompositeTrie.this.higherKey(e);
      }

      @Override
      public K pollFirst() {
         Entry<K, V> entry = AbstractNavigableCompositeTrie.this.pollFirstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K pollLast() {
         Entry<K, V> entry = AbstractNavigableCompositeTrie.this.pollLastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Iterator<K> iterator() {
         return AbstractNavigableCompositeTrie.this.entryIterator(
               (supplier, node) -> node.getLeafKey());
      }

      @Override
      public Iterator<K> descendingIterator() {
         return AbstractNavigableCompositeTrie.this.descendingEntryIterator(
               (supplier, node) -> node.getLeafKey());
      }

      @Override
      public Comparator<? super K> comparator() {
         return AbstractNavigableCompositeTrie.this.comparator();
      }

      @Override
      public K first() {
         return AbstractNavigableCompositeTrie.this.firstKey();
      }

      @Override
      public K last() {
         return AbstractNavigableCompositeTrie.this.lastKey();
      }

      @Override
      public int size() {
         return AbstractNavigableCompositeTrie.this.size();
      }

      @Override
      public boolean contains(Object o) {
         return AbstractNavigableCompositeTrie.this.containsKey(o);
      }

      @Override
      public boolean remove(Object o) {
         Iterable<C> iter = tryAsIterable(o);
         if (iter == null) {
            return false;
         }
         return AbstractNavigableCompositeTrie.this.remove(iter).isPresent();
      }

      @Override
      public void clear() {
         AbstractNavigableCompositeTrie.this.clear();
      }

      @Override
      public NavigableSet<K> descendingSet() {
         return new DescendingSet<>(this);
      }

      @Override
      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive,
            K toElement, boolean toInclusive) {
         return AbstractNavigableCompositeTrie.this
               .subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         return AbstractNavigableCompositeTrie.this.headMap(toElement, inclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return AbstractNavigableCompositeTrie.this.tailMap(fromElement, inclusive)
               .navigableKeySet();
      }

      @Override
      public NavigableSet<K> subSet(K fromElement, K toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      @Override
      public NavigableSet<K> headSet(K toElement) {
         return headSet(toElement, false);
      }

      @Override
      public NavigableSet<K> tailSet(K fromElement) {
         return tailSet(fromElement, true);
      }
   }
   
   static class SubTrie<K, C, V> extends AbstractNavigableMap<K, V>
         implements NavigableCompositeTrie<K, C, V> {
      private final AbstractNavigableCompositeTrie<K, C, V, ?> base;
      private final K lowerBound;
      private final BoundType lowerBoundType;
      private final K upperBound;
      private final BoundType upperBoundType;
      
      SubTrie(AbstractNavigableCompositeTrie<K, C, V, ?> base, K lowerBound,
            BoundType lowerBoundType, K upperBound, BoundType upperBoundType) {
         this.base = base;
         this.lowerBound = lowerBound;
         this.lowerBoundType = lowerBoundType;
         this.upperBound = upperBound;
         this.upperBoundType = upperBoundType;
      }

      @Override
      public int size() {
         // TODO: make this more efficient?
         int sz = 0;
         for (Iterator<?> iter = keySet().iterator(); iter.hasNext(); ) {
            iter.next();
            sz++;
         }
         return sz;
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

      // we switch on the ones we care about; intentional fall-through for the rest
      @SuppressWarnings("incomplete-switch")
      private boolean isInRange(K key) {
         Comparator<K> comp = base.comparator();
         switch (lowerBoundType) {
            case INCLUSIVE:
               return comp.compare(key, lowerBound) >= 0;
            case EXCLUSIVE:
               return comp.compare(key, lowerBound) > 0;
         }
         switch (upperBoundType) {
            case INCLUSIVE:
               return comp.compare(key, upperBound) <= 0;
            case EXCLUSIVE:
               return comp.compare(key, upperBound) < 0;
         }
         return true;
      }
      
      private void checkArgIsInRange(K key) {
         if (!isInRange(key)) {
            throw new IllegalArgumentException("Key " + key + " is outside sub-map range");
         }
      }

      // we switch on the ones we care about; intentional fall-through for the rest
      @SuppressWarnings("incomplete-switch")
      private boolean isInRange(Iterator<C> prefix) {
         Comparator<? super C> comp = base.componentComparator();
         switch (lowerBoundType) {
            case INCLUSIVE:
               Iterator<C> lower = base.asIterable(lowerBound).iterator();
               while (lower.hasNext()) {
                  if (!prefix.hasNext()) {
                     return false;
                  }
                  if (comp.compare(prefix.next(), lower.next()) < 0) {
                     return false;
                  }
               }
               return true;
            case EXCLUSIVE:
               lower = base.asIterable(lowerBound).iterator();
               while (lower.hasNext()) {
                  if (!prefix.hasNext()) {
                     return false;
                  }
                  if (comp.compare(prefix.next(), lower.next()) <= 0) {
                     return false;
                  }
               }
               return true;
         }
         switch (upperBoundType) {
            case INCLUSIVE:
               Iterator<C> upper = base.asIterable(upperBound).iterator();
               while (upper.hasNext()) {
                  if (!prefix.hasNext()) {
                     return true;
                  }
                  if (comp.compare(prefix.next(), upper.next()) > 0) {
                     return false;
                  }
               }
               return true;
            case EXCLUSIVE:
               upper = base.asIterable(upperBound).iterator();
               while (upper.hasNext()) {
                  if (!prefix.hasNext()) {
                     return true;
                  }
                  if (comp.compare(prefix.next(), upper.next()) >= 0) {
                     return false;
                  }
               }
               return true;
         }
         return true;
      }
      
      private void checkPrefixIsInRange(Iterable<C> prefix) {
         if (!isInRange(prefix.iterator())) {
            throw new IllegalArgumentException("Prefix " + prefix + " is outside sub-map range");
         }
      }

      @Override
      public V put(K key, V value) {
         checkArgIsInRange(key);
         return base.put(key, value);
      }

      @Override
      public Componentizer<? super K, ? extends C> componentizer() {
         return base.componentizer();
      }
      
      @Override
      public Comparator<? super C> componentComparator() {
         return base.componentComparator();
      }
      
      @Override
      public Comparator<? super K> comparator() {
         return base.comparator();
      }

      @Override
      public NavigableCompositeTrie<K, C, V> prefixMapByKey(K prefix) {
         return prefixMap(base.asIterable(prefix));
      }

      @Override
      public NavigableCompositeTrie<K, C, V> prefixMapByKey(K prefix, int numComponents) {
         return prefixMap(base.asIterable(prefix), numComponents);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> prefixMap(C prefix) {
         return prefixMap(Collections.singleton(prefix));
      }

      @Override
      public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix) {
         checkPrefixIsInRange(prefix);
         @SuppressWarnings("unchecked") // this is not actually an unchecked cast, javac!
         AbstractNavigableCompositeTrie<K, C, V, ?> prefixMap =
               (AbstractNavigableCompositeTrie<K, C, V, ?>) base.prefixMap(prefix);
         return new SubTrie<K, C, V>(prefixMap, lowerBound, lowerBoundType, upperBound,
               upperBoundType);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents) {
         return prefixMap(Iterables.limit(prefix, numComponents));
      }
      
      @Override
      public NavigableCompositeTrie<K, C, V> descendingMap() {
         return new DescendingCompositeTrie<>(this);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> subMap(K fromKey, boolean fromInclusive,
            K toKey, boolean toInclusive) {
         Comparator<K> comp = base.comparator();
         if (comp.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("From key " + fromKey + " > to key" + toKey);
         }
         checkArgIsInRange(fromKey);
         checkArgIsInRange(toKey);
         return new SubTrie<>(base,
               fromKey, fromInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
               toKey, toInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> headMap(K toKey, boolean inclusive) {
         checkArgIsInRange(toKey);
         return new SubTrie<>(base, lowerBound, lowerBoundType,
               toKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> tailMap(K fromKey, boolean inclusive) {
         checkArgIsInRange(fromKey);
         return new SubTrie<>(base, fromKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
               upperBound, upperBoundType);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> subMap(K fromKey, K toKey) {
         return subMap(fromKey, true, toKey, false);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> headMap(K toKey) {
         return headMap(toKey, false);
      }

      @Override
      public NavigableCompositeTrie<K, C, V> tailMap(K fromKey) {
         return tailMap(fromKey, true);
      }
      
      @Override
      public Entry<K, V> firstEntry() {
         switch (lowerBoundType) {
            case NO_BOUND:
               return base.firstEntry();
            case INCLUSIVE:
               return base.ceilingEntry(lowerBound);
            case EXCLUSIVE:
               return base.higherEntry(lowerBound);
            default:
               throw new AssertionError();
         }
      }

      @Override
      public Entry<K, V> lastEntry() {
         switch (upperBoundType) {
            case NO_BOUND:
               return base.lastEntry();
            case INCLUSIVE:
               return base.floorEntry(upperBound);
            case EXCLUSIVE:
               return base.lowerEntry(upperBound);
            default:
               throw new AssertionError();
         }
      }
      
      @Override
      public Entry<K, V> lowerEntry(K keys) {
         Entry<K, V> entry = base.lowerEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public K lowerKey(K keys) {
         Entry<K, V> entry = lowerEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Entry<K, V> higherEntry(K keys) {
         Entry<K, V> entry = base.higherEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public K higherKey(K keys) {
         Entry<K, V> entry = higherEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Entry<K, V> ceilingEntry(K keys) {
         Entry<K, V> entry = base.ceilingEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public K ceilingKey(K keys) {
         Entry<K, V> entry = ceilingEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Entry<K, V> floorEntry(K keys) {
         Entry<K, V> entry = base.floorEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public K floorKey(K keys) {
         Entry<K, V> entry = floorEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      protected Entry<K, V> getEntry(Object key) {
         Entry<K, V> entry = base.getEntry(key);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      protected Entry<K, V> removeEntry(Object key) {
         Iterable<C> keyIter = base.tryAsIterable(key);
         if (keyIter == null) {
            return null;
         }
         AbstractNavigableTrie.NavigableNode<C, K, V, ?> node = base.get(keyIter);
         if (node == null || !node.valuePresent()) {
            return null;
         }
         Reference<V> v = base.remove(keyIter);
         return v.isPresent()
               ? new AbstractMap.SimpleImmutableEntry<>(
                     node.getLeafKey(), v.get())
               : null;
      }
   }
}
