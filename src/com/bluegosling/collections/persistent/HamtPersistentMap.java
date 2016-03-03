package com.bluegosling.collections.persistent;

import static java.util.Objects.requireNonNull;

import com.bluegosling.collections.HamtMap;
import com.bluegosling.collections.MapUtils;
import com.bluegosling.collections.immutable.AbstractImmutableMap;
import com.bluegosling.collections.immutable.ImmutableMap;
import com.bluegosling.collections.immutable.Immutables;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * An implementation of {@link PersistentMap} that uses an immutable hash array-mapped trie (HAMT)
 * plus path copying on updates. This is an immutable and persistent version of {@link HamtMap}.
 * 
 * <p>Each level of the trie contains information for 4 bits of the hash code and thus can have up
 * to 16 children. This means the trie can have a depth of up to eight.
 *
 * @see HamtMap
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of value in the map
 */
// TODO: javadoc
// TODO: moar tests?
public class HamtPersistentMap<K, V> extends AbstractImmutableMap<K, V>
      implements PersistentMap<K, V> {
   
   /*
    * We intentionally use less density than HamtMap to reduce the cost of path copying on writes.
    * HamtMap uses 6 bits per level (up to 64 children). That means a path-copy could copy up to
    * 6 arrays (one at each level) and up to 324 elements. With a density of 4 bits per level (up to
    * 16 children), a path-copy will copy up to 8 arrays, but only up to 128 elements.
    * 
    * A non-persistent HamtMap never does path copying, so it generally is *more* efficient with
    * greater density. So we maximize density (up to 64 children since the biggest bitmask we can
    * use that is efficient is a long, which has 64 bits).
    */
   
   // TODO: Benchmark with densities of 4, 5, and 6 bits per level to verify that actual performance
   // aligns with the intuition behind the above comments. Also benchmark HamtMap in the same way.
   
   private static final int BITS_PER_LEVEL = 4;
   private static final int BITS_MASK = 0xf;

   /**
    * A node in a linked list that stores key-value pairs for a given hash code.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
      
      /**
       * Computes a copy of this linked list, with all values replaced with the result of applying
       * the given function to a node's current key and value.
       *
       * @param fn the function used to compute the replacement values
       * @return a new linked list with values replaced using the given function
       */
      public ListNode<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
         ListNode<K, V> newNext = next == null ? null : next.replaceAll(fn);
         return new ListNode<>(key, fn.apply(key, value), newNext);
      }
   }
   
   /**
    * A mutable holder for storing the result of {@link TrieNode#put(int, int, Object, Object)}.
    * Java doesn't support light-weight tuples or returning multiple values on the stack, so we use
    * this idiom to reduce allocations and garbage. Instead of each level in the trie creating and
    * returning a new tuple, we allocate just one at the leaf of the recursive call and then let
    * each level of the trie modify that one object.
    *
    * @param <K> the type of keys in the trie
    * @param <V> the type of values in the trie
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class PutResult<K, V> {
      PutResult() {
      }
      
      /**
       * The new sub-trie with the results of adding the new mapping.
       */
      TrieNode<K, V> node;
      
      /**
       * True if a new entry was added or false if an existing entry was updated. This is necessary
       * for proper accounting of the map's size during
       * {@link HamtPersistentMap#put(Object, Object)} operations.
       */
      boolean added;
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
       * Finds a node for the specified key. Each level of the tree represents four (out of 32) bits
       * from the key's hash code. 
       *
       * @param hashCode the hash code for the key
       * @param currentOffset represents the number of bits of the hash code already processed
       * @param key the key to find
       * @return the list node containing the key or {@code null} if the key is not found
       */
      ListNode<K, V> findNode(int hash, int currentOffset, Object key);

      /**
       * Computes a new trie where the given key is removed. If the returned object is the same
       * instance as {@code this}, then the key was not present.
       *
       * @param hashCode the hash code for the key
       * @param currentOffset represents the number of bits of the hash code already processed
       * @param key the key to remove
       * @return a new version of this trie with the given key removed
       */
      TrieNode<K, V> remove(int hash, int currentOffset, Object key);
      
      /**
       * Computes a new trie where the given mapping is added. The caller is expected to supply a
       * {@linkplain PutResult holder for the result}. It is populated with the results of adding
       * the mapping: a new trie and a flag indicating if the size of the new trie is different than
       * {@code this}.
       *
       * @param hashCode the hash code for the key
       * @param currentOffset represents the number of bits of the hash code already processed
       * @param key the key to find
       * @param value the value to associate with this key if not found and a new mapping is created
       * @return the result of the operation
       */
      PutResult<K, V> put(int hash, int currentOffset, K key, V value);
      
      /**
       * Executes the given action for every mapping present in this trie node.
       *
       * @param action the action to invoke
       */
      void forEach(Consumer<ListNode<K, V>> action);
      
      /**
       * Computes a copy of this trie node, with all values replaced with the result of applying
       * the given function to a mapping's current key and value.
       *
       * @param fn the function used to compute the replacement values
       * @return a new trie node with values replaced using the given function
       */
      TrieNode<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn);
   }
   
   /**
    * A non-leaf node in the trie. Each such level has an array of child nodes, each one
    * representing a different combination of bits. Each such level encodes four bits of the keys'
    * 32-bit hash codes. So each level can have up to 16 child nodes.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class IntermediateTrieNode<K, V> implements TrieNode<K, V> {
      
      /**
       * A bitmask for which children are present. Each child represents a different combination of
       * four bits, up to 16 possible children (so one bit in this mask for each possible child).
       */
      final int present;

      /**
       * The array of children. Its size is always the number of actual children.
       * 
       * <p>Implementation detail: Since this data structure is immutable, care must be taken to
       * ensure that the array is never updated after the trie node is built.
       */
      final TrieNode<K, V> children[];
      
      IntermediateTrieNode(int present, TrieNode<K, V> firstChild) {
         this.present = present;
         children = createChildren(1);
         children[0] = firstChild;
      }

      IntermediateTrieNode(int present, TrieNode<K, V> children[]) {
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
         int significantBits = (hash >>> currentOffset) & BITS_MASK;
         long mask = 1L << significantBits;
         if ((present & mask) == 0) {
            return null;
         }
         int index = Long.bitCount((mask - 1) & present);
         return children[index].findNode(hash, currentOffset + BITS_PER_LEVEL, key);
      }

      @Override
      public TrieNode<K, V> remove(int hash, int currentOffset, Object key) {
         int significantBits = (hash >>> currentOffset) & BITS_MASK;
         int mask = 1 << significantBits;
         if ((present & mask) == 0) {
            return this;
         }
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> oldChild = children[index];
         TrieNode<K, V> newChild = oldChild.remove(hash, currentOffset + BITS_PER_LEVEL, key);
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
            TrieNode<K, V> remainingChild;
            if (numChildren == 1
                  && (remainingChild = children[index == 0 ? 1 : 0]) instanceof LeafTrieNode) {
               // one leaf remains? collapse this node and the leaf into an InnerLeafTrieNode
               LeafTrieNode<K, V> leaf = (LeafTrieNode<K, V>) remainingChild;
               return leaf instanceof InnerLeafTrieNode
                     ? leaf
                     : new InnerLeafTrieNode<K, V>(leaf.key, leaf.value, leaf.next, hash(leaf.key));
            }
            // create a replacement node without this child entry
            TrieNode<K, V> newChildren[] = createChildren(numChildren);
            if (index > 0) {
               System.arraycopy(children, 0, newChildren, 0, index);
            }
            if (index < numChildren) {
               System.arraycopy(children, index + 1, newChildren, index, numChildren - index);
            }
            int newPresent = present & ~mask;
            return new IntermediateTrieNode<K, V>(newPresent, newChildren);
         } else if (numChildren == 1 && newChild instanceof LeafTrieNode) {
            // collapse this node and the leaf into an InnerLeafTrieNode
            LeafTrieNode<K, V> leaf = (LeafTrieNode<K, V>) newChild;
            return leaf instanceof InnerLeafTrieNode
                  ? leaf
                  : new InnerLeafTrieNode<K, V>(leaf.key, leaf.value, leaf.next, hash(leaf.key));
         } else {
            // create a replacement node, replacing old child with this new one
            TrieNode<K, V> newChildren[] = children.clone();
            newChildren[index] = newChild;
            return new IntermediateTrieNode<K, V>(present, newChildren);
         }
      }

      @Override
      public PutResult<K, V> put(int hash, int currentOffset, K key, V value) {
         int significantBits = (hash >>> currentOffset) & BITS_MASK;
         int mask = 1 << significantBits;
         if ((present & mask) == 0) {
            // add new child node for this new entry
            int newPresent = present | mask;
            int index = Long.bitCount((mask - 1) & newPresent);
            int length = children.length;
            TrieNode<K, V> newChildren[] = createChildren(length + 1);
            if (index > 0) {
               System.arraycopy(children, 0, newChildren, 0, index);
            }
            if (index < length) {
               System.arraycopy(children, index, newChildren, index + 1, length - index);
            }
            if (currentOffset + BITS_PER_LEVEL >= 32) {
               newChildren[index] = new LeafTrieNode<K, V>(key, value);
            } else {
               newChildren[index] = new InnerLeafTrieNode<K, V>(key, value, hash);
            }
            PutResult<K, V> result = new PutResult<>();
            result.node = new IntermediateTrieNode<K, V>(newPresent, newChildren);
            result.added = true;
            return result;
         }
         
         int index = Long.bitCount((mask - 1) & present);
         TrieNode<K, V> oldChild = children[index];
         PutResult<K, V>  result = oldChild.put(hash, currentOffset + BITS_PER_LEVEL, key, value);
         TrieNode<K, V> newChild = result.node;
         if (newChild == oldChild) {
            // no change
            assert !result.added;
            result.node = this;
            result.added = false;
            return result;
         }
         int length = children.length;
         TrieNode<K, V> newChildren[] = createChildren(length);
         System.arraycopy(children, 0, newChildren, 0, length);
         newChildren[index] = newChild;
         result.node = new IntermediateTrieNode<K, V>(present, newChildren);
         return result;
      }
      
      @Override
      public void forEach(Consumer<ListNode<K, V>> action) {
         for (TrieNode<K, V> child : children) {
            child.forEach(action);
         }
      }

      @Override
      public TrieNode<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
         int len = children.length;
         TrieNode<K, V> newChildren[] = createChildren(len);
         for (int i = 0; i < len; i++) {
            newChildren[i] = children[i].replaceAll(fn);
         }
         return new IntermediateTrieNode<>(present, newChildren);
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
         return doFind(searchKey);
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
         return doRemove(keyToRemove, hash);
      }
      
      TrieNode<K, V> create(K k, V v, ListNode<K, V> n, @SuppressWarnings("unused") int hash) {
         return new LeafTrieNode<K, V>(k, v, n);
      }
      
      TrieNode<K, V> doRemove(Object keyToRemove, int hash) {
         ArrayDeque<ListNode<K, V>> stack = new ArrayDeque<ListNode<K, V>>();
         for (ListNode<K, V> current = this; current != null; current = current.next) {
            if (keyToRemove == null ? current.key == null : keyToRemove.equals(current.key)) {
               if (stack.isEmpty()) {
                  if (next == null) {
                     // no other values here, deleting whole node
                     return null;
                  }
                  // removing the initial leaf node -- just create a new head ListNode that is
                  // also a TrieNode and keep the rest
                  return create(next.key, next.value, next.next, hash);
               }
               // rebuild path from leaf node to here and keep the rest of the list
               current = current.next; // skip over the one we're removing
               while (true) {
                  ListNode<K, V> node = stack.pop();
                  if (stack.isEmpty()) {
                     return create(node.key, node.value, current, hash);
                  }
                  current = new ListNode<K, V>(node.key, node.value, current); 
               }
            }
            stack.push(current);
         }
         // never found key to remove, so no change
         return this;
      }

      @Override
      public PutResult<K, V> put(int hash, int currentOffset, K newKey, V newValue) {
         assert currentOffset >= 32;
         return doPut(newKey, newValue, hash);
      }
      
      PutResult<K, V> doPut(K newKey, V newValue, int hash) {
         PutResult<K, V> result = new PutResult<>();
         ArrayDeque<ListNode<K, V>> stack = new ArrayDeque<ListNode<K, V>>();
         for (ListNode<K, V> current = this; current != null; current = current.next) {
            if (newKey == null ? current.key == null : newKey.equals(current.key)) {
               if (Objects.equals(newValue, current.value)) {
                  // no change
                  result.node = this;
                  result.added = false;
                  return result;
               }
               if (stack.isEmpty()) {
                  // changing the initial leaf node -- just create a new head ListNode with new
                  // value and keep the rest
                  result.node = create(newKey, newValue, next, hash);
                  result.added = false;
                  return result;
               }
               // rebuild path from leaf node to here and keep the rest of the list
               current = new ListNode<K, V>(newKey, newValue, current.next); 
               while (true) {
                  ListNode<K, V> node = stack.pop();
                  if (stack.isEmpty()) {
                     result.node = create(node.key, node.value, current, hash);
                     result.added = false;
                     return result;
                  }
                  current = new ListNode<K, V>(node.key, node.value, current);
               }
            }
            stack.push(current);
         }
         // adding new value - push to head of list
         result.node = create(newKey, newValue, new ListNode<K, V>(key, value, next), hash);
         result.added = true;
         return result;
      }
      
      @Override
      public void forEach(Consumer<ListNode<K, V>> action) {
         ListNode<K, V> node = this;
         while (node != null) {
            action.accept(node);
            node = node.next;
         }
      }

      @Override
      public LeafTrieNode<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
         ListNode<K, V> newNext = next == null ? null : next.replaceAll(fn);
         return new LeafTrieNode<>(key, fn.apply(key, value), newNext);
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
    * branch has only one descendant. 
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
         return hash == hashCode ? doRemove(keyToRemove, hash) : this;
      }

      @Override
      public PutResult<K, V> put(int hash, int currentOffset, K newKey, V newValue) {
         if (hash == hashCode) {
            return doPut(newKey, newValue, hash);
         } else {
            PutResult<K, V> result = new PutResult<>();
            result.node = create(hash, currentOffset, newKey, newValue);
            result.added = true;
            return result;
         }
      }

      @Override
      TrieNode<K, V> create(K k, V v, ListNode<K, V> n, int hash) {
         return new InnerLeafTrieNode<K, V>(k, v, n, hash);
      }

      private TrieNode<K, V> create(int hash, int currentOffset, K newKey, V newValue) {
         int significantBits1 = (hashCode >>> currentOffset) & BITS_MASK;
         int significantBits2 = (hash >>> currentOffset) & BITS_MASK;

         if (significantBits1 == significantBits2) {
            return new IntermediateTrieNode<K, V>(1 << significantBits1,
                  create(hash, currentOffset + BITS_PER_LEVEL, newKey, newValue));
         }
         
         int mask = (1 << significantBits1) | (1 << significantBits2);
         TrieNode<K, V> current, addition;
         if (currentOffset + BITS_PER_LEVEL >= 32) {
            // at the end of the chain so use leaf nodes
            current = new LeafTrieNode<K, V>(this.key, this.value, this.next);
            addition = new LeafTrieNode<K, V>(newKey, newValue);
         } else {
            current = this;
            addition = new InnerLeafTrieNode<K, V>(newKey, newValue, hash);
         }
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
      
      @Override
      public LeafTrieNode<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
         ListNode<K, V> newNext = next == null ? null : next.replaceAll(fn);
         return new InnerLeafTrieNode<>(key, fn.apply(key, value), newNext, hashCode);
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
      return HamtPersistentMap.<K, V>create().putAll(map);
   }

   @SuppressWarnings("unchecked") // due to immutability, cast is safe
   public static <K, V> HamtPersistentMap<K, V> create(ImmutableMap<? extends K, ? extends V> map) {
      if (map instanceof HamtPersistentMap) {
         return (HamtPersistentMap<K, V>) map;
      }
      return HamtPersistentMap.<K, V>create().putAll(map);
   }

   /**
    * The total number of mappings present.
    */
   private final int size;
   
   /**
    * The root of the trie.
    */
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
   public HamtPersistentMap<K, V> put(K key, V value) {
      if (root == null) {
         return new HamtPersistentMap<K, V>(1, new InnerLeafTrieNode<K, V>(key, value, hash(key)));
      }
      PutResult<K, V> result = root.put(hash(key), 0, key, value);
      return result.node == root ? this
            : new HamtPersistentMap<K, V>(result.added ? size + 1 : size, result.node);
   }
   
   @Override
   public HamtPersistentMap<K, V> remove(Object o) {
      if (root == null) {
         return this;
      }
      TrieNode<K, V> newRoot = root.remove(hash(o), 0, o);
      return newRoot == root ? this : new HamtPersistentMap<K, V>(size - 1, newRoot);
   }

   @Override
   public HamtPersistentMap<K, V> removeAll(Iterable<?> keys) {
      if (isEmpty()) {
         return this;
      }
      // TODO: bulk remove? maybe create array sorted by hash code and then use bulk operations
      // to remove chunks from TrieNodes?
      HamtPersistentMap<K, V> ret = this;
      for (Object key : keys) {
         ret = ret.remove(key);
      }
      return ret;
   }

   @Override
   public HamtPersistentMap<K, V> retainAll(Iterable<?> keys) {
      if (isEmpty()) {
         return this;
      }
      // TODO: bulk remove? maybe create array sorted by hash code and then use bulk operations
      // to remove chunks from TrieNodes?
      HamtPersistentMap<K, V> ret = create();
      for (Object key : keys) {
         ListNode<K, V> node = root == null ? null : root.findNode(hash(key), 0, key);
         if (node != null) {
            ret = ret.put(node.key, node.value);
         }
      }
      return ret;
   }

   @Override
   public HamtPersistentMap<K, V> putAll(Map<? extends K, ? extends V> items) {
      // TODO: bulk insert? maybe create array of entries sorted by hash code and then use bulk
      // operations to insert chunks into TrieNodes?
      HamtPersistentMap<K, V> ret = this;
      for (Map.Entry<? extends K, ? extends V> entry : items.entrySet()) {
         ret = ret.put(entry.getKey(), entry.getValue());
      }
      return ret;
   }

   @Override
   public HamtPersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items) {
      if (isEmpty() && items instanceof HamtPersistentMap) {
         return Immutables.cast((HamtPersistentMap<? extends K, ? extends V>) items);
      }
      // TODO: bulk insert? maybe create array of entries sorted by hash code and then use bulk
      // operations to insert chunks into TrieNodes?
      HamtPersistentMap<K, V> ret = this;
      for (Entry<? extends K, ? extends V> entry : items) {
         ret = ret.put(entry.key(), entry.value());
      }
      return ret;
   }

   @Override
   public int size() {
      return size;
   }
   
   @Override
   public HamtPersistentMap<K, V> clear() {
      return create();
   }
   
   @Override
   public void forEach(BiConsumer<? super K, ? super V> action) {
      requireNonNull(action);
      if (root != null) {
         root.forEach(e -> action.accept(e.key, e.value));
      }
   }
   
   @Override
   public PersistentMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
      requireNonNull(fn);
      return root == null ? this : new HamtPersistentMap<>(size, root.replaceAll(fn));
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
         this.node = node;
         childIndex = 0;
      }
   }
   
   /**
    * An iterator over the mappings in the trie.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class Iter implements Iterator<Entry<K, V>> {
      /**
       * The stack, used for doing a depth-first traversal without recursion.
       */
      private final ArrayDeque<StackFrame<K, V>> stack = new ArrayDeque<StackFrame<K, V>>();

      /**
       * The current position of this iterator. This holds the next mapping to be returned by this
       * iterator.
       */
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
                  break;
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
