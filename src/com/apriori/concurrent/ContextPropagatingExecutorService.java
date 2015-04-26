package com.apriori.concurrent;

import com.apriori.collections.Iterables;
import com.apriori.tuples.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

//TODO: doc
//TODO: tests
public class ContextPropagatingExecutorService extends WrappingExecutorService {
   
   private final Collection<ContextPropagator<?>> propagators;
   
   public ContextPropagatingExecutorService(ExecutorService delegate,
         Iterable<ContextPropagator<?>> propagators) {
      super(delegate);
      this.propagators = Iterables.snapshot(propagators);
   }
   
   @Override
   protected final <T> Callable<T> wrap(Callable<T> c) {
      // capture context in calling/submitting thread
      List<Pair<ContextPropagator<Object>, Object>> captured =
            new ArrayList<>(propagators.size());
      for (ContextPropagator<?> p : propagators) {
         @SuppressWarnings("unchecked")
         ContextPropagator<Object> pObj = (ContextPropagator<Object>) p;
         captured.add(Pair.create(pObj, pObj.capture()));
      }
      
      return () -> {
         // then install in worker thread
         List<Pair<ContextPropagator<Object>, Object>> installed = new ArrayList<>(captured.size());
         try {
            for (Pair<ContextPropagator<Object>, Object> p : captured) {
               installed.add(Pair.create(p.getFirst(), p.getFirst().install(p.getSecond())));
            }
            // run the task
            return c.call();
         } finally {
            // and reset on completion
            for (Pair<ContextPropagator<Object>, Object> p : installed) {
               try {
                  p.getFirst().restore(p.getSecond());
               } catch (Exception e) {
                  // TODO: log?
               }
            }
         }
      };
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
