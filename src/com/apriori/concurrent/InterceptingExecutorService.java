package com.apriori.concurrent;

import com.apriori.collections.Iterables;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

//TODO: doc
//TODO: tests
public class InterceptingExecutorService extends WrappingExecutorService {

   public interface Interceptor {
      <T> T intercept(ExecutorService delegate, Callable<T> task) throws Exception;
   }
   
   private final Iterable<Interceptor> interceptors;
   
   public InterceptingExecutorService(ExecutorService delegate,
         Iterable<Interceptor> interceptors) {
      super(delegate);
      this.interceptors = Iterables.snapshot(Iterables.reversed(interceptors));
   }
   
   @Override
   protected <T> Callable<T> wrap(Callable<T> c) {
      return () -> {
         Iterator<Interceptor> i = interceptors.iterator();
         Callable<T> wrapper = c;
         while (i.hasNext()) {
            final Callable<T> wrapped = wrapper;
            wrapper = () -> i.next().intercept(this, wrapped);
         }
         return wrapper.call();
      };
   }
}
