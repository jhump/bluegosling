package com.apriori.reflect;

import com.apriori.util.Predicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for working with instances of {@link Annotation}. These are mostly helpful in
 * fabricating annotation instances and performing comparisons between two annotations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: finish javadoc
public final class Annotations {
   /**
    * Prevents instantiation.
    */
   private Annotations() {}
   
   /**
    * Creates an annotation instance. Any values that are not specified will return their default
    * value as defined in the annotation interface. The values are specified using the annotation
    * method names as keys. The values in the map are the values to be returned by the annotation
    * method. Annotation methods that return other annotations can be represented in the map by
    * sub-map values (which will, in turn, be used to create the other annotations).
    * 
    * @param annotationType the type of annotation to create
    * @param attributes a map of values that are returned by the annotations methods
    * @return an annotation
    * @throws NullPointerException if either argument is {@code null} or if any of the map values
    *       is {@code null}
    * @throws IllegalArgumentException if the map contains a value of the wrong type (i.e. value's
    *       type does not match the expected return type of the corresponding method), if the map
    *       is missing a required value (a required value is one that has no default value defined
    *       on the annotation), or if the map has unrecognized keys
    */
   public static <T extends Annotation> T create(final Class<T> annotationType,
         Map<String, Object> attributes) {
      final Map<String, Object> resolvedAttributes = new HashMap<String, Object>(attributes);
      Set<String> keys = new HashSet<String>(attributes.keySet());
      for (Method m : annotationType.getDeclaredMethods()) {
         if (!keys.remove(m.getName())) {
            Object value = m.getDefaultValue();
            if (value == null) {
               throw new IllegalArgumentException("No value provided for " + m.getName()
                     + " and method has no default value either");
            }
            resolvedAttributes.put(m.getName(), value);
         } else {
            Object value = attributes.get(m.getName());
            if (value == null) {
               throw new NullPointerException(m.getName() + " should not be null");
            }
            if (!m.getReturnType().isAssignableFrom(value.getClass())) {
               if (Annotation.class.isAssignableFrom(m.getReturnType()) && value instanceof Map) {
                  
                  @SuppressWarnings("unchecked") // we just did assignability check, so this is ok
                  Class<? extends Annotation> returnType = (Class<? extends Annotation>) m.getReturnType();
                  
                  @SuppressWarnings("unchecked") // this could cause ClassCastExceptions if caller
                                                // provides a bad sub-map :(
                  Map<String, Object> subMap = (Map<String, Object>) value;
                  
                  // create annotation from the sub-map
                  resolvedAttributes.put(m.getName(), create(returnType, subMap));
               } else {
                  throw new IllegalArgumentException(m.getName() + " value must be of type "
                        + m.getReturnType().getName());
               }
            }
         }
      }
      if (!keys.isEmpty()) {
         throw new IllegalArgumentException("Map contains invalid keys for "
               + annotationType.getName() + ": " + keys);
      }
      @SuppressWarnings("unchecked") // we know that annotationType is T, so this is safe
      T ret = (T) Proxy.newProxyInstance(annotationType.getClassLoader(),
            new Class<?>[] { annotationType },
            new InvocationHandler() {
               @Override
               public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  Object val = resolvedAttributes.get(method.getName());
                  if (val == null || args.length > 0) {
                     if (args.length == 1 && method.getName().equals("equals")) {
                        return Annotations.equal((Annotation) proxy, args[0]);
                     } else if (args.length == 0 && method.getName().equals("hashCode")) {
                        return Annotations.hashCode((Annotation) proxy);
                     } else if (args.length == 0 && method.getName().equals("toString")) {
                        return Annotations.toString((Annotation) proxy);
                     } else if (args.length == 0 && method.getName().equals("annotationType")) {
                        return annotationType;
                     } else {
                        // WTF?
                        throw new UnsupportedOperationException(method.getName());
                     }
                  }
                  return val;
               }
            });
      return ret;
   }
   
   /**
    * Returns a map of values that correspond to the annotation's methods.
    * 
    * @param annotation the annotation
    * @return a map of values where keys are the annotation method names and values are the
    *       values returned by those methods (methods that return other annotations will result in
    *       map values that are sub-maps, constructed from the other annotation using this same
    *       method)
    */
   public static Map<String, Object> asMap(Annotation annotation) {
      return asMap(annotation, Predicate.ALL);
   }

   public static Map<String, Object> asMap(Annotation annotation,
         Predicate<? super Method> filterAttributes) {
      Map<String, Object> ret = new HashMap<String, Object>();
      for (Method m : annotation.annotationType().getDeclaredMethods()) {
         if (filterAttributes.apply(m)) {
            Object value = getAnnotationFieldValue(m, annotation);
            if (value instanceof Annotation) {
               value = asMap((Annotation) value, filterAttributes);
            }
            ret.put(m.getName(), value);
         }
      }
      return ret;
   }
   
   public static <T extends Annotation> boolean equal(T annotation, Object other) {
      return equal(annotation, other, Predicate.ALL);
   }

   public static <T extends Annotation> boolean equal(T annotation, Object other,
         Predicate<? super Method> filterAttributes) {
      Class<?> annotationType = annotation.annotationType();
      if (annotationType.isInstance(other)) {
         for (Method annotationField : annotationType.getDeclaredMethods()) {
            if (filterAttributes.apply(annotationField)) {
               Object v1 = getAnnotationFieldValue(annotationField, annotation);
               Object v2 = getAnnotationFieldValue(annotationField, other);
               if (v1 instanceof Object[]) {
                  if (!Arrays.deepEquals((Object[]) v1, (Object[]) v2)) {
                     return false;
                  }
               } else {
                  if (v1 == null ? v1 != v2 : !v1.equals(v2)) {
                     return false;
                  }
               }
            }
         }
      }
      return false;
   }
   
   public static <T extends Annotation> int hashCode(T annotation) {
      return hashCode(annotation, Predicate.ALL);
   }
   
   public static <T extends Annotation> int hashCode(T annotation,
         Predicate<? super Method> filterAttributes) {
      int ret = 0;
      for (Method annotationField : annotation.annotationType().getDeclaredMethods()) {
         if (filterAttributes.apply(annotationField)) {
            Object val = getAnnotationFieldValue(annotationField, annotation);
            int memberCode;
            if (val == null) {
               memberCode = 0;
            } else if (val instanceof Object[]) {
               memberCode = Arrays.deepHashCode((Object[]) val);
            } else {
               memberCode = val.hashCode();
            }
            ret += memberCode ^ (127 * annotationField.getName().hashCode());
         }
      }
      return ret;
   }
   
   public static <T extends Annotation> String toString(T annotation) {
      return toString(annotation, Predicate.ALL);
   }
   
   public static <T extends Annotation> String toString(T annotation,
         Predicate<? super Method> filterAttributes) {
      StringBuilder sb = new StringBuilder();
      sb.append("@");
      Class<?> annotationType = annotation.annotationType();
      sb.append(annotationType.getName());
      boolean first = true;
      for (Method annotationField : annotation.annotationType().getDeclaredMethods()) {
         if (filterAttributes.apply(annotationField)) {
            Object val = getAnnotationFieldValue(annotationField, annotation);
            if (val != null) {
               if (first) {
                  sb.append("(");
                  first = false;
               } else {
                  sb.append(", ");
               }
               sb.append(annotationField.getName());
               sb.append(" = ");
               if (val instanceof Object[]) {
                  sb.append(Arrays.deepToString((Object[]) val));
               } else {
                  sb.append(val);
               }
            }
         }
      }
      if (!first) {
         sb.append(")");
      }
      return sb.toString();
   }
   
   private static Object getAnnotationFieldValue(Method annotationField, Object annotation) {
      try {
         return annotationField.invoke(annotation);
      }
      catch (Exception e) {
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         }
         throw new RuntimeException(e);
      }
   }   
}
