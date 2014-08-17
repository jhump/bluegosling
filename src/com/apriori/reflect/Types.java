package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Numerous utility methods for using, constructing, and inspecting generic types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Type
 */
public final class Types {
   
   static final Type EMPTY_TYPES[] = new Type[0];
   static final TypeVariable<?> EMPTY_TYPE_VARIABLES[] = new TypeVariable<?>[0];
   static final Class<?> ARRAY_INTERFACES[] = new Class<?>[] { Cloneable.class, Serializable.class };
   static final Annotation EMPTY_ANNOTATIONS[] = new Annotation[0];
   
   private Types() {}

   /**
    * Finds the raw class token that best represents to the given type. If the type is a class then
    * it is returned. If it is a parameterized type, the parameterized type's raw type is returned.
    * If it is a generic array type, an class token representing an array of the component type is
    * returned. Finally, if it is either a wildcard type or type variable, its first upper bound is
    * returned. 
    *
    * @param type a generic type
    * @return a raw class token that best represents the given generic type
    */
   public static Class<?> getRawType(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return (Class<?>) type;
      } else if (type instanceof ParameterizedType) {
         return getRawType(((ParameterizedType) type).getRawType());
      } else if (type instanceof GenericArrayType) {
         return getArrayType(getRawType(((GenericArrayType) type).getGenericComponentType()));
      } else if (type instanceof TypeVariable) {
         Type bounds[] = ((TypeVariable<?>) type).getBounds();
         return bounds.length > 0 ? getRawType(bounds[0]) : Object.class;
      } else if (type instanceof WildcardType) {
         Type bounds[] = ((WildcardType) type).getUpperBounds();
         return bounds.length > 0 ? getRawType(bounds[0]) : Object.class;
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   /**
    * Returns true if the given type represents an array type.
    *
    * @param type a generic type
    * @return true if the given type represents an array type
    */
   public static boolean isArray(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).isArray();
      } else if (type instanceof GenericArrayType) {
         return true;
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         return bounds.length > 0 && isArray(bounds[0]);
      }
      return false;
   }
   
   /**
    * Returns the component type of the given array type. If the given type does not represent an
    * array type then {@code null} is returned.
    *
    * @param type a generic type
    * @return the component type of given array type or {@code null} if the given type does not
    *       represent an array type
    */
   public static Type getComponentType(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).getComponentType();
      } else if (type instanceof GenericArrayType) {
         return ((GenericArrayType) type).getGenericComponentType();
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         if (bounds.length > 0) {
            Type componentType = getComponentType(bounds[0]);
            // We synthesize a new wildcard type. So a wildcard type <? extends Number[]> will
            // return a component type of <? extends Number> instead of simply Number. Similarly, a
            // type variable <T extends Number[]> returns a component type of <? extends Number>.
            return componentType != null ? newExtendsWildcardType(componentType) : null;
         }
      }
      // type is not an array type
      return null;
   }
   
   /**
    * Determines if the given type is an interface type.
    *
    * @param type a generic type
    * @return true if the given type is an interface type; false otherwise
    */
   public static boolean isInterface(Type type) {
      return getRawType(type).isInterface();
   }
   
   /**
    * Determines if the given type is an enum type.
    *
    * @param type a generic type
    * @return true if the given type is an enum type; false otherwise
    */
   public static boolean isEnum(Type type) {
      return getRawType(type).isEnum();
   }
   
   /**
    * Determines if the given type is an annotation type.
    *
    * @param type a generic type
    * @return true if the given type is an annotation type; false otherwise
    */
   public static boolean isAnnotation(Type type) {
      return getRawType(type).isAnnotation();
   }
   
   /**
    * Determines if the given type is one of the eight primitive types or {@code void}.
    *
    * @param type a generic type
    * @return true if the given type is primitive; false otherwise
    */
   public static boolean isPrimitive(Type type) {
      return requireNonNull(type) instanceof Class && ((Class<?>) type).isPrimitive();
   }
   
   private static final Map<Class<?>, Class<?>> BOX;
   private static final Map<Class<?>, Class<?>> UNBOX;
   static {
      Map<Class<?>, Class<?>> box = new HashMap<>();
      Map<Class<?>, Class<?>> unbox = new HashMap<>();
      box.put(boolean.class, Boolean.class);
      unbox.put(Boolean.class, boolean.class);
      box.put(byte.class, Byte.class);
      unbox.put(Byte.class, byte.class);
      box.put(char.class, Character.class);
      unbox.put(Character.class, char.class);
      box.put(short.class, Short.class);
      unbox.put(Short.class, short.class);
      box.put(int.class, Integer.class);
      unbox.put(Integer.class, int.class);
      box.put(long.class, Long.class);
      unbox.put(Long.class, long.class);
      box.put(float.class, Float.class);
      unbox.put(Float.class, float.class);
      box.put(double.class, Double.class);
      unbox.put(Double.class, double.class);
      box.put(void.class, Void.class);
      unbox.put(Void.class, void.class);
      BOX = Collections.unmodifiableMap(box);
      UNBOX = Collections.unmodifiableMap(unbox);
   }
   
   /**
    * Boxes the given class if it is a primitive type. For example, if the given class is
    * {@code byte} then this returns the wrapper type {@link Byte}. If the given class is not a
    * primitive type then it is returned, unchanged.
    *
    * @param clazz a class
    * @return true the given class if it is not primitive or the corresponding wrapper type if it is
    */
   public static Class<?> box(Class<?> clazz) {
      Class<?> boxed = BOX.get(requireNonNull(clazz));
      return boxed == null ? clazz : boxed;
   }

   /**
    * Boxes the given type if it is a primitive type. This is just like {@link #box(Class)} except
    * that it accepts any generic type. If the given type is not a raw class then it is returned
    * unchanged
    *
    * @param type a generic type
    * @return true the given type if it is not primitive or the corresponding wrapper type if it is
    */
   public static Type box(Type type) {
      Class<?> boxed = BOX.get(requireNonNull(type));
      return boxed == null ? type : boxed;
   }
   
   /**
    * Unboxes the given class if it is a wrapper for a primitive type. For example, if the given
    * class is {@link Byte} then this returns the primitive type {@link byte}. If the given class is
    * not a wrapper type then it is returned, unchanged.
    *
    * @param clazz a class
    * @return true the given class if it is not a wrapper type or the corresponding primitive type
    *       if it is
    */
   public static Class<?> unbox(Class<?> clazz) {
      Class<?> unboxed = UNBOX.get(requireNonNull(clazz));
      return unboxed == null ? clazz : unboxed;
   }

   /**
    * Unboxes the given type if it is a primitive type. This is just like {@link #unbox(Class)}
    * except that it accepts any generic type. If the given type is not a raw class then it is
    * returned unchanged
    *
    * @param type a generic type
    * @return true the given type if it is not a wrapper type or the corresponding primitive type
    *       if it is
    */
   public static Type unbox(Type type) {
      Class<?> unboxed = UNBOX.get(requireNonNull(type));
      return unboxed == null ? type : unboxed;
   }
   
   /**
    * Returns the superclass of the given type. If the given type is one of the eight primitive
    * types or {@code void}, if it is {@code Object}, or if it is an interface then {@code null} is
    * returned. If the given type is an array type then {@code Object} is the returned superclass.
    * 
    * <p>If the given type is a wildcard or type variable (and is not an interface) then its first
    * upper bound is its superclass.
    *
    * @param type a generic type
    * @return the superclass of the given type
    * 
    * @see Class#getSuperclass()
    * @see #getGenericSuperclass(Type)
    */
   public static Class<?> getSuperclass(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).getSuperclass();
      } else if (type instanceof ParameterizedType) {
         return Types.getRawType(((ParameterizedType) type).getRawType()).getSuperclass();
      } else if (type instanceof GenericArrayType) {
         return Object.class;
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         assert bounds.length > 0;
         Class<?> superclass = getRawType(bounds[0]);
         return superclass.isInterface() ? null : superclass;
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   /**
    * Returns the interfaces implemented by the given type. If the given type is an interface then
    * the interfaces it directly extends are returned. If the given type is an array then an array
    * containing {@code Serializable} and {@code Cloneable} is returned. If the given type is a
    * class that does not directly implement any interfaces (including primitive types) then an
    * empty array is returned.
    * 
    * <p>If the given type is a wildcard or type variable, then this returns an array containing any
    * upper bounds that are interfaces.
    *
    * @param type a generic type
    * @return the interfaces directly implemented by the given type
    * 
    * @see Class#getInterfaces()
    * @see #getGenericInterfaces(Type)
    */
   public static Class<?>[] getInterfaces(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).getInterfaces();
      } else if (type instanceof ParameterizedType) {
         return Types.getRawType(((ParameterizedType) type).getRawType()).getInterfaces();
      } else if (type instanceof GenericArrayType) {
         return ARRAY_INTERFACES;
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         assert bounds.length > 0;
         List<Class<?>> interfaceBounds = Arrays.stream(bounds)
               .map(Types::getRawType).filter(Class::isInterface).collect(Collectors.toList());
         return interfaceBounds.toArray(new Class<?>[interfaceBounds.size()]);
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   /**
    * Returns the generic superclass of the given type. If the given type is one of the eight
    * primitive types or {@code void}, if it is {@code Object}, or if it is an interface then
    * {@code null} is returned. If the given type is an array type then {@code Object} is the
    * returned superclass.
    * 
    * <p>If the given type is a wildcard or type variable (and is not an interface) then its first
    * upper bound is its superclass.
    * 
    * <p>This differs from {@link #getSuperclass(Type)} in that it can return a non-raw type. For
    * example if a wildcard's bound is a parameterized type or if a class extends a parameterized
    * type (e.g. {@code class MyClass extends ArrayList<String>}) then a parameterized type is
    * returned. 
    *
    * @param type a generic type
    * @return the superclass of the given type
    * 
    * @see Class#getGenericSuperclass()
    */
   public static Type getGenericSuperclass(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).getGenericSuperclass();
      } else if (type instanceof ParameterizedType) {
         Class<?> superClass = getRawType(((ParameterizedType) type).getRawType()).getSuperclass();
         if (superClass == null) {
            return null;
         }
         Type superType = resolveSuperType(type, superClass);
         assert superType != null;
         return superType;
      } else if (type instanceof GenericArrayType) {
         return Object.class;
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         assert bounds.length > 0;
         return isInterface(bounds[0]) ? null : bounds[0];
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }
   
   /**
    * Returns the generic interfaces implemented by the given type. If the given type is an
    * interface then the interfaces it directly extends are returned. If the given type is an array
    * then an array containing {@code Serializable} and {@code Cloneable} is returned. If the given
    * type is a class that does not directly implement any interfaces (including primitive types)
    * then an empty array is returned.
    * 
    * <p>If the given type is a wildcard or type variable, then this returns an array containing any
    * upper bounds that are interfaces.
    * 
    * <p>This differs from {@link #getSuperclass(Type)} in that it can return a non-raw type. For
    * example if a wildcard type has an interface bound that is a parameterized type or if a class
    * implements a parameterized type (e.g. {@code class MyClass implements List<String>}) then a
    * parameterized type is returned. 
    *
    * @param type a generic type
    * @return the interfaces directly implemented by the given type
    * 
    * @see Class#getGenericInterfaces()
    */
   public static Type[] getGenericInterfaces(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).getGenericInterfaces();
      } else if (type instanceof ParameterizedType) {
         Class<?> interfaces[] =
               Types.getRawType(((ParameterizedType) type).getRawType()).getInterfaces();
         if (interfaces.length == 0) {
            return interfaces;
         }
         int len = interfaces.length;
         Type genericInterfaces[] = new Type[len];
         for (int i = 0; i < len; i++) {
            genericInterfaces[i] = resolveSuperType(type, interfaces[i]);
            assert genericInterfaces[i] != null;
         }
         return genericInterfaces;
      } else if (type instanceof GenericArrayType) {
         return ARRAY_INTERFACES;
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         assert bounds.length > 0;
         List<Type> interfaceBounds =
               Arrays.stream(bounds).filter(Types::isInterface).collect(Collectors.toList());
         return interfaceBounds.toArray(new Type[interfaceBounds.size()]);
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   /**
    * Returns the owner of the given type. The owner is the type's declaring class. If the given
    * type is a top-level type then the owner is {@code null}. Array types, wildcard types, and
    * type variables do not have owners, though their component types / bounds might. So this method
    * return {@code null} if given such a type.
    * 
    * <p>For non-static inner classes, the owner could be a parameterized type. In other cases, the
    * owner type will be a raw type (e.g. a {@code Class} token)
    *
    * @param type the generic type
    * @return the owner of the given type or {@code null} if it has no owner
    * 
    * @see Class#getDeclaringClass()
    * @see ParameterizedType#getOwnerType()
    */
   public static Type getOwnerType(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return ((Class<?>) type).getDeclaringClass();
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) type; 
         Type ownerType = parameterizedType.getOwnerType();
         return ownerType != null
               ? ownerType : getRawType(parameterizedType.getRawType()).getDeclaringClass();
      }
      return null;
   }
   
   /**
    * Finds the given annotation on the given type. This finds annotations on the class that
    * corresponds to the given generic type. If the annotation is {@linkplain Inherited inherited}
    * and not present on the given type then it may come from the type's superclass (if it has one).
    * If the annotation cannot be found then {@code null} is returned.
    * 
    * <p>If the given type is a wildcard type or a type variable then the annotation must be
    * inherited from the type's superclass (if it has one). Otherwise, {@code null} will be
    * returned. 
    *
    * @param type the generic type
    * @param annotationType the annotation type
    * @return the instance of the given annotation that is defined on or inherited by the given
    *       type, or {@code null} if no such annotation exists
    * 
    * @see Class#getAnnotation(Class)
    */
   public static <A extends Annotation> A getAnnotation(Type type, Class<A> annotationType) {
      requireNonNull(type); 
      requireNonNull(annotationType);
      if (type instanceof Class) {
         return ((Class<?>) type).getAnnotation(annotationType);
      } else if (type instanceof ParameterizedType) {
         Class<?> annotatedClass = getRawType(((ParameterizedType) type).getOwnerType());
         return annotatedClass.getAnnotation(annotationType);
      } else if (type instanceof GenericArrayType) {
         return Object[].class.getAnnotation(annotationType);
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         // we must get annotations from the superclass, so it only works if given type is inherited
         Type superclass = getGenericSuperclass(type);
         return superclass != null && annotationType.getAnnotation(Inherited.class) != null
               ? getAnnotation(superclass, annotationType)
               : null;
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }
   
   /**
    * Finds all annotations on the given type. This finds annotations on the class that corresponds
    * to the given generic type. For annotations that are {@linkplain Inherited inherited} and not
    * present on the given type, they could instead come from the type's superclass (if it has one).
    * If no annotations can be found then an empty array is returned.
    * 
    * <p>If the given type is a wildcard type or a type variable then the annotations must be
    * inherited from the type's superclass (if it has one). Otherwise, an empty array will be
    * returned. 
    *
    * @param type the generic type
    * @return annotations that are defined or inherited by the given type 
    * 
    * @see Class#getAnnotations()
    */
   public static Annotation[] getAnnotations(Type type) {
      requireNonNull(type); 
      if (type instanceof Class) {
         return ((Class<?>) type).getAnnotations();
      } else if (type instanceof ParameterizedType) {
         Class<?> annotatedClass = getRawType(((ParameterizedType) type).getOwnerType());
         return annotatedClass.getAnnotations();
      } else if (type instanceof GenericArrayType) {
         return Object[].class.getAnnotations();
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         // we must get annotations from the superclass, so only include inherited annotations
         Annotation annotations[] = getAnnotations(getGenericSuperclass(type));
         List<Annotation> inheritedOnly = Arrays.stream(annotations)
               .filter(a -> a.annotationType().getAnnotation(Inherited.class) != null)
               .collect(Collectors.toList());
         if (annotations.length == inheritedOnly.size()) {
            return annotations;
         } else if (inheritedOnly.isEmpty()) {
            return EMPTY_ANNOTATIONS;
         }
         return inheritedOnly.toArray(new Annotation[inheritedOnly.size()]);
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   /**
    * Finds all annotations declared on the given type. This finds annotations on the class that
    * corresponds to the given generic type. Annotations inherited from the type's superclass are
    * not included. If no annotations can be found then an empty array is returned.
    * 
    * <p>If the given type is a wildcard type or a type variable then an empty array is returned.
    *
    * @param type the generic type
    * @return annotations that are directly defined on the given type 
    * 
    * @see Class#getDeclaredAnnotations()
    */
   public static Annotation[] getDeclaredAnnotations(Type type) {
      requireNonNull(type); 
      if (type instanceof Class) {
         return ((Class<?>) type).getDeclaredAnnotations();
      } else if (type instanceof ParameterizedType) {
         Class<?> annotatedClass = getRawType(((ParameterizedType) type).getOwnerType());
         return annotatedClass.getDeclaredAnnotations();
      } else if (type instanceof GenericArrayType) {
         return Object[].class.getDeclaredAnnotations();
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         return EMPTY_ANNOTATIONS;
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   /**
    * Returns true if both of the given types refer to the same type or are equivalent.
    *
    * @param a a type
    * @param b another type
    * @return true if the two given types are equals; false otherwise
    */
   public static boolean equals(Type a, Type b) {
      if (requireNonNull(a) == requireNonNull(b)) {
         return true;
      } else if (a instanceof Class) {
         return b instanceof Class && a.equals(b);
      } else if (a instanceof ParameterizedType) {
         if (!(b instanceof ParameterizedType)) {
            return false;
         }
         ParameterizedType ptA = (ParameterizedType) a;
         ParameterizedType ptB = (ParameterizedType) b;
         Type ownerA = ptA.getOwnerType();
         Type ownerB = ptB.getOwnerType();
         if ((ownerA == null) != (ownerB == null)) {
            return false;
         }
         if ((ownerA != null && !equals(ownerA, ownerB))
               || !equals(ptA.getRawType(), ptB.getRawType())) {
            return false;
         }
         Type argsA[] = ptA.getActualTypeArguments();
         Type argsB[] = ptB.getActualTypeArguments();
         int len = argsA.length;
         if (len != argsB.length) {
            return false;
         }
         for (int i = 0; i < len; i++) {
            if (!equals(argsA[i], argsB[i])) {
               return false;
            }
         }
         return true;
      } else if (a instanceof GenericArrayType) {
         if (!(b instanceof GenericArrayType)) {
            return false;
         }
         GenericArrayType gatA = (GenericArrayType) a;
         GenericArrayType gatB = (GenericArrayType) b;
         return equals(gatA.getGenericComponentType(), gatB.getGenericComponentType());
      } else if (a instanceof TypeVariable) {
         if (!(b instanceof TypeVariable)) {
            return false;
         }
         TypeVariable<?> tvA = (TypeVariable<?>) a;
         TypeVariable<?> tvB = (TypeVariable<?>) b;
         // if we know these refer to the same variable on the same declaration, then we don't
         // need to also check that the type bounds match
         return tvA.getGenericDeclaration().equals(tvB.getGenericDeclaration())
               && tvA.getName().equals(tvB.getName());
      } else if (a instanceof WildcardType) {
         if (!(b instanceof WildcardType)) {
            return false;
         }
         WildcardType wtA = (WildcardType) a;
         WildcardType wtB = (WildcardType) b;
         // check that lower bounds match
         Type boundsA[] = wtA.getLowerBounds();
         Type boundsB[] = wtB.getLowerBounds();
         int len = boundsA.length;
         if (len != boundsB.length) {
            return false;
         }
         for (int i = 0; i < len; i++) {
            if (!equals(boundsA[i], boundsB[i])) {
               return false;
            }
         }
         // then check that upper bounds match
         boundsA = wtA.getUpperBounds();
         boundsB = wtB.getUpperBounds();
         len = boundsA.length;
         if (len != boundsB.length) {
            return false;
         }
         for (int i = 0; i < len; i++) {
            if (!equals(boundsA[i], boundsB[i])) {
               return false;
            }
         }
         return true;
      } else {
         // WTF?
         return a.equals(b);
      }
   }
   
   /**
    * Computes a hash code for the given generic type. The generic type interfaces do not document
    * {@code equals(Object)} or {@code hashCode()} definitions. So this method computes a stable
    * hash, regardless of the underlying type implementation.
    *
    * @param type a generic type
    * @return a hash code for the given type
    */
   static final int hashCode(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         return type.hashCode();
      } else if (type instanceof ParameterizedType) {
         ParameterizedType pt = (ParameterizedType) type;
         Type owner = pt.getOwnerType();
         Type raw = pt.getRawType();
         return hashCode(pt.getActualTypeArguments())
               ^ (owner == null ? 0 : hashCode(owner))
               ^ (raw == null ? 0 : hashCode(raw));
      } else if (type instanceof GenericArrayType) {
         GenericArrayType gat = (GenericArrayType) type;
         return hashCode(gat.getGenericComponentType());
      } else if (type instanceof TypeVariable) {
         TypeVariable<?> tv = (TypeVariable<?>) type;
         // if we know these refer to the same variable on the same declaration, then we don't
         // need to also include type bounds in the hash
         return tv.getGenericDeclaration().hashCode() ^ tv.getName().hashCode();
      } else if (type instanceof WildcardType) {
         WildcardType wt = (WildcardType) type;
         return hashCode(wt.getLowerBounds()) ^ hashCode(wt.getUpperBounds());
      } else {
         // WTF?
         return type.hashCode();
      }
   }
   
   private static int hashCode(Type types[]) {
      int result = 1;
      for (Type type : types) {
         result = 31 * result + (type == null ? 0 : hashCode(type));
      }
      return result;
   }

   
   /**
    * Constructs a string representation of the given type. Since the generic type interfaces do not
    * document a {@code toString()} definition, this method can be used to construct a suitable
    * string representation, regardless of the underlying type implementation.
    *
    * @param type a generic type
    * @return a string representation of the given type
    */
   static String toString(Type type) {
      requireNonNull(type);
      StringBuilder sb = new StringBuilder();
      toStringBuilder(type, sb);
      return sb.toString();
   }
   
   private static void toStringBuilder(Type type, StringBuilder sb) {
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>) type;
         // prefer canonical name, fall back to basic name if no canonical name available
         String name = clazz.getCanonicalName();
         if (name != null) {
            sb.append(name);
         } else if (clazz.isArray()) {
            toStringBuilder(clazz.getComponentType(), sb);
            sb.append("[]");
         } else {
            sb.append(clazz.getName());
         }
      } else if (type instanceof ParameterizedType) {
         ParameterizedType pt = (ParameterizedType) type;
         Type owner = pt.getOwnerType();
         if (owner == null) {
            toStringBuilder(pt.getRawType(), sb);
         } else {
            toStringBuilder(owner, sb);
            sb.append(".");
            Class<?> rawType = getRawType(pt.getRawType());
            String simpleName = rawType.getSimpleName();
            if (simpleName.isEmpty()) {
               // Anonymous class? This shouldn't really be possible: the Java language doesn't
               // allow parameterized anonymous classes. But just in case: use the class name suffix
               // (e.g. "$1") as its simple name
               Class<?> enclosing = rawType.getEnclosingClass();
               assert rawType.getName().startsWith(enclosing.getName());
               simpleName = rawType.getName().substring(enclosing.getName().length());
               assert !simpleName.isEmpty();
            }
            sb.append(simpleName);
         }
         Type args[] = pt.getActualTypeArguments();
         if (args.length > 0) {
            sb.append("<");
            boolean first = true;
            for (Type arg : args) {
               if (first) {
                  first = false;
               } else {
                  sb.append(",");
               }
               toStringBuilder(arg, sb);
            }
            sb.append(">");
         }
      } else if (type instanceof GenericArrayType) {
         toStringBuilder(((GenericArrayType) type).getGenericComponentType(), sb);
         sb.append("[]");
      } else if (type instanceof TypeVariable) {
         sb.append(((TypeVariable<?>) type).getName());
      } else if (type instanceof WildcardType) {
         WildcardType wc = (WildcardType) type;
         Type bounds[] = wc.getLowerBounds();
         if (bounds.length > 0) {
            sb.append("? super ");
         } else {
            bounds = wc.getUpperBounds();
            if (bounds.length == 1 && bounds[0] == Object.class) {
               sb.append("?");
               return;
            }
            sb.append("? extends ");
         }
         boolean first = true;
         for (Type bound : bounds) {
            if (first) {
               first = false;
            } else {
               sb.append("&");
            }
            toStringBuilder(bound, sb);
         }
      } else {
         // WTF?
         sb.append(type.getTypeName());
      }
   }
   
   /**
    * Determines if one type is assignable to another. This is true when the RHS is a sub-type
    * of the LHS (co-variance). This is effectively the same as {@link Class#isAssignableFrom(Class)
    * from.isAssignableFrom(to)}, but supports generic types instead of only raw types.
    * 
    * <p>For an assignment that would require an unchecked cast, this function returns false, as in
    * this example:
    * <pre>
    * Type genericType = new TypeRef&lt;List&lt;String&gt;&gt;() {};
    * Type rawType = List.class;
    * 
    * // This returns true, no unchecked cast:
    * Types.isAssignableFrom(rawType, genericType);
    * 
    * // This returns false as it requires unchecked cast:
    * Types.isAssignableFrom(genericType, rawType);
    * </pre>
    * 
    * <p>Similarly, even though the Java language allows an assignment of an {@code int} value to a
    * {@code long} variable, this function returns false in this case since {@code int} is not a
    * sub-type of {@code long}. In other words {@code Types.isAssignableFrom(long.class, int.class)}
    * returns false.
    *
    * @param to the LHS of assignment
    * @param from the RHS of assignment
    * @return true if the assignment is allowed
    */
   public static boolean isAssignable(Type to, Type from) {
      if (requireNonNull(to) == requireNonNull(from)) {
         return true;
      } else if (to instanceof Class && from instanceof Class) {
         return ((Class<?>) to).isAssignableFrom((Class<?>) from);
      } else if (to == Object.class) {
         return true;
      } else if (from instanceof WildcardType || from instanceof TypeVariable) {
         Type bounds[] = from instanceof WildcardType
               ? ((WildcardType) from).getUpperBounds()
               : ((TypeVariable<?>) from).getBounds();
         for (Type bound : bounds) {
            if (isAssignable(to, bound)) {
               return true;
            }
         }
         return false;
      } else if (to instanceof Class) {
         Class<?> toClass = (Class<?>) to;
         if (from instanceof GenericArrayType) {
            GenericArrayType fromArrayType = (GenericArrayType) from;
            return toClass.isArray() && isAssignable(toClass.getComponentType(),
                  fromArrayType.getGenericComponentType());
         } else if (from instanceof ParameterizedType) {
            Class<?> fromRaw = (Class<?>) ((ParameterizedType) from).getRawType();
            return toClass.isAssignableFrom(fromRaw);
         }
      } else if (to instanceof ParameterizedType) {
         ParameterizedType toParamType = (ParameterizedType) to;
         Class<?> toRawType = (Class<?>) toParamType.getRawType();
         if (from instanceof Class) {
            Class<?> fromClass = (Class<?>) from;
            if (fromClass.getTypeParameters().length > 0) {
               // Both types are generic, but RHS has no type arguments (e.g. raw). This requires
               // an unchecked cast, so no go
               return false;
            }
            if (!((Class<?>) toRawType).isAssignableFrom(fromClass)) {
               return false;
            }
         } else if (from instanceof ParameterizedType) {
            ParameterizedType fromParamType = (ParameterizedType) from;
            Class<?> fromRawType = (Class<?>) fromParamType.getRawType();
            if (!((Class<?>) toRawType).isAssignableFrom(fromRawType)) {
               return false;
            }
         } else {
            // We handle "from" being a WildcardType or TypeVariable above. If it's
            // a GenericArrayType (only remaining option), return false since arrays
            // cannot be parameterized (only their component types can be).
            return false;
         }
         Type resolvedToType = resolveSuperType(from, toRawType);
         Type args[] = toParamType.getActualTypeArguments();
         Type resolvedArgs[] = getActualTypeArguments(resolvedToType);
         assert args.length == resolvedArgs.length;
         // check each type argument
         for (int i = 0, len = args.length; i < len; i++) {
            Type toArg = args[i];
            Type fromArg = resolvedArgs[i];
            if (toArg instanceof WildcardType) {
               WildcardType wildcardArg = (WildcardType) toArg;
               for (Type upperBound : wildcardArg.getUpperBounds()) {
                  if (!isAssignable(upperBound, fromArg)) {
                     return false;
                  }
               }
               for (Type lowerBound : wildcardArg.getLowerBounds()) {
                  if (!isAssignable(fromArg, lowerBound)) {
                     return false;
                  }
               }
            } else if (!equals(toArg, fromArg)) {
               return false;
            }
         }
         return true;
      } else if (to instanceof GenericArrayType) {
         GenericArrayType toArrayType = (GenericArrayType) to;
         if (from instanceof Class) {
            Class<?> fromClass = (Class<?>) from;
            return fromClass.isArray() && isAssignable(toArrayType.getGenericComponentType(),
                  fromClass.getComponentType());
         } else if (from instanceof GenericArrayType) {
            return isAssignable(toArrayType.getGenericComponentType(),
                  ((GenericArrayType) from).getGenericComponentType());
         }
      } else if (to instanceof TypeVariable) {
         // Type variable value is not known. We can only assign to it from another instance of the
         // same type variable or some other variable or wildcard that extends it. (Extension case
         // is handled above in check for `from` being a TypeVariable or WildcardType.)
         return equals(to, from);
      } else if (to instanceof WildcardType) {
         WildcardType toWildcard = (WildcardType) to;
         Type lowerBounds[] = toWildcard.getLowerBounds();
         if (lowerBounds.length == 0) {
            // Can only assign to a wildcard type based on its lower bounds
            return false;
         }
         assert toWildcard.getUpperBounds().length == 1;
         assert toWildcard.getUpperBounds()[0] == Object.class;
         for (Type bound : lowerBounds) {
            if (!isAssignable(from, bound)) {
               return false;
            }
         }
         return true;
      }
      // no match found; not assignment-compatible
      return false;
   }
   
   /**
    * Resolves the given type variable in the context of the given type. For example, if the given
    * type variable is {@code Collection.<E>} and the given type is the parameterized type
    * {@code List<Optional<String>>}, then this will return {@code Optional<String>}.
    * 
    * <p>If the given type variable cannot be resolved then {@code null} is returned. For example,
    * if the type variable given is {@code Map.<K>} and the given type is {@link List<Number>}, then
    * the variable cannot be resolved.
    *
    * @param context the generic type whose context is used to resolve the given variable
    * @param variable the type variable to resolve
    * @return the resolved value of the given variable or {@code null} if it cannot be resolved
    */
   public static Type resolveTypeVariable(Type context, TypeVariable<?> variable) {
      GenericDeclaration declaration = variable.getGenericDeclaration();
      if (!(declaration instanceof Class)) {
         return null; // can only resolve variables declared on classes
      }
      while (true) {
         Type componentType = getComponentType(context);
         if (componentType == null) {
            break;
         }
         context = componentType;
      }
      Type superType = resolveSuperType(context, (Class<?>) declaration);
      if (superType == null || superType instanceof Class) {
         return null; // cannot resolve
      }
      TypeVariable<?> vars[] = ((Class<?>) declaration).getTypeParameters();
      Type actualArgs[] = ((ParameterizedType) superType).getActualTypeArguments();
      assert actualArgs.length == vars.length;
      for (int i = 0, len = vars.length; i < len; i++) {
         if (equals(vars[i], variable)) {
            Type value = actualArgs[i];
            // if actual type argument equals the type variable itself, it isn't resolved
            return equals(vars[i], value) ? null : value;
         }
      }
      throw new AssertionError("should not be reachable");
   }

   /**
    * Resolves the given type in the context of another type. Any type variable references in the
    * type will be resolved using the given context. For example, if the given type is
    * {@code Map<? extends K, ? extends V>} (where {@code K} and {@code V} are the type variables
    * of interface {@code Map}) and the given context is the parameterized type
    * {@code TreeMap<String, List<String>>} then the type returned will be
    * {@code Map<? super String, ? super List<String>>}.
    * 
    * <p>If any type variables present in the given type cannot be resolved, they will be unchanged
    * and continue to refer to type variables in the returned type.
    *
    * @param context the generic type whose context is used to resolve the given type
    * @param typeToResolve the generic type to resolve
    * @return the resolved type
    */
   public static Type resolveType(Type context, Type typeToResolve) {
      Map<TypeVariableWrapper, Type> resolvedVariableValues = new HashMap<>();
      Set<TypeVariableWrapper> resolvedVariables = new HashSet<>();
      resolveTypeVariables(context, typeToResolve, resolvedVariableValues, resolvedVariables);
      return replaceTypeVariablesInternal(typeToResolve, resolvedVariableValues);
   }
   
   private static void resolveTypeVariables(Type context, Type type,
         Map<TypeVariableWrapper, Type> resolvedVariableValues,
         Set<TypeVariableWrapper> resolvedVariables) {
      if (type instanceof Class) {
         // no-op
      } else if (type instanceof ParameterizedType) {
         for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
            resolveTypeVariables(context, arg, resolvedVariableValues, resolvedVariables);
         }
      } else if (type instanceof GenericArrayType) {
         resolveTypeVariables(context, ((GenericArrayType) type).getGenericComponentType(),
               resolvedVariableValues, resolvedVariables);
      } else if (type instanceof WildcardType) {
         WildcardType wt = (WildcardType) type;
         for (Type bound : wt.getUpperBounds()) {
            resolveTypeVariables(context, bound, resolvedVariableValues, resolvedVariables);
         }
         for (Type bound : wt.getLowerBounds()) {
            resolveTypeVariables(context, bound, resolvedVariableValues, resolvedVariables);
         }
      } else if (type instanceof TypeVariable) {
         TypeVariable<?> tv = (TypeVariable<?>) type;
         TypeVariableWrapper wrapper = wrap(tv);
         if (tv.getGenericDeclaration() instanceof Class) {
            // don't bother re-resolving occurrences of variables we've already seen
            if (resolvedVariables.add(wrapper)) {
               Type resolvedValue = resolveTypeVariable(context, tv);
               if (resolvedValue != null) {
                  resolvedVariableValues.put(wrapper, resolvedValue);
               }
            }
         }
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }
   
   /**
    * Computes a new version of the given type by replacing all occurrences of a given type variable
    * with a given value for that variable.
    *
    * @param type the type to be resolved
    * @param typeVariable the type variable 
    * @param typeValue the value that will replace the type variable
    * @return the given type, but with references to the given type variable replaced with the given
    *       value
    */
   public static Type replaceTypeVariable(Type type, TypeVariable<?> typeVariable,
         Type typeValue) {
      Map<TypeVariableWrapper, Type> wrappedVariables =
            Collections.singletonMap(wrap(typeVariable), typeValue); 
      checkTypeValue(typeVariable, typeValue, wrappedVariables);
      return replaceTypeVariablesInternal(type, wrappedVariables);
   }
   
   /**
    * Computes a new version of the given type by replacing all occurrences of mapped type variables
    * with the given mapped values. This provides a way to more efficiently resolve a batch of type
    * variables instead of iteratively resolving one variable at a time.
    *
    * @param type the type to be resolved
    * @param typeVariables a map of type variables to values
    * @return the given type, but with references to the given type variables replaced with the
    *       given mapped values
    */
   public static Type replaceTypeVariables(Type type, Map<TypeVariable<?>, Type> typeVariables) {
      // wrap the variables to make sure their hashCode and equals are well-behaved
      Map<TypeVariableWrapper, Type> wrappedVariables = new HashMap<>(typeVariables.size() * 4 / 3,
            0.75f);
      for (Entry<TypeVariable<?>, Type> entry : typeVariables.entrySet()) {
         TypeVariable<?> typeVariable = entry.getKey();
         Type typeValue = entry.getValue();
         wrappedVariables.put(wrap(typeVariable), typeValue);
      }
      // check type bounds
      checkTypeValues(wrappedVariables);
      return replaceTypeVariablesInternal(type, wrappedVariables);
   }
   
   private static Type replaceTypeVariablesInternal(Type type,
         Map<TypeVariableWrapper, Type> typeVariables) {
      if (typeVariables.isEmpty()) {
         return type;
      }
      
      if (type instanceof Class) {
         return type;
      }
      
      if (type instanceof ParameterizedType) {
         ParameterizedType pType = (ParameterizedType) type;
         Type initialOwner = pType.getOwnerType();
         Type resolvedOwner = initialOwner == null ? null
               : replaceTypeVariablesInternal(initialOwner, typeVariables);
         Type initialRaw = pType.getRawType();
         Type resolvedRaw = initialRaw == null ? null
               : replaceTypeVariablesInternal(initialRaw, typeVariables);
         boolean different = initialOwner != resolvedOwner || initialRaw != resolvedRaw;
         Type initialArgs[] = pType.getActualTypeArguments();
         List<Type> resolvedArgs = new ArrayList<>(initialArgs.length);
         for (Type initial : initialArgs) {
            Type resolved = replaceTypeVariablesInternal(initial, typeVariables);
            resolvedArgs.add(resolved);
            if (initial != resolved) {
               different = true;
            }
         }
         return different
               ? new ParameterizedTypeImpl(resolvedOwner, resolvedRaw, resolvedArgs)
               : type;
         
      } else if (type instanceof GenericArrayType) {
         Type initialComponent = ((GenericArrayType) type).getGenericComponentType();
         Type resolvedComponent = replaceTypeVariablesInternal(initialComponent, typeVariables);
         return resolvedComponent == initialComponent ? type
               : new GenericArrayTypeImpl(resolvedComponent);
         
      } else if (type instanceof WildcardType) {
         WildcardType wtType = (WildcardType) type;
         boolean different = false;
         Type initialUpperBounds[] = wtType.getUpperBounds();
         List<Type> resolvedUpperBounds = new ArrayList<>(initialUpperBounds.length);
         for (Type initial : initialUpperBounds) {
            Type resolved = replaceTypeVariablesInternal(initial, typeVariables);
            resolvedUpperBounds.add(resolved);
            if (initial != resolved) {
               different = true;
            }
         }
         Type initialLowerBounds[] = wtType.getLowerBounds();
         List<Type> resolvedLowerBounds = new ArrayList<>(initialLowerBounds.length);
         for (Type initial : initialLowerBounds) {
            Type resolved = replaceTypeVariablesInternal(initial, typeVariables);
            resolvedLowerBounds.add(resolved);
            if (initial != resolved) {
               different = true;
            }
         }
         return different
               ? new WildcardTypeImpl(resolvedUpperBounds, resolvedLowerBounds)
               : type;
         
      } else if (type instanceof TypeVariable) {
         Type resolvedType = typeVariables.get(wrap((TypeVariable<?>) type));
         return resolvedType != null ? resolvedType : type;
         
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }

   private static void checkTypeValue(TypeVariable<?> variable, Type argument,
         Map<TypeVariableWrapper, Type> resolvedVariables) {
      checkTypeValue(variable, argument, resolvedVariables, -1);
   }

   private static void checkTypeValues(Map<TypeVariableWrapper, Type> typeVariables) {
      for (Entry<TypeVariableWrapper, Type> entry : typeVariables.entrySet()) {
         checkTypeValue(entry.getKey().typeVariable, entry.getValue(), typeVariables, -1);
      }
   }

   private static void checkTypeValue(TypeVariable<?> variable, Type argument,
         Map<TypeVariableWrapper, Type> resolvedVariables, int index) {
      String id = index < 0 ? "" : (" #" + index);
      if (argument instanceof Class && ((Class<?>) argument).isPrimitive()) {
         throw new IllegalArgumentException("Type argument" + id + " is primitive: "
               + argument.getTypeName());
      }
      for (Type bound : variable.getBounds()) {
         // do any substitutions on owner type variables that may be referenced
         bound = replaceTypeVariablesInternal(bound, resolvedVariables);
         if (!isAssignable(bound, argument)) {
            throw new IllegalArgumentException("Argument" + id + ", "
                  + argument.getTypeName() + " does not extend bound "+ bound.getTypeName());
         }
      }
   }
   
   /**
    * For the given generic type, computes the generic super-type corresponding to the given raw
    * class token. If the given generic type is not actually assignable to the given super-type
    * token then {@code null} is returned. 
    * 
    * <p>For example, if the given generic type is {@code List<String>} and the given raw class
    * token is {@code Collection.class}, then this method will resolve type parameters and return a
    * parameterized type: {@code Collection<String>}.
    * 
    * <p>If the given generic type is a raw class token but represents a type with type parameters,
    * then raw types are returned. For example, if the generic type is {@code HashMap.class} and
    * the given raw class token is {@code Map.class}, then this method simply returns the raw type
    * {@code Map.class}. This is also done if any super-type traversed uses raw types. For example,
    * if the given type's super-class were defined as {@code class Xyz extends HashMap}, then the
    * type arguments to {@code HashMap} are lost due to raw type usage and a raw type is returned.
    * 
    * <p>If the given generic type is a raw class token that does <em>not</em> have any type
    * parameters, then the returned value can still be a generic type. For example, if the given
    * type is {@code Xyz.class} and that class is defined as {@code class Xyz extends
    * ArrayList<String>}, then querying for a super-type of {@code List.class} will return a
    * parameterized type: {@code List<String>}.
    * 
    * <p>If the given generic type is a wildcard type or type variable, its super-types include all
    * upper bounds (and their super-types), and the type's hierarchy is traversed as such.
    * 
    * <p>Technically, a generic array type's only super-types are {@code Object}, {@code Cloneable},
    * and {@code Serializable}. However, since array types are co-variant, this method can resolve
    * other super-types to which the given type is assignable. For example, if the given type is
    * {@code HashMap<String, Number>[]} and the given raw class token queried is {@code Map[].class}
    * then this method will resolve type parameters and return a generic type:
    * {@code Map<String, Number>[]}.
    *
    * @param type a generic type
    * @param superClass a class token for the super-type to query
    * @return a generic type that represents the given super-type token resolved in the context of
    *       the given type or {@code null} if the given token is not a super-type of the given
    *       generic type
    */
   public static Type resolveSuperType(Type type, Class<?> superClass) {
      requireNonNull(type);
      requireNonNull(superClass);
      Map<TypeVariableWrapper, Type> typeVariables = new HashMap<>();
      Type superType = findGenericSuperType(type, superClass, typeVariables);
      return superType != null ? replaceTypeVariablesInternal(superType, typeVariables) : null;
   }

   /**
    * Finds the generic super type for the given generic type and super-type token and accumulates
    * type variables and actual type arguments in the given map.
    *
    * @param type the generic type
    * @param superClass the class token for the super-type being queried
    * @param typeVariables a map of type variables that accumulates type variables and actual type
    *       arguments as types are traversed from the given type to the given super-type 
    * @return the generic type corresponding to the given super-type token as returned by
    *       {@link Class#getGenericSuperclass()} or {@link Class#getGenericInterfaces()} 
    */
   private static Type findGenericSuperType(Type type, Class<?> superClass,
         Map<TypeVariableWrapper, Type> typeVariables) {
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>) type;
         // if this is a raw type reference to a generic type, just forget generic type information
         // and use raw types from here on out
         boolean useRawTypes = clazz.getTypeParameters().length > 0; 
         return findGenericSuperType(clazz, superClass, useRawTypes, typeVariables);
          
      } else if (type instanceof GenericArrayType) {
         if (superClass == Object.class || superClass == Serializable.class
               || superClass == Cloneable.class) {
            return superClass;
         }
         if (!superClass.isArray()) {
            return null;
         }
         Type resolvedComponentType = findGenericSuperType(
               ((GenericArrayType) type).getGenericComponentType(), superClass.getComponentType(),
               typeVariables);
         if (resolvedComponentType instanceof Class) {
            return getArrayType((Class<?>) resolvedComponentType);
         } else if (resolvedComponentType == null) {
            return null;
         } else {
            return newGenericArrayType(resolvedComponentType);
         }
         
      } else if (type instanceof ParameterizedType) {
         Class<?> rawType = getRawType(type);
         ParameterizedType pType = (ParameterizedType) type;
         if (rawType == superClass) {
            return type;
         }
         Type superType = findGenericSuperType(rawType, superClass, false, typeVariables);
         mergeTypeVariables(pType, rawType, typeVariables);
         return superType;
         
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType
               ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         for (Type bound : bounds) {
            Type superType = findGenericSuperType(bound, superClass, typeVariables);
            if (superType != null) {
               return superType;
            }
         }
         return null;
         
      } else {
         throw new IllegalArgumentException("Unrecognized Type: " + type);
      }
   }
   
   private static Type findGenericSuperType(Class<?> clazz, Class<?> superClass,
         boolean useRawTypes, Map<TypeVariableWrapper, Type> typeVariables) {
      if (!superClass.isAssignableFrom(clazz)) {
         return null;
      }
      if (superClass.getTypeParameters().length == 0) {
         return superClass;
      }
      if (useRawTypes) {
         return superClass;
      }
      return superClass.isInterface()
             ? findGenericInterface(clazz, superClass, typeVariables, new HashSet<>())
             : findGenericSuperclass(clazz, superClass, typeVariables);

   }
   
   private static Type findGenericSuperclass(Class<?> clazz, Class<?> superClass,
         Map<TypeVariableWrapper, Type> typeVariables) {
      // TODO: iterative instead of recursive?
      Class<?> actualSuper = clazz.getSuperclass();
      assert actualSuper != null;
      Type genericSuper = clazz.getGenericSuperclass();
      Type ret;
      if (actualSuper == superClass) {
         ret = genericSuper;
      } else {
         // recurse until we find it
         ret = findGenericSuperclass(actualSuper, superClass, typeVariables);
         if (ret == null) {
            return null;
         }
      }
      if (genericSuper instanceof ParameterizedType) {
         mergeTypeVariables((ParameterizedType) genericSuper, actualSuper, typeVariables);
      }
      return ret;
   }

   private static Type findGenericInterface(Class<?> clazz, Class<?> intrface,
         Map<TypeVariableWrapper, Type> typeVariables, Set<Class<?>> alreadyChecked) {
      if (alreadyChecked.contains(clazz)) {
         return null;
      }
      // TODO: iterative and breadth-first instead of recursive?
      Class<?> actualInterfaces[] = clazz.getInterfaces();
      Type genericInterfaces[] = clazz.getGenericInterfaces();
      assert actualInterfaces.length > 0;
      Class<?> actualSuper = null;
      Type genericSuper = null;
      Type ret = null;
      // not quite breadth-first -- but first, shallowly check all interfaces before we
      // check their super-interfaces
      for (int i = 0, len = actualInterfaces.length; i < len; i++) {
         if (actualInterfaces[i] == intrface) {
            actualSuper = actualInterfaces[i];
            genericSuper = genericInterfaces[i];
            ret = genericSuper;
         }
      }
      if (ret == null) {
         // didn't find it: check super-interfaces
         for (int i = 0, len = actualInterfaces.length; i < len; i++) {
            ret = findGenericInterface(actualInterfaces[i], intrface, typeVariables, alreadyChecked);
            if (ret != null) {
               actualSuper = actualInterfaces[i];
               genericSuper = genericInterfaces[i];
            }
         }
      }
      if (ret == null) {
         // still didn't find it: check super-class's interfaces
         if ((actualSuper = clazz.getSuperclass()) == null) {
            return null; // no super-class
         }
         genericSuper = clazz.getGenericSuperclass();
         ret = findGenericInterface(clazz.getSuperclass(), intrface, typeVariables, alreadyChecked);
      }
      if (ret == null) {
         alreadyChecked.add(clazz);
         return null;
      }
      if (genericSuper instanceof ParameterizedType) {
         mergeTypeVariables((ParameterizedType) genericSuper, actualSuper, typeVariables);
      }
      return ret;
   }
   
   private static void mergeTypeVariables(ParameterizedType type, Class<?> rawType,
         Map<TypeVariableWrapper, Type> typeVariables) {
      Type ownerType = type.getOwnerType();
      if (ownerType instanceof ParameterizedType) {
         mergeTypeVariables((ParameterizedType) ownerType, getRawType(ownerType), typeVariables);
      }
      Map<TypeVariableWrapper, Type> currentVars = new HashMap<>();
      TypeVariable<?> vars[] = rawType.getTypeParameters();
      Type values[] = type.getActualTypeArguments();
      assert vars.length == values.length;
      for (int i = 0, len = vars.length; i < len; i++) {
         currentVars.put(wrap(vars[i]), values[i]);
      }
      // update any existing type variable values in case they refer to these new variables
      for (Entry<TypeVariableWrapper, Type> entry : typeVariables.entrySet()) {
         entry.setValue(replaceTypeVariablesInternal(entry.getValue(), currentVars));
      }
      typeVariables.putAll(currentVars);
   }
   
   /**
    * Creates a new {@link GenericArrayType} object with the given component type. The component
    * type should not be a {@link Class} since that would be a normal (e.g. not generic) array
    * type. Also, the component type should not be a wildcard type since the component type of an
    * array must be knowable.
    *
    * @param componentType the component type of the array.
    * @return a generic array type with the given component type
    * @throws NullPointerException if the given argument is {@code null}
    * @throws IllegalArgumentException if the given component type is not a
    *       {@link ParameterizedType}, {@link TypeVariable}, or {@link GenericArrayType} 
    */
   public static GenericArrayType newGenericArrayType(Type componentType) {
      requireNonNull(componentType);
      if (!(componentType instanceof ParameterizedType) && !(componentType instanceof TypeVariable)
            && !(componentType instanceof GenericArrayType)) {
         throw new IllegalArgumentException("GenericArrayType component should be a"
               + " ParameterizedType, TypeVariable, or GenericArrayType");
      }
      return new GenericArrayTypeImpl(componentType);
   }
   
   private static final Map<Class<?>, String> PRIMITIVE_CLASS_SYMBOLS;
   static {
      Map<Class<?>, String> symbols = new HashMap<>();
      symbols.put(boolean.class, "Z");
      symbols.put(byte.class, "B");
      symbols.put(char.class, "C");
      symbols.put(short.class, "S");
      symbols.put(int.class, "I");
      symbols.put(long.class, "J");
      symbols.put(float.class, "F");
      symbols.put(double.class, "D");
      PRIMITIVE_CLASS_SYMBOLS = Collections.unmodifiableMap(symbols);
   }

   /**
    * Gets the class token that represents an array of the given component type.
    *
    * @param componentType the component type
    * @return a class token that represents an array of the given component type
    * 
    */
   public static <T> Class<T[]> getArrayType(Class<T> componentType) {
      requireNonNull(componentType);
      StringBuilder arrayClassName = new StringBuilder("[");
      if (componentType == void.class) {
         throw new IllegalArgumentException("Cannot create an array with component type void");
      } else if (componentType.isPrimitive()) {
         String symbol = PRIMITIVE_CLASS_SYMBOLS.get(componentType);
         assert symbol != null;
         arrayClassName.append(symbol);
      } else if (componentType.isArray()) {
         arrayClassName.append(componentType.getName());
      } else {
         arrayClassName.append("L").append(componentType.getName()).append(";");
      }
      // use the component type's class loader to make sure the array type is correct
      ClassLoader loader = componentType.getClassLoader();
      try {
         String className = arrayClassName.toString();
         @SuppressWarnings("unchecked") // we know the type is right since we just built the name
         Class<T[]> arrayType = (Class<T[]>)
               (loader == null ? Class.forName(className)
                     : loader.loadClass(arrayClassName.toString()));
         assert arrayType.getComponentType() == componentType;
         return arrayType;
      } catch (ClassNotFoundException e) {
         // uh oh, we must have done something wrong
         throw new RuntimeException("Failed to construct array type", e);
      }
   }
   
   /**
    * Returns the type parameters for the given type or an empty array if it has none. Only class
    * tokens and parameterized types have type parameters. Array types cannot have type parameters
    * (only the arrays' component types can). Wildcard types and type variables also do not have
    * type parameters (only their bounds, e.g. super-types, can).
    *
    * @param type a generic type
    * @return the type parameters for the given type; an empty array if the type ha no parameters
    */
   public static TypeVariable<Class<?>>[] getTypeParameters(Type type) {
      requireNonNull(type);
      if (type instanceof Class) {
         // have to use TypeVariable<?> to obfuscate the T in TypeVariable<Class<T>> return type...
         TypeVariable<?> variables[] = ((Class<?>) type).getTypeParameters();
         @SuppressWarnings("unchecked")
         TypeVariable<Class<?>> ret[] = (TypeVariable<Class<?>>[]) variables;
         return ret;
      } else if (type instanceof ParameterizedType) {
         return getTypeParameters(getRawType(((ParameterizedType) type).getRawType()));
      } else {
         @SuppressWarnings("unchecked") // array is empty, so conversion is safe
         TypeVariable<Class<?>> empty[] = (TypeVariable<Class<?>>[]) EMPTY_TYPE_VARIABLES;
         return empty;
      }
   }
   
   /**
    * Returns the actual type arguments if the given type is a parameterized type. Otherwise, it
    * returns an empty array.
    *
    * @param type the generic type
    * @return the actual type arguments if the given types is a parameterized type; an empty array
    *       otherwise
    */
   public static Type[] getActualTypeArguments(Type type) {
      requireNonNull(type);
      if (type instanceof ParameterizedType) {
         return ((ParameterizedType) type).getActualTypeArguments();
      } else {
         return EMPTY_TYPES;
      }
   }
   
   /**
    * Creates a new {@link ParameterizedType} for a generic top-level or static type. In this case,
    * there is no enclosing instance of a generic type, so the resulting parameterized type has no
    * {@linkplain ParameterizedType#getOwnerType() owner type}.
    *
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type (should not be empty)
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if either of the given arguments is {@code null}
    * @throws IllegalArgumentException if the raw type is a non-generic type or if no type arguments
    *       are given, if the wrong number of type arguments are given, or if any of the type
    *       arguments is outside the bounds for the corresponding type variable
    */
   public static ParameterizedType newParameterizedType(Class<?> rawType, Type... typeArguments) {
      return newParameterizedTypeInternal(null, rawType, Arrays.asList(typeArguments));
   }
   
   /**
    * Creates a new {@link ParameterizedType} for a generic top-level or static type. In this case,
    * there is no enclosing instance of a generic type, so the resulting parameterized type has no
    * {@linkplain ParameterizedType#getOwnerType() owner type}.
    *
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type (should not be empty)
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if either of the given arguments is {@code null}
    * @throws IllegalArgumentException if the raw type is a non-generic type or if no type arguments
    *       are given, if the wrong number of type arguments are given, or if any of the type
    *       arguments is outside the bounds for the corresponding type variable
    */
   public static ParameterizedType newParameterizedType(Class<?> rawType, 
         List<Type> typeArguments) {
      return newParameterizedTypeInternal(null, rawType, typeArguments);
   }
   
   /**
    * Creates a new {@link ParameterizedType} for a generic enclosed type. This is necessary for
    * representing non-static enclosed types whose enclosing type is generic, e.g. {@code
    * GenericType<T, U>.EnclosedType}. The actual type arguments may be empty if the given type is
    * not generic, and only an enclosing type is generic.
    *
    * @param ownerType the generic enclosing type
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type or an empty list if
    *       the raw type is not a generic type
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if any of the given arguments is {@code null}
    * @throws IllegalArgumentException if the given raw type's owner does not match the given owner
    *       type, if the wrong number of type arguments are given, or if any of the type arguments
    *       is outside the bounds for the corresponding type variable
    */
   public static ParameterizedType newParameterizedType(ParameterizedType ownerType,
         Class<?> rawType, Type... typeArguments) {
      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
            Arrays.asList(typeArguments));
   }

   /**
    * Creates a new {@link ParameterizedType} for a generic enclosed type. This is necessary for
    * representing non-static enclosed types whose enclosing type is generic, e.g. {@code
    * GenericType<T, U>.EnclosedType}. The actual type arguments may be empty if the given raw type
    * is not generic and only its owner type is generic.
    *
    * @param ownerType the generic enclosing type
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type or an empty list if
    *       the raw type is not a generic type
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if any of the given arguments is {@code null} or if any of the
    *       elements of {@code typeArguments} is {@code null}
    * @throws IllegalArgumentException if the given raw type's owner does not match the given owner
    *       type, if the wrong number of type arguments are given, if any type argument is a
    *       primitive type or {@code void}, or if any of the type arguments is outside the bounds
    *       for the corresponding type variable
    */
   public static ParameterizedType newParameterizedType(ParameterizedType ownerType,
         Class<?> rawType, List<Type> typeArguments) {
      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
            typeArguments);
   }
   
   private static ParameterizedType newParameterizedTypeInternal(ParameterizedType ownerType,
         Class<?> rawType, List<Type> typeArguments) {
      requireNonNull(rawType);
      for (Type type : typeArguments) {
         requireNonNull(type);
      }
      List<Type> copyOfArguments = new ArrayList<>(typeArguments); // defensive copy
      Type owner;
      if (ownerType != null) {
         if (rawType.getDeclaringClass() != getRawType(ownerType)) {
            throw new IllegalArgumentException("Owner type " + ownerType.getTypeName() + " does not"
                  + " match actual owner of given raw type " + rawType.getTypeName());
         } else if ((rawType.getModifiers() & Modifier.STATIC) != 0) {
            throw new IllegalArgumentException("Given raw type " + rawType.getTypeName()
                  + " is static so cannot have a parameterized owner type");
         }
         owner = ownerType;
      } else {
         Class<?> ownerClass = rawType.getDeclaringClass();
         if (ownerClass != null && !typeArguments.isEmpty()
               && (rawType.getModifiers() & Modifier.STATIC) == 0
               && ownerClass.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Non-static parameterized type "
               + rawType.getTypeName() + " must have parameterized owner");
         }
         owner = ownerClass;
      }
      TypeVariable<?> typeVariables[] = rawType.getTypeParameters();
      int len = typeVariables.length;
      if (len != copyOfArguments.size()) {
         throw new IllegalArgumentException("Given type " + rawType.getTypeName() + " has " + len
               + " type variable(s), but " + copyOfArguments.size()
               + " argument(s) were specified");
      }
      Map<TypeVariableWrapper, Type> resolvedVariables = new HashMap<>();
      // add current type variables, in case there are recursive bounds
      for (int i = 0; i < len; i++) {
         resolvedVariables.put(wrap(typeVariables[i]), copyOfArguments.get(i));
      }      
      if (ownerType != null) {
         // also resolve owners' variables
         collectTypeParameters(ownerType, resolvedVariables);
      }
      for (int i = 0; i < len; i++) {
         TypeVariable<?> variable = typeVariables[i];
         Type argument = copyOfArguments.get(i);
         checkTypeValue(variable, argument, resolvedVariables);
      }
      if (ownerType == null && len == 0) {
         throw new IllegalArgumentException("A ParameterizedType must have either a parameterized"
               + " owner or its own type parameters");
      }
      return new ParameterizedTypeImpl(owner, rawType, copyOfArguments);
   }
   
   private static void collectTypeParameters(ParameterizedType type,
         Map<TypeVariableWrapper, Type> typeParameters) {
      Type owner = type.getOwnerType();
      if (owner instanceof ParameterizedType) {
         collectTypeParameters((ParameterizedType) owner, typeParameters);
      }
      Type args[] = type.getActualTypeArguments();
      TypeVariable<?> params[] = getTypeParameters(type.getRawType());
      assert args.length == params.length;
      for (int i = 0, len = args.length; i < len; i++) {
         typeParameters.put(wrap(params[i]), args[i]);
      }
   }

   /**
    * Gets the {@link TypeVariable} from the given declaration site (class, method, or constructor)
    * for the given name.
    *
    * @param name the name of the type variable
    * @param declaration the site of the generic declaration (a class, method, or constructor)
    * @return the named type variable
    * @throws NullPointerException if either of the given arguments is {@code null}
    * @throws IllegalArgumentException if the given declaration has no type variable with the given
    *       name 
    */
   public static <D extends GenericDeclaration> TypeVariable<D> getTypeVariable(String name,
         D declaration) {
      requireNonNull(name);
      for (TypeVariable<?> variable : declaration.getTypeParameters()) {
         if (variable.getName().equals(name)) {
            @SuppressWarnings("unchecked") // we know it came from a D, so cast is safe
            TypeVariable<D> ret = (TypeVariable<D>) variable;
            return ret;
         }
      }
      throw new IllegalArgumentException("No type parameter " + name + " exists for element "
            + declaration); 
   }
   
   /**
    * Retrieves the type variables of the given declaration site as a map of type parameter names to
    * {@link TypeVariable}s.
    *
    * @param declaration the site of the generic declaration (a class, method, or constructor)
    * @return a map of type parameter names to type variable definitions
    */
   public static <D extends GenericDeclaration> Map<String, TypeVariable<D>> getTypeVariablesAsMap(
         D declaration) {
      Map<String, TypeVariable<D>> ret = new LinkedHashMap<>();
      for (TypeVariable<?> variable : declaration.getTypeParameters()) {
         @SuppressWarnings("unchecked") // we know it came from a D, so cast is safe
         TypeVariable<D> var = (TypeVariable<D>) variable;
         ret.put(var.getName(), var);
      }
      return Collections.unmodifiableMap(ret);
   }
   
   /**
    * Creates a new {@link WildcardType} with an upper bound, i.e.&nbsp;{@code ? extends T}.
    *
    * @param bound the upper bound for the wildcard type
    * @return a new wildcard type with the given bound
    * @throws NullPointerException if the given bound is {@code null}
    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
    *       type
    */
   public static WildcardType newExtendsWildcardType(Type bound) {
      return newWildcardTypeInternal(bound, true);
   }

   /**
    * Creates a new {@link WildcardType} with a lower bound, i.e.&nbsp;{@code ? super T}.
    *
    * @param bound the lower bound for the wildcard type
    * @return a new wildcard type with the given bound
    * @throws NullPointerException if the given bound is {@code null}
    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
    *       type
    */
   public static WildcardType newSuperWildcardType(Type bound) {
      return newWildcardTypeInternal(bound, false);
   }
      
   private static WildcardType newWildcardTypeInternal(Type bound, boolean isUpperBound) {
      requireNonNull(bound);
      if (bound instanceof Class) {
         Class<?> boundClass = (Class<?>) bound;
         if (boundClass.isPrimitive()) {
            throw new IllegalArgumentException("Bound for a WildcardType cannot be primitive");
         }
      } else if (bound instanceof WildcardType) {
         throw new IllegalArgumentException("Bound for a WildcardType cannot be a WildcardType");
      }
      return new WildcardTypeImpl(bound, isUpperBound);
   }
   
   private static TypeVariableWrapper wrap(TypeVariable<?> typeVariable) {
      return new TypeVariableWrapper(typeVariable);
   }

   /**
    * A wrapper around {@link TypeVariable}s to ensure consistent equals and hash code, for use
    * as keys in a hash map. Since {@link TypeVariable} does not define semantics for equals and
    * hash code, we wrap them before putting into a map instead of relying on unknown behavior of
    * underlying implementation.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class TypeVariableWrapper {
      final TypeVariable<?> typeVariable;
      
      TypeVariableWrapper(TypeVariable<?> typeVariable) {
         this.typeVariable = typeVariable;
      }
      
      @Override public boolean equals(Object o) {
         if (o instanceof TypeVariableWrapper) {
            return Types.equals(typeVariable, ((TypeVariableWrapper) o).typeVariable);
         }
         return false;
      }
      
      @Override public int hashCode() {
         return Types.hashCode(typeVariable);
      }
      
      @Override public String toString() {
         return Types.toString(typeVariable);
      }
   }
   
   /**
    * Implements {@link ParameterizedType}.
    */
   private static class ParameterizedTypeImpl implements ParameterizedType, Serializable {
      private static final long serialVersionUID = -4933098144775956311L;

      private final Type ownerType;
      private final Type rawType;
      private final List<Type> typeArguments;

      ParameterizedTypeImpl(Type ownerType, Type rawType, List<Type> typeArguments) {
         this.ownerType = ownerType;
         this.rawType = rawType;
         this.typeArguments = typeArguments;
      }

      @Override
      public Type[] getActualTypeArguments() {
         return typeArguments.toArray(new Type[typeArguments.size()]);
      }

      @Override
      public Type getRawType() {
         return rawType;
      }

      @Override
      public Type getOwnerType() {
         return ownerType;
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof ParameterizedType) {
            return Types.equals(this, (ParameterizedType) o);
         }
         return false;
      }

      @Override
      public int hashCode() {
         return Types.hashCode(this);
      }

      @Override
      public String toString() {
         return Types.toString(this);
      }
   }
   
   /**
    * Implements {@link GenericArrayType}.
    */
   private static class GenericArrayTypeImpl implements GenericArrayType, Serializable {
      private static final long serialVersionUID = -8335550068623986776L;
      
      private final Type componentType;
      
      GenericArrayTypeImpl(Type componentType) {
         this.componentType = componentType;
      }
      
      @Override
      public Type getGenericComponentType() {
         return componentType;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof GenericArrayType) {
            return Types.equals(this, (GenericArrayType) o);
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return Types.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Types.toString(this);
      }
   }
   
   /**
    * Implements {@link WildcardType}.
    */
   private static class WildcardTypeImpl implements WildcardType, Serializable {
      private static final long serialVersionUID = -5371665313248454547L;
      
      private final Type upperBounds[];
      private final Type lowerBounds[];
      
      WildcardTypeImpl(Type bound, boolean isUpperBound) {
         upperBounds = new Type[] { isUpperBound ? bound : Object.class };
         lowerBounds = isUpperBound ? EMPTY_TYPES : new Type[] { bound };
      }
      
      WildcardTypeImpl(List<Type> upperBounds, List<Type> lowerBounds) {
         this.upperBounds = upperBounds.toArray(new Type[upperBounds.size()]);
         this.lowerBounds = lowerBounds.isEmpty() ? EMPTY_TYPES
               : lowerBounds.toArray(new Type[lowerBounds.size()]);
      }
      
      @Override
      public Type[] getUpperBounds() {
         return upperBounds.clone();
      }

      @Override
      public Type[] getLowerBounds() {
         return lowerBounds.clone();
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof WildcardType) {
            return Types.equals(this, (WildcardType) o);
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return Types.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Types.toString(this);
      }
   }
}
