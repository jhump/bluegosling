package com.apriori.collections;

import java.util.Iterator;

// TODO: javadoc
// TODO: tests
public class HamtPersistentSet<E> implements PersistentSet<E> {

   private static class ListNode<E> {
      final E element;
      final ListNode<E> next;
      
      ListNode(E element) {
         this(element, null);
      }
      
      ListNode(E element, ListNode<E> next) {
         this.element = element;
         this.next = next;
      }
   }
   
   private static class PutResult<E> {
      PutResult() {
      }
      
      TrieNode<E> node;
      boolean added;
   }
   
   private interface TrieNode<E> {/*
      ListNode<E> findNode(int hash, int currentOffset, Object o);
      TrieNode<E> remove(int hash, int currentOffset, Object o);
      void put(int hash, int currentOffset, E element, PutResult<E> result);
   */}
   
   private static class IntermediateTrieNode<E> implements TrieNode<E> {
      final long present;
      final TrieNode<E> children[];
      
      IntermediateTrieNode(long present, TrieNode<E> children[]) {
         this.present = present;
         this.children = children;
      }
   }

   private static class InnerLeafTrieNode<E> extends ListNode<E> implements TrieNode<E> {
      final int hashCode;
      
      InnerLeafTrieNode(E element, int hashCode) {
         super(element);
         this.hashCode = hashCode;
      }

      InnerLeafTrieNode(E element, ListNode<E> next, int hashCode) {
         super(element, next);
         this.hashCode = hashCode;
      }
   }

   private static class LeafTrieNode<E> extends ListNode<E> implements TrieNode<E> {
      LeafTrieNode(E element) {
         super(element);
      }

      LeafTrieNode(E element, ListNode<E> next) {
         super(element, next);
      }
   }
   
   public static <E> HamtPersistentSet<E> create() {
      // TODO
      return null;
   }
   
   public static <E> HamtPersistentSet<E> create(Iterable<E> iterable) {
      // TODO
      return null;
   }
   
   private final int size;
   private final TrieNode<E> root;
   
   private HamtPersistentSet(int size, TrieNode<E> root) {
      this.size = size;
      this.root = root;
   }
   
   @Override
   public int size() {
      // TODO: implement me
      return 0;
   }

   @Override
   public boolean isEmpty() {
      // TODO: implement me
      return false;
   }

   @Override
   public Object[] toArray() {
      // TODO: implement me
      return null;
   }

   @Override
   public <T> T[] toArray(T[] array) {
      // TODO: implement me
      return null;
   }

   @Override
   public boolean contains(Object o) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsAll(Iterable<?> items) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      // TODO: implement me
      return false;
   }

   @Override
   public Iterator<E> iterator() {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentSet<E> add(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentSet<E> remove(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentSet<E> removeAll(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentSet<E> removeAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentSet<E> retainAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentSet<E> addAll(Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }
}
