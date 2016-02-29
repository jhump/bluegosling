package com.bluegosling.concurrent.futures.fluent;

import com.bluegosling.concurrent.futures.fluent.FluentFuture;
import com.bluegosling.concurrent.futures.fluent.FluentFutureTask;

/**
 * Test cases for {@link FluentFutureTask}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FluentFutureTaskTest extends AbstractRunnableFluentFutureTest {
   @Override
   protected FluentFuture<String> makeFuture() {
      return new FluentFutureTask<String>(underlyingTask());
   }
}
