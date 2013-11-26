package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An implementation of {@link Map} that uses a hash array-mapped trie (HAMT). Under the hood, this
 * structure is the same as an {@link ArrayMappedBitwiseTrie} except that the key bits come from a
 * key's {@linkplain Object#hashCode() hash value}. Like a {@link java.util.HashMap HashMap}, a
 * linked list is used to store values whose keys' hash values collide. But collisions are far less
 * likely than in a {@link java.util.HashMap HashMap} because the full 32 bits of hash value are
 * used (vs. hash value modulo array size, which is typically far lower cardinality than the full
 * 32 bits).
 * 
 * <p>Since this structure doesn't use a fixed size array for the mappings, it never pays a penalty
 * for resizing internal structures and re-hashing all of its contents (unlike a
 * {@link java.util.HashMap HashMap}). Also, since the full 32 bits of hash code are used to store
 * entries, no alternative hashing is needed.
 * 
 * <p>A {@link HamtMap} supports both null keys and values, and it supports all optional operations
 * in the {@link Map} interface. This implementation is not thread-safe and its iterators will
 * attempt to fail fast, by throwing a {@link ConcurrentModificationException} if improper
 * concurrency is detected.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of value in the map
 */
// TODO: add InnerLeafTrieNode to short circuit search when a path has only one node
// TODO: add freeLists for more efficiently re-using/allocating node arrays
public class HamtMap<K, V> extends AbstractMap<K, V> implements Serializable, Cloneable {
   
   private static final long serialVersionUID = -9064441005458513893L;

   /**
    * Sentinel value returned from {@link TrieNode#findOrAddNode(int, int, Object, Object)} to
    * indicate that an inner-leaf node must be replaced with an intermediate node.
    */
   private static final ListNode<Object, Object> INNER_NODE_NEEDS_EXPANSION =
         new ListNode<Object, Object>(null, null);
         
   /**
    * A node in a linked list that stores key-value pairs for a given hash code.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListNode<K, V> {
      /**
       * The key.
       */
      K key;
      
      /**
       * The value.
       */
      V value;
      
      /**
       * A pointer to the next entry in the list.
       */
      ListNode<K, V> next;

      /**
       * Constructs a new list node.
       *
       * @param key the key
       * @param value the value
       */
      ListNode(K key, V value) {
         this.key = key;
         this.value = value;
      }
   }
   
   /**
    * A node in the trie.
    *
    * @param <K> the type of the keys
    * @param <V> the type of the values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private interface TrieNode<K, V> {
      // APIs need to be in terms of hash and offset instead of hash remaining and bits remaining
      // so the InnerLeafTrieNode can be properly implemented
      
      /**
       * Determines if this node or any of its descendants contain the specified value.
       *
       * @param value the value
       * @return true if the value is contained in the trie rooted at this node
       */
      boolean containsValue(Object value);
      
      /**
       * Finds a node for the specified key. Each level of the tree represents six (out of 32) bits
       * from the key's hash code. 
       *
       * @param hashCodeRemaining the remaining hash code for the key, shifted so that prior bits
       *       (for higher levels in the trie) are absent so that the number of bits remaining are
       *       the least significant bits and more significant bits are all zero
       * @param bitsRemaining the number of bits remaining in the hash code, 32 at the root node
       *       but only two at the deepest level
       * @param key the key to find
       * @return the list node containing the key or {@code null} if the key is not found
       */
      ListNode<K, V> findNode(int hashCodeRemaining, int bitsRemaining, Object key);
      
      /**
       * Finds or creates a node for the specified key and value. This is nearly the same as
       * {@link #findNode(int, int, Object)} except that a node is added to the trie with the
       * specified value if not found. In the event that a node is not found (so one is added) this
       * method still returns {@code null}, indicating that it didn't previously exist.
       *
       * @param hashCodeRemaining the remaining hash code for the key, shifted so that prior bits
       *       (for higher levels in the trie) are absent so that the number of bits remaining are
       *       the least significant bits and more significant bits are all zero
       * @param bitsRemaining the number of bits remaining in the hash code, 32 at the root node
       *       but only two at the deepest level
       * @param key the key to find
       * @param value the value to associate with this key if not found and a new mapping is created
       * @return the list node containing the key or {@code null} if the key was not found and a
       *       new node was added
       */
      ListNode<K, V> findOrAddNode(int hashCodeRemaining, int bitsRemaining, K key, V value);
      
      /**
       * Removes a node from the trie.
       *
       * @param hashCodeRemaining the remaining hash code for the key, shifted so that prior bits
       *       (for higher levels in the trie) are absent so that the number of bits remaining are
       *       the least significant bits and more significant bits are all zero
       * @param bitsRemaining the number of bits remaining in the hash code, 32 at the root node
       *       but only two at the deepest level
       * @param key the key to remove
       * @return the list node that was removed from the trie or {@code null} if the key was not
       *       found
       */
      ListNode<K, V> removeNode(int hashCodeRemaining, int bitsRemaining, Object key);
      
      /**
       * Determines if this node has any mappings.
       *
       * @return true if this node (or any of its descendants) has at least one mapping; false
       *       otherwise
       */
      boolean isEmpty();
   }
   
   /**
    * A non-leaf node in the trie. Each such level has an array of child nodes, each one
    * representing a different combination of bits. Each such level encodes six bits of the keys'
    * 32-bit hash codes, except the last level which only encodes two bits. So each level can have
    * up to 64 child nodes (up to 4 children at the last level).
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class IntermediateTrieNode<K, V> implements TrieNode<K, V> {
      /**
       * A bitmask for which children are present. Each child represents a different combination of
       * six bits, up to 64 possible children (so one bit in this mask for each possible child).
       */
      long present;
      
      /**
       * The array of children. Its size is always the number of actual children, so it gets
       * expanded and shrunk during add and remove operations.
       */
      TrieNode<K, V> children[];
      
      /**
       * Constructs a new non-leaf node that has a single child.
       *
       * @param mask the bitmask which has exactly one set bit, the position of which indicates
       *       while child is present 
       * @param firstChild the single child that is present
       */
      IntermediateTrieNode(long mask, TrieNode<K, V> firstChild) {
         assert Long.bitCount(mask) == 1;
         assert firstChild != null;
         present = mask;
         children = createChildren(1);
         children[0] = firstChild;
      }
      
      /**
       * Creates an array of child nodes.
       *
       * @param size the size of the array
       * @return an array
       */
      @SuppressWarnings("unchecked") // can't create generic array, so unchecked cast from raw
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
      public ListNode<K, V> findNode(int hashCodeRemaining, int bitsRemaining, Object key) {
         assert bitsRemaining > 0;
         
         int significantBits = hashCodeRemaining & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) != 0) {
            int index = Long.bitCount((mask - 1) & present);
            return children[index].findNode(hashCodeRemaining >> 6, bitsRemaining - 6, key);
         } else {
            return null;
         }
      }
      
      @Override
      public ListNode<K, V> findOrAddNode(int hashCodeRemaining, int bitsRemaining, K key,
            V value) {
         assert bitsRemaining > 0;

         int significantBits = hashCodeRemaining & 0x3f;
         long mask = 1L << significantBits;
         int index = Long.bitCount((mask - 1) & present);
         hashCodeRemaining >>= 6;
         bitsRemaining -= 6;
         if ((present & mask) != 0) {
            return children[index].findOrAddNode(hashCodeRemaining, bitsRemaining, key, value);
         }
         
         // not found, so we add it
         TrieNode<K, V> child = createNode(hashCodeRemaining, bitsRemaining, key, value);
         int numChildrenBefore = children.length;
         @SuppressWarnings("unchecked") // can't create generic array, so unchecked cast from raw
         TrieNode<K, V> newChildren[] = new TrieNode[numChildrenBefore + 1];
         if (index > 0) {
            System.arraycopy(children, 0, newChildren, 0, index);
         }
         newChildren[index] = child;
         int leftOver = numChildrenBefore - index;
         if (leftOver > 0) {
            System.arraycopy(children, index, newChildren, index + 1, leftOver);
         }
         children = newChildren;
         present |= mask;
         return null;
      }
      
      @Override
      public ListNode<K, V> removeNode(int hashCodeRemaining, int bitsRemaining, Object key) {
         assert bitsRemaining > 0;
         
         int significantBits = hashCodeRemaining & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            return null;
         }
         
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> child = children[index];
         ListNode<K, V> ret = child.removeNode(hashCodeRemaining >> 6, bitsRemaining - 6, key);
         if (ret == null || (ret != child && !child.isEmpty())) {
            return ret;
         }
         present &= ~mask;
         if (present == 0) {
            assert index == 0;
            // don't bother creating a zero-element array - just clear ref and exit
            children[index] = null;
            return ret;
         }
         // shrink the array and remove this child from it
         int numChildrenBefore = children.length;
         @SuppressWarnings("unchecked") // can't create generic array, so unchecked cast from raw
         TrieNode<K, V> newChildren[] = new TrieNode[numChildrenBefore - 1];
         if (index > 0) {
            System.arraycopy(children, 0, newChildren, 0, index);
         }
         int leftOver = numChildrenBefore - index - 1;
         if (leftOver > 0) {
            System.arraycopy(children, index + 1, newChildren, index, leftOver);
         }
         children = newChildren;
         return ret;
      }
      
      @Override
      public boolean isEmpty() {
         return present == 0;
      }
   }
   
   /**
    * A leaf node in the trie. Leaves represent a full 32-bit hash code. Since multiple objects
    * could hash to the same 32-bit code, there could be collisions. Such collisions are managed
    * using a linked list of all keys that share the same hash code, so each leaf node is also the
    * head of a list.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class LeafTrieNode<K, V> extends ListNode<K, V> implements TrieNode<K, V> {
      /**
       * Constructs a new leaf node for the specified mapping. 
       *
       * @param key the key
       * @param value the value
       */
      LeafTrieNode(K key, V value) {
         super(key, value);
      }

      @Override
      public boolean containsValue(Object searchValue) {
         ListNode<K, V> current = this;
         while (current != null) {
            if (searchValue == null ? current.value == null : searchValue.equals(current.value)) {
               return true;
            }
            current = current.next;
         }
         return false;
      }
      
      @Override
      public ListNode<K, V> findNode(int hashCodeRemaining, int bitsRemaining, Object searchKey) {
         assert hashCodeRemaining == 0 && bitsRemaining == -4;
         return doFindNode(searchKey);
      }
      
      protected ListNode<K, V> doFindNode(Object searchKey) {
         ListNode<K, V> current = this;
         while (current != null) {
            if (searchKey == null ? current.key == null : searchKey.equals(current.key)) {
               return current;
            }
            current = current.next;
         }
         return null;
      }

      @Override
      public ListNode<K, V> findOrAddNode(int hashCodeRemaining, int bitsRemaining, K newKey,
            V newValue) {
         assert hashCodeRemaining == 0 && bitsRemaining == -4;
         return doFindOrAddNode(newKey, newValue);
      }
      
      protected ListNode<K, V> doFindOrAddNode(K newKey, V newValue) {
         ListNode<K, V> current = this;
         while (current != null) {
            if (newKey == null ? current.key == null : newKey.equals(current.key)) {
               return current;
            }
            current = current.next;
         }
         // not found, so add it
         ListNode<K, V> newNode = new ListNode<K, V>(newKey, newValue);
         newNode = this.next;
         this.next = newNode;
         return null;
      }

      @Override
      public ListNode<K, V> removeNode(int hashCodeRemaining, int bitsRemaining,
            Object keyToRemove) {
         assert hashCodeRemaining == 0 && bitsRemaining == -4;
         return doRemoveNode(keyToRemove);
      }
      
      protected ListNode<K, V> doRemoveNode(Object keyToRemove) {
         if (keyToRemove == null ? this.key == null : keyToRemove.equals(this.key)) {
            if (next == null) {
               // no other list nodes, so just remove this whole thing
               return this;
            }
            // otherwise, swap with successor and remove it
            K tmpK = this.key;
            V tmpV = this.value;
            this.key = next.key;
            this.value = next.value;
            next.key = tmpK;
            next.value = tmpV;
            ListNode<K, V> ret = next;
            next = next.next;
            return ret;
         }
         
         // find node in linked list
         ListNode<K, V> current = this;
         while (current.next != null) {
            ListNode<K, V> node = current.next;
            if (keyToRemove == null ? node.key == null : keyToRemove.equals(node.key)) {
               // remove found node
               current.next = node.next;
               return node;
            }
            current = current.next;
         }
         
         // not found
         return null;
      }

      @Override
      public boolean isEmpty() {
         return false;
      }
   }
   
   // TODO: javadoc
   private static class InnerLeafTrieNode<K, V> extends LeafTrieNode<K, V> {
      private final int hashCode;
      
      InnerLeafTrieNode(int hashCode, K key, V value) {
         super(key, value);
         this.hashCode = hashCode;
      }
   }
   
   /**
    * Creates a path of trie nodes for the specified key. If less than 32 bits of the hash code
    * remain, then this will not be a full path. It will be just long enough to get from the
    * current level of the trie (implied by the number of bits remaining) to the leaf that will
    * contain the new mapping.
    *
    * @param hashCodeRemaining the remaining hash code for the key, shifted so that prior bits
    *       (for higher levels in the trie) are absent so that the number of bits remaining are
    *       the least significant bits and more significant bits are all zero
    * @param bitsRemaining the number of bits remaining in the hash code, 32 at the root node
    *       but only two at the deepest level
    * @param key the key to find
    * @param value the value to associate with this key if not found and a new mapping is created
    * @return the newly created trie node (which may have a line of new descendants)
    */
   static <K, V> TrieNode<K, V> createNode(int hashCodeRemaining, int bitsRemaining,
         K key, V value) {
      if (bitsRemaining > 0) {
         int significantBits = hashCodeRemaining & 0x3f;
         long mask = 1L << significantBits;
         hashCodeRemaining >>= 6;
         bitsRemaining -= 6;
         return new IntermediateTrieNode<K, V>(mask, createNode(hashCodeRemaining, bitsRemaining,
               key, value));
      } else {
         return new LeafTrieNode<K, V>(key, value);
      }
   }
   
   /**
    * A counter for modifications, used to detect concurrent modification bugs.
    */
   transient int modCount;
   
   /**
    * The number of mappings in the trie.
    */
   transient int size;
   
   /**
    * The root node of the trie ({@code null} if the trie is empty).
    */
   transient IntermediateTrieNode<K, V> root;
   
   /**
    * Constructs a new, empty map.
    */
   public HamtMap() {
   }
   
   /**
    * Constructs a new map populated with the mappings from the specified map.
    * 
    * @param map mappings used to initialize this map
    */
   public HamtMap(Map<? extends K, ? extends V> map) {
      putAll(map);
   }
   
   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean isEmpty() {
      return size == 0;
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
   
   @Override
   public boolean containsKey(Object key) {
      if (root == null) {
         return false;
      }
      ListNode<K, V> node = root.findNode(hash(key), 32, key);
      return node != null;
   }

   @Override
   public boolean containsValue(Object value) {
      // a little more efficient than using EntrySet's iterator: we can just depth-first search
      // the trie recursively
      return root == null ? false : root.containsValue(value);
   }

   @Override
   public V get(Object key) {
      if (root == null) {
         return null;
      }
      ListNode<K, V> node = root.findNode(hash(key), 32, key);
      return node == null ? null : node.value;
   }

   @Override
   public V put(K key, V value) {
      if (root == null) {
         root = (IntermediateTrieNode<K, V>) createNode(hash(key), 32, key, value);
         modCount++;
         size++;
         return null;
      }
      ListNode<K, V> existing = root.findOrAddNode(hash(key), 32, key, value);
      if (existing != null) {
         V ret = existing.value;
         existing.value = value;
         return ret;
      }
      modCount++;
      size++;
      return null;
   }

   @Override
   public V remove(Object key) {
      if (root == null) {
         return null;
      }
      ListNode<K, V> removed = root.removeNode(hash(key), 32, key);
      if (removed != null) {
         modCount++;
         if (--size == 0) {
            assert root.isEmpty();
            root = null;
         }
         return removed.value;
      }
      return null;
   }

   @Override
   public void clear() {
      modCount++;
      size = 0;
      root = null;
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new EntrySet();
   }
   
   /**
    * Returns a view of a {@link ListNode} mapping as a {@link Map.Entry}.
    *
    * @param node the list node
    * @return a view of the node as a {@link Map.Entry}
    */
   Entry<K, V> entry(final ListNode<K, V> node) {
      return new Entry<K, V>() {
         private ListNode<K, V> listNode = node;
         private int myModCount = modCount; 
         
         @Override
         public K getKey() {
            return listNode.key;
         }

         @Override
         public V getValue() {
            return listNode.value;
         }

         @Override
         public V setValue(V value) {
            V ret = listNode.value;
            if (modCount != myModCount) {
               if (root == null) {
                  throw new ConcurrentModificationException();
               }
               listNode = root.findNode(hash(listNode.key), 32, listNode.key);
               if (listNode == null) {
                  throw new ConcurrentModificationException();
               }
            }
            listNode.value = value;
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
      };
   }
   
   @Override
   public HamtMap<K, V> clone() {
      if (this.getClass() == HamtMap.class) {
         return new HamtMap<K, V>(this);
      } else {
         try {
            @SuppressWarnings("unchecked")
            HamtMap<K, V> clone = (HamtMap<K, V>) super.clone();
            clone.clear();
            clone.putAll(this);
            return clone;
            
         } catch (CloneNotSupportedException e) {
            // we implement Cloneable, so this shouldn't be possible
            throw new AssertionError();
         }
      }
   }
   
   /**
    * Customizes de-serialization to read list of mappings the same way as written by
    * {@link #writeObject(ObjectOutputStream)}.
    * 
    * @param in the stream from which the map is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   @SuppressWarnings("unchecked") // serialization of generic types is inherently unchecked
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      int sz = in.readInt();
      for (int i = 0; i < sz; i++) {
         K key = (K) in.readObject();
         V value = (V) in.readObject();
         put(key, value);
      }
   }
   
   /**
    * Customizes serialization by just writing the mappings as a sequence of key, value, key, value,
    * etc.
    * 
    * @param out the stream to which to serialize this map
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeInt(size);
      for (Entry<K, V> entry : entrySet()) {
         out.writeObject(entry.getKey());
         out.writeObject(entry.getValue());
      }
   }
   
   /**
    * The set of entries in the map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class EntrySet extends AbstractSet<Entry<K, V>> {
      EntrySet() {
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         return new EntryIterator();
      }
      
      @Override
      public boolean contains(Object o) {
         if (root != null && o instanceof Entry) {
            Entry<?, ?> entry = (Entry<?, ?>) o;
            Object key = entry.getKey();
            Object value = entry.getValue();
            ListNode<K, V> node = root.findNode(hash(key), 32, key);
            return node != null && (value == null ? node.value == null : value.equals(node.value));
         }
         return false;
      }
      
      @Override
      public void clear() {
         HamtMap.this.clear();
      }

      @Override
      public boolean remove(Object o) {
         if (root != null && o instanceof Entry) {
            Entry<?, ?> entry = (Entry<?, ?>) o;
            Object key = entry.getKey();
            Object value = entry.getValue();
            ListNode<K, V> node = root.findNode(hash(key), 32, key);
            if (node == null || !(value == null ? node.value == null : value.equals(node.value))) {
               // mapping not present or it has the wrong value
               return false;
            } else {
               HamtMap.this.remove(key);
               return true;
            }
         }
         return false;
      }

      @Override
      public int size() {
         return size;
      }
   }
   
   /**
    * A stack frame, used to iterate over the tree without using recursion.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class StackFrame<K, V> {
      /**
       * A node that is visited during iteration.
       */
      IntermediateTrieNode<K, V> node;
      
      /**
       * The current/latest child visited during iteration. The next frame on the stack will hold
       * a reference to the child.
       */
      int childIndex;
      
      StackFrame(IntermediateTrieNode<K, V> node) {
         this(node, 0);
      }
      
      StackFrame(IntermediateTrieNode<K, V> node, int childIndex) {
         this.node = node;
         this.childIndex = childIndex;
      }
   }
   
   /**
    * An iterator over the mappings in the trie.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class EntryIterator implements Iterator<Entry<K, V>> {
      /**
       * The stack, used for doing a depth-first traversal without recursion.
       */
      private final ArrayDeque<StackFrame<K, V>> stack;
      
      /**
       * The next mapping to be returned by this iterator.
       */
      private ListNode<K, V> next;
      
      /**
       * The key from the mapping last fetched by this iterator.
       */
      private K lastFetched;
      
      /**
       * The state of this iterator, for determining if {@link #remove()} is valid.
       */
      private IteratorModifiedState modState;
      
      /**
       * The modification count of the trie to which this iterator is pinned. If the trie is
       * structurally modified (such that the trie's mod count differs from this value) then we've
       * detected a concurrent modification.
       */
      private int myModCount;
      
      EntryIterator() {
         if (root == null) {
            stack = null;
            next = null;
         } else {
            stack = new ArrayDeque<StackFrame<K, V>>(6);
            TrieNode<K, V> node = root;
            for (int i = 0; i < 6; i++) {
               IntermediateTrieNode<K, V> iNode = (IntermediateTrieNode<K, V>) node; 
               stack.push(new StackFrame<K, V>(iNode));
               node = iNode.children[0];
            }
            next = (LeafTrieNode<K, V>) node;
         }
         myModCount = modCount;
      }
      
      private void checkModCount() {
         if (modCount != myModCount) {
            throw new ConcurrentModificationException();
         }
      }

      @Override
      public boolean hasNext() {
         checkModCount();
         return next != null;
      }
      
      @Override
      public Entry<K, V> next() {
         checkModCount();
         if (next == null) {
            throw new NoSuchElementException();
         }
         ListNode<K, V> ret = next;
         lastFetched = ret.key;
         if (next.next != null) {
            next = next.next;
         } else {
            while (true) {
               if (stack.isEmpty()) {
                  next = null;
                  break;
               }
               StackFrame<K, V> frame = stack.pop();
               if (++frame.childIndex < frame.node.children.length) {
                  stack.push(frame);
                  TrieNode<K, V> node = frame.node.children[frame.childIndex];
                  for (int i = stack.size(); i < 6; i++) {
                     IntermediateTrieNode<K, V> iNode = (IntermediateTrieNode<K, V>) node; 
                     stack.push(new StackFrame<K, V>(iNode));
                     node = iNode.children[0];
                  }
                  next = (LeafTrieNode<K, V>) node;
                  break;
               }
            }
         }
         modState = IteratorModifiedState.NONE;
         return entry(ret);
      }

      @Override
      public void remove() {
         checkModCount();
         if (modState != IteratorModifiedState.NONE) {
            throw new IllegalStateException();
         }
         modState = IteratorModifiedState.REMOVED;
         K nextKey = next == null ? null : next.key;
         HamtMap.this.remove(lastFetched);
         myModCount = modCount;
         if (next != null) {
            seekTo(nextKey);
         }
      }
      
      /**
       * Populates the stack with the path from root to the specified key.
       *
       * @param key the next key to be fetched by this iterator
       */
      private void seekTo(K key) {
         stack.clear();
         if (root == null) {
            next = null;
            return;
         }
         // navigate from root to leaf to reset the stack (since removal could have shifted any or
         // all child indices already in the stack)
         int hashCodeRemaining = hash(key);
         int bitsRemaining = 32;
         TrieNode<K, V> node = root;
         while (bitsRemaining > 0) {
            IntermediateTrieNode<K, V> iNode = (IntermediateTrieNode<K, V>) node; 
            int significantBits = hashCodeRemaining & 0x3f;
            long mask = 1L << significantBits;
            int index = Long.bitCount((mask - 1) & iNode.present);
            stack.push(new StackFrame<K, V>(iNode, index));
            node = iNode.children[index];
            hashCodeRemaining >>= 6;
            bitsRemaining -= 6;
         }
         next = (LeafTrieNode<K, V>) node;
         while (next != null) {
            if (key == null ? next.key == null : key.equals(next.key)) {
               // found it
               return;
            }
            next = next.next;
         }
         // we should never get here - should only be possible if the key was concurrently
         // removed from the map
         throw new ConcurrentModificationException();
      }
   }
}
