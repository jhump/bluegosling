package com.apriori.concurrent;

/**
 * Test cases for {@link ScheduledTaskDefinitionImpl.ScheduledTaskImpl}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ScheduledTaskImplTest extends ListenableScheduledFutureTaskTest {

   private ScheduledTaskManager taskManager;
   
   @Override
   public void setUp() {
      super.setUp();
      taskManager = new ScheduledTaskManager(1);
   }
   
   @Override
   protected ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl makeFuture(
         long scheduledStartNanoTime) {
      // TODO
      return null;
   }

   @Override
   protected ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl future() {
      return (ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl) future;
   }
}
