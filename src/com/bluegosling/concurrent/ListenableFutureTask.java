package com.bluegosling.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future task that implements the {@link ListenableFuture} interface. This builds on the
 * {@link FutureTask} implementation provided by the Java runtime library and extends it with
 * additional API.
 * 
 * <p>Another implementation of the {@link ListenableFuture} interface that doesn't necessarily
 * represent a runnable task is {@link AbstractListenableFuture}. That other class also implements
 * some of the new API more efficiently since it doesn't operate within constraints inherited from
 * {@link FutureTask} (which was designed to provide a more simplistic API).
 * 
 * <p>Unlike its super-class, the blocking methods in this class ({@link #get} and {@link #await})
 * are safe to call from a fork-join pool. When called from within a fork-join pool, these methods
 * use a {@link ManagedBlocker} instead of directly blocking.

 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of result produced by the future task
 * 
 * @see AbstractListenableFuture
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements RunnableListenableFuture<T> {

   /**
    * Sentinel value for the future's disposition indicating that it was cancelled.
    */
   private static final Object CANCELLED = new Object();

   /**
    * Sentinel value for the future's disposition indicating that it succeeded with a null value.
    */
   private static final Object NULL_RESULT = new Object();

   /**
    * Sentinel type for the future's disposition indicating that it failed.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Failure {
      final Throwable cause;
      
      Failure(Throwable cause) {
         this.cause = cause;
      }
   }
   
   /**
    * The final disposition of the future. This mirrors the state of the super-class since the
    * super-class doesn't actually provide any useful accessors. Without mirroring state, we only
    * have {@link FutureTask#get()} for inspecting state. That would require catching and handling
    * exceptions in many cases where a failed future isn't really exceptional. So we trade away the
    * code complexity (lots of ugly {@code try/catch} blocks in every method) and the potential
    * performance penalty (creating exceptions isn't free) for a little extra memory overhead.
    * 
    * <p>This value is {@code null} until the future is complete. Once complete, this value is
    * {@link #CANCELLED} if the future was cancelled, it is an instance of {@link Failure} if the
    * future failed, or it is the actual value of the future if it was successful.
    */
   private volatile Object disposition;
   
   /**
    * The set of listeners that will be invoked when this future completes.
    */
   private final FutureListenerSet<T> listeners = new FutureListenerSet<T>(this);
   
   /**
    * Constructs a new task for the specified callable. When {@linkplain #run() run}, the callable
    * is invoked and the future completes.
    * 
    * @param callable the task that produces the future result
    */
   public ListenableFutureTask(Callable<T> callable) {
      super(callable);
   }

   /**
    * Constructs a new task for the specified command and result. When {@linkplain #run() run}, the
    * runnable is invoked and the future completes.
    * 
    * @param runnable the task that is executed
    * @param result the future result
    */
   public ListenableFutureTask(Runnable runnable, T result) {
      super(runnable, result);
   }

   @Override
   public void addListener(FutureListener<? super T> listener, Executor executor) {
      listeners.addListener(listener, executor);
   }
   
   /**
    * Determines the disposition of the current future and memoizes the result.
    *
    * @return the current disposition of the future;
    */
   private Object determineDisposition() {
      Object d = disposition;
      if (d != null) {
         return d;
      }
      if (!isDone()) {
         return null;
      }
      if (isCancelled()) {
         return (disposition = CANCELLED);
      }
      boolean interrupted = false;
      try {
         while (true) {
            try {
               T value = get();
               return disposition = (value == null ? NULL_RESULT : value);
            } catch (ExecutionException e) {
               return (disposition = new Failure(e.getCause()));
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
   
   @Override
   protected void done() {
      determineDisposition();
      listeners.run();
   }
   
   // for implementing ManagedBlocker
   void internalBlock() throws InterruptedException {
      try {
         super.get();
      } catch (ExecutionException | CancellationException e) {
         // ignored
      }
   }

   // for implementing ManagedBlocker
   void internalBlock(long nanos) throws InterruptedException {
      try {
         super.get(nanos, TimeUnit.NANOSECONDS);
      } catch (ExecutionException | CancellationException | TimeoutException e) {
         // ignored
      }
   }
   
   @Override
   public T get() throws ExecutionException, InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         ForkJoinPool.managedBlock(new Blocker());
      }
      return super.get();
   }

   @Override
   public T get(long timeout, TimeUnit unit)
         throws ExecutionException, TimeoutException, InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         ForkJoinPool.managedBlock(new TimedBlocker(timeout, unit));
         if (!isDone()) {
            throw new TimeoutException();
         }
         return super.get();
      } else {
         return super.get(timeout, unit);
      }
   }

   @Override
   public boolean isSuccessful() {
      Object d = determineDisposition();
      return d != null && d != CANCELLED && !(d instanceof Failure);
   }

   @Override
   public T getResult() {
      Object d = determineDisposition();
      if (d == null || d == CANCELLED || d instanceof Failure) {
         throw new IllegalStateException();
      }
      if (d == NULL_RESULT) {
         return null;
      }
      @SuppressWarnings("unchecked")
      T t = (T) d;
      return t;
   }

   @Override
   public boolean isFailed() {
      return determineDisposition() instanceof Failure;
   }

   @Override
   public Throwable getFailure() {
      Object d = determineDisposition();
      if (!(d instanceof Failure)) {
         throw new IllegalStateException();
      }
      return ((Failure) d).cause;
   }

   @Override
   public void visit(FutureVisitor<? super T> visitor) {
      Object d = determineDisposition();
      if (d == null) {
         throw new IllegalStateException();
      }
      if (d instanceof Failure) {
         visitor.failed(((Failure) d).cause);
      } else if (d == CANCELLED) {
         visitor.cancelled();
      } else if (d == NULL_RESULT) {
         visitor.successful(null);
      } else {
         @SuppressWarnings("unchecked")
         T t = (T) d;
         visitor.successful(t);
      }
   }

   @Override
   public void await() throws InterruptedException {
      if (isDone()) {
         return;
      }
      
      if (ForkJoinTask.inForkJoinPool()) {
         ForkJoinPool.managedBlock(new Blocker());
         return;
      }
      
      try {
         super.get();
      } catch (ExecutionException | CancellationException e) {
         // ignored
      }
   }

   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      if (isDone()) {
         return true;
      }
      
      if (ForkJoinTask.inForkJoinPool()) {
         ForkJoinPool.managedBlock(new TimedBlocker(limit, unit));
         return isDone();
      }
      
      try {
         super.get(limit, unit);
      } catch (TimeoutException e) {
         return false;
      } catch (ExecutionException | CancellationException e) {
         // fall-through
      }
      return true;
   }
   
   /**
    * A helper class for blocking on the future's completion in a {@link ForkJoinPool}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class Blocker implements ManagedBlocker {
      
      Blocker() {
      }
      
      @Override
      public boolean block() throws InterruptedException {
         internalBlock();
         return true;
      }

      @Override
      public boolean isReleasable() {
         return isDone();
      }
   }

   /**
    * A helper class for blocking up to a time limit on the future's completion in a
    * {@link ForkJoinPool}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class TimedBlocker implements ManagedBlocker {

      private final long deadline;
      
      TimedBlocker(long timeout, TimeUnit unit) {
         long now = System.nanoTime();
         long end = now + unit.toNanos(Math.max(0, timeout));
         // avoid overflow
         if (end < now) {
            end = Long.MAX_VALUE;
         }
         this.deadline = end;
      }

      @Override
      public boolean block() throws InterruptedException {
         internalBlock(deadline - System.nanoTime());
         return true;
      }

      @Override
      public boolean isReleasable() {
         return isDone() || System.nanoTime() >= deadline;
      }
   }
}
