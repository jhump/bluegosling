package com.apriori.collections;

import static java.util.Objects.requireNonNull;

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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * An implementation of {@link Map} that uses a hash array-mapped trie (HAMT). Under the hood, this
 * structure is the same as an {@link ArrayMappedBitwiseTrie} except that the key bits come from a
 * key's {@linkplain Object#hashCode() hash value}. Like a {@link java.util.HashMap HashMap}, a
 * linked list is used to store values whose keys' hash values collide. But collisions are far less
 * likely than in a {@link java.util.HashMap HashMap} because the full 32 bits of hash value are
 * used (vs. hash value modulo array size, which is typically far lower cardinality than the full
 * 32 bits).
 * 
 * <p>Each level of the trie contains information for 6 bits of the hash code and thus can have up
 * to 64 children. This means the trie can have a depth of up to six.
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
// TODO: add spliterator that is more efficient than default iterator-based one
// TODO: allocate arrays in trie nodes less frequently (e.g. allow them to not be full) so that
// mutations produce less garbage
// TODO: more efficient impls of Map default methods (putIfAbsent, computeIfAbsent, replace, etc)
public class HamtMap<K, V> extends AbstractMap<K, V> implements Serializable, Cloneable {
   
   private static final long serialVersionUID = -9064441005458513893L;

   /**
    * Sentinel value returned from {@link TrieNode#findOrAddNode(int, int, Object, Object)} to
    * indicate that an inner-leaf node must be replaced with an intermediate node.
    */
   static final ListNode<Object, Object> INNER_NODE_NEEDS_EXPANSION =
         new ListNode<Object, Object>(null, null);
         
   /**
    * A node in a linked list that stores key-value pairs for a given hash code.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListNode<K, V> implements Cloneable {
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
      
      @Override
      protected ListNode<K, V> clone() {
         try {
            @SuppressWarnings("unchecked")
            ListNode<K, V> clone = (ListNode<K, V>) super.clone();
            // recursively clone the whole linked list
            if (next != null) {
               clone.next = next.clone();
            }
            return clone;
         } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // shouldn't be possible since this class is Cloneable
         }
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
       * @param hashCode the hash code for the key
       * @param currentOffset represents the number of bits of the hash code already processed
       * @param key the key to find
       * @return the list node containing the key or {@code null} if the key is not found
       */
      ListNode<K, V> findNode(int hashCode, int currentOffset, Object key);
      
      /**
       * Finds or creates a node for the specified key and value. This is nearly the same as
       * {@link #findNode(int, int, Object)} except that a node is added to the trie with the
       * specified value if not found.
       * 
       * <p>A non-null return value indicates that the specified key is already present in the map.
       * So if an existing entry is found, it is returned. Otherwise, an entry is created using the
       * specified value and {@code null} is returned.
       * 
       * <p>If the node is not found but <em>cannot</em> be added, then
       * {@link HamtMap#INNER_NODE_NEEDS_EXPANSION} is returned. This only happens when an
       * {@link InnerLeafTrieNode} needs to be expanded into an {@link IntermediateTrieNode} in
       * order to accommodate the new entry.
       *
       * @param hashCode the hash code for the key
       * @param currentOffset represents the number of bits of the hash code already processed
       * @param key the key to find
       * @param value the value to associate with this key if not found and a new mapping is created
       * @return the list node containing the key, {@code null} if the key was not found (and a
       *       new node was added), or {@link HamtMap#INNER_NODE_NEEDS_EXPANSION} if the key was not
       *       found and could not be added
       */
      ListNode<K, V> findOrAddNode(int hashCode, int currentOffset, K key, V value);
      
      /**
       * Removes a node from the trie.
       *
       * @param hashCode the hash code for the key
       * @param currentOffset represents the number of bits of the hash code already processed
       * @param key the key to remove
       * @return the list node that was removed from the trie or {@code null} if the key was not
       *       found
       */
      ListNode<K, V> removeNode(int hashCode, int currentOffset, Object key);

      /**
       * Tries to collapse this node into an {@link InnerLeafTrieNode}. This is possible if this
       * node has only one descendant. Ancestors of that one descendant, up to and including this
       * node, can be collapsed into a single node.
       *
       * @param hashCode a hash code that is known to exist under this trie node
       * @param currentOffset represents the number of bits of the hash code represented by this
       *       node's ancestors
       * @return a new {@link InnerLeafTrieNode} with the single descendant or {@code null} if this
       *       node could not be collapsed
       */
      InnerLeafTrieNode<K, V> tryCollapse(int hashCode, int currentOffset);
      
      /**
       * Determines if this node has any mappings.
       *
       * @return true if this node (or any of its descendants) has at least one mapping; false
       *       otherwise
       */
      boolean isEmpty();
      
      /**
       * Computes the depth of this trie. The depth is the maximum height of the tree, determined by
       * traversing each sub-trie until we find the deepest leaf node.
       *
       * @return the depth of this trie
       */
      int depth();
      
      /**
       * Clones this trie node by creating a deep copy that shares no references with this original.
       *
       * @return a deep copy of this node
       */
      TrieNode<K, V> clone();
      
      /**
       * Executes the given action for each mapping in this trie.
       *
       * @param action the action to execute
       */
      void forEach(Consumer<ListNode<K, V>> action);
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
   private static class IntermediateTrieNode<K, V> implements TrieNode<K, V>, Cloneable {
      
      private static final TrieNode<?, ?> EMPTY[] = new TrieNode<?, ?>[0];
      
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
      @SuppressWarnings("unchecked")
      private static <K, V> TrieNode<K, V>[] createChildren(int size) {
         return (TrieNode<K, V>[]) new TrieNode<?, ?>[size];
      }
      
      @SuppressWarnings("unchecked")
      private static <K, V> TrieNode<K, V>[] emptyChildren() {
         return (TrieNode<K, V>[]) EMPTY;
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
      public ListNode<K, V> findNode(int hashCode, int currentOffset, Object key) {
         assert currentOffset < 32;
         
         int significantBits = (hashCode >>> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) != 0) {
            int index = Long.bitCount((mask - 1) & present);
            return children[index].findNode(hashCode, currentOffset + 6, key);
         } else {
            return null;
         }
      }
      
      @Override
      public ListNode<K, V> findOrAddNode(int hashCode, int currentOffset, K key, V value) {
         assert currentOffset < 32;

         int significantBits = (hashCode >>> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         int index = Long.bitCount((mask - 1) & present);
         currentOffset += 6;
         if ((present & mask) != 0) {
            TrieNode<K, V> child = children[index];
            ListNode<K, V> ret = child.findOrAddNode(hashCode, currentOffset, key, value);
            if (ret == INNER_NODE_NEEDS_EXPANSION) {
               child = ((InnerLeafTrieNode<K, V>) child).expand(hashCode, currentOffset);
               children[index] = child;
               ret = child.findOrAddNode(hashCode, currentOffset, key, value);
               assert ret != INNER_NODE_NEEDS_EXPANSION;
            }
            return ret;
         }
         
         // not found, so we add it
         TrieNode<K, V> child = createNode(hashCode, currentOffset, key, value);
         int numChildrenBefore = children.length;
         TrieNode<K, V> newChildren[] = createChildren(numChildrenBefore + 1);
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
      public ListNode<K, V> removeNode(int hashCode, int currentOffset, Object key) {
         assert currentOffset < 32;
         
         int significantBits = (hashCode >>> currentOffset) & 0x3f;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            return null;
         }
         
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> child = children[index];
         ListNode<K, V> ret = child.removeNode(hashCode, currentOffset + 6, key);
         if (ret == null) {
            return null; // nothing removed
         }
         if (ret != child && !child.isEmpty()) {
            // not deleting this child, but maybe we need to collapse it into an InnerLeafTrieNode
            InnerLeafTrieNode<K, V> replacement = child.tryCollapse(hashCode, currentOffset + 6);
            if (replacement != null) {
               children[index] = replacement;
            }
            return ret;
         }
         present &= ~mask;
         if (present == 0) {
            assert index == 0;
            children = emptyChildren();
            return ret;
         }
         // shrink the array and remove this child from it
         int numChildrenBefore = children.length;
         TrieNode<K, V> newChildren[] = createChildren(numChildrenBefore - 1);
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
      public InnerLeafTrieNode<K, V> tryCollapse(int hashCode, int currentOffset) {
         if (children.length != 1) {
            return null;
         }
         TrieNode<K, V> child = children[0];
         if (child instanceof InnerLeafTrieNode) {
            // our only child is an inner-leaf node, so we can fold this node into it
            InnerLeafTrieNode<K, V> ret = (InnerLeafTrieNode<K, V>) child;
            // assertion verifies that the inner-leaf node's hash code is compatible
            assert (((1 << currentOffset) - 1) & hashCode)
                  == (((1 << currentOffset) - 1) & ret.hash); 
            return ret;
         } else if (child instanceof LeafTrieNode) {
            assert currentOffset == 30;
            int lowBits = (((1 << 30) - 1) & hashCode);
            assert Long.bitCount(present) == 1;
            // we need to compute the right hash code for the leaf based on 30 bits of the
            // supplied hash code and two bits from the position of this child
            int highBits = Long.numberOfTrailingZeros(present);
            assert highBits >= 0 && highBits <= 3;
            int hash = highBits << 30 + lowBits;
            LeafTrieNode<K, V> leaf = (LeafTrieNode<K, V>) child;
            // make an inner-leaf node that looks the same as the given leaf node
            InnerLeafTrieNode<K, V> ret = new InnerLeafTrieNode<>(hash, leaf.key, leaf.value);
            ret.next = leaf.next;
            return ret;
         } else {
            return null;
         }
      }
      
      @Override
      public boolean isEmpty() {
         return present == 0;
      }
      
      @Override
      public int depth() {
         int max = 0;
         for (TrieNode<K, V> child : children) {
            int d = child.depth();
            if (d > max) {
               max = d;
            }
         }
         return max + 1;
      }

      @Override
      public TrieNode<K, V> clone() {
         try {
            @SuppressWarnings("unchecked")
            IntermediateTrieNode<K, V> clone = (IntermediateTrieNode<K, V>) super.clone();
            clone.children = createChildren(children.length);
            for (int i = 0, len = children.length; i < len; i++) {
               clone.children[i] = children[i].clone();
            }
            return clone;
         } catch (CloneNotSupportedException e) {
            throw new AssertionError(e); // shouldn't be possible since this class is Cloneable
         }
      }
      
      @Override
      public void forEach(Consumer<ListNode<K, V>> action) {
         for (TrieNode<K, V> child : children) {
            child.forEach(action);
         }
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
      public ListNode<K, V> findNode(int hashCode, int currentOffset, Object searchKey) {
         assert currentOffset == 36;
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
      public ListNode<K, V> findOrAddNode(int hashCode, int currentOffset, K newKey, V newValue) {
         assert currentOffset == 36;
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
         newNode.next = this.next;
         this.next = newNode;
         return null;
      }

      @Override
      public ListNode<K, V> removeNode(int hashCode, int currentOffset, Object keyToRemove) {
         assert currentOffset == 36;
         return doRemoveNode(keyToRemove);
      }
      
      protected ListNode<K, V> doRemoveNode(Object keyToRemove) {
         if (Objects.equals(this.key, keyToRemove)) {
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
            ret.next = null;
            return ret;
         }
         
         // find node in linked list
         ListNode<K, V> predecessor = this;
         for (ListNode<K, V> node = next; node != null; predecessor = node, node = node.next) {
            if (Objects.equals(node.key, keyToRemove)) {
               // remove found node
               predecessor.next = node.next;
               node.next = null;
               return node;
            }
         }
         
         // not found
         return null;
      }

      @Override
      public InnerLeafTrieNode<K, V> tryCollapse(int hashCode, int currentOffset) {
         // nothing to collapse
         return null;
      }
      
      @Override
      public boolean isEmpty() {
         return false;
      }
      
      @Override
      public int depth() {
         return 1;
      }
         
      @Override
      public LeafTrieNode<K, V> clone() {
         return (LeafTrieNode<K, V>) super.clone();
      }
      
      @Override
      public void forEach(Consumer<ListNode<K, V>> action) {
         ListNode<K, V> node = this;
         while (node != null) {
            action.accept(node);
            node = node.next;
         }
      }
   }
   
   /**
    * An inner node in the trie, but with leaf information. This type of node is used to
    * "short-circuit" searches down a branch that has only one descendant. Instead of storing the
    * extra nodes along the branch just to store a single leaf, the entire branch can be collapsed
    * into one of these nodes.
    * 
    * <p>This can reduce search times by minimizing operations during key queries. It can also speed
    * up store operations since we don't need to create the whole branch of intermediate nodes if a
    * branch has only one descendant. When branches later get more occupants, the node can be easily
    * expanded, deferring the work of constructing the whole branch with minimal overhead. There is
    * some level of overhead when removing nodes, to collapse branches into these inner-leaf nodes
    * where possible, but it is minor. 
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class InnerLeafTrieNode<K, V> extends LeafTrieNode<K, V> {
      // Since this leaf isn't at the bottom of the branch, we can't infer its hash code just from
      // the path to this node. So we must store it explicitly.
      final int hash;
      
      InnerLeafTrieNode(int hashCode, K key, V value) {
         super(key, value);
         this.hash = hashCode;
      }
      
      @Override
      public ListNode<K, V> findNode(int hashCode, int currentOffset, Object searchKey) {
         return hashCode == this.hash ? doFindNode(searchKey) : null;
      }

      @Override
      @SuppressWarnings("unchecked")
      public ListNode<K, V> findOrAddNode(int hashCode, int currentOffset, K newKey, V newValue) {
         return hashCode == this.hash
               ? doFindOrAddNode(newKey, newValue)
               : (ListNode<K, V>) INNER_NODE_NEEDS_EXPANSION;
      }

      @Override
      public ListNode<K, V> removeNode(int hashCode, int currentOffset, Object keyToRemove) {
         return hashCode == this.hash ? doRemoveNode(keyToRemove) : null;
      }
      
      /**
       * Expands this node into a branch of one or more {@link IntermediateTrieNode}s. The length
       * of the branch depends on the differences between the given new hash code that we must
       * accommodate and this node's hash code.
       *
       * @param newHashCode the new hash code that is being added to this branch
       * @param currentOffset the current offset into the hash code, representing the number of bits
       *       already represented by the path to this node
       * @return a new node that includes a branch that holds this node's current keys and values
       *       and is long enough to accommodate insertion of a key with the given hash code
       */
      IntermediateTrieNode<K, V> expand(int newHashCode, int currentOffset) {
         // TODO: this would be a little simpler via recursion -- change it?
         int branchLength = 0;
         while (true) {
            assert currentOffset < 32;
            int mySignificantBits = (hash >>> currentOffset) & 0x3f;
            int newSignificantBits = (newHashCode >>> currentOffset) & 0x3f;
            if (mySignificantBits != newSignificantBits) {
               // here's the point where we split them
               long mask = 1L << mySignificantBits;
               // last intermediate level is at offset == 30
               LeafTrieNode<K, V> leaf;
               if (currentOffset < 30) {
                  // just pushing this inner-leaf down deeper in the tree
                  leaf = this;
               } else {
                  assert currentOffset == 30;
                  // at the bottom, so copy this inner-leaf to a normal leaf
                  leaf = new LeafTrieNode<K, V>(key, value);
                  leaf.next = this.next;
               }
               IntermediateTrieNode<K, V> node = new IntermediateTrieNode<K, V>(mask, leaf);
               while (branchLength > 0) {
                  // pop back up the tree, creating intermediate nodes as we go
                  currentOffset -= 6;
                  assert currentOffset >= 0;
                  mySignificantBits = (hash >>> currentOffset) & 0x3f;
                  mask = 1L << mySignificantBits;
                  node = new IntermediateTrieNode<K, V>(mask, node);
                  branchLength--;
               }
               return node;
            }
            // need to go further down the tree to find where the node must split
            currentOffset += 6;
            branchLength++;
         }
      }
   }
   
   /**
    * Creates a trie node for the specified key. If less than 32 bits of the hash code remain, then
    * this will be inner-leaf node. Otherwise, it is a normal leaf, at the bottom of the tree.
    *
    * @param hashCode the hash code for the key
    * @param currentOffset the number of bits remaining in the hash code, 32 at the root node
    *       but only two at the deepest level
    * @param currentOffset the current offset into the hash code, representing the number of bits
    *       already represented by the path to this node
    * @param key the key to find
    * @param value the value to associate with this key if not found and a new mapping is created
    * @return the newly created trie node
    */
   static <K, V> LeafTrieNode<K, V> createNode(int hashCode, int currentOffset, K key, V value) {
      if (currentOffset < 32) {
         return new InnerLeafTrieNode<K, V>(hashCode, key, value);
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
    * The root node of the trie, {@code null} if the trie is empty.
    */
   transient TrieNode<K, V> root;
   
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
      ListNode<K, V> node = root.findNode(hash(key), 0, key);
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
      ListNode<K, V> node = root.findNode(hash(key), 0, key);
      return node == null ? null : node.value;
   }

   @Override
   public V put(K key, V value) {
      if (root == null) {
         root = createNode(hash(key), 0, key, value);
         modCount++;
         size++;
         return null;
      }
      int hash = hash(key);
      ListNode<K, V> existing = root.findOrAddNode(hash, 0, key, value);
      if (existing == INNER_NODE_NEEDS_EXPANSION) {
         root = ((InnerLeafTrieNode<K, V>) root).expand(hash, 0);
         existing = root.findOrAddNode(hash, 0, key, value);
         assert existing != INNER_NODE_NEEDS_EXPANSION;
      }
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
      int hash = hash(key);
      ListNode<K, V> removed = root.removeNode(hash, 0, key);
      if (removed != null) {
         if (removed != root && !root.isEmpty()) {
            // didn't remove the root, but maybe we need to collapse it
            InnerLeafTrieNode<K, V> replacement = root.tryCollapse(hash, 0);
            if (replacement != null) {
               root = replacement;
            }
         }
         modCount++;
         if (--size == 0) {
            assert root.isEmpty() || removed == root;
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
         private final K key = node.key;
         private V memoizedValue = node.value;
         private ListNode<K, V> listNode = node;
         private int myModCount = modCount;
         
         private boolean refreshNode() {
            if (modCount != myModCount) {
               if (root == null) {
                  return false;
               }
               ListNode<K, V> newNode = root.findNode(hash(key), 0, key);
               if (newNode == null) {
                  return false;
               }
               listNode = newNode;
               memoizedValue = newNode.value;
               myModCount = modCount;
            }
            return true;
         }
         
         @Override
         public K getKey() {
            return key;
         }

         @Override
         public V getValue() {
            return refreshNode() ? listNode.value : memoizedValue;
         }

         @Override
         public V setValue(V value) {
            if (!refreshNode()) {
               // key was concurrently removed from the map
               throw new ConcurrentModificationException();
            }
            V ret = listNode.value;
            memoizedValue = listNode.value = value;
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
   
   // for testing
   int depth() {
      return root == null ? 0 : root.depth();
   }
   
   @Override
   public HamtMap<K, V> clone() {
      try {
         @SuppressWarnings("unchecked")
         HamtMap<K, V> clone = (HamtMap<K, V>) super.clone();
         clone.modCount = 0;
         clone.root = root.clone();
         return clone;
         
      } catch (CloneNotSupportedException e) {
         // we implement Cloneable, so this shouldn't be possible
         throw new AssertionError();
      }
   }

   @Override
   public void forEach(BiConsumer<? super K, ? super V> action) {
      requireNonNull(action);
      if (root != null) {
         root.forEach(e -> action.accept(e.key, e.value));
      }
   }

   @Override
   public void replaceAll(BiFunction<? super K, ? super V, ? extends V> remapper) {
      requireNonNull(remapper);
      if (root != null) {
         root.forEach(e -> e.value = remapper.apply(e.key, e.value));
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
            ListNode<K, V> node = root.findNode(hash(key), 0, key);
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
            ListNode<K, V> node = root.findNode(hash(key), 0, key);
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
      final IntermediateTrieNode<K, V> node;
      
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
            while (node instanceof IntermediateTrieNode) {
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
                  while (node instanceof IntermediateTrieNode) {
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
         int hashCode = hash(key);
         int currentOffset = 0;
         TrieNode<K, V> node = root;
         while (node instanceof IntermediateTrieNode) {
            IntermediateTrieNode<K, V> iNode = (IntermediateTrieNode<K, V>) node; 
            int significantBits = (hashCode >>> currentOffset) & 0x3f;
            long mask = 1L << significantBits;
            int index = Long.bitCount((mask - 1) & iNode.present);
            stack.push(new StackFrame<K, V>(iNode, index));
            node = iNode.children[index];
            currentOffset += 6;
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
