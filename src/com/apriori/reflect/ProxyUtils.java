package com.apriori.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

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
    * Determines a "null" return value to use for a method. For most methods this will simply be
    * {@code null}. But for methods that return primitives, we need to choose a suitable primitive
    * value to avoid a {@code NullPointerException} (such as zero or false).
    * 
    * @param clazz the return type of the method
    * @return the "null" value that method should return
    */
   public static Object getNullReturnValue(Class<?> clazz) {
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
               "Suppressing exceptions could not determine return value for "
                     + clazz.getName());
      }
   }
   
   /**
    * Casts an object to interface that it may not necessarily implement. If it does not
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
    *       are cast at runtime if necessary to conform to interface method
    * @return an instance of the specified interface whose methods are backed by the target
    *       object
    * @throws NullPointerException if any of the specified parameters are {@code null}
    * @throws IllegalArgumentException if the specified {@code Class} is not an interface
    * @throws ClassCastException if there is an issue casting the specified object to the
    *       specified interface, like if it does not have compatible methods
    *       
    * @see #castToInterface(Object, Class)
    */
   @SuppressWarnings("unchecked")
   public static <T> T castToInterface(final Object o, Class<T> clazz, boolean recursiveCast) {
      if (o == null || clazz.isInstance(o)) {
         return (T) o;
      }
      if (!clazz.isInterface()) {
         throw new IllegalArgumentException("Specified class, " + clazz.getName() + ", is not an interface");
      }
      final Map<Method, InvocationHandler> methodMap = new HashMap<Method, InvocationHandler>();
      Class<?> targetClass = o.getClass();
      for (Method m : clazz.getMethods()) {
         boolean hasCompatibleMethod = true;
         try {
            // TODO: consider looking for "compatible" methods, even if signature doesn't match exactly
            // (i.e. a method where parameter types are assignable from those on the interface method)
            final Method targetMethod = targetClass.getMethod(m.getName(), m.getParameterTypes());
            final Class<?> expectedReturnType = m.getReturnType();
            if (expectedReturnType.isAssignableFrom(targetMethod.getReturnType())) {
               methodMap.put(m, new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                     return targetMethod.invoke(o, args);
                  }
               });
            } else if (recursiveCast && expectedReturnType.isInterface()) {
               // delay compatibility check to runtime via another cast
               methodMap.put(m, new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                     return castToInterface(targetMethod.invoke(o, args), expectedReturnType, true);
                  }
               });
            } else {
               hasCompatibleMethod = false;
            }
         } catch (NoSuchMethodException e) {
            hasCompatibleMethod = false;
         }
         if (!hasCompatibleMethod) {
            StringBuilder sb = new StringBuilder();
            sb.append("Specified object, of type ");
            sb.append(targetClass.getName());
            sb.append(", has no method that is compatible with:\n");
            sb.append(m.getReturnType().getName());
            sb.append(" ");
            sb.append(m.getName());
            sb.append("(");
            boolean first = true;
            for (Class<?> argClass : m.getParameterTypes()) {
               if (first) {
                  first = false;
               } else {
                  sb.append(",");
               }
               sb.append(argClass.getName());
            }
            sb.append(")");
            throw new ClassCastException(sb.toString());
         }
      }
      return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
            new InvocationHandler() {
               @Override
               public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  return methodMap.get(method).invoke(proxy, method, args);
               }
            });
   }
}
