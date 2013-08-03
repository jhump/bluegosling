package com.apriori.concurrent;

import java.util.concurrent.CancellationException;

// TODO: javadoc
public class SimpleFutureVisitor<T> implements FutureVisitor<T> {

   @Override
   public void successful(T result) {
   }

   @Override
   public void failed(Throwable failure) {
   }

   @Override
   public void cancelled() {
      failed(new CancellationException());
   }

}
