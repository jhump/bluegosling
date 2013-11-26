package com.apriori.concurrent.atoms;

import com.apriori.concurrent.DeadlockException;
import com.apriori.concurrent.HierarchicalLock.AcquiredLock;
import com.apriori.concurrent.HierarchicalLock.ExclusiveLock;
import com.apriori.concurrent.HierarchicalLock.SharedLock;
import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.ListenableFutureTask;
import com.apriori.concurrent.SettableListenableFuture;
import com.apriori.tuples.Pair;
import com.apriori.tuples.Trio;
import com.apriori.util.Function;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A transaction in which changes to {@link TransactionalAtom}s can be made. Transactions use
 * multi-version concurrency control to provide a level of isolation between concurrently executing
 * transactions. The isolation level is configurable at the time of creating a new transaction.
 * 
 * <p>Once a transaction has begun, it can be check-pointed or rolled back, just like transactions
 * in an RDBMS. If a given block that is executed in a transaction completes successfully, then the
 * changes made in the transaction are committed. Otherwise, all changes are rolled back and the
 * exception that was thrown from the block is propagated.
 * 
 * <p>{@link AsynchronousAtom}s also participate in transactions. Changes that are queued for an
 * {@link AsynchronousAtom} are only submitted for execution when the transaction commits. Any
 * pending operations that are effected by a rollback will be cancelled.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class Transaction {
   
   /**
    * The isolation level of a transaction.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public enum IsolationLevel {
      /**
       * Serializable isolation. This is the strongest type of isolation and guarantees that the
       * state of the database would be as if all transactions were executed sequentially.
       * 
       * <p>This can result in lower throughput and greater contention since it means that the
       * transaction running in this mode must acquire exclusive locks, even for atoms that it is
       * just reading (in order to prevent "write skew").
       */
      SERIALIZABLE,
      
      /**
       * Repeatable read isolation; aka snapshot isolation. This is close to serializable, but can
       * provide better performance since locks aren't required for every read. The main artifact
       * that can occur is called "write skew". But this can be mitigated in updates to a
       * transactional atom by using its {@link TransactionalAtom#commute(Function)
       * commute(Function)} method.
       * 
       * <p>This is the default isolation level for a transaction when not otherwise configured.
       */
      REPEATABLE_READ,
      
      /**
       * Read committed isolation. This only provides locking for write operations and does not
       * really isolate query operations. In a transaction, subsequent reads of the same atom may
       * return different results. This can provide the best performance, since a particular version
       * of an atom does not have to be found on reads, but is likely to only be marginal
       * improvement over {@link #REPEATABLE_READ}. Repeatable reads are still possible for any
       * single atom via pinning it.
       */
      READ_COMMITTED
   }
   
   /**
    * A task that executes in (or with) a transaction.
    *
    * @param <X> a type of exception that the task may throw
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Task<X extends Throwable> {
      /**
       * Executes the task with the specified transaction.
       *
       * @param t the current transaction, in which the task is run
       * @throws X 
       */
      void execute(Transaction t) throws X;
   }
   
   /**
    * A task to execute in a transaction that does not throw any checked exceptions.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface UncheckedTask extends Task<RuntimeException> {
   }

   /**
    * A computation that produces a value and executes in (or with) a transaction.
    *
    * @param <T> the type of value produced
    * @param <X> the type of exception that the computation may throw
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Computation<T, X extends Throwable> {
      /**
       * Executes the computation in the specified transaction.
       *
       * @param t the current transaction, in which the computation is run
       * @return the result of the computation
       * @throws X 
       */
      T compute(Transaction t) throws X;
   }

   /**
    * A computation that produces a value, executes in a transaction, and does not throw any checked
    * exceptions.
    *
    * @param <T> the type of value produced
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface UncheckedComputation<T> extends Computation<T, RuntimeException> {
   }
   
   /**
    * Adapts a {@link Task} as a {@link Computation}.
    *
    * @param task the task
    * @return a computation that will execute the task and return {@code null}
    */
   static <X extends Throwable> Computation<Void, X> asComputation(final Task<X> task) {
      return new Computation<Void, X>() {
         @Override
         public Void compute(Transaction t) throws X {
            task.execute(t);
            return null;
         }
      };
   }
   
   /**
    * An object that can initiate execution of code in a transaction. The isolation level of the
    * transaction as well as thresholds for certain types of failures can be defined prior to
    * running the transaction.
    * 
    * <p>If otherwise unspecified, the default isolation level is
    * {@link IsolationLevel#REPEATABLE_READ}. Similarly, if unspecified, the default maximum number
    * of isolation failures is 1000, and the default maximum number of deadlock failures is 10. 
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Runner {
      /**
       * The isolation level to use in the transaction.
       */
      private IsolationLevel isolationLevel = IsolationLevel.REPEATABLE_READ;
      
      /**
       * The maximum number of isolation failures before the transaction is aborted.
       */
      private int maxIsolationFailures = 1000;
      
      /**
       * The maximum number of deadlock failures before the transaction is aborted.
       */
      private int maxDeadlockFailures = 10;
      
      /**
       * Runs transactions using the specified isolation level.
       *
       * @param level the isolation level in which to run transactions
       * @return this, for method chaining
       * @throws NullPointerException is the specified isolation level is null
       */
      public Runner withIsolationLevel(IsolationLevel level) {
         if (level == null) {
            throw new NullPointerException();
         }
         this.isolationLevel = level;
         return this;
      }
      
      /**
       * Defines the maximum number of isolation failures in a transaction. Such a failure occurs
       * when a {@link TransactionIsolationException} is thrown while running the transaction. After
       * this many failures have been observed, the transaction is aborted and the last observed
       * exception is propagated. If the transaction is not idempotent, the specified value is
       * ignored as such transactions are never re-tried.
       * 
       * <p>The minimum valid value is one, which means that after observing just one such failure,
       * the transaction is aborted.
       *
       * @param maxFailures the maximum number of isolation failures
       * @return this, for method chaining
       * @throws IllegalArgumentException if the specified number of failures is less than one
       */
      public Runner maxIsolationFailures(int maxFailures) {
         if (maxFailures < 1) {
            throw new IllegalArgumentException(maxFailures + " < 1");
         }
         this.maxIsolationFailures = maxFailures;
         return this;
      }
      
      /**
       * Defines the maximum number of deadlock failures in a transaction. Such a failure occurs
       * when a {@link DeadlockException} is thrown while running the transaction. After this many
       * failures have been observed, the transaction is aborted and the last observed exception is
       * propagated. If the transaction is not idempotent, the specified value is ignored as such
       * transactions are never re-tried.
       * 
       * <p>The minimum valid value is one, which means that after observing just one such failure,
       * the transaction is aborted.
       *
       * @param maxFailures the maximum number of deadlock failures
       * @return this, for method chaining
       * @throws IllegalArgumentException if the specified number of failures is less than one
       */
      public Runner maxDeadlockFailures(int maxFailures) {
         if (maxFailures < 1) {
            throw new IllegalArgumentException(maxFailures + " < 1");
         }
         this.maxDeadlockFailures = maxFailures;
         return this;
      }
      
      /**
       * Performs the specified computation in a transaction and returns its result. The computation
       * is assumed to be idempotent, which means may be re-tried if certain failures are observed.
       * If such failures happen too many times, exceptions are propagated.
       * 
       * <p>If the computation has side effects, then it may not tolerate being re-tried and may
       * not be idempotent. For such computations, use {@link #computeNonIdempotent(Computation)}
       * instead.
       *
       * @param computation the computation to perform in a transaction
       * @return the result of the computation
       * @throws IllegalStateException if a transaction is already in progress on the current thread
       * @throws X a type of exception thrown by the computation
       * @throws TransactionIsolationException if there is too much contention over the atoms such
       *       that the computation cannot be performed with the proper isolation level
       * @throws DeadlockException if this computation generated deadlock exceptions when accessing
       *       and modifying atoms
       */
      public <T, X extends Throwable> T compute(Computation<T, X> computation) throws X {
         return new Transaction(isolationLevel, maxIsolationFailures, maxDeadlockFailures)
               .transact(computation);
      }
   
      /**
       * Performs the specified task in a transaction. The task is assumed to be idempotent, which
       * means may be re-tried if certain failures are observed. If such failures happen too many
       * times, exceptions are propagated.
       * 
       * <p>If the task has side effects, then it may not tolerate being re-tried and may not be
       * idempotent. For such tasks, use {@link #executeNonIdempotent(Task)} instead.
       *
       * @param task the task to perform in a transaction
       * @throws IllegalStateException if a transaction is already in progress on the current thread
       * @throws X a type of exception thrown by the task
       * @throws TransactionIsolationException if there is too much contention over the atoms such
       *       that the task cannot be performed with the proper isolation level
       * @throws DeadlockException if this task generated deadlock exceptions when accessing
       *       and modifying atoms
       */
      public <X extends Throwable> void execute(Task<X> task) throws X {
         compute(asComputation(task));
      }
   
      /**
       * Performs the specified computation in a transaction and returns its result. The computation
       * will not be re-tried.
       * 
       * <p>If the computation has no side effects and is idempotent, then it is less likely to be
       * aborted if you use {@link #compute(Computation)} instead.
       *
       * @param computation the computation to perform in a transaction
       * @return the result of the computation
       * @throws IllegalStateException if a transaction is already in progress on the current thread
       * @throws X a type of exception thrown by the computation
       * @throws TransactionIsolationException if there is contention over the atoms that prevent
       *       the computation from running with the proper isolation level
       * @throws DeadlockException if this computation generated a deadlock exception when accessing
       *       and modifying atoms
       */
      public <T, X extends Throwable> T computeNonIdempotent(Computation<T, X> computation)
            throws X {
         return new Transaction(isolationLevel, 1, 1).transact(computation);
      }
   
      /**
       * Performs the specified task in a transaction. The task will not be re-tried.
       * 
       * <p>If the task has no side effects and is idempotent, then it is less likely to be aborted
       * if you use {@link #execute(Task)} instead.
       *
       * @param task the task to perform in a transaction
       * @throws IllegalStateException if a transaction is already in progress on the current thread
       * @throws X a type of exception thrown by the task
       * @throws TransactionIsolationException if there is contention over the atoms that prevent
       *       the task from running with the proper isolation level
       * @throws DeadlockException if this task generated a deadlock exception when accessing and
       *       modifying atoms
       */
      public <X extends Throwable> void executeNonIdempotent(Task<X> task) throws X {
         computeNonIdempotent(asComputation(task));
      }
   }
   
   /**
    * Runs the specified computation in a transaction and returns the result. This uses default
    * settings for the transaction and is equivalent to the following:<pre>
    * new Transaction.Runner().compute(computation);
    * </pre>
    *
    * @param computation the computation to perform in a transaction
    * @return the result of the computation
    * @throws IllegalStateException if a transaction is already in progress on the current thread
    * @throws X a type of exception thrown by the computation
    * @throws TransactionIsolationException if there is too much contention over the atoms such that
    *       the computation cannot be performed with the proper isolation level
    * @throws DeadlockException if this computation generated deadlock exceptions when accessing and
    *       modifying atoms
    */
   public static <T, X extends Throwable> T compute(Computation<T, X> computation) throws X {
      return new Runner().compute(computation);
   }

   /**
    * Runs the specified task in a transaction. This uses default settings for the transaction and
    * is equivalent to the following:<pre>
    * new Transaction.Runner().execute(task);
    * </pre>
    *
    * @param task the task to perform in a transaction
    * @throws IllegalStateException if a transaction is already in progress on the current thread
    * @throws X a type of exception thrown by the task
    * @throws TransactionIsolationException if there is too much contention over the atoms such that
    *       the task cannot be performed with the proper isolation level
    * @throws DeadlockException if this task generated deadlock exceptions when accessing and
    *       modifying atoms
    */
   public static <X extends Throwable> void execute(Task<X> task) throws X {
      new Runner().execute(task);
   }

   /**
    * Runs the specified computation in a transaction and returns the result. This uses default
    * settings for the transaction and is equivalent to the following:<pre>
    * new Transaction.Runner().computeNonIdempotent(computation);
    * </pre>
    *
    * @param computation the computation to perform in a transaction
    * @return the result of the computation
    * @throws IllegalStateException if a transaction is already in progress on the current thread
    * @throws X a type of exception thrown by the computation
    * @throws TransactionIsolationException if there is contention over the atoms that prevent the
    *       computation from running with the proper isolation level
    * @throws DeadlockException if this computation generated a deadlock exception when accessing
    *       and modifying atoms
    */
   public static <T, X extends Throwable> T computeNonIdempotent(Computation<T, X> computation)
         throws X {
      return new Runner().computeNonIdempotent(computation);
   }

   /**
    * Runs the specified task in a transaction. This uses default settings for the transaction and
    * is equivalent to the following:<pre>
    * new Transaction.Runner().executeNonIdempotent(task);
    * </pre>
    *
    * @param task the task to perform in a transaction
    * @throws IllegalStateException if a transaction is already in progress on the current thread
    * @throws X a type of exception thrown by the task
    * @throws TransactionIsolationException if there is contention over the atoms that prevent the
    *       task from running with the proper isolation level
    * @throws DeadlockException if this task generated a deadlock exception when accessing and
    *       modifying atoms
    */
   public static <X extends Throwable> void executeNonIdempotent(Task<X> task) throws X {
      new Runner().executeNonIdempotent(task);
   }

   /**
    * Determines whether a transaction is in progress on the current thread.
    *
    * @return true if a transaction is in progress on the current thread
    */
   public static boolean isInTransaction() {
      return current() != null;
   }
   
   /**
    * Gets the transaction that is in progress on the current thread
    *
    * @return the current transaction or null if one is not in progress on the current thread
    */
   static Transaction current() {
      return currentTransaction.get();
   }
   
   /**
    * Gets the oldest version that is pinned by in-progress transactions. Version numbers are
    * monotonically increasing.
    *
    * @return the oldest version pinned by in-progress transactions
    */
   static long oldestVersion() {
      Map.Entry<Long, ?> entry = pinnedVersions.firstEntry();
      return entry == null ? versionNumber.get() : entry.getKey();
   }
   
   /**
    * Gets the current version number. This represents the state of data at the current moment. Any
    * modifications will result in new version numbers. Version numbers are monotonically
    * increasing.
    *
    * @return the current (latest) version number
    */
   static long currentVersion() {
      return versionNumber.get();
   }
   
   /**
    * Pins the specified version. While pinned, atom values associated with this version will be
    * retained.
    *
    * @param version the version to pin
    */
   private static void pinVersion(long version) {
      while (true) {
         AtomicInteger count = pinnedVersions.get(version);
         if (count == null) {
            count = new AtomicInteger(1);
            AtomicInteger existing = pinnedVersions.putIfAbsent(version, count);
            if (existing == null) {
               return;
            }
            // lost race, so try to use the one already there
            count = existing;
         }
         while (true) {
            int currentValue = count.get();
            if (currentValue == 0) {
               // zero? there is a concurrent removal, so try again
               break;
            }
            if (count.compareAndSet(currentValue, currentValue + 1)) {
               // successfully incremented; if there was a race with removal, we won
               return;
            }
         }
      }
   }
   
   /**
    * Unpins the specified version. The version must have first been {@linkplain #pinVersion(long)
    * pinned}. Once unpinned, atom values associated with this version may be removed.
    *
    * @param version the version to unpin
    */
   static void unpinVersion(long version) {
      if (pinnedVersions.get(version).decrementAndGet() == 0) {
         pinnedVersions.remove(version);
      }
   }

   /**
    * Creates a new version number and pins it. The caller must unpin the returned version number
    * when it is no longer needed.
    *
    * @return the new version number
    */
   static long pinNewVersion() {
      // If we just do volatile read of versionNumber and then pin it, there's a race where a
      // concurrent commit could increment the versionNumber and, not seeing any pins for prior
      // version, remove old versions of atom values that we are trying to pin. So we first put down
      // a value, then get a new version number for this transaction and pin it, and then release
      // old pin. That way we will never accidentally use a version number for which we have no
      // data.
      long last = currentVersion();
      pinVersion(last);
      long ret = versionNumber.incrementAndGet();
      pinVersion(ret);
      unpinVersion(last);
      return ret;
   }

   /**
    * Adjusts the held lock for a given atom. The atom's lock may be acquired, released, promoted,
    * or demoted based on the given source and target lock states. If a new object is acquired, it
    * will be stored in the specified map, associated with the given atom.
    *
    * @param atom the atom whose lock state is being modified
    * @param current the current state of this atom
    * @param desired the desired state of this atom
    * @param locks a map of atoms to their currently held lock object
    */
   static void realizeLockState(TransactionalAtom<?> atom, LockState current, LockState desired,
         Map<TransactionalAtom<?>, AcquiredLock> locks) {
      if (current == desired) {
         return;
      }
      assert current != null;
      assert desired != null;
      AcquiredLock lock = locks.get(atom);
      switch (current) {
         case LOCKED_EXCLUSIVE:
            ExclusiveLock exclusive = (ExclusiveLock) lock;
            switch (desired) {
               case LOCKED_SHARED:
                  locks.put(atom, exclusive.demoteToShared());
                  return;

               case NONE:
                  exclusive.unlock();
                  locks.remove(atom);
                  return;
                  
               default:
                  throw new AssertionError();
            }
            
         case LOCKED_SHARED:
            SharedLock share = (SharedLock) lock;
            switch (desired) {
               case LOCKED_EXCLUSIVE:
                  locks.put(atom, share.promoteToExclusive());
                  return;

               case NONE:
                  share.unlock();
                  locks.remove(atom);
                  return;
                  
               default:
                  throw new AssertionError();
            }
            
         case NONE:
            assert lock == null;
            switch (desired) {
               case LOCKED_EXCLUSIVE:
                  locks.put(atom, atom.exclusiveLock());
                  return;

               case LOCKED_SHARED:
                  locks.put(atom, atom.sharedLock());
                  return;
                  
               default:
                  throw new AssertionError();
            }
            
         default:
            throw new AssertionError();
      }
   }
   
   /**
    * The current version number. This produces monotonically increasing version numbers.
    */
   private static final AtomicLong versionNumber = new AtomicLong();
   
   /**
    * The set of pinned version numbers. The same version may be pinned more than once, so this map
    * stores a reference count to the pinned versions. When a count for a given version reaches
    * zero, that version is removed from the map.
    */
   private static final ConcurrentNavigableMap<Long, AtomicInteger> pinnedVersions =
         new ConcurrentSkipListMap<Long, AtomicInteger>();
   
   /**
    * The transaction for the current thread or null if none is in progress.
    */
   private static final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();
   
   /**
    * The state of a lock held by the transaction.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private enum LockState {
      /**
       * Indicates that no lock is held.
       */
      NONE,
      
      /**
       * Indicates that a lock is held in shared mode.
       */
      LOCKED_SHARED,
      
      /**
       * Indicates that a lock is held in exclusive mode.
       */
      LOCKED_EXCLUSIVE
   }
   
   /**
    * Information about the state of an atom in the currently running transaction.
    *
    * @param <T> the type of the atom's value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AtomInfo<T> {
      /**
       * The sequence of commute operations for this atom. Each operation is comprised of a function
       * to apply and a future whose value is set when the transaction is committed and the function
       * has been applied.
       */
      final Queue<Pair<Function<? super T, ? extends T>, SettableListenableFuture<T>>> commutes =
            new ArrayDeque<Pair<Function<? super T, ? extends T>, SettableListenableFuture<T>>>();
      
      /**
       * The previous lock state for this atom. If this information is rolled back, the atom's lock
       * state will be restored to this value.
       */
      final LockState previousState;
      
      /**
       * The current lock state for this atom.
       */
      LockState currentState;
      
      /**
       * The current value for this atom. This is a pending value that may be recorded to the atom
       * when the transaction commits.
       */
      private T value;
      
      /**
       * A flag indicating the atom's value is "dirty". The value is dirty if it needs to be
       * recorded to the atom when the transaction commits.
       */
      private boolean dirty;
      
      /**
       * Constructs base information for an atom. The specified value is the atom's current value
       * and is not dirty. No lock is held for the atom.
       *
       * @param value the atom's value
       */
      AtomInfo(T value) {
         currentState = previousState = LockState.NONE;
         this.value = value;
      }
      
      /**
       * Constructs new information for an atom. The predecessor represents previous information for
       * the atom and is associated with an earlier {@link Savepoint}.
       *
       * @param predecessor the previous information for an atom
       */
      AtomInfo(AtomInfo<T> predecessor) {
         currentState = previousState = predecessor.currentState;
         value = predecessor.value;
         dirty = predecessor.dirty;
      }
      
      boolean isDirty() {
         return dirty;
      }
      
      T getValue() {
         return value;
      }
      
      /**
       * Sets a new value for the atom and returns the previous atom.
       *
       * @param value the atom's new value
       * @return the atom's previous value
       */
      T setValue(T value) {
         // can only set atom's value if it is locked
         assert currentState == LockState.LOCKED_EXCLUSIVE;
         
         T ret = this.value;
         this.value = value;
         dirty = true;
         return ret;
      }
      
      /**
       * Cancels all pending futures for commute operations. This is done during a roll-back.
       */
      void cancelFutures() {
         for (Pair<Function<? super T, ? extends T>, SettableListenableFuture<T>> commute
               : commutes) {
            commute.getSecond().cancel(false);
         }
      }
   }
   
   /**
    * Represents a checkpoint in the transaction. Updates can be rolled back completely or just to
    * a specified savepoint.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Savepoint {
      /**
       * A sequence of updates made to asynchronous atoms. These updates are only executed if the
       * transaction is committed.
       */
      final Queue<Pair<AsynchronousAtom<?>, ListenableFutureTask<?>>> asyncActions =
            new ArrayDeque<Pair<AsynchronousAtom<?>, ListenableFutureTask<?>>>();
      
      /**
       * Information about atoms modified in the scope of this savepoint.
       */
      final Map<TransactionalAtom<?>, AtomInfo<?>> atomInfo =
            new HashMap<TransactionalAtom<?>, AtomInfo<?>>();
      
      /**
       * This savepoint's predecessor.
       */
      Savepoint predecessor;
      
      /**
       * Constructs a new savepoint.
       */
      Savepoint() {
      }
      
      /**
       * Constructs a new savepoint with the specified predecessor.
       *
       * @param predecessor the previous savepoint
       */
      Savepoint(Savepoint predecessor) {
         this.predecessor = predecessor;
      }
      
      /**
       * Gets information for the specified atom.
       *
       * @param atom an atom
       * @return information for the atom or null if this savepoint has no such information
       */
      @SuppressWarnings("unchecked")
      <T> AtomInfo<T> getInfo(TransactionalAtom<T> atom) {
         return (AtomInfo<T>) atomInfo.get(atom);
      }
      
      /**
       * Cancels all pending futures for this savepoint. This cancels futures associated with
       * asynchronous actions and also cancels futures corresponding to commute operations. This
       * is done during a roll-back.
       */
      void cancelFutures() {
         for (Pair<AsynchronousAtom<?>, ListenableFutureTask<?>> asyncAction : asyncActions) {
            asyncAction.getSecond().cancel(false);
         }
         for (AtomInfo<?> info : atomInfo.values()) {
            info.cancelFutures();
         }
      }
      
      /**
       * Submits asynchronous actions for execution. This is done during a commit.
       */
      void submitAsyncActions() {
         for (Pair<AsynchronousAtom<?>, ListenableFutureTask<?>> asyncAction : asyncActions) {
            @SuppressWarnings("unchecked")
            AsynchronousAtom<Object> atom = (AsynchronousAtom<Object>) asyncAction.getFirst();
            @SuppressWarnings("unchecked")
            ListenableFutureTask<Object> future =
                  (ListenableFutureTask<Object>) asyncAction.getSecond();

            AsynchronousAtom.submitFuture(atom, future);
         }
      }
      
      /**
       * Reverses the lock states for all atoms. This reverts the locks for all atoms to the state
       * of this savepoint's predecessor. This is done during a partial roll-back.
       *
       * @param locks the state of acquired locks
       */
      void reverseLocks(Map<TransactionalAtom<?>, AcquiredLock> locks) {
         for (Map.Entry<TransactionalAtom<?>, AtomInfo<?>> info : atomInfo.entrySet()) {
            realizeLockState(info.getKey(), info.getValue().currentState,
                  info.getValue().previousState, locks);
         }
      }
   }
   
   /**
    * The transaction's isolation level.
    */
   private final IsolationLevel isolationLevel;
   
   /**
    * The maximum number of isolation failures. When this many are observed, the transaction is
    * aborted.
    */
   private final int maxIsolationFailures;
   
   /**
    * The maximum number of deadlock failures. When this many are observed, the transaction is
    * aborted.
    */
   private final int maxDeadlockFailures;
   
   /**
    * The state of locks acquired.
    */
   private final Map<TransactionalAtom<?>, AcquiredLock> locks =
         new HashMap<TransactionalAtom<?>, AcquiredLock>();
   
   /**
    * The version of data that is read when querying atom values inside this transaction. A value
    * of negative one indicates that the read version has not yet been identified. The value is
    * determined as late as possible, to reduce the transaction's exposure to contention. With an
    * isolation level of {@link IsolationLevel#READ_COMMITTED}, this value is never identified and
    * all reads take an atom's most recent committed value.
    */
   private long readVersion = -1;
   
   /**
    * The current savepoint. This is a linked list, with each savepoint holding a reference to its
    * predecessor.
    */
   private Savepoint savepoint;
   
   /**
    * Upon completion, will represent the version number of committing this transaction. This field
    * is null while the transaction is running and becomes non-null when the commit operation is
    * underway. It is completed with the commit version number once all updated atoms have been
    * {@linkplain TransactionalAtom#markForCommit(Transaction) marked} and commit version number
    * allocated.
    */
   private volatile SettableListenableFuture<Long> commitVersion;
   
   /**
    * A latch that is opened once this transaction is committed. This field is null while the
    * transaction is running and becomes non-null when the commit operation is underway. When the
    * commit operation finishes, the latch will open.
    */
   private volatile CountDownLatch commitLatch;
   
   /**
    * Constructs a new transaction.
    *
    * @param isolationLevel the isolation level for the transaction
    * @param maxIsolationFailures the maximum number of isolation failures before aborting
    * @param maxDeadlockFailures the maximum number of deadlock failures before aborting
    */
   Transaction(IsolationLevel isolationLevel, int maxIsolationFailures, int maxDeadlockFailures) {
      this.isolationLevel = isolationLevel;
      this.maxIsolationFailures = maxIsolationFailures;
      this.maxDeadlockFailures = maxDeadlockFailures;
   }
   
   /**
    * Gets the read version for the transaction. This method is unused when the isolation level is
    * {@link IsolationLevel#READ_COMMITTED} since reads will access atoms' latest committed value.
    * The first time this method is called in the transaction, the read version is assigned. This
    * delays pinning the version until absolutely necessary, to minimize exposure to concurrent
    * modifications that could otherwise cause isolation failures.
    *
    * @return the read version for the transaction
    */
   private long getReadVersion() {
      if (readVersion == -1) {
         readVersion = pinNewVersion();
      }
      return readVersion;
   }
   
   /**
    * Runs the specified computation in a transaction and returns its result. This is the step
    * where the transaction is actually executed.
    *
    * @param computation the computation to perform in a transaction
    * @return the result of the computation
    * @throws IllegalStateException if a transaction is already in progress on the current thread
    * @throws X a type of exception thrown by the computation
    * @throws TransactionIsolationException if there is too much contention over the atoms such that
    *       the computation cannot be performed with the proper isolation level
    * @throws DeadlockException if this computation generated deadlock exceptions when accessing and
    *       modifying atoms
    */
   <T, X extends Throwable> T transact(Computation<T, X> computation) throws X {
      if (currentTransaction.get() != null) {
         throw new IllegalStateException("A transaction is already in progress. Another cannot be started.");
      }
      savepoint = new Savepoint();
      int isolationFailures = 0;
      int deadlocks = 0;
      while (true) {
         readVersion = -1;
         currentTransaction.set(this);
         try {
            T ret = computation.compute(this);
            doCommit();
            return ret;
         } catch (Throwable t) {
            doRollback();
            if (t instanceof TransactionIsolationException) {
               if (++isolationFailures >= maxIsolationFailures) {
                  // failed too many times, propagate
                  throw (TransactionIsolationException) t;
               }
            } else if (t instanceof DeadlockException) {
               if (++deadlocks >= maxDeadlockFailures) {
                  // failed too many times, propagate
                  throw (DeadlockException) t;
               }
            } else {
               @SuppressWarnings("unchecked")
               X cast = (X) t;
               throw cast;
            }
         } finally {
            currentTransaction.set(null);
            if (readVersion != -1) {
               unpinVersion(readVersion);
            }
         }
      }
   }
   
   /**
    * Unlocks all atoms that have been locked during this transaction. This also {@linkplain
    * TransactionalAtom#unmark() unmarks} atoms in case we are recovering from a commit failure.
    */
   private void unlockAll() {
      for (Map.Entry<TransactionalAtom<?>, AcquiredLock> lockEntry : locks.entrySet()) {
         lockEntry.getKey().unmark();
         lockEntry.getValue().unlock();
      }
      locks.clear();
   }

   /**
    * Computes commute operation results for the specified atom information. These will represent
    * a set of commutes for a given savepoint. The results are computed and stored in the specified
    * list of pending futures. The atom's value is also {@linkplain #setAtom(TransactionalAtom,
    * Object, boolean) set} after all commute functions have been applied.
    *
    * @param atom the atom whose value is being commuted
    * @param info the atom information that contains the commute operations
    * @param pendingFutures a list of pending futures which contains the futures corresponding to
    *       commute operations as well as their pending computed value
    */
   private <T> void processCommutesForAtom(TransactionalAtom<T> atom, AtomInfo<T> info,
         List<Pair<SettableListenableFuture<?>, Object>> pendingFutures) {
      if (info.commutes.isEmpty()) {
         return;
      }
      boolean first = true;
      T value = pinAtom(atom, true);
      for (Pair<Function<? super T, ? extends T>, SettableListenableFuture<T>> commute
            : info.commutes) {
         if (first) {
            first = false;
         } else {
            // validate previously computed value (this wonky conditional is here just to avoid
            // double validation of the last value, since setAtom(...) below will also validate)
            atom.validate(value);
         }
         value = commute.getFirst().apply(value);
         pendingFutures.add(
               Pair.<SettableListenableFuture<?>, Object>create(commute.getSecond(), value));
      }
      setAtom(atom, value, true);
   }
   
   /**
    * Executes commute operations for the specified savepoint. This will first excecute commutes for
    * the savepoint's predecessor. Even though commute operations should be commutative, and thus
    * not dependent on ordering, we execute them in the order they were enqueued. On completion, the
    * specified list will include data for all commute operations for this savepoint.
    *
    * @param sp the savepoint whose commute operations are to be performed
    * @param pendingFutures a list of pending futures which will contain the futures corresponding
    *       to commute operations as well as their pending computed value
    */
   private void processCommutes(Savepoint sp,
         List<Pair<SettableListenableFuture<?>, Object>> pendingFutures) {
      if (sp == null) {
         return;
      }
      processCommutes(sp.predecessor, pendingFutures);
      for (Map.Entry<TransactionalAtom<?>, AtomInfo<?>> entry : sp.atomInfo.entrySet()) {
         @SuppressWarnings("unchecked")
         TransactionalAtom<Object> atom = (TransactionalAtom<Object>) entry.getKey();
         @SuppressWarnings("unchecked")
         AtomInfo<Object> info = (AtomInfo<Object>) entry.getValue();
         processCommutesForAtom(atom, info, pendingFutures);
      }
   }
   
   /**
    * Submits actions for asynchronous atoms. As changes are made in a transaction, they are
    * enqueued. When the transaction is committed, this method is called to actually submit the
    * changes for execution. This method first processes actions for the specified savepoint's
    * predecessor so that all actions are submitted in the same order they were enqueued.
    *
    * @param sp the savepoint whose asynchronous actions are to be submitted
    */
   private void processAsyncActions(Savepoint sp) {
      if (sp == null) {
         return;
      }
      processAsyncActions(sp.predecessor);
      sp.submitAsyncActions();
   }
   
   /**
    * Commits the current transaction.
    */
   private void doCommit() {
      List<Pair<SettableListenableFuture<?>, Object>> pendingFutures =
            new ArrayList<Pair<SettableListenableFuture<?>, Object>>();
      processCommutes(savepoint, pendingFutures);
      // If all pending commutes pass validation, then we should be safe from here on out
      // to successfully complete the transaction
      if (readVersion != -1) {
         unpinVersion(readVersion);
         readVersion = -1;
      }
      
      // After we generate the commit version, there's a race where concurrent transactions could
      // grab it as their read version, but we haven't yet finished writing data to the atoms. So
      // we use this future and latch to synchronize with those concurrent readers.
      commitLatch = new CountDownLatch(1);
      commitVersion = new SettableListenableFuture<Long>();
      try {
         // Mark atoms that we're about to change
         Set<TransactionalAtom<?>> markedAtoms = new HashSet<TransactionalAtom<?>>();
         for (Savepoint sp = savepoint; sp != null; sp = sp.predecessor) {
            for (Map.Entry<TransactionalAtom<?>, AtomInfo<?>> entry : sp.atomInfo.entrySet()) {
               TransactionalAtom<?> atom = entry.getKey();
               if (entry.getValue().isDirty() && markedAtoms.add(atom)) {
                  atom.markForCommit(this);
               }
            }
         }
         
         List<Trio<TransactionalAtom<Object>, Object, Object>> notifications =
               new ArrayList<Trio<TransactionalAtom<Object>, Object, Object>>();
         
         long newVersion = pinNewVersion();
         try {
            commitVersion.setValue(newVersion);
            long oldestVersion = oldestVersion();
            // now save all values with this version
            for (Savepoint sp = savepoint; sp != null; sp = sp.predecessor) {
               for (Map.Entry<TransactionalAtom<?>, AtomInfo<?>> entry : sp.atomInfo.entrySet()) {
                  @SuppressWarnings("unchecked")
                  TransactionalAtom<Object> atom = (TransactionalAtom<Object>) entry.getKey();
                  AtomInfo<?> info = entry.getValue();
                  if (info.isDirty() && markedAtoms.remove(atom)) {
                     Object newValue = info.getValue();
                     Object oldValue = atom.addValue(info.getValue(), newVersion, oldestVersion);
                     notifications.add(Trio.create(atom, oldValue, newValue));
                     atom.unmark(); // "release" the atom eagerly
                  }
               }
            }
         } finally {
            unpinVersion(newVersion);
         }
         
         savepoint = null;
         unlockAll();
         
         // fulfill pending commute futures
         for (Pair<SettableListenableFuture<?>, Object> pending : pendingFutures) {
            @SuppressWarnings("unchecked")
            SettableListenableFuture<Object> future =
                  (SettableListenableFuture<Object>) pending.getFirst();
            future.setValue(pending.getSecond());
         }
         
         // submit async tasks
         processAsyncActions(savepoint);
         
         // send notifications
         for (Trio<TransactionalAtom<Object>, Object, Object> notification : notifications) {
            notification.getFirst().notify(notification.getSecond(), notification.getThird());
         }
         
      } finally {
         // this is a no-op if the version was successfully set above
         commitVersion.cancel(false);
         // also a no-op if the latch was successfully opened above
         commitLatch.countDown();
         
         commitVersion = null;
         commitLatch = null;
      }
   }
   
   /**
    * Rolls back the current transaction.
    */
   private void doRollback() {
      // cancel all pending futures
      while (savepoint != null) {
         savepoint.cancelFutures();
         savepoint = savepoint.predecessor;
      }
      savepoint = new Savepoint();
      unlockAll();
   }
   
   /**
    * Verifies that the transaction is running in the current thread.
    */
   private void checkState() {
      if (current() != this) {
         throw new IllegalStateException("The tranasction is not running on this thread");
      }
   }
   
   /**
    * Rolls back the current transaction. This resets all mutations made. All futures associated
    * with {@linkplain TransactionalAtom#commute(Function) commute} operations and {@linkplain
    * AsynchronousAtom asynchronous actions} that have been performed in this transaction are
    * cancelled.
    * 
    * @throws IllegalStateException if this transaction is not running on the current thread
    */
   public void rollback() {
      checkState();
      doRollback();
   }
   
   /**
    * Creates a checkpoint in the transaction. After such a checkpoint is established, the
    * transaction can be partially rolled back such that changes made <em>after</em> the savepoint
    * are rolled back, but changes made prior to the savepoint are retained.
    *
    * @return the new savepoint
    * @throws IllegalStateException if this transaction is not running on the current thread
    * 
    * @see #rollbackTo(Savepoint)
    */
   public Savepoint savepoint() {
      checkState();
      Savepoint ret = savepoint;
      savepoint = new Savepoint(ret);
      return ret;
   }

   /**
    * Performs a partial roll back. All mutations made since the specified savepoint was established
    * are reset. Similarly, all futures for {@linkplain TransactionalAtom#commute(Function) commute}
    * operations and {@linkplain AsynchronousAtom asynchronous actions} are cancelled. Changes made
    * prior to the savepoint being established are retained.
    *
    * @param point the savepoint up to which point the transaction is reversed
    * @throws IllegalStateException if this transaction is not running on the current thread
    * @throws NullPointerException if the specified savepoint is null
    * @throws IllegalArgumentException if the specified savepoint is not valid, due to either being
    *       created by a different transaction or already having been rolled back
    */
   public void rollbackTo(Savepoint point) {
      checkState();
      if (point == null) {
         throw new NullPointerException();
      }

      assert point != savepoint;
      
      boolean found = false;
      for (Savepoint current = savepoint; current != null; current = current.predecessor) {
         if (current == point) {
            found = true;
            break;
         }
      }
      if (!found) {
         throw new IllegalArgumentException("No such savepoint in this transaction");
      }
      
      while (savepoint != point) {
         savepoint.cancelFutures();
         savepoint.reverseLocks(locks);
         savepoint = savepoint.predecessor;
      }
      savepoint = new Savepoint(point);
   }
   
   /**
    * Finds the most recent information for the specified atom. If the atom has not been touched
    * during this transaction, and thus has no associated information, then this returns null.
    *
    * @param atom an atom
    * @return information for the specified item from the most recent savepoint which contains such
    *       information; null if no such information exists
    */
   private <T> AtomInfo<T> findAtomInfo(TransactionalAtom<T> atom) {
      AtomInfo<T> info = null;
      for (Savepoint current = savepoint; current != null; current = current.predecessor) {
         info = current.getInfo(atom);
         if (info != null) {
            return info;
         }
      }
      return null;
   }
   
   /**
    * Extracts the value of the specified atom. When the isolation level is {@link
    * IsolationLevel#READ_COMMITTED}, this returns the latest value for the atom. For other
    * isolation levels, this queries the atom for the value at the transaction's current
    * {@linkplain #getReadVersion() read version}.
    *
    * @param atom an atom
    * @return the version of the atom's value that is appropriate for this transaction
    */
   private <T> T extractValueFromAtom(TransactionalAtom<T> atom) {
      return isolationLevel == IsolationLevel.READ_COMMITTED
            ? atom.getLatestValue() : atom.getValue(getReadVersion());
   }

   /**
    * Creates information about the specified item in the current/active savepoint. If information
    * for the atom already exists, it is the basis for new information. If the current savepoint
    * already has information for the specified atom, that information object is returned.
    *
    * @param atom an atom
    * @return information for the specified atom in the current/active savepoint
    */
   private <T> AtomInfo<T> createAtomInfo(TransactionalAtom<T> atom) {
      for (Savepoint current = savepoint; current != null; current = current.predecessor) {
         AtomInfo<T> info = current.getInfo(atom);
         if (info != null) {
            if (current != savepoint) {
               info = new AtomInfo<T>(info);
               savepoint.atomInfo.put(atom, info);
            }
            return info;
         }
      }
      AtomInfo<T> info = new AtomInfo<T>(extractValueFromAtom(atom));
      savepoint.atomInfo.put(atom, info);
      return info;
   }
   
   /**
    * Gets the value for the specified atom. In {@linkplain IsolationLevel#SERIALIZABLE serializable}
    * isolation level, this causes the specified atom to be read-locked. If the atom has been
    * modified in this transaction, that last set value is returned. If commute operations have been
    * made for the atom, they are ignored, and the value returned is the atom's last known value,
    * ignoring commutes (which do not get applied until the transaction is committed).
    *
    * @param atom an atom
    * @return the atom's value in this transaction
    * @throws TransactionIsolationException if the transaction's isolation level is {@link
    *    IsolationLevel#SERIALIZABLE} and the atom has already been concurrently modified since the
    *    transaction's read version
    * @throws DeadlockException if the transaction's isolation level is {@link
    *    IsolationLevel#SERIALIZABLE} and a deadlock was detected when trying to lock the atom 
    */
   <T> T getAtom(TransactionalAtom<T> atom) {
      if (isolationLevel == IsolationLevel.SERIALIZABLE) {
         return pinAtom(atom);
      }
      AtomInfo<T> info = findAtomInfo(atom);
      return info != null ? info.getValue() : extractValueFromAtom(atom);
   }
   
   /**
    * Acquires a lock on the specified atom. If atom is already write-locked, no action needs to be
    * taken.
    *
    * @param atom the atom to lock
    * @param info information about the current state of the atom in this transaction
    * @param desiredLockState the desired lock state
    * @param validateVersion if true, an exception will be thrown if the atom has been concurrently
    *       modified since the transaction's read version 
    * @throws TransactionIsolationException if validating the atom's version and the atom has
    *    already been concurrently modified since the transaction's read version
    * @throws DeadlockException if a deadlock was detected when trying to acquire the lock 
    */
   private <T> void acquireLock(TransactionalAtom<T> atom, AtomInfo<T> info,
         LockState desiredLockState, boolean validateVersion) {
      if (info.currentState == LockState.LOCKED_EXCLUSIVE 
            || info.currentState == desiredLockState) {
         // don't demote exclusive to shared and no need to do anything if we already
         // have the right lock
         return;
      }
      realizeLockState(atom, info.currentState, desiredLockState, locks);
      info.currentState = desiredLockState;
      if (validateVersion && isolationLevel != IsolationLevel.READ_COMMITTED
            && atom.getLatestVersion() > getReadVersion()) {
         throw new TransactionIsolationException();
      }
   }

   /**
    * Pins the specified atom so it cannot be concurrently modified. This read-locks the specified
    * atom, which prevents other transactions from write-locking it. This is the default behavior
    * for all reads when the isolation level is {@link IsolationLevel#SERIALIZABLE}. For other
    * isolation levels, pinning an atom can prevent write skew. If this pin is for getting the input
    * value for applying commute functions, the version is not validated. This is because commute
    * operations should be commutative and not rely on any specific starting value.
    *
    * @param atom an atom
    * @param commute true if this operation is pinning the input value for commute operations
    * @return the atom's value in this transaction
    * @throws TransactionIsolationException if the atom has already been concurrently modified since
    *    the transaction's read version
    * @throws DeadlockException if a deadlock was detected when trying to lock the atom 
    */
   private <T> T pinAtom(TransactionalAtom<T> atom, boolean commute) {
      AtomInfo<T> info = createAtomInfo(atom);
      acquireLock(atom, info, LockState.LOCKED_SHARED, !commute);
      return info.getValue();
   }

   /**
    * Pins the specified atom so it cannot be concurrently modified. This read-locks the specified
    * atom, which prevents other transactions from write-locking it. This is the default behavior
    * for all reads when the isolation level is {@link IsolationLevel#SERIALIZABLE}. For other
    * isolation levels, pinning an atom can prevent write skew. 
    *
    * @param atom an atom
    * @return the atom's value in this transaction
    * @throws TransactionIsolationException if the atom has already been concurrently modified since
    *    the transaction's read version
    * @throws DeadlockException if a deadlock was detected when trying to lock the atom 
    */
   <T> T pinAtom(TransactionalAtom<T> atom) {
      return pinAtom(atom, false);
   }

   /**
    * Sets the value of the specified atom. This operation write-locks the specified atom so that
    * no other transaction can modify the atom's value until this one completes. If this value is
    * the result of a commute operation, the atom's version will not be validated when acquiring
    * the lock, since commute operations should be commutative and thus not dependent on the atom's
    * initial value.
    *
    * @param atom an atom
    * @param commute true if this operation is setting the result of commute operations
    * @param newValue the atom's new value
    * @return the atom's previous value in this transaction
    * @throws TransactionIsolationException if the atom has already been concurrently modified since
    *    the transaction's read version
    * @throws DeadlockException if a deadlock was detected when trying to lock the atom 
    */
   private <T> T setAtom(TransactionalAtom<T> atom, T newValue, boolean commute) {
      atom.validate(newValue);
      AtomInfo<T> info = createAtomInfo(atom);
      acquireLock(atom, info, LockState.LOCKED_EXCLUSIVE, !commute);
      T ret = info.setValue(newValue);
      return ret;
   }
   
   /**
    * Sets the value of the specified atom. This operation write-locks the specified atom so that
    * no other transaction can modify the atom's value until this one completes.
    *
    * @param atom an atom
    * @param newValue the atom's new value
    * @return the atom's previous value in this transaction
    * @throws TransactionIsolationException if the atom has already been concurrently modified since
    *    the transaction's read version
    * @throws DeadlockException if a deadlock was detected when trying to lock the atom 
    */
   <T> T setAtom(TransactionalAtom<T> atom, T newValue) {
      return setAtom(atom, newValue, false);
   }

   /**
    * Enqueues a commute operation for the specified atom. If the isolation level is {@link
    * IsolationLevel#SERIALIZABLE} then the atom will also be write-locked (to preserve the
    * serializability guarantees). The actual functions are not applied and futures fulfilled until
    * the transaction is committed.
    *
    * @param atom an atom
    * @param function the function that will be applied to the atom, and whose result will become
    *       the atom's new value
    * @return the future result of applying the function to the atom
    */
   <T> ListenableFuture<T> enqueueCommute(final TransactionalAtom<T> atom,
         final Function<? super T, ? extends T> function) {
      SettableListenableFuture<T> future = new SettableListenableFuture<T>();
      AtomInfo<T> info = createAtomInfo(atom);
      if (isolationLevel == IsolationLevel.SERIALIZABLE) {
         acquireLock(atom, info, LockState.LOCKED_EXCLUSIVE, true);
      }
      info.commutes.add(Pair.<Function<? super T, ? extends T>, SettableListenableFuture<T>>
            create(function, future));
      return future;
   }
   
   /**
    * Enqueues an asynchronous action for the specified atom. The specified runnable future will be
    * submitted for execution when the transaction is committed.
    *
    * @param atom an asynchronous atom
    * @param runnable the runnable future that performs the action
    */
   <T> void enqueueAsynchronousAction(AsynchronousAtom<T> atom, ListenableFutureTask<T> runnable) {
      Pair<AsynchronousAtom<?>, ListenableFutureTask<?>> pair =
            Pair.<AsynchronousAtom<?>, ListenableFutureTask<?>>create(atom, runnable); 
      savepoint.asyncActions.add(pair);
   }
   
   /**
    * Waits for this transaction to commit a version. If the specified version is less than (e.g.
    * before) the current transaction's commit version, then the waiting thread can be released
    * immediately and does not need to wait until the commit completes. Otherwise, this method will
    * block until the commit is finished.
    *
    * @param waitingForVersion the version that the caller is waiting on
    */
   void awaitCommit(long waitingForVersion) {
      Future<Long> futureVersion = commitVersion;
      if (futureVersion == null) {
         return;
      }
      boolean interrupted = false;
      try {
         // get pinned version and return early if we're not actually waiting on this commit
         while (true) {
            try {
               long version = futureVersion.get();
               if (waitingForVersion < version) {
                  // no need to wait
                  return;
               }
               break;
            } catch (InterruptedException e) {
               interrupted = true;
            } catch (CancellationException e) {
               return;
            } catch (Exception e) {
               // shouldn't happen...
               throw new AssertionError(e);
            }
         }
         // wait for the commit to complete
         CountDownLatch latch = commitLatch;
         if (latch == null) {
            return;
         }
         while (true) {
            try {
               latch.await();
               return;
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }
}
