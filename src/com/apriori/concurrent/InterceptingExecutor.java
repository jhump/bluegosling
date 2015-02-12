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
   protected Runnable wrap(Runnable r) {
      return () -> {
         Iterator<Interceptor> i = interceptors.iterator();
         Runnable wrapper = r;
         while (i.hasNext()) {
            final Runnable wrapped = wrapper;
            wrapper = () -> i.next().intercept(this, wrapped);
         }
         wrapper.run();
      };
   }
}
