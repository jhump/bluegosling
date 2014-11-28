package com.apriori.collections;

import java.util.Iterator;
import java.util.Objects;

/**
 * A fully persistent list backed by an array-mapped trie. This structure supports inexpensive
 * insertions both at the beginning and end of the list. Inexpensive means that only the path to the
 * first or last leaf trie node is copied in these cases. Insertions or removals from the middle,
 * however, are expensive and require linear runtime complexity and space overhead. Such operations
 * mean that much of the list must be copied to form the new list.
 *
 * @param <E> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class AmtPersistentList<E> extends AbstractRandomAccessImmutableList<E>
      implements PersistentList<E>, BidiIterable<E> {

   private static final AmtPersistentList<Object> EMPTY = new AmtPersistentList<>(null, 0, 0, 0);
   
   @SuppressWarnings("unchecked") // safe due to immutability
   public AmtPersistentList<E> create() {
      return (AmtPersistentList<E>) EMPTY;
   }

   public AmtPersistentList<E> create(Iterable<? extends E> items) {
      return create().addAll(items);
   }
   
   private interface TrieNode<E> {
   }
   
   private static class IntermediateTrieNode<E> implements TrieNode<E> {
      final TrieNode<E> children[];
      
      IntermediateTrieNode(TrieNode<E> children[]) {
         this.children = children;
      }
   }

   private static class LeafTrieNode<E> implements TrieNode<E> {
      final E elements[];

      LeafTrieNode(E elements[]) {
         this.elements = elements;
      }
   }
   
   private final TrieNode<E> root;
   private final int depth;
   private final int size;
   private final int firstElementIndex;

   private AmtPersistentList(TrieNode<E> root, int depth, int size, int firstElementIndex) {
      this.depth = depth;
      this.size = size;
      this.firstElementIndex = firstElementIndex;
      this.root = root;
   }

   @Override
   public E get(int i) {
      rangeCheck(i);
      // TODO: implement me
      return null;
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public AmtPersistentList<E> subList(int from, int to) {
      rangeCheckWide(from);
      rangeCheckWide(to);
      if (from == to) {
         return create();
      }
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> add(int i, E e) {
      if (i == size) {
         return addLast(e);
      } else if (i == 0) {
         return addFirst(e);
      }
      rangeCheck(i);
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> addAll(int i, Iterable<? extends E> items) {
      if (i == size) {
         return addAll(items);
      }
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> addFirst(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> addLast(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> set(int i, E e) {
      rangeCheck(i);
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> remove(int i) {
      rangeCheck(i);
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> rest() {
      return subList(1, size);
   }

   @Override
   public AmtPersistentList<E> add(E e) {
      return addLast(e);
   }

   @Override
   public AmtPersistentList<E> remove(Object o) {
      int i = indexOf(o);
      return i >= 0 ? remove(i) : this;
   }

   @Override
   public AmtPersistentList<E> removeAll(Object o) {
      // TODO: bulk removal?
      AmtPersistentList<E> ret = this;
      int i = 0;
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         E e = iter.next();
         if (Objects.equals(e, o)) {
            ret = ret.remove(i);
         } else {
            i++;
         }
      }
      return ret;
   }

   @Override
   public AmtPersistentList<E> removeAll(Iterable<?> items) {
      // TODO: bulk removal?
      AmtPersistentList<E> ret = this;
      for (Object o : items) {
         ret = ret.removeAll(o);
      }
      return ret;
   }

   @Override
   public AmtPersistentList<E> retainAll(Iterable<?> items) {
      // TODO: bulk removal?
      AmtPersistentList<E> ret = create();
      for (E e : this) {
         if (Iterables.contains(items, e)) {
            ret = ret.add(e);
         }
      }
      return ret;
   }

   @Override
   public AmtPersistentList<E> addAll(Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> clear() {
      return create();
   }
}
