package com.apriori.concurrent;

import java.util.concurrent.CancellationException;

//TODO: javadoc
public interface FutureCallback<T> {
   void onSuccess(T t);
   void onFailure(Throwable t);
   void onCancel();

   abstract class SimpleCallback<T> implements FutureCallback<T> {
      @Override public void onCancel() {
         onFailure(new CancellationException());
      }
   }
}