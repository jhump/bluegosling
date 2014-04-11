package com.apriori.concurrent;

/**
 * Test cases for {@link ListenableFutureTask}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ListenableFutureTaskTest extends AbstractListenableRunnableFutureTest {
   @Override
   protected ListenableFuture<String> makeFuture() {
      return new ListenableFutureTask<String>(underlyingTask());
   }
}
