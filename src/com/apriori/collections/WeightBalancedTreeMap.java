package com.apriori.collections;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

//TODO: implement me! (don't forget serialization and cloning)
//TODO: javadoc
//TODO: tests
public class WeightBalancedTreeMap<K, V>
      implements RandomAccessNavigableMap<K, V>, Serializable, Cloneable {

   private static class Entry<K, V> implements Map.Entry<K, V> {
      K key;
      V value;
      int subTreeSize;
      Entry<K, V> left, right;
      
      Entry(Map.Entry<? extends K, ? extends V> other) {
         this(other.getKey(), other.getValue());
      }

      Entry(K key, V value) {
         this.key = key;
         this.value = value;
         subTreeSize = 1;
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
         V ret = this.value;
         this.value = value;
         return ret;
      }
   }
   
   transient int size;
   transient Entry<K, V> root;
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

   @Override
   public List<V> values() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessSet<Map.Entry<K, V>> entrySet() {
      // TODO Auto-generated method stub
      return null;
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
      return getEntry(root, key) != null;
   }

   private boolean searchForValue(Entry<K, V> entry, Object value) {
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
   
   private Entry<K, V> getEntry(Entry<K, V> entry, Object key) {
      @SuppressWarnings("unchecked") // safe if user provided valid/correct comparator
      int c = comparator.compare((K) key, entry.key);
      if (c == 0) {
         return entry;
      } else if (c < 0) {
         if (entry.left == null) {
            return null;
         }
          return getEntry(entry.left, key);
      } else {
         if (entry.right == null) {
            return null;
         }
         return getEntry(entry.right, key);
      }
   }

   @Override
   public V get(Object key) {
      Entry<K, V> entry = getEntry(root, key);
      return entry == null ? null : entry.value;
   }
   
   private Entry<K, V> subtreePredecessor(Entry<K, V> entry) {
      Entry<K, V> predecessor = entry.left;
      while (predecessor.right != null) {
         predecessor = predecessor.right;
      }
      return predecessor;
   }

   private Entry<K, V> subtreeSuccessor(Entry<K, V> entry) {
      Entry<K, V> successor = entry.right;
      while (successor.left != null) {
         successor = successor.left;
      }
      return successor;
   }
   
   private void swap(Entry<K, V> e1, Entry<K, V> e2) {
      K tmpKey = e1.key;
      e1.key = e2.key;
      e2.key = tmpKey;
      V tmpVal = e1.value;
      e1.value = e2.value;
      e2.value = tmpVal;
   }

   private void rotateRight(Entry<K, V> entry) {
      // to move an item from left to right, we remove the leaf predecessor from the left sub-tree,
      // swap places with it, and then add current value to right sub-tree
      Entry<K, V> other = subtreePredecessor(entry);
      removeEntry(entry, other.key);
      swap(entry, other);
      // reset node attributes and then re-insert
      other.left = other.right = null;
      other.subTreeSize = 1;
      addEntry(entry, other);
   }

   private void rotateLeft(Entry<K, V> entry) {
      // to move from right to left, mirror image of logic in rotateRight
      Entry<K, V> other = subtreeSuccessor(entry);
      removeEntry(entry, other.key);
      swap(entry, other);
      other.left = other.right = null;
      other.subTreeSize = 1;
      addEntry(entry, other);
   }
   
   private void rebalance(Entry<K, V> entry) {
      int left = entry.left == null ? 0 : entry.left.subTreeSize;
      int right = entry.right == null ? 0 : entry.right.subTreeSize;
      if (left > right + 1) {
         rotateRight(entry);
      } else if (right > left + 1) {
         rotateLeft(entry);
      }
   }

   private Entry<K, V> addEntry(Entry<K, V> current, Entry<K, V> newEntry) {
      int c = comparator.compare(newEntry.key, current.key);
      Entry<K, V> priorEntry;
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
      Entry<K, V> newEntry = new Entry<K, V>(key, value);
      V priorValue;
      if (root == null) {
         root = newEntry;
         priorValue = null;
      } else {
         Entry<K, V> priorEntry = addEntry(root, newEntry);
         priorValue = priorEntry == null ? null : priorEntry.value;
      }
      size++;
      modCount++;
      return priorValue;
   }

   private Entry<K, V> removeEntry(Entry<K, V> entry, Object key) {
      if (entry == null) {
         return null;
      }
      @SuppressWarnings("unchecked") // safe if user provided valid/correct comparator
      int c = comparator.compare((K) key, entry.key);
      Entry<K, V> removed;
      if (c == 0) {
         if (entry.left != null && entry.right != null) {
            // if two children, swap with predecessor and then recurse so we can remove that
            // predecessor node and then rebalance
            Entry<K, V> predecessor = subtreePredecessor(entry);
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
      Entry<K, V> removed = removeEntry(root, key);
      if (removed == null) {
         return null;
      } else if (removed == root) {
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
   
   private Entry<K, V> balancedTreeFromArray(Map.Entry<? extends K, ? extends V> entries[],
         int start, int len) {
      int mid = start + (len >> 1);
      Entry<K, V> entry = new Entry<K, V>(entries[mid]);
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
      Entry<K, V> max = null;
      Entry<K, V> current = root;
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
      return max;
   }

   @Override
   public K lowerKey(K key) {
      Map.Entry<K, V> entry = lowerEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> floorEntry(K key) {
      Entry<K, V> max = null;
      Entry<K, V> current = root;
      while (current != null) {
         int c = comparator.compare(key, current.key);
         if (c == 0) {
            return current;
         } else if (c < 0) {
            current = current.left;
         } else {
            if (max == null || comparator.compare(current.key, max.key) > 0) {
               max = current;
            }
            current = current.right;
         }
      }
      return max;
   }

   @Override
   public K floorKey(K key) {
      Map.Entry<K, V> entry = floorEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> ceilingEntry(K key) {
      Entry<K, V> min = null;
      Entry<K, V> current = root;
      while (current != null) {
         int c = comparator.compare(key, current.key);
         if (c == 0) {
            return current;
         } else if (c < 0) {
            if (min == null || comparator.compare(current.key, min.key) < 0) {
               min = current;
            }
            current = current.left;
         } else {
            current = current.right;
         }
      }
      return min;
   }

   @Override
   public K ceilingKey(K key) {
      Map.Entry<K, V> entry = ceilingEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Map.Entry<K, V> higherEntry(K key) {
      Entry<K, V> min = null;
      Entry<K, V> current = root;
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
      return min;
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
      Entry<K, V> entry = root;
      while (entry.left != null) {
         entry = entry.left;
      }
      return entry;
   }

   @Override
   public Map.Entry<K, V> lastEntry() {
      if (root == null) {
         return null;
      }
      Entry<K, V> entry = root;
      while (entry.right != null) {
         entry = entry.right;
      }
      return entry;
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
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableSet<K> navigableKeySet() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableSet<K> descendingKeySet() {
      // TODO Auto-generated method stub
      return null;
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
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableMap<K, V> headMap(K toKey) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableMap<K, V> tailMap(K fromKey) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableSet<K> keySet() {
      return navigableKeySet();
   }
   


   private static Random rnd = new Random();
   
   private static String randomString() {
      StringBuilder sb = new StringBuilder(10);
      for (int i = 0; i < 10; i++) {
         sb.append((char)('a' + rnd.nextInt(26)));
      }
      return sb.toString();
   }
   
   private static <K, V> int checkBalance(Entry<K, V> entry) {
      if (entry == null) {
         return 0;
      }
      int count = checkBalance(entry.left) + 1 + checkBalance(entry.right);
      if (count != entry.subTreeSize) {
         System.out.println("map entry (" + entry.key + " -> " + entry.value + ") size "
               + entry.subTreeSize + " doesn't match node count " + count);
      }
      return count;
   }
   
   public static void main(String args[]) {
      WeightBalancedTreeMap<String, Integer> weightBalanced = new WeightBalancedTreeMap<String, Integer>();
      for (int i = 0; i < 1000; i++) {
         weightBalanced.put(randomString(), i);
      }
      // check tree for balance
      int size = checkBalance(weightBalanced.root);
      if (size != weightBalanced.size()) {
         System.out.println("map size " + weightBalanced.size() + " doesn't match node count " + size);
      }
      // bfs to print node sizes
      class Tuple {
         Entry<String, Integer> entry;
         int level;
         Tuple(Entry<String, Integer> entry, int level) {
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

      System.out.println("------------------");

      // dump backwards
      key = weightBalanced.lastKey();
      i = 0;
      while (key != null) {
         System.out.println("" + (++i) + ". " + key + " -> " + weightBalanced.get(key));
         key = weightBalanced.lowerKey(key);
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
