package com.apriori.testing;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An object that can verify an implementation of an interface by comparing
 * its behavior to that of a reference implementation. This can be
 * particularly useful, for example, when creating new implementations
 * of standard interfaces like collections ({@code Collection}, {@code List},
 * {@code Map}, etc.). One can test that the basic interface methods are
 * implemented correctly by comparing their behavior against known good
 * implementations (e.g. {@code ArrayList}, {@code HashMap}, etc.).
 * 
 * <p>When writing tests, create an {@code InterfaceVerifier} and configure
 * the way it verifies method invocations as appropriate. Then use it to
 * create an interface proxy. When a method is called on the proxy object,
 * it is dispatched to both the implementation under test and the reference
 * implementation. The proxy will assert that the test implementation
 * behaves correctly, per the configuration for that method.
 * 
 * <p>By default, methods are verified per the following rules:
 * <ul>
 * <li>If the interface method is a <em>mutator</em> method then both
 * objects must <em>match</em> after the method is called. By
 * default, the objects will be compared against one another using
 * {@link Object#equals(Object)}. But you can supply an alternative
 * {@link ObjectVerifier} that can determine if the test object has correctly
 * mutated in the same way as the reference object.
 * <p>By default, mutator methods are assumed to be all methods on the
 * interface that return {@code void}, but this can be overridden by
 * specifying the set of mutator methods (see {@link #clearMutatorMethods()}
 * and {@link #defineMutatorMethods(MethodSignature...)}, and
 * {@link #removeMutatorMethods(MethodSignature...)} for more details).
 * <p>Also, if the two implementations implement {@link Object#hashCode()}
 * using the same scheme, their hash codes can be compared after a
 * mutator method is called to ensure that they match, too (see
 * {@link #setCheckHashCodesAfterMutation(boolean)} for more details).
 * </li>
 * <li>If the interface method has a non-{@code void} return type then the 
 * return values of both the test and reference implementation will be compared
 * to make sure the method behaved correctly. By default, there are a few
 * options for how the invocation handler will perform that check:
 *   <ol>
 *   <li>If the return type specified for the method is an {@code interface}
 *   then the verifier used will make sure that if one implementation returns
 *   {@code null} that they both return {@code null} and then return a proxy for
 *   that interface as well, in order to subsequently test the implementation of
 *   returned interfaces. This verifier is created using
 *   {@link ObjectVerifiers#forTesting(Class)}.</li>
 *   <li>If the return type specified for the method is not an {@code interface}
 *   (i.e. it is a primitive or a class) then the verifier used will check that
 *   the test implementation returns the same values as the reference
 *   implementation. By default the return values are compared using the
 *   {@link ObjectVerifiers#EQUALS} verifier.</li>
 *   </ol>
 * The default verifier used for any given method can be overridden using
 * {@link #setObjectVerifierForMethods(ObjectVerifier, MethodSignature...)}.
 * </li>
 * <li>If the method on the reference implementation throws an exception
 * then the method under test should also throw a similar exception.
 * <p>By default, the proxy will use strict checking of the exception: the test
 * implementation must throw an exception whose class is the same as the
 * exception thrown by the reference implementation. You can configure
 * the proxy to use relaxed exception checking, in which case the test
 * implementation must throw either a sub-class (descendant class) of the
 * exception thrown by the reference implementation or a <em>sibling</em>
 * class. A sibling exception class simply has a common ancestor with the
 * exception thrown by the reference implementation, and the ancestor
 * must be one of the checked exceptions in the interface method's
 * signature (see {@link #setDefaultStrictExceptionHandling(boolean)}
 * and {@link #setStrictExceptionHandling(boolean, MethodSignature...)} for
 * more details).
 * <p>In the event that both implementations refer {@code RuntimeException}s,
 * but not precisely the same concrete exception class, then you can still
 * use the relaxed exception handling, but you must register the ancestor
 * exceptions that can be thrown since {@code RuntimeException}s are not
 * generally included in the method's signature (see
 * {@link #setUncheckedExceptionsForMethod(MethodSignature, Class...)} for
 * more details).
 * </li>
 * </ul>
 * 
 * <p>The behavior that is configured <em>per method</em> is actually defined using
 * {@code MethodSignature}s. In cases where multiple interfaces for a test object are
 * being verified, it is possible more than one interface have methods with the same
 * signature. Such a method's configuration will be the same, regardless of which of
 * the interfaces was used to invoke the method. So the configuration is defined per
 * signature, not per individual interface method. In cases where multiple methods have
 * the same signature but different return types, the most specific return type will
 * be used to determine the default behavior for that method (i.e. whether
 * it uses object verifiers to check for matching return values or if it instead
 * creates proxies for verifying implementation of returned interfaces).
 * 
 * @author jhumphries
 *
 * @param <T>  The interface implemented by the class under test or {@code Object}
 *             if testing a class against more than one implemented interface
 */
public class InterfaceVerifier<T> {
   
   /**
    * Configures how a method is executed and verified during interface
    * verification.
    * 
    * <p>This interface uses a builder pattern with chained methods. All
    * methods return the {@code MethodConfigurator} to simplify usage and
    * enhance readability.
    * 
    * @author jhumphries
    *
    * @param <T>  The interface implemented by the class under test or
    *             {@code Object} if testing a class against more than one
    *             implemented interface
    */
   public interface MethodConfigurator<T> {
      
      /**
       * Indicates that the method is a mutator method. The test and
       * reference implementations will be verified after the method
       * is called using the default mutator verifier.
       * 
       * <p>Note that if this method is called <em>after</em> a call
       * to {@link #mutator(ObjectVerifier)}, the verifier will be
       * changed from the previously specified verifier to the default
       * verifier.
       * 
       * @return  this
       * @see InterfaceVerifier#setDefaultMutatorVerifier(ObjectVerifier)
       */
      MethodConfigurator<T> mutator();
      
      /**
       * Indicates that the method is a mutator method. The test and
       * reference implementations will be verified after the method
       * is called using the specified verifier.
       * 
       * @param v The verifier to use to make sure the mutation in
       *          the test implementation is correct
       * @return  this
       */
      MethodConfigurator<T> mutator(ObjectVerifier<T> v);
      
      /**
       * Indicates that the method is not a mutator method.
       * 
       * @return  this
       */
      MethodConfigurator<T> notMutator();

      /**
       * Specified how to verify the return values after the method is called.
       * This method takes a class token and verifies that it is compatible
       * with the method's return type. This allows a runtime check during
       * configuration <em>before</em> methods are invoked (which could cause
       * {@code ClassCastException}s to be thrown later if the verifier is
       * not compatible).
       * 
       * @param <R>     The return type of the method
       * @param v       The verifier used to verify the method return values
       * @param clazz   A class token for the method's return type
       * @return        this
       * @throws IllegalStateException If this method's return type is {@code void}
       *                         or if the specified class is not compatible with
       *                         the method's return type
       */
      <R> MethodConfigurator<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz);
      
      MethodConfigurator<T> returnVerifier(ObjectVerifier<?> v);
      
      MethodConfigurator<T> exceptionVerifier(ObjectVerifier<Throwable> v);
      
      MethodConfigurator<T> uncheckedExceptions(Class<? extends Throwable>... throwables);
      MethodConfigurator<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables);
      
      MethodConfigurator<T> cloneAndVerifyArguments();
      
      MethodConfigurator<T> cloneArguments();
      MethodConfigurator<T> cloneArgumentsWith(Cloner<?>... cloners);
      MethodConfigurator<T> cloneArgumentsWith(List<Cloner<?>> cloners);
      MethodConfigurator<T> cloneAllArgumentsWith(Cloner<?> cloner);
      MethodConfigurator<T> noCloneArguments();

      MethodConfigurator<T> verifyArguments();
      MethodConfigurator<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers);
      MethodConfigurator<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers);
      MethodConfigurator<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier);
      MethodConfigurator<T> noVerifyArguments();
   }
   
   public interface MethodConfiguration<T> extends MethodConfigurator<T> {

      Set<Method> getMethods();
      MethodSignature getSignature();
      Class<?> getReturnType();
      Set<Class<? extends Throwable>> getCheckedExceptions();
      Set<Class<? extends Throwable>> getAllExceptions();
      
      @Override MethodConfiguration<T> mutator();
      @Override MethodConfiguration<T> mutator(ObjectVerifier<T> v);
      @Override MethodConfiguration<T> notMutator();
      ObjectVerifier<T> getMutatorVerifier();
      boolean isMutator();
      
      @Override <R> MethodConfiguration<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz);
      @Override MethodConfiguration<T> returnVerifier(ObjectVerifier<?> v);
      ObjectVerifier<?> getReturnVerifier();
      
      @Override MethodConfiguration<T> exceptionVerifier(ObjectVerifier<Throwable> v);
      ObjectVerifier<Throwable> getExceptionVerifier();
      
      @Override MethodConfiguration<T> uncheckedExceptions(Class<? extends Throwable>... throwables);
      @Override MethodConfiguration<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables);
      Set<Class<? extends Throwable>> getUncheckedExceptions();

      @Override MethodConfiguration<T> cloneAndVerifyArguments();
      
      @Override MethodConfiguration<T> cloneArguments();
      @Override MethodConfiguration<T> cloneArgumentsWith(Cloner<?>... cloners);
      @Override MethodConfiguration<T> cloneArgumentsWith(List<Cloner<?>> cloners);
      @Override MethodConfiguration<T> cloneAllArgumentsWith(Cloner<?> cloner);
      @Override MethodConfiguration<T> noCloneArguments();
      List<Cloner<?>> getArgumentCloners();
      boolean isCloningArguments();

      @Override MethodConfiguration<T> verifyArguments();
      @Override MethodConfiguration<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers);
      @Override MethodConfiguration<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers);
      @Override MethodConfiguration<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier);
      @Override MethodConfiguration<T> noVerifyArguments();
      List<ObjectVerifier<?>> getArgumentVerifiers();
      boolean isVerifyingArguments();
   }
   
   private class MethodConfiguratorImpl implements MethodConfiguration<T> {
      
      public MethodConfiguratorImpl(Method m) {
         //TODO
      }
      
      public void addMethod(Method m) {
         //TODO
      }

      @Override
      public Set<Method> getMethods() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodSignature getSignature() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public Class<?> getReturnType() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public Set<Class<? extends Throwable>> getCheckedExceptions() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public Set<Class<? extends Throwable>> getAllExceptions() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> mutator() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> mutator(ObjectVerifier<T> v) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> notMutator() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public ObjectVerifier<T> getMutatorVerifier() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public boolean isMutator() {
         // TODO Auto-generated method stub
         return false;
      }

      @Override
      public <R> MethodConfiguration<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> returnVerifier(ObjectVerifier<?> v) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public ObjectVerifier<?> getReturnVerifier() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> exceptionVerifier(ObjectVerifier<Throwable> v) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public ObjectVerifier<Throwable> getExceptionVerifier() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> uncheckedExceptions(Class<? extends Throwable>... throwables) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public Set<Class<? extends Throwable>> getUncheckedExceptions() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> cloneAndVerifyArguments() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> cloneArguments() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> cloneArgumentsWith(Cloner<?>... cloners) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> cloneArgumentsWith(List<Cloner<?>> cloners) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> cloneAllArgumentsWith(Cloner<?> cloner) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> noCloneArguments() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public List<Cloner<?>> getArgumentCloners() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public boolean isCloningArguments() {
         // TODO Auto-generated method stub
         return false;
      }

      @Override
      public MethodConfiguration<T> verifyArguments() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public MethodConfiguration<T> noVerifyArguments() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public List<ObjectVerifier<?>> getArgumentVerifiers() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public boolean isVerifyingArguments() {
         // TODO Auto-generated method stub
         return false;
      }
   }

   private interface MethodInvocationHandler<T> extends InvocationHandler {
      public InterfaceVerifier<T> getVerifier();
   }
   
   private class MethodInvocationHandlerImpl implements MethodInvocationHandler<T> {
      
      @Override
      public InterfaceVerifier<T> getVerifier() {
         return InterfaceVerifier.this;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         // TODO Auto-generated method stub
         return null;
      }
   }
   
   /**
    * The implementation under test.
    */
   private T testImpl;
   
   /**
    * The reference implementation.
    */
   private T referenceImpl;
   
   /**
    * The set of interfaces to test.
    */
   private HashSet<Class<?>> interfaces;

   /**
    * A set of all supported methods.
    */
   private HashSet<Method> methods;
   
   /**
    * A map of all supported method signatures to their configuration.
    * The keys form the set of all supported method signatures.
    */
   private HashMap<MethodSignature, MethodConfiguratorImpl> methodConfig;
   
   /**
    * A structure for querying for method signatures. The keys are method names,
    * and the values are sub-maps. The sub-maps' keys are lists of parameter types
    * and the values are the actual method signatures composed of the two keys --
    * method signature = name (first key) and parameter types (second key).
    */
   private HashMap<String, HashMap<List<Class<?>>, MethodSignature>> methodSigMap;

   /**
    * The default verifier used to make sure mutation operations
    * change the test object in the same way as the reference object.
    * It can be overridden for a particular mutator method in that
    * method's configuration.
    * 
    * @see #setDefaultMutatorVerifier(ObjectVerifier)
    */
   private ObjectVerifier<? super T> defaultMutatorVerifier;

   /**
    * A flag indicating whether or not hash codes should be checked
    * when verifying mutator methods.
    */
   private boolean checkHashCodesAfterMutation;
   
   /**
    * A flag indicating whether or not exceptions should be suppressed.
    * If true, exceptions generated in method invocations will be
    * suppressed and those methods will instead return {@code null}.
    * 
    * @see #setSuppressExceptions(boolean)
    */
   private boolean suppressExceptions;
   
   /**
    * The exception thrown during the most recent method invocation or
    * {@code null} if no exception was thrown.
    * 
    * @see #getLastException()
    */
   private Throwable lastException;

   /**
    * Sets the default verifier used to verify the implementations of mutator
    * methods. Individual mutator methods can override this default verifier.
    * 
    * @param v the new default verifier
    * @throws NullPointerException if the specified verifier is {@code null}
    */
   public void setDefaultMutatorVerifier(ObjectVerifier<? super T> v) {
      if (v == null) {
         throw new NullPointerException("verifier");
      }
      defaultMutatorVerifier = v;
   }
 
   /**
    * Returns the default verifier used to verify the implementations of mutator
    * methods.
    * 
    * @return the default verifier for mutations
    */
   public ObjectVerifier<? super T> getDefaultMutatorVerifier() {
      return defaultMutatorVerifier;
   }

   public void setDefaultExceptionVerifier(ObjectVerifier<T> v) {
      //TODO
   }
   public ObjectVerifier<T> getDefaultExceptionVerifier() {
      //TODO
      return null;
   }
   
   /**
    * Sets whether hash codes are verified after mutator methods are invoked.
    * 
    * @param check   If true then hash codes will be verified after mutations
    */
   public void setCheckHashCodesAfterMutation(boolean check) {
      checkHashCodesAfterMutation = check;
   }

   /**
    * Returns whether hash codes are verified after mutator methods are invoked.
    * By default this is false.
    * 
    * @return  True if hash codes are verified after mutations
    */
   public boolean isCheckHashCodesAfterMutation() {
      return checkHashCodesAfterMutation;
   }
   
   /**
    * Sets whether exceptions should be suppressed during method invocation.
    * This defaults to true. When true, methods that raise exceptions will
    * instead return {@code null}.
    * 
    * <p>This can be useful when writing large tests so that lengthy
    * {@code try-catch} blocks don't have to be included to describe
    * cases that are expected to raise exceptions.
    * 
    * @param suppress   True if exceptions should be suppressed
    * 
    * @see #getLastException()
    */
   public void setSuppressExceptions(boolean suppress) {
      suppressExceptions = suppress;
   }

   /**
    * Returns true if exceptions will be suppressed during method invocations.
    * 
    * @return  True if exceptions will be suppressed
    */
   public boolean isSuppressExceptions() {
      return suppressExceptions;
   }
   
   /**
    * Returns the exception thrown during the most recent method invocation
    * or {@code null} if the most recent invocation did not throw an
    * exception.
    * 
    * <p>This can be used when exceptions are suppressed to easily access
    * thrown exceptions without having to write verbose {@code try-catch}
    * expressions.
    * 
    * @return  The most recently thrown exception or {@code null}
    */
   public Throwable getLastException() {
      return lastException;
   }

   /**
    * Resets the verifier to the default configuration. All configuration for
    * how this object handles and tests method invocations will be as if the
    * object had just been created.
    */
   public void reset() {
      //TODO
   }
   
   /**
    * Gets the method with the specified name that is supported by this
    * interface verifier. If there are no methods with the specified
    * name, {@code null} is returned.
    * 
    * @param methodName The method name
    * @return  The method with the specified name or {@code null}
    * @throws TooManyMatchesException If more than one method has the
    *          specified name
    */
   public MethodSignature getMethodNamed(String methodName) {
      Set<MethodSignature> matches = getMethodsNamed(methodName);
      int numMatches = matches.size();
      if (numMatches > 1) {
         throw new TooManyMatchesException(methodName, numMatches);
      } else if (numMatches == 0) {
         return null;
      } else {
         return matches.iterator().next();
      }
   }

   /**
    * Gets all methods with the specified name that are supported by
    * this interface verifier. This will return more than one method
    * if the named method is overloaded.
    * 
    * @param methodName The method name
    * @return  The set of supported methods with the specified name
    */
   public Set<MethodSignature> getMethodsNamed(String methodName) {
      HashMap<List<Class<?>>, MethodSignature> matches = methodSigMap.get(methodName);
      HashSet<MethodSignature> results = new HashSet<MethodSignature>();
      if (matches != null) {
         results.addAll(matches.values());
      }
      return Collections.unmodifiableSet(results);
   }

   /**
    * Finds a method whose name matches the specified search string. The
    * string can include wildcard characters like "?" and "*". If there
    * are no methods with matching names, {@code null} is returned.
    * 
    * @param methodNamePattern   The search pattern
    * @return  The method with a name that matches the specified pattern
    *          or {@code null}
    * @throws TooManyMatchesException If more than one method name matches
    *          the specified pattern
    */
   public MethodSignature findMethod(String methodNamePattern) {
      Set<MethodSignature> matches = findMethods(methodNamePattern);
      int numMatches = matches.size();
      if (numMatches > 1) {
         throw new TooManyMatchesException(methodNamePattern, numMatches);
      } else if (numMatches == 0) {
         return null;
      } else {
         return matches.iterator().next();
      }
   }

   /**
    * Finds all methods whose names match the specified search string. The
    * string can include wildcard characters like "?" and "*". This can
    * return multiple matching methods with the same name if a matching
    * method name is overloaded.
    * 
    * @param methodNamePattern   The search pattern
    * @return The set methods with a name that matches the specified
    *          pattern
    */
   public Set<MethodSignature> findMethods(String methodNamePattern) {
      Pattern p = createRegExPattern(methodNamePattern);
      HashSet<MethodSignature> results = new HashSet<MethodSignature>();
      for (String name : methodSigMap.keySet()) {
         if (p.matcher(name).matches()) {
            results.addAll(methodSigMap.get(name).values());
         }
      }
      return Collections.unmodifiableSet(results);
   }

   /**
    * Converts a search string with wildcard characters ("?" and "*") to an
    * equivalent regular expression pattern.
    * 
    * @param search  The search string
    * @return  A compiled pattern that matches the incoming search string
    */
   private static Pattern createRegExPattern(String search) {
      // quote everything in the incoming string *except* wildcards,
      // which get converted to regex equivalents
      StringBuilder regex = new StringBuilder();
      int p1 = 0;
      for (int i = 0; i < search.length(); i++) {
         char c = search.charAt(i);
         if (c == '?' || c == '*') {
            regex.append(Pattern.quote(search.substring(p1, i)));
            regex.append(c == '?' ? "." : ".*");
            p1 = i + 1;
         }
      }
      if (p1 < search.length()) {
         regex.append(Pattern.quote(search.substring(p1)));
      }
      return Pattern.compile(regex.toString());
   }
   
   /**
    * Gets the method with the specified signature. This is similar to constructing
    * a new method signature with the specified name and argument types except that
    * if no such method is supported by this invocation handler then this method
    * returns {@code null}.
    * 
    * @param methodName The method name
    * @param args       The method's parameter types
    * @return  The method signature matching the specified name and parameter types
    *          or {@code null}
    */
   public MethodSignature getMethod(String methodName, Class<?>... args) {
      HashMap<List<Class<?>>, MethodSignature> matches = methodSigMap.get(methodName);
      if (matches != null) {
         return matches.get(Arrays.asList(args));
      }
      return null;
   }
   
   public MethodCapturer<T> methodCapturer() {
      //TODO
      return null;
   }
   
   public Set<Class<?>> getInterfaces() {
      return Collections.unmodifiableSet(interfaces);
   }

   /**
    * Returns all methods supported by this invocation handler.
    * 
    * @return an array with all supported methods
    */
   public Set<Method> allMethods() {
      return Collections.unmodifiableSet(methods);
   }

   /**
    * Returns all method signatures supported by this invocation handler.
    * 
    * @return an array with all supported method signatures
    */
   public Set<MethodSignature> allMethodSignaures() {
      return methodConfig.keySet();
   }

   /**
    * Verifies that all methods specified are valid.
    * 
    * @param sigs The methods to verify
    * @throws IllegalArgumentException If any of the specified methods is not
    *          supported by this handler (i.e. not a method on any of the
    *          handler's interfaces)
    * @throws NullPointerException If any of the specified methods are {@code null}
    */
   private void verifyMethods(MethodSignature... sigs) {
      for (MethodSignature m : sigs) {
         if (m == null) {
            throw new NullPointerException("method signature");
         } else if (!methodConfig.containsKey(m)) {
            throw new IllegalArgumentException(m.toString() + " not supported by this handler");
         }
      }
   }
   
   public MethodConfiguration<T> configureMethod(MethodSignature sig) {
      verifyMethods(sig);
      return methodConfig.get(sig);
   }

   public MethodConfigurator<T> configureMethod(MethodSignature... sigs) {
      verifyMethods(sigs);
      //TODO - configurator that multiplexes method calls to multiple configurators
      return null;
   }
   
   /**
    * Creates a proxy object. Methods on the proxy are handled by an invocation
    * handler that dispatches to the test and reference implementations for
    * verifying the functionality of the test implementation.
    * 
    * <p>The current object's class loader ({@code this.getClass().getClassLoader()})
    * is used to define the proxy.
    * 
    * @return  The proxy object
    * @throws IllegalArgumentException If any of the interfaces to be implemented
    *                         by the proxy are not visible by name through the
    *                         current object's class loader
    */
   public T createProxy() {
      return createProxy(this.getClass().getClassLoader());
   }

   /**
    * Creates a proxy object. Methods on the proxy are handled by an invocation
    * handler that dispatches to the test and reference implementations for
    * verifying the functionality of the test implementation.
    * 
    * @param classLoader   The class loader used to define the proxy
    * @return  The proxy object
    * @throws IllegalArgumentException If any of the interfaces to be implemented
    *                         by the proxy are not visible by name through the
    *                         specified class loader
    */
   public T createProxy(ClassLoader classLoader) {
//      @SuppressWarnings("unchecked")
//      T ret = (T) Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[0]), this);
//      return ret;
      //TODO
      return null;
   }

   /**
    * Returns the {@code InterfaceVerifier} for configuring the specified proxy object.
    * This is particularly useful for configuring the verification of returned
    * interfaces.
    * 
    * @param <T>     The interface that the proxy implements
    * @param proxy   The proxy
    * @return  The invocation handler for the proxy
    * @throws IllegalArgumentException If the specified object is not actually
    *                            a proxy or if it is a proxy that was not created
    *                            using {@link #createProxy(ClassLoader)}
    * @throws NullPointerException If the specified proxy is {@code null}
    * 
    * @see ObjectVerifiers#forTesting(Class)
    */
   public static <T> InterfaceVerifier<T> verifierFor(T proxy) {
      try {
         @SuppressWarnings("unchecked")
         MethodInvocationHandler<T> handler = 
               (MethodInvocationHandler<T>) Proxy.getInvocationHandler(proxy);
         return handler.getVerifier();
      } catch (ClassCastException e) {
         throw new IllegalArgumentException("Proxy not created by an InterfaceVerifier", e);
      }
   }

}
