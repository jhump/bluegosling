package com.bluegosling.concurrent.fluent;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An abstract future that combines the results from other futures. General use entails calling
 * code to {@link #mark()} this future as each constituent future completes. Subclasses are
 * responsible for collecting these results and then implementing {@link #computeValue()} to
 * actually combine the results into a single value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the combined result
 */
public abstract class CombiningFluentFuture<T> extends AbstractFluentFuture<T> {
   private final Collection<FluentFuture<?>> components;
   private final AtomicInteger remaining;

   /**
    * Constructs a new combining future. The specified list should be immutable. Its size is
    * used to determine how many components must be {@linkplain #mark() marked} before a result
    * can be ready. If this combined future is cancelled, this class attempts to cancel all of
    * the underlying components as well.
    * 
    * @param components the list of component futures
    */
   protected CombiningFluentFuture(Collection<FluentFuture<?>> components) {
      this.components = components;
      remaining = new AtomicInteger(components.size());
      FutureVisitor<Object> visitor = new CombiningVisitor(this);
      for (FluentFuture<?> future : components) {
         future.visitWhenDone(visitor);
      }
   }
   
   /**
    * Computes the combined value of this future once all components have completed.
    * 
    * @return the combined value
    */
   protected abstract T computeValue();

   /**
    * Marks a single result as complete. Once all components have completed, this will set the
    * value to the {@linkplain #computeValue() combined result}. 
    */
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
         for (FluentFuture<?> future : components) {
            future.cancel(mayInterrupt);
         }
         return true;
      }
      return false;
   }
   
   /**
    * A visitor used in conjunction with {@link CombiningFluentFuture} to produce future values that
    * are the results of combining other component futures. The general use pattern is that a
    * visitor is created for each component future and added to that component as a listener. The
    * future is {@linkplain CombiningFluentFuture#mark() marked} as each component future is
    * visited.
    * 
    * <p>If a component future is cancelled or fails, this visitor will mark the combined result as
    * cancelled or failed also.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the component value
    */
   static class CombiningVisitor implements FutureVisitor<Object> {
      private final CombiningFluentFuture<?> result;

      /**
       * Creates a new visitor. This visitor is added as a listener to the component future.
       * 
       * @param component the component used as input to computing the combined result, fulfilled
       *       when the component future successfully completes
       * @param result the combined result, which is marked on successful completion of a component
       *       or set as cancelled or failed on cancelled or failed completion of a component
       */
      CombiningVisitor(CombiningFluentFuture<?> result) {
         this.result = result;
      }
      
      @Override
      public void successful(Object o) {
         result.mark();
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