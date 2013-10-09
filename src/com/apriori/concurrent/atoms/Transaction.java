package com.apriori.concurrent.atoms;

import com.apriori.util.Function;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// TODO: javadoc
// TODO: tests
public class Transaction {
   
   public enum IsolationLevel {
      SERIALIZABLE,
      REPEATABLE_READ,
      READ_COMMITTED
   }
   
   public static final IsolationLevel DEFAULT_ISOLATION_LEVEL = IsolationLevel.REPEATABLE_READ;
   
   public interface Action<X extends Throwable> {
      void run(Transaction t) throws X;
   }
   
   public interface UncheckedAction extends Action<RuntimeException> {
   }

   public interface Task<T, X extends Throwable> {
      T run(Transaction t) throws X;
   }

   public interface UncheckedTask<T> extends Task<T, RuntimeException> {
   }
   
   static <X extends Throwable> Task<Void, X> asTask(final Action<X> action) {
      return new Task<Void, X>() {
         @Override
         public Void run(Transaction t) throws X {
            action.run(t);
            return null;
         }
      };
   }
   
   public static class TransactionRunner {
      private final IsolationLevel isolationLevel;
      
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

   public <T, X extends Throwable> T executeNonIdempotent(Task<T, X> task) throws X {
      return withIsolation(DEFAULT_ISOLATION_LEVEL).executeNonIdempotent(task);
   }

   public <X extends Throwable> void executeNonIdempotent(Action<X> action) throws X {
      withIsolation(DEFAULT_ISOLATION_LEVEL).executeNonIdempotent(action);
   }

   private static final AtomicLong pinCounter = new AtomicLong();
   private static final ThreadLocal<Transaction> currentTransaction =
         new ThreadLocal<Transaction>();

   private enum TransactionState {
      NOT_STARTED, RUNNING, COMMITTING, COMPLETED
   }
   
   private enum LockState {
      NONE, LOCKED_SHARED, LOCKED_EXCLUSIVE
   }
   
   private static class AtomInfo<T> {
      T value;
      List<Function<? super T, ? extends T>> commutes;
      LockState lockState;
   }
   
   private static class Savepoint {
      List<Callable<?>> asyncActions;
      Map<TransactionalAtom<?>, AtomInfo<?>> atomInfo;
      Savepoint next;
   }
   
   private final IsolationLevel isolationLevel;
   private final boolean idempotent; 
   private final AtomicReference<TransactionState> state =
         new AtomicReference<TransactionState>(TransactionState.NOT_STARTED);
   private long pin;
   private Savepoint savepoint;
   
   Transaction(IsolationLevel isolationLevel, boolean idempotent) {
      this.isolationLevel = isolationLevel;
      this.idempotent = idempotent;
   }
   
   <T, X extends Throwable> T transact(Task<T, X> task) throws X {
      try {
         // TODO: init
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
      // TODO: implement me
   }

   public void rollback() {
      // TODO: implement me
   }
   
   public long savepoint() {
      // TODO: implement me
      return 0;
   }

   public void rollbackTo(long savepoint) {
      // TODO: implement me
   }
   
   Transaction current() {
      // TODO: implement me
      return null;
   }
   
   <T> T getAtom(TransactionalAtom<T> atom) {
      // TODO: implement me
      return null;
   }

   <T> T pinAtom(TransactionalAtom<T> atom) {
      // TODO: implement me
      return null;
   }

   <T> T setAtom(TransactionalAtom<T> atom) {
      // TODO: implement me
      return null;
   }

   <T> T enqueueCommute(TransactionalAtom<T> atom, Function<? super T, ? extends T> function) {
      // TODO: implement me
      return null;
   }
}
