package com.apriori.reflect;

import static com.apriori.util.Predicates.alwaysAccept;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility methods for working with instances of {@link Annotation}. These are mostly helpful in
 * fabricating annotation instances and performing comparisons between two annotations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class Annotations {
   /**
    * Prevents instantiation.
    */
   private Annotations() {}
   
   /**
    * Returns an annotation, possibly searching for the annotation on super-types if not declared
    * on the specified type. This will find annotations on super-types even if they aren't
    * {@linkplain Inherited inheritable}.
    * 
    * <p>If the annotation is not found on the specified type then the type's super-classes are
    * searched for the annotation. If the annotation is still not found, then implemented interfaces
    * are searched. Interfaces declared on the specified type are preferred over their
    * super-interfaces. Furthermore, interfaces implemented directly by the specified type (both
    * declared interfaces and their super-interfaces) are preferred over "inherited interfaces",
    * which are those declared on super-types. Finally, interfaces earlier in the declaration order
    * are preferred over those later in the {@code implements} declaration. Similarly,
    * super-interfaces earlier in the declaration order are preferred over ones later in the
    * {@code extends} declaration.
    * 
    * @param clazz the type from which the annotation is queried
    * @param annotationType the annotaition type to find
    * @return the annotation, possibly on a super-type, or {@code null} if not found
    */
   public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
      A annotation = clazz.getAnnotation(annotationType);
      if (annotation != null) {
         return annotation;
      }
      // if not @Inherited, then look at super-type(s)
      if (annotationType.getAnnotation(Inherited.class) == null) {
         for (Class<?> ancestor = clazz.getSuperclass(); ancestor != null;
               ancestor = ancestor.getSuperclass()) {
            annotation = ancestor.getAnnotation(annotationType);
            if (annotation != null) {
               return annotation;
            }
         }
      }
      // Still not found? Now we move on to inspecting implemented interfaces. We do a breadth-first
      // search so that interfaces that are "closer" to the specified class as preferred (e.g. we
      // prefer an interface implemented directly by the class vs. by a super-class, and we prefer
      // an interface declared directly on the class vs. annotations on one a super-interface)
      Set<Class<?>> interfacesChecked = new HashSet<Class<?>>(); 
      ArrayDeque<Class<?>> queue = new ArrayDeque<Class<?>>();
      while (clazz != null) {
         // start with specified class, looking through interfaces
         queue.addAll(Arrays.asList(clazz.getInterfaces()));
         while (!queue.isEmpty()) {
            Class<?> iface = queue.remove();
            if (interfacesChecked.add(iface)) {
               annotation = iface.getAnnotation(annotationType);
               if (annotation != null) {
                  return annotation;
               }
               // look at super-interfaces
               queue.addAll(Arrays.asList(clazz.getInterfaces()));
            }
         }
         // move on to interfaces declared on super-classes
         clazz = clazz.getSuperclass();
      }
      // still not found -- annotation does not exist
      return null;
   }
   
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
    * @throws NullPointerException if either argument is {@code null}
    * @throws IllegalArgumentException if the map contains a value of the wrong type (includes null
    *       values and values whose type does not match the expected return type of the
    *       corresponding method), if the map is missing a required value (a required value is one
    *       that has no default value defined on the annotation), or if the map has unrecognized
    *       keys
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
            Object value;
            try {
               value = getFieldValue(m.getReturnType(), m.getGenericReturnType(),
                     attributes.get(m.getName()));
            } catch (IllegalArgumentException e) {
               throw new IllegalArgumentException("Unable to compute "
                     + m.getGenericReturnType().getTypeName() + " from map value");
            }
            resolvedAttributes.put(m.getName(), value);
         }
      }
      if (!keys.isEmpty()) {
         throw new IllegalArgumentException("Map contains invalid keys for "
               + annotationType.getName() + ": " + keys);
      }
      T ret = ProxyUtils.newProxyInstance(annotationType,
            new InvocationHandler() {
               @Override
               public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  Object val = resolvedAttributes.get(method.getName());
                  if (val == null || args.length > 0) {
                     if (args.length == 1 && method.getName().equals("equals")) {
                        return equal((Annotation) proxy, args[0]);
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
   
   private static Object getFieldValue(Class<?> fieldType, Type genericFieldType, Object value) {
      if (value == null) {
         throw new IllegalArgumentException(
               "Could not convert null to " + genericFieldType.getTypeName());
      }
      if (Class.class.isAssignableFrom(fieldType)) {
         return getClassValue(genericFieldType, value);
      } else if (Object[].class.isAssignableFrom(fieldType)) {
         return getArrayValue(fieldType, genericFieldType, value);
      } else if (Annotation.class.isAssignableFrom(fieldType)) {
         @SuppressWarnings("unchecked") // we just did assignability check, so this is ok
         Class<? extends Annotation> annotationType = (Class<? extends Annotation>) fieldType;
         return getAnnotationValue(annotationType, value);
      } else if (fieldType == byte.class) {
         return getNumericValue(value, byte.class).byteValue();
      } else if (fieldType == short.class) {
         return getNumericValue(value, short.class).shortValue();
      } else if (fieldType == char.class) {
         if (value instanceof Character) {
            return ((Character) value).charValue();
         }
         return (char) getNumericValue(value, char.class).shortValue();
      } else if (fieldType == int.class) {
         return getNumericValue(value, int.class).intValue();
      } else if (fieldType == long.class) {
         return getNumericValue(value, long.class).longValue();
      } else if (fieldType == float.class) {
         return getNumericValue(value, float.class).floatValue();
      } else if (fieldType == double.class) {
         return getNumericValue(value, double.class).doubleValue();
      } else if (fieldType == boolean.class) {
         if (value instanceof Boolean) {
            return value;
         } else {
            return badType(boolean.class, value.getClass());
         }
      } else if (fieldType.isInstance(value)) {
         return value;
      } else {
         return badType(genericFieldType, value.getClass());
      }
   }
   
   private static Class<?> getClassValue(Type returnType, Object value) {
      if (!(value instanceof Class)) {
         return badType(returnType, value.getClass());
      }
      Class<?> classValue = (Class<?>) value;
      // Using int.class as the value for an annotation method that returns List<Integer>, or
      // even List<? extends Number>, is totally legit since int.class is actually a List<Integer>.
      // So we box the types before checking assignability.
      Type actualTypeArg = Types.box(classValue);
      if (returnType instanceof Class) {
         // not a parameterized type, so any Class value will do
         return classValue;
      } else if (returnType instanceof ParameterizedType) {
         ParameterizedType pt = (ParameterizedType) returnType;
         Type args[] = pt.getActualTypeArguments();
         // the given Class value must conform to the given arg
         assert args.length == 1;
         if (!Types.isAssignableFrom(args[0], actualTypeArg)) {
            badClassType(returnType, classValue);
         }
         return classValue;
      } else {
         throw new AssertionError("Class return type should be a Class or a ParameterizedType");
      }      
   }
   
   private static Object[] getArrayValue(Class<?> arrayType, Type genericArrayType, Object value) {
      if (value instanceof Object[]) {
         value = Arrays.asList((Object[]) value);
      }
      
      if (value instanceof List) {
         Class<?> componentType = arrayType.getComponentType();
         Type genericComponentType = Types.getComponentType(genericArrayType);
         assert componentType != null;
         assert genericComponentType != null;
         List<?> list = (List<?>) value;
         Object newArray[] = (Object[]) Array.newInstance(componentType, list.size());
         int index = 0;
         for (Object o : list) {
            newArray[index] = getFieldValue(componentType, genericComponentType, o);
         }
         return newArray;
      } else {
         return badType(genericArrayType, value.getClass());
      }
   }
   
   private static Annotation getAnnotationValue(Class<? extends Annotation> annotationType,
         Object value) {
      if (annotationType.isInstance(value)) {
         return (Annotation) value;
      } else if (value instanceof Map) {
         Map<?, ?> subMap = (Map<?, ?>) value;
         for (Object k : subMap.keySet()) {
            if (!(k instanceof String)) {
               throw new IllegalArgumentException("Cannot convert map to "
                     + annotationType.getTypeName() + " because it has non-string keys");
            }
         }
         @SuppressWarnings("unchecked") // we just checked the keys, so this will be okay
         Map<String, Object> typedMap = (Map<String, Object>) subMap;
         // recursively create annotation field value
         return create(annotationType, typedMap);
      } else {
         return badType(annotationType, value.getClass());
      }
   }
   
   private static Number getNumericValue(Object value, Class<?> numericType) {
      if (value instanceof Number) {
         return (Number) value;
      } else {
         return badType(numericType, value.getClass());
      }
   }

   private static <T> T badClassType(Type expectedType, Class<?> actualType) {
      throw new IllegalArgumentException("Could not convert Class<" + actualType.getTypeName()
            + "> to " + expectedType.getTypeName());
   }
   
   private static <T> T badType(Type expectedType, Class<?> actualType) {
      throw new IllegalArgumentException("Could not convert " + actualType.getTypeName()
            + " to " + expectedType.getTypeName());
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
      return asMap(annotation, alwaysAccept());
   }

   /**
    * Returns a map of values that correspond to the annotation's methods that match the specified
    * predicate.
    * 
    * @param annotation the annotation
    * @param filterAttributes a predicate, for filtering the annotation's methods
    * @return a map of values where keys are the annotation method names and values are the values
    *       returned by those methods, filtered according to the specified predicate
    */
   public static Map<String, Object> asMap(Annotation annotation,
         Predicate<? super Method> filterAttributes) {
      Map<String, Object> ret = new HashMap<String, Object>();
      for (Method m : annotation.annotationType().getDeclaredMethods()) {
         if (filterAttributes.test(m)) {
            Object value = getAnnotationFieldValue(m, annotation);
            if (value instanceof Annotation) {
               value = asMap((Annotation) value, filterAttributes);
            }
            ret.put(m.getName(), value);
         }
      }
      return ret;
   }
   
   /**
    * Determines if two annotation objects are equal according to the contract defined on
    * {@link Annotation}. This is useful for creating proxies that implement annotation interfaces
    * so as to completely adhere to the annotation spec.
    * 
    * @param annotation an annotation
    * @param other another object to which the annotation is compared
    * @return true if the other object has the same annotation type and returns equal values from
    *       the various annotation attributes
    * @see Annotation#equals(Object)
    */
   public static <T extends Annotation> boolean equal(T annotation, Object other) {
      return equal(annotation, other, alwaysAccept());
   }

   /**
    * Determines if two annotation objects are equal, only considering attributes that match the
    * specified predicate. This implements the logic defined in the contract for equals defined on
    * {@link Annotation}, except that it only compares methods that match the specified predicate.
    * This is useful for determining similarity between two annotations where not all annotation
    * attributes are important.
    * 
    * @param annotation an annotation
    * @param other another object to which the annotation is compared
    * @param filterAttributes a predicate, for filtering the annotation's methods
    * @return true if the other object has the same annotation type and returns equal values from
    *       the annotation attributes that match the predicate
    */
   public static <T extends Annotation> boolean equal(T annotation, Object other,
         Predicate<? super Method> filterAttributes) {
      Class<?> annotationType = annotation.annotationType();
      if (annotationType.isInstance(other)
            && annotationType.equals(((Annotation) other).annotationType())) {
         for (Method annotationField : annotationType.getDeclaredMethods()) {
            if (filterAttributes.test(annotationField)) {
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
   
   /**
    * Computes the hash code for an annotation as defined in the contract on {@link Annotation}.
    * This is useful for creating proxies that implement annotation interfaces so as to completely
    * adhere to the annotation spec. This implements the logic defined in the contract for hash code
    * defined on {@link Annotation}, except that it only uses methods that match the specified
    * predicate as part of the computation.
    * 
    * @param annotation the annotation
    * @return the annotation's hash code
    */
   public static <T extends Annotation> int hashCode(T annotation) {
      return hashCode(annotation, alwaysAccept());
   }
   
   /**
    * Computes the has code for an annotation, only considering attributes that match the specified
    * predicate.
    * 
    * @param annotation an annotation
    * @param filterAttributes a predicate, for filtering the annotation's methods
    * @return the annotation's hash code, computed based only on attributes that match the predicate
    */
   public static <T extends Annotation> int hashCode(T annotation,
         Predicate<? super Method> filterAttributes) {
      int ret = 0;
      for (Method annotationField : annotation.annotationType().getDeclaredMethods()) {
         if (filterAttributes.test(annotationField)) {
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
   
   /**
    * Returns a string representation of the specified annotation object. The string will be in the
    * form:<pre>
    * {@literal @}package.Class(attribute1 = attribute1 value, attribute2 = attribute2 value)
    * </pre>
    * The values come from the string representation of annotation attribute values.
    * 
    * @param annotation the annotation
    * @return a string representation of the annotation
    */
   public static <T extends Annotation> String toString(T annotation) {
      return toString(annotation, alwaysAccept());
   }
   
   /**
    * Returns a string representation of the annotation, with only the attributes that match the
    * specified predicate. The string representation is in the same form as
    * {@link #toString(Annotation)} but it only includes annotation attributes that match the
    * specified predicate.
    * 
    * @param annotation the annotation
    * @param filterAttributes a predicate, for filtering the annotation's methods
    * @return a string representation of the annotation with only the attributes that match the
    *       predicate
    */
   public static <T extends Annotation> String toString(T annotation,
         Predicate<? super Method> filterAttributes) {
      StringBuilder sb = new StringBuilder();
      sb.append("@");
      Class<?> annotationType = annotation.annotationType();
      sb.append(annotationType.getName());
      boolean first = true;
      for (Method annotationField : annotation.annotationType().getDeclaredMethods()) {
         if (filterAttributes.test(annotationField)) {
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
         annotationField.setAccessible(true);
         return annotationField.invoke(annotation);
      } catch (Exception e) {
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         }
         throw new RuntimeException(e);
      }
   }   
}
