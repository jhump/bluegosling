package com.apriori.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link Future} that also provides the fluent API of {@link CompletionStage}. This extracts a
 * useful interface from {@link CompletableFuture}.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface CompletionStageFuture<T> extends CompletionStage<T>, Future<T> {
   
   /**
    * Adapts a {@link CompletableFuture} to this interface. All methods on the returned object
    * simply delegate to the given future.
    *
    * @param f the completable future
    * @return the given completable future, as a {@link CompletionStageFuture}
    */
   static <T> CompletionStageFuture<T> fromCompletableFuture(CompletableFuture<T> f) {
      if (f instanceof CompletionStageFuture) {
         Future<T> future = f; // we upcast CompletableFuture -> Future so that javac sees that the
                              // next downcast to CompletionStageFuture is safe (otherwise, it
                              // complains that the downcast is unchecked)
         return (CompletionStageFuture<T>) future;
      }
      return new CompletionStageFuture<T>() {
         @Override
         public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
            return f.thenApply(fn);
         }

         @Override
         public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
            return f.thenApplyAsync(fn);
         }

         @Override
         public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn,
               Executor executor) {
            return f.thenApplyAsync(fn, executor);
         }

         @Override
         public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
            return f.thenAccept(action);
         }

         @Override
         public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
            return f.thenAcceptAsync(action);
         }

         @Override
         public CompletionStage<Void>
               thenAcceptAsync(Consumer<? super T> action, Executor executor) {
            return f.thenAcceptAsync(action, executor);
         }

         @Override
         public CompletionStage<Void> thenRun(Runnable action) {
            return f.thenRun(action);
         }

         @Override
         public CompletionStage<Void> thenRunAsync(Runnable action) {
            return f.thenRunAsync(action);
         }

         @Override
         public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
            return f.thenRunAsync(action, executor);
         }

         @Override
         public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
               BiFunction<? super T, ? super U, ? extends V> fn) {
            return f.thenCombine(other, fn);
         }

         @Override
         public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
               BiFunction<? super T, ? super U, ? extends V> fn) {
            return f.thenCombineAsync(other, fn);
         }

         @Override
         public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
               BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
            return f.thenCombineAsync(other, fn, executor);
         }

         @Override
         public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
               BiConsumer<? super T, ? super U> action) {
            return f.thenAcceptBoth(other, action);
         }

         @Override
         public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
               BiConsumer<? super T, ? super U> action) {
            return f.thenAcceptBothAsync(other, action);
         }

         @Override
         public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
               BiConsumer<? super T, ? super U> action, Executor executor) {
            return f.thenAcceptBothAsync(other, action, executor);
         }

         @Override
         public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
            return f.runAfterBoth(other, action);
         }

         @Override
         public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
            return f.runAfterBothAsync(other, action);
         }

         @Override
         public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
               Executor executor) {
            return f.runAfterBothAsync(other, action, executor);
         }

         @Override
         public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other,
               Function<? super T, U> fn) {
            return f.applyToEither(other, fn);
         }

         @Override
         public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
               Function<? super T, U> fn) {
            return f.applyToEitherAsync(other, fn);
         }

         @Override
         public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
               Function<? super T, U> fn, Executor executor) {
            return f.applyToEitherAsync(other, fn, executor);
         }

         @Override
         public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
               Consumer<? super T> action) {
            return f.acceptEither(other, action);
         }

         @Override
         public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
               Consumer<? super T> action) {
            return f.acceptEitherAsync(other, action);
         }

         @Override
         public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
               Consumer<? super T> action, Executor executor) {
            return f.acceptEitherAsync(other, action, executor);
         }

         @Override
         public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
            return f.runAfterEither(other, action);
         }

         @Override
         public CompletionStage<Void>
               runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
            return f.runAfterEitherAsync(other, action);
         }

         @Override
         public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other,
               Runnable action, Executor executor) {
            return f.runAfterEitherAsync(other, action, executor);
         }

         @Override
         public <U> CompletionStage<U> thenCompose(
               Function<? super T, ? extends CompletionStage<U>> fn) {
            return f.thenCompose(fn);
         }

         @Override
         public <U> CompletionStage<U> thenComposeAsync(
               Function<? super T, ? extends CompletionStage<U>> fn) {
            return f.thenComposeAsync(fn);
         }

         @Override
         public <U> CompletionStage<U> thenComposeAsync(
               Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
            return f.thenComposeAsync(fn, executor);
         }

         @Override
         public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
            return f.exceptionally(fn);
         }

         @Override
         public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
            return f.whenComplete(action);
         }

         @Override
         public CompletionStage<T>
               whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
            return f.whenCompleteAsync(action);
         }

         @Override
         public CompletionStage<T> whenCompleteAsync(
               BiConsumer<? super T, ? super Throwable> action, Executor executor) {
            return f.whenCompleteAsync(action, executor);
         }

         @Override
         public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
            return f.handle(fn);
         }

         @Override
         public <U> CompletionStage<U>
               handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
            return f.handleAsync(fn);
         }

         @Override
         public <U> CompletionStage<U> handleAsync(
               BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
            return f.handleAsync(fn, executor);
         }

         @Override
         public CompletableFuture<T> toCompletableFuture() {
            return f;
         }

         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            return f.cancel(mayInterruptIfRunning);
         }

         @Override
         public boolean isCancelled() {
            return f.isCancelled();
         }

         @Override
         public boolean isDone() {
            return f.isDone();
         }

         @Override
         public T get() throws InterruptedException, ExecutionException {
            return f.get();
         }

         @Override
         public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
               TimeoutException {
            return f.get(timeout, unit);
         }
      };
   }
   
   /**
    * Adapts a {@link CompletionStage} to this broader interface. The {@link Future} methods on the
    * returned object delegate to the completable future {@linkplaun #toCompletableFuture() provided
    * by} the given stage.
    *
    * @param stage a completion stage
    * @return the given stage, augmented as a {@link CompletionStageFuture}
    */
   static <T> CompletionStageFuture<T> fromCompletionStage(CompletionStage<T> stage) {
      if (stage instanceof CompletionStageFuture) {
         return (CompletionStageFuture<T>) stage;
      } else if (stage instanceof CompletableFuture) {
         return fromCompletableFuture((CompletableFuture<T>) stage);
      } else {
         return fromCompletableFuture(stage.toCompletableFuture());
      }
   }
}
