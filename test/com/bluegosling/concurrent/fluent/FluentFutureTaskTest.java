package com.bluegosling.concurrent.fluent;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.FluentFutureTask;

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
