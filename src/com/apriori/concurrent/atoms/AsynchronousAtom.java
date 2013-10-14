package com.apriori.concurrent.atoms;

import com.apriori.concurrent.ListenableExecutors;
import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.ListenableFutureTask;
import com.apriori.concurrent.PipeliningExecutorService;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

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
 * </ol>
 * 
 * <p>If a {@link Transaction} is in progress, then a mutation is not actually submitted to the
 * thread pool until the transaction is committed. So, like {@link TransactionalAtom}s but unlike
 * other types of atoms, mutations to asynchronous atoms can be rolled back.
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
      BLOCK;
      
      private ErrorHandler<Object> errorHandler = new ErrorHandler<Object>() {
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
   // TODO: maybe a thread pool w/ limited number of threads
   // TODO: use thread factory for named, daemon threads
   private static final PipeliningExecutorService<AsynchronousAtom<?>> threadPool =
         new PipeliningExecutorService<AsynchronousAtom<?>>(
               ListenableExecutors.makeListenable(Executors.newCachedThreadPool()));

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
    * Constructs a new asynchronous atom with a {@code null} value and no validator.
    */
   public AsynchronousAtom() {
   }

   /**
    * Constructs a new asynchronous atom with the specified value and no validator.
    */
   public AsynchronousAtom(T value) {
      this.value = value;
   }

   /**
    * Constructs a new asynchronous atom with the specified value and the specified validator.
    */
   public AsynchronousAtom(T value, Predicate<? super T> validator) {
      super(validator);
      this.value = value;
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
      threadPool.submit(this, new Runnable() {
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
    * Returns the number of queued mutation operations. Operations are queued only when an error
    * causes the atom to become blocked. {@linkplain #resume() Resuming} the atom will drain the
    * queue and process pending operations.
    *
    * @return the length of the queue of pending mutations
    */
   public int getQueueLength() {
      return queued.size();
   }
   
   @Override
   public T get() {
      return value;
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
   
   static <T> void submitFuture(final AsynchronousAtom<T> atom,
         final ListenableFutureTask<T> future) {
      threadPool.submit(atom, new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override 
         public void run() {
            atom.runSingleTask(future);
         }
      });
   }
   
   private void runSingleTask(ListenableFutureTask<T> task) {
      if (blocked) {
         queued.add(task);
      } else {
         task.run();
         if (task.isFailed()) {
            blocked = errorHandler.onError(AsynchronousAtom.this, task.getFailure())
                  == ErrorAction.BLOCK;
         }
      }
   }
}
