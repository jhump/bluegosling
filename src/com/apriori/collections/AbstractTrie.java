package com.apriori.collections;

import com.apriori.possible.Reference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * An abstract base class for a trie implementation. This class provides numerous basic operations
 * on the trie. It also provides an interface that trie nodes are expected to implement. Typical
 * concrete trie implementations will use a {@link Map} to implement the node interface.
 * 
 * <p>Each key in a trie is a sequence of elements. So each node in the trie is a map of elements to
 * the next node along the path for that element. Each node can optionally have a value associated
 * with it. The key that corresponds to that value is the sequence of elements along the path from
 * the root node to the node with the mapped value. Leaf nodes (which are empty maps) always have
 * values.
 * 
 * @param <K> the type of element in each key
 * @param <X> represents an entire key, or sequence of {@code K} ({@link Void} if no such
 *       representation is necessary)
 * @param <V> the type of value in the trie
 * @param <N> the concrete type of trie node
 * 
 * @see AbstractCompositeTrie
 * @see AbstractSequenceTrie
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractTrie<K, X, V, N extends AbstractTrie.Node<K, X, V, N>> {
   
   /**
    * This interfaces represents a single node in the trie. The interface is typically implemented
    * by an implementation of {@code Map<K, N>}. As such, several methods on this interface
    * intentionally have the same signature as methods on such a map.
    *
    * @param <K> the type of element in each key
    * @param <X> represents an entire key, or sequence of {@code K} ({@link Void} if no such
    *       representation is necessary)
    * @param <V> the type of value in the trie
    * @param <N> the concrete type of trie node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Node<K, X, V, N extends Node<K, X, V, N>> {
      
      // Operations for the key/value at this node
      
      /**
       * Gets the key element for this node. This element is just one value in a sequence. An
       * entire sequence of such values represents the key to a value in the trie.
       *
       * @return the key element for this node
       */
      K getKey();
      
      /**
       * Returns true if this node has a mapped value.
       *
       * @return true if this node has a mapped value
       */
      boolean valuePresent();
      
      /**
       * Returns the mapped value for this node.
       *
       * @return the mapped value for this node
       */
      V getValue();
      
      /**
       * Sets the mapped value for this node.
       *
       * @param newValue the mapped value for this node
       */
      void setValue(V newValue);
      
      /**
       * Clears the mapped value for this node.
       */
      void clearValue();

      // Composite key methods, only used by tries with alternate representations of keys
      
      /**
       * Gets the full trie key for this node. The returned object is an alternate representation of
       * the path of elements from the root node to this one.
       *
       * @return the full trie key for this node or {@code null} if this trie has no such
       *       representation
       */
      X getLeafKey();

      /**
       * Sets the full trie key for this node. This value is an alternate representation of the path
       * of elements from the root node to this one.
       *
       * @param leafKey the full trie key for this node
       */
      void setLeafKey(X leafKey);

      // For tracking number of elements in this node's sub-tree
      
      /**
       * The total number of elements in this sub-trie. (Every node in the trie is itself a
       * trie since tries are recursive data structures.)
       * 
       * <p>For nodes that are implemented by a map, this value will not be the same as the size of
       * the map.
       * 
       * <p><strong>Note:</strong> implementations need not update this value from any of the other
       * method implementations. For example, adding or removing a sub-trie need not touch this
       * value. The other methods, to increment and decrement this count, are used by {@link 
       *
       * @return the total number of elements in this sub-trie
       */
      int elementCount();
      
      /**
       * Increments the count of elements for this sub-trie. Invoked whenever a value is added to
       * this sub-trie. This is the same as the following:<pre>
       * node.addToCount(1);
       * </pre>
       */
      void incrementCount();
      
      /**
       * Decrements the count of elements for this sub-trie. Invoked whenever a value is removed
       * from this sub-trie. This is the same as the following:<pre>
       * node.addToCount(-1);
       * </pre>
       */
      void decrementCount();
      
      /**
       * Adds the given value to the count of elements for this sub-trie. Invoked when a bulk add
       * or remove operation is performed.
       *
       * @param delta the amount to add to the current element count
       */
      void addToCount(int delta);

      // A couple of basic navigation operations
      
      /**
       * Returns the parent of this node.
       *
       * @return the parent of this node or {@code null} if this is the root node
       */
      N getParent();
      
      /**
       * Provides an iterator over all children of this node.
       *
       * @return an iterator over all children / sub-tries
       */
      Iterator<N> childIterator();

      // Map operations, for working with direct children of this node
      
      /**
       * Gets the sub-trie associated with the given key element.
       *
       * @param key the next element in a key
       * @return the sub-trie corresponding to that next element or {@code null} if no such
       *       element exists
       */
      N get(Object key);
      
      /**
       * Associates the given sub-trie with the given key element.
       *
       * @param key the next element in a key
       * @param node the sub-trie corresponding to that next element
       * @return the previous sub-trie that corresponded to the given element or {@code null} if
       *       there was no previous mapping
       */
      N put(K key, N node);

      /**
       * Removes the mapping for the given key element.
       *
       * @param key the next element in a key
       * @return the sub-trie that was removed or {@code null} if there was no previous mapping
       */
      N remove(Object key);
      
      /**
       * Determines if this node has any children / sub-tries.
       *
       * @return true if this node has no children; false otherwise
       */
      boolean isEmpty();
      
      /**
       * Removes all children from this node.
       */
      void clear();      
   }
   
   /**
    * The root of this trie.
    */
   protected N root;
   
   /**
    * The generation number of this trie. This is updated whenever major structural changes are
    * made to the trie that cannot otherwise easily be detected.
    * 
    * <p>This value is used by sub-classes in implementing prefix sub-tries, which memoize their
    * prefixed root for efficiency, but need to know how to invalidate the value and recompute when
    * necessary. For example, minimal book-keeping is done when the trie is cleared so that the
    * operation can run in constant time. So the generation is incremented when the trie is cleared.
    * A prefix sub-trie will see the change to generation and know to invalidate its memoized root.
    */
   protected int generation;
   
   /**
    * Used by concrete sub-classes to construct a new, empty trie.
    */
   protected AbstractTrie() {
      this.root = newNode(null, null);
   }
   
   /**
    * Used by concrete sub-classes to construct a new trie that wraps the given node. This can be
    * used to easily create prefix sub-tries where the specified node is an inner node, the path to
    * which is the prefix.
    * 
    * @param root the node that will be this trie's root
    */
   protected AbstractTrie(N root) {
      this.root = root;
   }
   
   /**
    * Creates a new node for the given key element and with the given parent node.
    *
    * @param key a key element
    * @param parent the new node's parent or {@code null} if the new node is a root
    * @return a new node for the given key element and with the given parent
    */
   protected abstract N newNode(K key, N parent);
   
   /**
    * Finds the node that represents the given sequence of key elements. This navigates down the
    * trie, finding the next sub-trie that corresponds to the next element in the sequence. If, at
    * any level, no such sub-trie exists, then the given key is not present in this trie.
    *
    * @param keys a sequence of key elements
    * @return the node that represents the given key elements or {@code null} if it does not exist
    */
   protected N get(Iterable<K> keys) {
      Iterator<K> iter = keys.iterator();
      N node = root;
      while (iter.hasNext() && node != null) {
         K k = iter.next();
         node = node.get(k);
      }
      return node;
   }
   
   /**
    * Ensures that a path of nodes exists for the given sequence of key elements, starting with the
    * root node. This is similar to {@link #get(Iterable)} except that, if any sub-trie is absent
    * along the given path, it is created. 
    *
    * @param keys a sequence of key elements
    * @return the node that represents the given key elements
    */
   protected N ensurePath(Iterable<K> keys) {
      return ensurePath(root, keys);
   }

   /**
    * Ensures that a path of nodes exists for the given sequence of key elements, starting with the
    * given node. If any sub-trie is absent along the given path, it is created.
    * 
    * @param start the starting node
    * @param keys a sequence of key elements
    * @return the node that represents the given key elements
    */
   protected final N ensurePath(N start, Iterable<K> keys) {
      Iterator<K> iter = keys.iterator();
      N node = start;
      while (iter.hasNext()) {
         K k = iter.next();
         N next = node.get(k);
         if (next == null) {
            next = newNode(k, node);
            node.put(k, next);
         }
         node = next;
      }
      return node;
   }
   
   /**
    * Puts the given mapping in the trie.
    *
    * @param keys the sequence of key elements for this mapping
    * @param leafKey an alternate representation of the sequence of key elements
    * @param value the value for this mapping
    * @return the previously mapped value for this key or {@code null} if no such mapping existed
    */
   protected V put(Iterable<K> keys, X leafKey, V value) {
      N node = ensurePath(keys);
      V ret;
      if (node.valuePresent()) {
         ret = node.getValue();
      } else {
         ret = null;
         for (N n = node; n != null; n = n.getParent()) {
            n.incrementCount();
         }
      }
      node.setLeafKey(leafKey);
      node.setValue(value);
      return ret;
   }
   
   /**
    * Removes a mapping from the trie. If removal of this mapping creates any empty sub-tries along
    * the path to the removed mapping, they are also removed.
    *
    * @param keys the sequence of key elements that identifies the mapping to remove
    * @return an unset reference if the given sequence of key elements did not exist; otherwise a
    *       reference set to the value that was associated with the removed keys
    */
   protected Reference<V> remove(Iterable<K> keys) {
      Iterator<K> iter = keys.iterator();
      N node = root;
      while (iter.hasNext()) {
         K k = iter.next();
         node = node.get(k);
         if (node == null) {
            return Reference.unset();
         }
      }
      if (!node.valuePresent()) {
         return Reference.unset();
      }
      V v = node.getValue();
      remove(node);
      return Reference.setTo(v);
   }
   
   /**
    * Removes the mapping for the given sub-trie from this trie. This method then performs clean up
    * to ensure that no empty sub-tries are left in the trie. So a precondition is that the given
    * node {@linkplain Node#valuePresent() has a value}.
    *
    * @param node the node whose mapping is to be removed from the trie
    */
   protected void remove(N node) {
      assert node.valuePresent();
      node.clearValue();
      for (N n = node.getParent(); n != null; n = n.getParent()) {
         n.decrementCount();
      }
      while (node != null && !node.valuePresent() && node.isEmpty()) {
         N parent = node.getParent();
         if (parent != null) {
            parent.remove(node.getKey());
         }
         node = parent;
      }
   }
   
   /**
    * Clears the trie, removing all mappings in bulk.
    */
   public void clear() {
      root.clear();
      root.clearValue();
      generation++;
   }
   
   /**
    * Searches the trie for the given mapped value. Since this is looking for a value, not a key, it
    * runs in linear time, <em>O(n)</em>.
    *
    * @param value a value
    * @return true if the given value is mapped in the trie; false otherwise
    */
   public boolean containsValue(Object value) {
      ArrayDeque<Iterator<N>> stack = new ArrayDeque<>();
      N node = root;
      if (node == null) {
         return false;
      }
      while (true) {
         if (node.valuePresent() && Objects.equals(node.getValue(), value)) {
            return true;
         }
         Iterator<N> iter = node.childIterator();
         while (!iter.hasNext()) {
            if (stack.isEmpty()) {
               return false;
            }
            iter = stack.pop();
         }
         node = iter.next();
         stack.push(iter);
      }
   }
   
   /**
    * Returns the total number mappings present in this trie.
    *
    * @return the number of mappings in the trie
    */
   public int size() {
      return root.elementCount();
   }
   
   /**
    * Determines if this trie is empty. The trie is empty if it has no mappings present.
    *
    * @return true if this trie is empty
    */
   public boolean isEmpty() {
      return size() == 0;
   }
   
   /**
    * Returns an iterator that will visit each mapping in the trie. The given producer is used to
    * construct the values returned by the iterator.
    *
    * @param <T> the type of value returned by the iterator
    * @param producer produces values returned by the iterator, given a node and a way to construct
    *       the key list for that node
    * @return an iterator that will visit each mapping in the trie
    */
   protected <T> Iterator<T> entryIterator(BiFunction<Supplier<List<K>>, N, T> producer) {
      return new EntryIterator<>(root, producer);
   }
   
   /**
    * Returns an iterator that will visit each mapping in the trie, starting at the given node and
    * skipping any nodes in the trie that might otherwise have been visited before the given node.
    * The given producer is used to construct the values returned by the iterator.
    *
    * @param <T> the type of value returned by the iterator
    * @param producer produces values returned by the iterator, given a node and a way to construct
    *       the key list for that node
    * @param startAt the initial node visited by the iterator
    * @return an iterator that will visit each mapping in the trie
    */
   protected <T> Iterator<T> entryIteratorFrom(BiFunction<Supplier<List<K>>, N, T> producer,
         N startAt) {
      return new EntryIterator<>(root, producer, startAt);
   }

   /**
    * Constructs a list representing the path between the given two nodes. This navigates the path
    * in reverse, so the start node must be a descendant of the end node.
    *
    * @param start the starting node for the path (will actually be the final element in the path)
    * @param end the ending node (will actually be the parent of the first element in the path)
    * @return the list that represents the path between the two given nodes
    */
   protected List<K> createKeyList(N start, N end) {
      List<K> ret = new ArrayList<>();
      N node = start;
      while (node != null && node != end) {
         ret.add(node.getKey());
         node = node.getParent();
      }
      assert node == end;
      Collections.reverse(ret);
      return Collections.unmodifiableList(ret);
   }
   
   /**
    * An implementation of {@link Map.Entry} for mappings in a trie.
    *
    * @param <T> the type of key for this entry (usually either {@code List<K>} or {@code X})
    * @param <K> the type of element in each key
    * @param <X> represents an entire key, or sequence of {@code K} ({@link Void} if no such
    *       representation is necessary)
    * @param <V> the type of value in the trie
    * @param <N> the concrete type of trie node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected static class EntryImpl<T, K, X, V, N extends Node<K, X, V, N>> implements Entry<T, V> {
      final T key;
      final N node;
      
      EntryImpl(T key, N node) {
         this.key = key;
         this.node = node;
      }

      @Override
      public T getKey() {
         return key;
      }

      @Override
      public V getValue() {
         if (!node.valuePresent()) {
            throw new ConcurrentModificationException();
         }
         return node.getValue();
      }

      @Override
      public V setValue(V value) {
         if (!node.valuePresent()) {
            throw new ConcurrentModificationException();
         }
         V ret = node.getValue();
         node.setValue(value);
         return ret;
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
   
   /**
    * An iterator over mappings in the trie.
    *
    * @param <T> the type of value returned by the iterator
    * @param <K> the type of element in each key
    * @param <X> represents an entire key, or sequence of {@code K} ({@link Void} if no such
    *       representation is necessary)
    * @param <V> the type of value in the trie
    * @param <N> the concrete type of trie node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected static class EntryIterator<T, K, X, V, N extends Node<K, X, V, N>>
         implements Iterator<T> {
      
      /**
       * Since the trie is a recursive structure, we store the state of descending through it
       * as a stack. Each frame in the stack has state about the progress of iteration through a
       * given node's children.
       */
      final ArrayDeque<StackFrame<K, X, V, N>> frames = new ArrayDeque<>();
      
      /**
       * Produces the value returned by the iterator.
       */
      final BiFunction<Supplier<List<K>>, N, T> producer;

      /**
       * A flag indicating if we're still on the very first value. After the first call to
       * {@link #next()}, this will be false.
       */
      boolean first = true;
      
      /**
       * The node that represents the last mapping that was returned by the iterator. 
       */
      N lastFetched;
      
      /**
       * Constructs a new iterator.
       *
       * @param root the root of the trie
       * @param producer an object that constructs the fetched values
       */
      protected EntryIterator(N root, BiFunction<Supplier<List<K>>, N, T> producer) {
         this(root, producer, root);
      }

      protected EntryIterator(N root, BiFunction<Supplier<List<K>>, N, T> producer,
            N startAt) {
         N prev = null;
         while(true) {
            assert startAt != null;
            StackFrame<K, X, V, N> frame = newStackFrame(startAt);
            if (prev != null) {
               // advance this frame to the right state
               Iterator<N> iter = frame.iter;
               boolean found = false;
               while (iter.hasNext()) {
                  N n = iter.next();
                  if (Objects.equals(n, prev)) {
                     found = true;
                     break;
                  }
               }
               assert found;
            }
            frames.push(frame);
            if (startAt == root) {
               break;
            }
            prev = startAt;
            startAt = startAt.getParent();
         }
         this.producer = producer;
      }

      /**
       * Constructs a new stack frame to represent the given node. This is called each time
       * iteration descends into a sub-trie.
       *
       * @param node the node that is being visited
       * @return a stack frame for trackinv the state of iterating through the given node's children
       */
      private StackFrame<K, X, V, N> newStackFrame(N node) {
         return new StackFrame<>(node, node.childIterator());
      }
      
      @Override
      public boolean hasNext() {
         for (StackFrame<K, X, V, N> frame : frames) {
            if (first && frame.node.valuePresent()) {
               // has initial element in the root node 
               return true;
            }
            if (frame.iter.hasNext()) {
               return true;
            }
         }
         return false;
      }

      @Override
      public T next() {
         if (frames.isEmpty()) {
            throw new NoSuchElementException();
         }
         StackFrame<K, X, V, N> top = frames.peek();
         if (first) {
            // have to special case initial conditions
            first = false;
            if (top.node.valuePresent()) {
               return producer.apply(this::createKeyList, lastFetched = top.node);
            }
         }
         // Find next node:
         // First, pop off any nodes whose ancestors have been exhausted.
         while (!top.iter.hasNext()) {
            StackFrame<K, X, V, N> frame = frames.pop();
            assert frame == top;
            if (frames.isEmpty()) {
               throw new NoSuchElementException();
            }
            top = frames.peek();
         }
         // Then drill down to find next node that has a value.
         N node;
         while (true) {
            node = top.iter.next();
            frames.push(top = newStackFrame(node));
            if (node.valuePresent()) {
               break;
            }
            assert top.iter.hasNext();
         }
         return producer.apply(this::createKeyList, lastFetched = node);
      }
      
      /**
       * Creates a list that represents the path to the current value. This looks at the nodes on
       * the {@linkplain #frames stack}, which represents the path, and creates a list of the
       * corresponding key elements.
       *
       * @return a list that represents the path to the current value
       */
      public List<K> createKeyList() {
         List<K> ret = new ArrayList<>(frames.size() - 1);
         boolean isFirst = true;
         for (Iterator<StackFrame<K, X, V, N>> iter = frames.descendingIterator();
               iter.hasNext(); ) {
            StackFrame<K, X, V, N> frame = iter.next();
            if (isFirst) {
               // skip the first
               isFirst = false;
               continue;
            }
            ret.add(frame.node.getKey());
         }
         return Collections.unmodifiableList(ret);
      }
      
      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         if (!lastFetched.valuePresent()) {
            throw new ConcurrentModificationException();
         }
         lastFetched.clearValue();
         for (N n = lastFetched.getParent(); n != null; n = n.getParent()) {
            n.decrementCount();
         }
         if (lastFetched.isEmpty()) {
            // Remove dead sub-tries. Must use iterators' remove() method so we don't accidentally
            // cause ConcurrentModificationExceptions on subsequent use of this iterator
            boolean isFirst = true;
            for (Iterator<StackFrame<K, X, V, N>> iter = frames.iterator(); iter.hasNext(); ) {
               StackFrame<K, X, V, N> frame = iter.next();
               if (isFirst) {
                  assert !frame.iter.hasNext();
                  isFirst = false;
               } else {
                  frame.iter.remove();
                  if (!frame.node.isEmpty() || frame.node.valuePresent()) {
                     // the rest of the ancestors have other content, so we're done pruning
                     break;
                  }
               }
            }
         }
         lastFetched = null;
      }
   }
   
   /**
    * A frame in the stack used to track iteration through the trie. The stack frame consists of a
    * node and an iterator that tracks the state of iteration through the node's children.
    *
    * @param <K> the type of element in each key
    * @param <X> represents an entire key, or sequence of {@code K} ({@link Void} if no such
    *       representation is necessary)
    * @param <V> the type of value in the trie
    * @param <N> the concrete type of trie node
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class StackFrame<K, X, V, N extends Node<K, X, V, N>> {
      final N node;
      final Iterator<N> iter;
      
      StackFrame(N node, Iterator<N> iter) {
         this.node = node;
         this.iter = iter;
      }
   }
}
