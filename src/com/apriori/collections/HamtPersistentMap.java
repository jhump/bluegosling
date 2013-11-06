package com.apriori.collections;

import com.apriori.tuples.Pair;

import java.util.Iterator;
import java.util.Map;

// TODO: javadoc
// TODO: tests
public class HamtPersistentMap<K, V> implements PersistentMap<K, V> {

   private static class ListNode<K, V> {
      final K key;
      final V value;
      final ListNode<K, V> next;
      
      ListNode(K key, V value) {
         this(key, value, null);
      }
      
      ListNode(K key, V value, ListNode<K, V> next) {
         this.key = key;
         this.value = value;
         this.next = next;
      }
   }
   
   private interface TrieNode<K, V> {
      boolean containsValue(Object value);
      ListNode<K, V> findNode(int hash, int currentOffset, Object key);
      TrieNode<K, V> remove(int hash, int currentOffset, Object key);
      Pair<TrieNode<K, V>, Boolean> put(int hash, int currentOffset, K key, V value);
   }
   
   private static class IntermediateTrieNode<K, V> implements TrieNode<K, V> {
      final long present;
      final TrieNode<K, V> children[];
      
      IntermediateTrieNode(long present, TrieNode<K, V> firstChild) {
         this.present = present;
         children = createChildren(1);
         children[0] = firstChild;
      }

      IntermediateTrieNode(long present, TrieNode<K, V> children[]) {
         this.present = present;
         this.children = children;
      }
      
      @SuppressWarnings("unchecked")
      private static <K, V> TrieNode<K, V>[] createChildren(int size) {
         return new TrieNode[size];
      }

      @Override
      public boolean containsValue(Object value) {
         for (TrieNode<K, V> child : children) {
            if (child.containsValue(value)) {
               return true;
            }
         }
         return false;
      }

      @Override
      public ListNode<K, V> findNode(int hash, int currentOffset, Object key) {
         int significantBits = (hash >> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            return null;
         }
         int index = Long.bitCount((mask - 1) & present);
         return children[index].findNode(hash, currentOffset + 6, key);
      }

      @Override
      public TrieNode<K, V> remove(int hash, int currentOffset, Object key) {
         int significantBits = (hash >> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            return this;
         }
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> oldChild = children[index];
         TrieNode<K, V> newChild = oldChild.remove(hash, currentOffset + 6, key);
         if (newChild == oldChild) {
            // nothing removed
            return this;
         }
         int numChildren = children.length;
         if (newChild == null) {
            if (--numChildren == 0) {
               // no more children, so scrap this empty node 
               return null;
            }
            // create a replacement node without this child entry
            TrieNode<K, V> newChildren[] = createChildren(numChildren);
            if (index > 0) {
               System.arraycopy(children, 0, newChildren, 0, index);
            }
            if (index < numChildren) {
               System.arraycopy(children, index + 1, newChildren, index, numChildren - index);
            }
            long newPresent = present & ~mask;
            return new IntermediateTrieNode<K, V>(newPresent, newChildren);
         } else {
            // create a replacement node with this child entry swapped
            TrieNode<K, V> newChildren[] = children.clone();
            newChildren[index] = newChild;
            return new IntermediateTrieNode<K, V>(present, newChildren);
         }
      }

      @Override
      public Pair<TrieNode<K, V>, Boolean> put(int hash, int currentOffset, K key, V value) {
         int significantBits = (hash >> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            // TODO: create path and return Pair of <new node, true>
            return null;
         }
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> oldChild = children[index];
         Pair<TrieNode<K, V>, Boolean> addition = oldChild.put(hash, currentOffset + 6, key, value);
         TrieNode<K, V> newChild = addition.getFirst();
         if (newChild == oldChild) {
            // no change
            assert !addition.getSecond();
            return Pair.<TrieNode<K, V>, Boolean>create(this, false);
         }
         // TODO: create replace node and return Pair of <replacement, addition.getSecond()>
         return null;
      }
   }

   private static class LeafTrieNode<K, V> extends ListNode<K, V> implements TrieNode<K, V> {
      LeafTrieNode(K key, V value) {
         super(key, value);
      }

      LeafTrieNode(K key, V value, ListNode<K, V> next) {
         super(key, value, next);
      }

      @Override
      public boolean containsValue(Object searchValue) {
         // TODO: implement me
         return false;
      }

      @Override
      public ListNode<K, V> findNode(int hash, int currentOffset, Object searchKey) {
         assert currentOffset >= 32;
         return doFind(key);
      }
      
      ListNode<K, V> doFind(Object searchKey) {
         // TODO
         return null;
      }

      @Override
      public TrieNode<K, V> remove(int hash, int currentOffset, Object keyToRemove) {
         assert currentOffset >= 32;
         return doRemove(keyToRemove);
      }
      
      TrieNode<K, V> doRemove(Object keyToRemove) {
         // TODO
         return null;
      }

      @Override
      public Pair<TrieNode<K, V>, Boolean> put(int hash, int currentOffset, K newKey, V newValue) {
         assert currentOffset >= 32;
         return doPut(newKey, newValue);
      }
      
      Pair<TrieNode<K, V>, Boolean> doPut(K newKey, V newValue) {
         // TODO
         return null;
      }
   }

   private static class InnerLeafTrieNode<K, V> extends LeafTrieNode<K, V> {
      final int hashCode;
      
      InnerLeafTrieNode(K key, V value, int hashCode) {
         super(key, value);
         this.hashCode = hashCode;
      }

      InnerLeafTrieNode(K key, V value, ListNode<K, V> next, int hashCode) {
         super(key, value, next);
         this.hashCode = hashCode;
      }
      
      @Override
      public ListNode<K, V> findNode(int hash, int currentOffset, Object searchKey) {
         return hash == hashCode ? doFind(searchKey) : null;
      }

      @Override
      public TrieNode<K, V> remove(int hash, int currentOffset, Object keyToRemove) {
         return hash == hashCode ? doRemove(keyToRemove) : this;
      }

      @Override
      public Pair<TrieNode<K, V>, Boolean> put(int hash, int currentOffset, K newKey, V newValue) {
         if (hash == hashCode) {
            return doPut(newKey, newValue);
         }
         // TODO: this one's tricky -- may need to split inner node into a path of
         // intermediate nodes capped by leaf or inner-leaf node
         return null;
      }
   }

   /**
    * Computes the hash code for an object. No alternative hashing is used since collisions are
    * unlikely due to the full 32 bits of the hash code being used to store a mapping. This returns
    * zero for null keys or the key's {@linkplain #hashCode() hash code} for non-null keys.
    *
    * @param o the key
    * @return the hash code for the key, zero if the key is {@code null}
    */
   static int hash(Object o) {
      return o == null ? 0 : o.hashCode();
   }
   
   private static HamtPersistentMap<Object, Object> EMPTY_INSTANCE =
         new HamtPersistentMap<Object, Object>(0, null);
   
   @SuppressWarnings("unchecked") // safe due to immutability
   public static <K, V> HamtPersistentMap<K, V> create() {
      return (HamtPersistentMap<K, V>) EMPTY_INSTANCE;
   }
   
   public static <K, V> HamtPersistentMap<K, V> create(Map<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   public static <K, V> HamtPersistentMap<K, V> create(ImmutableMap<? extends K, ? extends V> map) {
      // TODO
      return null;
   }

   private final int size;
   private final TrieNode<K, V> root;
   
   private HamtPersistentMap(int size, TrieNode<K, V> root) {
      this.size = size;
      this.root = root;
   }
   
   @Override
   public Iterator<Entry<K, V>> iterator() {
      // TODO: implement me
      return null;
   }
   
   @Override
   public boolean containsKey(Object o) {
      ListNode<K, V> node = root == null ? null : root.findNode(hash(o), 0, o);
      return node != null;
   }

   @Override
   public boolean containsAllKeys(Iterable<?> keys) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsAnyKey(Iterable<?> keys) {
      // TODO: implement me
      return false;
   }

   @Override
   public V get(Object key) {
      ListNode<K, V> node = root == null ? null : root.findNode(hash(key), 0, key);
      return node == null ? null : node.value;
   }

   @Override
   public ImmutableSet<K> keySet() {
      // TODO: implement me
      return null;
   }

   @Override
   public ImmutableCollection<V> values() {
      // TODO: implement me
      return null;
   }

   @Override
   public ImmutableSet<Entry<K, V>> entrySet() {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> put(K key, V value) {
      if (root == null) {
         return new HamtPersistentMap<K, V>(1, new InnerLeafTrieNode<K, V>(key, value, hash(key)));
      }
      Pair<TrieNode<K, V>, Boolean> addition = root.put(hash(key), 0, key, value);
      TrieNode<K, V> newRoot = addition.getFirst();
      return newRoot == root ? this
            : new HamtPersistentMap<K, V>(addition.getSecond() ? size + 1 : size, newRoot);
   }

   @Override
   public PersistentMap<K, V> remove(Object o) {
      if (root == null) {
         return this;
      }
      TrieNode<K, V> newRoot = root.remove(hash(o), 0, o);
      return newRoot == root ? this : new HamtPersistentMap<K, V>(size - 1, newRoot);
   }

   @Override
   public PersistentMap<K, V> removeAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> retainAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> putAll(Map<? extends K, ? extends V> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean isEmpty() {
      return size == 0;
   }
}
