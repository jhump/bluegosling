package com.apriori.concurrent;

import com.apriori.collections.Iterables;
import com.apriori.tuples.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * An executor that allows for propagation of context from submitting threads to worker threads.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class ContextPropagatingExecutor extends WrappingExecutor {
   
   private final Collection<ContextPropagator<?>> propagators;
   
   /**
    * Constructs a new context propagating executor.
    *
    * @param delegate the underlying executor, for executing tasks
    * @param propagators objects that manage and propagate context for each task submission
    */
   public ContextPropagatingExecutor(Executor delegate,
         Iterable<? extends ContextPropagator<?>> propagators) {
      super(delegate);
      this.propagators = Iterables.snapshot(propagators);
   }
   
   @Override
   protected final Runnable wrap(Runnable r) {
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
            r.run();
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
}
