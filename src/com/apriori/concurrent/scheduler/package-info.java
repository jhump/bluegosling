/**
 * A {@link com.apriori.concurrent.scheduler.ScheduledTaskManager ScheduledTaskManager} API, which
 * provides lots of scheduled task and job management functionality. The API is based on the
 * standard {@code ScheduledExecutorService} but with significant improvements and increased
 * functionality. The API here allows introspection on the status and history of scheduled tasks and
 * allows tasks to be configured with custom exception handling strategies. It also provides more
 * control over scheduling of subsequent instances for recurring tasks, the ability to pause/resume
 * recurring tasks, and cancel individual instances of recurring tasks.
 * 
 * <h3>Key Interfaces</h3>
 * <dl>
 *  <dt>{@link TaskDefinition}</dt>
 *    <dd>The definition of a scheduled task. This defines when a task runs, if it is repeated and
 *    how often, what actual code is executed when the task is run, etc.</dd>
 *  <dt>{@link ScheduledTaskDefinition}</dt>
 *    <dd>A {@link TaskDefinition} that has been scheduled for execution with a
 *    {@link ScheduledTaskManager}. This provides additional API for inspecting the status of task
 *    invocations and controlling the task, like pausing/suspending executions and canceling the
 *    task.</dd>
 *  <dt>{@link ScheduledTask}</dt>
 *    <dd>A single invocation of a {@link ScheduledTaskDefinition}. If the task is defined as a
 *    repeating task, there will be multiple such invocations of this task over time. This interface
 *    is also a {@link ScheduledFuture} and is returned when submitting scheduled tasks to the
 *    executor service.</dd>
 *  <dt>{@link RepeatingScheduledTask}</dt>
 *    <dd>Represents all invocations of a repeating {@link ScheduledTaskDefinition}. This is for
 *    API and behavioral compatibility with the {@link ScheduledFuture}s returned by the standard
 *    {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} and
 *    {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
 *    methods.</dd>
 * </dl>
 * 
 * <p>Client code uses a {@link TaskDefinition.Builder} to construct a task and define all of the
 * parameters for its execution. It is then {@linkplain #schedule(TaskDefinition) scheduled} with 
 * the {@link ScheduledTaskManager} for execution.
 */
package com.apriori.concurrent.scheduler;

