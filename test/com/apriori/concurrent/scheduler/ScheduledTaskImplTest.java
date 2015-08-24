package com.apriori.concurrent.scheduler;

import com.apriori.concurrent.ListenableScheduledFutureTaskTest;
import com.apriori.util.Clock;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test cases for {@link ScheduledTaskDefinitionImpl.ScheduledTaskImpl}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: more test cases, specific to ScheduledTask API
public class ScheduledTaskImplTest extends ListenableScheduledFutureTaskTest {

   private ScheduledTaskManager taskManager;
   
   @Override
   public void setUp() {
      super.setUp();
      taskManager = new ScheduledTaskManager(1);
   }
   
   private <V> ScheduledTaskDefinitionImpl<V>.ScheduledTaskImpl create(TaskDefinition<V> taskDef,
         long scheduledStartNanoTime, final Clock clock) {
      ScheduledTaskDefinitionImpl<V> task =
            new ScheduledTaskDefinitionImpl<V>(taskDef, taskManager) {
         @Override
         ScheduledTaskImpl createTask(Callable<V> callable, AtomicLong taskStart,
               AtomicLong taskEnd, long startNanoTime) {
            return new ScheduledTaskImpl(callable, taskEnd, taskEnd, startNanoTime) {
               @Override protected long now() {
                  return clock.nanoTime();
               }
            };
         }
      };
      return task.scheduleTask(scheduledStartNanoTime);
   }
   
   @Override
   protected ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl makeFuture(
         long scheduledStartNanoTime, Clock clock) {
      return create(TaskDefinition.forCallable(underlyingTask()).build(),
            scheduledStartNanoTime, clock);
   }

   @Override
   protected ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl makeFuture(
         long scheduledStartNanoTime) {
      TaskDefinition<String> taskDef = TaskDefinition.forCallable(underlyingTask()).build();
      ScheduledTaskDefinitionImpl<String> task =
            new ScheduledTaskDefinitionImpl<String>(taskDef, taskManager);
      return task.scheduleTask(scheduledStartNanoTime);
   }

   @Override
   protected ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl future() {
      return (ScheduledTaskDefinitionImpl<String>.ScheduledTaskImpl) future;
   }
}
