package com.apriori.testing;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A mechanism for capturing Java method calls. This is useful to lookup
 * instances of {@code java.lang.reflect.Method} and {@link MethodSignature}
 * with compile-time type and method checking instead of relying only on
 * runtime lookup/checking with a String and array of argument types.
 * 
 * @see #capture()
 * 
 * @author jhumphries
 *
 * @param <E> the interface whose methods are captured or {@code Object}
 *          if methods from multiple interfaces are captured
 */
public class MethodCapturer<E> {
   
   private Set<Class<? extends E>> interfaces;
   private E proxy;
   private Method captured;
   private MethodSignature capturedSig;
   
   private MethodCapturer(Class<E> iface) {
      this(Collections.<Class<? extends E>> singleton(iface));
   }
   
   @SuppressWarnings("unchecked")
   private MethodCapturer(Set<Class<? extends E>> interfaces) {
      // check interfaces
      if (interfaces.size() == 0) {
         throw new IllegalArgumentException("Must provide at least one interface");
      }
      HashSet<ClassLoader> classLoaders = new HashSet<ClassLoader>();
      for (Class<?> clazz : interfaces) {
         if (!clazz.isInterface()) {
            throw new IllegalArgumentException(clazz.getName() + " is not an interface");
         }
         classLoaders.add(clazz.getClassLoader());
      }
      this.interfaces = interfaces;
      // use first class loader that works for creating proxy
      IllegalArgumentException e = null;
      for (ClassLoader classLoader : classLoaders) {
         try {
            proxy = (E) Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[interfaces.size()]), new InvocationHandler() {
               @SuppressWarnings("synthetic-access")
               @Override
               public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                  captured = method;
                  capturedSig = new MethodSignature(method);
                  return ProxyUtil.getNullReturnValue(method.getReturnType());
               }
            });
            break; // got it!
         } catch (IllegalArgumentException thrown) {
            e = thrown;
         }
      }
      // couldn't create proxy!
      if (proxy == null) {
         throw e;
      }
      // done initializing!
   }
   
   /**
    * Creates an instance for capturing methods for a single interface. This is the
    * most common type of capturer.
    * 
    * @param <T> the interface whose methods will be captured
    * @param iface the class token for the interface whose methods will be captured
    * @return the method capturer
    * @throws IllegalArgumentException if the specified class token does not represent
    *          an interface
    * @throws NullPointerException if the specified class token is {@code null}
    */
   public static <T> MethodCapturer<T> forOne(Class<T> iface) {
      return new MethodCapturer<T>(iface);
   }

   /**
    * Creates an instance for capturing methods for multiple interfaces. This type of
    * capturer is less common than ones for a single interface.
    * 
    * <p>This method cannot be used for a single interface. If only one interface is
    * specified in the var-args then an {@code IllegalArgumentException} will be
    * thrown. For one interface, use {@link #forOne(Class)} instead.
    * 
    * @param interfaces the class tokens for the interfaces whose methods will be
    *          captured
    * @return the method capturer
    * @throws IllegalArgumentException if no interfaces are specified or if only one
    *          interface is specified, if any of the specified class tokens does not
    *          represent an interface, if the interfaces are incompatible (they have
    *          a method with the same signature but different and incompatible return
    *          types), or if a proxy object for the interfaces cannot be created using
    *          the {@code ClassLoader} of any of the interfaces
    * @throws NullPointerException if any of the specified class tokens is {@code null}
    */
   public static MethodCapturer<Object> forMultiple(Class<?>... interfaces) {
      if (interfaces.length == 1) {
         throw new IllegalArgumentException("More than one interface expected. " +
         		"Use MethodCapturer.forOne(Class) to create an instance for a single interface");
      }
      return new MethodCapturer<Object>(new HashSet<Class<? extends Object>>(Arrays.asList(interfaces)));
   }
   
   /**
    * Creates an instance for capturing methods for one or more interfaces.
    * 
    * @param interfaces the class tokens for the interfaces whose methods will be
    *          captured
    * @return the method capturer
    * @throws IllegalArgumentException if no interfaces are specified (empty set), if
    *          any of the specified class tokens does not represent an interface, if
    *          the interfaces are incompatible (they have a method with the same
    *          signature but different and incompatible return types), or if a proxy
    *          object for the interfaces cannot be created using the {@code ClassLoader}
    *          of any of the interfaces
    * @throws NullPointerException if any of the specified class tokens is {@code null}
    */
   public static MethodCapturer<Object> forMultiple(Set<Class<? extends Object>> interfaces) {
      return new MethodCapturer<Object>(interfaces);
   }
   
   /**
    * Returns the set of interfaces whose methods can be captured. If
    * only a single interface's methods are captured then this returns
    * a singleton set with just {@code Class<E>}.
    * 
    * @return set of interfaces whose methods can be captured
    */
   public Set<Class<? extends E>> getInterfaces() {
      return interfaces;
   }
   
   /**
    * Returns a proxy for the interfaces whose methods are captured. Invoking a
    * method on the returned object will "capture" the method call. Subsequent calls
    * to {@link #getMethod()} or {@link #getSignature()} will return the method
    * called. Invoking the method does nothing else (no real implementation) and
    * will return null (or zero for numeric primitive return types; false for boolean
    * primitive return type). So parameters passed to the method can all be
    * {@code null}. The method invocation just lets the compiler verify that the
    * method is valid and lets client programs access it as a {@code java.lang.Method}
    * or {@code MethodSignature} without runtime lookups.
    * 
    * <p>Note that multiple calls to this method all return the same object and do
    * not in any way "reset" the last captured method. Also note that multiple calls
    * to this proxy do not in any way "queue" the captured methods. The {@link #getMethod()}
    * and {@link #getSignature()} methods always return the last method called.
    * 
    * <p>Take the following example of an the wrong way to use the capturer.
    * <pre>
    * MethodCapturer&lt;MyInterface&gt; capturer = MethodCapturer.forOne(MyInterface.class);
    * MyInterface i = capturer.capture();
    * 
    * // call some methods
    * i.someMethod();
    * i.someOtherMethod(null, null);
    * i.yetAnotherMethod(null);
    * 
    * // get the method objects
    * Method m1 = capturer.getMethod();
    * Method m2 = capturer.getMethod();
    * Method m3 = capturer.getMethod();
    * </pre>
    * All returned methods -- {@code m1}, {@code m2}, and {@code m3} -- will be the same
    * object and will correspond to {@code MyInterface.yetAnotherMethod}. The correct
    * usage follows:
    * <pre>
    * i.someMethod();
    * Method m1 = capturer.getMethod();
    * 
    * i.someOtherMethod(null, null);
    * Method m2 = capturer.getMethod();
    * 
    * i.yetAnotherMethod(null);
    * Method m3 = capturer.getMethod();
    * </pre>
    * 
    * @return a proxy that captures method calls
    */
   public E capture() {
      return proxy;
   }
   
   /**
    * Returns the last method captured. If no method has ever been called on the
    * proxy (accessible from {@link #capture()}) then this will return {@code null}.
    * 
    * @return the last method called on the proxy
    */
   public Method getMethod() {
      return captured;
   }
   
   /**
    * Returns the last method captured. The argument is a dummy argument and is
    * meant to provide "syntactic sugar" to enhance readability.
    * 
    * <p>Take the following example:
    * <pre>
    * i.someMethod();
    * Method m = capturer.getMethod();
    * </pre>
    * This could instead be written as follows to enhance readability:
    * <pre>
    * Method m = capturer.getMethod(i.someMethod());
    * </pre>
    *  
    * <p>Note that you will not be able to use this "sugar" for methods
    * that return {@code void} and will have to use the first form
    * shown above.
    * 
    * @param o dummy argument that isn't used but <em>should</em> be an
    *          expression that calls a single method on the proxy interface,
    *          like in the example above
    * @return the last method called on the proxy
    */
   public Method getMethod(@SuppressWarnings("unused") Object o) {
      return getMethod();
   }

   /**
    * Returns the signature of the last method captured. If no method has ever been
    * called on the proxy (accessible from {@link #capture()}) then this will return
    * {@code null}.
    * 
    * @return the signature of the last method called on the proxy
    */
   MethodSignature getSignature() {
      return capturedSig;
   }
   
   /**
    * Returns the last method captured. The argument is a dummy argument and is
    * meant to provide "syntactic sugar" to enhance readability. See
    * {@link #getMethod(Object)} for examples.
    * 
    * @param o dummy argument that isn't used but <em>should</em> be an
    *          expression that calls a single method on the proxy interface
    *          for improved readability
    * @return the last method called on the proxy
    */
   public MethodSignature getSignature(@SuppressWarnings("unused") Object o) {
      return getSignature();
   }
}
