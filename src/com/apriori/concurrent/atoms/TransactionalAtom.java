package com.apriori.concurrent.atoms;

import com.apriori.concurrent.HierarchicalLock;
import com.apriori.concurrent.HierarchicalLock.ExclusiveLock;
import com.apriori.concurrent.HierarchicalLock.SharedLock;
import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.ListenableFutures;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An atom whose updates are coordinated in a {@link Transaction}. Transactions allow updates to
 * multiple atoms to be done atomically.
 * 
 * <p>When a transaction is committed, {@linkplain Atom.Watcher watchers} are only notified of the
 * new final value. If the transaction modified a single atom multiple times, watchers will not see
 * the intermediate values.
 * 
 * <p>When a transaction is rolled back, all futures corresponding to rolled back commute operations
 * are cancelled.
 */
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
   private AtomicReference<Transaction> committer;
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
      this(null, lock);
   }
   
   private TransactionalAtom(T value, HierarchicalLock lock) {
      this(value, null, lock);
   }

   private TransactionalAtom(T value, Predicate<? super T> validator, HierarchicalLock lock) {
      super(validator);
      validate(value);
      this.lock = lock;
      latest = new Version<T>(value, Transaction.currentVersion(), null);
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
   
   ExclusiveLock exclusiveLock() {
      return lock.exclusiveLock();
   }
   
   void markForCommit(Transaction transaction) {
      Transaction previous = committer.getAndSet(transaction);
      assert previous == null;
   }
   
   void unmark() {
      // tolerate multiple unmarks, since transaction both unmarks eagerly when it can
      // but also has fail-safe code that may try to unmark again on failure
      committer.set(null);
   }

   SharedLock sharedLock() {
      return lock.sharedLock();
   }

   T getLatestValue() {
      return latest.value;
   }
   
   long getLatestVersion() {
      return latest.version;
   }
   
   T getValue(long version) {
      Version<T> node = latest;
      Transaction transaction = committer.get();
      if (transaction != null && version > latest.version) {
         transaction.awaitCommit(version);
         node = latest; // re-read latest now that commit is complete
      }
      for (; node != null; node = node.predecessor) {
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
         ExclusiveLock exclusive = lock.exclusiveLock();
         try {
            T newValue = function.apply(latest.value);
            validate(newValue);
            long version = Transaction.pinNewVersion();
            addValue(newValue, version, Transaction.oldestVersion());
            Transaction.unpinVersion(version);
            return newValue;
         } finally {
            exclusive.unlock();
         }
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
         ExclusiveLock exclusive = lock.exclusiveLock();
         try {
            T newValue = function.apply(latest.value);
            validate(newValue);
            long version = Transaction.pinNewVersion();
            addValue(newValue, version, Transaction.oldestVersion());
            Transaction.unpinVersion(version);
            return ListenableFutures.completedFuture(newValue);
         } finally {
            exclusive.unlock();
         }
      } else {
         return current.enqueueCommute(this, function);
      }
   }

   @Override
   public T set(T newValue) {
      validate(newValue);
      Transaction current = Transaction.current();
      if (current == null) {
         ExclusiveLock exclusive = lock.exclusiveLock();
         try {
            T oldValue = latest.value;
            long version = Transaction.pinNewVersion();
            addValue(newValue, version, Transaction.oldestVersion());
            Transaction.unpinVersion(version);
            return oldValue;
         } finally {
            exclusive.unlock();
         }
      } else {
         return current.setAtom(this, newValue);
      } 
   }

   // must hold write lock when calling this method
   T addValue(T newValue, long version, long oldestVersion) {
      Version<T> node = latest;
      T ret = latest.value;
      node = new Version<T>(newValue, version, node);
      while (node != null) {
         if (node.version < oldestVersion) {
            // remove values that are old and will never be referenced again
            node.predecessor = null;
            break;
         }
         node = node.predecessor;
      }
      return ret;
   }
}
