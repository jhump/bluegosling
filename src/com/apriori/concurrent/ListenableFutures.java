package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListeners.forVisitor;

import com.apriori.util.Fulfillable;
import com.apriori.util.Fulfillables;
import com.apriori.util.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: tests
//TODO: javadoc
//TODO: makeListenable(ScheduledExecutorService)
//TODO: makeListenable(ScheduledFuture)
public final class ListenableFutures {
   private ListenableFutures() {
   }
   
   private static final Executor SAME_THREAD_EXECUTOR = new Executor() {
      @Override
      public void execute(Runnable command) {
         try {
            command.run();
         } catch (Throwable t) {
            if (t instanceof InterruptedException) {
               Thread.currentThread().interrupt();
            }
         }
      }
   };

   private static class ListeningExecutorServiceWrapper implements ListenableExecutorService {
      private final ExecutorService delegate;
      
      ListeningExecutorServiceWrapper(ExecutorService delegate) {
         this.delegate = delegate;
      }

      @Override
      public void execute(Runnable command) {
         delegate.execute(command);
      }

      @Override
      public void shutdown() {
         delegate.shutdown();
      }

      @Override
      public List<Runnable> shutdownNow() {
         return delegate.shutdownNow();
      }

      @Override
      public boolean isShutdown() {
         return delegate.isShutdown();
      }

      @Override
      public boolean isTerminated() {
         return delegate.isTerminated();
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
         return delegate.awaitTermination(timeout, unit);
      }

      @Override
      public <T> ListenableFuture<T> submit(Callable<T> task) {
         ListenableFutureTask<T> t = new ListenableFutureTask<T>(task);
         delegate.execute(t);
         return t;
      }

      @Override
      public <T> ListenableFuture<T> submit(Runnable task, T result) {
         ListenableFutureTask<T> t = new ListenableFutureTask<T>(task, result);
         delegate.execute(t);
         return t;
      }

      @Override
      public ListenableFuture<Void> submit(Runnable task) {
         return submit(task, null);
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
         List<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
         for (Callable<T> task : tasks) {
            results.add(submit(task));
         }
         for (Future<T> future : results) {
            try {
               future.get();
            } catch (CancellationException ignore) {
            } catch (ExecutionException ignore) {
            }
         }
         return results;
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
         long startNanos = System.nanoTime();
         List<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
         for (Callable<T> task : tasks) {
            results.add(submit(task));
         }
         boolean timedOut = false;
         for (Future<T> future : results) {
            try {
               if (timedOut) {
                  future.cancel(true);
               } else {
                  long spent = System.nanoTime() - startNanos;
                  long nanosLeft = unit.toNanos(timeout) - spent;
                  future.get(nanosLeft, TimeUnit.NANOSECONDS);
               }
            } catch (TimeoutException e) {
               future.cancel(true);
               timedOut = true;
            } catch (CancellationException ignore) {
            } catch (ExecutionException ignore) {
            }
         }
         return results;
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
            ExecutionException {
         return delegate.invokeAny(tasks);
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
         return delegate.invokeAny(tasks, timeout, unit);
      }
   }

   public static <T> ListenableFuture<T> makeListenable(final Future<T> future) {
      if (future instanceof ListenableFuture) {
         return (ListenableFuture<T>) future;
      }
      final SimpleListenableFuture<T> result = new SimpleListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               future.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      new Thread() {
         @Override public void run() {
            try {
               boolean interrupted = false;
               while (true) {
                  try {
                     result.setValue(future.get());
                     break;
                  } catch (InterruptedException e) {
                     interrupted = true;
                  }
               }
               if (interrupted) {
                  Thread.currentThread().interrupt(); // restore interrupt status
               }
            } catch (ExecutionException e) {
               result.setFailure(e.getCause());
            } catch (CancellationException e) {
               result.setCancelled();
            }
         }
      }.start();
      return result;
   }
   
   public static ListenableExecutorService makeListenable(ExecutorService executor) {
      if (executor instanceof ListenableExecutorService) {
         return (ListenableExecutorService) executor;
      }
      return new ListeningExecutorServiceWrapper(executor);
   }
   
   public static Executor sameThreadExecutor() {
      return SAME_THREAD_EXECUTOR;
   }
   
   public static <T> void addCallback(ListenableFuture<T> future,
         FutureVisitor<? super T> visitor) {
      future.addListener(forVisitor(visitor), sameThreadExecutor());
   }
   
   public static <T> ListenableFuture<T> chain(ListenableFuture<?> future, final Callable<T> task,
         Executor executor) {
      return chain(future, new Function<Object, T>() {
         @Override public T apply(Object unused) {
            try {
               return task.call();
            } catch (Exception e) {
               throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
         }
      }, executor);
   }

   public static <T> ListenableFuture<T> chain(ListenableFuture<?> future, final Runnable task,
         final T result, Executor executor) {
      return chain(future, new Function<Object, T>() {
         @Override public T apply(Object unused) {
            task.run();
            return result;
         }
      }, executor);
   }

   public static ListenableFuture<Void> chain(ListenableFuture<?> future, Runnable task,
         Executor executor) {
      return chain(future, task, null, executor);
   }

   public static <T, U> ListenableFuture<U> chain(ListenableFuture<T> future,
         final Function<? super T, ? extends U> function, Executor executor) {
      final SimpleListenableFuture<U> result = new SimpleListenableFuture<U>();
      future.addListener(forVisitor(new FutureVisitor<T>() {
         @Override
         public void successful(T t) {
            try {
               result.setValue(function.apply(t));
            } catch (Throwable th) {
               result.setFailure(th);
            }
         }
   
         @Override
         public void failed(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      }), executor);
      return result;
   }

   public static <T, U> ListenableFuture<U> transform(final ListenableFuture<T> future,
         final Function<? super T, ? extends U> function) {
      final SimpleListenableFuture<U> result = new SimpleListenableFuture<U>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               future.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      addCallback(future, new FutureVisitor<T>() {
         @Override
         public void successful(T t) {
            try {
               result.setValue(function.apply(t));
            } catch (Throwable th) {
               result.setFailure(th);
            }
         }
   
         @Override
         public void failed(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      });
      return result;
   }
   
   public static <T> ListenableFuture<T> completedFuture(final T value) {
      return new ListenableFuture<T>() {
         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
         }

         @Override
         public boolean isCancelled() {
            return false;
         }

         @Override
         public boolean isDone() {
            return true;
         }

         @Override
         public T get() {
            return value;
         }

         @Override
         public T get(long timeout, TimeUnit unit) {
            return value;
         }

         @Override
         public boolean isSuccessful() {
            return true;
         }

         @Override
         public T getResult() {
            return value;
         }

         @Override
         public boolean isFailed() {
            return false;
         }

         @Override
         public Throwable getFailure() {
            throw new IllegalStateException();
         }

         @Override
         public void addListener(final FutureListener<? super T> listener, Executor executor) {
            final ListenableFuture<T> f = this;
            executor.execute(new Runnable() {
               @Override
               public void run() {
                  listener.onCompletion(f);
               }
            });
         }

         @Override
         public void visit(FutureVisitor<? super T> visitor) {
            visitor.successful(value);
         }
      };
   }
   
   public static <T> ListenableFuture<T> failedFuture(final Throwable failure) {
      return new ListenableFuture<T>() {
         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
         }

         @Override
         public boolean isCancelled() {
            return false;
         }

         @Override
         public boolean isDone() {
            return true;
         }

         @Override
         public T get() throws ExecutionException {
            throw new ExecutionException(failure);
         }

         @Override
         public T get(long timeout, TimeUnit unit) throws ExecutionException {
            throw new ExecutionException(failure);
         }

         @Override
         public boolean isSuccessful() {
            return false;
         }

         @Override
         public T getResult() {
            throw new IllegalStateException();
         }

         @Override
         public boolean isFailed() {
            return true;
         }

         @Override
         public Throwable getFailure() {
            return failure;
         }

         @Override
         public void addListener(final FutureListener<? super T> listener, Executor executor) {
            final ListenableFuture<T> f = this;
            executor.execute(new Runnable() {
               @Override
               public void run() {
                  listener.onCompletion(f);
               }
            });
         }

         @Override
         public void visit(FutureVisitor<? super T> visitor) {
            visitor.failed(failure);
         }
      };
   }
   
   public static <T> ListenableFuture<List<T>> join(ListenableFuture<? extends T>... futures) {
      return join(Arrays.asList(futures));
   }

   public static <T> ListenableFuture<List<T>> join(
         final Iterable<ListenableFuture<? extends T>> futures) {
      List<ListenableFuture<? extends T>> futureList;
      if (futures instanceof List) {
         futureList = (List<ListenableFuture<? extends T>>) futures;
      } else {
         if (futures instanceof Collection) {
            futureList = new ArrayList<ListenableFuture<? extends T>>(
                  (Collection<ListenableFuture<? extends T>>) futures);
         } else {
            futureList = new ArrayList<ListenableFuture<? extends T>>();
            for (ListenableFuture<? extends T> future : futures) {
               futureList.add(future);
            }
         }
      }
      if (futureList.isEmpty()) {
         return completedFuture(Collections.<T>emptyList());
      }
      final int len = futureList.size();
      if (len == 1) {
         return transform(futureList.get(0), new Function<T, List<T>>() {
            @Override
            public List<T> apply(T input) {
               return Collections.singletonList(input);
            }
         });
      }
      final List<Fulfillable<T>> resolved = new ArrayList<Fulfillable<T>>(len);
      for (int i = 0; i < len; i++) {
         resolved.add(Fulfillables.<T>create());
      }
      @SuppressWarnings({"unchecked", "rawtypes"}) // java generics not expressive enough
      CombiningFuture<List<T>> result = new CombiningFuture<List<T>>((Collection)futureList) {
         @Override List<T> computeValue() {
            List<T> list = new ArrayList<T>(len);
            for (Fulfillable<T> f : resolved) {
               list.add(f.get());
            }
            return list;
         }
      };
      for (int i = 0; i < len; i++) {
         futureList.get(i).addListener(
               forVisitor(new CombiningVisitor<T>(resolved.get(i), result)),
               sameThreadExecutor());
      }
      return result;
   }
   
   public static <T, U, V> ListenableFuture<V> combine(
         ListenableFuture<T> future1, ListenableFuture<U> future2,
         final Function.Bivariate<? super T, ? super U, ? extends V> function) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-args array is safe here
      CombiningFuture<V> result = new CombiningFuture<V>(Arrays.asList(future1, future2)) {
         @Override V computeValue() {
            return function.apply(t.get(), u.get());
         }
      };
      future1.addListener(forVisitor(new CombiningVisitor<T>(t, result)), sameThreadExecutor());
      future2.addListener(forVisitor(new CombiningVisitor<U>(u, result)), sameThreadExecutor());
      return result;
   }

   public static <T, U, V, W> ListenableFuture<W> combine(ListenableFuture<T> future1,
         ListenableFuture<U> future2, ListenableFuture<V> future3,
         final Function.Trivariate<? super T, ? super U, ? super V, ? extends W> function) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-args array is safe here
      CombiningFuture<W> result = new CombiningFuture<W>(Arrays.asList(future1, future2, future3)) {
         @Override W computeValue() {
            return function.apply(t.get(), u.get(), v.get());
         }
      };
      future1.addListener(forVisitor(new CombiningVisitor<T>(t, result)), sameThreadExecutor());
      future2.addListener(forVisitor(new CombiningVisitor<U>(u, result)), sameThreadExecutor());
      future3.addListener(forVisitor(new CombiningVisitor<V>(v, result)), sameThreadExecutor());
      return result;
   }
   
   public static <T> ListenableFuture<T> dereference(
         final ListenableFuture<? extends ListenableFuture<T>> future) {
      final SimpleListenableFuture<T> result = new SimpleListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               future.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      future.addListener(forVisitor(new FutureVisitor<ListenableFuture<T>>() {
         @Override
         public void successful(ListenableFuture<T> value) {
            value.addListener(forVisitor(new FutureVisitor<T>() {
               @Override
               public void successful(T t) {
                  result.setValue(t);
               }

               @Override
               public void failed(Throwable t) {
                  result.setFailure(t);
               }
               
               @Override
               public void cancelled() {
                  result.setCancelled();
               }
            }), sameThreadExecutor());
         }
   
         @Override
         public void failed(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      }), sameThreadExecutor());
      return result;
   }
   
   static abstract class CombiningFuture<T> extends SimpleListenableFuture<T> {
      private final Collection<ListenableFuture<?>> components;
      private final AtomicInteger remaining;
      
      CombiningFuture(Collection<ListenableFuture<?>> components) {
         this.components = components;
         remaining = new AtomicInteger(components.size());
      }
      
      abstract T computeValue();
      
      void mark() {
         if (remaining.decrementAndGet() == 0) {
            try {
               setValue(computeValue());
            } catch (Throwable t) {
               setFailure(t);
            }
         }
      }
      
      @Override public boolean cancel(boolean mayInterrupt) {
         if (super.cancel(mayInterrupt)) {
            for (ListenableFuture<?> future : components) {
               future.cancel(mayInterrupt);
            }
            return true;
         }
         return false;
      }
   }

   static class CombiningVisitor<T> implements FutureVisitor<T> {
      private final Fulfillable<T> component;
      private final CombiningFuture<?> result;
      
      CombiningVisitor(Fulfillable<T> component, CombiningFuture<?> result) {
         this.component = component;
         this.result = result;
      }
      
      @Override
      public void successful(T t) {
         if (component.fulfill(t)) {
            result.mark();
         }
      }

      @Override
      public void failed(Throwable t) {
         result.setFailure(t);
      }

      @Override
      public void cancelled() {
         result.setCancelled();
      }
   }
}
