package com.apriori.collections;

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

/**
 * A binary search tree that always maintains perfect balance. This is an experimental data
 * structure. In practice, the overhead to maintain balance during mutations isn't really worth the
 * minimal performance benefit for queries when compared to a height-balanced tree.
 * 
 * <p>The time to rebalance with the current algorithm is <em>O( (log n) <sup>3</sup> )</em>. Even
 * though this is still sub-linear, it ends up being significantly worse in large trees than the
 * rebalancing operation used by height-balanced trees (which is just <em>O(log n)</em>). Note that
 * minimal time was spent developing the rebalance algorithm and no time has been spent on
 * optimizing its implementation. So it is entirely possible that a logarithmic solution exists to
 * maintain weight balance in the tree.
 * 
 * <p>One interesting artifact is that the state needed to maintain fully weight-balanced trees and
 * sub-trees also makes it possible to randomly access keys by their ordinal position.
 * 
 * <h3>Weight Balance</h3>
 * A tree is weight-balanced when all of the following criteria are met:
 * <ol>
 * <li>The difference in the number of elements between the left and right sub-trees is less than or
 * equal to one.</li>
 * <li>Both left and right sub-trees are also weight-balanced.</li>
 * </ol> 
 * 
 * <a name="add-entry"></a><h3>Adding an Entry</h3>
 * To maintain weight balance when adding nodes to the tree, the following procedure is used:
 * <ol>
 * <li>Perform a standard insertion into a binary tree. This will end up adding a new leaf node to
 * the tree.</li>
 * <li>Check each ancestor of the new node for balance, starting with its parent and ending with the
 * root node (e.g. check each node as you pop up the stack from a recursive insertion). If the node
 * is still in balance, the operation is complete.</li>
 * <li>To restore balance to a node, when the right sub-tree is larger, we <a href="#shift-entry">
 * shift</a> one descendant from right to left, and vice versa: left to right when the left sub-tree
 * is larger.</li>
 * </ol>
 * 
 * <a name="remove-entry"></a><h3>Removing an Entry</h3>
 * To maintain weight balance when removing nodes from the tree, the following procedure is used:
 * <ol>
 * <li>Perform a standard removal from a binary tree.</li>
 * <li>If the node to remove, {@code toBeRemoved}, is an inner node with two children:
 *   <ol type="a">
 *   <li>Find the node from the left sub-tree that has the largest key. We'll call this
 *   node {@code predecessor}.</li>
 *   <li>Swap places -- e.g. swap keys and values between {@code toBeRemoved} and
 *   {@code predecessor}.</li>
 *   <li>Continue with the remove operation, but now removing {@code predecessor} (after the swap,
 *   it now has the key we were originally intending to remove).</li>
 *   </ol>
 * <li>Check each ancestor of the removed node for balance, starting with its parent and ending with
 * the root node (e.g. check each node as you pop up the stack from a recursive removal). If the
 * node is still in balance, the operation is complete.</li>
 * <li>To restore balance to a node, when the right sub-tree is larger, we <a href="#shift-entry">
 * shift</a> one descendant from right to left, and vice versa: left to right when the left sub-tree
 * is larger.</li>
 * </ol>
 * 
 * <a name="shift-entry"></a><h3>Shifting Entries</h3>
 * For a given node, {@code current}, to shift an item from left to right:
 * <ol>
 * <li><a href="#remove-entry">Remove</a> the node from the left sub-tree that has the largest
 * key. We'll call this node {@code predecessor}.</li>
 * <li>Swap places (e.g. swap keys and values) between {@code current} node and the
 * {@code predecessor}.</li>
 * <li><a href="#add-entry">Add</a> the displaced node (which now has the key and value that
 * {@code current} originally had) into the right sub-tree.</li>
 * <li>Note that adding, removing, and shifting entries are all indirectly recursive operations.</li>
 * </ol>
 * 
 * <p>Shifting from right to left is the mirror image of the above steps: 
 * <ol>
 * <li><a href="#remove-entry">Remove</a> the node from the right sub-tree that has the smallest
 * key. We'll call this node {@code successor}.</li>
 * <li>Swap places (e.g. swap keys and values) between {@code current} node and the
 * {@code successor}.</li>
 * <li><a href="#add-entry">Add</a> the displaced node (which now has the key and value that
 * {@code current} originally had) into the left sub-tree.</li>
 * </ol>
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: implement me! (don't forget serialization and cloning)
//TODO: javadoc
//TODO: tests
//TODO: subMapByIndices/subSetByIndices should use checkWide(from) instead of check(from)
public class WeightBalancedTreeMap<K, V>
      implements RandomAccessNavigableMap<K, V>, Serializable, Cloneable {
   
   private static final long serialVersionUID = -3252472174080097845L;

   /**
    * A node in the tree. The only extra state required for balancing the tree is the total size of
    * the sub-tree rooted at this node.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Node<K, V> {
      K key;
      V value;
      int subTreeSize;
      Node<K, V> left, right;
      
      Node(Entry<? extends K, ? extends V> other) {
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
         this.comparator = CollectionUtils.naturalOrder();
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

   Entry<K, V> mapEntry(Node<K, V> node) {
      return node == null ? null : new EntryImpl(node);
   }

   @Override
   public List<V> values() {
      return new TransformingList.RandomAccess<Entry<K, V>, V>(
            new RandomAccessSetList<Entry<K, V>>(entrySet()),
            (entry) -> entry.getValue());
   }

   @Override
   public RandomAccessSet<Entry<K, V>> entrySet() {
      return new EntrySet<K, V>(this);
   }

   @Override
   public Comparator<? super K> comparator() {
      return comparator == Comparator.naturalOrder() ? null : comparator;
   }

   @Override
   public K firstKey() {
      Entry<K, V> entry = firstEntry();
      if (entry == null) {
         throw new NoSuchElementException("map is empty");
      }
      return entry.getKey();
   }

   @Override
   public K lastKey() {
      Entry<K, V> entry = lastEntry();
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
            && (comparator() == null ? ((SortedMap<?,?>) m).comparator() == null
                  : comparator().equals(((SortedMap<?,?>) m).comparator()))) {
         // we can efficiently construct a balanced tree without rotations
         @SuppressWarnings("unchecked")
         Entry<? extends K, ? extends V> entries[] = new Entry[m.size()];
         entries = m.entrySet().toArray(entries);
         size = entries.length;
         root = balancedTreeFromArray(entries, 0, size);
      } else{
         for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
         }
      }
   }
   
   private Node<K, V> balancedTreeFromArray(Entry<? extends K, ? extends V> entries[],
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
   public Entry<K, V> lowerEntry(K key) {
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
      Entry<K, V> entry = lowerEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
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
      Entry<K, V> entry = floorEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
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
      Entry<K, V> entry = ceilingEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
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
      Entry<K, V> entry = higherEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> firstEntry() {
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
   public Entry<K, V> lastEntry() {
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
   public Entry<K, V> pollFirstEntry() {
      Entry<K, V> first = firstEntry();
      if (first == null) {
         return null;
      }
      removeEntry(root, first.getKey());
      return first;
   }

   @Override
   public Entry<K, V> pollLastEntry() {
      Entry<K, V> last = lastEntry();
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
      return new SubMap(fromKey, fromInclusive, toKey, toInclusive);
   }

   @Override
   public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return new SubMap(NO_BOUND, false, toKey, inclusive);
   }

   @Override
   public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new SubMap(fromKey, inclusive, NO_BOUND, false);
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
   public ListIterator<Entry<K, V>> listIterator() {
      return listIterator(0);
   }
   
   @Override
   public ListIterator<Entry<K, V>> listIterator(int index) {
      checkWideRange(index);
      return new TransformingListIterator<Node<K, V>, Entry<K, V>>(new ListIteratorImpl(index),
            (entry) -> mapEntry(entry));
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
   public Entry<K, V> getEntry(int index) {
      checkRange(index);
      Node<K, V> node = getByIndex(index);
      return node == null ? null : mapEntry(node);
   }
   
   @Override
   public Entry<K, V> removeEntry(int index) {
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
    * Wraps a {@link Node} in the {@link Entry} interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class EntryImpl implements Entry<K, V> {
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
      private final WeightBalancedTreeMap<K, V>.SubMapByIndices parent;
      private final int startIndex;
      private int endIndex;
      private int myModCount;
      private Node<K, V> startNode;
      private Node<K, V> endNode;
      
      SubMapByIndices(int startIndex, int endIndex) {
         this(startIndex, endIndex, null);
      }
      
      SubMapByIndices(int startIndex, int endIndex,
            WeightBalancedTreeMap<K, V>.SubMapByIndices parent) {
         this.startIndex = startIndex;
         this.endIndex = endIndex;
         this.myModCount = modCount;
         this.parent = parent;
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
         // removed and only clear startNode when first item is removed)
         endNode = startNode = null;
         myModCount = modCount;
         if (parent != null) {
            parent.contractAfterRemove();
         }
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
         if (parent != null) {
            parent.expandAfterAdd(expandBy);
         }
      }
      
      private Node<K, V> startNode() {
         if (startNode == null && startIndex != 0) {
            startNode = WeightBalancedTreeMap.this.getByIndex(startIndex - 1);
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
         if (endIndex == startIndex) {
            return false;
         }
         Node<K, V> start = startNode();
         Node<K, V> end = endNode();
         return (start == null || comparator.compare(key, start.key) > 0)
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
      public Entry<K, V> lowerEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.key) >= 0) {
            return lastEntry();
         }
         Node<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.key) <= 0) {
            return null;
         }
         Entry<K, V> candidate = WeightBalancedTreeMap.this.lowerEntry(key);
         return start == null || comparator.compare(candidate.getKey(), start.key) > 0
               ? candidate : null;
      }

      @Override
      public K lowerKey(K key) {
         Entry<K, V> entry = lowerEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> floorEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.key) >= 0) {
            return lastEntry();
         }
         Node<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.key) <= 0) {
            return null;
         }
         Entry<K, V> candidate = WeightBalancedTreeMap.this.floorEntry(key);
         return start == null || comparator.compare(candidate.getKey(), start.key) > 0
               ? candidate : null;
      }

      @Override
      public K floorKey(K key) {
         Entry<K, V> entry = floorEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> ceilingEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.key) <= 0) {
            return firstEntry();
         }
         Node<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.key) >= 0) {
            return null;
         }
         Entry<K, V> candidate = WeightBalancedTreeMap.this.ceilingEntry(key);
         return end == null || comparator.compare(candidate.getKey(), end.key) < 0
               ? candidate : null;
      }

      @Override
      public K ceilingKey(K key) {
         Entry<K, V> entry = ceilingEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> higherEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.key) <= 0) {
            return firstEntry();
         }
         Node<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.key) >= 0) {
            return null;
         }
         Entry<K, V> candidate = WeightBalancedTreeMap.this.higherEntry(key);
         return end == null || comparator.compare(candidate.getKey(), end.key) < 0
               ? candidate : null;
      }

      @Override
      public K higherKey(K key) {
         Entry<K, V> entry = higherEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> firstEntry() {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> start = startNode();
         return start == null ? WeightBalancedTreeMap.this.firstEntry()
               : WeightBalancedTreeMap.this.higherEntry(start.key);
      }

      @Override
      public Entry<K, V> lastEntry() {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Node<K, V> end = endNode();
         return end == null ? WeightBalancedTreeMap.this.lastEntry()
               : WeightBalancedTreeMap.this.lowerEntry(end.key);
      }

      @Override
      public Entry<K, V> pollFirstEntry() {
         checkModCount();
         Entry<K, V> first = firstEntry();
         if (first == null) {
            return null;
         }
         remove(first.getKey());
         contractAfterRemove();
         return first;
      }

      @Override
      public Entry<K, V> pollLastEntry() {
         checkModCount();
         Entry<K, V> last = lastEntry();
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
         Entry<K, V> entry = firstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K lastKey() {
         Entry<K, V> entry = lastEntry();
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
         for (Iterator<Entry<K, V>> iter = listIterator(); iter.hasNext();) {
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
         return new TransformingList.RandomAccess<Entry<K, V>, V>(
               new RandomAccessSetList<Entry<K, V>>(entrySet()),
               (entry) -> entry.getValue());
      }

      @Override
      public RandomAccessSet<Entry<K, V>> entrySet() {
         checkModCount();
         return new EntrySet<K, V>(this);
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator(int index) {
         checkModCount();
         checkWideRange(index);
         return new TransformingListIterator<Node<K, V>, Entry<K, V>>(
               new SubMapByIndicesIterator<K, V>(new ListIteratorImpl(adjustIndex(index)), this),
               (entry) -> mapEntry(entry));
      }

      @Override
      public Entry<K, V> getEntry(int index) {
         checkModCount();
         checkRange(index);
         return WeightBalancedTreeMap.this.getEntry(adjustIndex(index));
      }

      @Override
      public Entry<K, V> removeEntry(int index) {
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
         return new SubMapByIndices(adjustIndex(fromIndex), adjustIndex(toIndex), this);
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
         if (comparator.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("fromKey (" + fromKey + ") > toKey (" + toKey + ")");
         }
         K presentLowerBound = fromInclusive ? ceilingKey(fromKey) : higherKey(fromKey);
         K presentUpperBound = toInclusive ? floorKey(toKey) : lowerKey(toKey);
         int fromIndex = presentLowerBound == null ? size() : indexOfKey(presentLowerBound);
         int toIndex = presentUpperBound == null ? 0 : indexOfKey(presentUpperBound) + 1;
         return new SubMapOfSubMapByIndices(adjustIndex(fromIndex), adjustIndex(toIndex),
               fromKey, fromInclusive, toKey, toInclusive, this);
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         K presentUpperBound = inclusive ? floorKey(toKey) : lowerKey(toKey);
         int toIndex = presentUpperBound == null ? 0 : indexOfKey(presentUpperBound) + 1;
         return new SubMapOfSubMapByIndices(startIndex, adjustIndex(toIndex), NO_BOUND, false,
               toKey, inclusive, this);
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         K presentLowerBound = inclusive ? ceilingKey(fromKey) : higherKey(fromKey);
         int fromIndex = presentLowerBound == null ? size() : indexOfKey(presentLowerBound);
         return new SubMapOfSubMapByIndices(adjustIndex(fromIndex), endIndex, fromKey, inclusive,
               NO_BOUND, false, this);
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
    * A placeholder that represents an unlimited bound of a sub-map view.
    */
   static final Object NO_BOUND = new Object();
   
   /**
    * A sub-map view for a {@link SubMapByIndices}. This view has constraints on both the key ranges
    * as well as index ranges.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubMapOfSubMapByIndices extends SubMapByIndices {
      private final Object lowerBound;
      private final boolean lowerInclusive;
      private final Object upperBound;
      private final boolean upperInclusive;
      
      SubMapOfSubMapByIndices(int startIndex, int endIndex, Object startKey, boolean startInclusive,
            Object endKey, boolean endInclusive, WeightBalancedTreeMap<K, V>.SubMapByIndices parent) {
         super(startIndex, endIndex, parent);
         this.lowerBound = startKey;
         this.lowerInclusive = startInclusive;
         this.upperBound = endKey;
         this.upperInclusive = endInclusive;
      }
      
      private boolean isKeyWithinBounds(K key) {
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound == NO_BOUND) {
            return CollectionUtils.isInRangeHigh(key, true, upperBound, upperInclusive, comp);
         } else if (upperBound == NO_BOUND) {
            return CollectionUtils.isInRangeLow(key, true, lowerBound, lowerInclusive, comp);
         } else {
            return CollectionUtils.isInRange(key, lowerBound, lowerInclusive,
                  upperBound, upperInclusive, comp);
         }
      }
      
      @Override boolean isKeyInRange(K key) {
         return isKeyWithinBounds(key) && super.isKeyInRange(key);
      }
      
      @Override
      public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         if (!isKeyWithinBounds(fromKey)) {
            throw new IllegalArgumentException("key " + fromKey + " out of range for sub-map");
         }
         if (!isKeyWithinBounds(toKey)) {
            throw new IllegalArgumentException("key " + toKey + " out of range for sub-map");
         }
         return super.subMap(fromKey, fromInclusive, toKey, toInclusive);
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         if (!isKeyWithinBounds(toKey)) {
            throw new IllegalArgumentException("key " + toKey + " out of range for sub-map");
         }
         return super.headMap(toKey, inclusive);
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         if (!isKeyWithinBounds(fromKey)) {
            throw new IllegalArgumentException("key " + fromKey + " out of range for sub-map");
         }
         return super.tailMap(fromKey, inclusive);
      }
   }

   /**
    * Implements an iterator over a {@linkplain #subMapByIndices(int, int) sub-map (by indices)} of
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
    * A sub-map view, with key value bounds, not indices.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubMap implements RandomAccessNavigableMap<K, V> {
      private final Object lowerBound;
      private final boolean lowerInclusive;
      private final Object upperBound;
      private final boolean upperInclusive;
      private int indexOffset = -1;
      private int mySize = -1;
      private int myModCount = modCount;
      
      SubMap(Object lowerBound, boolean lowerInclusive, Object upperBound, boolean upperInclusive) {
         this.lowerBound = lowerBound;
         this.lowerInclusive = lowerInclusive;
         this.upperBound = upperBound;
         this.upperInclusive = upperInclusive;
      }
      
      @Override
      public Entry<K, V> lowerEntry(K key) {
         Entry<K, V> candidate = WeightBalancedTreeMap.this.lowerEntry(key);
         if (candidate == null) {
            return null;
         }
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound != NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound, lowerInclusive,
                     comp)) {
            return null;
         }
         if (upperBound != NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound, upperInclusive,
                     comp)) {
            return lastEntry();
         }
         return candidate;
      }

      @Override
      public K lowerKey(K key) {
         Entry<K, V> entry = lowerEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> floorEntry(K key) {
         Entry<K, V> candidate = WeightBalancedTreeMap.this.floorEntry(key);
         if (candidate == null) {
            return null;
         }
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound != NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound, lowerInclusive,
                     comp)) {
            return null;
         }
         if (upperBound != NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound, upperInclusive,
                     comp)) {
            return lastEntry();
         }
         return candidate;
      }

      @Override
      public K floorKey(K key) {
         Entry<K, V> entry = floorEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> ceilingEntry(K key) {
         Entry<K, V> candidate = WeightBalancedTreeMap.this.ceilingEntry(key);
         if (candidate == null) {
            return null;
         }
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound != NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound, lowerInclusive,
                     comp)) {
            return firstEntry();
         }
         if (upperBound != NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound, upperInclusive,
                     comp)) {
            return null;
         }
         return candidate;
      }

      @Override
      public K ceilingKey(K key) {
         Entry<K, V> entry = ceilingEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> higherEntry(K key) {
         Entry<K, V> candidate = WeightBalancedTreeMap.this.higherEntry(key);
         if (candidate == null) {
            return null;
         }
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound != NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound, lowerInclusive,
                     comp)) {
            return firstEntry();
         }
         if (upperBound != NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound, upperInclusive,
                     comp)) {
            return null;
         }
         return candidate;
      }

      @Override
      public K higherKey(K key) {
         Entry<K, V> entry = higherEntry(key);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Entry<K, V> firstEntry() {
         if (lowerBound == NO_BOUND) {
            return WeightBalancedTreeMap.this.firstEntry();
         }
         @SuppressWarnings("unchecked") // if not NO_BOUND then it must be a K
         K lowerBoundKey = (K) lowerBound;
         Entry<K, V> candidate = lowerInclusive
               ? WeightBalancedTreeMap.this.ceilingEntry(lowerBoundKey)
               : WeightBalancedTreeMap.this.higherEntry(lowerBoundKey);
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (upperBound != NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound, upperInclusive,
                     comp)) {
            return null;
         }
         return candidate;
      }

      @Override
      public Entry<K, V> lastEntry() {
         if (upperBound == NO_BOUND) {
            return WeightBalancedTreeMap.this.lastEntry();
         }
         @SuppressWarnings("unchecked") // if not NO_BOUND then it must be a K
         K upperBoundKey = (K) upperBound;
         Entry<K, V> candidate = upperInclusive
               ? WeightBalancedTreeMap.this.floorEntry(upperBoundKey)
               : WeightBalancedTreeMap.this.lowerEntry(upperBoundKey);
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound != NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound, lowerInclusive,
                     comp)) {
            return null;
         }
         return candidate;
      }

      @Override
      public Entry<K, V> pollFirstEntry() {
         Entry<K, V> entry = firstEntry();
         if (entry == null) {
            return null;
         }
         remove(entry.getKey());
         return entry;
      }

      @Override
      public Entry<K, V> pollLastEntry() {
         Entry<K, V> entry = lastEntry();
         if (entry == null) {
            return null;
         }
         remove(entry.getKey());
         return entry;
      }

      @Override
      public Comparator<? super K> comparator() {
         return WeightBalancedTreeMap.this.comparator();
      }

      @Override
      public K firstKey() {
         Entry<K, V> entry = firstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K lastKey() {
         Entry<K, V> entry = lastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public int size() {
         if (mySize == -1 || myModCount != modCount) {
            Entry<K, V> first = firstEntry();
            Entry<K, V> last = lastEntry();
            if (first == null || last == null) {
               mySize = 0;
            } else {
               mySize = WeightBalancedTreeMap.this.indexOfKey(last.getKey()) -
                     WeightBalancedTreeMap.this.indexOfKey(first.getKey()) + 1; 
            }
            if (myModCount != modCount) {
               myModCount = modCount;
               indexOffset = -1;
            }
         }
         return mySize;
      }

      @Override
      public boolean isEmpty() {
         return size() == 0;
      }
      
      boolean isInRange(Object key) {
         @SuppressWarnings("unchecked") // we'll only feed it values of the right type, promise
         Comparator<Object> comp = (Comparator<Object>) comparator;
         if (lowerBound == NO_BOUND) {
            return CollectionUtils.isInRangeHigh(key, true, upperBound, upperInclusive, comp);
         } else if (upperBound == NO_BOUND) {
            return CollectionUtils.isInRangeLow(key, true, lowerBound, lowerInclusive, comp);
         } else {
            return CollectionUtils.isInRange(key, lowerBound, lowerInclusive,
                  upperBound, upperInclusive, comp);
         }
      }

      @Override
      public boolean containsKey(Object key) {
         return isInRange(key) ? WeightBalancedTreeMap.this.containsKey(key) : false;
      }

      @Override
      public boolean containsValue(Object value) {
         return values().contains(value);
      }

      @Override
      public V get(Object key) {
         return isInRange(key) ? WeightBalancedTreeMap.this.get(key) : null;
      }

      @Override
      public V put(K key, V value) {
         if (!isInRange(key)) {
            throw new IllegalArgumentException("Key " + key + " outside of submap range");
         }
         return WeightBalancedTreeMap.this.put(key,  value);
      }

      @Override
      public V remove(Object key) {
         return isInRange(key) ? WeightBalancedTreeMap.this.remove(key) : null;
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         for (K k : m.keySet()) {
            if (!isInRange(k)) {
               throw new IllegalArgumentException("Key " + k + " outside of submap range");
            }
         }
         WeightBalancedTreeMap.this.putAll(m);
      }

      @Override
      public void clear() {
         ListIterator<Entry<K, V>> iter = listIterator();
         while (iter.hasNext()) {
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
         return new TransformingList.RandomAccess<Entry<K, V>, V>(
               new RandomAccessSetList<Entry<K, V>>(entrySet()),
               (entry) -> entry.getValue());
      }

      @Override
      public RandomAccessSet<Entry<K, V>> entrySet() {
         return new EntrySet<K, V>(this);
      }
      
      private int getIndexOffset() {
         if (indexOffset == -1 || myModCount != modCount) {
            Entry<K, V> first = firstEntry();
            indexOffset = first == null ? 0 : WeightBalancedTreeMap.this.indexOfKey(first.getKey());
            if (myModCount != modCount) {
               myModCount = modCount;
               mySize = -1;
            }
         }
         return indexOffset;
      }
      
      private void checkRange(int index) {
         if (index < 0) {
            throw new IndexOutOfBoundsException("" + index + " < 0");
         }
         int sz = size();
         if (index >= sz) {
            throw new IndexOutOfBoundsException("" + index + " >= " + sz);
         }
      }
      
      private void checkWideRange(int index) {
         if (index < 0) {
            throw new IndexOutOfBoundsException("" + index + " < 0");
         }
         int sz = size();
         if (index > sz) {
            throw new IndexOutOfBoundsException("" + index + " > " + sz);
         }
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator(int index) {
         int offset = getIndexOffset();
         checkWideRange(index);
         return new TransformingListIterator<Node<K, V>, Entry<K, V>>(
               new SubMapIterator<K, V>(new ListIteratorImpl(index + offset), offset, this),
               (entry) -> mapEntry(entry));
      }

      @Override
      public Entry<K, V> getEntry(int index) {
         checkRange(index);
         return WeightBalancedTreeMap.this.getEntry(index + getIndexOffset());
      }

      @Override
      public Entry<K, V> removeEntry(int index) {
         checkRange(index);
         return WeightBalancedTreeMap.this.removeEntry(index + getIndexOffset());
      }

      @Override
      public int indexOfKey(Object key) {
         return isInRange(key) ? WeightBalancedTreeMap.this.indexOfKey(key) + getIndexOffset() : -1;
      }

      @Override
      public RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex) {
         checkRange(startIndex);
         checkWideRange(endIndex);
         int offset = getIndexOffset();
         return new SubMapByIndices(startIndex + offset, endIndex + offset);
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
         if (!isInRange(toKey)) {
            throw new IllegalArgumentException("to key " + toKey + " is outside sub-map range");
         }
         if (!isInRange(fromKey)) {
            throw new IllegalArgumentException("from key " + fromKey + " is outside sub-map range");
         }
         return new SubMap(fromKey, fromInclusive, toKey, toInclusive);
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         if (!isInRange(toKey)) {
            throw new IllegalArgumentException("to key " + toKey + " is outside sub-map range");
         }
         return new SubMap(NO_BOUND, false, toKey, inclusive);
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         if (!isInRange(fromKey)) {
            throw new IllegalArgumentException("from key " + fromKey + " is outside sub-map range");
         }
         return new SubMap(fromKey, inclusive, NO_BOUND, false);
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
    * Implements an iterator over a {@linkplain #subMap(Object, boolean, Object, boolean) sub-map}
    * of elements for the various set views of this map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <K> the type of key in the map
    * @param <V> the type of element in the map
    */
   private static class SubMapIterator<K, V> implements ListIterator<Node<K, V>> {
      private final ListIterator<Node<K, V>> iterator;
      private final int indexOffset;
      private final WeightBalancedTreeMap<K, V>.SubMap submap;
      private boolean needNext = true;
      private Node<K, V> next;
      private boolean needPrevious = true;
      private Node<K, V> previous;
      
      SubMapIterator(ListIterator<Node<K, V>> iterator, int indexOffset,
            WeightBalancedTreeMap<K, V>.SubMap submap) {
         this.iterator = iterator;
         this.indexOffset = indexOffset;
         this.submap = submap;
      }
      
      private void peekNext() {
         if (needNext && iterator.hasNext()) {
            next = iterator.next();
            iterator.previous(); // go back so cursor stays in the proper position
            needNext = false;
         }
      }
      
      private void peekPrevious() {
         if (needPrevious && iterator.hasPrevious()) {
            previous = iterator.previous();
            iterator.next(); // go back so cursor stays in the proper position
            needPrevious = false;
         }
      }

      @Override
      public boolean hasNext() {
         peekNext();
         return next != null && submap.isInRange(next.key);
      }

      @Override
      public Node<K, V> next() {
         peekNext();
         if (next == null) {
            throw new NoSuchElementException();
         }
         iterator.next(); // advance iterator
         // update local members
         previous = next;
         needPrevious = false;
         next = null;
         needNext = true;
         return previous;
      }

      @Override
      public boolean hasPrevious() {
         peekPrevious();
         return previous != null && submap.isInRange(previous.key);
      }

      @Override
      public Node<K, V> previous() {
         peekPrevious();
         if (previous == null) {
            throw new NoSuchElementException();
         }
         iterator.previous(); // move back iterator
         // update local members
         next = previous;
         needNext = false;
         previous = null;
         needPrevious = true;
         return next;
      }

      @Override
      public int nextIndex() {
         return iterator.nextIndex() + indexOffset;
      }

      @Override
      public int previousIndex() {
         return iterator.previousIndex() + indexOffset;
      }

      @Override
      public void remove() {
         iterator.remove();
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
    * The view of nodes as a set of {@link Entry} objects.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class EntrySet<K, V> implements RandomAccessSet<Entry<K, V>> {
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
      public Iterator<Entry<K, V>> iterator() {
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
      public boolean add(Entry<K, V> e) {
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
      public boolean addAll(Collection<? extends Entry<K, V>> c) {
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
      public Entry<K, V> get(int index) {
         return map.getEntry(index);
      }

      @Override
      public int indexOf(Object o) {
         ListIterator<Entry<K, V>> iter = listIterator();
         while (iter.hasNext()) {
            Entry<K, V> entry = iter.next();
            if (entry.equals(o)) {
               return iter.previousIndex();
            }
         }
         return -1;
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator() {
         return map.listIterator();
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator(int index) {
         return map.listIterator(index);
      }

      @Override
      public Entry<K, V> remove(int index) {
         return map.removeEntry(index);
      }

      @Override
      public RandomAccessSet<Entry<K, V>> subSetByIndices(int fromIndex, int toIndex) {
         return new EntrySet<K, V>(map.subMapByIndices(fromIndex,  toIndex));
      }

      @Override
      public List<Entry<K, V>> asList() {
         return new RandomAccessSetList<Entry<K, V>>(this);
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
         Entry<K, V> entry = map.pollFirstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public K pollLast() {
         Entry<K, V> entry = map.pollLastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public Iterator<K> iterator() {
         return listIterator();
      }

      @Override
      public Iterator<K> descendingIterator() {
         return new TransformingIterator<Entry<K, V>, K>(
                  CollectionUtils.reverseIterator(map.listIterator(map.size())),
                  (entry) -> entry.getKey());
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
         Iterator<Entry<K, V>> iter = map.tailMap((K) o).listIterator();
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
         Entry<K, V> entry = map.getEntry(index);
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
         return new TransformingListIterator<Entry<K, V>, K>(map.listIterator(index),
               (entry) -> entry.getKey());
      }

      @Override
      public K remove(int index) {
         Entry<K, V> entry = map.removeEntry(index);
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
