package com.apriori.concurrent.atoms;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.ListenableFutureTask;
import com.apriori.concurrent.PipeliningExecutorService;
import com.apriori.possible.Reference;
import com.apriori.util.Function;
import com.apriori.util.Functions;
import com.apriori.util.Predicate;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An atom that is mutated asynchronously using a thread pool. For a given atom, all mutations are
 * applied sequentially. Multiple threads in a pool are used to process mutations on multiple such
 * atoms. Since the actual update is applied asynchronously, mutation methods return future values,
 * not immediate values.
 * 
 * <p>If an error occurs while applying a change, the future that represents that change fails.
 * Additionally, the atom will consult an error handler to decide what to do. There are two options:
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
//TODO: tests
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
   private static final PipeliningExecutorService<AsynchronousAtom<?>> threadPool =
         new PipeliningExecutorService<AsynchronousAtom<?>>(Executors.newCachedThreadPool(
               new ThreadFactory() {
                  private final AtomicInteger id = new AtomicInteger();
                  
                  @Override public Thread newThread(Runnable r) {
                     Thread ret = new Thread(r);
                     ret.setDaemon(true);
                     ret.setName("AsynchronousAtom " + String.valueOf(id.incrementAndGet()));
                     return ret;
                  }
               }));

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
    */
   private final ConcurrentLinkedQueue<ListenableFutureTask<T>> queued =
         new ConcurrentLinkedQueue<ListenableFutureTask<T>>();
   
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
               while (!queued.isEmpty() && !blocked) {
                  runSingleTask(queued.remove());
               }
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
      final AtomicBoolean success = new AtomicBoolean();
      final CountDownLatch latch = new CountDownLatch(1);
      threadPool.execute(this, new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
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
                  while (!queued.isEmpty()) {
                     ListenableFutureTask<T> op = queued.remove();
                     op.cancel(false);
                  }
                  queued.clear();
               } else {
                  while (!queued.isEmpty() && !blocked) {
                     runSingleTask(queued.remove());
                  }
               }
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
    * Returns the number of queued mutation operations. Operations are queued only when an error
    * causes the atom to become blocked. {@linkplain #resume() Resuming} the atom will drain the
    * queue and process pending operations.
    *
    * @return the length of the queue of pending mutations
    */
   public int getQueueLength() {
      return queued.size();
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
   public ListenableFuture<T> getPending() {
      return apply(Functions.<T>identityFunction());
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
   public ListenableFuture<T> set(final T newValue) {
      validate(newValue);
      return submit(new Callable<T>() {
         @SuppressWarnings("synthetic-access")
         @Override
         public T call() {
            T oldValue = value;
            value = newValue;
            AsynchronousAtom.this.notify(oldValue, newValue);
            return oldValue;
         }
      });
   }

   /**
    * Submits a mutation that will apply the specified function and set the atom's value to its
    * result. Validation cannot be done immediately, so a validation failure manifests as a failed
    * future result. Depending on the atom's {@link #getErrorHandler() error handler}, a validation
    * failure could block subsequent mutations.
    *
    * @param function the function to apply; the atom's new value will be the result of applying
    *       this function to the atom's previous value
    * @return a future result that will be the atom's new value after the function is applied
    */
   public ListenableFuture<T> apply(final Function<? super T, ? extends T> function) {
      return submit(new Callable<T>() {
         @SuppressWarnings("synthetic-access")
         @Override
         public T call() throws Exception {
            T oldValue = value;
            T newValue = function.apply(oldValue);
            validate(newValue);
            value = newValue;
            AsynchronousAtom.this.notify(oldValue, newValue);
            return newValue;
         }
      });
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
   private ListenableFuture<T> submit(Callable<T> task) {
      final ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
      Transaction transaction = Transaction.current();
      if (transaction != null) {
         // play nice with transactions -- queue up actions so that they are only submitted
         // when the transaction gets committed
         transaction.enqueueAsynchronousAction(this, future);
      } else {
         submitFuture(this, future);
      }
      return future;
   }
   
   /**
    * Submits the specified operation to a thread pool. The operation is queued behind any other
    * operations for the same specified atom.
    *
    * @param atom the atom which this operation affects
    * @param future a future task that will perform an operation on the specified atom when executed
    */
   static <T> void submitFuture(final AsynchronousAtom<T> atom,
         final ListenableFutureTask<T> future) {
      threadPool.execute(atom, new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override 
         public void run() {
            atom.runSingleTask(future);
         }
      });
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
   private void runSingleTask(ListenableFutureTask<T> task) {
      if (blocked) {
         queued.add(task);
      } else {
         task.run();
         if (task.isFailed()) {
            switch (errorHandler.onError(AsynchronousAtom.this, task.getFailure())) {
               case BLOCK:
                  blocked = true;
                  break;
               case RESTART:
                  value = seedValue;
                  break;
               default:
                  // IGNORE
                  break;
            }
         }
      }
   }
}
