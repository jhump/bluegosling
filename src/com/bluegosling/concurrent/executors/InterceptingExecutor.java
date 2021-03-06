package com.bluegosling.concurrent.executors;

import com.bluegosling.choice.Either;
import com.bluegosling.function.TriConsumer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An executor that provides an interceptor pattern, to apply cross-cutting concerns in the course
 * of executing tasks.
 * 
 * <p>Two different styles of interceptor are supported:
 * <ul>
 * <li>{@link Interceptor}: Can intercept all invocations of tasks and even choose to short-circuit
 *    their execution.</li>
 * <li>{@link Decorator}: Can decorate invocations with actions that are performed before and after
 *    the execution of a task. Short-circuiting actual execution is not directly supported.
 *    Execution of multiple decorators can be done in a single stack-frame, so their use can lead
 *    to friendlier stack traces than the use of interceptors.</li>
 * </ul>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public class InterceptingExecutor extends WrappingExecutor {
   /**
    * An interceptor for tasks. Interceptor implementations can perform actions before and after
    * running the task. Interceptors are expected to invoke {@code task.run()}, unless they are
    * choosing to block execution of the given task.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Interceptor {
      /**
       * Intercepts the given task. Implementations should call {@code task.run()} to proceed to
       * the next interceptor in the chain and run the task.
       *
       * @param delegate the underlying executor that is executing the task
       * @param task the task to run, which may be a wrapper that invokes subsequent interceptors
       */
      void intercept(Executor delegate, Runnable task);
      
      /**
       * Returns a new builder, for building a list of interceptors from both interceptor and
       * decorator instances.
       *
       * @return a new list builder
       */
      static ListBuilder newList() {
         return new ListBuilder();
      }
      
      /**
       * Builds a list of interceptors. In addition to {@link Interceptor} instances, this allows
       * the use of {@link Decorator}. Multiple adjacent {@link Decorator}s will be collapsed into
       * a single interceptor, to reduce the number of frames in the stack trace of an intercepted
       * task.
       * 
       * <p>Interceptors and decorators for a given task are invoked in the order in which they are
       * added.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      class ListBuilder {
         private final List<Either<Interceptor, Decorator>> list = new ArrayList<>();
         
         ListBuilder() {
         }
         
         /**
          * Adds an interceptor to the list.
          *
          * @param i the interceptor
          * @return {@code this}, for method chaining
          */
         public ListBuilder add(Interceptor i) {
            list.add(Either.withFirst(i));
            return this;
         }

         /**
          * Adds a decorator to the list.
          *
          * @param d the decorator
          * @return {@code this}, for method chaining
          */
         public ListBuilder add(Decorator d) {
            list.add(Either.withSecond(d));
            return this;
         }
         
         /**
          * Builds a list of interceptors, comprised of all interceptors and decorators added so
          * far.
          *
          * @return a list of interceptors
          */
         public List<Interceptor> build() {
            // we can tell exactly how big the list will be, so figure that out
            int listLen = list.size();
            int resultSize = 0;
            boolean lastWasDecorator = false;
            for (int i = 0; i < listLen; i++) {
               if (list.get(i).hasFirst()) {
                  resultSize++;
                  lastWasDecorator = false;
               } else if (!lastWasDecorator) {
                  resultSize++;
                  lastWasDecorator = true;
               }
            }
            // now build the list of interceptors, collapsing runs of decorators
            ArrayList<Interceptor> result = new ArrayList<>(resultSize);
            for (int i = 0; i < listLen; i++) {
               Either<Interceptor, Decorator> entry = list.get(i);
               if (entry.hasFirst()) {
                  result.add(entry.getFirst());
               } else {
                  int j = i + 1;
                  for (; j < listLen && !list.get(j).hasFirst(); j++);
                  int numDecorators = j - i;
                  ArrayList<Decorator> decorators = new ArrayList<>(numDecorators);
                  while (i < j) {
                     decorators.add(list.get(i++).getSecond());
                  }
                  assert decorators.size() == numDecorators;
                  result.add(Decorator.asInterceptor(decorators));
               }
            }
            assert result.size() == resultSize;
            return Collections.unmodifiableList(result);
         }
      }
   }
   
   /**
    * A decorator, that can perform actions before and after each task.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Decorator {
      /**
       * Performs actions before the given task. Implementations should not call {@code task.run()}.
       *
       * @param delegate the underlying executor that is executing the task
       * @param task the task that is running
       */
      @SuppressWarnings("unused") // args will be used by implementing classes
      default void beforeStart(Executor delegate, Runnable task) {
      }
      
      /**
       * Performs actions after the given task has completed successfully.
       *
       * @param delegate the underlying executor that is executing the task
       * @param task the task that ran
       */
      @SuppressWarnings("unused") // args will be used by implementing classes
      default void afterFinish(Executor delegate, Runnable task) {
      }
      
      /**
       * Performs actions after the given task has failed.
       *
       * @param delegate the underlying executor that is executing the task
       * @param task the task that failed
       * @param cause the cause of failure
       */
      @SuppressWarnings("unused") // args will be used by implementing classes
      default void afterFailed(Executor delegate, Runnable task, Throwable cause) {
      }
      
      /**
       * Returns a new builder, for creating a decorator from simple lambda expressions.
       *
       * @return a new builder
       */
      static Builder builder() {
         return new Builder();
      }
      
      /**
       * Builds a decorator, composing up to three functional interfaces. This is useful since
       * {@link Decorator} itself is not a function interface and thus cannot be provided directly
       * by lambda expressions.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      class Builder {
         private BiConsumer<? super Executor, ? super Runnable> beforeStart;
         private BiConsumer<? super Executor, ? super Runnable> afterFinish;
         private TriConsumer<? super Executor, ? super Runnable, ? super Throwable> afterFailed;
         
         Builder() {
         }
         
         /**
          * Defines the action performed before each task is run.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder beforeStart(BiConsumer<? super Executor, ? super Runnable> action) {
            this.beforeStart = action;
            return this;
         }

         /**
          * Defines the action performed before each task is run. The action takes only the task.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder beforeStart(Consumer<? super Runnable> action) {
            this.beforeStart = (e, r) -> action.accept(r);
            return this;
         }

         /**
          * Defines the action performed before each task is run. The action takes no arguments.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder beforeStart(Runnable action) {
            this.beforeStart = (e, r) -> action.run();
            return this;
         }
         
         /**
          * Defines the action performed after each task is run. This action will be run after both
          * successful and failed tasks. If the task completes successfully, {@code null} is passed
          * as the cause of failure.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder after(
               TriConsumer<? super Executor, ? super Runnable, ? super Throwable> action) {
            this.afterFinish = (e, r) -> action.accept(e, r, null);
            this.afterFailed = action;
            return this;
         }

         /**
          * Defines the action performed after each task is run. This action will be run after both
          * successful and failed tasks. The action does not take the cause of failure (when
          * invoked after failed tasks).
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder after(BiConsumer<? super Executor, ? super Runnable> action) {
            this.afterFinish = action;
            this.afterFailed = (e, r, t) -> action.accept(e, r);
            return this;
         }

         /**
          * Defines the action performed after each task is run. This action will be run after both
          * successful and failed tasks. The action takes only the task.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder after(Consumer<? super Runnable> action) {
            this.afterFinish = (e, r) -> action.accept(r);
            this.afterFailed = (e, r, t) -> action.accept(r);
            return this;
         }

         /**
          * Defines the action performed after each task is run. This action will be run after both
          * successful and failed tasks. The action takes no arguments.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder after(Runnable action) {
            this.afterFinish = (e, r) -> action.run();
            this.afterFailed = (e, r, t) -> action.run();
            return this;
         }
         
         /**
          * Defines the action performed after each successful task.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterSuccess(BiConsumer<? super Executor, ? super Runnable> action) {
            this.afterFinish = action;
            return this;
         }

         /**
          * Defines the action performed after each successful task. The action takes only the task.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterSuccess(Consumer<? super Runnable> action) {
            this.afterFinish = (e, r) -> action.accept(r);
            return this;
         }

         /**
          * Defines the action performed after each successful task. The action takes no arguments.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterSuccess(Runnable action) {
            this.afterFinish = (e, r) -> action.run();
            return this;
         }
         
         /**
          * Defines the action performed after each failed task.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterFailure(
               TriConsumer<? super Executor, ? super Runnable, ? super Throwable> action) {
            this.afterFailed = action;
            return this;
         }

         /**
          * Defines the action performed after each failed task. The action does not take the cause
          * of failure.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterFailure(BiConsumer<? super Executor, ? super Runnable> action) {
            this.afterFailed = (e, r, t) -> action.accept(e, r);
            return this;
         }

         /**
          * Defines the action performed after each failed task. The action takes only the task.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterFailure(Consumer<? super Runnable> action) {
            this.afterFailed = (e, r, t) -> action.accept(r);
            return this;
         }

         /**
          * Defines the action performed after each failed task. The action takes no arguments.
          *
          * @param action the action
          * @return {@code this}, for method chaining
          */
         public Builder afterFailure(Runnable action) {
            this.afterFailed = (e, r, t) -> action.run();
            return this;
         }
         
         /**
          * Creates a decorator that invokes the configured actions when decorating a task.
          *
          * @return a new decorator
          */
         public Decorator build() {
            final BiConsumer<? super Executor, ? super Runnable> before = beforeStart;
            final BiConsumer<? super Executor, ? super Runnable> after = afterFinish;
            final TriConsumer<? super Executor, ? super Runnable, ? super Throwable> failure = afterFailed;
            return new Decorator() {
               @Override
               public void beforeStart(Executor delegate, Runnable task) {
                  if (before != null) {
                     before.accept(delegate, task);
                  }
               }
               
               @Override
               public void afterFinish(Executor delegate, Runnable task) {
                  if (after != null) {
                     after.accept(delegate, task);
                  }
               }
               
               @Override
               public void afterFailed(Executor delegate, Runnable task, Throwable cause) {
                  if (failure != null) {
                     failure.accept(delegate, task, cause);
                  }
               }
            };
         }
      }
      
      /**
       * Converts a list of decorators into a single interceptor. The interceptor invokes each
       * decorator before the task, in the order they appear in the list. After the task, the
       * decorators are invoked in the opposite order.
       * 
       * <p>Execution of the task is short-circuited if a decorator throws an exception from its
       * {@link #beforeStart} action. In such an event, subsequent decorators are
       * skipped. For every invocation of {@link #beforeStart}, either {@link #afterFinish} or
       * {@link #afterFailed} will be invoked. So, if a decorator throws an exception,
       * short-circuiting task execution, any decorators already invoked will have their
       * {@link #afterFailed} action invoked.
       *
       * @param decorators a list of decorators
       * @return an interceptor that invokes the given decorators
       */
      static Interceptor asInterceptor(List<? extends Decorator> decorators) {
         return (e, r) -> {
            ListIterator<? extends Decorator> iter = decorators.listIterator();
            try {
               while (iter.hasNext()) {
                  Decorator d = iter.next();
                  d.beforeStart(e, r);
               }
               r.run();
               // apply the finish half of decorators in reverse order
               while (iter.hasPrevious()) {
                  Decorator d = iter.previous();
                  d.afterFinish(e, r);
               }
            } catch (RuntimeException | Error ex) {
               // in case exception was thrown by a decorator, just proceed with the remaining
               // decorators, but as a failed task instead of a successful one
               while (iter.hasPrevious()) {
                  Decorator d = iter.previous();
                  try {
                     d.afterFailed(e, r, ex);
                  } catch (Throwable th) {
                     ex.addSuppressed(th);
                  }
               }
               throw ex;
            }
         };
      }
   }

   private final Deque<Interceptor> interceptors;
   
   /**
    * Constructs a new intercepting executor.
    *
    * @param delegate the underlying executor, for executing tasks
    * @param interceptors the interceptors that are applied to each task executed
    */
   public InterceptingExecutor(Executor delegate, Iterable<? extends Interceptor> interceptors) {
      super(delegate);
      if (interceptors instanceof Collection) {
         this.interceptors = new ArrayDeque<>((Collection<? extends Interceptor>) interceptors);
      } else {
         this.interceptors = new ArrayDeque<>();
         for (Interceptor i : interceptors) {
            this.interceptors.add(i);
         }
      }
   }
   
   @Override
   protected final Runnable wrap(Runnable r) {
      return () -> {
         Iterator<Interceptor> iter = interceptors.descendingIterator();
         Runnable wrapper = r;
         while (iter.hasNext()) {
            Interceptor interceptor = iter.next();
            final Runnable wrapped = wrapper;
            wrapper = () -> interceptor.intercept(delegate(), wrapped);
         }
         wrapper.run();
      };
   }
}
