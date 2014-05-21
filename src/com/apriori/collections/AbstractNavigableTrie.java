package com.apriori.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

//TODO: javadoc
abstract class AbstractNavigableTrie<K, X, V, N extends AbstractNavigableTrie.NavigableNode<K, X, V, N>>
      extends AbstractTrie<K, X, V, N> {
   
   interface NavigableNode<K, X, V, N extends NavigableNode<K, X, V, N>> extends Node<K, X, V, N> {
      // navigable map operations
      Entry<K, N> firstEntry();
      Entry<K, N> lastEntry();
      Entry<K, N> floorEntry(K key);
      Entry<K, N> ceilingEntry(K key);
      Entry<K, N> lowerEntry(K key);
      Entry<K, N> higherEntry(K key);
      Iterator<N> descendingChildIterator();
   }
   
   protected N firstNode() {
      // TODO
      return null;
   }
   
   protected N lastNode() {
      // TODO
      return null;
   }

   protected N floorNode(Iterable<K> keys) {
      // TODO
      return null;
   }

   protected N ceilingNode(Iterable<K> keys) {
      // TODO
      return null;
   }
   
   protected N lowerNode(Iterable<K> keys) {
      // TODO
      return null;
   }

   protected N higherNode(Iterable<K> keys) {
      // TODO
      return null;
   }

   protected <T> Iterator<T> descendingEntryIterator(
         BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer) {
      return new DescendingEntryIterator<>(root, producer);
   }

   // TODO: make this descend -- needs to use descendingChildIterator for each node and to visit
   // node values on the way back up the tree (popping) instead of the way down (pushing)
   protected static class DescendingEntryIterator<T, K, X, V, N extends NavigableNode<K, X, V, N>>
         implements Iterator<T> {
      final ArrayDeque<StackFrame<K, X, V, N>> frames = new ArrayDeque<>();
      final BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer;
      boolean first;
      N lastFetched;
      
      DescendingEntryIterator(N root,
            BiFunction<DescendingEntryIterator<T, K, X, V, N>, N, T> producer) {
         frames.push(newStackFrame(root));
         this.producer = producer;
      }
      
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
               return producer.apply(this, lastFetched = top.node);
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
         return producer.apply(this, lastFetched = node);
      }
      
      public List<K> createKeyList() {
         // TODO: Use AmtPersistentList instead of ArrayDeque so we don't need to create a new
         // list every time. We could instead just return a "view" of the persistent list.
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
