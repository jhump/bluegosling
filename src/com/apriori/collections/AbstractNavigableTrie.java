package com.apriori.collections;

import com.apriori.tuples.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

//TODO: javadoc
abstract class AbstractNavigableTrie<K, X, V, N extends AbstractNavigableTrie.NavigableNode<K, X, V, N>>
      extends AbstractTrie<K, X, V, N> {
   
   interface NavigableNode<K, X, V, N extends NavigableNode<K, X, V, N>>
         extends Node<K, X, V, N> {
      // navigable map operations
      Entry<K, N> firstEntry();
      Entry<K, N> lastEntry();
      Entry<K, N> floorEntry(K key);
      Entry<K, N> ceilingEntry(K key);
      Entry<K, N> lowerEntry(K key);
      Entry<K, N> higherEntry(K key);
      Iterator<N> descendingChildIterator();
   }
   
   final Comparator<? super K> componentComparator;
   
   public AbstractNavigableTrie(Comparator<? super K> componentComparator) {
      this.componentComparator = componentComparator == null
            ? CollectionUtils.naturalOrder()
            : componentComparator;
   }
   
   protected N firstNode() {
      return findFirst(root);
   }
   
   private N findFirst(N start) {
      if (start == null || start.isEmpty()) {
         return null;
      }
      for (N node = start; node != null; node = node.firstEntry().getValue()) {
         if (node.valuePresent()) {
            return node;
         }
      }
      return null;
   }
   
   protected N lastNode() {
      return findLast(root);
   }

   static <K, X, V, N extends NavigableNode<K, X, V, N>> N findLast(N start) {
      if (start == null || start.isEmpty()) {
         return null;
      }
      N ret = null;
      for (N node = start; node != null; node = node.lastEntry().getValue()) {
         if (node.valuePresent()) {
            ret = node;
         }
      }
      return ret;
   }

   protected N floorNode(Iterable<K> keys) {
      return findFloor(keys, true);
   }

   protected N lowerNode(Iterable<K> keys) {
      return findFloor(keys, false);
   }

   private N findFloor(Iterable<K> keys, boolean acceptExactMatch) {
      // This would be so much simpler to do recursively. But we use the slightly more convoluted
      // iterative approach in order to support arbitrarily long keys (e.g. deep tries) without
      // overflowing the stack.
      if (root.isEmpty()) {
         return null;
      }
      N node = root;
      Iterator<K> iter = keys.iterator();
      boolean onPath = true;
      ArrayDeque<Pair<N, K>> path = null;
      while (true) {
         if (!iter.hasNext()) {
            if (acceptExactMatch && node.valuePresent()) {
               // found an exact match
               return node;
            }
            break;
         }
         K k = iter.next();
         Entry<K, N> entry = node.floorEntry(k);
         if (entry != null && componentComparator.compare(k, entry.getKey()) != 0) {
            onPath = false;
            // if we allocated a deque for the path, we no longer need it once we're off the path
            path = null;
            break;
         } else if (entry == null) {
            if (node.valuePresent()) {
               // No need to look for predecessors since they will be strictly less than this node.
               // This is the floor
               return node;
            }
            break;
         } else {
            if (path == null) {
               assert node == root;
               // initialize this lazily -- no need to allocate the deque until we know we might
               // need it...
               path = new ArrayDeque<>();
            }
            path.push(Pair.create(node, k));
         }
         node = entry.getValue();
         assert node != null;
      }
      
      if (onPath) {
         // Back-track up the trie to find a predecessor branch
         node = null;
         if (path != null) {
            while (!path.isEmpty()) {
               Pair<N, K> pair = path.pop();
               N n = pair.getFirst();
               if (n.valuePresent()) {
                  // A predecessor can only be less than n, so this is the floor
                  return n;
               }
               Entry<K, N> nextHighest = n.lowerEntry(pair.getSecond());
               if (nextHighest != null) {
                  // found it
                  node = nextHighest.getValue();
                  assert node != null;
                  break;
               }
            }
         }
      }
      
      // At this point, node represents the predecessor branch of the given sequence, or null if
      // there is no such branch in the trie. So we just look for the "greatest" item in this
      // sub-trie to find the floor.
      N ret = findLast(node);
      assert node == null || ret != null;
      return ret;
   }

   protected N ceilingNode(Iterable<K> keys) {
      return findCeiling(keys, true);
   }
   
   protected N higherNode(Iterable<K> keys) {
      return findCeiling(keys, false);
   }

   private N findCeiling(Iterable<K> keys, boolean acceptExactMatch) {
      // This would be so much simpler to do recursively. But we use the slightly more convoluted
      // iterative approach in order to support arbitrarily long keys (e.g. deep tries) without
      // overflowing the stack.
      if (root.isEmpty()) {
         return null;
      }
      N node = root;
      Iterator<K> iter = keys.iterator();
      boolean onPath = true;
      ArrayDeque<Pair<N, K>> path = null;
      while (true) {
         if (!iter.hasNext()) {
            if (acceptExactMatch && node.valuePresent()) {
               // found an exact match
               return node;
            }
            // otherwise, grab the smallest item from this sub-trie, and that's the ceiling
            N ret = findFirst(node);
            assert ret != null;
            return ret;
         }
         K k = iter.next();
         Entry<K, N> entry = node.ceilingEntry(k);
         if (entry != null && componentComparator.compare(k, entry.getKey()) != 0) {
            onPath = false;
            // if we allocated a deque for the path, we no longer need it once we're off the path
            path = null;
            break;
         } else if (entry == null) {
            break;
         } else {
            if (path == null) {
               assert node == root;
               // initialize this lazily -- no need to allocate the deque until we know we might
               // need it...
               path = new ArrayDeque<>();
            }
            path.push(Pair.create(node, k));
         }
         node = entry.getValue();
      }
      
      if (onPath) {
         // Back-track up the trie to find a successor branch
         node = null;
         if (path != null) {
            while (!path.isEmpty()) {
               Pair<N, K> pair = path.pop();
               N n = pair.getFirst();
               Entry<K, N> nextLowest = n.higherEntry(pair.getSecond());
               if (nextLowest != null) {
                  // found it
                  node = nextLowest.getValue();
                  assert node != null;
                  break;
               }
            }
         }
      }
      
      // At this point, node represents the successor branch of the given sequence, or null if
      // there is no such branch in the trie. So we just look for the "least" item in this sub-trie
      // to find the ceiling.
      N ret = findFirst(node);
      assert node == null || ret != null;
      return ret;
   }

   protected <T> Iterator<T> descendingEntryIterator(
         BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer) {
      return new DescendingEntryIterator<>(root, producer);
   }

   protected <T> Iterator<T> descendingEntryIteratorFrom(
         BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer,
         N startAt) {
      return new DescendingEntryIterator<>(root, producer, startAt);
   }

   // TODO: make this descend -- needs to use descendingChildIterator for each node and to visit
   // node values on the way back up the tree (popping) instead of the way down (pushing)
   protected static class DescendingEntryIterator<T, K, X, V, N extends NavigableNode<K, X, V, N>>
         implements Iterator<T> {
      final ArrayDeque<StackFrame<K, X, V, N>> frames = new ArrayDeque<>();
      final BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer;
      boolean first;
      N lastFetched;
      
      protected DescendingEntryIterator(N root,
            BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer) {
         this(root, producer, findLast(root));
      }
      
      protected DescendingEntryIterator(N root,
            BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer, N startAt) {
         while(true) {
            assert startAt != null;
            frames.push(newStackFrame(startAt));
            if (startAt == root) {
               break;
            }
            startAt = startAt.getParent();
         }
         this.producer = producer;
      }
      
      private StackFrame<K, X, V, N> newStackFrame(N node) {
         return new StackFrame<>(node, node.descendingChildIterator());
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
               return producer.apply(this, lastFetched = top.node);
            }
         }
         // Find next node:
         // First, pop off any nodes, looking for predecessor branch or present value.
         while (true) {
            StackFrame<K, X, V, N> frame = frames.pop();
            assert frame == top;
            if (frames.isEmpty()) {
               throw new NoSuchElementException();
            }
            top = frames.peek();
            if (top.iter.hasNext() || top.node.valuePresent()) {
               break;
            }
         }
         // Then drill down to next leaf/unvisited node
         while (top.iter.hasNext()) {
            N node = top.iter.next();
            frames.push(top = newStackFrame(node));
         }
         assert top.node.valuePresent();
         return producer.apply(this, lastFetched = top.node);
      }
      
      public List<K> createKeyList() {
         // TODO: Use AmtPersistentList instead of ArrayDeque so we don't need to create a new
         // list every time? Then we could instead just return a "view" of the persistent list.
         List<K> ret = new ArrayList<>(frames.size() - 1);
         boolean isFirst = true;
         for (Iterator<StackFrame<K, X, V, N>> iter = frames.descendingIterator();
               iter.hasNext(); ) {
            if (isFirst) {
               // skip the first
               isFirst = false;
               continue;
            }
            ret.add(iter.next().node.getKey());
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
         for (N n = lastFetched; n != null; n = n.getParent()) {
            n.decrementCount();
         }
         if (lastFetched.isEmpty()) {
            // Remove dead sub-tries. Must use iterators' remove() method so we don't accidentally
            // cause ConcurrentModificationExceptions on subsequent use of this iterator
            boolean isFirst = true;
            for (Iterator<StackFrame<K, X, V, N>> iter = frames.descendingIterator();
                  iter.hasNext(); ) {
               StackFrame<K, X, V, N> frame = iter.next();
               if (isFirst) {
                  assert !frame.iter.hasNext();
                  isFirst = false;
               } else {
                  frame.iter.remove();
                  if (!frame.node.isEmpty()) {
                     // the rest of the ancestors have other content, so we're done pruning
                     break;
                  }
               }
            }
         }
         lastFetched = null;
      }
   }
}
