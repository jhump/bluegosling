package com.apriori.concurrent;

//TODO: javadoc
public interface FutureVisitor<T> {
   void successful(T result);
   void failed(Throwable failure);
   void cancelled();
}