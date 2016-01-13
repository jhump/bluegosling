package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import com.apriori.collections.Iterables;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Numerous utility methods for using, constructing, and inspecting generic types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Type
 */
public final class Types {
   
   static final Type EMPTY_TYPES[] = new Type[0];
   static final Type JUST_OBJECT[] = new Type[] { Object.class };
   
   private static final TypeVariable<?> EMPTY_TYPE_VARIABLES[] = new TypeVariable<?>[0];
   private static final Class<?> ARRAY_INTERFACES[] =
         new Class<?>[] { Cloneable.class, Serializable.class };
   private static final Type ARRAY_SUPERTYPES[] =
         new Type[] { Object.class, Serializable.class, Cloneable.class };
   private static final Annotation EMPTY_ANNOTATIONS[] = new Annotation[0];
   private static final WildcardType EXTENDS_ANY = newExtendsWildcardType(Object.class);
   private static final Method CLASS_FORNAME0;
   
   static {
      Method m;
      try {
         m = Class.class.getDeclaredMethod("forName0", String.class, boolean.class,
               ClassLoader.class);
         m.setAccessible(true);
      } catch (Exception e) {
         m = null;
      }
      CLASS_FORNAME0 = m;
   }
   
   private Types() {}

   /**
    * Finds the raw class token that best represents the given type. If the type is a class then it
    * is returned. If it is a parameterized type, the parameterized type's raw type is returned. If
    * it is a generic array type, an class token representing an array of the component type is
    * returned. Finally, if it is either a wildcard type or type variable, its first upper bound is
    * returned.
    * 
    * <p>These are the same rules as defined in <em>Type Erasure</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.6">JLS
    * 4.6</a>).
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
         throw new UnknownTypeException(type);
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
         return getRawType(((ParameterizedType) type).getRawType()).getSuperclass();
      } else if (type instanceof GenericArrayType) {
         return Object.class;
      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
               : ((TypeVariable<?>) type).getBounds();
         assert bounds.length > 0;
         Class<?> superclass = getRawType(bounds[0]);
         return superclass.isInterface() ? null : superclass;
      } else {
         throw new UnknownTypeException(type);
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
         return getRawType(((ParameterizedType) type).getRawType()).getInterfaces();
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
         throw new UnknownTypeException(type);
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
         throw new UnknownTypeException(type);
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
    * <p>This differs from {@link #getInterfaces(Type)} in that it can return a non-raw type. For
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
               getRawType(((ParameterizedType) type).getRawType()).getInterfaces();
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
         throw new UnknownTypeException(type);
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
         Class<?> superclass = getSuperclass(type);
         return superclass != null && annotationType.getAnnotation(Inherited.class) != null
               ? superclass.getAnnotation(annotationType)
               : null;
      } else {
         throw new UnknownTypeException(type);
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
         Annotation annotations[] = getSuperclass(type).getAnnotations();
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
         throw new UnknownTypeException(type);
      }
   }

   /**
    * Finds all annotations declared on the given type. This finds annotations on the class that
    * corresponds to the given generic type. Annotations inherited from the type's superclass are
    * not included. If no annotations can be found then an empty array is returned.
    * 
    * <p>If the given type is an array type, a wildcard type, or a type variable then an empty
    * array is returned.
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
      } else if (type instanceof GenericArrayType || type instanceof WildcardType
            || type instanceof TypeVariable) {
         return EMPTY_ANNOTATIONS;
      } else {
         throw new UnknownTypeException(type);
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
   public static final int hashCode(Type type) {
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
   public static String toString(Type type) {
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
    * Determines if one type is assignable to another. This is true when the RHS is the same type or
    * a sub-type of the LHS (co-variance), but also true when the RHS is compatible with the LHS
    * after possible assignment conversions. The possible conversions include <em>Widening Primitive
    * Conversions</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">JLS
    * 5.1.3</a>), <em>Boxing and Unboxing Conversions</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.7">JLS
    * 5.1.7</a> and <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.8">
    * 5.1.8</a>), and <em>Unchecked Conversions</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.9">JLS
    * 5.1.9</a>).
    * 
    * <p>There is also a {@linkplain #isAssignableStrict stricter version}, that behaves more like
    * {@link Class#isAssignableFrom(Class)}, but supporting generic types instead of only raw types.
    *
    * @param to the LHS of assignment
    * @param from the RHS of assignment
    * @return true if the assignment is allowed
    * 
    * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.2">
    *       JLS 5.2: Assignment Contexts</a>
    */
   public static boolean isAssignable(Type to, Type from) {
      // This helper will test identity conversions, widening reference conversions, and unchecked
      // conversions.
      if (isAssignableReference(to, from, true)) {
         return true;
      }
      // If that fails, we still need to try widening primitive conversion and boxing/unboxing
      // conversion. All of these require from to be a raw class token (either a primitive type OR a
      // boxed primitive type, none which of are generic).
      if (from instanceof Class) {
         Class<?> fromClass = (Class<?>) from;
         if (to instanceof ParameterizedType && fromClass.isPrimitive()) {
            // try a boxing conversion.
            Class<?> boxedFromClass = box(fromClass);
            return boxedFromClass != fromClass && isAssignableReference(to, boxedFromClass, true); 
         } else if (to instanceof Class) {
            Class<?> toClass = (Class<?>) to;
            if (fromClass.isPrimitive()) {
               if (toClass.isPrimitive()) {
                  // primitive widening conversions
                  return isPrimitiveSubtype(toClass, fromClass);
               }
               // boxing conversion
               return toClass.isAssignableFrom(box(fromClass));
            } else if (toClass.isPrimitive()) {
               // unboxing conversion
               return toClass.isAssignableFrom(unbox(fromClass));
            }
         }
      }
      return false;
   }
   
   /**
    * Determines if one type is assignable to another, with restrictions. This is true when the RHS
    * is the same type or a subtype of the LHS (co-variance).
    * 
    * <p>This is effectively the same as {@link Class#isAssignableFrom(Class)} but supports generic
    * types instead of only raw types. To that end, it returns true if {@code from} can be assigned
    * to {@code to} using either <em>Identity Conversion</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.1">JLS
    * 5.1.1</a>) or <em>Widening Reference Conversion</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.4">JLS
    * 5.1.4</a>).
    * 
    * <p>Of particular note, this method does <strong>not</strong> check for assignability via
    * <em>Widening Primitive Conversion</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">JLS
    * 5.1.3</a>), <em>Boxing and Unboxing Conversions</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.7">JLS
    * 5.1.7</a> and <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.8">
    * 5.1.8</a>), or <em>Unchecked Conversions</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.9">JLS
    * 5.1.9</a>). To instead test assignment compatibility using all of these, see the
    * {@linkplain #isAssignable non-strict form}.
    * 
    * <p>It follows that, for an assignment that would require an unchecked cast, this function
    * returns false, as in this example:
    * <pre>
    * Type genericType = new TypeRef&lt;List&lt;String&gt;&gt;() {}.asTypes();
    * Type rawType = List.class;
    * 
    * // This returns true, no unchecked cast:
    * Types.isAssignable(rawType, genericType);
    * 
    * // This returns false as it requires unchecked cast:
    * Types.isAssignable(genericType, rawType);
    * </pre>
    * 
    * <p>Similarly, even though the Java language allows an assignment of an {@code int} value to a
    * {@code long} variable, this function returns false in this case since it does not perform
    * widening primitive conversion. This is analogous to the observation that {@code
    * long.class.isAssignableFrom(int.class)} returns false.
    *
    * @param to the LHS of assignment
    * @param from the RHS of assignment
    * @return true if the assignment is allowed
    */
   public static boolean isAssignableStrict(Type to, Type from) {
      return isAssignableReference(to, from, false);
   }
   
   /**
    * Tests for reference type assignability, optionally including unchecked conversions.
    *
    * @param to the LHS of assignment
    * @param from the RHS of assignment
    * @param allowUncheckedConversion if true then unchecked conversions are considered when
    *       decising if the types are assignable
    * @return true if the assignment is allowed
    */
   private static boolean isAssignableReference(Type to, Type from,
         boolean allowUncheckedConversion) {
      assert !isPrimitive(to) && !isPrimitive(from);
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
            if (isAssignableReference(to, bound, allowUncheckedConversion)) {
               return true;
            }
         }
         // they might still be assignable if they refer to the same type variable 
         return from instanceof TypeVariable && to instanceof TypeVariable && equals(to, from);
      } else if (to instanceof Class) {
         Class<?> toClass = (Class<?>) to;
         if (from instanceof GenericArrayType) {
            if (toClass == Cloneable.class || toClass == Serializable.class) {
               return true;
            }
            GenericArrayType fromArrayType = (GenericArrayType) from;
            return toClass.isArray() && isAssignableReference(toClass.getComponentType(),
                  fromArrayType.getGenericComponentType(), allowUncheckedConversion);
         } else if (from instanceof ParameterizedType) {
            Class<?> fromRaw = (Class<?>) ((ParameterizedType) from).getRawType();
            return toClass.isAssignableFrom(fromRaw);
         } else {
            return false;
         }
      } else if (to instanceof ParameterizedType) {
         ParameterizedType toParamType = (ParameterizedType) to;
         Class<?> toRawType = getRawType(toParamType.getRawType());
         if (from instanceof Class) {
            Class<?> fromClass = (Class<?>) from;
            if (!toRawType.isAssignableFrom(fromClass)) {
               // Raw types aren't even compatible? Abort!
               return false;
            }
            if (fromClass.getTypeParameters().length > 0) {
               // Both types are generic, but RHS has no type arguments (e.g. raw). This requires
               // an unchecked conversion.
               return allowUncheckedConversion;
            }
         } else if (from instanceof ParameterizedType) {
            ParameterizedType fromParamType = (ParameterizedType) from;
            Class<?> fromRawType = (Class<?>) fromParamType.getRawType();
            if (!toRawType.isAssignableFrom(fromRawType)) {
               // Raw types aren't even compatible? Abort!
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
         if (resolvedArgs.length == 0) {
            // assigning from raw type to parameterized type requires unchecked conversion
            return allowUncheckedConversion;
         }
         assert args.length == resolvedArgs.length;
         // check each type argument
         for (int i = 0, len = args.length; i < len; i++) {
            Type toArg = args[i];
            Type fromArg = resolvedArgs[i];
            if (toArg instanceof WildcardType) {
               WildcardType wildcardArg = (WildcardType) toArg;
               for (Type upperBound : wildcardArg.getUpperBounds()) {
                  if (!isAssignableReference(upperBound, fromArg, allowUncheckedConversion)) {
                     return false;
                  }
               }
               for (Type lowerBound : wildcardArg.getLowerBounds()) {
                  if (!isAssignableReference(fromArg, lowerBound, allowUncheckedConversion)) {
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
            return fromClass.isArray()
                  && isAssignableReference(toArrayType.getGenericComponentType(),
                        fromClass.getComponentType(), allowUncheckedConversion);
         } else if (from instanceof GenericArrayType) {
            return isAssignableReference(toArrayType.getGenericComponentType(),
                  ((GenericArrayType) from).getGenericComponentType(), allowUncheckedConversion);
         } else {
            return false;
         }
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
            if (!isAssignableReference(bound, from, allowUncheckedConversion)) {
               return false;
            }
         }
         return true;
      } else if (to instanceof TypeVariable) {
         // We don't actually know the type bound to this variable. So we can only assign to it from
         // another instance of the same type variable or some other variable or wildcard that
         // extends it. Both of those cases are handled above (check for `from` being a TypeVariable
         // or WildcardType). So if we get here, it's not assignable.
         return false;
      } else {
         throw new UnknownTypeException(to);
      }
   }
   
   /**
    * A map of primitive types to their direct supertype for purposes of primitive widening
    * conversions. For example, the super-type for {@code byte} is {@code short}, for {@code int}
    * is {@code long}, and for {@code float} is {@code double}.
    * 
    * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10.1">
    *       JLS 4.10.1: Subtyping among Primitive Types</a>
    */
   private static final Map<Class<?>, Class<?>> PRIMITIVE_DIRECT_SUPERTYPES;
   
   /**
    * A map of primitive types to all their primitive subtypes. If a primitive type has no
    * sub-types (for example {@code byte} and {@code boolean}) then it will have no key in this
    * map.
    * 
    * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10.1">
    *       JLS 4.10.1: Subtyping among Primitive Types</a>
    */
   private static final Map<Class<?>, Set<Class<?>>> PRIMITIVE_SUBTYPES;
   
   static {
      // NB: map and set capacities below are specified so that the underlying hash tables will
      // fit all elements without needing to incur an internal resize (using default load factor)
      Map<Class<?>, Class<?>> primitiveSuperTypes = new HashMap<>(8);
      primitiveSuperTypes.put(byte.class, short.class);
      primitiveSuperTypes.put(short.class, int.class);
      primitiveSuperTypes.put(char.class, int.class);
      primitiveSuperTypes.put(int.class, long.class);
      primitiveSuperTypes.put(long.class, float.class);
      primitiveSuperTypes.put(float.class, double.class);
      PRIMITIVE_DIRECT_SUPERTYPES = Collections.unmodifiableMap(primitiveSuperTypes);
      
      Map<Class<?>, Set<Class<?>>> primitiveSubTypes = new HashMap<>(8);
      Set<Class<?>> subTypes = new HashSet<>(2);
      subTypes.add(byte.class);
      primitiveSubTypes.put(short.class, Collections.unmodifiableSet(subTypes));
      subTypes = new HashSet<>(4);
      subTypes.add(byte.class);
      subTypes.add(short.class);
      subTypes.add(char.class);
      primitiveSubTypes.put(int.class, Collections.unmodifiableSet(subTypes));
      subTypes = new HashSet<>(6);
      subTypes.add(byte.class);
      subTypes.add(short.class);
      subTypes.add(char.class);
      subTypes.add(int.class);
      primitiveSubTypes.put(long.class, Collections.unmodifiableSet(subTypes));
      subTypes = new HashSet<>(7);
      subTypes.add(byte.class);
      subTypes.add(short.class);
      subTypes.add(char.class);
      subTypes.add(int.class);
      subTypes.add(long.class);
      primitiveSubTypes.put(float.class, Collections.unmodifiableSet(subTypes));
      subTypes = new HashSet<>(8);
      subTypes.add(byte.class);
      subTypes.add(short.class);
      subTypes.add(char.class);
      subTypes.add(int.class);
      subTypes.add(long.class);
      subTypes.add(float.class);
      primitiveSubTypes.put(double.class, Collections.unmodifiableSet(subTypes));
      PRIMITIVE_SUBTYPES = Collections.unmodifiableMap(primitiveSubTypes);
   }

   /**
    * Determines if one type is a subtype of another. This uses the rules in <em>Subtyping</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10">JLS
    * 4.10</a>) to decide if one type is a subtype of the other.
    *
    * @param aType a type
    * @param possibleSubtype another type
    * @return true if the second type is a subtype of the first
    */
   public static boolean isSubtype(Type aType, Type possibleSubtype) {
      if (isSubtypeStrict(aType, possibleSubtype)) {
         return true;
      }
      if (!(aType instanceof Class) || !(possibleSubtype instanceof Class)) {
         return false;
      }
      return isPrimitiveSubtype((Class<?>) aType, (Class<?>) possibleSubtype);
   }
   
   /**
    * Determines if the given possible subclass is a subtype of the other given class according to
    * the rules of <em>Subtyping among Primitive Types</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-4.10.1">JLS
    * 4.10.1</a>).
    *
    * @param aClass a class, possibly a primitive type
    * @param possibleSubclass another class, possibly a sub-type of the other
    * @return true if the one class is a subtype of the other per primitive subtyping
    */
   private static boolean isPrimitiveSubtype(Class<?> aClass, Class<?> possibleSubclass) {
      Set<Class<?>> subTypes = PRIMITIVE_SUBTYPES.get(aClass);
      return subTypes != null && subTypes.contains(possibleSubclass);
   }
   
   /**
    * Determines if one type is a subtype of another, with restrictions. The restrictions are the
    * same as those observed in calls to {@link Class#isAssignableFrom(Class)}: primitive subtyping
    * rules are ignored. So this will return false if given {code int} and {@code short} (even
    * though {@code short} is a subtype of {@code int} per primitive subtyping rules).
    * 
    * <p>So only the rules defined in <em>Subtyping among Class and Interface Types</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10.2">JLS
    * 4.10.2</a>) and <em>Subtyping among Array Types</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10.3">JLS
    * 4.10.3</a>) are consulted.
    *
    * @param aType a type
    * @param possibleSubtype another type
    * @return true if the second type is a subtype of the first (but not according to primitive
    *       subtyping rules)
    */
   public static boolean isSubtypeStrict(Type aType, Type possibleSubtype) {
      return isAssignableStrict(aType, possibleSubtype)
            && !isSameType(aType, possibleSubtype);
   }

   /**
    * Determines whether two given types represent the same type. This is like testing for the
    * possibility of an <em>Identity Conversion</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.1">JLS
    * 5.1.1</a>).
    * 
    * <p>It behaves the the same as {@link Types#equals(Type, Type)} with one important caveat: two
    * wildcard types, even if they have identical bounds (and therefore are equal), are never the
    * same type. This is because they represent unknown types. Since it cannot be known whether the
    * types they represent are the same, this method assumes they are not. This behavior mirrors
    * that of the annotation processing API's {@linkplain javax.lang.model.util.Types#isSameType(
    * javax.lang.model.type.TypeMirror, javax.lang.model.type.TypeMirror) method of the same name}. 
    *
    * @param a a type
    * @param b another type
    * @return true if they represent the same type
    */
   public static boolean isSameType(Type a, Type b) {
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
         if ((ownerA != null && !isSameType(ownerA, ownerB))
               || !isSameType(ptA.getRawType(), ptB.getRawType())) {
            return false;
         }
         Type argsA[] = ptA.getActualTypeArguments();
         Type argsB[] = ptB.getActualTypeArguments();
         int len = argsA.length;
         if (len != argsB.length) {
            return false;
         }
         for (int i = 0; i < len; i++) {
            // Recursive call uses equals() instead of isSameType(), after ruling out wildcards.
            // If either/both have a wildcard argument, e.g. List<? extends Number>, then they
            // are not the same type (even with same bounds, since the actual type represented
            // by the wildcard is unknown). But with the wildcard pushed down further, they can
            // be the same type. E.g. two types, both List<Class<?>>, are the same type. 
            if (argsA[i] instanceof WildcardType || argsB[i] instanceof WildcardType
                  || !equals(argsA[i], argsB[i])) {
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
         return isSameType(gatA.getGenericComponentType(), gatB.getGenericComponentType());
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
         return false;
      } else {
         // WTF?
         return a.equals(b);
      }
   }
   
   /**
    * Returns the set of direct supertypes for the given type. The direct supertypes are determined
    * using the rules described in <em>Subtyping</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10">JLS
    * 4.10</a>).
    * 
    * <p>If invoked for the type {@code Object}, an empty array is returned.
    *
    * @param type a type
    * @return its direct supertypes
    */
   public static Type[] getDirectSupertypes(Type type) {
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>) type;
         if (clazz.isPrimitive()) {
            Class<?> superType = PRIMITIVE_DIRECT_SUPERTYPES.get(clazz);
            return superType == null ? EMPTY_TYPES : new Type[] { superType };
         }
      }
      
      if (isArray(type)) {
         Type componentType = getComponentType(type);
         if (componentType == Object.class) {
            // base case supertypes for Object[]
            return ARRAY_SUPERTYPES.clone();
         }
         // create array types for all of the element's supertypes
         Type[] superTypes = getDirectSupertypes(componentType);
         for (int i = 0; i < superTypes.length; i++) {
            superTypes[i] = arrayTypeOf(superTypes[i]);
         }
         return superTypes;
      }
      
      Type superClass;
      Type[] superInterfaces;
      Type rawType;
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>) type;
         if (isGeneric(clazz)) {
            // raw type use
            superClass = clazz.getSuperclass();
            superInterfaces = clazz.getInterfaces();
            rawType = null;
         } else {
            superClass = clazz.getGenericSuperclass();
            superInterfaces = clazz.getGenericInterfaces();
            rawType = null;
         }
      } else {
         superClass = getGenericSuperclass(type);
         superInterfaces = getGenericInterfaces(type);
         // a raw type is a supertype of a parameterized type 
         rawType = type instanceof ParameterizedType ? getRawType(type) : null;
      }
      if (superClass == null && superInterfaces.length == 0 && isInterface(type)) {
         // direct supertype of an interface that has no direct super-interfaces is Object
         superClass = Object.class;
      }

      // now construct array of results
      if (superClass == null && rawType == null) {
         return superInterfaces;
      }
      int addl = 0;
      if (superClass != null) {
         addl++;
      }
      if (rawType != null) {
         addl++;
      }
      Type[] superTypes = new Type[superInterfaces.length + addl];
      if (superClass != null) {
         superTypes[0] = superClass;
         System.arraycopy(superInterfaces, 0, superTypes, 1, superInterfaces.length);
      } else {
         System.arraycopy(superInterfaces, 0, superTypes, 0, superInterfaces.length);
      }
      if (rawType != null) {
         superTypes[superTypes.length - 1] = rawType;
      }
      return superTypes;
   }
   
   /**
    * Determines if the given type is a generic type. The only types that are <em>not</em> generic
    * are {@code Class} tokens for non-parameterizable types (e.g. classes and interfaces with no
    * type arguments).
    * 
    * <p>If a class has no type arguments but is a non-static member of a class that <em>does</em>
    * have type arguments, that class is parameterizable and thus generic.
    *
    * @param type a type
    * @return true if the type is a generic type
    */
   public static boolean isGeneric(Type type) {
      return !(type instanceof Class) || isGeneric((Class<?>) type);
   }
   
   /**
    * Determines if the given class is generic. A generic class is one that has type arguments or
    * that is a non-static member of a generic class.
    *
    * @param clazz a class
    * @return true if the class is a generic type
    */
   public static boolean isGeneric(Class<?> clazz) {
      if (clazz.getTypeParameters().length > 0) {
         return true;
      }
      if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
         Class<?> enclosing = clazz.getEnclosingClass();
         assert enclosing != null;
         return isGeneric(enclosing);
      }
      return false;
   }

   /**
    * Returns the set of all supertypes for the given type. This returned set has the same elements
    * as if {@link #getDirectSupertypes(Type)} were invoked, and then repeatedly invoked recursively
    * on the returned supertypes until the entire hierarchy is exhausted (e.g. reach {@code Object},
    * which has no supertypes).
    * 
    * <p>This method uses a breadth-first search and returns a set with deterministic iteration
    * order so that types "closer" to the given type appear first when iterating through the set.
    * 
    * <p>If invoked for the type {@code Object}, an empty set is returned.
    *
    * @param type a type
    * @return the set all of the given type's supertypes
    */
   public static Set<Type> getAllSupertypes(Type type) {
      return Collections.unmodifiableSet(getAllSupertypesInternal(type, false, false));
   }
   
   /**
    * Breadth-first searches the type hierarchy, returning all supertypes for the given type. If
    * so instructed, the returned set will also include the given type. If so instructed, all types
    * in the returned set will be erased (e.g. raw) types.
    *
    * @param type a type
    * @param includeType if true, the given type is included in the returned set
    * @param erased if true, only erasures for the supertypes are included in the returned set
    * @return the set of all of the given type's supertypes
    */
   private static Set<Type> getAllSupertypesInternal(Type type, boolean includeType,
         boolean erased) {
      Queue<Type> pending = new ArrayDeque<>();
      if (erased) {
         type = getRawType(type);
      }
      pending.add(type);
      Set<Type> results = new LinkedHashSet<>();
      while (!pending.isEmpty()) {
         Type t = pending.poll();
         if ((t == type && !includeType) || results.add(t)) {
            Type[] superTypes = getDirectSupertypes(t);
            for (Type st : superTypes) {
               pending.add(erased ? getRawType(st) : st);
            }
         }
      }
      return results;
   }
   
   private static Set<Class<?>> getAllErasedSupertypesInternal(Type type, boolean includeType) {
      @SuppressWarnings({"unchecked", "rawtypes"}) // passing erased=true means only Class tokens
                                                   // put into resulting set, so cast is safe
      Set<Class<?>> ret = (Set) getAllSupertypesInternal(type, includeType, true);
      return ret;
   }

   /**
    * Computes least upper bounds for the given array of types. This is a convenience method that is
    * shorthand for {@code Types.getLeastUpperBounds(Arrays.asList(types))}.
    *
    * @param types the types whose least upper bounds are computed
    * @return the least upper bounds for the given types
    * 
    * @see #getLeastUpperBounds(Iterable)
    */
   public static Type[] getLeastUpperBounds(Type... types) {
      return getLeastUpperBounds(Arrays.asList(types));
   }

   /**
    * Computes least upper bounds for the given types. The algorithm used is detailed in
    * <em>Least Upper Bound</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10.4">JLS
    * 4.10.4</a>).
    * 
    * <p>The least upper bounds can include up to one class type but multiple interface types. When
    * the bounds include a class type, it will be the first element of the given array (and all
    * subsequent elements interface types).
    * 
    * <p>The JLS indicates that recursive types, which could result in infinite recursion in
    * computing the last upper bounds, should result in cyclic data structures. Core reflection type
    * interfaces are not designed to model such cycles. So if the computed least upper bound
    * {@code `lub`} were, for example, to result in {@code Comparable<`lub`>} then a non-cyclic
    * type, {@code Comparable<?>} would be returned instead.
    *
    * @param types the types whose least upper bounds are computed
    * @return the least upper bounds for the given types
    */
   public static Type[] getLeastUpperBounds(Iterable<Type> types) {
      Iterator<Type> iter = types.iterator();
      if (!iter.hasNext()) {
         throw new IllegalArgumentException();
      }
      Type first = iter.next();
      if (!iter.hasNext()) {
         // if just one type given, it is its own least upper bound
         return new Type[] { first };
      }
      
      // Move the types into a set to de-dup. Even if given iterable is a set, we still do this so
      // we have a defensive copy of the set for all subsequent operations.
      OptionalInt numTypes = Iterables.trySize(types);
      Set<Type> typesSet = new LinkedHashSet<>(numTypes.orElse(6) * 4 / 3);
      typesSet.add(first);
      Iterables.addTo(iter, typesSet);
      if (typesSet.size() == 1) {
         // all other types were duplicates of the first, so least upper bound is the one type
         return new Type[] { first };
      }
      
      return leastUpperBounds(typesSet, new HashSet<>());
   }

   /**
    * Computes least upper bounds for the given types, using the given sets to track recursion and
    * prevent recursive types from causing infinite recursion.
    *
    * @param types the types whose least upper bounds are computed
    * @param setsSeen sets of types already observed, to prevent infinite recursion
    * @return the least upper bounds for the given types
    */
   private static Type[] leastUpperBounds(Set<Type> types, Set<Set<Type>> setsSeen) {
      return leastUpperBounds(types, setsSeen, false);
   }

   /**
    * Computes least upper bounds for the given types or reduces them via similar logic.
    * 
    * <p>In addition to computing least upper bounds, it can also just reduce the given types, using
    * the same logic in computing least upper bounds (as if given types are the intersection of
    * supertypes and then computing the <em>minimal erased candidate set</em> and the best
    * <em>candidate parameterization</em> for candidates that are generic types).
    * 
    * <p>This also uses another given set to track recursions and thereby prevent cycles from
    * causing infinite recursion. If a cycle is detected, a least upper bound of {@code Object} is
    * returned instead of trying to construct cyclic data structures.
    *
    * @param types the types whose least upper bounds are computed (or are reduced)
    * @param setsSeen sets of types already observed, to prevent infinite recursion
    * @param reduceTypesDirectly if true, just reduce the given types instead of computing their
    *       least upper bounds
    * @return the least upper bounds for the given types (or the reduction of the given types using
    *       similar logic)
    */
   private static Type[] leastUpperBounds(Set<Type> types, Set<Set<Type>> setsSeen,
         boolean reduceTypesDirectly) {
      if (!setsSeen.add(types)) {
         // Already seen these types? That means we have recursive types, like
         // Foo extends Comparable<Foo>, and the least upper bound wants to also be recursive
         // (e.g. `lub` == Comparable<`lub`>). The JLS says compilers must model this situation
         // with cyclic data structures. But this is core reflection, not a compiler. We don't
         // want cyclic types because they'd likely cause infinite recursion with algorithms that
         // expect (for good reason) reflection types to be a dag or a tree. So instead of a cyclic
         // data structure, we terminate the cycle with the mother-of-all-upper-bounds: Object
         return JUST_OBJECT.clone();
      }
      
      // Build erased candidate set
      Set<Class<?>> candidateSet;
      if (reduceTypesDirectly) {
         candidateSet = new LinkedHashSet<>(types.size() * 4 / 3);
         for (Type t : types) {
            candidateSet.add(getRawType(t));
         }
      } else {
         Iterator<Type> iter = types.iterator();
         Type first = iter.next();
         candidateSet = getAllErasedSupertypesInternal(first, true);
         while (iter.hasNext()) {
            Set<Class<?>> nextCandidates = getAllErasedSupertypesInternal(iter.next(), true);
            candidateSet.retainAll(nextCandidates);
         }
      }
      
      if (candidateSet.isEmpty()) {
         // this can only happen if given types include a mix of reference and non-reference types
         assert StreamSupport.stream(types.spliterator(), false)
               .anyMatch(t -> getRawType(t).isPrimitive());
         assert StreamSupport.stream(types.spliterator(), false)
               .anyMatch(t -> !getRawType(t).isPrimitive());
         throw new IllegalArgumentException(
               "cannot compute least upper bounds: mix of reference and primitive types given");
      }
      
      // Now compute "minimal candidate set" by filtering out redundant supertypes
      for (Iterator<Class<?>> csIter = candidateSet.iterator(); csIter.hasNext();) {
         Class<?> t1 = csIter.next();
         for (Class<?> t2 : candidateSet) {
            if (t1 == t2) {
               continue;
            }      
            if (isSubtype(t2, t1)) {
               csIter.remove();
               break;
            }
         }
      }
      
      // Determine final result by computing parameters for any generic supertypes
      Type[] results = new Type[candidateSet.size()];
      int index = 0;
      for (Class<?> rawCandidate : candidateSet) {
         Type parameterizedCandidate;
         if (!isGeneric(rawCandidate)) {
            // not generic, so no type parameters to compute
            parameterizedCandidate = rawCandidate;
         } else {
            // we have to compute type parameter values
            parameterizedCandidate = null;
            for (Type t : types) {
               Type resolved;
               if (reduceTypesDirectly) {
                  if (getRawType(t) != rawCandidate) {
                     continue;
                  }
                  resolved = t;
               } else {
                  resolved = resolveSuperType(t, rawCandidate);
                  assert resolved != null;
               }
               if (resolved instanceof Class) {
                  // raw type use trumps, so skip parameters
                  parameterizedCandidate = resolved;
                  break;
               }
               assert resolved instanceof ParameterizedType;
               
               // reduce this resolution and the current parameterization
               if (parameterizedCandidate == null) {
                  parameterizedCandidate = resolved;
               } else {
                  parameterizedCandidate = leastContainingInvocation(
                        (ParameterizedType) parameterizedCandidate, (ParameterizedType) resolved,
                        setsSeen);
               }
            }
            assert parameterizedCandidate != null;
         }
         results[index++] = parameterizedCandidate;
      }
      
      // should not end up with more than one class in the results
      assert Arrays.stream(results).filter(Types::isInterface).count() >= results.length - 1;
      
      for (int i = 0; i < results.length; i++) {
         if (!isInterface(results[i])) {
            if (i > 0) {
               // move class to beginning of array and shift the rest down
               Type tmp = results[i];
               System.arraycopy(results, 0, results, 1, i);
               results[0] = tmp;
            }
            break;
         }
      }
      return results;
   }
   
   /**
    * Computes {@code lcp()}, "least containing invocation", for the given relevant
    * parameterizations (as described in JLS 4.10.4).
    *
    * @param t1 a relevant parameterization
    * @param t2 another relevant parameterization
    * @param setsSeen sets of types already observed during calculation of least upper bounds, to
    *       prevent infinite recursion
    * @return the most specific parameterization that contains both given
    */
   private static Type leastContainingInvocation(ParameterizedType t1, ParameterizedType t2,
         Set<Set<Type>> setsSeen) {
      assert t1.getRawType() == t2.getRawType();
      Type pt1Owner = t1.getOwnerType();
      Type pt2Owner = t2.getOwnerType();
      Type resultOwner;
      assert (pt1Owner == null) == (pt2Owner == null);
      if (pt1Owner != null) {
         if (pt1Owner instanceof Class) {
            resultOwner = pt1Owner;
         } else if (pt2Owner instanceof Class) {
            resultOwner = pt2Owner;
         } else {
            resultOwner = leastContainingInvocation((ParameterizedType) pt1Owner,
                  (ParameterizedType) pt2Owner, setsSeen);
         }
      } else {
         resultOwner = null;
      }
      Type[] pt1Args = t1.getActualTypeArguments();
      Type[] pt2Args = t2.getActualTypeArguments();
      assert pt1Args.length == pt2Args.length;
      Type[] resultArgs = new Type[pt1Args.length];
      for (int i = 0; i < pt1Args.length; i++) {
         resultArgs[i] = leastContainingTypeArgument(pt1Args[i], pt2Args[i], setsSeen);
      }
      return new ParameterizedTypeImpl(resultOwner, t1.getRawType(), resultArgs);
   }
   
   /**
    * Computes {@code lcta()}, "least containing type argument", for the given type argument values
    * (as described in JLS 4.10.4).
    *
    * @param t1 a value for the type argument being resolved
    * @param t2 another value for the type argument being resolved
    * @param setsSeen sets of types already observed during calculation of least upper bounds, to
    *       prevent infinite recursion
    * @return the most specific type argument that contains both the given values
    */
   private static Type leastContainingTypeArgument(Type t1, Type t2, Set<Set<Type>> setsSeen) {
      if (t1 instanceof WildcardType) {
         return leastContainingTypeArgument((WildcardType) t1, t2, setsSeen);
      } else if (t2 instanceof WildcardType) {
         return leastContainingTypeArgument((WildcardType) t2, t1, setsSeen);
      } else {
         // JLS: lcta(U, V) = U if U = V, otherwise ? extends lub(U, V)
         Set<Type> asSet = new LinkedHashSet<Type>(4);
         asSet.add(t1);
         asSet.add(t2);
         if (asSet.size() == 1) {
            return t1;
         }
         Type[] lubs = leastUpperBounds(asSet, setsSeen);
         return new WildcardTypeImpl(lubs, EMPTY_TYPES);
      }
   }

   /**
    * Computes {@code lcta()} for type arguments when one is a wildcard type (as described in JLS
    * 4.10.4).
    *
    * @param t1 a wildcard type value for the type argument being resolved
    * @param t2 another value (may or may not be a wildcard) for the type argument being resolved
    * @param setsSeen sets of types already observed during calculation of least upper bounds, to
    *       prevent infinite recursion
    * @return the most specific type argument that contains both the given values
    */
   private static Type leastContainingTypeArgument(WildcardType t1, Type t2, 
         Set<Set<Type>> setsSeen) {
      Type[] superBounds = t1.getLowerBounds();
      Type[] extendsBounds = t1.getUpperBounds();
      assert extendsBounds.length > 0;
      
      // Lower bounds
      if (superBounds.length != 0) {
         if (extendsBounds.length > 1 || extendsBounds[0] != Object.class) {
            // a wildcard with both super and extends: treat as if it were "?"
            superBounds = EMPTY_TYPES;
            extendsBounds = JUST_OBJECT;
         } else {
            if (t2 instanceof WildcardType) {
               Type[] superBounds2 = t1.getLowerBounds();
               Type[] extendsBounds2 = t1.getUpperBounds();
               assert extendsBounds2.length > 0;
               if (superBounds2.length != 0) {
                  if (extendsBounds2.length > 1 || extendsBounds2[0] != Object.class) {
                     // a wildcard with both super and extends: treat as if it were "?"
                     superBounds2 = EMPTY_TYPES;
                     extendsBounds2 = JUST_OBJECT;
                     // fall-through...
                  } else {
                     // JLS: lcta(? super U, ? super V) = ? super glb(U, V)
                     for (Type st : superBounds2) {
                        superBounds = greatestLowerBounds(superBounds, st);
                     }
                     return new WildcardTypeImpl(extendsBounds, superBounds);
                  }
               }
               // JLS: lcta(? extends U, ? super V) = U if U = V, otherwise ?
               if (superBounds.length == 1 && extendsBounds2.length == 1
                     && equals(superBounds[0], extendsBounds2[0])) {
                  return superBounds[0];
               }
               return extendsAnyWildcard();
            } else {
               // JLS: lcta(U, ? super V) = ? super glb(U, V)
               superBounds = greatestLowerBounds(superBounds, t2);
               return new WildcardTypeImpl(extendsBounds, superBounds);
            }
         }
      }
      
      // Upper bounds
      if (t2 instanceof WildcardType) {
         Type[] superBounds2 = t1.getLowerBounds();
         Type[] extendsBounds2 = t1.getUpperBounds();
         assert extendsBounds2.length > 0;
         if (superBounds2.length != 0) {
            if (extendsBounds2.length > 1 || extendsBounds2[0] != Object.class) {
               // a wildcard with both super and extends: treat as if it were "?"
               superBounds2 = EMPTY_TYPES;
               extendsBounds2 = JUST_OBJECT;
               // fall-through...
            } else {
               // JLS: lcta(? extends U, ? super V) = U if U = V, otherwise ?
               if (superBounds2.length == 1 && extendsBounds.length == 1
                     && equals(superBounds2[0], extendsBounds[0])) {
                  return superBounds2[0];
               }
               return extendsAnyWildcard();
            }
         }
         // JLS: lcta(? extends U, ? extends V) = ? extends lub(U, V)
         Set<Type> typeSet =
               new LinkedHashSet<>((extendsBounds.length + extendsBounds2.length) * 4 /3);
         for (Type t : extendsBounds) {
            typeSet.add(t);
         }
         for (Type t : extendsBounds2) {
            typeSet.add(t);
         }
         Type[] lub = leastUpperBounds(typeSet, setsSeen);
         return new WildcardTypeImpl(lub, EMPTY_TYPES);
      } else {
         // JLS: lcta(U, ? extends V) = ? extends lub(U, V)
         Set<Type> typeSet =
               new LinkedHashSet<>((extendsBounds.length + 1) * 4 /3);
         for (Type t : extendsBounds) {
            typeSet.add(t);
         }
         typeSet.add(t2);
         Type[] lub = leastUpperBounds(typeSet, setsSeen);
         return new WildcardTypeImpl(lub, EMPTY_TYPES);
      }
   }
   
   private static Type[] greatestLowerBounds(Type[] bounds, Type newBound) {
      // Section 5.1.10 of the JLS seems to define the glb function as a simple intersection of the
      // given arguments. But we also need to reduce the set to eliminate redundant types (where one
      // is a sub-type of another).
      
      // NB: Intersections cannot contain conflicting types; e.g. two classes (not interfaces) where
      // one is *not* a sub-type of another OR different parameterizations of the same generic type.
      // The natural resolution when faced with a conflict would be a union type, but there is no
      // such thing in Java (at least not in core reflection). So instead, we merge incompatible
      // types via least-upper-bounds. So an attempt to intersect String and Class results in
      // Serializable since String & Class is not possible. Similarly, an attempt to intersect
      // List<String> and List<Class<?>> results in List<? extends Serializable>.
      
      Set<Type> asSet = new LinkedHashSet<>((bounds.length + 1) * 4 / 3);
      for (Type t : bounds) {
         asSet.add(t);
      }
      asSet.add(newBound);
      if (asSet.size() == 1) {
         return new Type[] { newBound };
      } else {
         // instead of gathering super-types and reducing to least upper bounds, the last argument
         // being "true" means this will just reduce the given input types
         return leastUpperBounds(asSet, new HashSet<>(), true);
      }
   }

   /**
    * Returns true if the given type represents a functional interface. A functional interface is an
    * interface type that has exactly one abstract method. Default methods on an interface are not
    * counted as abstract.
    * 
    * <p>This applies the rules defined in 
    *
    * @param type
    * @return
    */
   // TODO: javadoc
   // JLS 9.8
   public static boolean isFunctionalInterface(Type type) {
      if (!(type instanceof Class || type instanceof ParameterizedType)) {
         return false;
      }
      if (!isInterface(type)) {
         return false;
      }
      // TODO: implement me!
      return false;
   }

   /**
    * Resolves the given type variable in the context of the given type. For example, if the given
    * type variable is {@code Collection.<E>} and the given type is the parameterized type
    * {@code List<Optional<String>>}, then this will return {@code Optional<String>}.
    * 
    * <p>If the given type variable cannot be resolved then {@code null} is returned. For example,
    * if the type variable given is {@code Map.<K>} and the given type is {@code List<Number>}, then
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
    * {@code Map<? extends String, ? extends List<String>>}.
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
         throw new UnknownTypeException(type);
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
      // wrap the variable to make sure its hashCode and equals are well-behaved
      HashMap<TypeVariableWrapper, Type> resolvedVariables = new HashMap<>();
      resolvedVariables.put(wrap(typeVariable), typeValue);
      // extract additional context from the given type, in case it has resolved type arguments
      // necessary for validating bounds of given type variables
      collectTypeParameters(type, resolvedVariables);
      // check type bounds
      checkTypeValue(typeVariable, typeValue, resolvedVariables);
      return replaceTypeVariablesInternal(type, resolvedVariables);
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
      HashMap<TypeVariableWrapper, Type> resolvedVariables =
            new HashMap<>(typeVariables.size() * 4 / 3);
      for (Entry<TypeVariable<?>, Type> entry : typeVariables.entrySet()) {
         TypeVariable<?> typeVariable = entry.getKey();
         Type typeValue = entry.getValue();
         resolvedVariables.put(wrap(typeVariable), typeValue);
      }
      // extract additional context from the given type, in case it has resolved type arguments
      // necessary for validating bounds of given type variables
      collectTypeParameters(type, resolvedVariables);
      // check type bounds
      for (Entry<TypeVariable<?>, Type> entry : typeVariables.entrySet()) {
         checkTypeValue(entry.getKey(), entry.getValue(), resolvedVariables);
      }
      return replaceTypeVariablesInternal(type, resolvedVariables);
   }

   private static Type replaceTypeVariablesInternal(Type type,
         Map<TypeVariableWrapper, Type> typeVariables) {
      return replaceTypeVariablesInternal(type, typeVariables, false);
   }

   private static Type replaceTypeVariablesInternal(Type type,
         Map<TypeVariableWrapper, Type> typeVariables, boolean wildcardForMissingTypeVars) {
      // NB: if replacing variable references in this type result in no changes (e.g. no references
      // to replace), then return the given type object as is. Do *not* return a different-but-equal
      // type since that could cause a lot of excess work to re-construct a complex type for a
      // no-op replacement operation.
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
         TypeVariable<?> typeVar = (TypeVariable<?>) type;
         Type resolvedType = typeVariables.get(wrap(typeVar));
         if (resolvedType == null) {
            if (wildcardForMissingTypeVars) {
               resolvedType = new WildcardTypeImpl(Arrays.asList(typeVar.getBounds()),
                     Collections.emptyList());
            } else {
               resolvedType = type;
            }
         }
         return resolvedType;
         
      } else {
         throw new UnknownTypeException(type);
      }
   }

   private static void checkTypeValue(TypeVariable<?> variable, Type argument,
         Map<TypeVariableWrapper, Type> resolvedVariables) {
      checkTypeValue(variable, argument, resolvedVariables, -1);
   }

   private static void checkTypeValue(TypeVariable<?> variable, Type argument,
         Map<TypeVariableWrapper, Type> resolvedVariables, int index) {
      String id = index < 0 ? "" : (" #" + index);
      if (argument instanceof Class && ((Class<?>) argument).isPrimitive()) {
         throw new IllegalArgumentException("Type argument" + id + " is primitive: "
               + argument.getTypeName());
      }
      for (Type bound : variable.getBounds()) {
         // Do any substitutions on owner type variables that may be referenced (or recursive
         // bounds, where a bound references its type variable). If any type variables cannot be
         // resolved, substitute them with an appropriate wildcard
         bound = replaceTypeVariablesInternal(bound, resolvedVariables, true);
         if (!isAssignableStrict(bound, argument)) {
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
         // If this is a raw type reference to a generic type, just forget generic type information
         // and use raw types from here on out.
         // NOTE: This is how the Java compiler works. However, it would be nice to support a case
         // like so:
         //    interface X<T> extends List<String> { ... }
         //    X x; // raw type
         //    List<String> l = x; // generates an unchecked cast warning!
         // The Java compiler insists that the last line is an unchecked cast since raw types cause
         // it to ignore generic type information. However, the missing type parameter to X, in this
         // example, shouldn't really matter. We statically know, even without the type arg, that
         // the assignment is safe.
         // TODO: change this to behave in the more intuitive way instead of mimic'ing javac?
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
         return resolvedComponentType == null ? null : arrayTypeOf(resolvedComponentType);
         
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
         throw new UnknownTypeException(type);
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
      Class<?> actualInterfaces[] = clazz.getInterfaces();
      Type genericInterfaces[] = clazz.getGenericInterfaces();
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
      if (componentType == void.class) {
         throw new IllegalArgumentException("Cannot create an array with component type void");
      }
      boolean canLoad = canLoad(componentType);
      if (canLoad || CLASS_FORNAME0 != null) {
         // We should be able to use Class.forName (or a variant thereof) to load the class, so
         // build the class name.
         StringBuilder arrayClassName = new StringBuilder("[");
         if (componentType.isPrimitive()) {
            String symbol = PRIMITIVE_CLASS_SYMBOLS.get(componentType);
            assert symbol != null;
            arrayClassName.append(symbol);
         } else if (componentType.isArray()) {
            arrayClassName.append(componentType.getName());
         } else {
            arrayClassName.append("L").append(componentType.getName()).append(";");
         }
         String className = arrayClassName.toString();
         if (canLoad) {
            try {
               // Try loading the class using Class.forName(...). This might fail if we have a
               // misbehaving class loader.
               @SuppressWarnings("unchecked") // we'll verify the type before returning it
               Class<T[]> arrayType = (Class<T[]>) Class.forName(className);
               if (arrayType.getComponentType() == componentType) {
                  return arrayType;
               }
            } catch (Exception e) {
               // intentional fall-through
            }
         }
         if (CLASS_FORNAME0 != null) {
            try {
               // Either we can't load the class due to its class loader not being in our class
               // loader hierarchy, or we encountered a problem trying to load. Be sneaky and try
               // using a private native Class.forName0(...), which allows specifying the class
               // loader.
               @SuppressWarnings("unchecked") // we'll verify the type before returning it
               Class<T[]> arrayType = (Class<T[]>)
                     CLASS_FORNAME0.invoke(null, className, true, componentType.getClassLoader());
               if (arrayType.getComponentType() == componentType) {
                  // this test should really always be true
                  return arrayType;
               }
            } catch (Exception e) {
               // intentional fall-through
            }
         }
      }
      // Fallback approach requires reflective array creation
      @SuppressWarnings("unchecked") // if it's not right then Array.newInstance is broken!
      Class<T[]> arrayType = (Class<T[]>) Array.newInstance(componentType, 0).getClass();
      return arrayType;
   }
   
   private static boolean canLoad(Class<?> clazz) {
      // Can we load the given class using Class.forName? We can if its class loader is the same as
      // or is an ancestor of our class loader.
      ClassLoader target = clazz.getClassLoader();
      if (target == null) {
         // the class was loaded by boot class loader, so we're good
         return true;
      }
      for (ClassLoader current = Types.class.getClassLoader(); current != null;
            current = current.getParent()) {
         if (current.equals(target)) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Returns an array type with the given component type. If the given component type is a simple
    * class token, then a class token that represents an array of that type is returned. Otherwise,
    * a generic array type is returned.
    * 
    * <p>This is a convenience method that will delegate to {@link #getArrayType(Class)} or
    * {@link #newGenericArrayType(Type)}, depending on the type of the argument.
    *
    * @param componentType the component type
    * @return a type that represents an array of the given component type
    */
   public static Type arrayTypeOf(Type componentType) {
      return componentType instanceof Class
               ? getArrayType((Class<?>) componentType)
               : newGenericArrayType(componentType);
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
      List<Type> copyOfArguments = new ArrayList<>(typeArguments); // defensive copy
      for (Type type : copyOfArguments) {
         requireNonNull(type);
      }
      Type owner;
      if (ownerType != null) {
         if (rawType.getDeclaringClass() != getRawType(ownerType)) {
            throw new IllegalArgumentException("Owner type " + ownerType.getTypeName() + " does not"
                  + " match actual owner of given raw type " + rawType.getTypeName());
         } else if (Modifier.isStatic(rawType.getModifiers())) {
            throw new IllegalArgumentException("Given raw type " + rawType.getTypeName()
                  + " is static so cannot have a parameterized owner type");
         }
         owner = ownerType;
      } else if (copyOfArguments.isEmpty()) {
         throw new IllegalArgumentException("Parameterized type must either have type arguments or "
               + "have a parameterized owner type");
      } else {
         for (Class<?> clazz = rawType, enclosing = null; clazz != null; clazz = enclosing) {
            enclosing = clazz.getDeclaringClass();
            if (Modifier.isStatic(clazz.getModifiers())) {
               break;
            }
            if (enclosing != null && enclosing.getTypeParameters().length > 0) {
               throw new IllegalArgumentException("Non-static parameterized type "
                  + rawType.getTypeName() + " must have parameterized owner");
            }
         }
         owner = rawType.getDeclaringClass();
      }
      TypeVariable<?> typeVariables[] = rawType.getTypeParameters();
      int len = typeVariables.length;
      if (len != copyOfArguments.size()) {
         throw new IllegalArgumentException("Given type " + rawType.getTypeName() + " has " + len
               + " type variable(s), but " + copyOfArguments.size()
               + " argument(s) were specified");
      }
      Map<TypeVariableWrapper, Type> resolvedVariables = new HashMap<>();
      if (ownerType != null) {
         // resolve owners' type variables
         collectTypeParameters(ownerType, resolvedVariables);
      }
      for (int i = 0; i < len; i++) {
         // add current type variables, in case there are recursive bounds
         resolvedVariables.put(wrap(typeVariables[i]), copyOfArguments.get(i));
      }
      for (int i = 0; i < len; i++) {
         // validate that given arguments are compatible with bounds
         TypeVariable<?> variable = typeVariables[i];
         Type argument = copyOfArguments.get(i);
         checkTypeValue(variable, argument, resolvedVariables, i);
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
   
   private static void collectTypeParameters(Type type,
         Map<TypeVariableWrapper, Type> typeParameters) {
      if (type instanceof ParameterizedType) {
         collectTypeParameters((ParameterizedType) type, typeParameters);
      } else if (type instanceof GenericArrayType) {
         collectTypeParameters(((GenericArrayType) type).getGenericComponentType(), typeParameters);
      } else if (type instanceof WildcardType) {
         WildcardType wt = (WildcardType) type;
         for (Type b : wt.getUpperBounds()) {
            collectTypeParameters(b, typeParameters);
         }
         for (Type b : wt.getLowerBounds()) {
            collectTypeParameters(b, typeParameters);
         }
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
    * Returns a {@link WildcardType} with an upper bound of {@code Object}, i.e.&nbsp;{@code ?}.
    *
    * @return the wildcard type with an open upper bound of {@code Object}
    */
   public static WildcardType extendsAnyWildcard() {
      return EXTENDS_ANY;
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
   static class ParameterizedTypeImpl implements ParameterizedType, Serializable {
      private static final long serialVersionUID = -4933098144775956311L;

      private final Type ownerType;
      private final Type rawType;
      private final Type[] typeArguments;

      ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
         this.ownerType = ownerType;
         this.rawType = rawType;
         this.typeArguments = typeArguments;
      }
      
      ParameterizedTypeImpl(Type ownerType, Type rawType,
            Collection<? extends Type> typeArguments) {
         this.ownerType = ownerType;
         this.rawType = rawType;
         this.typeArguments = typeArguments.toArray(new Type[typeArguments.size()]);
      }

      @Override
      public Type[] getActualTypeArguments() {
         return typeArguments.clone();
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
   // NB: not private so its visible for use from AnnotatedTypes
   static class WildcardTypeImpl implements WildcardType, Serializable {
      private static final long serialVersionUID = -5371665313248454547L;
      
      private final Type upperBounds[];
      private final Type lowerBounds[];
      
      WildcardTypeImpl(Type bound, boolean isUpperBound) {
         upperBounds = new Type[] { isUpperBound ? bound : Object.class };
         lowerBounds = isUpperBound ? EMPTY_TYPES : new Type[] { bound };
      }

      WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
         assert upperBounds.length > 0; // at a minimum, it contains Object
         this.upperBounds = upperBounds;
         this.lowerBounds = lowerBounds;
      }

      WildcardTypeImpl(Collection<? extends Type> upperBounds,
            Collection<? extends Type> lowerBounds) {
         this.upperBounds = upperBounds.isEmpty() ? JUST_OBJECT
               : upperBounds.toArray(new Type[upperBounds.size()]);
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
