package com.apriori.concurrent;

import com.apriori.collections.Iterables;

import java.util.Collection;
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
   protected final <T> Callable<T> wrap(Callable<T> c) {
      Iterator<Interceptor> iter = interceptors.iterator();
      Callable<T> wrapper = c;
      while (iter.hasNext()) {
         final Callable<T> wrapped = wrapper;
         Interceptor interceptor = iter.next();
         wrapper = () -> interceptor.intercept(this, wrapped);
      }
      return wrapper;
   }
   
   @Override
   protected final Runnable wrap(Runnable r) {
      return super.wrap(r);
   }
   
   @Override
   protected final <T, C extends Callable<T>> Collection<Callable<T>> wrap(Collection<C> coll) {
      return super.wrap(coll);
   }
}
