package com.apriori.concurrent;

import java.util.concurrent.Executor;

//TODO: doc
//TODO: tests
public abstract class WrappingExecutor implements Executor {

   private final Executor delegate;
   
   protected WrappingExecutor(Executor delegate) {
      this.delegate = delegate;
   }
   
   protected Executor delegate() {
      return delegate;
   }
   
   protected abstract Runnable wrap(Runnable r);

   @Override
   public void execute(Runnable command) {
      delegate.execute(wrap(command));
   }
}
