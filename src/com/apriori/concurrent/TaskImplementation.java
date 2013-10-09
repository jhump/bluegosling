package com.apriori.concurrent;

import com.apriori.util.Source;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * The implementation for a task that is executed using a {@link ScheduledTaskManager}. The task
 * can be implemented by either a {@link Callable}, a {@link Runnable}, or a {@link Source}.
 * 
 * <p>This class implements {@link Callable}. Calling this task will forward to the underlying
 * implementation, so this object also serves as an adapter from any type of implementation to
 * {@link Callable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of result produced by the task
 */
public abstract class TaskImplementation<V> implements Callable<V> {
   TaskImplementation() {
   }

   /**
    * A visitor, for accessing the underlying task implementation using the visitor pattern.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <R> the result of visiting a task implementation
    * @param <V> the type of result of visited tasks
    */
   public interface Visitor<R, V> {
      /**
       * Visits the underlying callable, for tasks implemented by a callable.
       * 
       * @param callable the task's underlying callable
       * @return the result of visiting the callable
       */
      R visitCallable(Callable<? extends V> callable); 

      /**
       * Visits the underlying runnable and associated result, for tasks implemented by a runnable.
       * 
       * @param runnable the task's underlying runnable
       * @param result the result produced on completion of the runnable
       * @return the result of visiting the runnable
       */
      R visitRunnable(Runnable runnable, V result); 

      /**
       * Visits the underlying source, for tasks implemented by a source.
       * 
       * @param source the task's underlying source
       * @return the result of visiting the source
       */
      R visitSource(Source<? extends V> source); 
   }
   
   /**
    * Visits this task using the specified visitor. Depending on the type of implementation, one of
    * the visitor's methods will be called back.
    * 
    * @param visitor the visitor
    * @return the value returned by the visitor method that gets invoked
    */
   public abstract <R> R visit(Visitor<R, ? super V> visitor);
   
   /**
    * Returns a task for the specified callable implementation.
    * 
    * @param callable the callable that will underlie this task
    * @return a task that represents the specified callable
    */
   public static <V> TaskImplementation<V> forCallable(final Callable<V> callable) {
      return new TaskImplementation<V>() {
         @Override
         public <R> R visit(Visitor<R, ? super V> visitor) {
            return visitor.visitCallable(callable);
         }

         @Override
         public V call() throws Exception {
            return callable.call();
         }
      };
   }
   
   /**
    * Returns a task for the specified runnable implementation.
    * 
    * @param runnable the runnable that will underlie this task
    * @return a task that represents the specified runnable
    */
   public static TaskImplementation<Void> forRunnable(Runnable runnable) {
      return forRunnable(runnable, null);
   }

   /**
    * Returns a task for the specified runnable implementation.
    * 
    * @param runnable the runnable that will underlie this task
    * @param result the result value produced when the runnable completes
    * @return a task that represents the specified runnable
    */
   public static <V> TaskImplementation<V> forRunnable(final Runnable runnable, final V result) {
      return new TaskImplementation<V>() {
         @Override
         public <R> R visit(Visitor<R, ? super V> visitor) {
            return visitor.visitRunnable(runnable, result);
         }
         
         @Override
         public V call() throws Exception {
            return Executors.callable(runnable, result).call();
         }
      };
   }

   /**
    * Returns a task for the specified source implementation.
    * 
    * @param source the source that will underlie this task
    * @return a task that represents the specified source
    */
   public static <V> TaskImplementation<V> forSource(final Source<V> source) {
      return new TaskImplementation<V>() {
         @Override
         public <R> R visit(Visitor<R, ? super V> visitor) {
            return visitor.visitSource(source);
         }

         @Override
         public V call() throws Exception {
            return source.get();
         }
      };
   }
}
