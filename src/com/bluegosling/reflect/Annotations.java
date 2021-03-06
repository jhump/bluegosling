package com.bluegosling.reflect;

import static com.bluegosling.function.Predicates.alwaysAccept;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.reflect.Reflection;

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

   static final Object[] EMPTY = new Object[0];

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
      // (if inherited, no need to do this since Class#getAnnotation will have already done so)
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
      // search so that interfaces that are "closer" to the specified class are preferred (e.g. we
      // prefer an interface implemented directly by the class vs. by a super-class, and we prefer
      // annotations on an interface implemented directly by the class vs. annotations on a
      // super-interface).
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
         Map<String, ?> attributes) {
      if (!annotationType.isAnnotation()) {
         throw new IllegalArgumentException(annotationType + " is not an annotation");
      }
      Method[] methods = annotationType.getDeclaredMethods();
      final Map<String, Object> resolvedAttributes = new HashMap<>(methods.length * 4 / 3);
      Set<String> keys = new HashSet<>(attributes.keySet());
      for (Method m : methods) {
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
                     + m.getGenericReturnType().getTypeName() + " from map value", e);
            }
            resolvedAttributes.put(m.getName(), value);
         }
      }
      if (keys.remove("annotationType")) {
         Object typeFromMap = attributes.get("annotationType"); 
         if (!annotationType.equals(typeFromMap)) {
            throw new IllegalArgumentException("Map contains key \"annotationType\" with invalid"
                  + " value. Expecting " + annotationType + "; found " + typeFromMap);
         }
      }
      if (!keys.isEmpty()) {
         throw new IllegalArgumentException("Map contains invalid keys for "
               + annotationType.getName() + ": " + keys);
      }
      T ret = Reflection.newProxy(annotationType,
            (Object proxy, Method method, Object[] args) -> {
               // ugh, why does this send null instead of empty array when there are no args...
               if (args == null) {
                  args = EMPTY;
               }
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
               // arrays are defensively copied
               return val.getClass().isArray() ? ArrayUtils.clone(val) : val;
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
      } else if (fieldType.isArray()) {
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
      // Using int.class as the value for an annotation method that returns Class<Integer>, or
      // even Class<? extends Number>, is (surprisingly?) legit since int.class is actually a
      // Class<Integer>. So we box the types before checking assignability.
      Type actualTypeArg = Types.box(classValue);
      if (returnType instanceof Class) {
         // not a parameterized type, so any Class value will do
         return classValue;
      } else {
         if (!Types.isAssignableStrict(Types.newParameterizedType(Class.class, actualTypeArg), 
               returnType)) {
            badClassType(returnType, classValue);
         }
         return classValue;
      }      
   }
   
   private static Object getArrayValue(Class<?> arrayType, Type genericArrayType, Object value) {
      if (arrayType.isInstance(value)) {
         return ArrayUtils.clone(value); // defensive copy
      }
      if (value.getClass().isArray()) {
         value = ArrayUtils.clone(value); // defensive copy
         value = ArrayUtils.asList(value); // then wrap in List interface
      }
      
      if (value instanceof List) {
         Class<?> componentType = arrayType.getComponentType();
         Type genericComponentType = Types.getComponentType(genericArrayType);
         assert componentType != null;
         assert genericComponentType != null;
         List<?> list = (List<?>) value;
         Object ret = Array.newInstance(componentType, list.size());
         int index = 0;
         for (Object o : list) {
            Array.set(ret, index++, getFieldValue(componentType, genericComponentType, o));
         }
         return ret;
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
         Map<String, ?> typedMap = (Map<String, ?>) subMap;
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
    * Returns a map of values that correspond to the annotation's methods. If the given annotation
    * has any fields that are nested annotations or arrays of annotations, their values in the
    * returned map will be recursively converted to maps. Arrays will be converted to lists so that
    * creating maps from two equal annotations will result in equal maps (array values in the map
    * prevent this since arrays don't have a sane equals implementation).
    * 
    * @param annotation the annotation
    * @return a map of values where keys are the annotation method names and values are the
    *       values returned by those methods (methods that return other annotations will result in
    *       map values that are sub-maps, constructed from the other annotation using this same
    *       method)
    */
   public static Map<String, Object> toMap(Annotation annotation) {
      return toMap(annotation, alwaysAccept());
   }

   /**
    * Returns a map of values that correspond to the annotation's methods, optionally including the
    * {@link Annotation#annotationType()}. If the given annotation has any fields that are nested
    * annotations or arrays of annotations, their values in the returned map will be recursively
    * converted to maps. Arrays will be converted to lists so that creating maps from two equal
    * annotations will result in equal maps (array values in the map prevent this since arrays don't
    * have a sane equals implementation).
    * 
    * @param annotation the annotation
    * @param includeAnnotationType if true, the map will have a key named "annotationType" whose
    *       value is the class token returned from calling {@code annotation.annotationType()}
    * @return a map of values where keys are the annotation method names and values are the
    *       values returned by those methods (methods that return other annotations will result in
    *       map values that are sub-maps, constructed from the other annotation using this same
    *       method)
    */
   public static Map<String, Object> toMap(Annotation annotation, boolean includeAnnotationType) {
      return toMap(annotation, includeAnnotationType, alwaysAccept());
   }

   /**
    * Returns a map of values that correspond to the annotation's methods that match the specified
    * predicate. If the given annotation has any fields that are nested annotations or arrays of
    * annotations, their values in the returned map will be recursively converted to maps. Arrays
    * will be converted to lists so that creating maps from two equal annotations will result in
    * equal maps (array values in the map prevent this since arrays don't have a sane equals
    * implementation).
    * 
    * @param annotation the annotation
    * @param filterAttributes a predicate, for filtering the annotation's methods
    * @return a map of values where keys are the annotation method names and values are the values
    *       returned by those methods, filtered according to the specified predicate
    */
   public static Map<String, Object> toMap(Annotation annotation,
         Predicate<? super Method> filterAttributes) {
      return toMap(annotation, false, filterAttributes);
   }
   
   /**
    * Returns a map of values that correspond to the annotation's methods that match the specified
    * predicate, optionally including the {@link Annotation#annotationType()}. If the given
    * annotation has any fields that are nested annotations or arrays of annotations, their values
    * in the returned map will be recursively converted to maps. Arrays will be converted to lists
    * so that creating maps from two equal annotations will result in equal maps (array values in
    * the map prevent this since arrays don't have a sane equals implementation).
    * 
    * @param annotation the annotation
    * @param includeAnnotationType if true, the map will have a key named "annotationType" whose
    *       value is the class token returned from calling {@code annotation.annotationType()}
    * @param filterAttributes a predicate, for filtering the annotation's methods
    * @return a map of values where keys are the annotation method names and values are the values
    *       returned by those methods, filtered according to the specified predicate
    */
   public static Map<String, Object> toMap(Annotation annotation, boolean includeAnnotationType,
         Predicate<? super Method> filterAttributes) {
      Map<String, Object> ret = new LinkedHashMap<String, Object>();
      Class<?> annotationType = annotation.annotationType();
      if (includeAnnotationType) {
         ret.put("annotationType", annotationType);
      }
      for (Method m : annotationType.getDeclaredMethods()) {
         if (filterAttributes.test(m)) {
            Object value = getAnnotationFieldValue(m, annotation);
            if (value instanceof Annotation) {
               value = toMap((Annotation) value, includeAnnotationType);
            } else if (value instanceof Annotation[]) {
               Annotation annotationArray[] = (Annotation[]) value;
               ArrayList<Object> list = new ArrayList<>(annotationArray.length);
               for (Annotation a : annotationArray) {
                  list.add(toMap(a, includeAnnotationType));
               }
               value = list;
            } else if (value.getClass().isArray()) {
               value = ArrayUtils.asList(value);
            }
            ret.put(m.getName(), value);
         }
      }
      return Collections.unmodifiableMap(ret);
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
      Class<? extends Annotation> annotationType = annotation.annotationType();
      if (annotationType.isInstance(other)
            && annotationType.equals(((Annotation) other).annotationType())) {
         Annotation otherAnnotation = annotationType.cast(other);
         for (Method annotationField : annotationType.getDeclaredMethods()) {
            if (filterAttributes.test(annotationField)) {
               Object v1 = getAnnotationFieldValue(annotationField, annotation);
               Object v2 = getAnnotationFieldValue(annotationField, otherAnnotation);
               if (v1 != null && v1.getClass().isArray()) {
                  if (!ArrayUtils.equals(v1, v2)) {
                     return false;
                  }
               } else if (v1 == null ? v1 != v2 : !v1.equals(v2)) {
                  return false;
               }
            }
         }
         return true;
      }
      return false;
   }
   
   /**
    * Computes the hash code for an annotation as defined in the contract on {@link Annotation}.
    * This is useful for creating proxies that implement annotation interfaces so as to completely
    * adhere to the annotation spec.
    * 
    * @param annotation the annotation
    * @return the annotation's hash code
    */
   public static <T extends Annotation> int hashCode(T annotation) {
      return hashCode(annotation, alwaysAccept());
   }
   
   /**
    * Computes the hash code for an annotation, only considering attributes that match the specified
    * predicate. This implements the logic defined in the contract for hash code defined on
    * {@link Annotation}, except that it only includes methods that match the specified predicate as
    * part of the computation.
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
            } else if (val.getClass().isArray()) {
               memberCode = ArrayUtils.hashCode(val);
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
      for (Method annotationField : annotationType.getDeclaredMethods()) {
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
               if (val.getClass().isArray()) {
                  sb.append(ArrayUtils.toString(val));
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
   
   static Object getAnnotationFieldValue(Method annotationField, Annotation annotation) {
      try {
         if (!annotationField.isAccessible()) {
            annotationField.setAccessible(true);
         }
         return annotationField.invoke(annotation);
      } catch (Exception e) {
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         }
         throw new RuntimeException(e);
      }
   }   
}
