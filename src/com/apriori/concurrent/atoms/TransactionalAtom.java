package com.apriori.concurrent.atoms;

import com.apriori.concurrent.HierarchicalLock;
import com.apriori.concurrent.ListenableFuture;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

// TODO: javadoc
// TODO: tests
public class TransactionalAtom<T> extends AbstractAtom<T> implements SynchronousAtom<T> {

   private static class Version<T> {
      final T value;
      final long version;
      volatile Version<T> predecessor;
      
      Version(T value, long version, Version<T> predecessor) {
         this.value = value;
         this.version = version;
         this.predecessor = predecessor;
      }
   }
   
   private final HierarchicalLock lock;
   private volatile Version<T> latest;
   
   public TransactionalAtom() {
      this(HierarchicalLock.create());
   }
   
   public TransactionalAtom(T value) {
      this(value, HierarchicalLock.create());
   }

   public TransactionalAtom(T value, Predicate<? super T> validator) {
      this(value, validator, HierarchicalLock.create());
   }
   
   private TransactionalAtom(HierarchicalLock lock) {
      this.lock = lock;
      // TODO: implement me
   }
   
   private TransactionalAtom(T value, HierarchicalLock lock) {
      this.lock = lock;
      // TODO: implement me
   }

   private TransactionalAtom(T value, Predicate<? super T> validator, HierarchicalLock lock) {
      this.lock = lock;
      // TODO: implement me
   }
   
   public <U> TransactionalAtom<U> newComponent() {
      return new TransactionalAtom<U>(lock.newChild());
   }

   public <U> TransactionalAtom<U> newComponent(U value) {
      return new TransactionalAtom<U>(value, lock.newChild());
   }

   public <U> TransactionalAtom<U> newComponent(U value, Predicate<? super U> validator) {
      return new TransactionalAtom<U>(value, validator, lock.newChild());
   }

   @Override
   public T get() {
      Transaction current = Transaction.current();
      return current == null ? getLatestValue() : current.getAtom(this);
   }
   
   T getLatestValue() {
      return latest.value;
   }
   
   T getValue(long version) {
      for (Version<T> node = latest; node != null; node = node.predecessor) {
         if (node.version <= version) {
            return node.value;
         }
      }
      throw new AssertionError("value for requested version no longer exists");
   }
   
   public T pin() {
      Transaction current = Transaction.current();
      if (current == null) {
         throw new IllegalStateException("not currently in a transaction");
      }
      return current.pinAtom(this);
   }

   @Override 
   public T apply(final Function<? super T, ? extends T> function) {
      Transaction current = Transaction.current();
      if (current == null) {
         // TODO: write lock and then set value directly
         return null;
      } else {
         T newValue = function.apply(current.getAtom(this));
         validate(newValue);
         current.setAtom(this, newValue);
         return newValue;
      }
   }

   public ListenableFuture<T> commute(Function<? super T, ? extends T> function) {
      Transaction current = Transaction.current();
      if (current == null) {
         // TODO: write lock and then set value directly
         return null;
      } else {
         return current.enqueueCommute(this, function);
      }
   }

   @Override
   public T set(T newValue) {
      validate(newValue);
      Transaction current = Transaction.current();
      if (current == null) {
         // TODO: write lock and then set value directly
         return null;
      } else {
         current.setAtom(this, newValue);
         return newValue;
      } 
   }
   
   // must hold write lock when calling this method
   void addValue(T newValue, long version, long oldestVersion) {
      Version<T> node = new Version<T>(newValue, version, latest);
      latest = node;
      while (node != null) {
         if (node.version < oldestVersion) {
            // remove values that are old and will never be referenced again
            node.predecessor = null;
            break;
         }
         node = node.predecessor;
      }
   }
}
