package com.apriori.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * The future to end all futures. This provides API improvements over the standard {@link Future} in
 * several categories:
 * <ol>
 * <li><strong>Callbacks</strong>: This allows for real asynchronous processing of futures and
 * is what gives this future its name. You can add listeners that are invoked when the future
 * completes.</li>
 * <li><strong>Blocking</strong>: This interface extends {@link Awaitable}, giving you more API
 * choices for blocking until the future completes (that do not require catching {@link
 * ExecutionException}, {@link CancellationException}, or {@link TimeoutException}).</li>
 * <li><strong>Inspecting</strong>: Numerous new methods are provided for inspecting the result of
 * a completed future, none of which require a {@code try/catch} block ({@link #isSuccessful()},
 * {@link #getResult()}, {@link #isFailed()}, {@link #getFailure()}, and
 * {@link #visit(FutureVisitor)}). All of these new methods are non-blocking and are intended to
 * assist with implementing listeners and with writing asynchronous code. Many will throw an
 * {@link IllegalStateException} if invoked before the future is done.</li>
 * </ol>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future value
 */
public interface ListenableFuture<T> extends Future<T>, Cancellable, Awaitable {
   /**
    * Returns true if the future is done and was successful.
    * 
    * @return true if the future is done and was successful; false otherwise
    */
   boolean isSuccessful();
   
   /**
    * Gets the result of a successful future. This method can only be called when the future is
    * done and was successful.
    * 
    * @return the result
    * @throws IllegalArgumentException if the future is not complete or was not successful
    */
   T getResult();
   
   /**
    * Returns true if the future is done and failed.
    * 
    * @return true if the future is done and failed; false otherwise
    */
   boolean isFailed();
   
   /**
    * Gets the cause of failure for a future. This method can only be called when the future is done
    * and has failed.
    * 
    * @return the cause of failure
    * @throws IllegalArgumentException if the future is not complete or did not fail
    */
   Throwable getFailure();
   
   /**
    * Adds a listener that will be called when the future completes. The listener will be invoked
    * using the specified executor. If the future is already complete when the listener is added,
    * the listener will be immediately invoked. If the listener can be called synchronously (e.g.
    * it will complete very quickly and not block) then consider using {@link
    * ListenableExecutors#sameThreadExecutor()}.
    * 
    * @param listener the listener
    * @param executor the executor used when calling the listener
    */
   void addListener(FutureListener<? super T> listener, Executor executor);
   
   /**
    * Invokes applicable methods on the specified visitor, depending on the disposition of this
    * future. The future must be complete in order to be visited.
    * 
    * @param visitor the visitor
    * @throws IllegalStateException if the future is not complete
    */
   void visit(FutureVisitor<? super T> visitor);
}
