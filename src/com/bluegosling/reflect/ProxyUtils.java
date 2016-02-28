package com.bluegosling.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Utility methods for working with proxies and implementing invocation handlers.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class ProxyUtils {
   /**
    * Prevents instantiation.
    */
   private ProxyUtils() {}

   /**
    * Determines the default value for a given type. For most types this will simply be
    * {@code null}. But for methods that return primitives, we need to choose a suitable primitive
    * value to avoid a {@code NullPointerException} (such as zero or false).
    * 
    * @param clazz the type
    * @return the default value for the given type
    */
   public static Object getDefaultValue(Class<?> clazz) {
      if (!clazz.isPrimitive()) {
         return null;
      }
      else if (clazz == int.class) {
         return 0;
      }
      else if (clazz == short.class) {
         return (short) 0;
      }
      else if (clazz == byte.class) {
         return (byte) 0;
      }
      else if (clazz == char.class) {
         return '\u0000';
      }
      else if (clazz == long.class) {
         return 0L;
      }
      else if (clazz == double.class) {
         return 0.0;
      }
      else if (clazz == float.class) {
         return 0.0F;
      }
      else if (clazz == boolean.class) {
         return false;
      }
      else if (clazz == void.class) {
         return null;
      }
      else {
         // this should never happen...
         throw new IllegalArgumentException(
               "Could not determine default value for "
                     + clazz.getName());
      }
   }

   /**
    * Creates a new proxy instance. This is the same as using
    * {@link Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)} except that it
    * has a generic type parameter to eliminate the need for casting the returned proxy.
    * 
    * @param iface the interface that the new proxy will implement
    * @param handler the invocation handler for the proxy
    * @return the new proxy
    */
   @SuppressWarnings("unchecked") // we know returned value implements T, so this is safe
   public static <T> T newProxyInstance(Class<T> iface, InvocationHandler handler) {
      return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, handler);
   }
   
   /**
    * Casts an object to an interface that it may not necessarily implement. If it does not
    * implement the specified interface, it must have compatible methods -- meaning it must
    * have methods with the same signatures (names and argument lists) as all of the
    * interface's methods. In addition to having the same signature, the object's method
    * must have a return type that is assignment-compatible with the interface's method.
    * 
    * <p>This allows you, for example, to create interfaces with a subset of methods on a
    * class and then be able to cast instances of that class to the new interface -- even
    * for classes that you can't change to actually <em>implement</em> the new interface.
    * 
    * <p>This can be particularly useful with annotation types since they are not allowed
    * to extend one another or extend other interfaces. If you have multiple annotation
    * types that are structurally similar, you can extract that similarity into a normal
    * interface and use this method to cast annotations to the new interface.
    * 
    * <p>The returned object is a {@link Proxy} object that delegates the interface methods
    * to compatible methods that exist on the specified target object.
    * 
    * @param o the target object which is to be cast
    * @param clazz the interface to which the object should be cast
    * @return an instance of the specified interface whose methods are backed by the target
    *       object
    */
   public static <T> T castToInterface(final Object o, Class<T> clazz) {
      return castToInterface(o, clazz, false);
   }
   
   /**
    * Casts an object to interface that it may not necessarily implement. If it does not
    * implement the specified interface, it must have compatible methods -- meaning it must
    * have methods with the same signatures (names and argument lists) as all of the
    * interface's methods. In addition to having the same signature, the object's method
    * must have a return type that is assignment-compatible with the interface's method.
    * 
    * <p>Return type assignment-compatibility is relaxed for "recursive casts". For such
    * casts, if the return type of a method is an interface and the object's method of the
    * same signature returns an incompatible type, the returned value is <em>cast</em> to
    * the expected interface type. So the compatibility check is essentially deferred to
    * runtime. The resulting cast is also recursive (so it too may do subsequent casts on
    * returned values).
    * 
    * @param o the target object which is to be cast
    * @param clazz the interface to which the object should be cast
    * @param recursiveCast if true, return type compatibility is relaxed and returned values
    *       are re-cast at runtime if necessary to conform to interface method
    * @return an instance of the specified interface whose methods are backed by the target
    *       object
    * @throws NullPointerException if any of the specified parameters are {@code null}
    * @throws IllegalArgumentException if the specified {@code Class} is not an interface
    * @throws ClassCastException if there is an issue casting the specified object to the
    *       specified interface, like if it does not have compatible methods
    *       
    * @see #castToInterface(Object, Class)
    */
   public static <T> T castToInterface(final Object o, Class<T> clazz, boolean recursiveCast) {
      Caster.Builder<T> builder = Caster.builder(clazz);
      if (recursiveCast) {
         builder.castingReturnTypes();
      }
      return builder.build().cast(o);
   }
}
