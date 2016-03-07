package com.bluegosling.collections.tries;

import com.bluegosling.collections.AbstractNavigableMap;
import com.bluegosling.collections.BoundType;
import com.bluegosling.collections.MoreIterables;
import com.bluegosling.collections.views.DescendingSequenceTrie;
import com.bluegosling.collections.views.DescendingSet;
import com.bluegosling.possible.Reference;
import com.google.common.collect.Iterables;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
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
import java.util.function.BiFunction;
import java.util.function.Supplier;

abstract class AbstractNavigableSequenceTrie<K, V, N extends AbstractNavigableTrie.NavigableNode<K, Void, V, N>>
      extends AbstractNavigableTrie<K, Void, V, N> implements NavigableSequenceTrie<K, V> {

   public AbstractNavigableSequenceTrie(Comparator<? super K> componentComparator) {
      super(componentComparator);
   }
   
   @SuppressWarnings("unchecked")
   Iterable<K> asIterable(Object key) {
      return (Iterable<K>) key;
   }

   @Override
   public Comparator<Iterable<K>> comparator() {
      return MoreIterables.comparator(componentComparator);
   }

   @Override
   public Comparator<? super K> componentComparator() {
      return componentComparator == Comparator.naturalOrder()
            ? null
            : componentComparator;
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
   
   Entry<List<K>, V> getEntry(Object key) {
      if (!(key instanceof Iterable)) {
         return null;
      }
      N node = get(asIterable(key));
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }

   @Override
   public Entry<List<K>, V> lowerEntry(Iterable<K> key) {
      N node = lowerNode(key);
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }
   
   @Override
   public Entry<List<K>, V> lowerEntry(List<K> key) {
      return lowerEntry((Iterable<K>) key);
   }

   @Override
   public List<K> lowerKey(Iterable<K> key) {
      N node = lowerNode(key);
      return node != null ? createKeyList(node, root) : null;
   }

   @Override
   public List<K> lowerKey(List<K> key) {
      return lowerKey((Iterable<K>) key);
   }

   @Override
   public Entry<List<K>, V> floorEntry(Iterable<K> key) {
      N node = floorNode(key);
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }

   @Override
   public Entry<List<K>, V> floorEntry(List<K> key) {
      return floorEntry((Iterable<K>) key);
   }

   @Override
   public List<K> floorKey(Iterable<K> key) {
      N node = floorNode(key);
      return node != null ? createKeyList(node, root) : null;
   }

   @Override
   public List<K> floorKey(List<K> key) {
      return floorKey((Iterable<K>) key);
   }

   @Override
   public Entry<List<K>, V> ceilingEntry(Iterable<K> key) {
      N node = ceilingNode(key);
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }

   @Override
   public Entry<List<K>, V> ceilingEntry(List<K> key) {
      return ceilingEntry((Iterable<K>) key);
   }

   @Override
   public List<K> ceilingKey(Iterable<K> key) {
      N node = ceilingNode(key);
      return node != null ? createKeyList(node, root) : null;
   }

   @Override
   public List<K> ceilingKey(List<K> key) {
      return ceilingKey((Iterable<K>) key);
   }

   @Override
   public Entry<List<K>, V> higherEntry(Iterable<K> key) {
      N node = higherNode(key);
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }

   @Override
   public Entry<List<K>, V> higherEntry(List<K> key) {
      return higherEntry((Iterable<K>) key);
   }

   @Override
   public List<K> higherKey(Iterable<K> key) {
      N node = higherNode(key);
      return node != null ? createKeyList(node, root) : null;
   }

   @Override
   public List<K> higherKey(List<K> key) {
      return higherKey((Iterable<K>) key);
   }

   @Override
   public Entry<List<K>, V> firstEntry() {
      N node = firstNode();
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }

   @Override
   public Entry<List<K>, V> lastEntry() {
      N node = lastNode();
      return node != null ? new EntryImpl<>(createKeyList(node, root), node) : null;
   }

   @Override
   public Entry<List<K>, V> pollFirstEntry() {
      N node = firstNode();
      if (node == null) {
         return null;
      }
      remove(node);
      return new EntryImpl<>(createKeyList(node, root), node);
   }

   @Override
   public Entry<List<K>, V> pollLastEntry() {
      N node = lastNode();
      if (node == null) {
         return null;
      }
      remove(node);
      return new EntryImpl<>(createKeyList(node, root), node);
   }

   @Override
   public List<K> firstKey() {
      N node = firstNode();
      if (node == null) {
         throw new NoSuchElementException();
      }
      return createKeyList(node, root);
   }

   @Override
   public List<K> lastKey() {
      N node = lastNode();
      if (node == null) {
         throw new NoSuchElementException();
      }
      return createKeyList(node, root);
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
   public NavigableSequenceTrie<K, V> descendingMap() {
      return new DescendingSequenceTrie<>(this);
   }
   
   @Override
   public Set<List<K>> keySet() {
      return navigableKeySet();
   }
   
   @Override
   public NavigableSet<List<K>> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }
   
   @Override
   public NavigableSet<List<K>> navigableKeySet() {
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
            return AbstractNavigableSequenceTrie.this.size();
         }
         
         @Override
         public boolean contains(Object o) {
            return containsValue(o);
         }

         @Override
         public void clear() {
            AbstractNavigableSequenceTrie.this.clear();
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
            return AbstractNavigableSequenceTrie.this.size();
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
            AbstractNavigableSequenceTrie.this.remove(node);
            return true;
         }
         
         @Override
         public void clear() {
            AbstractNavigableSequenceTrie.this.clear();
         }
      };
   }

   @Override
   public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, boolean fromInclusive,
         Iterable<K> toKey, boolean toInclusive) {
      return new SubTrie<>(this, fromKey, fromInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
            toKey, toInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey, boolean inclusive) {
      return new SubTrie<>(this, null, BoundType.NO_BOUND, toKey,
            inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey, boolean inclusive) {
      return new SubTrie<>(this, fromKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
            null, BoundType.NO_BOUND);
   }

   @Override
   public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, Iterable<K> toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   @Override
   public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey) {
      return headMap(toKey, false);
   }

   @Override
   public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey) {
      return tailMap(fromKey, true);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> subMap(List<K> fromKey, boolean fromInclusive, List<K> toKey,
         boolean toInclusive) {
      return subMap((Iterable<K>) fromKey, fromInclusive, toKey, toInclusive);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> headMap(List<K> toKey, boolean inclusive) {
      return headMap((Iterable<K>) toKey, inclusive);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> tailMap(List<K> fromKey, boolean inclusive) {
      return tailMap((Iterable<K>) fromKey, inclusive);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> subMap(List<K> fromKey, List<K> toKey) {
      return subMap((Iterable<K>) fromKey, toKey);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> headMap(List<K> toKey) {
      return headMap((Iterable<K>) toKey);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> tailMap(List<K> fromKey) {
      return tailMap((Iterable<K>) fromKey);
   }
   
   @Override
   public NavigableSequenceTrie<K, V> prefixMap(K prefix) {
      return prefixMap(Collections.singleton(prefix));
   }

   @Override
   public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix) {
      Collection<K> snapshot = MoreIterables.snapshot(prefix);
      return snapshot.isEmpty() ? this : new NavigablePrefixSequenceTrie<>(snapshot, this);
   }

   @Override
   public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents) {
      return prefixMap(Iterables.limit(prefix, numComponents));
   }
   
   static class NavigablePrefixSequenceTrie<K, V, N extends NavigableNode<K, Void, V, N>>
         extends AbstractNavigableSequenceTrie<K, V, N> {
      final Iterable<K> prefix;
      final AbstractNavigableSequenceTrie<K, V, N> parent;

      NavigablePrefixSequenceTrie(Iterable<K> prefix,
            AbstractNavigableSequenceTrie<K, V, N> parent) {
         super(parent.componentComparator);
         this.prefix = prefix;
         this.parent = parent;
         this.generation = parent.generation;
         this.root = parent.get(prefix);
      }
   
      @Override
      public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> newPrefix) {
         Collection<K> snapshot = MoreIterables.snapshot(newPrefix);
         return snapshot.isEmpty() ? this : new NavigablePrefixSequenceTrie<>(
               Iterables.concat(this.prefix, snapshot), parent);
      }
      
      N getRoot() {
         N node = this.root;
         if (node == null || (node.isEmpty() && !node.valuePresent())
               || this.generation != parent.generation) {
            // node needs to be recomputed
            node = parent.get(prefix);
            this.root = node;
         }
         return node;
      }

      @Override
      protected N get(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.get(keys) : null;
      }

      @Override
      protected N firstNode() {
         N node = getRoot();
         return node != null ? super.firstNode() : null;
      }
      
      @Override
      protected N lastNode() {
         N node = getRoot();
         return node != null ? super.lastNode() : null;
      }

      @Override
      protected N floorNode(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.floorNode(keys) : null;
      }

      @Override
      protected N ceilingNode(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.ceilingNode(keys) : null;
      }
      
      @Override
      protected N lowerNode(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.lowerNode(keys) : null;
      }

      @Override
      protected N higherNode(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.higherNode(keys) : null;
      }
      
      // don't need to override put() since it uses ensurePath, which is overridden below
      
      @Override
      protected N ensurePath(Iterable<K> path) {
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
      protected Reference<V> remove(Iterable<K> keys) {
         N node = getRoot();
         return node != null ? super.remove(keys) : Reference.unset();
      }

      @Override
      protected N newNode(K key, N p) {
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
               for (N n = node, p = node.getParent();
                     !n.valuePresent() && n.isEmpty() && p != null; n = p, p = p.getParent()) {
                  p.remove(n.getKey());
               }
            }
         };
      }

      @Override
      protected <T> Iterator<T> descendingEntryIterator(
            BiFunction<Supplier<List<K>>, N, T> producer) {
         N node = getRoot();
         if (node == null) {
            return Collections.emptyIterator();
         }
         return new DescendingEntryIterator<T, K, Void, V, N>(node, producer) {
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
   
   protected class KeySet extends AbstractSet<List<K>> implements NavigableSet<List<K>> {
      
      @Override
      public List<K> lower(List<K> e) {
         return AbstractNavigableSequenceTrie.this.lowerKey(e);
      }

      @Override
      public List<K> floor(List<K> e) {
         return AbstractNavigableSequenceTrie.this.floorKey(e);
      }

      @Override
      public List<K> ceiling(List<K> e) {
         return AbstractNavigableSequenceTrie.this.ceilingKey(e);
      }

      @Override
      public List<K> higher(List<K> e) {
         return AbstractNavigableSequenceTrie.this.higherKey(e);
      }

      @Override
      public List<K> pollFirst() {
         Entry<List<K>, V> entry = AbstractNavigableSequenceTrie.this.pollFirstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public List<K> pollLast() {
         Entry<List<K>, V> entry = AbstractNavigableSequenceTrie.this.pollLastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Iterator<List<K>> iterator() {
         return AbstractNavigableSequenceTrie.this.entryIterator(
               (supplier, node) -> supplier.get());
      }

      @Override
      public Iterator<List<K>> descendingIterator() {
         return AbstractNavigableSequenceTrie.this.descendingEntryIterator(
               (supplier, node) -> supplier.get());
      }

      @Override
      public Comparator<? super List<K>> comparator() {
         return AbstractNavigableSequenceTrie.this.comparator();
      }

      @Override
      public List<K> first() {
         return AbstractNavigableSequenceTrie.this.firstKey();
      }

      @Override
      public List<K> last() {
         return AbstractNavigableSequenceTrie.this.lastKey();
      }

      @Override
      public int size() {
         return AbstractNavigableSequenceTrie.this.size();
      }

      @Override
      public boolean contains(Object o) {
         return AbstractNavigableSequenceTrie.this.containsKey(o);
      }

      @Override
      public boolean remove(Object o) {
         if (!(o instanceof Iterable)) {
            return false;
         }
         return AbstractNavigableSequenceTrie.this.remove(asIterable(o)).isPresent();
      }

      @Override
      public void clear() {
         AbstractNavigableSequenceTrie.this.clear();
      }

      @Override
      public NavigableSet<List<K>> descendingSet() {
         return new DescendingSet<>(this);
      }

      @Override
      public NavigableSet<List<K>> subSet(List<K> fromElement, boolean fromInclusive,
            List<K> toElement, boolean toInclusive) {
         return AbstractNavigableSequenceTrie.this
               .subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<List<K>> headSet(List<K> toElement, boolean inclusive) {
         return AbstractNavigableSequenceTrie.this.headMap(toElement, inclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<List<K>> tailSet(List<K> fromElement, boolean inclusive) {
         return AbstractNavigableSequenceTrie.this.tailMap(fromElement, inclusive)
               .navigableKeySet();
      }

      @Override
      public NavigableSet<List<K>> subSet(List<K> fromElement, List<K> toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      @Override
      public NavigableSet<List<K>> headSet(List<K> toElement) {
         return headSet(toElement, false);
      }

      @Override
      public NavigableSet<List<K>> tailSet(List<K> fromElement) {
         return tailSet(fromElement, true);
      }
   }
   
   protected static class SubTrie<K, V> extends AbstractNavigableMap<List<K>, V>
         implements NavigableSequenceTrie<K, V> {
      private final AbstractNavigableSequenceTrie<K, V, ?> base;
      private final Iterable<K> lowerBound;
      private final BoundType lowerBoundType;
      private final Iterable<K> upperBound;
      private final BoundType upperBoundType;
      
      SubTrie(AbstractNavigableSequenceTrie<K, V, ?> base, Iterable<K> lowerBound,
            BoundType lowerBoundType, Iterable<K> upperBound, BoundType upperBoundType) {
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

      // we switch on the ones we care about; intentional fall-through for the rest
      @SuppressWarnings("incomplete-switch")
      private boolean isInRange(Iterable<K> key) {
         Comparator<Iterable<K>> comp = base.comparator();
         switch (lowerBoundType) {
            case INCLUSIVE:
               if (comp.compare(key, lowerBound) < 0) {
                  return false;
               }
            case EXCLUSIVE:
               if (comp.compare(key, lowerBound) <= 0) {
                  return false;
               }
         }
         switch (upperBoundType) {
            case INCLUSIVE:
               if (comp.compare(key, upperBound) > 0) {
                  return false;
               }
            case EXCLUSIVE:
               if (comp.compare(key, upperBound) >= 0) {
                  return false;
               }
         }
         return true;
      }
      
      private void checkArgIsInRange(Iterable<K> key) {
         if (!isInRange(key)) {
            throw new IllegalArgumentException("Key " + key + " is outside sub-map range");
         }
      }
      
      @Override
      public V put(Iterable<K> key, V value) {
         checkArgIsInRange(key);
         return base.put(key, value);
      }

      @Override
      public V put(List<K> key, V value) {
         return put((Iterable<K>) key, value);
      }

      @Override
      public Comparator<? super K> componentComparator() {
         return base.componentComparator();
      }
      
      @Override
      public Comparator<? super List<K>> comparator() {
         return base.comparator();
      }

      @Override
      public NavigableSequenceTrie<K, V> prefixMap(K prefix) {
         return prefixMap(Collections.singleton(prefix));
      }

      @Override
      public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix) {
         checkArgIsInRange(prefix);
         @SuppressWarnings("unchecked") // this is not actually an unchecked cast, javac!
         AbstractNavigableSequenceTrie<K, V, ?> prefixMap =
               (AbstractNavigableSequenceTrie<K, V, ?>) base.prefixMap(prefix);
         return new SubTrie<K, V>(prefixMap, lowerBound, lowerBoundType, upperBound,
               upperBoundType);
      }

      @Override
      public NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents) {
         return prefixMap(Iterables.limit(prefix, numComponents));
      }

      @Override
      public NavigableSequenceTrie<K, V> descendingMap() {
         return new DescendingSequenceTrie<>(this);
      }

      @Override
      public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, boolean fromInclusive,
            Iterable<K> toKey, boolean toInclusive) {
         Comparator<Iterable<K>> comp = base.comparator();
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
      public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey, boolean inclusive) {
         checkArgIsInRange(toKey);
         return new SubTrie<>(base, lowerBound, lowerBoundType,
               toKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE);
      }

      @Override
      public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey, boolean inclusive) {
         checkArgIsInRange(fromKey);
         return new SubTrie<>(base, fromKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
               upperBound, upperBoundType);
      }

      @Override
      public NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, Iterable<K> toKey) {
         return subMap(fromKey, true, toKey, false);
      }

      @Override
      public NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey) {
         return headMap(toKey, false);
      }

      @Override
      public NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey) {
         return tailMap(fromKey, true);
      }

      @Override
      public NavigableSequenceTrie<K, V> subMap(List<K> fromKey, boolean fromInclusive,
            List<K> toKey, boolean toInclusive) {
         return subMap((Iterable<K>) fromKey, fromInclusive, toKey, toInclusive);
      }

      @Override
      public NavigableSequenceTrie<K, V> headMap(List<K> toKey, boolean inclusive) {
         return headMap((Iterable<K>) toKey, inclusive);
      }

      @Override
      public NavigableSequenceTrie<K, V> tailMap(List<K> fromKey, boolean inclusive) {
         return tailMap((Iterable<K>) fromKey, inclusive);
      }

      @Override
      public NavigableSequenceTrie<K, V> subMap(List<K> fromKey, List<K> toKey) {
         return subMap(fromKey, true, toKey, false);
      }

      @Override
      public NavigableSequenceTrie<K, V> headMap(List<K> toKey) {
         return headMap(toKey, false);
      }

      @Override
      public NavigableSequenceTrie<K, V> tailMap(List<K> fromKey) {
         return tailMap(fromKey, true);
      }

      @Override
      public Entry<List<K>, V> lowerEntry(List<K> keys) {
         return lowerEntry((Iterable<K>) keys);
      }

      @Override
      public Entry<List<K>, V> higherEntry(List<K> keys) {
         return higherEntry((Iterable<K>) keys);
      }

      @Override
      public Entry<List<K>, V> ceilingEntry(List<K> keys) {
         return ceilingEntry((Iterable<K>) keys);
      }

      @Override
      public Entry<List<K>, V> floorEntry(List<K> keys) {
         return floorEntry((Iterable<K>) keys);
      }
      
      @Override
      public Entry<List<K>, V> firstEntry() {
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
      public Entry<List<K>, V> lastEntry() {
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
      public Entry<List<K>, V> lowerEntry(Iterable<K> keys) {
         Entry<List<K>, V> entry = base.lowerEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public List<K> lowerKey(Iterable<K> keys) {
         Entry<List<K>, V> entry = lowerEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Entry<List<K>, V> higherEntry(Iterable<K> keys) {
         Entry<List<K>, V> entry = base.higherEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public List<K> higherKey(Iterable<K> keys) {
         Entry<List<K>, V> entry = higherEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Entry<List<K>, V> ceilingEntry(Iterable<K> keys) {
         Entry<List<K>, V> entry = base.ceilingEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public List<K> ceilingKey(Iterable<K> keys) {
         Entry<List<K>, V> entry = ceilingEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      public Entry<List<K>, V> floorEntry(Iterable<K> keys) {
         Entry<List<K>, V> entry = base.floorEntry(keys);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      public List<K> floorKey(Iterable<K> keys) {
         Entry<List<K>, V> entry = floorEntry(keys);
         return entry != null ? entry.getKey() : null;
      }

      @Override
      protected Entry<List<K>, V> getEntry(Object key) {
         Entry<List<K>, V> entry = base.getEntry(key);
         return entry != null && isInRange(entry.getKey()) ? entry : null;
      }

      @Override
      protected Entry<List<K>, V> removeEntry(Object key) {
         if (!(key instanceof Iterable)) {
            return null;
         }
         Iterable<K> keyIter = base.asIterable(key);
         Reference<V> v = base.remove(keyIter);
         return v.isPresent()
               ? new AbstractMap.SimpleImmutableEntry<>(
                     new ArrayList<>(MoreIterables.snapshot(keyIter)), v.get())
               : null;
      }
   }
}
