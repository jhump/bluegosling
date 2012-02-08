package com.apriori.testing;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.apriori.reflect.MethodCapturer;
import com.apriori.reflect.MethodSignature;
import com.apriori.reflect.ProxyUtil;
import com.apriori.util.Cloner;
import com.apriori.util.Cloners;

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
 * configuring methods to be mutator methods (see {@link #configureMethod(MethodSignature)}
 * and {@link MethodConfiguration#mutator()} for more details).
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
 * The default verifier used for any given method can be overridden by configuring
 * the methods (see {@link MethodConfiguration#returnVerifier(ObjectVerifier)} for
 * more details).
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
 * signature (see {@link #setDefaultExceptionVerifier(ObjectVerifier)}
 * and {@link MethodConfiguration#exceptionVerifier(ObjectVerifier)} for
 * more details).
 * <p>In the event that both implementations throw {@code RuntimeException}s,
 * but not precisely the same concrete exception class, then you can still
 * use the relaxed exception handling, but you must define the ancestor
 * exceptions that can be thrown since {@code RuntimeException}s are not
 * generally included in the method's signature (see
 * {@link MethodConfiguration#uncheckedExceptions(Set)} for more details).
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
    * Configures how one or more methods are executed and verified during interface
    * verification.
    * 
    * <p>This interface uses a builder pattern. All methods return the
    * {@code MethodConfigurator} to simplify usage and enhance readability
    * through method chaining.
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
       * @throws NullPointerException If the argument is null
       */
      MethodConfigurator<T> mutator(ObjectVerifier<? super T> v);
      
      /**
       * Indicates that the method is not a mutator method.
       * 
       * @return  this
       */
      MethodConfigurator<T> notMutator();

      /**
       * Specifies how to verify the return values after the method is called.
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
       * @throws IllegalArgumentException If this method's return type is {@code void}
       *                         and the specified verifier is non-null or if the
       *                         specified class is not compatible with the method's
       *                         return type
       * @throws NullPointerException If the specified class token is null
       */
      <R> MethodConfigurator<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz);
      
      /**
       * Specifies how to verify the return values after the method is called.
       * 
       * @param v The verifier used to verify the method return values
       * @return  this
       * @throws IllegalArgumentException If this method's return type is {@code void}
       *                         and the specified verifier is non-null
       */
      MethodConfigurator<T> returnVerifier(ObjectVerifier<?> v);
      
      /**
       * Specifies that exceptions should be verified using strict checking.
       * Exceptions thrown will be verified using {@link ObjectVerifiers#STRICT_EXCEPTIONS}.
       * 
       * @return  this
       */
      MethodConfigurator<T> strictExceptionVerifier();
      
      /**
       * Specifies that exceptions should be verified using relaxed checking.
       * Exceptions thrown will be verified using {@link ObjectVerifiers#relaxedExceptions(Set)},
       * created with the method's declared (checked) exceptions <em>and</em> any
       * unchecked exceptions defined (see {@link #uncheckedExceptions(Set)}.
       * 
       * @return  this
       */
      MethodConfigurator<T> relaxedExceptionVerifier();

      /**
       * Specifies how to verify any exceptions thrown when a method is called.
       * If a {@code null} verifier is specified then the method will use the
       * default exception verifier (see {@link InterfaceVerifier#getDefaultExceptionVerifier()}.
       * 
       * @param v The verifier used to verify exceptions thrown in methods
       * @return  this
       */
      MethodConfigurator<T> exceptionVerifier(ObjectVerifier<Throwable> v);
      
      /**
       * Indicates the set of unchecked exceptions thrown by a method.
       * 
       * @param throwables The class tokens for the unchecked exception types
       * @return           this
       * @throws NullPointerException If any of the specified types are null
       * 
       * @see #uncheckedExceptions(Set)
       */
      //@SafeVararg
      MethodConfigurator<T> uncheckedExceptions(Class<? extends Throwable>... throwables);
      
      /**
       * Indicates the set of unchecked exceptoins thrown by a method. These
       * include {@code RuntimeExceptions} or {@code Errors} that could be
       * thrown by a method that should be considered when verifying the
       * exceptions thrown.
       * 
       * @param throwables The set of class tokens for the unchecked exception types
       * @return           this
       * @throws NullPointerException If the specified set or any of the specified types
       *                   therein are null
       */
      MethodConfigurator<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables);
      
      /**
       * Indicates that arguments should be cloned prior to method execution and then
       * verified. This is appropriate for methods that mutate the incoming arguments.
       * In this case, the arguments are cloned (so that mutations by the test implementation
       * don't change the parameter to be passed to the reference implementation). After
       * the method returns, the two argument objects are verified to make sure both
       * implementations altered the arguments in the same way.
       * 
       * <p>This method will cause default implementations to be used for both cloning
       * and verifying argument values. If you need to specify non-default implementations
       * then use the other methods for explicitly setting cloners and verifiers. Note
       * that you need to define the cloners <em>before</em> defining the verifiers.
       * 
       * @return  this
       * @throws IllegalArgumentException If this method has no arguments that require
       *          cloning (i.e. all arguments are primitives) or has any arguments
       *          for which no appropriate default cloner exists
       *          
       * @see #cloneArguments()
       * @see #verifyArguments()
       */
      MethodConfigurator<T> cloneAndVerifyArguments();
      
      /**
       * Indicates that the arguments should be cloned prior to method execution.
       * A default implementation for cloning will be used to clone each argument.
       * 
       * <p>A single generic cloner will be used for cloning all arguments. The
       * cloner used is {@link Cloners#GENERIC_CLONER}.
       * 
       * <p>If no verifiers are configured for the arguments then the arguments will
       * <em>not</em> be verified -- only cloned prior to execution.
       * 
       * @return  this
       * @throws IllegalArgumentException If this method has no arguments that require
       *                cloning (i.e. no arguments or all arguments are primitives) or
       *                if a default cloner cannot be created for any argument
       */
      MethodConfigurator<T> cloneArguments();
      
      /**
       * Indicates that the arguments should be cloned prior to method execution.
       * The specified cloners will be used. The number of cloners provided must
       * match the number of arguments for the method. A {@code null} cloner means
       * that argument does not need to be cloned. If an argument does not need
       * to be cloned because it is a primitive but a non-null cloner is provided,
       * an exception is thrown.
       * 
       * @param cloners The sequence of argument cloners
       * @return        this
       * @throws IllegalArgumentException If the number of cloners specified is
       *                not the same as the number of method arguments or if a
       *                non-null cloner is specified for an argument whose type
       *                is primitive
       */
      MethodConfigurator<T> cloneArgumentsWith(Cloner<?>... cloners);
      
      /**
       * Indicates that the arguments should be cloned prior to method execution.
       * The specified cloners will be used. The number of cloners provided must
       * match the number of arguments for the method. A {@code null} cloner means
       * that argument does not need to be cloned. If an argument does not need
       * to be cloned because it is a primitive but a non-null cloner is provided,
       * an exception is thrown.
       * 
       * @param cloners The list of argument cloners
       * @return        this
       * @throws IllegalArgumentException If the number of cloners specified is
       *                not the same as the number of method arguments or if a
       *                non-null cloner is specified for an argument whose type
       *                is primitive
       * @throws NullPointerException If the specified list is null
       */
      MethodConfigurator<T> cloneArgumentsWith(List<Cloner<?>> cloners);
      
      /**
       * Indicates that the arguments should be cloned prior to method execution.
       * The specified cloner will be used for all arguments (except arguments that
       * are primitive types).
       * 
       * @param cloner  The argument cloner
       * @return        this
       * @throws IllegalArgumentException If this method has no arguments that require
       *                cloning (i.e. no arguments or all arguments are primitives)
       * @throws NullPointerException If the specified cloner is null
       */
      MethodConfigurator<T> cloneAllArgumentsWith(Cloner<?> cloner);
      
      /**
       * Indicates that arguments should not be cloned prior to method execution,
       * which implies that arguments do not need to be verified after execution.
       * 
       * @return this
       */
      MethodConfigurator<T> noCloneAndVerifyArguments();

      /**
       * Indicates that the arguments should be verified after the method is
       * executed. This is only useful in conjunction with cloning arguments
       * since otherwise the two argument objects, the one passed to the
       * test implementation and the one passed to the reference implementation,
       * will be the same instance.
       * 
       * <p>The default verifier, {@link ObjectVerifiers#EQUALS}, will be used
       * to verify the arguments.
       * 
       * @return  this
       * @throws IllegalArgumentException If this method has no arguments that
       *          are to be cloned prior to execution
       */
      MethodConfigurator<T> verifyArguments();
      
      /**
       * Indicates that the arguments should be verified after the method is
       * executed. This is only useful in conjunction with cloning arguments
       * since otherwise the two argument objects, the one passed to the
       * test implementation and the one passed to the reference implementation,
       * will be the same instance.
       * 
       * <p>The specified verifiers will be used. The number of verifiers provided
       * must match the number of arguments for the method. A {@code null} verifier
       * should be passed for arguments that are not cloned. If an argument is not
       * cloned but a non-null verifier is provided, an exception is thrown.
       * 
       * @param verifiers  The sequence of verifiers
       * @return           this
       * @throws IllegalArgumentException If the number of cloners specified is
       *                   not the same as the number of method arguments or if a
       *                   non-null verifier is provided for an argument that is
       *                   not to be cloned prior to execution
       */
      MethodConfigurator<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers);
      
      /**
       * Indicates that the arguments should be verified after the method is
       * executed. This is only useful in conjunction with cloning arguments
       * since otherwise the two argument objects, the one passed to the
       * test implementation and the one passed to the reference implementation,
       * will be the same instance.
       * 
       * <p>The specified verifiers will be used. The number of verifiers provided
       * must match the number of arguments for the method. A {@code null} verifier
       * should be passed for arguments that are not cloned. If an argument is not
       * cloned but a non-null verifier is provided, an exception is thrown.
       * 
       * @param verifiers  The list of verifiers
       * @return           this
       * @throws IllegalArgumentException If the number of cloners specified is
       *                   not the same as the number of method arguments or if a
       *                   non-null verifier is provided for an argument that is
       *                   not to be cloned prior to execution
       * @throws NullPointerException If the specified list is null
       */
      MethodConfigurator<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers);
      
      /**
       * Indicates that the arguments should be verified after the method is
       * executed. This is only useful in conjunction with cloning arguments
       * since otherwise the two argument objects, the one passed to the
       * test implementation and the one passed to the reference implementation,
       * will be the same instance.
       * 
       * <p>The same verifier will be used for all cloned arguments.
       * 
       * @param verifier   The argument verifier
       * @return           this
       * @throws IllegalArgumentException If this method has no arguments that
       *          are to be cloned prior to execution
       * @throws NullPointerException If the specified verifier is null
       */
      MethodConfigurator<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier);
      
      /**
       * Indicates that arguments do not need to be verified after execution.
       * Note that arguments may still be cloned prior to execution. To prevent
       * argument cloning, use {@link #noCloneAndVerifyArguments()}.
       * 
       * @return this
       */
      MethodConfigurator<T> noVerifyArguments();
   }
   
   /**
    * A {@code MethodConfigurator} with accessors to inspect the current
    * configuration. This is useful when configuring one method at a time,
    * when the existing configuration is straight-forward to inspect and
    * interpret. (Multiple methods could have varying/conflicting
    * configuration settings).
    * 
    * @author jhumphries
    *
    * @param <T>  The interface implemented by the class under test or
    *             {@code Object} if testing a class against more than one
    *             implemented interface
    */
   public interface MethodConfiguration<T> extends MethodConfigurator<T> {

      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> mutator();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> mutator(ObjectVerifier<? super T> v);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> notMutator();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override <R> MethodConfiguration<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> returnVerifier(ObjectVerifier<?> v);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> strictExceptionVerifier();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> relaxedExceptionVerifier();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> exceptionVerifier(ObjectVerifier<Throwable> v);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      //@SafeVararg
      @Override MethodConfiguration<T> uncheckedExceptions(Class<? extends Throwable>... throwables);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> cloneAndVerifyArguments();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> cloneArguments();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> cloneArgumentsWith(Cloner<?>... cloners);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> cloneArgumentsWith(List<Cloner<?>> cloners);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> cloneAllArgumentsWith(Cloner<?> cloner);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> noCloneAndVerifyArguments();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> verifyArguments();
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier);
      
      /**
       * {@inheritDoc}
       * 
       * <p>Overridden to narrow the return type from {@code MethodConfigurator}
       * to {@code MethodConfiguration} in order to maintain the more specific
       * type information with method chaining.
       */
      @Override MethodConfiguration<T> noVerifyArguments();

      /**
       * Returns the set of methods configured by this object. All of the
       * methods will have the same signature. The set will only include
       * more than one method when the {@code InterfaceVerifier} is used
       * to verify more than one interface for a given object under test.
       * (In which case multiple interfaces could have methods with the
       * same signature.)
       * 
       * @return  The set of methods
       */
      Set<Method> getMethods();
      
      /**
       * Returns the method signature configured by this object.
       * 
       * @return  The method signature
       */
      MethodSignature getSignature();
      
      /**
       * Determines the appropriate return type for the method signature
       * configured by this object. If there are multiple methods with
       * the same signature, this will return the type of the most
       * specific return type.
       * 
       * @return  The effective return type for the method signature
       */
      Class<?> getReturnType();
      
      /**
       * Determines the set of checked exceptions thrown from the method
       * signature configured by this object. If there are multiple method
       * with the same signature, this will be a union of all checked
       * exceptions for all of the methods.
       * 
       * @return  The set of checked exceptions thrown by the method signature
       */
      Set<Class<? extends Throwable>> getCheckedExceptions();
      
      /**
       * Determine the set of <em>all</em> exceptios thrown from the method
       * signature configured by this object. This will be the union of both
       * checked and unchecked exceptions.
       * 
       * @return  The set of all exceptions thrown by the method signature
       * 
       * @see #getCheckedExceptions()
       * @see #getUncheckedExceptions()
       */
      Set<Class<? extends Throwable>> getAllExceptions();
      
      /**
       * Returns the verifier for mutations or {@code null} if this is not
       * a mutator method. If {@link #mutator()} was used to indicate this
       * is a mutator method and thus no verifier was specified, the default
       * mutator verifier will be returned.
       * 
       * @return  The verifier for mutations
       * 
       * @see InterfaceVerifier#getDefaultMutatorVerifier()
       */
      ObjectVerifier<? super T> getMutatorVerifier();
      
      /**
       * Returns true if this method is a mutator method.
       * 
       * @return  True if this method is a mutator method
       */
      boolean isMutator();

      /**
       * Returns the verifier used to check values returned from method
       * calls. If no verification is done (usually only for methods
       * that return {@code void}) then this will return {@code null}.
       * 
       * @return  The verifier for return values
       */
      ObjectVerifier<?> getReturnVerifier();
      
      /**
       * Returns the verifier used to check exceptions thrown from
       * method calls.
       * 
       * @return  The verifier for thrown exceptions
       */
      ObjectVerifier<Throwable> getExceptionVerifier();

      /**
       * Returns the set of unchecked exceptions thrown by method calls
       * as configured using {@link #uncheckedExceptions}.
       * 
       * @return  The set of unchecked exceptions
       */
      Set<Class<? extends Throwable>> getUncheckedExceptions();

      /**
       * Returns the list of argument cloners. The size of the list will be
       * the same as the number of arguments for the method. If an argument
       * is not cloned, the cloner in the corresponding position in the
       * list will be {@code null}. If no arguments are cloned then the
       * list will contain only {@code null} values.
       * 
       * @return  The list of argument cloners
       */
      List<Cloner<?>> getArgumentCloners();
      
      /**
       * Returns true if any arguments are cloned prior to method execution.
       * 
       * @return  True if any arguments are cloned
       */
      boolean isCloningArguments();

      /**
       * Returns the list of argument verifiers. The size of the list will
       * be the same as the number of arguments for the method. If an
       * argument is not verified, the verifier in the corresponding position
       * in the list will be {@code null}. If no arguments are verified then
       * the list will contain only {@code null} values.
       * 
       * @return  The list of argument cloners
       */
      List<ObjectVerifier<?>> getArgumentVerifiers();

      /**
       * Returns true if any arguments are verified after method execution.
       * 
       * @return  True if any arguments are verified
       */
      boolean isVerifyingArguments();
   }

   /**
    * Implementation of {@code MethodConfiguration}.
    * 
    * @author jhumphries
    */
   private class MethodConfigurationImpl implements MethodConfiguration<T>, Cloneable {

      // fields
      private MethodSignature sig;
      private Set<Method> confMethods;
      // default access so they can be accessed from MethodInvocationHandler below
      Class<?> returnType;
      Set<Class<? extends Throwable>> checkedExceptions;
      boolean isMutator;
      ObjectVerifier<? super T> mutatorVerifier;
      ObjectVerifier<?> returnVerifier;
      ObjectVerifier<Throwable> exceptionVerifier;
      boolean isRelaxedExceptions;
      Set<Class<? extends Throwable>> uncheckedExceptions;
      List<Cloner<?>> argumentCloners;
      List<ObjectVerifier<?>> argumentVerifiers;

      /**
       * Constructs a new method configuration for the specified method.
       * 
       * @param m The method to configure
       */
      public MethodConfigurationImpl(Method m) {
         assert m != null;
         
         // init collections
         confMethods = new HashSet<Method>();
         checkedExceptions =
            new HashSet<Class<? extends Throwable>>(m.getExceptionTypes().length);
         uncheckedExceptions = new HashSet<Class<? extends Throwable>>();

         // get info from method
         sig = new MethodSignature(m);
         confMethods.add(m);
         returnType = m.getReturnType();
         @SuppressWarnings("unchecked")
         Class<? extends Throwable> exceptions[] = (Class<? extends Throwable>[]) m.getExceptionTypes();
         Collections.addAll(checkedExceptions, exceptions);
      }
      
      /**
       * Resets the configuration to default.
       */
      public void reset() {
         // default config
         mutatorVerifier = null;
         if (returnType == Void.class || returnType == void.class) {
            isMutator = true;
            returnVerifier = null;
         } else if (returnType.isInterface()) {
            isMutator = false;
            returnVerifier = ObjectVerifiers.forTesting(returnType, returnType.getClassLoader());
         } else {
            isMutator = false;
            returnVerifier = ObjectVerifiers.EQUALS;
         }
         exceptionVerifier = null;
         isRelaxedExceptions = false;
         uncheckedExceptions.clear();
         int numArgs = sig.getParameterTypes().size();
         argumentCloners = new ArrayList<Cloner<?>>(
               Collections.<Cloner<?>> nCopies(numArgs, null));
         argumentVerifiers = new ArrayList<ObjectVerifier<?>>(
               Collections.<ObjectVerifier<?>> nCopies(numArgs, null));
      }
      
      @Override
      public MethodConfigurationImpl clone() {
         try {
            @SuppressWarnings("unchecked")
            MethodConfigurationImpl clone = (MethodConfigurationImpl) super.clone();
            // deep copy the collection fields
            clone.confMethods = new HashSet<Method>(this.confMethods);
            clone.checkedExceptions = new HashSet<Class<? extends Throwable>>(this.checkedExceptions);
            clone.uncheckedExceptions = new HashSet<Class<? extends Throwable>>(this.uncheckedExceptions);
            clone.argumentCloners = new ArrayList<Cloner<?>>(this.argumentCloners);
            clone.argumentVerifiers= new ArrayList<ObjectVerifier<?>>(this.argumentVerifiers);
            return clone;
         }
         catch (CloneNotSupportedException e) {
            throw new AssertionError(); // should never happen
         }
      }

      /**
       * Augments the current method configuration to include another method
       * (with the same signature as existing methods). This will narrow the
       * current return type for the configuration (if appropriate) and add
       * additional checked exceptions to the current configuration.
       * 
       * @param m The additional method
       * @throws IllegalArgumentException If the specified method has a return
       *          type that is incompatible with other methods with the same
       *          signature that are already included in the configuration
       */
      public void addMethod(Method m) {
         assert sig.equals(new MethodSignature(m));

         Class<?> newReturnType = m.getReturnType();
         if (returnType.isAssignableFrom(newReturnType)) {
            // new one is more specific
            returnType = newReturnType;

         } else if (!newReturnType.isAssignableFrom(returnType)) {
            // uh oh -- incompatible types
            StringBuilder msg = new StringBuilder();
            msg.append("Incompatible return types for method ");
            msg.append(sig.toString());
            msg.append(": ");
            msg.append(returnType);
            msg.append(" and ");
            msg.append(newReturnType);
            throw new IllegalArgumentException(msg.toString());
         }
         
         @SuppressWarnings("unchecked")
         Class<? extends Throwable> exceptions[] = (Class<? extends Throwable>[]) m.getExceptionTypes();
         Collections.addAll(checkedExceptions, exceptions);

         confMethods.add(m);
      }

      @Override
      public Set<Method> getMethods() {
         return Collections.unmodifiableSet(confMethods);
      }

      @Override
      public MethodSignature getSignature() {
         return sig;
      }

      @Override
      public Class<?> getReturnType() {
         return returnType;
      }

      @Override
      public Set<Class<? extends Throwable>> getCheckedExceptions() {
         return Collections.unmodifiableSet(checkedExceptions);
      }

      @Override
      public Set<Class<? extends Throwable>> getAllExceptions() {
         Set<Class<? extends Throwable>> ret =
            new HashSet<Class<? extends Throwable>>(checkedExceptions.size() + uncheckedExceptions.size());
         ret.addAll(checkedExceptions);
         ret.addAll(uncheckedExceptions);
         return Collections.unmodifiableSet(ret);
      }

      @Override
      public MethodConfiguration<T> mutator() {
         mutatorVerifier = null;
         isMutator = true;
         return this;
      }

      @Override
      public MethodConfiguration<T> mutator(ObjectVerifier<? super T> v) {
         if (v == null) {
            throw new NullPointerException();
         }
         mutatorVerifier = v;
         isMutator = true;
         return this;
      }

      @Override
      public MethodConfiguration<T> notMutator() {
         mutatorVerifier = null;
         isMutator = false;
         return this;
      }

      @Override
      public ObjectVerifier<? super T> getMutatorVerifier() {
         if (!isMutator) {
            return null;
         }
         if (mutatorVerifier == null) {
            return defaultMutatorVerifier;
         } else {
            return mutatorVerifier;
         }
      }

      @Override
      public boolean isMutator() {
         return isMutator;
      }

      @Override
      public <R> MethodConfiguration<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz) {
         if (clazz == null) {
            throw new NullPointerException();
         }
         if (!clazz.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException("Verifier type " + clazz.getName()
                  + " is not compatible with return type " + returnType.getName());
         }
         return returnVerifier(v);
      }

      @Override
      public MethodConfiguration<T> returnVerifier(ObjectVerifier<?> v) {
         if ((returnType == void.class || returnType == Void.class) && v != null) {
            throw new IllegalArgumentException("Non-null return verifier not allowed for void return type");
         }
         returnVerifier = v;
         return this;
      }

      @Override
      public ObjectVerifier<?> getReturnVerifier() {
         return returnVerifier;
      }

      @Override
      public MethodConfiguration<T> strictExceptionVerifier() {
         exceptionVerifier = ObjectVerifiers.STRICT_EXCEPTIONS;
         isRelaxedExceptions = false;
         return this;
      }

      @Override
      public MethodConfiguration<T> relaxedExceptionVerifier() {
         exceptionVerifier = null;
         isRelaxedExceptions = true;
         return this;
      }
      
      @Override
      public MethodConfiguration<T> exceptionVerifier(ObjectVerifier<Throwable> v) {
         exceptionVerifier = v;
         isRelaxedExceptions = false;
         return this;
      }

      @Override
      public ObjectVerifier<Throwable> getExceptionVerifier() {
         if (isRelaxedExceptions || (exceptionVerifier == null && defaultExceptionVerifier == null)) {
            return ObjectVerifiers.relaxedExceptions(getAllExceptions());
         } else {
            return exceptionVerifier == null ? defaultExceptionVerifier : exceptionVerifier;
         }
      }

      //@SafeVararg
      @Override
      public MethodConfiguration<T> uncheckedExceptions(Class<? extends Throwable>... throwables) {
         return uncheckedExceptions(new HashSet<Class<? extends Throwable>>(Arrays.asList(throwables)));
      }

      @Override
      public MethodConfiguration<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables) {
         if (throwables == null) {
            throw new NullPointerException();
         }
         for (Class<? extends Throwable> t : throwables) {
            if (t == null) {
               throw new NullPointerException();
            }
         }
         // defensive copy
         uncheckedExceptions = new HashSet<Class<? extends Throwable>>(throwables);
         return this;
      }

      @Override
      public Set<Class<? extends Throwable>> getUncheckedExceptions() {
         return Collections.unmodifiableSet(uncheckedExceptions);
      }

      @Override
      public MethodConfiguration<T> cloneAndVerifyArguments() {
         cloneArguments();
         verifyArguments();
         return this;
      }

      @Override
      public MethodConfiguration<T> cloneArguments() {
         return cloneAllArgumentsWith(Cloners.GENERIC_CLONER);
      }

      @Override
      public MethodConfiguration<T> cloneArgumentsWith(Cloner<?>... cloners) {
         return cloneArgumentsWith(Arrays.asList(cloners));
      }

      @Override
      public MethodConfiguration<T> cloneArgumentsWith(List<Cloner<?>> cloners) {
         if (cloners == null) {
            throw new NullPointerException();
         }
         List<Class<?>> args = sig.getParameterTypes();
         if (cloners.size() != args.size()) {
            throw new IllegalArgumentException("Wrong number of cloners specified");
         }
         Iterator<Cloner<?>> clnIter = cloners.iterator();
         Iterator<Class<?>> argIter = args.iterator();
         while (clnIter.hasNext()) {
            Cloner<?> cloner = clnIter.next();
            Class<?> argType = argIter.next();
            if (argType.isPrimitive() && cloner != null) {
               throw new IllegalArgumentException("Non-null cloner specified for primitive argument");
            }
         }
         // defensive copy
         argumentCloners = new ArrayList<Cloner<?>>(cloners);
         return this;
      }

      @Override
      public MethodConfiguration<T> cloneAllArgumentsWith(Cloner<?> cloner) {
         if (cloner == null) {
            throw new NullPointerException();
         }
         List<Class<?>> args = sig.getParameterTypes();
         List<Cloner<?>> cloners = new ArrayList<Cloner<?>>(args.size());
         int used = 0;
         for (Class<?> argType : args) {
            if (argType.isPrimitive()) {
               cloners.add(null);
            } else {
               cloners.add(cloner);
               used++;
            }
         }
         if (used == 0) {
            throw new IllegalArgumentException("Method has no arguments to clone");
         }
         argumentCloners = cloners;
         return this;
      }

      @Override
      public MethodConfiguration<T> noCloneAndVerifyArguments() {
         noVerifyArguments();
         // set cloners to all null
         int numArgs = sig.getParameterTypes().size();
         argumentCloners = new ArrayList<Cloner<?>>(
               Collections.<Cloner<?>> nCopies(numArgs, null));
         return this;
      }

      @Override
      public List<Cloner<?>> getArgumentCloners() {
         return Collections.unmodifiableList(argumentCloners);
      }

      @Override
      public boolean isCloningArguments() {
         for (Cloner<?> c : argumentCloners) {
            if (c != null) return true;
         }
         return false;
      }

      @Override
      public MethodConfiguration<T> verifyArguments() {
         return verifyAllArgumentsWith(ObjectVerifiers.EQUALS);
      }

      @Override
      public MethodConfiguration<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers) {
         return verifyArgumentsWith(Arrays.asList(verifiers));
      }

      @Override
      public MethodConfiguration<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers) {
         if (verifiers == null) {
            throw new NullPointerException();
         }
         if (verifiers.size() != argumentCloners.size()) {
            throw new IllegalArgumentException("Wrong number of verifiers specified");
         }
         Iterator<ObjectVerifier<?>> vfyIter = verifiers.iterator();
         Iterator<Cloner<?>> clnIter = argumentCloners.iterator();
         while (vfyIter.hasNext()) {
            ObjectVerifier<?> verifier = vfyIter.next();
            Cloner<?> cloner = clnIter.next();
            if (cloner == null && verifier != null) {
               throw new IllegalArgumentException("Non-null verifier specified for non-cloned argument");
            }
         }
         // defensive copy
         argumentVerifiers = new ArrayList<ObjectVerifier<?>>(verifiers);
         return this;
      }

      @Override
      public MethodConfiguration<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier) {
         if (verifier == null) {
            throw new NullPointerException();
         }
         if (! isCloningArguments()) {
            throw new IllegalArgumentException("No cloned arguments to verify");
         }
         List<ObjectVerifier<?>> verifiers = new ArrayList<ObjectVerifier<?>>(argumentCloners.size());
         for (Cloner<?> c : argumentCloners) {
            verifiers.add(c == null ? null : verifier);
         }
         argumentVerifiers = verifiers;
         return this;
      }

      @Override
      public MethodConfiguration<T> noVerifyArguments() {
         int numArgs = sig.getParameterTypes().size();
         argumentVerifiers = new ArrayList<ObjectVerifier<?>>(
               Collections.<ObjectVerifier<?>> nCopies(numArgs, null));
         return this;
      }

      @Override
      public List<ObjectVerifier<?>> getArgumentVerifiers() {
         return Collections.unmodifiableList(argumentVerifiers);
      }

      @Override
      public boolean isVerifyingArguments() {
         for (ObjectVerifier<?> v : argumentVerifiers) {
            if (v != null) return true;
         }
         return false;
      }
   }

   /**
    * An implementation of {@code MethodConfigurator} that dispatches
    * configuration updates to multiple method configuration objects.
    * 
    * @author jhumphries
    */
   private class MultiMethodConfigurator implements MethodConfigurator<T> {
      
      private Set<MethodConfigurationImpl> configs;
      
      public MultiMethodConfigurator(Set<MethodConfigurationImpl> configs) {
         this.configs = configs;
      }
      
      // all methods look the same -- dispatch method to all configs
      // and then return this

      @Override
      public MethodConfigurator<T> mutator() {
         for (MethodConfigurationImpl conf : configs) {
            conf.mutator();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> mutator(ObjectVerifier<? super T> v) {
         for (MethodConfigurationImpl conf : configs) {
            conf.mutator(v);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> notMutator() {
         for (MethodConfigurationImpl conf : configs) {
            conf.notMutator();
         }
         return this;
      }

      @Override
      public <R> MethodConfigurator<T> returnVerifier(ObjectVerifier<R> v, Class<R> clazz) {
         for (MethodConfigurationImpl conf : configs) {
            conf.returnVerifier(v, clazz);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> returnVerifier(ObjectVerifier<?> v) {
         for (MethodConfigurationImpl conf : configs) {
            conf.returnVerifier(v);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> strictExceptionVerifier() {
         for (MethodConfigurationImpl conf : configs) {
            conf.strictExceptionVerifier();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> relaxedExceptionVerifier() {
         for (MethodConfigurationImpl conf : configs) {
            conf.relaxedExceptionVerifier();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> exceptionVerifier(ObjectVerifier<Throwable> v) {
         for (MethodConfigurationImpl conf : configs) {
            conf.exceptionVerifier(v);
         }
         return this;
      }

      //@SafeVararg
      @Override
      public MethodConfigurator<T> uncheckedExceptions(Class<? extends Throwable>... throwables) {
         for (MethodConfigurationImpl conf : configs) {
            conf.uncheckedExceptions(throwables);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> uncheckedExceptions(Set<Class<? extends Throwable>> throwables) {
         for (MethodConfigurationImpl conf : configs) {
            conf.uncheckedExceptions(throwables);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> cloneAndVerifyArguments() {
         for (MethodConfigurationImpl conf : configs) {
            conf.cloneAndVerifyArguments();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> cloneArguments() {
         for (MethodConfigurationImpl conf : configs) {
            conf.cloneArguments();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> cloneArgumentsWith(Cloner<?>... cloners) {
         for (MethodConfigurationImpl conf : configs) {
            conf.cloneArgumentsWith(cloners);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> cloneArgumentsWith(List<Cloner<?>> cloners) {
         for (MethodConfigurationImpl conf : configs) {
            conf.cloneArgumentsWith(cloners);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> cloneAllArgumentsWith(Cloner<?> cloner) {
         for (MethodConfigurationImpl conf : configs) {
            conf.cloneAllArgumentsWith(cloner);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> noCloneAndVerifyArguments() {
         for (MethodConfigurationImpl conf : configs) {
            conf.noCloneAndVerifyArguments();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> verifyArguments() {
         for (MethodConfigurationImpl conf : configs) {
            conf.verifyArguments();
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> verifyArgumentsWith(ObjectVerifier<?>... verifiers) {
         for (MethodConfigurationImpl conf : configs) {
            conf.verifyArgumentsWith(verifiers);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> verifyArgumentsWith(List<ObjectVerifier<?>> verifiers) {
         for (MethodConfigurationImpl conf : configs) {
            conf.verifyArgumentsWith(verifiers);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> verifyAllArgumentsWith(ObjectVerifier<?> verifier) {
         for (MethodConfigurationImpl conf : configs) {
            conf.verifyAllArgumentsWith(verifier);
         }
         return this;
      }

      @Override
      public MethodConfigurator<T> noVerifyArguments() {
         for (MethodConfigurationImpl conf : configs) {
            conf.noVerifyArguments();
         }
         return this;
      }
      
   }

   /**
    * Invocation handler that invokes the method on both test and reference
    * objects and validates that the test implementation behaves correctly.
    * 
    * @author jhumphries
    */
   private class MethodInvocationHandler implements InvocationHandler {

      /**
       * The implementation under test.
       */
      private T testImpl;
      
      /**
       * The reference implementation.
       */
      private T referenceImpl;
      
      /**
       * Constructs a new invocation handler. The two implementations
       * are each used during method calls and the test implementation
       * is verified against the reference implementation.
       * 
       * @param testImpl      The test implementation
       * @param referenceImpl The reference implementation
       */
      public MethodInvocationHandler(T testImpl, T referenceImpl) {
         this.testImpl = testImpl;
         this.referenceImpl = referenceImpl;
      }

      public InterfaceVerifier<T> getVerifier() {
         return InterfaceVerifier.this;
      }

      /**
       * Invokes the specified method on both the test and reference objects. The
       * logic then verifies the test implementation by comparing the results to
       * that of the reference implementation.
       * 
       * @param proxy   The object on which the method was invoked
       * @param method  The method that was invoked
       * @param args    The arguments passed to the invoked method
       * @throws junit.framework.AssertionFailedError if the behavior of the test
       *                implementation fails to match the behavior of the
       *                reference implementation
       * @throws Throwable if the method under test throws anything and
       *                {@link #isSuppressExceptions()} is false
       */
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         MethodSignature sig = new MethodSignature(method);
         MethodConfigurationImpl conf = methodConfig.get(sig);
         Object refArgs[] = args;
         
         // see if we need to clone any of the arguments prior to method invocation
         int idx = 0;
         for (Cloner<?> cloner : conf.argumentCloners) {
            if (cloner != null) {
               @SuppressWarnings("unchecked")
               Cloner<Object> objCloner = (Cloner<Object>) cloner;
               refArgs[idx] = objCloner.clone(args[idx]);
            }
            idx++;
         }
         
         // invoke the method
         Throwable testThrown = null, referenceThrown = null;
         Object testRet = null, referenceRet = null;
         // call the method on both test and reference implementations
         try {
            testRet = method.invoke(testImpl, args);
         } catch (InvocationTargetException e) {
            testThrown = e.getCause();
         }
         lastException = testThrown; // save this for use from getLastException()
         
         try {
            referenceRet = method.invoke(referenceImpl, refArgs);
         } catch (InvocationTargetException e) {
            referenceThrown = e.getCause();
         }

         // make sure test implementation behaves the same as reference implementation
         if (referenceThrown != null || testThrown != null) {
            ObjectVerifier<Throwable> v = conf.getExceptionVerifier();
            v.verify(testThrown, referenceThrown);
            
         } else {
            // up-cast the generic type to Object in order to do this check
            // (can result in ClassCastExceptions at runtime if a verifier of
            // the wrong type is registered for a method)
            @SuppressWarnings("unchecked")
            ObjectVerifier<Object> v = (ObjectVerifier<Object>) conf.returnVerifier;
            
            if (v != null) {
               testRet = v.verify(testRet, referenceRet);
            }
         }
         
         if (conf.isMutator) {
            // verify mutations by comparing objects
            ObjectVerifier<? super T> v = conf.getMutatorVerifier();
            
            v.verify(testImpl, referenceImpl);
            
            if (checkHashCodesAfterMutation) {
               ObjectVerifiers.HASH_CODES.verify(testImpl, referenceImpl);
            }
         }

         // see if we need to verify any mutated arguments
         idx = 0;
         for (ObjectVerifier<?> v : conf.argumentVerifiers) {
            if (v != null) {
               @SuppressWarnings("unchecked")
               ObjectVerifier<Object> objVerifier= (ObjectVerifier<Object>) v;
               
               objVerifier.verify(args[idx], refArgs[idx]);
            }
            idx++;
         }

         // finish
         if (testThrown != null) {
            if (suppressExceptions) {
               return ProxyUtil.getNullReturnValue(conf.returnType);
            } else {
               throw testThrown;
            }
         }
         return testRet;
      }
   }
   
   /**
    * The set of interfaces to test.
    */
   private HashSet<Class<? extends T>> interfaces;

   /**
    * A set of all supported methods.
    */
   private HashSet<Method> methods;
   
   /**
    * A structure for querying for method signatures. The keys are method names,
    * and the values are sub-maps. The sub-maps' keys are lists of parameter types
    * and the values are the actual method signatures composed of the two keys --
    * method signature = name (first key) and parameter types (second key).
    */
   private HashMap<String, HashMap<List<Class<?>>, MethodSignature>> methodSigMap;

   /**
    * A map of all supported method signatures to their configuration.
    * The keys form the set of all supported method signatures.
    */
   HashMap<MethodSignature, MethodConfigurationImpl> methodConfig;
   
   /**
    * The default verifier used to make sure mutation operations
    * change the test object in the same way as the reference object.
    * It can be overridden for a particular mutator method in that
    * method's configuration.
    * 
    * @see #setDefaultMutatorVerifier(ObjectVerifier)
    */
   ObjectVerifier<? super T> defaultMutatorVerifier;

   /**
    * The default verifier used to make sure exceptions thrown by
    * test object match exceptions thrown by the reference object.
    * It can be overridden for a particular method in that method's
    * configuration.
    * 
    * @see #setDefaultExceptionVerifier(ObjectVerifier)
    */
   ObjectVerifier<Throwable> defaultExceptionVerifier;
   
   /**
    * A flag indicating whether or not hash codes should be checked
    * when verifying mutator methods.
    */
   boolean checkHashCodesAfterMutation;
   
   /**
    * A flag indicating whether or not exceptions should be suppressed.
    * If true, exceptions generated in method invocations will be
    * suppressed and those methods will instead return {@code null}.
    * 
    * @see #setSuppressExceptions(boolean)
    */
   boolean suppressExceptions;
   
   /**
    * The exception thrown during the most recent method invocation or
    * {@code null} if no exception was thrown.
    * 
    * @see #getLastException()
    */
   Throwable lastException;
   
   /**
    * Constructs a new verifier for a single interface.
    * 
    * @param iface   The interface whose implementation will be verified
    * @throws IllegalArgumentException If the specified class tokens does not
    *                represent an interface
    * @throws NullPointerException If the specified interface is {@code null}
    */
   public InterfaceVerifier(Class<T> iface) {
      this(Collections.<Class<? extends T>> singleton(iface));
   }
   
   /**
    * Constructs a new verifier for one or more interfaces.
    * 
    * @param interfaces The interfaces whose implementations will be verified
    * @throws IllegalArgumentException if no interfaces are specified, if any of the
    *                   specified class tokens does not represent an interface, or if
    *                   the interfaces are incompatible (they have a method with the
    *                   same signature but different and incompatible return types)
    * @throws NullPointerException If any of the specified interfaces are {@code null}
    */
   //@SafeVararg
   public InterfaceVerifier(Class<? extends T>... interfaces) {
      this(new HashSet<Class<? extends T>>(Arrays.asList(interfaces)));
   }
   
   /**
    * Constructs a new verifier for one or more interfaces.
    * 
    * @param interfaces The interfaces whose implementations will be verified
    * @throws IllegalArgumentException if no interfaces are specified, if any of the
    *                   specified class tokens does not represent an interface, or if
    *                   the interfaces are incompatible (they have a method with the
    *                   same signature but different and incompatible return types)
    * @throws NullPointerException If the set of interfaces is {@code null} or if any
    *                   of the specified interfaces therein are {@code null}
    */
   public InterfaceVerifier(Set<Class<? extends T>> interfaces) {
      if (interfaces == null) {
         throw new NullPointerException();
      }
      if (interfaces.isEmpty()) {
         throw new IllegalArgumentException();
      }
      for (Class<?> iface : interfaces) {
         if (iface == null) {
            throw new NullPointerException();
         }
         if (! iface.isInterface()) {
            throw new IllegalArgumentException();
         }
      }
      this.interfaces = new HashSet<Class<? extends T>>(interfaces); // defensive copy
      
      methods = new HashSet<Method>();
      methodSigMap = new HashMap<String, HashMap<List<Class<?>>, MethodSignature>>();
      methodConfig = new HashMap<MethodSignature, MethodConfigurationImpl>();

      // accumulate all methods
      for (Class<?> iface : interfaces) {
         for (Method m : iface.getMethods()) {
            methods.add(m);
            MethodSignature sig = new MethodSignature(m);
            // create configuration for this signature
            MethodConfigurationImpl conf = methodConfig.get(sig);
            if (conf == null) {
               methodConfig.put(sig, new MethodConfigurationImpl(m));
               // store signature in searchable structure
               String name = sig.getName();
               HashMap<List<Class<?>>, MethodSignature> sigs;
               if (methodSigMap.containsKey(name)) {
                  sigs = methodSigMap.get(name);
               } else {
                  sigs = new HashMap<List<Class<?>>, MethodSignature>();
                  methodSigMap.put(name, sigs);
               }
               sigs.put(sig.getParameterTypes(), sig);
            } else {
               conf.addMethod(m);
            }
         }
      }

      // apply default configuration:
      reset();
   }
   
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
    * methods. By default, this verifier will be {@link ObjectVerifiers#EQUALS}.
    * 
    * @return the default verifier for mutations
    */
   public ObjectVerifier<? super T> getDefaultMutatorVerifier() {
      return defaultMutatorVerifier;
   }

   /**
    * Sets the default verifier used to verify the exceptions thrown by
    * methods. Individual methods can override this default verifier.
    * 
    * @param v the new default verifier
    * @throws NullPointerException if the specified verifier is {@code null}
    */
   public void setDefaultExceptionVerifier(ObjectVerifier<Throwable> v) {
      if (v == null) {
         throw new NullPointerException("verifier");
      }
      defaultExceptionVerifier = v;
   }
   
   /**
    * Sets the default verifier used to verify the exceptions thrown by
    * methods to use strict checking. Individual methods can override this
    * default verifier. The strict checking verifier is
    * {@link ObjectVerifiers#STRICT_EXCEPTIONS}.
    */
   public void setStrictDefaultExceptionVerifier() {
      defaultExceptionVerifier = ObjectVerifiers.STRICT_EXCEPTIONS;
   }

   /**
    * Sets the default verifier used to verify the exceptions thrown by
    * methods to use relaxed checking. Individual methods can override this
    * default verifier. The relaxed checking verifier is built using
    * {@link ObjectVerifiers#relaxedExceptions(Set)} and can actually
    * vary from one method to another, depending on the exceptions
    * thrown by a method.
    */
   public void setRelaxedDefaultExceptionVerifier() {
      defaultExceptionVerifier = null;
   }

   /**
    * Returns the default verifier used to verify exceptions thrown by
    * methods. By default this verifier will be {@link ObjectVerifiers#STRICT_EXCEPTIONS}.
    * 
    * <p>This method returns {@code null} if the default verifier is using
    * relaxed checking. This is because no single verifier is used. Instead,
    * a verifier is created per method, based on the actual exceptions thrown
    * by that method (both declared/checked exceptions as well as configured
    * unchecked exceptions).
    * 
    * @return  The default verifier for thrown exceptions or {@code null} if
    *          relaxed checking is the default
    */
   public ObjectVerifier<Throwable> getDefaultExceptionVerifier() {
      return defaultExceptionVerifier;
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
    * By default this is true.
    * 
    * @return  True if exceptions will be suppressed
    */
   public boolean isSuppressExceptions() {
      return suppressExceptions;
   }
   
   /**
    * Returns the exception thrown during the most recent method invocation
    * or {@code null} if the most recent invocation did not throw an
    * exception. This returns the excpetion thrown by the implementation
    * under test. So if the reference implementation threw an exception but
    * the test implementation did not, this would return {@code null}.
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
      this.defaultMutatorVerifier = ObjectVerifiers.EQUALS;
      this.checkHashCodesAfterMutation = false;
      this.defaultExceptionVerifier = ObjectVerifiers.STRICT_EXCEPTIONS;
      this.suppressExceptions = true;
      // and now set their default attributes
      for (MethodConfigurationImpl conf : methodConfig.values()) {
         conf.reset();
      }
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
   
   /**
    * Returns a new method capturer for this verifier's interfaces. Each call to
    * this results in a new capturer.
    * 
    * @return  A method capturer for this verifier's interfaces
    */
   public MethodCapturer<T> methodCapturer() {
      return new MethodCapturer<T>(interfaces);
   }
   
   /**
    * Returns the set of interfaces supported by this verifier.
    * 
    * @return  A set of interfaces
    */
   public Set<Class<? extends T>> getInterfaces() {
      return Collections.unmodifiableSet(interfaces);
   }

   /**
    * Returns all methods supported by this interface verifier.
    * 
    * @return  The set of all supported methods
    */
   public Set<Method> allMethods() {
      return Collections.unmodifiableSet(methods);
   }

   /**
    * Returns all method signatures supported by this interface verifier.
    * 
    * @return  The set of all supported method signatures
    */
   public Set<MethodSignature> allMethodSignatures() {
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

   /**
    * Returns the configuration for the specified method. The configuration can
    * be used to change the configuration of how invocations of that method are
    * verified.
    * 
    * @param sig  The signature of the method to configure
    * @return     The configuration for the specified method signature
    * @throws IllegalArgumentException If the specified method is not
    *          supported by this handler (i.e. not a method on any of the
    *          handler's interfaces)
    * @throws NullPointerException If the specified method is {@code null}
    */
   public MethodConfiguration<T> configureMethod(MethodSignature sig) {
      verifyMethods(sig);
      return methodConfig.get(sig);
   }

   /**
    * Returns a configurator for configuring multiple methods at the same time.
    * All configuration changes are applied to all of the specified method
    * signatures. If configuration changes are invalid for any of the included
    * methods then an exception will be thrown.
    * 
    * @param sigs The signatures of the methods to configure
    * @return     A configurator for configuring all specified methods at once
    * @throws IllegalArgumentException If any of the specified methods is not
    *          supported by this handler (i.e. not a method on any of the
    *          handler's interfaces) or if no methods are specified
    * @throws NullPointerException If any of the specified methods are {@code null}
    */
   public MethodConfigurator<T> configureMethods(MethodSignature... sigs) {
      if (sigs.length == 0) {
         throw new IllegalArgumentException("No method signatures specified");
      }
      verifyMethods(sigs);
      HashSet<MethodConfigurationImpl> configs = new HashSet<MethodConfigurationImpl>(sigs.length);
      for (MethodSignature sig : sigs) {
         configs.add(methodConfig.get(sig));
      }
      return new MultiMethodConfigurator(configs);
   }
   
   /**
    * Creates a proxy object. Methods on the proxy are handled by an invocation
    * handler that dispatches to the test and reference implementations for
    * verifying the functionality of the test implementation.
    * 
    * <p>The current object's class loader ({@code this.getClass().getClassLoader()})
    * is used to define the proxy.
    * 
    * @param testImpl      The test implementation
    * @param referenceImpl The reference implementation
    * @return              The proxy object
    * @throws IllegalArgumentException If any of the interfaces to be implemented
    *                      by the proxy are not visible by name through the
    *                      current object's class loader
    */
   public T createProxy(T testImpl, T referenceImpl) {
      return createProxy(testImpl, referenceImpl, this.getClass().getClassLoader());
   }

   /**
    * Creates a proxy object. Methods on the proxy are handled by an invocation
    * handler that dispatches to the test and reference implementations for
    * verifying the functionality of the test implementation.
    * 
    * @param testImpl      The test implementation
    * @param referenceImpl The reference implementation
    * @param classLoader   The class loader used to define the proxy
    * @return              The proxy object
    * @throws IllegalArgumentException If either of the implementations does not
    *                      implement all required interfaces or if any of the
    *                      interfaces are not visible by name through the
    *                      specified class loader
    * @throws NullPointerException If either of the implementations specified
    *                      is {@code null}
    */
   public T createProxy(T testImpl, T referenceImpl, ClassLoader classLoader) {
      Class<?> testClass = testImpl.getClass();
      Class<?> referenceClass = referenceImpl.getClass();
      for (Class<?> iface : interfaces) {
         // check that the interfaces are all correct for specified objects
         if (!iface.isAssignableFrom(testClass)) {
            throw new IllegalArgumentException("Interface " + iface.getName()
                  + " incompatible with test class " + testClass.getName());
         }
         if (!iface.isAssignableFrom(referenceClass)) {
            throw new IllegalArgumentException("Interface " + iface.getName()
                  + " incompatible with reference class " + referenceClass.getName());
         }
      }
      @SuppressWarnings("unchecked")
      T ret = (T) Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[0]),
            new MethodInvocationHandler(testImpl, referenceImpl));
      return ret;
   }

   /**
    * Returns the {@code InterfaceVerifier} for configuring the specified proxy object.
    * This is particularly useful for configuring the verification of returned
    * interfaces.
    * 
    * @param <T>     The interface that the proxy implements
    * @param proxy   The proxy
    * @return        The invocation handler for the proxy
    * @throws IllegalArgumentException If the specified object is not actually
    *                            a proxy or if it is a proxy that was not created
    *                            using {@link #createProxy}
    * @throws NullPointerException If the specified proxy is {@code null}
    * 
    * @see ObjectVerifiers#forTesting(Class)
    */
   public static <T> InterfaceVerifier<T> verifierFor(T proxy) {
      try {
         @SuppressWarnings("unchecked")
         InterfaceVerifier<T>.MethodInvocationHandler handler =
               (InterfaceVerifier<T>.MethodInvocationHandler) Proxy.getInvocationHandler(proxy);
         return handler.getVerifier();
      } catch (ClassCastException e) {
         throw new IllegalArgumentException("Proxy not created by an InterfaceVerifier", e);
      }
   }

   /**
    * Copies the configuration from another verifier. This can be useful when
    * testing a whole graph of related interfaces. In such a case, you could
    * configure verification of all interfaces in a set-up step using multiple
    * {@code InterfaceVerifier}s. Additional verifiers will be created during
    * method calls that return interface types (for subsequent verification
    * of returned interfaces). You can then quickly configure those verifiers
    * by copying the configuration defined during set-up using this method.
    * 
    * @param other   A verifier whose configuration will be copied into this
    * @throws IllegalArgumentException If the specified verifier does not
    *                support exactly the same set of interfaces as this
    */
   public void copyFrom(InterfaceVerifier<T> other) {
      if (!this.interfaces.equals(other.interfaces)) {
         // build error message
         StringBuilder msg = new StringBuilder();
         msg.append("Incompatible set of supported interfaces: (");
         boolean first = true;
         for (Class<?> clz : other.interfaces) {
            if (first) {
               first = false;
            } else {
               msg.append(", ");
            }
            msg.append(clz.getName());
         }
         msg.append(") != (");
         first = true;
         for (Class<?> clz : this.interfaces) {
            if (first) {
               first = false;
            } else {
               msg.append(", ");
            }
            msg.append(clz.getName());
         }
         msg.append(")");
         throw new IllegalArgumentException(msg.toString());
      }
      this.defaultMutatorVerifier = other.defaultMutatorVerifier;
      this.checkHashCodesAfterMutation = other.checkHashCodesAfterMutation;
      this.defaultExceptionVerifier = other.defaultExceptionVerifier;
      this.suppressExceptions = other.suppressExceptions;
      // now copy the method configuration
      this.methodConfig.clear();
      for (Map.Entry<MethodSignature, InterfaceVerifier<T>.MethodConfigurationImpl> entry
            : other.methodConfig.entrySet()) {
         this.methodConfig.put(entry.getKey(), entry.getValue().clone());
      }
   }
}
