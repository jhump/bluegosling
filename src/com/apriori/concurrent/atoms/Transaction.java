package com.apriori.concurrent.atoms;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.ListenableFutureTask;
import com.apriori.concurrent.SettableListenableFuture;
import com.apriori.tuples.Pair;
import com.apriori.util.Function;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A transaction in which changes to {@link TransactionalAtom}s can be made. Transactions use
 * multi-version concurrency control to provide a level of isolation between concurrently executing
 * transactions. The isolation level is configurable at the time of creating a new transaction.
 * Once a transaction has begun, it can be checkpointed, rolled back, and committed, just as if it
 * were an RDBMS transaction.
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
       * provide better performance since read locks aren't necessarily acquired for every read.
       * The main artifact that can occur is called "write skew". But this can be mitigated in
       * updates to a transactional atom by using its {@link TransactionalAtom#commute(Function)
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
       * improvement over {@link #REPEATABLE_READ}.
       */
      READ_COMMITTED
   }
   
   /**
    * The default isolation level for a transaction.
    */
   public static final IsolationLevel DEFAULT_ISOLATION_LEVEL = IsolationLevel.REPEATABLE_READ;

   /**
    * Represents a checkpoint in a transaction. This is a marker interface and is intentionally
    * opaque.
    *
    * @see Transaction#savepoint()
    * @see Transaction#rollbackTo(Savepoint)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static interface Savepoint {
   }
   
   /**
    * An action to execute in a transaction.
    *
    * @param <X> a type of exception that the action may throw
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Action<X extends Throwable> {
      /**
       * Executes the action in the specified transaction.
       *
       * @param t the current transaction, in which the action runs
       * @throws X 
       */
      void run(Transaction t) throws X;
   }
   
   /**
    * An action to execute in a transaction that does not throw any checked exceptions.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface UncheckedAction extends Action<RuntimeException> {
   }

   /**
    * A task that produces a value and executes in a transaction.
    *
    * @param <T> the type of value produced
    * @param <X> the type of exception that the action may throw
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Task<T, X extends Throwable> {
      /**
       * Executes the task in the specified transaction.
       *
       * @param t the current transaction, in which the action runs
       * @return the task's result
       * @throws X 
       */
      T run(Transaction t) throws X;
   }

   /**
    * A task that produces a value and executes in a transaction and does not throw any checked
    * exceptions.
    *
    * @param <T> the type of value produced
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface UncheckedTask<T> extends Task<T, RuntimeException> {
   }
   
   /**
    * Adapts an {@link Action} as a {@link Task}.
    *
    * @param action the action
    * @return a task that will execute the action and return {@code null}
    */
   static <X extends Throwable> Task<Void, X> asTask(final Action<X> action) {
      return new Task<Void, X>() {
         @Override
         public Void run(Transaction t) throws X {
            action.run(t);
            return null;
         }
      };
   }
   
   /**
    * An object that can initiate execution of code in a transaction.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class TransactionRunner {
      /**
       * The isolation level to use in the transaction.
       */
      private final IsolationLevel isolationLevel;
      
      /**
       * Constructs a new task runner object.
       *
       * @param isolationLevel the isolation level to use in the transaction
       */
      TransactionRunner(IsolationLevel isolationLevel) {
         this.isolationLevel = isolationLevel;
      }
      
      public <T, X extends Throwable> T execute(Task<T, X> task) throws X {
         return new Transaction(isolationLevel, true).transact(task);
      }
   
      public <X extends Throwable> void execute(Action<X> action) throws X {
         execute(asTask(action));
      }
   
      public <T, X extends Throwable> T executeNonIdempotent(Task<T, X> task) throws X {
         return new Transaction(isolationLevel, false).transact(task);
      }
   
      public <X extends Throwable> void executeNonIdempotent(Action<X> action) throws X {
         executeNonIdempotent(asTask(action));
      }
   }
   
   public static TransactionRunner withIsolation(IsolationLevel isolationLevel) {
      return new TransactionRunner(isolationLevel);
   }

   public static <T, X extends Throwable> T execute(Task<T, X> task) throws X {
      return withIsolation(DEFAULT_ISOLATION_LEVEL).execute(task);
   }

   public static <X extends Throwable> void execute(Action<X> action) throws X {
      withIsolation(DEFAULT_ISOLATION_LEVEL).execute(action);
   }

   public static <T, X extends Throwable> T executeNonIdempotent(Task<T, X> task) throws X {
      return withIsolation(DEFAULT_ISOLATION_LEVEL).executeNonIdempotent(task);
   }

   public static <X extends Throwable> void executeNonIdempotent(Action<X> action) throws X {
      withIsolation(DEFAULT_ISOLATION_LEVEL).executeNonIdempotent(action);
   }

   public static boolean isInTransaction() {
      return current() != null;
   }
   
   static Transaction current() {
      return currentTransaction.get();
   }
   
   static long oldestVersion() {
      Iterator<Long> pinIterator = pinnedVersions.iterator();
      if (pinIterator.hasNext()) {
         return pinIterator.next();
      }
      return versionNumber.get();
   }
   
   private static final AtomicLong versionNumber = new AtomicLong();
   private static final Set<Long> pinnedVersions =
         Collections.newSetFromMap(new ConcurrentSkipListMap<Long, Boolean>());
   private static final ThreadLocal<Transaction> currentTransaction =
         new ThreadLocal<Transaction>();

   /**
    * The state of a transaction.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private enum TransactionState {
      /**
       * The initial state of a transaction upon construction.
       */
      NOT_STARTED,
      
      /**
       * The state of a transaction once execution begins. The transaction may return to this
       * state after entering the {@link #COMMITTING} state if a task or action explicitly invokes
       * {@link Transaction#commit()}.
       */
      RUNNING,
      
      /**
       * The state of a transaction that is in the process of committing writes. This happens when
       * the task or action explicitly invokes {@link Transaction#commit()} or automatically when 
       * the task or action completes successfully.
       */
      COMMITTING,
      
      /**
       * The state of a transaction that has ended. The transaction can no longer be used. This
       * state is set automatically when the task or action completes, regardless of success.
       */
      COMPLETED
   }
   
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
      T value;
      Queue<Pair<Function<? super T, ? extends T>, SettableListenableFuture<T>>> commutes;
      LockState currentState;
      LockState previousState;
   }
   
   /**
    * Represents a checkpoint in the transaction. Updates can be rolled back completely or just to
    * a specified savepoint.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SavepointNode implements Savepoint {
      final Queue<Pair<AsynchronousAtom<?>, ListenableFutureTask<?>>> asyncActions =
            new ArrayDeque<Pair<AsynchronousAtom<?>, ListenableFutureTask<?>>>();
      final Map<TransactionalAtom<?>, AtomInfo<?>> atomInfo =
            new HashMap<TransactionalAtom<?>, AtomInfo<?>>();
      SavepointNode next;
      
      SavepointNode() {
      }
      
      SavepointNode(SavepointNode next) {
         this.next = next;
      }
      
      @SuppressWarnings("unchecked")
      <T> AtomInfo<T> getInfo(TransactionalAtom<T> atom) {
         return (AtomInfo<T>) atomInfo.get(atom);
      }
   }
   
   private final IsolationLevel isolationLevel;
   private final boolean idempotent; 
   private final AtomicReference<TransactionState> state =
         new AtomicReference<TransactionState>(TransactionState.NOT_STARTED);
   private long version;
   private SavepointNode savepoint;
   
   Transaction(IsolationLevel isolationLevel, boolean idempotent) {
      this.isolationLevel = isolationLevel;
      this.idempotent = idempotent;
   }
   
   <T, X extends Throwable> T transact(Task<T, X> task) throws X {
      try {
         version = versionNumber.get();
         savepoint = new SavepointNode();
         if (!state.compareAndSet(TransactionState.NOT_STARTED, TransactionState.RUNNING)) {
            throw new AssertionError("State must be NOT_STARTED before use");
         }
         T ret = task.run(this);
         commit();
         state.set(TransactionState.COMPLETED);
         return ret;
         // TODO: catch and retry
      } finally {
         TransactionState finalState;
         while (true) {
            finalState = state.get();
            if (finalState == TransactionState.COMPLETED
                  || state.compareAndSet(finalState, TransactionState.COMPLETED)) {
               break;
            }
         }
         if (finalState != TransactionState.COMPLETED) {
            // exception caused an abort
            rollback();
         }
      }
   }
   
   public void commit() {
      if (!state.compareAndSet(TransactionState.RUNNING, TransactionState.COMMITTING)) {
         throw new IllegalStateException("The transaction is not running");
      }
      try {
         
      } finally {
         state.set(TransactionState.RUNNING);
      }
      // TODO: implement me
   }

   private void reverse(SavepointNode node) {
      // TODO: release locks? cancel queued futures?
   }
   
   public void rollback() {
      if (state.get() != TransactionState.RUNNING) {
         throw new IllegalStateException("The transaction is not running");
      }
      while (savepoint != null) {
         reverse(savepoint);
         savepoint = savepoint.next;
      }
      savepoint = new SavepointNode();
   }
   
   public Savepoint savepoint() {
      if (state.get() != TransactionState.RUNNING) {
         throw new IllegalStateException("The transaction is not running");
      }
      SavepointNode ret = savepoint;
      savepoint = new SavepointNode(ret);
      return ret;
   }

   public void rollbackTo(Savepoint point) {
      if (state.get() != TransactionState.RUNNING) {
         throw new IllegalStateException("The transaction is not running");
      }
      if (point == null) {
         throw new NullPointerException();
      }
      if (point == savepoint) {
         return; // nothing to do
      }
      
      SavepointNode node = (SavepointNode) point;
      boolean found = false;
      for (SavepointNode current = savepoint; current != null; current = current.next) {
         if (current == node) {
            found = true;
            break;
         }
      }
      if (!found) {
         throw new IllegalArgumentException("No such savepoint in this transaction");
      }
      
      while (savepoint != node) {
         reverse(savepoint);
         savepoint = savepoint.next;
      }
   }
   
   private <T> Pair<SavepointNode, AtomInfo<T>> findAtomInfo(TransactionalAtom<T> atom) {
      for (SavepointNode current = savepoint; current != null; current = current.next) {
         @SuppressWarnings("unchecked")
         AtomInfo<T> info = (AtomInfo<T>) current.atomInfo.get(atom);
         if (info != null) {
            return Pair.create(current, info);
         }
      }
      return null;
   }
   
   <T> T getAtom(TransactionalAtom<T> atom) {
      if (isolationLevel == IsolationLevel.SERIALIZABLE) {
         return pinAtom(atom);
      }
      
      AtomInfo<T> info = findAtomInfo(atom).getSecond();
      return info != null ? info.value : atom.getValue(version);
   }

   void realizeLockState(TransactionalAtom<?> atom, LockState current, LockState desired) {
      // TODO: acquire/promote/etc locks
   }
   
   <T> T pinAtom(TransactionalAtom<T> atom) {
      Pair<SavepointNode, AtomInfo<T>> found = findAtomInfo(atom);
      AtomInfo<T> info;
      if (found == null) {
         info = new AtomInfo<T>();
         savepoint.atomInfo.put(atom, info);
      } else if (found.getFirst() != savepoint) {
         //info = new AtomInfo<T>(found.getFirst());
         //savepoint.atomInfo.put(atom, info);
      } else {
         
      }
      // TODO: write-lock in serializable isolation level, otherwise read-lock
      return null;
   }

   <T> T setAtom(TransactionalAtom<T> atom, T newValue) {
      // TODO: implement me
      return null;
   }

   <T> ListenableFuture<T> enqueueCommute(final TransactionalAtom<T> atom,
         final Function<? super T, ? extends T> function) {
      SettableListenableFuture<T> future = new SettableListenableFuture<T>();
      
      // TODO: implement me - enqueue pair of function and future
      return future;
   }
   
   <T> void enqueueAsynchronousAction(AsynchronousAtom<T> atom, ListenableFutureTask<T> runnable) {
      Pair<AsynchronousAtom<?>, ListenableFutureTask<?>> pair =
            Pair.<AsynchronousAtom<?>, ListenableFutureTask<?>>create(atom, runnable); 
      savepoint.asyncActions.add(pair);
   }
}
