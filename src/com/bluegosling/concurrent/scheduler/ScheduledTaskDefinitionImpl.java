package com.bluegosling.concurrent.scheduler;

import com.bluegosling.concurrent.fluent.FluentRepeatingFuture;
import com.bluegosling.concurrent.fluent.FluentScheduledFutureTask;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements {@link ScheduledTaskDefinition}. This implementation assumes that the
 * {@link ScheduledTaskManager} that executes the tasks will properly schedule objects for delayed
 * execution when using {@link ScheduledTaskManager#execute(Runnable)} and passing it an object
 * that implements {@link Delayed}.
 * 
 * <p>Scheduling the task for execution is done using {@link #scheduleFirst()} after construction.
 * As tasks are completed, this automatically schedules subsequent tasks.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of result produced by each task
 */
// TODO: tests
class ScheduledTaskDefinitionImpl<V> implements ScheduledTaskDefinition<V> {

   private final TaskDefinition<V> taskDef;
   private final ScheduledTaskManager executor;
   private final long submitTimeMillis = System.currentTimeMillis();
   private final long submitNanoTime = System.nanoTime();
   private final Lock lock = new ReentrantLock();
   private final Set<ScheduledTaskListener<? super V>> listeners;
   private boolean paused;
   private boolean finished;
   private boolean cancelled;
   private int successCount;
   private int failedCount;
   private ScheduledTaskImpl current;
   private final Deque<ScheduledTaskImpl> history = new ArrayDeque<ScheduledTaskImpl>();
   
   /**
    * Creates a new instance.
    * 
    * @param taskDef the definition of the task
    * @param executor the executor used to schedule the tasks
    */
   ScheduledTaskDefinitionImpl(TaskDefinition<V> taskDef, ScheduledTaskManager executor) {
      this.taskDef = taskDef;
      this.listeners = new LinkedHashSet<ScheduledTaskListener<? super V>>(taskDef.listeners());
      this.executor = executor;
   }
   
   // visible only for testing! do not call
   ScheduledTaskImpl scheduleTask(long nanoTime) {
      final AtomicLong taskStart = new AtomicLong();
      final AtomicLong taskEnd = new AtomicLong();
      final AtomicReference<ScheduledTaskImpl> task = new AtomicReference<ScheduledTaskImpl>();
      Callable<V> trackingCallable = new Callable<V>() {
         @SuppressWarnings("synthetic-access") // anonymous class access private fields
         @Override
         public V call() throws Exception {
            try {
               lock.lock();
               try {
                  if (paused && current == null && task.get().isCancelled()) {
                     // Task was paused and cancelled this instance because it hadn't yet
                     // started. Let's honor that intent by not actually do anything.
                     return null;
                  }
                  taskStart.set(System.currentTimeMillis());
               } finally {
                  lock.unlock();
               }
               try {
                  return taskDef.task().call();
               } finally {
                  taskEnd.set(System.currentTimeMillis());
               }
            } catch (Exception | Error e) {
               UncaughtExceptionHandler handler = uncaughtExceptionHandler();
               if (handler != null) {
                  try {
                     handler.uncaughtException(Thread.currentThread(), e);
                  } catch (Throwable th2) {
                     // TODO: log?
                  }
               }
               throw e;
            }
         }
      };
      task.set(current = createTask(trackingCallable, taskStart, taskEnd, nanoTime));
      return current;
   }
   
   ScheduledTaskImpl createTask(Callable<V> callable, AtomicLong taskStart, AtomicLong taskEnd,
         long startNanoTime) {
      return new ScheduledTaskImpl(callable, taskStart, taskEnd, startNanoTime);
   }
   
   ScheduledTask<V> scheduleFirst() {
      lock.lock();
      try {
         scheduleTask(scheduledStartNanoTime());
      } finally {
         lock.unlock();
      }
      executor.execute(current);
      return current;
   }

   @Override
   public TaskImplementation<V> task() {
      return taskDef.task();
   }

   @Override
   public long scheduledStartTimeMillis() {
      return submitTimeMillis + initialDelay(TimeUnit.MILLISECONDS);
   }
   
   @Override
   public long scheduledStartNanoTime() {
      return submitNanoTime + initialDelay(TimeUnit.NANOSECONDS);
   }
   
   @Override
   public long initialDelay(TimeUnit unit) {
      return taskDef.initialDelay(unit);
   }

   @Override
   public boolean isRepeating() {
      return taskDef.isRepeating();
   }

   @Override
   public Rescheduler<? super V> rescheduler() {
      return taskDef.rescheduler();
   }

   @Override
   public boolean isFixedRate() {
      return taskDef.isFixedRate();
   }

   @Override
   public long periodDelay(TimeUnit unit) {
      return taskDef.periodDelay(unit);
   }

   @Override
   public ScheduleNextTaskPolicy<? super V> scheduleNextTaskPolicy() {
      return taskDef.scheduleNextTaskPolicy();
   }

   @Override
   public int maxHistorySize() {
      return taskDef.maxHistorySize();
   }

   @Override
   public Set<ScheduledTaskListener<? super V>> listeners() {
      lock.lock();
      try {
         // return a read-only snapshot of current listeners
         return Collections.unmodifiableSet(
               new LinkedHashSet<ScheduledTaskListener<? super V>>(listeners));
      } finally {
         lock.unlock();
      }
   }

   @Override
   public UncaughtExceptionHandler uncaughtExceptionHandler() {
      return taskDef.uncaughtExceptionHandler();
   }

   @Override
   public long submitTimeMillis() {
      return submitTimeMillis;
   }

   @Override
   public ScheduledTaskManager executor() {
      return executor;
   }

   @Override
   public int executionCount() {
      lock.lock();
      try {
         return successCount + failedCount;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int failureCount() {
      lock.lock();
      try {
         return failedCount;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int successCount() {
      lock.lock();
      try {
         return successCount;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public List<ScheduledTask<V>> history() {
      lock.lock();
      try {
         // read-only snapshot
         return Collections.unmodifiableList(new ArrayList<ScheduledTask<V>>(history));
      } finally {
         lock.unlock();
      }
   }

   @Override
   public ScheduledTask<V> latest() {
      lock.lock();
      try {
         return history.peekFirst();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public ScheduledTask<V> current() {
      lock.lock();
      try {
         return current;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean addListener(ScheduledTaskListener<? super V> listener) {
      lock.lock();
      try {
         return listeners.add(listener);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean removeListener(ScheduledTaskListener<? super V> listener) {
      lock.lock();
      try {
         return listeners.remove(listener);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean isFinished() {
      lock.lock();
      try {
         return finished;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean cancel(boolean interrupt) {
      lock.lock();
      try {
         if (finished) {
            return false;
         }
         finished = cancelled = true;
         paused = false;
         if (current != null) {
            current.cancel(interrupt);
            current = null;
         }
         return true;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean isCancelled() {
      lock.lock();
      try {
         return cancelled;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean pause() {
      lock.lock();
      try {
         if (paused || finished) {
            return false;
         }
         paused = true;
         if (!current.isStarted()) {
            ScheduledTaskImpl toCancel = current;
            current = null;
            toCancel.cancel(false);
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean isPaused() {
      lock.lock();
      try {
         return paused;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean resume() {
      boolean needToExecute = false;
      lock.lock();
      try {
         if (!paused) {
            return false;
         }
         paused = false;
         if (current == null) {
            ScheduledTaskImpl latest = history.peekFirst();
            if (latest == null) {
               scheduleTask(scheduledStartNanoTime());
            } else {
               long newScheduledNanoTime =
                     rescheduler().computeNextStartTime(asRepeatingFuture(),
                           latest.getScheduledNanoTime());
               scheduleTask(newScheduledNanoTime);
            }
            needToExecute = true;
         }
      } finally {
         lock.unlock();
      }
      
      if (needToExecute) {
         executor.execute(current);
      }
      return true;
   }
   
   FluentRepeatingFuture<V> asRepeatingFuture() {
      return new RepeatingScheduledTaskImpl<V>(this);
   }
   
   /**
    * Implementation of {@link ScheduledTask} based on {@link FluentScheduledFutureTask}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class ScheduledTaskImpl extends FluentScheduledFutureTask<V> implements ScheduledTask<V> {
      
      private final long taskScheduledStartMillis;
      private final AtomicLong taskStartMillis;
      private final AtomicLong taskEndMillis;
      
      /**
       * Constructs a new task instance.
       * 
       * @param callable the task to execute
       * @param taskStartMillis the actual start time of the task (in millis since the epoch),
       *       initialized to zero and set when the task actually executes
       * @param taskEndMillis the finish time of the task (in millis since the epoch), initialized
       *       to zero and set when the task completes
       * @param scheduledNanoTime the time in {@link System#nanoTime() nanos} when this task is
       *       supposed to start
       */
      ScheduledTaskImpl(Callable<V> callable, AtomicLong taskStartMillis,
            AtomicLong taskEndMillis, long scheduledNanoTime) {
         super(callable, scheduledNanoTime);
         this.taskStartMillis = taskStartMillis;
         this.taskEndMillis = taskEndMillis;
         this.taskScheduledStartMillis = System.currentTimeMillis()
               + TimeUnit.NANOSECONDS.toMillis(scheduledNanoTime - System.nanoTime());
      }
      
      @SuppressWarnings("synthetic-access") // accesses private fields of enclosing class
      @Override
      protected void done() {
         // schedule the next occurrence if necessary
         boolean reschedule = scheduleNextTaskPolicy().shouldScheduleNext(this);
         long newScheduledNanoTime = reschedule
               ? rescheduler().computeNextStartTime(asRepeatingFuture(),
                     current.getScheduledNanoTime())
               : 0;
         Collection<ScheduledTaskListener<? super V>> listenersToRun;
         lock.lock();
         try {
            if (paused && current == null && isCancelled()) {
               // nothing to do; future completed because it was cancelled by a pause operation
               return;
            }
            if (current != this) {
               throw new IllegalStateException();
            }
            
            listenersToRun = new ArrayList<ScheduledTaskListener<? super V>>(listeners);
            history.addFirst(current);
            if (history.size() > maxHistorySize()) {
               history.removeLast();
            }
            
            if (current.isSuccessful()) {
               successCount++;
            } else {
               failedCount++;
            }
            
            if (finished || !reschedule) {
               finished = true;
               current = null;
            } else if (paused) {
               current = null;
            } else {
               scheduleTask(newScheduledNanoTime);
            }
         } finally {
            lock.unlock();
         }
         for (ScheduledTaskListener<? super V> listener : listenersToRun) {
            try {
               listener.taskCompleted(this);
            } catch (RuntimeException e) {
               // TODO: log?
            }
         }
         if (current != null) {
            executor.execute(current);
         }
         super.done();
      }

      @Override
      public ScheduledTaskDefinition<V> taskDefinition() {
         return ScheduledTaskDefinitionImpl.this;
      }

      @Override
      public boolean isStarted() {
         return taskStartMillis.get() != 0;
      }

      @Override
      public long scheduledStartTimeMillis() {
         return taskScheduledStartMillis;      
      }

      @Override
      public long actualStartTimeMillis() {
         final long start = taskStartMillis.get();
         if (start == 0) {
            throw new IllegalStateException();
         }
         return start;
      }

      @Override
      public long finishTimeMillis() {
         final long end = taskEndMillis.get();
         if (end == 0) {
            throw new IllegalStateException();
         }
         return end;
      }
   }
}
