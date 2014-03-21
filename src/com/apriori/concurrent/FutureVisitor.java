package com.apriori.concurrent;

/**
 * Interface for the visitor pattern with {@link ListenableFuture}s. When a visitor is passed to
 * {@link ListenableFuture#visit(FutureVisitor)}, the future calls one of these methods, depending
 * on the actual disposition of the future.
 * 
 * <p>This is particularly handy, when combined with {@link
 * FutureListener#forVisitor(FutureVisitor)}, for implementing listeners and asynchronously
 * handling futures when they complete.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of result value visited
 */
public interface FutureVisitor<T> {
   /**
    * Invoked when the visited future completed successfully.
    * 
    * @param result the result of the visited future
    */
   void successful(T result);
   
   /**
    * Invoked when the visited future failed.
    * 
    * @param failure the cause of failure of the visited future
    */
   void failed(Throwable failure);
   
   /**
    * Invoked when the visited future was cancelled.
    */
   void cancelled();
}