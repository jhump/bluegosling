package com.apriori.apt.reflect;

import com.apriori.collections.TransformingList;
import com.apriori.reflect.ProxyUtils;
import com.apriori.util.Function;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Bridges an annotation mirror into an actual annotation interface. The trick is that classes may
 * not be available as actual {@code java.lang.Class} tokens at runtime since they may refer to
 * classes that are not yet compiled. So the bridge provides a way to access these class references
 * as instances of {@link Class} instead.
 * 
 * <p>For example, take the following definition:
 * <pre>
 * {@literal @}interface AnnoXyz {
 *    Class&lt;? extends Closeable&gt;[] arrayOfClasses();
 * }
 * </pre>
 * 
 * <p>And let's say we're processing the following code:
 * <pre>
 * {@literal @}AnnoXyz(SomeClass.class, Writer.class, Reader.class)
 * class SomeClass implements Closeable {
 *    // ...
 * }
 * </pre>
 * 
 * This means that we can access the annotation in an annotation processor like so:
 * <pre>
 * // The bridge is an instance of the AnnoXyz annotation.
 * AnnoXyz annotation = createBridge(
 *       com.apriori.apt.reflect.Class.forName("SomeClass")
 *             .getAnnotation(AnnoXyz.class),
 *       AnnoXyz.class);
 *       
 * // Since classes might not be available, annotation fields that return classes or
 * // arrays of classes will instead return null.
 * annotation.arrayOfClasses() == null; // true! 
 * 
 * // So the bridge to the rescue!
 * com.apriori.apt.reflect.Class classes[] = bridge(annotation.arrayOfClasses());
 * // Yay!
 * classes[0].getName(); // "SomeClass" ftw!
 * </pre>
 * 
 * <p>So use {@link #createBridge(Annotation)} to create an instance of an annotation interface
 * based on a {@link Annotation}. And then use {@link #bridge(Class)} or {@link #bridge(Class[])}
 * to access class tokens returned from that interface. You can optionally use
 * {@link #bridge(Object)} to access other fields returned from the interface but it's not
 * necessary.
 * 
 * <p>Since the annotation method actually returns {@code null}, the {@link #bridge(Class)} method
 * doesn't actually look at the incoming argument. It accepts an argument for syntactic sugar. In
 * reality, the call to this method sets thread-local state to the bridged value which is then
 * accessed via the call to {@link #bridge(Class)}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class AnnotationBridge {
   
   private AnnotationBridge() {}
   
   private static ThreadLocal<Object> lastValue = new ThreadLocal<Object>(); 
   
   private static <T> T getLastValue(java.lang.Class<T> clazz) {
      @SuppressWarnings("unchecked") // we'll check it before we try to return it
      T val = (T) lastValue.get();
      lastValue.set(null);
      if (val == null) {
         throw new IllegalStateException("no annotation method called");
      } else if (!clazz.isInstance(val)) {
         throw new IllegalStateException("last annotation method did not return compatible type");
      }
      return val;
   }

   /**
    * Creates an annotation bridge. The returned annotation acts as would a real runtime annotation
    * that had the same values as the specified {@link Annotation} (which itself comes from an
    * annotation mirror).
    * 
    * <p>They key differences between an actual {@link java.lang.annotation.Annotation
    * java.lang.annotation.Annotation} and the value returned follow:
    * <ul>
    * <li>The returned value does not implement {@code equals(Object)} or {@code hashCode()} per
    * the contract specified by {@link java.lang.annotation.Annotation}. Bridged annotations can
    * safely be compared to one another but not to actual (non-bridge) annotation objects.</li>
    * <li>Methods that return {@code java.lang.Class} or {@code java.lang.Class[]} values will
    * always return {@code null}. Callers must use {@link #bridge(Class)} or {@link #bridge(Class[])}
    * to get to these values, but as {@link Class} objects instead of as {@link java.lang.Class
    * java.lang.Class} tokens.</li>
    * </ul>
    * 
    * @param annotation the annotation to be bridged
    * @param clazz the annotation type
    * @return an annotation bridge
    * @throws IllegalArgumentException if the specified type is not actually an annotation type or
    *       if the specified annotation is not of the specified type
    * @throws NullPointerException if either argument is {@code null}
    */
   public static <T extends java.lang.annotation.Annotation> T createBridge(Annotation annotation,
         java.lang.Class<T> clazz) {
      if (annotation == null || clazz == null) {
         throw new NullPointerException();
      }
      if (!clazz.isAnnotation()) {
         throw new IllegalArgumentException("specified type is not an annotation type");
      }
      try {
         if (!annotation.annotationType().asJavaLangClass(clazz.getClassLoader()).equals(clazz)) {
            throw new IllegalArgumentException("specified annotation is not of specified type");
         }
      } catch (ClassNotFoundException e) {
         // if we couldn't find a java.lang.Class for this annotation, then it obviously wasn't
         // an instance of the clazz
         throw new IllegalArgumentException("specified annotation is not of specified type");
      }
      return createProxy(annotation, clazz);
   }
   
   /**
    * Creates an annotation bridge.
    * 
    * @param annotation the annotation to be bridged
    * @return an annotation bridge
    * @throws ClassNotFoundException if no runtime {@code java.lang.Class} could be loaded for
    *       the type of the specified annotation
    * @throws NullPointerException if the argument is {@code null}
    *       
    * @see #createBridge(Annotation, Class)
    */
   public static java.lang.annotation.Annotation createBridge(Annotation annotation)
         throws ClassNotFoundException {
      if (annotation == null) {
         throw new NullPointerException();
      }
      @SuppressWarnings("unchecked")
      java.lang.Class<? extends java.lang.annotation.Annotation> clazz =
            (java.lang.Class<? extends java.lang.annotation.Annotation>)
               annotation.annotationType().asJavaLangClass();
      return createProxy(annotation, clazz);
   }
   
   /**
    * Creates an annotation bridge. The specified class loader is used in the attempt to load the
    * annotation type.
    * 
    * @param annotation the annotation to be bridged
    * @param classLoader a class loader
    * @return an annotation bridge
    * @throws ClassNotFoundException if no runtime {@code java.lang.Class} could be loaded for
    *       the type of the specified annotation
    * @throws NullPointerException if either argument is {@code null}
    *       
    * @see #createBridge(Annotation, Class)
    */
   public static java.lang.annotation.Annotation createBridge(Annotation annotation,
         ClassLoader classLoader) throws ClassNotFoundException {
      if (annotation == null || classLoader == null) {
         throw new NullPointerException();
      }
      @SuppressWarnings("unchecked")
      java.lang.Class<? extends java.lang.annotation.Annotation> clazz =
            (java.lang.Class<? extends java.lang.annotation.Annotation>)
               annotation.annotationType().asJavaLangClass(classLoader);
      return createProxy(annotation, clazz);
   }
   
   private static <T extends java.lang.annotation.Annotation> T createProxy(Annotation annotation,
         final java.lang.Class<T> clazz) {
      if (!clazz.isAnnotation()) {
         throw new IllegalStateException("annotation has improper type");
      }
      T proxy = ProxyUtils.newProxyInstance(clazz, new AnnotationBridgeHandler(annotation, clazz));
      return proxy;      
   }
   
   /**
    * Bridges an annotation value from {@code java.lang.Class} to {@link Class}.
    * 
    * @param val the value returned from an annotation bridge (which will actually always be
    *       {@code null} and should be supplied as an argument to make the syntax more readable)
    * @return the {@link Class} object
    */
   public static Class bridge(@SuppressWarnings("unused") java.lang.Class<?> val) {
      return getLastValue(Class.class);
   }
   
   /**
    * Bridges an annotation value from {@code java.lang.Class[]} to {@link Class Class[]}.
    * 
    * @param val the value returned from an annotation bridge (which will actually always be
    *       {@code null} and should be supplied as an argument to make the syntax more readable)
    * @return the {@link Class} object
    */
   public static Class[] bridge(@SuppressWarnings("unused") java.lang.Class<?> val[]) {
      return getLastValue(Class[].class);
   }
   
   /**
    * Bridges an annotation value. This is not necessary for values other than class tokens but
    * is provided so code can access methods on the annotation bridge consistently.
    * 
    * @param val the value returned from an annotation bridge
    * @return the supplied value
    */
   public static <T> T bridge(T val) {
      // TODO: don't clear this; instead, compare it to provided value to verify that API is
      // being used correctly
      lastValue.set(null);
      return val;
   }
   
   /**
    * The invocation handler used to implement annotation bridges. This sets thread-local state
    * after each method call that can then be accessed, if necessary (like for methods that return
    * {@code java.lang.Class} or {@code java.lang.Class[]}), via {@link AnnotationBridge#bridge(Class)}
    * and the like. For most methods, the annotation bridge returns the actual value. For those
    * that must be accessed via bridging, {@code null} is actually returned.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AnnotationBridgeHandler implements InvocationHandler {
      private final Annotation annotation;
      private final Map<String, ?> attributes;
      private final java.lang.Class<?> clazz;
      
      public AnnotationBridgeHandler(Annotation annotation, java.lang.Class<?> clazz) {
         this.annotation = annotation;
         this.attributes = annotation.getAnnotationAttributes();
         this.clazz = clazz;
      }
      
      @SuppressWarnings("synthetic-access") // it accesses private ThreadLocal
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         lastValue.set(null); // clear until we see a valid value
         // eerily similar to what's done in com.apriori.reflect.Annotations.create()...
         Object val = attributes.get(method.getName());
         int argsLength = args == null ? 0 : args.length;
         if (val == null || argsLength > 0) {
            if (argsLength == 1 && method.getName().equals("equals")) {
               // Return true if and only if the specified object is also an annotation bridge
               // with the same annotation type and attributes. So you can't use this to compare a
               // real annotation with an annotation bridge.
               @SuppressWarnings("null") // no, it can't be null if argsLength > 0, silly compiler...
               Object o = args[0];
               if (o instanceof Proxy) {
                  InvocationHandler handler = Proxy.getInvocationHandler(o);
                  if (handler instanceof AnnotationBridgeHandler) {
                     return annotation.equals(((AnnotationBridgeHandler) handler).annotation); 
                  }
               }
               return false;
            } else if (argsLength == 0 && method.getName().equals("hashCode")) {
               return annotation.hashCode();
            } else if (argsLength == 0 && method.getName().equals("toString")) {
               return annotation.toString();
            } else if (argsLength == 0 && method.getName().equals("annotationType")) {
               return clazz;
            } else {
               // WTF?
               throw new UnsupportedOperationException(method.getName());
            }
         } else {
            val = convertAnnotationValue(val, method.getReturnType(), method.getName());
            lastValue.set(val);
            if (val instanceof Class || val instanceof Class[]) {
               // class values can't be returned and can only be accessed via bridge() methods
               return null;
            }
            return val;
         }
      }
      
      private Object convertAnnotationValue(Object val, java.lang.Class<?> returnType,
            final String methodName) {
         if (returnType.isInstance(val)
               || (val instanceof Class && java.lang.Class.class.isAssignableFrom(returnType))) {
            // no conversion
            return val;
         } else if (val instanceof Field && Enum.class.isAssignableFrom(returnType)) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            java.lang.Class<? extends Enum> enumType = (java.lang.Class<? extends Enum>) returnType;
            String name = ((Field) val).getName();
            try {
               @SuppressWarnings("unchecked")
               Enum<?> enumVal = Enum.valueOf(enumType, name);
               return enumVal;
            } catch (IllegalArgumentException e) {
               throw new EnumConstantNotPresentException(enumType, name);
            }
         } else if (val instanceof List && returnType.isArray()) {
            final java.lang.Class<?> componentType = returnType.getComponentType();
            @SuppressWarnings("unchecked")
            List<?> vals = new TransformingList<Object, Object>((List<Object>) val, new Function<Object, Object>() {
               @SuppressWarnings("synthetic-access")
               @Override public Object apply(Object input) {
                  return convertAnnotationValue(input, componentType, methodName);
               }
            });
            // java.lang.Class is special since we actually return array of our Class instead
            java.lang.Class<?> arrayElementType;
            if (java.lang.Class.class.isAssignableFrom(componentType)) {
               arrayElementType = Class.class;
            } else {
               arrayElementType = componentType;
            }
            Object ret[] = (Object[]) Array.newInstance(arrayElementType, vals.size());
            int i = 0; 
            for (Object o : vals) {
               ret[i++] = o;
            }
            return ret;
         } else if (val instanceof Annotation && java.lang.annotation.Annotation.class.isAssignableFrom(returnType)) {
            @SuppressWarnings("unchecked") // we just checked in condition above
            Object ret = createBridge((Annotation) val,
                  (java.lang.Class<? extends java.lang.annotation.Annotation>) returnType);
            return ret;
         } else {
            // WTF?
            throw new IllegalStateException("Incompatible value found for " + methodName);
         }
      }
   }
}
