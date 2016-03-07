package com.bluegosling.concurrent.atoms;

import static com.bluegosling.concurrent.ThreadFactories.newGroupingDaemonThreadFactory;

import com.bluegosling.concurrent.FutureListener;
import com.bluegosling.concurrent.FutureVisitor;
import com.bluegosling.concurrent.executors.ActorThreadPool;
import com.bluegosling.concurrent.executors.SerializingExecutor;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.FluentFutureTask;
import com.bluegosling.concurrent.fluent.RunnableFluentFuture;
import com.bluegosling.function.TriFunction;
import com.bluegosling.possible.Reference;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An atom that is mutated asynchronously using a thread pool. For a given atom, all mutations are
 * applied sequentially. Multiple threads in a pool are used to process mutations on multiple such
 * atoms. Since the actual update is applied asynchronously, mutation methods return future values,
 * not immediate values.
 * 
 * <p>If an error occurs while applying a change, the future that represents that change fails.
 * Additionally, the atom will consult an error handler to decide what to do. There are three
 * options:
 * <ol>
 *    <li><strong>Ignore:</strong> Processing continues. The failed mutation is ignored and
 *    subsequent mutations continue to be processed.</li>
 *    <li><strong>Block:</strong> Processing of subsequent mutations is stopped until the atom
 *    is explicitly {@linkplain #resume() resumed}. While blocked, mutations are held in a queue
 *    instead of executed. Once the atom is resumed, the queue is processed using a thread pool
 *    thread, just as if they were submitted while the atom was processing normally.</li>
 *    <li><strong>Restart:</strong> The atom is restored to its most recent seeded value (set
 *    during construction or from a prior {@link #restart(Object, boolean)}) and then immediately
 *    resumed. This is similar to ignoring the error, except that the value is re-seeded.
 * </ol>
 * 
 * <p>If a {@link Transaction} is in progress, then a mutation is not actually submitted to the
 * thread pool until the transaction is committed. So, like {@link TransactionalAtom}s but unlike
 * other types of atoms, mutations to asynchronous atoms can be rolled back. When a transaction is
 * rolled back, all futures that correspond to rolled back asynchronous mutations are cancelled.
 * 
 * <p>Ordering of operations cannot be guaranteed as asynchronous operations could be submitted and
 * interleaved with those submitted from a given thread. Thus this form of atom is most useful when
 * only commutative functions are applied to it.
 * 
 * @param <T> the type of the atom's value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class AsynchronousAtom<T> extends AbstractAtom<T> {
   
   /**
    * One of the possible actions taken when an error occurs during a mutation operation.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public enum ErrorAction {
      /**
       * Indicates that a failure is ignored and subsequent operations are processed as if the
       * failure never occurred.
       */
      IGNORE,
      
      /**
       * Indicates that a failure blocks subsequent operations, which will be queued until the
       * atom is resumed.
       */
      BLOCK,
      
      /**
       * Indicates that a failure should restart the atom. This is similar to {@link #IGNORE} except
       * that the atom is first reset to its seed value. The seed value is the one specified during
       * construction or the most recent successful {@link AsynchronousAtom#restart(Object, boolean)}
       * operation.
       */
      RESTART;
      
      private final ErrorHandler<Object> errorHandler = new ErrorHandler<Object>() {
         @Override
         public ErrorAction onError(AsynchronousAtom<? extends Object> atom, Throwable failure) {
            return ErrorAction.this;
         }
      };
      
      /**
       * Returns a simple {@link ErrorHandler} that always returns this action. For example,
       * {@code ErrorAction.IGNORE.always()} returns a handler that always returns {@link #IGNORE}.
       *
       * @return an error handler that always returns this action
       */
      public ErrorHandler<Object> always() {
         return errorHandler;
      }
   }
   
   /**
    * Handles errors that occur during a mutation operation on an atom.
    *
    * @param <T> the type of the handled atom's value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface ErrorHandler<T> {
      /**
       * Handles an error. The action returned is what determines how the atom will proceed with
       * respect to subsequent mutation operations.
       *
       * @param atom the atom for which a mutation failed
       * @param failure the cause of the failure
       * @return the action to take
       */
      ErrorAction onError(AsynchronousAtom<? extends T> atom, Throwable failure);
   }
   
   /**
    * The thread pool used to execute mutation operations.
    */
   private static final SerializingExecutor<AsynchronousAtom<?>> threadPool =
         new ActorThreadPool<>(Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE,
               30, TimeUnit.SECONDS,
               newGroupingDaemonThreadFactory(AsynchronousAtom.class.getSimpleName()));

   /**
    * The atom's error handler.
    */
   private volatile ErrorHandler<? super T> errorHandler = ErrorAction.IGNORE.always();
   
   /**
    * A flag that indicates whether the atom is currently blocked (due to prior error).
    */
   private volatile boolean blocked;
   
   /**
    * The queue of operations; empty unless the atom is blocked due to prior error.
    * 
    * <p>This field should only be accessed from a thread pool and from tasks that execute
    * sequentially with respect to other tasks for this same atom. So it need not be thread-safe.

    */
   private final LinkedList<RunnableFluentFuture<T>> queued =
         new LinkedList<RunnableFluentFuture<T>>();
   
   /**
    * The size of the queue of operations; zero unless the atom is blocked due to prior error. This
    * is maintained separately as a volatile value so that it is visible from any thread.
    */
   private volatile int queueSize;
   
   /**
    * The atom's value.
    */
   private volatile T value;
   
   /**
    * The atom's seed value. This is the initial value set during construction. The seed value can
    * be updated by {@link #restart(Object, boolean)} operations.
    */
   private volatile T seedValue;
   
   /**
    * Constructs a new asynchronous atom with a {@code null} value and no validator.
    */
   public AsynchronousAtom() {
   }

   /**
    * Constructs a new asynchronous atom with the specified value and no validator.
    */
   public AsynchronousAtom(T value) {
      this.seedValue = this.value = value;
   }

   /**
    * Constructs a new asynchronous atom with the specified value and the specified validator.
    */
   public AsynchronousAtom(T value, Predicate<? super T> validator) {
      super(validator);
      validate(value);
      this.seedValue = this.value = value;
   }
   
   /**
    * Returns the atom's error handler. The default handler always ignores errors.
    *
    * @return this atom's error handler
    */
   public ErrorHandler<? super T> getErrorHandler() {
      return errorHandler;
   }
   
   /**
    * Sets the atom's error handler.
    *
    * @param errorHandler the error handler that this atom should use
    * 
    * @throws NullPointerException if the specified handler is {@code null}
    */
   public void setErrorHandler(ErrorHandler<? super T> errorHandler) {
      if (errorHandler == null) {
         throw new NullPointerException();
      }
      this.errorHandler = errorHandler;
   }
   
   /**
    * Returns true if the atom is blocked due to an earlier failure.
    *
    * @return true if the atom is blocked
    */
   public boolean isBlocked() {
      return blocked;
   }
   
   /**
    * Resumes mutation operations in a blocked atom. If the atom is not blocked, this effectively
    * does nothing. Upon becoming unblocked, any queued mutations will be processed in the order
    * that they were submitted.
    */
   public void resume() {
      threadPool.execute(this, new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            if (blocked) {
               blocked = false;
               processQueued();
            }
         }
      });
   }
   
   /**
    * Restarts mutation operations in a blocked atom. Any queued operations will be applied after
    * the atom is restarted. The atom will be reset to its seed value, which is the value used
    * during construction of the atom or the seed value specified by the most recent successful
    * {@link #restart(Object, boolean)} operation.
    *
    * @return true if the atom was restarted or false if it was not because the atom wasn't
    *       actually blocked
    */
   public boolean restart() {
      return restart(Reference.<T>unset(), false);
   }
   
   /**
    * Restarts mutation operations in a blocked atom with the given new seed value. The caller can
    * indicate whether the restart operation should discard all pending mutations or if they should
    * be retained and applied to the new restarted value.
    *
    * @param newSeed the new value with which to seed the restarted atom
    * @param cancelPending if true, any queued mutations will be discarded; otherwise, queued
    *       mutations will be applied to the new seeded value
    * @return true if the atom was restarted or false if it was not because the atom wasn't
    *       actually blocked
    * @throws IllegalArgumentException if the given restart value is invalid according to the atom's
    *       current validator
    */
   public boolean restart(T newSeed, boolean cancelPending) {
      return restart(Reference.setTo(newSeed), cancelPending);
   }
   
   /**
    * Restarts the atom.
    *
    * @param newSeed if present, the new value with which to seed the restarted the atom; if not
    *       present, the most recent seed value is used
    * @param cancelPending if true, any queued mutations will be discarded; otherwise, queued
    *       mutations will be applied to the new seeded value
    * @return true if the atom was restarted or false if it was not because the atom wasn't
    *       actually blocked
    */
   private boolean restart(final Reference<T> newSeed, final boolean cancelPending) {
      if (newSeed.isPresent()) {
         validate(newSeed.get());
      }
      AtomicBoolean success = new AtomicBoolean();
      CountDownLatch latch = new CountDownLatch(1);
      threadPool.execute(this, () -> {
         success.set(blocked);
         latch.countDown();
         if (blocked) {
            blocked = false;
            if (newSeed.isPresent()) {
               seedValue = value = newSeed.get();
            } else {
               value = seedValue;
            }
            if (cancelPending) {
               try {
                  while (!queued.isEmpty()) {
                     queued.remove().cancel(false);
                  }
               } finally {
                  queueSize = queued.size();
               }
            } else {
               processQueued();
            }
         }
      });
      // wait for the task to start and see if the atom was actually blocked 
      boolean interrupted = false;
      while (true) {
         try {
            latch.await();
            break;
         } catch (InterruptedException e) {
            interrupted = true;
         }
      }
      if (interrupted) {
         Thread.currentThread().interrupt();
      }
      
      return success.get();
   }

   /**
    * Processes any tasks in the queue. This is used after error recovery to execute any tasks that
    * were queued while the atom was blocked. This method returns when either a queued task fails
    * and causes the atom to be blocked again or when the queue has been exhausted.
    * 
    * <p>This should only be executed in a thread pool and must execute sequentially with respect to
    * other tasks for this same atom.
    */
   private void processQueued() {
      while (!queued.isEmpty() && !blocked) {
         RunnableFluentFuture<T> task = queued.remove();
         queueSize = queued.size();
         runSingleTask(task);
      }
   }

   /**
    * Returns the number of queued mutation operations. Operations are queued only when an error
    * causes the atom to become blocked. {@linkplain #resume() Resuming} the atom will drain the
    * queue and process pending operations.
    *
    * @return the length of the queue of pending mutations
    */
   public int getQueueLength() {
      return queueSize;
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This is a volatile read of the atom's value. There may be pending and/or concurrently
    * executing operations that will change and/or are changing this value. To get the value of
    * this atom after all such pending operations are complete, use {@link #getPending()} instead.
    */
   @Override
   public T get() {
      return value;
   }
   
   /**
    * Returns a future that completes with the atom's value once all currently pending operations
    * complete. If this atom is blocked or becomes blocked while processing currently pending
    * operations, the returned future will not complete until the atom is unblocked. If the atom is
    * unblocked via a {@link #restart} that cancels pending operations, then this future will be
    * cancelled.
    *
    * @return a future that completes with the atom's value once all currently pending operations
    *       complete
    */
   public FluentFuture<T> getPending() {
      return submit(() -> value);
   }
   
   /**
    * Submits a mutation that will set the atom to the specified value. The value will be validated
    * immediately, before it is submitted.
    *
    * @param newValue the new value for the atom
    * @return a future result that will be the atom's previous value at the time this mutation is
    *       applied
    * @throws IllegalArgumentException if the specified value is not valid for this atom
    * 
    */
   public FluentFuture<T> set(T newValue) {
      validate(newValue);
      return submit(() -> {
         T oldValue = value;
         value = newValue;
         notify(oldValue, newValue);
         return oldValue;
      });
   }
   
   /**
    * Applies a function to the atom's value. The atom's new value will be the result of applying
    * the specified function to the atom's current value. Watchers are notified when the value is
    * changed.
    * 
    * <p>Since validation cannot be done immediately, validation failure manifests as a failed
    * future. Depending on the atom's {@link #getErrorHandler() error handler}, a validation failure
    * could block subsequent mutations.
    *
    * @param function the function to apply
    * @return a future result that will be the atom's new value after the function is applied
    */
   public FluentFuture<T> updateAndGet(Function<? super T, ? extends T> function) {
      return doUpdate(function, true);
   }

   /**
    * Applies a function to the atom's value. The atom's new value will be the result of applying
    * the specified function to the atom's current value. Watchers are notified when the value is
    * changed.
    * 
    * <p>Since validation cannot be done immediately, validation failure manifests as a failed
    * future. Depending on the atom's {@link #getErrorHandler() error handler}, a validation failure
    * could block subsequent mutations.
    *
    * @param function the function to apply
    * @return a future result that will be the atom's initial value (before function is applied) but
    *       that won't complete until after the function is applied
    */
   public FluentFuture<T> getAndUpdate(Function<? super T, ? extends T> function) {
      return doUpdate(function, false);
   }

   /**
    * Applies the given function asynchronously, returning a future that either completes with the
    * atom's initial or resulting value.
    *
    * @param function a function that is used to compute a new value for the atom
    * @param returnNew if true, the returned future completes with the atom's new value, after the
    *       function is applied; otherwise returns the atom's initial value
    * @return a future that completes once the function has been applied
    */
   private FluentFuture<T> doUpdate(Function<? super T, ? extends T> function,
         boolean returnNew) {
      return submit(() -> {
         T oldValue = value;
         T newValue = function.apply(oldValue);
         validate(newValue);
         value = newValue;
         notify(oldValue, newValue);
         return returnNew ? newValue : oldValue;
      });
   }
   
   // TODO: fix up javadoc for accumulate methods

   /**
    * Submits a mutation that will combine the atom's value with the given value, using the given
    * function, and set the atom's value to the result. Validation cannot be done immediately, so a
    * validation failure manifests as a failed future. Depending on the atom's
    * {@link #getErrorHandler() error handler}, a validation failure could block subsequent
    * mutations.
    *
    * @param t the value to combine
    * @param function the function to apply
    * @return a future result that will be the atom's new value after the function is applied
    */
   public FluentFuture<T> accumulateAndGet(T t,
         BiFunction<? super T, ? super T, ? extends T> function) {
      return updateAndGet(v -> function.apply(v, t));
   }

   public FluentFuture<T> getAndAccumulate(T t,
         BiFunction<? super T, ? super T, ? extends T> function) {
      return getAndUpdate(v -> function.apply(v, t));
   }

   /**
    * Submits the specified operation for asynchronous execution. If a transaction is in progress,
    * the operation is queued up in the transaction and will be submitted to a thread pool only
    * when the transaction commits. If no transaction is in progress, the operation is immediately
    * submitted to a thread pool for execution.
    *
    * @param task the operation to execute asynchronously
    * @return a future that completes when the specified operation completes
    */
   private FluentFuture<T> submit(Callable<T> task) {
      FluentFutureTask<T> future = new FluentFutureTask<T>(task);
      Transaction transaction = Transaction.current();
      if (transaction != null) {
         // play nice with transactions -- queue up actions so that they are only submitted
         // when the transaction gets committed
         TransactionalFutureTask<T> ret = new TransactionalFutureTask<>(future);
         transaction.enqueueAsynchronousAction(this, ret);
         return ret;
      } else {
         submitFuture(this, future);
         return future;
      }
   }
   
   /**
    * Submits the specified operation to a thread pool. The operation is queued behind any other
    * operations for the same specified atom.
    *
    * @param atom the atom which this operation affects
    * @param future a future task that will perform an operation on the specified atom when executed
    */
   static <T> void submitFuture(AsynchronousAtom<T> atom, RunnableFluentFuture<T> future) {
      if (future instanceof TransactionalFutureTask) {
         ((TransactionalFutureTask<T>) future).markCommitted();
      }
      threadPool.execute(atom, () -> atom.runSingleTask(future));
   }
   
   /**
    * Runs a single operation for an atom. This should only be executed in a thread pool and must
    * execute sequentially with respect to other tasks for this same atom. If the current atom is
    * blocked due to a prior failure, this task is queued for later execution (when the atom is
    * resumed or restarted). Otherwise, it executes the task immediately and may cause the current
    * atom to become blocked if the task fails.
    *
    * @param task a future task that will perform an operation on this atom when executed
    */
   private void runSingleTask(RunnableFluentFuture<T> task) {
      if (blocked) {
         queued.add(task);
         queueSize = queued.size();
      } else {
         task.run();
         if (task.isFailed()) {
            ErrorAction action = errorHandler.onError(AsynchronousAtom.this, task.getFailure());
            switch (action) {
               case BLOCK:
                  blocked = true;
                  break;
               case RESTART:
                  value = seedValue;
                  break;
               default:
                  assert action == ErrorAction.IGNORE;
                  break;
            }
         }
      }
   }
   
   /**
    * A future that is scheduled from within a transaction. The main functionality it adds is for
    * deadlock avoidance: this future doesn't allow blocking a thread until the future completes if
    * that thread is the submitter (the one in the transaction) and the transaction has not yet been
    * committed.
    * 
    * <p>This is necessary since the task isn't actually submitted for execution until the
    * transaction is committed. So the future can never complete until after the transaction is
    * committed; blocking on the future before then cannot work and would deadlock.
    * 
    * <p>So the {@code get} and {@code await} methods on this future will immediately throw an
    * {@link IllegalStateException} if called while still inside the uncommitted transaction.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class TransactionalFutureTask<T> implements RunnableFluentFuture<T> {

      /**
       * The actual task that will run once submitted for execution.
       */
      private final RunnableFluentFuture<T> delegate;
      
      /**
       * The thread that created/submitted this task inside a transaction.
       */
      private final Thread submitter = Thread.currentThread();
      
      /**
       * A flag that indicates if this task is still pending (e.g. transaction not committed). If
       * false, the future has been scheduled for execution or has been cancelled.
       */
      private volatile boolean pending = true;
      
      TransactionalFutureTask(RunnableFluentFuture<T> delegate) {
         this.delegate = delegate;
      }
      
      /**
       * Indicates that the transaction has been committed and this task submitted for execution.
       */
      void markCommitted() {
         pending = false;
      }

      @Override
      public void run() {
         delegate.run();
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         pending = false;
         return delegate.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
         return delegate.isCancelled();
      }

      @Override
      public boolean isDone() {
         return delegate.isDone();
      }
      
      /**
       * Checks whether the current thread is allowed to block until this future is complete. If the
       * thread cannot block, an {@link IllegalStateException} is thrown. A thread is not allowed to
       * block if it would cause deadlock in the transaction that scheduled the task.
       */
      void checkCanBlock() {
         // If the transaction hasn't been committed, then the submitter thread isn't allowed to
         // block because that would cause the transaction to freeze in a deadlock!
         if (!pending && Thread.currentThread() == submitter) {
            throw new IllegalStateException(
                  "Cannot block on future until corresponding transaction is committed");
         }
      }

      @Override
      public T getNow(T valueIfIncomplete) {
         return delegate.getNow(valueIfIncomplete);
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         checkCanBlock();
         return delegate.get();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         checkCanBlock();
         return delegate.get();
      }

      @Override
      public void await() throws InterruptedException {
         checkCanBlock();
         delegate.await();
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         checkCanBlock();
         return delegate.await(limit, unit);
      }

      @Override
      public boolean isSuccessful() {
         return delegate.isSuccessful();
      }

      @Override
      public T getResult() {
         return delegate.getResult();
      }

      @Override
      public boolean isFailed() {
         return delegate.isFailed();
      }

      @Override
      public Throwable getFailure() {
         return delegate.getFailure();
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
         delegate.addListener(listener, executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         delegate.visit(visitor);
      }
      
      @Override
      public void awaitUninterruptibly() {
         delegate.awaitUninterruptibly();
      }

      @Override
      public boolean awaitUninterruptibly(long limit, TimeUnit unit) {
         return delegate.awaitUninterruptibly(limit, unit);
      }

      @Override
      public void visitWhenDone(FutureVisitor<? super T> visitor) {
         delegate.visitWhenDone(visitor);
      }

      @Override
      public <U> FluentFuture<U> chainTo(Callable<U> task, Executor executor) {
         return delegate.chainTo(task, executor);
      }

      @Override
      public <U> FluentFuture<U> chainTo(Runnable task, U result, Executor executor) {
         return delegate.chainTo(task, result, executor);
      }

      @Override
      public FluentFuture<Void> chainTo(Runnable task, Executor executor) {
         return delegate.chainTo(task, executor);
      }

      @Override
      public <U> FluentFuture<U> chainTo(Function<? super T, ? extends U> task,
            Executor executor) {
         return delegate.chainTo(task, executor);
      }

      @Override
      public <U> FluentFuture<U> map(Function<? super T, ? extends U> function) {
         return delegate.map(function);
      }

      @Override
      public <U> FluentFuture<U> flatMap(
            Function<? super T, ? extends FluentFuture<U>> function) {
         return delegate.flatMap(function);
      }

      @Override
      public <U> FluentFuture<U> mapFuture(
            Function<? super FluentFuture<T>, ? extends U> function) {
         return delegate.mapFuture(function);
      }

      @Override
      public FluentFuture<T> mapException(
            Function<Throwable, ? extends Throwable> function) {
         return delegate.mapException(function);
      }

      @Override
      public FluentFuture<T> recover(Function<Throwable, ? extends T> function) {
         return delegate.recover(function);
      }

      @Override
      public <U, V> FluentFuture<V> combineWith(FluentFuture<U> other,
            BiFunction<? super T, ? super U, ? extends V> function) {
         return delegate.combineWith(other, function);
      }

      @Override
      public <U, V, W> FluentFuture<W> combineWith(FluentFuture<U> other1,
            FluentFuture<V> other2,
            TriFunction<? super T, ? super U, ? super V, ? extends W> function) {
         return delegate.combineWith(other1, other2, function);
      }

      @Override
      public CompletionStage<T> asCompletionStage() {
         return delegate.asCompletionStage();
      }
   }
}
