package com.apriori.collections;

// TODO: javadoc
// TODO: tests
public class HamtPersistentList<E> implements PersistentList<E>, BidiIterable<E> {

   private interface TrieNode<E> {
      // TODO
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
   
   private final int depth;
   private final int size;
   private final TrieNode<E> root;
   
   private HamtPersistentList(int depth, int size, TrieNode<E> root) {
      this.depth = depth;
      this.size = size;
      this.root = root;
   }
   
   @Override
   public E get(int i) {
      // TODO: implement me
      return null;
   }

   @Override
   public int indexOf(Object o) {
      // TODO: implement me
      return 0;
   }

   @Override
   public int lastIndexOf(Object o) {
      // TODO: implement me
      return 0;
   }

   @Override
   public E first() {
      // TODO: implement me
      return null;
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
   public BidiListIterator<E> iterator() {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> subList(int from, int to) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> add(int i, E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addAll(int i, Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addFirst(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addLast(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> set(int i, E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> remove(int i) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> rest() {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> add(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> remove(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> removeAll(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> removeAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> retainAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addAll(Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }
}
