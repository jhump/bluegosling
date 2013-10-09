package com.apriori.concurrent.atoms;

import com.apriori.concurrent.Awaitable;
import com.apriori.concurrent.ListenableExecutors;
import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.PipeliningExecutorService;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

//TODO: javadoc
//TODO: tests
public class AsynchronousAtom<T> extends AbstractAtom<T> {

   // TODO: maybe a thread pool w/ limited number of threads
   // TODO: use thread factory for named, daemon threads
   private static final PipeliningExecutorService<AsynchronousAtom<?>> threadPool =
         new PipeliningExecutorService<AsynchronousAtom<?>>(
               ListenableExecutors.makeListenable(Executors.newCachedThreadPool()));

   private volatile T value;
   
   public AsynchronousAtom() {
   }

   public AsynchronousAtom(T value) {
      this.value = value;
   }

   public AsynchronousAtom(T value, Predicate<? super T> validator) {
      super(validator);
      this.value = value;
   }

   @Override
   public T get() {
      return value;
   }
   
   public ListenableFuture<T> set(final T newValue) {
      // TODO: if in transaction?
      validate(newValue);
      return threadPool.submit(this, new Callable<T>() {
         @Override
         public T call() {
            T oldValue = value;
            value = newValue;
            AsynchronousAtom.this.notify(oldValue, newValue);
            return oldValue;
         }
      });
   }

   // TODO: error handling - ignore or block on errors? listeners for errors?
   
   public ListenableFuture<T> send(final Function<? super T, ? extends T> function) {
      // TODO: if in transaction?
      return threadPool.submit(this, new Callable<T>() {
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
}
