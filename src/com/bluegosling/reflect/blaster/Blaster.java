package com.bluegosling.reflect.blaster;

import com.bluegosling.reflect.Types;
import com.bluegosling.reflect.blaster.BlasterException.BlasterExceptionCause;
import com.google.common.reflect.Reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Forwards method calls to multiple instances. Effectively, this broadcasts methods invoked on a
 * "Blaster" (proxy) out to multiple objects that implement the same interface. In other words, it
 * "blasts" method calls out to multiple objects.
 * 
 * <p>If a method is invoked that returns an interface, then the proxy returns another blaster, that
 * sends method invocations to all such interfaces returned by underlying objects. If a method is
 * invoked that does not return an interface, then the proxy returns the last successful call to
 * one of the target objects or a default value for the return type if none of the method
 * invocations were successful.
 * 
 * <p>Exceptions thrown by the target objects are suppressed, but available by inspecting the full
 * set of results after a method call. Similarly, for methods that do not return an interface, if
 * you want to know all results (instead of only the last successful one), you can inspect the full
 * set of results.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the interface type whose methods are "blasted"
 */
//TODO: tests
//TODO: finish javadoc; describe how failures are propagated
public class Blaster<T> {
   
   /**
    * The result of blasting a method invocation out to a single target object. This result can
    * either be a returned value or a thrown exception, depending on what happened during the method
    * invocation.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the result
    */
   public static class Result<T> {
      private final T result;
      private final Throwable failure;
      
      private Result(T result) {
         this.result = result;
         this.failure = null;
      }
      
      private Result(Throwable failure) {
         this.result = null;
         this.failure = failure;
      }
      
      static <T> Result<T> create(T t) {
         return new Result<T>(t);
      }
      
      static <T> Result<T> failed(Throwable failure) {
         return new Result<T>(failure);
      }
      
      /**
       * Gets the result value. If the method invocation was not successful then this will throw
       * the exception that was caught when the method was invoked. This method does not throw
       * checked exceptions. If the failure that caused the method invocation was a checked
       * exception then it will be wrapped in a {@link RuntimeException}.
       * 
       * @return the value returned from a method invocation
       * @throws Error if the method invocation threw an {@link Error}
       * @throws RuntimeException if the method invocation threw an exception
       */
      public T get() {
         if (failure != null) {
            throwUnchecked(failure);
         }
         return result;
      }
      
      /**
       * Gets the result value. This is just like {@link #get()} except that it wraps the failures
       * in the checked exception type {@link ExecutionException} (just like {@code Future}s do).
       * 
       * @return the value returned from a method invocation
       * @throws ExecutionException if the method invocation threw an exception
       */
      public T checkedGet() throws ExecutionException {
         if (failure != null) {
            throw new ExecutionException(failure);
         }
         return result;
      }
      
      /**
       * Gets the throwable that caused the method invocation to fail. If the method invocation
       * succeeded then this will return {@code null}.
       * 
       * @return the throwable that caused an invocation to fail or {@code null}
       */
      public Throwable getFailure() {
         return failure;
      }
      
      /**
       * Returns true if the method invocation was successful (e.g. did not throw).
       * 
       * @return true if the method invocation was successful; false otherwise
       */
      public boolean success() {
         return failure == null;
      }
      
      /**
       * Returns true if the method invocation failed (e.g. threw).
       * 
       * @return true if the method invocation failed; false otherwise
       */
      public boolean failed() {
         return failure != null;
      }
   }
   
   /**
    * Holds the results of the latest blaster invocation on this thread.
    */
   static final ThreadLocal<List<Result<?>>> latestBlasted =
         new ThreadLocal<List<Result<?>>>();
   
   /**
    * Constructs a blaster that implements the specified interface.
    * 
    * @param iface the interface for which method invocations are blasted
    * @return a new blaster
    */
   public static <T> Blaster<T> forInterface(Class<T> iface) {
      return new Blaster<T>(iface);
   }
   
   /**
    * Creates a "blasting proxy". When methods are invoked on this proxy, they will be dispatched
    * to all specified instances. This uses default blasting settings and is equivalent to the
    * following:<pre>
    * forInterface(iface).blastTo(instances);
    * </pre>
    * 
    * @param iface the interface for which method invocations are blasted
    * @param instances the instances to which method invocations are blasted
    * @return a new blasting proxy
    * 
    * @see #blastedResults()
    */
   public static <T> T blastTo(Class<T> iface, Collection<? extends T> instances) {
      return forInterface(iface).blastTo(instances);
   }
   
   /**
    * Returns true if the specified object is a blasting proxy.
    * 
    * @param o an object
    * @return true if the specified object is blasting proxy; false otherwise
    */
   public static boolean isBlaster(Object o) {
      return Proxy.isProxyClass(o.getClass())
            && Proxy.getInvocationHandler(o) instanceof BlasterInvocationHandler;
   }
   
   /**
    * Gets the list of values returned from the most recent method invocation on a blasting proxy
    * on this thread. This method is only useful if all blasted method invocations succeeded.
    * Otherwise, this will throw an exception.
    * 
    * <p>The argument is for readability, so code can do the following:<pre>
    * SomeInterface o1, o2, o3;
    * SomeInterface blaster = Blaster.blastTo(SomeInterface.class, o1, o2, o3);
    * // Just calling blaster.getIntegerValue() would only return the last
    * // successful return value, but we want all of them.
    * List&lt;Integer&gt; ints = blasted(blaster.getIntegerValue());
    * </pre>
    * <p>If no invocation has occurred on this thread or the last such invocation was aborted then
    * this will throw an exception.
    * 
    * @param t a dummy argument, for readability using the idiom described above
    * @return the list of values returned from the last blasted method invocation
    * @throws IllegalStateException if no invocation has occurred on this thread or the last such
    *       invocation was aborted
    * @throws Error if any of the blaster's target instances threw an {@link Error} during method
    *       invocation
    * @throws RuntimeException if any of the blaster's target instances threw an exception during
    *       method invocation
    * 
    * @see #blastedResults()
    */
   public static <T> List<T> blasted(T t) {
      List<Result<T>> results = blastedResults(t);
      List<T> ret = new ArrayList<T>(results.size());
      for (Result<T> res : results) {
         ret.add(res.get());
      }
      return Collections.unmodifiableList(ret);
   }
   
   /**
    * Gets the list of method invocation results from the most recent method invocation on a
    * blasting proxy on this thread.
    * 
    * <p>The argument is for readability, so code can do the following:<pre>
    * SomeInterface o1, o2, o3;
    * SomeInterface blaster = Blaster.blastTo(SomeInterface.class, o1, o2, o3);
    * // Just calling blaster.getIntegerValue() would only return the last
    * // successful return value, but we want all of them.
    * List&lt;Result&lt;Integer&gt;&gt; intResults = blastedResults(blaster.getIntegerValue());
    * </pre>
    * <p>If no invocation has occurred on this thread or the last such invocation was aborted then
    * this will throw an exception.
    * 
    * @param t a dummy argument, for readability
    * @return the list of results from the last blasted method invocation
    * @throws IllegalStateException if no invocation has occurred on this thread or the last such
    *       invocation was aborted
    *       
    * @see #blastedResults()         
    */
   @SuppressWarnings({"unchecked", "rawtypes"}) // could be unsafe, but ok if caller is using this idiom properly
   public static <T> List<Result<T>> blastedResults(T t) {
      return (List) blastedResults();
   }
   
   /**
    * Gets the list of method invocation results from the most recent method invocation on a
    * blasting proxy on this thread. If no invocation has occurred on this thread or the last such
    * invocation was aborted then this will throw an exception.
    *
    * @return the list of results from the last blasted method invocation
    * @throws IllegalStateException if no invocation has occurred on this thread or the last such
    *       invocation was aborted
    *       
    * @see #abortOnNullTarget()
    * @see #abortOnAnyException()
    */
   public static List<Result<?>> blastedResults() {
      List<Result<?>> results = latestBlasted.get();
      if (results == null) {
         throw new IllegalStateException();
      }
      return results;
   }

   /**
    * Describes what behavior to take under error situations.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private enum Action {
      /**
       * Indicates that the error situation should be propagated if possible. If not possible, then
       * the situation is ignored, but can be programmatically handled by inspecting operation
       * {@linkplain Result results}.
       */
      PROPAGATE,
      
      /**
       * Indicates that the error situation should result in an exception being thrown. This will
       * not, however, abort the operation. The operation is applied to all targets and then an
       * exception is thrown if any single target failed.
       */
      THROW,
      
      /**
       * Indicates that the error situation should result in the current operation being aborted.
       * Any other targets to which the operation has not yet applied will be skipped. The exception
       * that caused the failure is thrown.
       */
      ABORT
   }
   
   private final Class<T> iface;
   private Action onNullAction = Action.PROPAGATE;
   private Action onExceptionAction = Action.PROPAGATE;
   
   private Blaster(Class<T> iface) {
      this.iface = iface;
   }
   
   /**
    * Blasts method invocations to all of the specified objects.
    * 
    * @param instances the instances to which method invocations are blasted
    * @return a new blasting proxy
    */
   public T blastTo(Collection<? extends T> instances) {
      return Reflection.newProxy(iface,
            new BlasterInvocationHandler(instances, onNullAction, onExceptionAction));
   }
   
   public Blaster<T> propagateOnNullTarget() {
      onNullAction = Action.PROPAGATE;
      return this;
   }
   
   public Blaster<T> throwOnNullTarget() {
      onNullAction = Action.THROW;
      return this;
   }
   
   public Blaster<T> abortOnNullTarget() {
      onNullAction = Action.ABORT;
      return this;
   }
   
   public Blaster<T> propagateOnAnyException() {
      onExceptionAction = Action.PROPAGATE;
      return this;
   }
   
   public Blaster<T> throwOnAnyException() {
      onExceptionAction = Action.THROW;
      return this;
   }

   public Blaster<T> abortOnAnyException() {
      onExceptionAction = Action.ABORT;
      return this;
   }
   
   static void throwUnchecked(Throwable t) {
      if (t instanceof RuntimeException) {
         throw (RuntimeException) t;
      } else if (t instanceof Error) {
         throw (Error) t;
      } else {
         throw new RuntimeException(t);
      }
   }

   /**
    * Dispatches methods to multiple objects and accumulates the results. For methods that return
    * an interface, a new blasting proxy is returned so methods can be chained and continue to be
    * blasted. For methods that return other types, the result of the last successful invocation on
    * a target object is returned. If no invocation was successful (e.g. all threw) then a default
    * value for the return type is used.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class BlasterInvocationHandler implements InvocationHandler {
      private final List<Result<?>> targets;
      private final Action onNullAction;
      private final Action onExceptionAction;
      
      BlasterInvocationHandler(Collection<?> targets, Action onNullAction,
            Action onExceptionAction) {
         this.targets = targets.stream().map(Result::create).collect(Collectors.toList());
         this.onNullAction = onNullAction;
         this.onExceptionAction = onExceptionAction;
      }

      BlasterInvocationHandler(List<Result<?>> targets, Action onNullAction,
            Action onExceptionAction) {
         this.targets = targets;
         this.onNullAction = onNullAction;
         this.onExceptionAction = onExceptionAction;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         // clear last results
         latestBlasted.set(null);
         // blast the method call out to all targets
         List<Result<?>> blastedResults = new ArrayList<Result<?>>(targets.size());
         List<Result<?>> nextBlastTargets = new ArrayList<Result<?>>(targets.size());
         Result<?> last = null;
         List<BlasterExceptionCause> causes = null;
         for (Result<?> target : targets) {
            if (target.get() == null) {
               NullPointerException npe = new NullPointerException();
               switch (onNullAction) {
                  case THROW:
                     if (causes == null) {
                        causes = new ArrayList<BlasterExceptionCause>();
                     }
                     causes.add(new BlasterExceptionCause(npe, null));
                     // intentional fall-through:
                  case PROPAGATE:
                     blastedResults.add(Result.failed(npe));
                     nextBlastTargets.add(Result.create(null));
                     break;
                  case ABORT:
                     throw npe;
               }
            } else if (target.failed()) {
               // propagate prior failures to next set of results
               blastedResults.add(target);
               nextBlastTargets.add(target);
            } else {
               try {
                  Result<?> result = Result.create(method.invoke(target.get(), args));
                  blastedResults.add(result);
                  nextBlastTargets.add(result);
                  // for non-interface return types, we return the last successful result
                  last = result;
               } catch (Throwable t) {
                  Throwable unwrapped = t instanceof InvocationTargetException ? t.getCause() : t;
                  switch (onExceptionAction) {
                     case THROW:
                        if (causes == null) {
                           causes = new ArrayList<BlasterExceptionCause>();
                        }
                        causes.add(new BlasterExceptionCause(unwrapped, target));
                        // intentional fall-through:
                     case PROPAGATE:
                        Result<?> result = Result.failed(unwrapped);
                        blastedResults.add(result);
                        nextBlastTargets.add(result);
                        break;
                     case ABORT:
                        if (t instanceof InvocationTargetException) {
                           // should be able to safely throw this type
                           throw unwrapped;
                        } else {
                           // if it's some other reflection failure, maybe wrap as unchecked
                           throwUnchecked(t);
                        }
                  }
               }
            }
         }
         // set thread-local with results on completion
         latestBlasted.set(blastedResults);
         // throw if error and we're configured to throw
         if (causes != null) {
            throw new BlasterException(causes);
         }
         // otherwise return value
         Class<?> returnType = method.getReturnType();
         if (returnType.isInterface()) {
            return Reflection.newProxy(returnType,
                  new BlasterInvocationHandler(nextBlastTargets, onNullAction, onExceptionAction));
         } else {
            return last == null ? Types.getZeroValue(returnType) : last.get();
         }
      }
   }
}
