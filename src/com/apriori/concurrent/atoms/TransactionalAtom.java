package com.apriori.concurrent.atoms;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.locks.ReadWriteLock;

// TODO: javadoc
// TODO: tests
public class TransactionalAtom<T> extends AbstractAtom<T> implements SynchronousAtom<T> {

   private static class Node<T> {
      T value;
      long pin;
      Node<T> next;
   }
   
   private Node<T> latest;
   private /* final */ ReadWriteLock lock;
   
   public TransactionalAtom() {
      // TODO: implement me
   }
   
   public TransactionalAtom(T value) {
      // TODO: implement me
   }

   public TransactionalAtom(T value, Predicate<? super T> validator) {
      // TODO: implement me
   }
   
   public <U> TransactionalAtom<U> newComponent() {
      // TODO: implement me
      return null;
   }

   public <U> TransactionalAtom<U> newComponent(U value) {
      // TODO: implement me
      return null;
   }

   public <U> TransactionalAtom<U> newComponent(U value, Predicate<? super U> validator) {
      // TODO: implement me
      return null;
   }

   @Override
   public T get() {
      // TODO: implement me
      return null;
   }
   
   public T pin() {
      // TODO: implement me
      return null;
   }

   @Override 
   public T apply(Function<? super T, ? extends T> function) {
      // TODO: implement me
      return null;
   }

   public ListenableFuture<T> commute(Function<? super T, ? extends T> function) {
      // TODO: implement me
      return null;
   }

   @Override
   public T set(T newValue) {
      // TODO: implement me
      return null;
   }
}
