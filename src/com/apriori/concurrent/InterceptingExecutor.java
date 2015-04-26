package com.apriori.concurrent;

import com.apriori.collections.Iterables;

import java.util.Iterator;
import java.util.concurrent.Executor;

//TODO: doc
//TODO: tests
public class InterceptingExecutor extends WrappingExecutor {
   
   public interface Interceptor {
      void intercept(Executor delegate, Runnable task);
   }
   
   private final Iterable<Interceptor> interceptors;
   
   public InterceptingExecutor(Executor delegate, Iterable<Interceptor> interceptors) {
      super(delegate);
      this.interceptors = Iterables.snapshot(Iterables.reversed(interceptors));
   }
   
   @Override
   protected final Runnable wrap(Runnable r) {
      Iterator<Interceptor> iter = interceptors.iterator();
      Runnable wrapper = r;
      while (iter.hasNext()) {
         final Runnable wrapped = wrapper;
         Interceptor interceptor = iter.next();
         wrapper = () -> interceptor.intercept(this, wrapped);
      }
      return wrapper;
   }
}
