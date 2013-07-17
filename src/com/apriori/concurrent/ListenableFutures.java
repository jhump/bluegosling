package com.apriori.concurrent;

import com.apriori.util.Function;
import com.apriori.util.Sink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: tests
//TODO: javadoc
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

   private static class ListeningExecutorServiceWrapper implements ListenableFuture.ExecutorService {
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
      public ListenableFuture<?> submit(Runnable task) {
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
            if (future.cancel(mayInterrupt)) {
               // when future is cancelled, listener below marks this cancelled, but
               // listener could be executed async. since we need this to be cancelled
               // before we return from this method, cancel now and then listener
               // will be a no-op
               setCancelled();
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
   
   public static ListenableFuture.ExecutorService makeListenable(ExecutorService executor) {
      if (executor instanceof ListenableFuture.ExecutorService) {
         return (ListenableFuture.ExecutorService) executor;
      }
      return new ListeningExecutorServiceWrapper(executor);
   }
   
   public static Executor sameThreadExecutor() {
      return SAME_THREAD_EXECUTOR;
   }
   
   public static <T> void addListener(ListenableFuture<T> future, Runnable listener) {
      future.addListener(listener, SAME_THREAD_EXECUTOR);
   }

   public static <T> void addSink(final ListenableFuture<T> future,
         final Sink<? super ListenableFuture<T>> sink) {
      addSink(future, sink, SAME_THREAD_EXECUTOR);
   }
   
   public static <T> void addSink(final ListenableFuture<T> future,
         final Sink<? super ListenableFuture<T>> sink, Executor executor) {
      future.addListener(new Runnable() {
         @Override public void run() {
            sink.accept(future);
         }
      }, executor);
   }
   
   public static <T> void addCallback(ListenableFuture<T> future,
         FutureCallback<? super T> callback) {
      addCallback(future, callback, SAME_THREAD_EXECUTOR);
   }
   
   public static <T> void addCallback(final ListenableFuture<T> future,
         final FutureCallback<? super T> callback, Executor executor) {
      future.addListener(new Runnable() {
         @Override public void run() {
            if (future.isCancelled()) {
               callback.onCancel();
               return;
            }
            T result;
            try {
               result = future.get();
            } catch (ExecutionException e) {
               callback.onFailure(e.getCause());
               return;
            } catch (InterruptedException e) {
               // this shouldn't be possible because future should be complete, no wait needed.
               // but, just in case, don't want to leave listener waiting forever...
               AssertionError err = new AssertionError();
               err.initCause(e);
               callback.onFailure(err);
               return;
            } catch (Throwable t) {
               callback.onFailure(t);
               return;
            }
            callback.onSuccess(result);
         }
      }, executor);
   }

   public static <T, U> ListenableFuture<U> transform(final ListenableFuture<T> future,
         final Function<? super T, ? extends U> function) {
      final SimpleListenableFuture<U> result = new SimpleListenableFuture<U>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            // when future is cancelled, listener below marks this cancelled, but
            // listener could be executed async. since we need this to be cancelled
            // before we return from this method, cancel now and then listener
            // will be a no-op
            if (future.cancel(mayInterrupt)) {
               setCancelled();
               return true;
            }
            return false;
         }
      };
      addCallback(future, new FutureCallback<T>() {
         @Override
         public void onSuccess(T t) {
            try {
               result.setValue(function.apply(t));
            } catch (Throwable th) {
               result.setFailure(th);
            }
         }
   
         @Override
         public void onFailure(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void onCancel() {
            result.setCancelled();
         }
      });
      return result;
   }
   
   public static <T> ListenableFuture<List<T>> join(ListenableFuture<? extends T>... futures) {
      return join(Arrays.asList(futures));
   }

   public static <T> ListenableFuture<List<T>> join(
         final Iterable<ListenableFuture<? extends T>> futures) {
      final SimpleListenableFuture<List<T>> result = new SimpleListenableFuture<List<T>>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            boolean ret = false;
            for (ListenableFuture<?> future : futures) {
               if (future.cancel(mayInterrupt)) {
                  ret = true;
               }
            }
            if (ret) {
               // when an element future is cancelled, listener below marks this cancelled, but
               // listener could be executed async. since we need this to be cancelled before we
               // return from this method, cancel now and then listener will be a no-op
               setCancelled();
            }
            return ret;
         }
      };
      List<ListenableFuture<? extends T>> futureList = 
            new ArrayList<ListenableFuture<? extends T>>();
      int len = futureList.size();
      final List<T> resolved = new ArrayList<T>(len);
      final List<AtomicBoolean> setList = new ArrayList<AtomicBoolean>();
      for (@SuppressWarnings("unused") Object unused : futures) {
         // prefill with nulls
         resolved.add(null);
         setList.add(new AtomicBoolean());
      }
      final AtomicInteger remaining = new AtomicInteger(len);
      for (int i = 0; i < len; i++) {
         final int index = i;
         ListenableFuture<? extends T> future = futureList.get(i);
         addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(T t) {
               // defend against callback being incorrectly called more than once
               if (setList.get(index).compareAndSet(false, true)) {
                  resolved.set(index, t);
                  if (remaining.decrementAndGet() == 0) {
                     // all outstanding futures have completed
                     result.setValue(resolved);
                  }
               }
            }

            @Override
            public void onFailure(Throwable t) {
               result.setFailure(t);
            }
            
            @Override
            public void onCancel() {
               result.setCancelled();
            }
         });
      }
      return result;
   }
}
