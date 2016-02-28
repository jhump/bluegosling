package com.bluegosling.concurrent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An object that helps propagate context from one thread to another. The general usage pattern of
 * this interface is as follows:
 * <pre>
 * ContextPropagator&lt;T&gt; propagator;
 * // capture state from one thread
 * T state = propagator.capture();
 * 
 * // then install that state in a different thread
 * T prevVal = propagator.install(state);
 * try {
 *    doSomeWorkInOtherThread();
 * } finally {
 *   propagator.restore(prevVal);
 * }
 * </pre>
 *
 * @param <T> the type of context value that is propagated
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests for static methods
public interface ContextPropagator<T> {
   /**
    * Captures the context from the current thread.
    *
    * @return the context from the current thread
    */
   T capture();
   
   /**
    * Installs the given context in the current thread.
    *
    * @param t a context value
    * @return the previous context value for the current thread
    */
   T install(T t);
   
   /**
    * Restores the given context in the current thread.
    *
    * @param t the original context value for the current thread
    */
   void restore(T t);
   
   /**
    * Creates a {@link ContextPropagator} whose pieces are defined using functional interfaces. This
    * allows lambda expressions to be used to define the three methods of this interface.
    *
    * @param capturer captures context of the calling thread
    * @param installer installs context into the calling thread
    * @param restorer restores a previous context value into the calling thread
    * @return a propagator that uses the given objects to propagate context
    */
   public static <T> ContextPropagator<T> create(Supplier<? extends T> capturer,
         Function<? super T, ? extends T> installer, Consumer<? super T> restorer) {
      return new ContextPropagator<T>() {
         @Override
         public T capture() {
            return capturer.get();
         }

         @Override
         public T install(T t) {
            return installer.apply(t);
         }

         @Override
         public void restore(T t) {
            restorer.accept(t);
         }
      };
   }
   
   /**
    * Creates a {@link ContextPropagator} whose pieces are defined using functional interfaces. This
    * allows lambda expressions to be used to define the three methods of this interface.
    * 
    * <p>This override allows callers to just provide a getter (the {@code capture} step) and a
    * setter (the {@code restore} step). The {@code install} step of the returned propagator will
    * just use both of those.
    *
    * @param capturer captures context of the calling thread
    * @param installer installs a context value into the calling thread
    * @return a propagator that uses the given objects to propagate context
    */
   public static <T> ContextPropagator<T> create(Supplier<? extends T> capturer,
         Consumer<? super T> installer) {
      return create(capturer,
            t -> { T prevVal = capturer.get(); installer.accept(t); return prevVal; },
            installer);
   }
   
   /**
    * Creates a {@link ContextPropagator} whose pieces are defined using functional interfaces. This
    * allows lambda expressions to be used to define the three methods of this interface.
    * 
    * <p>This override allows callers to just provide the {@code capture} piece and the
    * {@code install} piece. The {@code restore} step of the returned propagator will just invoke
    * the {@code install} logic and discard its return value.
    *
    * @param capturer captures context of the calling thread
    * @param installer installs a context value into the calling thread
    * @return a propagator that uses the given objects to propagate context
    */
   public static <T> ContextPropagator<T> create(Supplier<? extends T> capturer,
         Function<? super T, ? extends T> installer) {
      return create(capturer, installer, installer::apply);
   }
   
   /**
    * Creates a {@link ContextPropagator} that propagates the value of the given thread-local
    * variable.
    *
    * @param threadLocal the thread-local variable whose value is propagated
    * @return a propagator that propagates the value of the given thread-local variable
    */
   public static <T> ContextPropagator<T> forThreadLocal(ThreadLocal<T> threadLocal) {
      return create(threadLocal::get, threadLocal::set);
   }

   /**
    * Creates a {@link ContextPropagator} that propagates the value of the given thread-local
    * variable, clearing thread-local state after each task. This differs from
    * {@link #forThreadLocal(ThreadLocal)} in that the {@code restore} step simply
    * {@linkplain ThreadLocal#remove() clears} the value instead of restoring any previous value.
    * Similarly, the {@code install} step of the returned propagator always returns {@code null}.
    *
    * @param threadLocal the thread-local variable whose value is propagated
    * @return a propagator that propagates the value of the given thread-local variable, but clears
    *       it on finish instead of trying to restore a prior value
    */
   public static <T> ContextPropagator<T> forThreadLocalClearAfterTask(ThreadLocal<T> threadLocal) {
      return create(threadLocal::get,
            t -> { threadLocal.set(t); return null; },
            t -> threadLocal.remove());
   }
}
