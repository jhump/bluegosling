package com.apriori.concurrent.atoms;

import com.apriori.concurrent.DeadlockException;
import com.apriori.concurrent.HierarchicalLock;
import com.apriori.concurrent.HierarchicalLock.ExclusiveLock;
import com.apriori.concurrent.HierarchicalLock.SharedLock;
import com.apriori.concurrent.ListenableFuture;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

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
 * 
 * <p>Every transactional atom uses a {@link HierarchicalLock} for synchronizing access from
 * potentially multiple concurrent transactions. Transactions that pin the atom's value acquire
 * shared locks; those that modify atom's value acquire exclusive locks. Transactions are restarted
 * if a deadlock is detected when acquiring atoms' locks.
 */
public class TransactionalAtom<T> extends AbstractSynchronousAtom<T> {

   /**
    * Represents a version of this atom's value. Transactions are implemented using an MVCC, so each
    * atom stores all necessary recent versions. When updates are committed to the atom, old and
    * unnecessary versions are purged.
    * 
    * <p>Each version is a node in a linked list. The head of the list is the latest version. Each
    * node has a reference to its predecessor, the previous version.
    *
    * @param <T> the type of the atom's value
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
   
   /**
    * The lock used to acquire shared (read) or exclusive (write) access to this atom.
    */
   private final HierarchicalLock lock;
   
   /**
    * When a commit operation that affects this atom is in progress, this reference is set to the
    * transaction that is committing.
    */
   private final AtomicReference<Transaction> committer = new AtomicReference<>();
   
   /**
    * The head of the versions list, which contains the atom's current/latest value.
    */
   private volatile Version<T> latest;
   
   /**
    * Constructs a new atom with a {@code null} value and no validator.
    */
   public TransactionalAtom() {
      this(HierarchicalLock.create());
   }
   
   /**
    * Constructs a new atom with the specified value and no validator.
    */
   public TransactionalAtom(T value) {
      this(value, HierarchicalLock.create());
   }

   /**
    * Constructs a new atom with the specified value and the specified validator.
    */
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
   
   /**
    * Creates a new component atom with a {@code null} initial value and no validator. Accessing the
    * component requires shared access to this atom. For example, a transaction cannot modify this
    * atom (which requires acquiring an exclusive lock) while transactions are outstanding that
    * affect any of this atom's components (or their components, i.e. descendants).
    * 
    * <p>More precisely, the component is protected by a lock that is a child of this atom's lock
    * (see {@link HierarchicalLock}).
    *
    * @return a new atom that is a component of this atom
    */
   public <U> TransactionalAtom<U> newComponent() {
      return new TransactionalAtom<U>(lock.newChild());
   }

   /**
    * Creates a new atom with the specified initial value and no validator.
    *
    * @return a new atom that is a component of this atom
    * 
    * @see #newComponent()
    */
   public <U> TransactionalAtom<U> newComponent(U value) {
      return new TransactionalAtom<U>(value, lock.newChild());
   }

   /**
    * Creates a new atom with the specified initial value and the specified validator.
    *
    * @return a new atom that is a component of this atom
    * 
    * @see #newComponent()
    */
   public <U> TransactionalAtom<U> newComponent(U value, Predicate<? super U> validator) {
      return new TransactionalAtom<U>(value, validator, lock.newChild());
   }

   /**
    * {@inheritDoc}
    * 
    * <p>When called outside of a transaction, this returns the most recently committed value for
    * the atom. When called inside of a transaction, this may reflect updates to the atom that have
    * not yet been committed. Unless the transaction is using a "read committed" isolation level,
    * getting the atom's value in the transaction is repeatable, regardless of concurrent
    * modifications being made by other transactions.
    */
   @Override
   public T get() {
      Transaction current = Transaction.current();
      return current == null ? getLatestValue() : current.getAtom(this);
   }
   
   /**
    * Locks this atom exclusively. This is done by a transaction that is modifying this atom.
    *
    * @return the lock on this atom
    */
   ExclusiveLock exclusiveLock() {
      return lock.exclusiveLock();
   }
   
   /**
    * Locks this atom in shared mode. This is done by transactions that have pinned this atom.
    *
    * @return the lock on this atom
    */
   SharedLock sharedLock() {
      return lock.sharedLock();
   }

   /**
    * Marks the current atom with a reference to the transaction that is currently modifying it. The
    * atom is only marked when the transaction is being committed.
    *
    * @param transaction the transaction that is currently committing data to this atom
    */
   void markForCommit(Transaction transaction) {
      Transaction previous = committer.getAndSet(transaction);
      assert previous == null;
   }
   
   /**
    * Marks the current atom as not being modified. The atom is unmarked once the transaction has
    * finished committing.
    */
   void unmark() {
      // tolerate multiple unmarks, since transaction both unmarks eagerly when it can
      // but also has fail-safe code that may try to unmark again on failure
      committer.set(null);
   }

   /**
    * Returns the atom's most recently committed value.
    *
    * @return the most recently committed value
    */
   T getLatestValue() {
      return latest.value;
   }
   
   /**
    * Returns the version number associated with the most recently committed value.
    *
    * @return the version number associated with the most recently committed value
    */
   long getLatestVersion() {
      return latest.version;
   }
   
   /**
    * Returns the atom's value at the specified version number. Version numbers are system-wide. The
    * system version number is incremented every time a transaction is started and every time one is
    * committed. This method essentially walks the list of versions for this atom and returns the
    * value for the most recent version that is less than or equal to the specified version.
    * 
    * <p>If this atom is marked by an in-process transaction and the requested version number is
    * greater than the most recent version number associated with the atom, this method will block,
    * waiting on that transaction to commit. 
    *
    * @param version a version number
    * @return the atom's value at the specified version
    */
   T getValue(long version) {
      Transaction transaction = committer.get();
      Version<T> node = latest;
      if (transaction != null && version > node.version) {
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
   
   /**
    * Pins an atom so that no concurrent transaction can modify it. This can be used in
    * non-serializable isolation levels to prevent write skew that could otherwise occur. Pinning
    * is achieved by acquiring a shared lock on the atom.
    *
    * @return the value of the atom in the current transaction
    * @throws IllegalStateException if no transaction is in progress on the current thread
    * @throws DeadlockException if a deadlock is detected when trying to acquire a lock for the atom
    * @throws TransactionIsolationException if a concurrent transaction has already updated the atom
    *       since the current transaction's version was established 
    */
   public T pin() {
      Transaction current = Transaction.current();
      if (current == null) {
         throw new IllegalStateException("not currently in a transaction");
      }
      return current.pinAtom(this);
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>If no transaction is in progress on the current thread, this will effectively create one
    * that does nothing but transform this atom. If one is in progress, the transaction will acquire
    * an exclusive lock on the atom to prevent concurrent writers and will hold the lock until it is
    * rolled back or committed.
    * 
    * <p>Watchers are only notified when this operation is committed. If there are other operations
    * on this same atom, watchers will only be notified once with the value of this atom before the
    * transaction began and its final value upon the transaction being committed.
    * 
    * @throws DeadlockException if a deadlock is detected when trying to acquire a lock for the atom
    * @throws TransactionIsolationException if a transaction is in progress, and a concurrent
    *       transaction has updated the atom since the current transaction's version was established 
    */
   @Override
   public T getAndUpdate(Function<? super T, ? extends T> function) {
      return super.getAndUpdate(function);
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>If no transaction is in progress on the current thread, this will effectively create one
    * that does nothing but transform this atom. If one is in progress, the transaction will acquire
    * an exclusive lock on the atom to prevent concurrent writers and will hold the lock until it is
    * rolled back or committed.
    * 
    * <p>Watchers are only notified when this operation is committed. If there are other operations
    * on this same atom, watchers will only be notified once with the value of this atom before the
    * transaction began and its final value upon the transaction being committed.
    * 
    * @throws DeadlockException if a deadlock is detected when trying to acquire a lock for the atom
    * @throws TransactionIsolationException if a transaction is in progress, and a concurrent
    *       transaction has updated the atom since the current transaction's version was established 
    */
   @Override
   public T updateAndGet(Function<? super T, ? extends T> function) {
      return super.updateAndGet(function);
   }

   @Override 
   T update(Function<? super T, ? extends T> function, boolean returnNew) {
      Transaction current = Transaction.current();
      T oldValue;
      T newValue;
      if (current == null) {
         ExclusiveLock exclusive = lock.exclusiveLock();
         try {
            oldValue = latest.value;
            newValue = function.apply(oldValue);
            if (newValue == oldValue) {
               return oldValue;
            }
            validate(newValue);
            long version = Transaction.pinNewVersion();
            try {
               addValue(newValue, version, Transaction.oldestVersion());
            } finally {
               Transaction.unpinVersion(version);
            }
         } finally {
            exclusive.unlock();
         }
         notify(oldValue, newValue);
      } else {
         oldValue = current.getAtom(this);
         newValue = function.apply(oldValue);
         if (newValue == oldValue) {
            return oldValue;
         }
         validate(newValue);
         current.setAtom(this, newValue);
      }
      return returnNew ? newValue : oldValue;
   }

   /**
    * Enqueues a commutative transform operation. This differs from {@link #updateAndGet(Function)}
    * in that commutes do not eagerly acquire an exclusive lock on the atom. This allows for
    * concurrent transactions to also be modifying the same atom. But since the function must be
    * commutative, the order of applying functions does not matter. Using this method can lead to
    * greater throughput than using {@link #updateAndGet(Function)} but is more restrictive since
    * the applied operation must be commutative.
    * 
    * <p>Note that if other operations are performed on this atom (like {@link #set} or
    * {@link #updateAndGet}), commutes are always applied <strong>last</strong>, regardless of
    * whether the commute was enqueued after the other operations were performed or not.
    * 
    * <p>If no transaction is in progress on the current thread, this will effectively create one
    * that does nothing but transform this atom. If a serializable transaction is in progress, then
    * an exclusive lock is acquired. So, with serializable isolation level, the only difference
    * between this operation and {@link #updateAndGet(Function)} is that the commute functions won't
    * actually be evaluated until the transaction is committed.
    * 
    * <p>Watchers are only notified when this operation is committed. If there are other operations
    * on this same atom, watchers will only be notified once with the value of this atom before the
    * transaction began and its final value upon the transaction being committed.
    *
    * @param function a commutative function to apply to transform the atom's value
    * @return a future that will complete with the result of the commute operation when the
    *       transaction is committed or will be cancelled if this operation is rolled back
    * @throws DeadlockException if a deadlock is detected when trying to acquire a lock for the atom
    *       (only when a transaction is in progress with serializable isolation)
    * @throws TransactionIsolationException if a transaction is in progress, and a concurrent
    *       transaction has updated the atom since the current transaction's version was established
    *       (only when a transaction is in progress with serializable isolation) 
    */
   public ListenableFuture<T> commute(Function<? super T, ? extends T> function) {
      Transaction current = Transaction.current();
      if (current == null) {
         T oldValue, newValue;
         ExclusiveLock exclusive = lock.exclusiveLock();
         try {
            oldValue = latest.value;
            newValue = function.apply(oldValue);
            if (newValue == oldValue) {
               return ListenableFuture.completedFuture(oldValue);
            }
            validate(newValue);
            long version = Transaction.pinNewVersion();
            try {
               addValue(newValue, version, Transaction.oldestVersion());
            } finally {
               Transaction.unpinVersion(version);
            }
         } finally {
            exclusive.unlock();
         }
         notify(oldValue, newValue);
         return ListenableFuture.completedFuture(newValue);
      } else {
         return current.enqueueCommute(this, function);
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>If no transaction is in progress on the current thread, this will effectively create one
    * that does nothing but set this atom's value. If one is in progress, the transaction will
    * acquire an exclusive lock on the atom to prevent concurrent writers and will hold the lock
    * until it is rolled back or committed.
    * 
    * <p>Watchers are only notified when this operation is committed. If there are other operations
    * on this same atom, watchers will only be notified once with the value of this atom before the
    * transaction began and its final value upon the transaction being committed.
    * 
    * @throws DeadlockException if a deadlock is detected when trying to acquire a lock for the atom
    * @throws TransactionIsolationException if a transaction is in progress, and a concurrent
    *       transaction has updated the atom since the current transaction's version was established 
    */
   @Override
   public T set(T newValue) {
      validate(newValue);
      Transaction current = Transaction.current();
      if (current == null) {
         T oldValue;
         ExclusiveLock exclusive = lock.exclusiveLock();
         try {
            oldValue = latest.value;
            if (newValue == oldValue) {
               return oldValue;
            }
            long version = Transaction.pinNewVersion();
            try {
               addValue(newValue, version, Transaction.oldestVersion());
            } finally {
               Transaction.unpinVersion(version);
            }
         } finally {
            exclusive.unlock();
         }
         notify(oldValue, newValue);
         return oldValue;
      } else {
         return current.setAtom(this, newValue);
      } 
   }

   /**
    * Adds a new value to this atom. The caller must hold this atom's lock in exclusive mode. Any
    * versions older than the given oldest active version number will be purged.
    *
    * @param newValue the atom's new value
    * @param version the version number associated with this new value
    * @param oldestVersion the oldest active version number
    * @return the atom's most recent value prior to adding this new one
    */
   T addValue(T newValue, long version, long oldestVersion) {
      // must hold exclusive lock on this atom when calling this method!
      assert lock.getExclusiveHolder() == Thread.currentThread();
      
      assert oldestVersion <= version;
      Version<T> node = latest;
      assert version > node.version;
      T ret = node.value;
      node = latest = new Version<T>(newValue, version, node);
      while (node != null) {
         if (node.version <= oldestVersion) {
            // remove values that are old and will never be referenced again
            node.predecessor = null;
            break;
         }
         node = node.predecessor;
      }
      return ret;
   }
}
