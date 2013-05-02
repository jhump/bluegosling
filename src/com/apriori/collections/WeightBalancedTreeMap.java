package com.apriori.collections;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

//TODO: implement me! (don't forget serialization and cloning)
//TODO: javadoc
//TODO: tests
//TODO: subMapByIndices/subSetByIndices should use checkWide(from) instead of check(from)
public class WeightBalancedTreeMap<K, V>
      implements RandomAccessNavigableMap<K, V>, Serializable, Cloneable {
   
   private static final long serialVersionUID = -3252472174080097845L;

   private static class Node<K, V> {
      K key;
      V value;
      int subTreeSize;
      Node<K, V> left, right;
      
      Node(Map.Entry<? extends K, ? extends V> other) {
         this(other.getKey(), other.getValue());
      }

      Node(K key, V value) {
         this.key = key;
         this.value = value;
         subTreeSize = 1;
      }
   }
   
   transient int size;
   transient Node<K, V> root;
   transient int modCount;
   transient final Comparator<? super K> comparator;
   
   public WeightBalancedTreeMap() {
      this((Comparator<? super K>) null);
   }

   public WeightBalancedTreeMap(Comparator<? super K> comparator) {
      if (comparator == null) {
         this.comparator = CollectionUtils.NATURAL_ORDERING;
      } else {
         this.comparator = comparator;
      }
   }
   
   public WeightBalancedTreeMap(Map<? extends K, ? extends V> map) {
      this(map, null);
   }

   public WeightBalancedTreeMap(Map<? extends K, ? extends V> map,
         Comparator<? super K> comparator) {
      this(comparator);
      putAll(map);
   }
   
   void checkRange(int index) {
      if  (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index >= size) {
         throw new IndexOutOfBoundsException(index + " >= " + size);
      }
   }

   void checkWideRange(int index) {
      if  (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index > size) {
         throw new IndexOutOfBoundsException(index + " > " + size);
      }
   }

   Map.Entry<K, V> mapEntry(Node<K, V> node) {
      return node == null ? null : new EntryImpl(node);
   }

   @Override
   public List<V> values() {
      return new TransformingList.RandomAccess<Map.Entry<K, V>, V>(
            new RandomAccessSetList<Map.Entry<K, V>>(entrySet()),
            new Function<Map.Entry<K, V>, V>() {
               @Override public V apply(Map.Entry<K, V> input) {
                  return input.getValue();
               }
            });
   }

   @Override
   public RandomAccessSet<Map.Entry<K, V>> entrySet() {
      return new EntrySet<K, V>(this);
   }

   @Override
   public Comparator<? super K> comparator() {
      return comparator == CollectionUtils.NATURAL_ORDERING ? null : comparator;
   }

   @Override
   public K firstKey() {
      Map.Entry<K, V> entry = firstEntry();
      if (entry == null) {
         throw new NoSuchElementException("map is empty");
      }
      return entry.getKey();
   }

   @Override
   public K lastKey() {
      Map.Entry<K, V> entry = lastEntry();
      if (entry == null) {
         throw new NoSuchElementException("map is empty");
      }
      return entry.getKey();
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean isEmpty() {
      return size == 0;
   }

   @Override
   public boolean containsKey(Object key) {
      return getNode(key) != null;
   }

   private boolean searchForValue(Node<K, V> entry, Object value) {
      if (entry.value == null ? value == null : entry.value.equals(value)) {
         return true;
      }
      if (entry.left != null && searchForValue(entry.left, value)) {
         return true;
      }
      if (entry.right != null && searchForValue(entry.right, value)) {
         return true;
      }
      return false;
   }
   
   @Override
   public boolean containsValue(Object value) {
      if (root == null) {
         return false;
      }
      return searchForValue(root, value);
   }
   
   Node<K, V> getNode(Object key) {
      Node<K, V> entry = root;
      while (entry != null) {
         @SuppressWarnings("unchecked") // safe if user provided valid/correct comparator
         int c = comparator.compare((K) key, entry.key);
         if (c == 0) {
            return entry;
         } else if (c < 0) {
            entry = entry.left;
         } else {
            entry = entry.right;
         }
      }
      return null;
   }

   @Override
   public V get(Object key) {
      Node<K, V> entry = getNode(key);
      return entry == null ? null : entry.value;
   }
   
   private Node<K, V> subtreePredecessor(Node<K, V> entry) {
      Node<K, V> predecessor = entry.left;
      while (predecessor.right != null) {
         predecessor = predecessor.right;
      }
      return predecessor;
   }

   private Node<K, V> subtreeSuccessor(Node<K, V> entry) {
      Node<K, V> successor = entry.right;
      while (successor.left != null) {
         successor = successor.left;
      }
      return successor;
   }
   
   private void swap(Node<K, V> e1, Node<K, V> e2) {
      K tmpKey = e1.key;
      e1.key = e2.key;
      e2.key = tmpKey;
      V tmpVal = e1.value;
      e1.value = e2.value;
      e2.value = tmpVal;
   }

   private void rotateRight(Node<K, V> entry) {
      // to move an item from left to right, we remove the leaf predecessor from the left sub-tree,
      // swap places with it, and then add current value to right sub-tree
      Node<K, V> other = subtreePredecessor(entry);
      removeEntry(entry, other.key);
      swap(entry, other);
      // reset node attributes and then re-insert
      other.left = other.right = null;
      other.subTreeSize = 1;
      addEntry(entry, other);
   }

   private void rotateLeft(Node<K, V> entry) {
      // to move from right to left, mirror image of logic in rotateRight
      Node<K, V> other = subtreeSuccessor(entry);
      removeEntry(entry, other.key);
      swap(entry, other);
      other.left = other.right = null;
      other.subTreeSize = 1;
      addEntry(entry, other);
   }
   
   private void rebalance(Node<K, V> entry) {
      int left = entry.left == null ? 0 : entry.left.subTreeSize;
      int right = entry.right == null ? 0 : entry.right.subTreeSize;
      if (left > right + 1) {
         rotateRight(entry);
      } else if (right > left + 1) {
         rotateLeft(entry);
      }
   }

   private Node<K, V> addEntry(Node<K, V> current, Node<K, V> newEntry) {
      int c = comparator.compare(newEntry.key, current.key);
      Node<K, V> priorEntry;
      if (c == 0) {
         // replace value
         current.value = newEntry.value;
         return current;
      } else if (c < 0) {
         if (current.left == null) {
            current.left = newEntry;
            priorEntry = null;
         } else {
            priorEntry = addEntry(current.left, newEntry);
         }
      } else {
         if (current.right == null) {
            current.right = newEntry;
            priorEntry = null;
         } else {
            priorEntry = addEntry(current.right, newEntry);
         }
      }
      if (priorEntry == null) {
         // if no prior entry was found, that means we had to actually add a node,
         // so we need to rebalance sub-tree
         current.subTreeSize++;
         rebalance(current);
      }
      return priorEntry;
   }

   @Override
   public V put(K key, V value) {
      Node<K, V> newEntry = new Node<K, V>(key, value);
      if (root == null) {
         root = newEntry;
         size++;
         modCount++;
         return null;
      } else {
         Node<K, V> priorEntry = addEntry(root, newEntry);
         if (priorEntry == null) {
            size++;
            modCount++;
            return null;
         } else {
            return priorEntry.value;
         }
      }
   }

   Node<K, V> removeEntry(Node<K, V> entry, Object key) {
      if (entry == null) {
         return null;
      }
      @SuppressWarnings("unchecked") // safe if user provided valid/correct comparator
      int c = comparator.compare((K) key, entry.key);
      Node<K, V> removed;
      if (c == 0) {
         if (entry.left != null && entry.right != null) {
            // if two children, swap with predecessor and then recurse so we can remove that
            // predecessor node and then rebalance
            Node<K, V> predecessor = subtreePredecessor(entry);
            swap(entry, predecessor);
            removed = removeEntry(entry.left, key);
         } else {
            return entry;
         }
      } else if (c < 0) {
         removed = removeEntry(entry.left, key);
      } else {
         removed = removeEntry(entry.right, key);
      }
      // did we find anything?
      if (removed != null) {
         // fix up refs if necessary
         if (removed == entry.left) {
            entry.left = removed.left == null ? removed.right : removed.left;
         } else if (removed == entry.right) {
            entry.right = removed.left == null ? removed.right : removed.left;
         }
         // now we rebalance sub-tree
         entry.subTreeSize--;
         rebalance(entry);
      }
      return removed;
   }
   
   @Override
   public V remove(Object key) {
      Node<K, V> removed = removeEntry(root, key);
      if (removed == null) {
         return null;
      }
      if (removed == root) {
         root = root.left == null ? root.right : root.left;
      }
      size--;
      modCount++;
      return removed.value;
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      if (size == 0 && m instanceof SortedMap
            && comparator() == null ? ((SortedMap<?,?>)m).comparator() == null
                  : comparator().equals(((SortedMap<?,?>)m).comparator())) {
         // we can efficiently construct a balanced tree without rotations
         @SuppressWarnings("unchecked")
         Map.Entry<? extends K, ? extends V> entries[] = new Map.Entry[m.size()];
         entries = m.entrySet().toArray(entries);
         size = entries.length;
         root = balancedTreeFromArray(entries, 0, size);
      } else{
         for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
         }
      }
   }
   
   private Node<K, V> balancedTreeFromArray(Map.Entry<? extends K, ? extends V> entries[],
         int start, int len) {
      int mid = start + (len >> 1);
      Node<K, V> entry = new Node<K, V>(entries[mid]);
      entry.subTreeSize = len;
      int left = mid - start;
      if (left > 0) {
         entry.left = balancedTreeFromArray(entries, start, left);
      }
      int right = len - left - 1;
      if (right > 0) {
         entry.right = balancedTreeFromArray(entries, mid + 1, right);
      }
      return entry;
   }

   @Override
   public void clear() {
      size = 0;
      root = null;
      modCount++;
   }

   @Override
   public Map.Entry<K, V> lowerEntry(K key) {
      Node<K, V> max = null;
      Node<K, V> current = root;
      while (current != null) {
         int c = comparator.compare(key, current.key);
         if (c <= 0) {
            current = current.left;
         } else {
            if (max == null || comparator.compare(current.key, max.key) > 0) {
               max = current;
            }
            current = current.right;
         }
      }
      return mapEntry(max);
   }

   @Override
   public K lowerKey(K key) {
      Map.Entry<K, V> entry = lowerEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> floorEntry(K key) {
      Node<K, V> max = null;
      Node<K, V> current = root;
      while (current != null) {
         int c = comparator.compare(key, current.key);
         if (c == 0) {
            return mapEntry(current);
         } else if (c < 0) {
            current = current.left;
         } else {
            if (max == null || comparator.compare(current.key, max.key) > 0) {
               max = current;
            }
            current = current.right;
         }
      }
      return mapEntry(max);
   }

   @Override
   public K floorKey(K key) {
      Map.Entry<K, V> entry = floorEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> ceilingEntry(K key) {
      Node<K, V> min = null;
      Node<K, V> current = root;
      while (current != null) {
         int c = comparator.compare(key, current.key);
         if (c == 0) {
            return mapEntry(current);
         } else if (c < 0) {
            if (min == null || comparator.compare(current.key, min.key) < 0) {
               min = current;
            }
            current = current.left;
         } else {
            current = current.right;
         }
      }
      return mapEntry(min);
   }

   @Override
   public K ceilingKey(K key) {
      Map.Entry<K, V> entry = ceilingEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> higherEntry(K key) {
      Node<K, V> min = null;
      Node<K, V> current = root;
      while (current != null) {
         int c = comparator.compare(key, current.key);
         if (c < 0) {
            if (min == null || comparator.compare(current.key, min.key) < 0) {
               min = current;
            }
            current = current.left;
         } else {
            current = current.right;
         }
      }
      return mapEntry(min);
   }

   @Override
   public K higherKey(K key) {
      Map.Entry<K, V> entry = higherEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> firstEntry() {
      if (root == null) {
         return null;
      }
      Node<K, V> entry = root;
      while (entry.left != null) {
         entry = entry.left;
      }
      return mapEntry(entry);
   }

   @Override
   public Map.Entry<K, V> lastEntry() {
      if (root == null) {
         return null;
      }
      Node<K, V> entry = root;
      while (entry.right != null) {
         entry = entry.right;
      }
      return mapEntry(entry);
   }

   @Override
   public Map.Entry<K, V> pollFirstEntry() {
      Map.Entry<K, V> first = firstEntry();
      if (first == null) {
         return null;
      }
      removeEntry(root, first.getKey());
      return first;
   }

   @Override
   public Map.Entry<K, V> pollLastEntry() {
      Map.Entry<K, V> last = lastEntry();
      if (last == null) {
         return null;
      }
      removeEntry(root, last.getKey());
      return last;
   }

   @Override
   public RandomAccessNavigableMap<K, V> descendingMap() {
      return new DescendingRandomAccessMap<K, V>(this);
   }

   @Override
   public RandomAccessNavigableSet<K> navigableKeySet() {
      return new KeySet<K, V>(this);
   }

   @Override
   public RandomAccessNavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }
   
   @Override
   public RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex) {
      return new SubMapByIndices(startIndex, endIndex);
   }

   @Override
   public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
         boolean toInclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   @Override
   public RandomAccessNavigableMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   @Override
   public RandomAccessNavigableMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
   }

   @Override
   public RandomAccessNavigableSet<K> keySet() {
      return navigableKeySet();
   }
   
   @Override
   public ListIterator<Map.Entry<K, V>> listIterator() {
      return listIterator(0);
   }
   
   @Override
   public ListIterator<Map.Entry<K, V>> listIterator(int index) {
      checkWideRange(index);
      return new TransformingListIterator<Node<K, V>, Map.Entry<K, V>>(new ListIteratorImpl(index),
            new Function<Node<K, V>, Map.Entry<K, V>>() {
               @Override public Map.Entry<K, V> apply(Node<K, V> input) {
                  return mapEntry(input);
               }
            });
   }
   
   Node<K, V> getByIndex(int index) {
      Node<K, V> entry = root;
      while (entry != null) {
         int leftSize = entry.left == null ? 0 : entry.left.subTreeSize;
         if (index == leftSize) {
            return entry;
         } else if (index < leftSize) {
            entry = entry.left;
         } else {
            entry = entry.right;
            index -= leftSize + 1;
         }
      }
      return null;
   }
   
   Node<K, V> removeByIndex(Node<K, V> entry, int index) {
      // Same as #removeEntry(Node,Object), except we're searching by index
      if (entry == null) {
         return null;
      }
      Node<K, V> removed;
      int leftSize = entry.left == null ? 0 : entry.left.subTreeSize;
      if (index == leftSize) {
         if (entry.left != null && entry.right != null) {
            // if two children, swap with predecessor and then remove that predecessor node
            Node<K, V> predecessor = subtreePredecessor(entry);
            swap(entry, predecessor);
            removed = removeEntry(entry.left, predecessor.key);
         } else {
            return entry;
         }
      } else if (index < leftSize) {
         removed = removeByIndex(entry.left, index);
      } else {
         removed = removeByIndex(entry.right, index - leftSize - 1);
      }
      // did we find anything?
      if (removed != null) {
         // fix up refs if necessary
         if (removed == entry.left) {
            entry.left = removed.left == null ? removed.right : removed.left;
         } else if (removed == entry.right) {
            entry.right = removed.left == null ? removed.right : removed.left;
         }
         // now we rebalance sub-tree
         entry.subTreeSize--;
         rebalance(entry);
      }
      return removed;
   }
   
   int getIndex(Node<K, V> entry, Object key) {
      @SuppressWarnings("unchecked") // safe if user provided valid/correct comparator
      int c = comparator.compare((K) key, entry.key);
      if (c == 0) {
         return entry.left == null ? 0 : entry.left.subTreeSize;
      } else if (c < 0) {
         if (entry.left == null) {
            return -1;
         }
         return getIndex(entry.left, key);
      } else {
         if (entry.right == null) {
            return -1;
         }
         int index = getIndex(entry.right, key);
         if (index == -1) {
            return -1;
         } else {
            return (entry.left == null ? 0 : entry.left.subTreeSize) + 1 + index;
         }
      }
   }
   
   @Override
   public Map.Entry<K, V> getEntry(int index) {
      checkRange(index);
      Node<K, V> node = getByIndex(index);
      return node == null ? null : mapEntry(node);
   }
   
   @Override
   public Map.Entry<K, V> removeEntry(int index) {
      checkRange(index);
      Node<K, V> node = removeByIndex(root, index);
      return node == null ? null : mapEntry(node);
   }
   
   @Override
   public int indexOfKey(Object key) {
      return getIndex(root, key);
   }

   @Override
   public boolean equals(Object o) {
      return MapUtils.equals(this,  o);
   }
   
   @Override
   public int hashCode() {
      return MapUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return MapUtils.toString(this);
   }

   /**
    * Wraps a {@link Node} in the {@link Map.Entry} interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class EntryImpl implements Map.Entry<K, V> {
      private final K key;
      private V value;
      private Node<K, V> node;
      private int myModCount;
      
      EntryImpl(Node<K, V> node) {
         this.node = node;
         this.key = node.key;
         this.value = node.value;
         this.myModCount = modCount;
      }
      
      private boolean checkNode() {
         if (myModCount == modCount) {
            return true;
         }
         node = getNode(key);
         if (node != null) {
            myModCount = modCount;
            return true;
         }
         return false;
      }
      
      @Override
      public K getKey() {
         return key;
      }
      
      @Override
      public V getValue() {
         return value;
      }
      
      @Override
      public V setValue(V value) {
         if (checkNode()) {
            throw new ConcurrentModificationException("entry no longer exists in map");
         }
         V ret = this.value;
         this.value = node.value = value;
         return ret;
      }
      
      @Override
      public boolean equals(Object o) {
         return MapUtils.equals(this,  o);
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

   /**
    * An iterator that iterates through the tree's nodes in sorted order (pre-order traversal).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class IteratorImpl implements Iterator<Node<K, V>> {
      final ArrayDeque<Node<K, V>> pathToNext = new ArrayDeque<Node<K, V>>();
      final int myModCount = modCount;
      Node<K, V> lastFetched;
      IteratorModifiedState state = IteratorModifiedState.NONE;
      
      IteratorImpl() {
         init();
      }
      
      void init() {
         Node<K, V> node = root;
         while (node != null) {
            pathToNext.push(node);
            node = node.left;
         }
      }

      void findSuccessor(Deque<Node<K,V>> stack) {
         Node<K, V> current = stack.peek();
         if (current.right != null) {
            // successor is a descendant
            current = current.right;
            stack.push(current);
            while (current.left != null) {
               current = current.left;
               stack.push(current);
            }
         } else {
            // successor is an ancestor
            while (!stack.isEmpty()) {
               stack.pop();
               Node<K, V> ancestor = stack.peek();
               if (ancestor != null && ancestor.left == current) {
                  return; // found it;
               }
               current = ancestor;
            }
         }
      }

      void checkModCount() {
         if (myModCount != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         return !pathToNext.isEmpty();
      }
      
      void advance() {
         findSuccessor(pathToNext);
      }
      
      @Override
      public Node<K, V> next() {
         checkModCount();
         Node<K, V> ret = pathToNext.peek();
         if (ret == null) {
            throw new NoSuchElementException();
         }
         lastFetched = ret;
         state = IteratorModifiedState.NONE;
         advance();
         return ret;
      }
      
      void prepareForRemove() {
         // Nothing to do, but sub-classes can do something before node being removed from tree
      }
      
      @Override
      public void remove() {
         checkModCount();
         if (state != IteratorModifiedState.NONE || lastFetched == null) {
            throw new IllegalStateException();
         }
         state = IteratorModifiedState.REMOVED;
         prepareForRemove();
         removeEntry(root, lastFetched.key);
      }
   }
   
   /**
    * A list iterator that can iterate backwards and forwards through the tree's nodes.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class ListIteratorImpl extends IteratorImpl implements ListIterator<Node<K, V>> {
      final ArrayDeque<Node<K, V>> pathToPrevious = new ArrayDeque<Node<K, V>>();
      boolean lastFetchedPrevious;
      int nextIndex;

      ListIteratorImpl(int nextIndex) {
         this.nextIndex = nextIndex;
         if (nextIndex < size) {
            findByIndex(pathToNext, nextIndex);
            if (nextIndex > 0) {
               pathToPrevious.addAll(pathToNext);
               findPredecessor(pathToPrevious);
            }
         } else {
            findByIndex(pathToPrevious, nextIndex - 1);
         }
      }
      
      @Override void init() {
         // skip super-class init -- we'll init the stacks ourselves based on index passed to
         // the constructor
      }
      
      void findByIndex(Deque<Node<K,V>> stack, int index) {
         Node<K, V> node = root;
         while (node != null) {
            stack.push(node);
            int leftSize = node.left == null ? 0 : node.left.subTreeSize;
            if (index < leftSize) {
               node = node.left;
            } else if (index == leftSize) {
               return; // we have arrived
            } else {
               index -= leftSize + 1;
               node = node.right;
            }
         }
      }

      private void findPredecessor(Deque<Node<K,V>> stack) {
         Node<K, V> current = stack.peek();
         if (current.left != null) {
            // predecessor is a descendant
            current = current.left;
            stack.push(current);
            while (current.right != null) {
               current = current.right;
               stack.push(current);
            }
         } else {
            // predecessor is an ancestor
            while (!stack.isEmpty()) {
               stack.pop();
               Node<K, V> ancestor = stack.peek();
               if (ancestor != null && ancestor.right == current) {
                  return; // found it;
               }
               current = ancestor;
            }
         }
      }

      @Override void advance() {
         nextIndex++;
         pathToPrevious.clear();
         pathToPrevious.addAll(pathToNext);
         super.advance();
         lastFetchedPrevious = false;
      }
      
      void retreat() {
         nextIndex--;
         pathToNext.clear();
         pathToNext.addAll(pathToPrevious);
         findPredecessor(pathToPrevious);
         lastFetchedPrevious = true;
      }
      
      @Override
      public boolean hasPrevious() {
         checkModCount();
         return !pathToPrevious.isEmpty();
      }

      @Override
      public Node<K, V> previous() {
         checkModCount();
         Node<K, V> ret = pathToPrevious.peek();
         if (ret == null) {
            throw new NoSuchElementException();
         }
         lastFetched = ret;
         state = IteratorModifiedState.NONE;
         retreat();
         return ret;
      }

      @Override
      public int nextIndex() {
         checkModCount();
         return nextIndex;
      }

      @Override
      public int previousIndex() {
         checkModCount();
         return nextIndex - 1;
      }

      @Override
      void prepareForRemove() {
         if (lastFetchedPrevious) {
            findSuccessor(pathToNext);
         } else {
            findPredecessor(pathToPrevious);
         }
      }
      
      @Override
      public void set(Node<K, V> e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(Node<K, V> e) {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * A sub-map view, defined by a range of indices.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubMapByIndices implements RandomAccessNavigableMap<K, V> {
      private final int startIndex;
      private int endIndex;
      private int myModCount;
      // TODO: fix! startNode should be node *before* start of submap and comparisons should treat
      // it as an exclusive bound (currently its set to first element and assumed to be inclusive)
      private Node<K, V> startNode;
      private Node<K, V> endNode;
      
      SubMapByIndices(int startIndex, int endIndex) {
         this.startIndex = startIndex;
         this.endIndex = endIndex;
         this.myModCount = modCount;
      }
      
      void checkModCount() {
         if (myModCount != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      void contractAfterRemove() {
         endIndex--;
         // TODO: could be a little more efficient by only clearing this when really required,
         // based on what key was removed (e.g. only clear endNode when last item in submap is
         // removed and only clear startNode when first itme is removed)
         endNode = startNode = null;
         myModCount = modCount;
      }
      
      private void expandAfterAdd(int expandBy) {
         if (expandBy == 0) {
            return;
         }
         endIndex += expandBy;
         // TODO: could be a little more efficient by only clearing this when really required,
         // based on where key was added (e.g. only clear endNode when we store an item after the
         // end of the submap)
         endNode = null;
         myModCount = modCount;
      }
      
      private Node<K, V> startNode() {
         if (startNode == null && endIndex != startIndex) {
            startNode = WeightBalancedTreeMap.this.getByIndex(startIndex);
         }
         return startNode;
      }
      
      private Node<K, V> endNode() {
         if (endNode == null && endIndex != size) {
            endNode = WeightBalancedTreeMap.this.getByIndex(endIndex);
         }
         return endNode;
      }
      
      boolean isKeyInRange(K key) {
         K startKey = startNode().key;
         Node<K, V> end = endNode();
         return comparator.compare(key, startKey) >= 0
               && (end == null || comparator.compare(key, end.key) < 0);
      }
      
      void checkRange(int index) {
         if  (index < 0) {
            throw new IndexOutOfBoundsException(index + " < 0");
         } else if (index >= endIndex - startIndex) {
            throw new IndexOutOfBoundsException(index + " >= " + (endIndex - startIndex));
         }
      }
      
      void checkWideRange(int index) {
         if  (index < 0) {
            throw new IndexOutOfBoundsException(index + " < 0");
         } else if (index > endIndex - startIndex) {
            throw new IndexOutOfBoundsException(index + " > " + (endIndex - startIndex));
         }
      }
      
      boolean isInSubRange(int index) {
         return index >= startIndex && index < endIndex;
      }
      
      int adjustIndex(int index) {
         return startIndex + index;
      }
      
      @Override
      public int size() {
         checkModCount();
         return endIndex - startIndex;
      }

      @Override
      public boolean isEmpty() {
         checkModCount();
         return startIndex == endIndex;
      }

      @Override
      public Map.Entry<K, V> lowerEntry(K key) {
         checkModCount();
         Map.Entry<K, V> candidate = WeightBalancedTreeMap.this.lowerEntry(key);
         if (candidate == null) {
            return null;
         }
         return isKeyInRange(candidate.getKey()) ? candidate : null;
      }

      @Override
      public K lowerKey(K key) {
         Map.Entry<K, V> entry = lowerEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Map.Entry<K, V> floorEntry(K key) {
         checkModCount();
         Map.Entry<K, V> candidate = WeightBalancedTreeMap.this.floorEntry(key);
         if (candidate == null) {
            return null;
         }
         return isKeyInRange(candidate.getKey()) ? candidate : null;
      }

      @Override
      public K floorKey(K key) {
         Map.Entry<K, V> entry = floorEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Map.Entry<K, V> ceilingEntry(K key) {
         checkModCount();
         Map.Entry<K, V> candidate = WeightBalancedTreeMap.this.ceilingEntry(key);
         if (candidate == null) {
            return null;
         }
         return isKeyInRange(candidate.getKey()) ? candidate : null;
      }

      @Override
      public K ceilingKey(K key) {
         Map.Entry<K, V> entry = ceilingEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Map.Entry<K, V> higherEntry(K key) {
         checkModCount();
         Map.Entry<K, V> candidate = WeightBalancedTreeMap.this.higherEntry(key);
         if (candidate == null) {
            return null;
         }
         return isKeyInRange(candidate.getKey()) ? candidate : null;
      }

      @Override
      public K higherKey(K key) {
         Map.Entry<K, V> entry = higherEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Map.Entry<K, V> firstEntry() {
         checkModCount();
         return isEmpty() ? null : mapEntry(startNode());
      }

      @Override
      public Map.Entry<K, V> lastEntry() {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> end = endNode();
         return end == null ? WeightBalancedTreeMap.this.lastEntry()
               : WeightBalancedTreeMap.this.lowerEntry(end.key);
      }

      @Override
      public Map.Entry<K, V> pollFirstEntry() {
         checkModCount();
         Map.Entry<K, V> first = firstEntry();
         if (first == null) {
            return null;
         }
         remove(first.getKey());
         contractAfterRemove();
         return first;
      }

      @Override
      public Map.Entry<K, V> pollLastEntry() {
         checkModCount();
         Map.Entry<K, V> last = lastEntry();
         if (last == null) {
            return null;
         }
         remove(last.getKey());
         contractAfterRemove();
         return last;
      }

      @Override
      public Comparator<? super K> comparator() {
         checkModCount();
         return WeightBalancedTreeMap.this.comparator;
      }

      @Override
      public K firstKey() {
         Map.Entry<K, V> entry = firstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K lastKey() {
         Map.Entry<K, V> entry = lastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public boolean containsKey(Object key) {
         checkModCount();
         return isInSubRange(WeightBalancedTreeMap.this.indexOfKey(key));
      }

      @Override
      public boolean containsValue(Object value) {
         return values().contains(value);
      }

      @Override
      public V get(Object key) {
         checkModCount();
         // get() is spec'ed to throw ClassCastException if the comparator doesn't like the
         // specified object, so it's okay to cast here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         return isKeyInRange(k) ? WeightBalancedTreeMap.this.get(key) : null;
      }

      @Override
      public V put(K key, V value) {
         checkModCount();
         if (isKeyInRange(key)) {
            int sz = size;
            V ret = WeightBalancedTreeMap.this.put(key, value);
            expandAfterAdd(size - sz);
            return ret;
         } else {
            throw new IllegalArgumentException("key " + key + " out of range for sub-map");
         }
      }

      @Override
      public V remove(Object key) {
         checkModCount();
         // remove() is spec'ed to throw ClassCastException if the comparator doesn't like the
         // specified object, so it's okay to cast here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         if (isKeyInRange(k)) {
            V ret = WeightBalancedTreeMap.this.remove(key);
            contractAfterRemove();
            return ret;
         }
         return null;
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         checkModCount();
         for (K key : m.keySet()) {
            if (!isKeyInRange(key)) {
               throw new IllegalArgumentException("key " + key + " out of range for sub-map");
            }
         }
         int sz = size;
         WeightBalancedTreeMap.this.putAll(m);
         expandAfterAdd(size - sz);
      }

      @Override
      public void clear() {
         for (Iterator<Map.Entry<K, V>> iter = listIterator(); iter.hasNext();) {
            iter.next();
            iter.remove();
         }
      }

      @Override
      public RandomAccessSet<K> keySet() {
         return navigableKeySet();
      }

      @Override
      public List<V> values() {
         return new TransformingList.RandomAccess<Map.Entry<K, V>, V>(
               new RandomAccessSetList<Map.Entry<K, V>>(entrySet()),
               new Function<Map.Entry<K, V>, V>() {
                  @Override public V apply(Map.Entry<K, V> input) {
                     return input.getValue();
                  }
               });
      }

      @Override
      public RandomAccessSet<Map.Entry<K, V>> entrySet() {
         checkModCount();
         return new EntrySet<K, V>(this);
      }

      @Override
      public ListIterator<Map.Entry<K, V>> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<Map.Entry<K, V>> listIterator(int index) {
         checkModCount();
         checkWideRange(index);
         return new TransformingListIterator<Node<K, V>, Map.Entry<K, V>>(
               new SubMapByIndicesIterator<K, V>(new ListIteratorImpl(adjustIndex(index)), this),
               new Function<Node<K, V>, Map.Entry<K, V>>() {
                  @Override public Map.Entry<K, V> apply(Node<K, V> input) {
                     return mapEntry(input);
                  }
               });
      }

      @Override
      public Map.Entry<K, V> getEntry(int index) {
         checkModCount();
         checkRange(index);
         return WeightBalancedTreeMap.this.getEntry(adjustIndex(index));
      }

      @Override
      public Map.Entry<K, V> removeEntry(int index) {
         checkModCount();
         checkRange(index);
         return WeightBalancedTreeMap.this.removeEntry(adjustIndex(index));
      }

      @Override
      public int indexOfKey(Object key) {
         checkModCount();
         int index = WeightBalancedTreeMap.this.indexOfKey(key);
         return isInSubRange(index) ? index - startIndex : -1;
      }

      @Override
      public RandomAccessNavigableMap<K, V> subMapByIndices(int fromIndex, int toIndex) {
         checkModCount();
         checkRange(fromIndex);
         checkWideRange(toIndex);
         return new SubMapByIndices(adjustIndex(fromIndex), adjustIndex(toIndex));
      }

      @Override
      public RandomAccessNavigableMap<K, V> descendingMap() {
         return new DescendingRandomAccessMap<K, V>(this);
      }

      @Override
      public RandomAccessNavigableSet<K> navigableKeySet() {
         return new KeySet<K, V>(this);
      }

      @Override
      public RandomAccessNavigableSet<K> descendingKeySet() {
         return navigableKeySet().descendingSet();
      }

      @Override
      public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey) {
         return subMap(fromKey, true, toKey, false);
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey) {
         return headMap(toKey, false);
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey) {
         return tailMap(fromKey, true);
      }
   }

   /**
    * Implements an iterator over a {@linkplain RandomAccessSet#subSetByIndices(int, int) sub-list} of
    * elements for the various set views of this map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <K> the type of key in the map
    * @param <V> the type of element in the map
    */
   private static class SubMapByIndicesIterator<K, V> implements ListIterator<Node<K, V>> {
      private final ListIterator<Node<K, V>> iterator;
      private final WeightBalancedTreeMap<K, V>.SubMapByIndices submap;
      
      SubMapByIndicesIterator(ListIterator<Node<K, V>> iterator,
            WeightBalancedTreeMap<K, V>.SubMapByIndices subset) {
         this.iterator = iterator;
         this.submap = subset;
      }

      @Override
      public boolean hasNext() {
         return submap.isInSubRange(iterator.nextIndex());
      }

      @Override
      public Node<K, V> next() {
         if (submap.isInSubRange(iterator.nextIndex())) {
            return iterator.next();
         }
         throw new NoSuchElementException();
      }

      @Override
      public boolean hasPrevious() {
         return submap.isInSubRange(iterator.previousIndex());
      }

      @Override
      public Node<K, V> previous() {
         if (submap.isInSubRange(iterator.previousIndex())) {
            return iterator.previous();
         }
         throw new NoSuchElementException();
      }

      @Override
      public int nextIndex() {
         return submap.adjustIndex(iterator.nextIndex());
      }

      @Override
      public int previousIndex() {
         return submap.adjustIndex(iterator.previousIndex());
      }

      @Override
      public void remove() {
         iterator.remove();
         submap.contractAfterRemove();
      }

      @Override
      public void set(Node<K, V> e) {
         throw new UnsupportedOperationException("set");
      }

      @Override
      public void add(Node<K, V> e) {
         throw new UnsupportedOperationException("add");
      }
   }

   /**
    * The view of nodes as a set of {@link Map.Entry} objects.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class EntrySet<K, V> implements RandomAccessSet<Map.Entry<K, V>> {
      private final RandomAccessNavigableMap<K, V> map;
      
      EntrySet(RandomAccessNavigableMap<K, V> map) {
         this.map = map;
      }

      @Override
      public int size() {
         return map.size();
      }

      @Override
      public boolean isEmpty() {
         return map.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return CollectionUtils.contains(iterator(), o);
      }

      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
         return map.listIterator();
      }

      @Override
      public Object[] toArray() {
         return CollectionUtils.toArray(this);
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return CollectionUtils.toArray(this, a);
      }

      @Override
      public boolean add(Map.Entry<K, V> e) {
         throw new UnsupportedOperationException("add");
      }

      @Override
      public boolean remove(Object o) {
         return CollectionUtils.removeObject(o,  iterator(),  true);
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return CollectionUtils.containsAll(this, c);
      }

      @Override
      public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
         throw new UnsupportedOperationException("addAll");
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), false);
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), true);
      }

      @Override
      public void clear() {
         map.clear();
      }

      @Override
      public Map.Entry<K, V> get(int index) {
         return map.getEntry(index);
      }

      @Override
      public int indexOf(Object o) {
         ListIterator<Map.Entry<K, V>> iter = listIterator();
         while (iter.hasNext()) {
            Map.Entry<K, V> entry = iter.next();
            if (entry.equals(o)) {
               return iter.previousIndex();
            }
         }
         return -1;
      }

      @Override
      public ListIterator<Map.Entry<K, V>> listIterator() {
         return map.listIterator();
      }

      @Override
      public ListIterator<Map.Entry<K, V>> listIterator(int index) {
         return map.listIterator(index);
      }

      @Override
      public Map.Entry<K, V> remove(int index) {
         return map.removeEntry(index);
      }

      @Override
      public RandomAccessSet<Map.Entry<K, V>> subSetByIndices(int fromIndex, int toIndex) {
         return new EntrySet<K, V>(map.subMapByIndices(fromIndex,  toIndex));
      }

      @Override
      public List<Map.Entry<K, V>> asList() {
         return new RandomAccessSetList<Map.Entry<K, V>>(this);
      }
      
      @Override
      public boolean equals(Object o) {
         return CollectionUtils.equals(this,  o);
      }
      
      @Override
      public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
      
      @Override
      public String toString() {
         return CollectionUtils.toString(this);
      }
   }

   /**
    * A view of the map's keys as a set.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class KeySet<K, V> implements RandomAccessNavigableSet<K> {
      private final RandomAccessNavigableMap<K, V> map;
      
      KeySet(RandomAccessNavigableMap<K, V> map) {
         this.map = map;
      }
      
      @Override
      public K lower(K e) {
         return map.lowerKey(e);
      }

      @Override
      public K floor(K e) {
         return map.floorKey(e);
      }

      @Override
      public K ceiling(K e) {
         return map.ceilingKey(e);
      }

      @Override
      public K higher(K e) {
         return map.higherKey(e);
      }

      @Override
      public K pollFirst() {
         Map.Entry<K, V> entry = map.pollFirstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K pollLast() {
         Map.Entry<K, V> entry = map.pollLastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Iterator<K> iterator() {
         return listIterator();
      }

      @Override
      public Iterator<K> descendingIterator() {
         return new TransformingIterator<Map.Entry<K, V>, K>(
                  CollectionUtils.reverseIterator(map.listIterator(map.size())),
                  new Function<Map.Entry<K, V>, K>() {
                     @Override public K apply(Map.Entry<K, V> node) {
                        return node.getKey();
                     }
                  });
      }

      @Override
      public Comparator<? super K> comparator() {
         return map.comparator();
      }

      @Override
      public K first() {
         return map.firstKey();
      }

      @Override
      public K last() {
         return map.lastKey();
      }

      @Override
      public int size() {
         return map.size();
      }

      @Override
      public boolean isEmpty() {
         return map.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return map.containsKey(o);
      }

      @Override
      public Object[] toArray() {
         return CollectionUtils.toArray(this);
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return CollectionUtils.toArray(this, a);
      }

      @Override
      public boolean add(K e) {
         throw new UnsupportedOperationException("add");
      }

      @Override
      public boolean remove(Object o) {
         @SuppressWarnings("unchecked")
         Iterator<Map.Entry<K, V>> iter = map.tailMap((K) o).listIterator();
         if (iter.hasNext()) {
            K k = iter.next().getKey();
            if (k == null ? o == null : k.equals(o)) {
               iter.remove();
               return true;
            }
         }
         return false;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return CollectionUtils.containsAll(this,  c);
      }

      @Override
      public boolean addAll(Collection<? extends K> c) {
         throw new UnsupportedOperationException("addAll");
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), false);
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), true);
      }

      @Override
      public void clear() {
         map.clear();
      }

      @Override
      public K get(int index) {
         Map.Entry<K, V> entry = map.getEntry(index);
         return entry == null ? null : entry.getKey();
      }
      
      @Override
      public int indexOf(Object o) {
         return map.indexOfKey(o);
      }

      @Override
      public ListIterator<K> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<K> listIterator(int index) {
         return new TransformingListIterator<Map.Entry<K, V>, K>(map.listIterator(index),
               new Function<Map.Entry<K, V>, K>() {
                  @Override public K apply(Map.Entry<K, V> input) {
                     return input.getKey();
                  }
               });
      }

      @Override
      public K remove(int index) {
         Map.Entry<K, V> entry = map.removeEntry(index);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public List<K> asList() {
         return new RandomAccessSetList<K>(this);
      }

      @Override
      public RandomAccessNavigableSet<K> subSetByIndices(int fromIndex, int toIndex) {
         return new KeySet<K, V>(map.subMapByIndices(fromIndex, toIndex));
      }

      @Override
      public RandomAccessNavigableSet<K> descendingSet() {
         return new DescendingRandomAccessSet<K>(this);
      }

      @Override
      public RandomAccessNavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement,
            boolean toInclusive) {
         return new KeySet<K, V>(map.subMap(fromElement, fromInclusive, toElement, toInclusive));
      }

      @Override
      public RandomAccessNavigableSet<K> headSet(K toElement, boolean inclusive) {
         return new KeySet<K, V>(map.headMap(toElement, inclusive));
      }

      @Override
      public RandomAccessNavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return new KeySet<K, V>(map.tailMap(fromElement, inclusive));
      }

      @Override
      public RandomAccessNavigableSet<K> subSet(K fromElement, K toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      @Override
      public RandomAccessNavigableSet<K> headSet(K toElement) {
         return headSet(toElement, false);
      }

      @Override
      public RandomAccessNavigableSet<K> tailSet(K fromElement) {
         return tailSet(fromElement, true);
      }
      
      @Override
      public boolean equals(Object o) {
         return CollectionUtils.equals(this,  o);
      }
      
      @Override
      public int hashCode() {
         return CollectionUtils.hashCode(this);
      }
      
      @Override
      public String toString() {
         return CollectionUtils.toString(this);
      }
   }
   
   
   
   
   
   
   
   
   
   
   


   private static Random rnd = new Random();
   
   private static String randomString() {
      StringBuilder sb = new StringBuilder(10);
      for (int i = 0; i < 10; i++) {
         sb.append((char)('a' + rnd.nextInt(26)));
      }
      return sb.toString();
   }
   
   private static <K, V> int checkBalance(Node<K, V> entry) {
      if (entry == null) {
         return 0;
      }
      int leftCount = checkBalance(entry.left);
      int rightCount = checkBalance(entry.right);
      if (Math.abs(leftCount - rightCount) > 1) {
         System.out.println("map entry (" + entry.key + " -> " + entry.value + ") is not balanced: "
               + " left size = " + leftCount + ", right count = " + rightCount);
      }
      int count = leftCount + 1 + rightCount;
      if (count != entry.subTreeSize) {
         System.out.println("map entry (" + entry.key + " -> " + entry.value + ") size "
               + entry.subTreeSize + " doesn't match node count " + count);
      }
      return count;
   }
   
   public static void main(String args[]) {
      WeightBalancedTreeMap<String, Integer> weightBalanced = new WeightBalancedTreeMap<String, Integer>();
      for (int i = 0; i < 1023; i++) {
         weightBalanced.put(randomString(), i);
      }
      // check tree for balance
      int size = checkBalance(weightBalanced.root);
      if (size != weightBalanced.size()) {
         System.out.println("map size " + weightBalanced.size() + " doesn't match node count " + size);
      }
      // bfs to print node sizes
      class Tuple {
         Node<String, Integer> entry;
         int level;
         Tuple(Node<String, Integer> entry, int level) {
            this.entry = entry;
            this.level = level;
         }
      }
      LinkedList<Tuple> queue = new LinkedList<Tuple>();
      queue.add(new Tuple(weightBalanced.root, 1));
      int level = 0;
      while (!queue.isEmpty()) {
         Tuple t = queue.remove();
         if (t.level != level) {
            System.out.println();
            System.out.print("" + t.level + ". ");
            level = t.level;
         }
         System.out.print("" + t.entry.subTreeSize + " ");
         if (t.entry.left != null) {
            queue.add(new Tuple(t.entry.left, t.level + 1));
         }
         if (t.entry.right != null) {
            queue.add(new Tuple(t.entry.right, t.level + 1));
         }
      }
      System.out.println();

      System.out.println("------------------");

      // dump contents
      String key = weightBalanced.firstKey();
      int i = 0;
      while (key != null) {
         System.out.println("" + (++i) + ". " + key + " -> " + weightBalanced.get(key));
         key = weightBalanced.higherKey(key);
      }

      System.out.println("---- Iterator ----");
      
      i = 0;
      for (Iterator<Node<String, Integer>> iter = weightBalanced.new IteratorImpl(); iter.hasNext();) {
         Node<String, Integer> node = iter.next();
         System.out.println("" + (++i) + ". " + node.key + " -> " + node.value);
      }

      System.out.println("-- ListIterator --");
      
      for (ListIterator<Node<String, Integer>> iter = weightBalanced.new ListIteratorImpl(0); iter.hasNext();) {
         Node<String, Integer> node = iter.next();
         System.out.println("" + iter.previousIndex() + ". " + node.key + " -> " + node.value);
      }

      System.out.println("- ListIterator/2 -");
      
      for (ListIterator<Node<String, Integer>> iter = weightBalanced.new ListIteratorImpl(700); iter.hasNext();) {
         Node<String, Integer> node = iter.next();
         System.out.println("" + iter.previousIndex() + ". " + node.key + " -> " + node.value);
      }

      System.out.println("------------------");

      // dump backwards
      key = weightBalanced.lastKey();
      i = 0;
      while (key != null) {
         System.out.println("" + (++i) + ". " + key + " -> " + weightBalanced.get(key));
         key = weightBalanced.lowerKey(key);
      }
      
      System.out.println("-- ListIterator --");
      
      for (ListIterator<Node<String, Integer>> iter = weightBalanced.new ListIteratorImpl(1023); iter.hasPrevious();) {
         Node<String, Integer> node = iter.previous();
         System.out.println("" + iter.nextIndex() + ". " + node.key + " -> " + node.value);
      }

      System.out.println("------------------");
      
      Map<String, Integer> original = new TreeMap<String, Integer>();
      original.put("abc", 9);
      original.put("def", 8);
      original.put("ghi", 7);
      original.put("jkl", 6);
      original.put("mno", 5);
      original.put("pqr", 4);
      original.put("stu", 3);
      original.put("vwx", 2);
      original.put("yz", 1);
      weightBalanced = new WeightBalancedTreeMap<String, Integer>(original);
      System.out.println("abc -> " + weightBalanced.get("abc"));
      System.out.println("def -> " + weightBalanced.get("stu"));
      System.out.println("zzz -> " + weightBalanced.get("zzz"));
      System.out.println("zzz -> " + weightBalanced.get("zzz"));
      System.out.println("floor(yyz) = " + weightBalanced.floorKey("yyz"));
      System.out.println("floor(jkl) = " + weightBalanced.floorKey("jkl"));
      System.out.println("floor(aaa) = " + weightBalanced.floorKey("aaa"));
      System.out.println("ceil(fff) = " + weightBalanced.ceilingKey("fff"));
      System.out.println("ceil(stu) = " + weightBalanced.ceilingKey("stu"));
      System.out.println("ceil(zzz) = " + weightBalanced.ceilingKey("zzz"));
      System.out.println("low(yyz) = " + weightBalanced.lowerKey("yyz"));
      System.out.println("low(jkl) = " + weightBalanced.lowerKey("jkl"));
      System.out.println("low(aaa) = " + weightBalanced.lowerKey("aaa"));
      System.out.println("high(fff) = " + weightBalanced.higherKey("fff"));
      System.out.println("high(stu) = " + weightBalanced.higherKey("stu"));
      System.out.println("high(zzz) = " + weightBalanced.higherKey("zzz"));
      System.out.println("contains(0) = " + weightBalanced.containsValue(0));
      System.out.println("contains(3) = " + weightBalanced.containsValue(3));
      System.out.println("contains(9) = " + weightBalanced.containsValue(9));
      System.out.println("contains(27) = " + weightBalanced.containsValue(27));
      System.out.println("first: " + weightBalanced.firstKey() + " -> " + weightBalanced.firstEntry().getValue());
      System.out.println("last: " + weightBalanced.lastKey() + " -> " + weightBalanced.lastEntry().getValue());
    }
}