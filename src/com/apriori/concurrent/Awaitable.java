package com.apriori.concurrent;

import java.util.concurrent.TimeUnit;

// TODO: javadoc
public interface Awaitable {
   void await() throws InterruptedException;
   boolean await(long limit, TimeUnit unit) throws InterruptedException;
   boolean isDone();
}
