package com.apriori.concurrent;

import com.apriori.choice.Either;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executor;

//TODO: doc
//TODO: tests
public class InterceptingExecutor extends WrappingExecutor {
   
   public interface Interceptor {
      void intercept(Executor delegate, Runnable task);
      
      static Builder newList() {
         return new Builder();
      }
      
      class Builder {
         private final List<Either<Interceptor, Decorator>> list = new ArrayList<>();
         
         Builder() {
         }
         
         public Builder add(Interceptor i) {
            list.add(Either.withFirst(i));
            return this;
         }

         public Builder add(Decorator d) {
            list.add(Either.withSecond(d));
            return this;
         }
         
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
   
   public interface Decorator {
      @SuppressWarnings("unused") // args will be used by implementing classes
      default void beforeStart(Executor delegate, Runnable task) {
      }
      
      @SuppressWarnings("unused") // args will be used by implementing classes
      default void afterFinish(Executor delegate, Runnable task) {
      }
      
      @SuppressWarnings("unused") // args will be used by implementing classes
      default void afterFailed(Executor delegate, Runnable task, Throwable cause) {
      }
      
      static Interceptor asInterceptor(List<Decorator> decorators) {
         return (e, r) -> {
            ListIterator<Decorator> iter = decorators.listIterator(decorators.size());
            try {
               for (Decorator d : decorators) {
                  d.beforeStart(e, r);
               }
               r.run();
               // apply the finish half of decorators in reverse order
               while (iter.hasPrevious()) {
                  Decorator d = iter.previous();
                  d.afterFinish(e, r);
               }
            } catch (Throwable th) {
               // in case exception was thrown by a decorator, just proceed with the remaining
               // decorators, but as a failed task instead of a successful one
               while (iter.hasPrevious()) {
                  Decorator d = iter.previous();
                  try {
                     d.afterFailed(e, r, th);
                  } catch (Throwable th2) {
                     th.addSuppressed(th2);
                  }
               }
               if (th instanceof RuntimeException) {
                  throw (RuntimeException) th;
               } else if (th instanceof Error) {
                  throw (Error) th;
               } else {
                  throw new RuntimeException(th);
               }
            }
         };
      }
   }

   private final Deque<Interceptor> interceptors;
   
   public InterceptingExecutor(Executor delegate, Iterable<Interceptor> interceptors) {
      super(delegate);
      if (interceptors instanceof Collection) {
         this.interceptors = new ArrayDeque<>((Collection<Interceptor>) interceptors);
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
            wrapper = () -> interceptor.intercept(this, wrapped);
         }
         wrapper.run();
      };
   }
}
