package com.apriori.collections;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

// TODO: javadoc
// TODO: tests
public class HamtPersistentMap<K, V> extends AbstractImmutableMap<K, V>
      implements PersistentMap<K, V> {

   private static class ListNode<K, V> implements Entry<K, V> {
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

      @Override
      public K key() {
         return key;
      }

      @Override
      public V value() {
         return value;
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
   
   private static class PutResult<K, V> {
      PutResult() {
      }
      
      TrieNode<K, V> node;
      boolean added;
   }
   
   private interface TrieNode<K, V> {
      boolean containsValue(Object value);
      ListNode<K, V> findNode(int hash, int currentOffset, Object key);
      TrieNode<K, V> remove(int hash, int currentOffset, Object key);
      void put(int hash, int currentOffset, K key, V value, PutResult<K, V> result);
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
      static <K, V> TrieNode<K, V>[] createChildren(int size) {
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
            // TODO: use InnerLeafTrieNode if only one child and it's a LeafTrieNode
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
      public void put(int hash, int currentOffset, K key, V value, PutResult<K, V> result) {
         int significantBits = (hash >> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            // add new child node for this new entry
            long newPresent = present | mask;
            int index = Long.bitCount((mask - 1) & newPresent);
            int length = children.length;
            TrieNode<K, V> newChildren[] = createChildren(length + 1);
            if (index > 0) {
               System.arraycopy(children, 0, newChildren, 0, index);
            }
            if (index < length) {
               System.arraycopy(children, index + 1, newChildren, 0, length - index);
            }
            if (currentOffset + 6 >= 32) {
               newChildren[index] = new LeafTrieNode<K, V>(key, value);
            } else {
               newChildren[index] = new InnerLeafTrieNode<K, V>(key, value, hash);
            }
            result.node = new IntermediateTrieNode<K, V>(newPresent, newChildren);
            result.added = true;
            return;
         }
         
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> oldChild = children[index];
         oldChild.put(hash, currentOffset + 6, key, value, result);
         TrieNode<K, V> newChild = result.node;
         if (newChild == oldChild) {
            // no change
            assert !result.added;
            result.node = this;
            result.added = false;
            return;
         }
         int length = children.length;
         TrieNode<K, V> newChildren[] = createChildren(length);
         System.arraycopy(children, 0, newChildren, 0, length);
         newChildren[index] = newChild;
         result.node = new IntermediateTrieNode<K, V>(present, newChildren); 
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
         for (ListNode<K, V> current = this; current != null; current = current.next) {
            if (searchValue == null ? current.value == null : searchValue.equals(current.value)) {
               return true;
            }
         }
         return false;
      }

      @Override
      public ListNode<K, V> findNode(int hash, int currentOffset, Object searchKey) {
         assert currentOffset >= 32;
         return doFind(key);
      }
      
      ListNode<K, V> doFind(Object searchKey) {
         for (ListNode<K, V> current = this; current != null; current = current.next) {
            if (searchKey == null ? current.key == null : searchKey.equals(current.key)) {
               return current;
            }
         }
         return null;
      }

      @Override
      public TrieNode<K, V> remove(int hash, int currentOffset, Object keyToRemove) {
         assert currentOffset >= 32;
         return doRemove(keyToRemove);
      }
      
      TrieNode<K, V> create(K k, V v, ListNode<K, V> n) {
         return new LeafTrieNode<K, V>(k, v, n);
      }
      
      TrieNode<K, V> doRemove(Object keyToRemove) {
         ArrayDeque<ListNode<K, V>> stack = new ArrayDeque<ListNode<K, V>>();
         for (ListNode<K, V> current = this; current != null; current = current.next) {
            if (keyToRemove == null ? current.key == null : keyToRemove.equals(current.key)) {
               if (stack.isEmpty()) {
                  // removing the initial leaf node -- just create a new head ListNode that is
                  // also a TrieNode and keep the rest
                  return create(next.key, next.value, next.next);
               }
               // rebuild path from leaf node to here and keep the rest of the list
               while (stack.isEmpty()) {
                  ListNode<K, V> node = stack.pop();
                  if (stack.isEmpty()) {
                     return create(node.key, node.value, current.next);
                  }
                  current = new ListNode<K, V>(node.key, node.value, current.next); 
               }
            }
            stack.push(current);
         }
         return null;
      }

      @Override
      public void put(int hash, int currentOffset, K newKey, V newValue, PutResult<K, V> result) {
         assert currentOffset >= 32;
         doPut(newKey, newValue, result);
         return;
      }
      
      void doPut(K newKey, V newValue, PutResult<K, V> result) {
         ArrayDeque<ListNode<K, V>> stack = new ArrayDeque<ListNode<K, V>>();
         for (ListNode<K, V> current = this; current != null; current = current.next) {
            if (newKey == null ? current.key == null : newKey.equals(current.key)) {
               if (stack.isEmpty()) {
                  // changing the initial leaf node -- just create a new head ListNode with new
                  // value and keep the rest
                  result.node = create(newKey, newValue, next);
                  result.added = false;
                  return;
               }
               // rebuild path from leaf node to here and keep the rest of the list
               current = new ListNode<K, V>(newKey, newValue, current.next); 
               while (stack.isEmpty()) {
                  ListNode<K, V> node = stack.pop();
                  if (stack.isEmpty()) {
                     result.node = create(node.key, node.value, current);
                     result.added = false;
                     return;
                  }
                  current = new ListNode<K, V>(node.key, node.value, current);
               }
            }
            stack.push(current);
         }
         // adding new value - push to head of list
         result.node = create(newKey, newValue, new ListNode<K, V>(key, value, next));
         result.added = true;
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
      public void put(int hash, int currentOffset, K newKey, V newValue, PutResult<K, V> result) {
         if (hash == hashCode) {
            doPut(newKey, newValue, result);
         } else {
            result.node = create(hash, currentOffset, newKey, newValue);
            result.added = true;
         }
      }
      
      private TrieNode<K, V> create(int hash, int currentOffset, K newKey, V newValue) {
         int significantBits1 = (hashCode >> currentOffset) & 0x3f;
         int significantBits2 = (hash >> currentOffset) & 0x3f;

         if (significantBits1 == significantBits2) {
            return new IntermediateTrieNode<K, V>(1L << significantBits1,
                  create(hash, currentOffset + 6, newKey, newValue));
         }
         
         long mask = (1L << significantBits1) | (1L << significantBits2);
         TrieNode<K, V> current = new InnerLeafTrieNode<K, V>(key, value, next, hashCode);
         TrieNode<K, V> addition = new InnerLeafTrieNode<K, V>(newKey, newValue, hash);
         TrieNode<K, V> children[] = IntermediateTrieNode.createChildren(2);
         if (significantBits1 < significantBits2) {
            children[0] = current;
            children[1] = addition;
         } else {
            children[0] = addition;
            children[1] = current;
         }
         return new IntermediateTrieNode<K, V>(mask, children);
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
      return HamtPersistentMap.<K, V>create().merge(map);
   }

   @SuppressWarnings("unchecked") // due to immutability, cast is safe
   public static <K, V> HamtPersistentMap<K, V> create(ImmutableMap<? extends K, ? extends V> map) {
      if (map instanceof HamtPersistentMap) {
         return (HamtPersistentMap<K, V>) map;
      }
      return HamtPersistentMap.<K, V>create().merge(map);
   }
   
   

   private final int size;
   private final TrieNode<K, V> root;
   
   private HamtPersistentMap(int size, TrieNode<K, V> root) {
      this.size = size;
      this.root = root;
   }
   
   @Override
   public Iterator<Entry<K, V>> iterator() {
      return new Iter();
   }
   
   @Override
   public boolean containsKey(Object o) {
      ListNode<K, V> node = root == null ? null : root.findNode(hash(o), 0, o);
      return node != null;
   }

   @Override
   public boolean containsValue(Object o) {
      return root == null ? false : root.containsValue(o);
   }

   @Override
   public V get(Object key) {
      ListNode<K, V> node = root == null ? null : root.findNode(hash(key), 0, key);
      return node == null ? null : node.value;
   }

   @Override
   public PersistentMap<K, V> put(K key, V value) {
      return add(key, value);
   }

   private HamtPersistentMap<K, V> add(K key, V value) {
      if (root == null) {
         return new HamtPersistentMap<K, V>(1, new InnerLeafTrieNode<K, V>(key, value, hash(key)));
      }
      PutResult<K, V> result = new PutResult<K, V>();
      root.put(hash(key), 0, key, value, result);
      return result.node == root ? this
            : new HamtPersistentMap<K, V>(result.added ? size + 1 : size, result.node);
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
   public PersistentMap<K, V> removeAll(Iterable<?> keys) {
      if (isEmpty()) {
         return this;
      }
      // TODO: bulk remove? maybe create array sorted by hash code and then use bulk operations
      // to remove chunks from TrieNodes?
      PersistentMap<K, V> ret = this;
      for (Object key : keys) {
         ret = ret.remove(key);
      }
      return ret;
   }

   @Override
   public PersistentMap<K, V> retainAll(Iterable<?> keys) {
      if (isEmpty()) {
         return this;
      }
      // TODO: bulk remove? maybe create array sorted by hash code and then use bulk operations
      // to remove chunks from TrieNodes?
      PersistentMap<K, V> ret = create();
      for (Object key : keys) {
         ListNode<K, V> node = root == null ? null : root.findNode(hash(key), 0, key);
         if (node != null) {
            ret = ret.put(node.key, node.value);
         }
      }
      return ret;
   }

   @Override
   public PersistentMap<K, V> putAll(Map<? extends K, ? extends V> items) {
      return merge(items);
   }
   
   private HamtPersistentMap<K, V> merge(Map<? extends K, ? extends V> items) {
      // TODO: bulk insert? maybe create array of entries sorted by hash code and then use bulk
      // operations to insert chunks into TrieNodes?
      HamtPersistentMap<K, V> ret = this;
      for (Map.Entry<? extends K, ? extends V> entry : items.entrySet()) {
         ret = ret.add(entry.getKey(), entry.getValue());
      }
      return ret;
   }

   @Override
   public PersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items) {
      return merge(items);
   }

   private HamtPersistentMap<K, V> merge(ImmutableMap<? extends K, ? extends V> items) {
      // TODO: bulk insert? maybe create array of entries sorted by hash code and then use bulk
      // operations to insert chunks into TrieNodes?
      HamtPersistentMap<K, V> ret = this;
      for (Entry<? extends K, ? extends V> entry : items) {
         ret = ret.add(entry.key(), entry.value());
      }
      return ret;
   }

   @Override
   public int size() {
      return size;
   }
   
   private static class StackFrame<K, V> {
      final IntermediateTrieNode<K, V> node;
      int childIndex;
      
      StackFrame(IntermediateTrieNode<K, V> node) {
         this.node = node;
         childIndex = 0;
      }
   }
   
   private class Iter implements Iterator<Entry<K, V>> {
      private final ArrayDeque<StackFrame<K, V>> stack = new ArrayDeque<StackFrame<K, V>>();
      private ListNode<K, V> current;
      
      @SuppressWarnings({ "unchecked", "synthetic-access" })
      Iter() {
         if (root == null) {
            current = null;
         } else {
            TrieNode<K, V> node = root;
            while (node instanceof IntermediateTrieNode) {
               IntermediateTrieNode<K, V> inode = (IntermediateTrieNode<K, V>) node;
               stack.push(new StackFrame<K, V>(inode));
               node = inode.children[0];
            }
            current = (ListNode<K, V>) node;
         }
      }
      
      @Override
      public boolean hasNext() {
         return current != null;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Entry<K, V> next() {
         if (current == null) {
            throw new NoSuchElementException();
         }
         Entry<K, V> ret = current;
         if (current.next != null) {
            current = current.next;
         } else {
            while (!stack.isEmpty()) {
               StackFrame<K, V> frame = stack.pop();
               if (++frame.childIndex < frame.node.children.length) {
                  stack.push(frame);
                  TrieNode<K, V> node = frame.node.children[frame.childIndex];
                  while (node instanceof IntermediateTrieNode) {
                     IntermediateTrieNode<K, V> inode = (IntermediateTrieNode<K, V>) node;
                     stack.push(new StackFrame<K, V>(inode));
                     node = inode.children[0];
                  }
                  current = (ListNode<K, V>) node;
               }
            }
            if (stack.isEmpty()) {
               // at the end
               current = null;
            }
         }
         return ret;
      }
   }
}
