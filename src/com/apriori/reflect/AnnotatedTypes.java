package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import com.apriori.collections.Iterables;
import com.apriori.collections.MapBuilder;
import com.apriori.collections.TransformingCollection;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
/**
 * Numerous utility methods for using, constructing, and inspecting annotated types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotatedType
 */
// TODO: uncomment stuff and implement it!
// TODO: tests, javadoc
public final class AnnotatedTypes {
   
   static final AnnotatedType OBJECT = newAnnotatedType(Object.class);
   static final AnnotatedType EMPTY_TYPES[] = new AnnotatedType[0];
   private static final AnnotatedTypeVariable EMPTY_TYPE_VARIABLES[] = new AnnotatedTypeVariable[0];
   private static final AnnotatedType ARRAY_INTERFACES[] = new AnnotatedType[] {
      newAnnotatedType(Cloneable.class), newAnnotatedType(Serializable.class)
   };
   private static final Annotation EMPTY_ANNOTATIONS[] = new Annotation[0];

   private AnnotatedTypes() {}
   
   /**
    * Returns the component type of the given array type. If the given type does not represent an
    * array type then {@code null} is returned.
    *
    * @param type a generic type
    * @return the component type of given array type or {@code null} if the given type does not
    *       represent an array type
    */
   public static AnnotatedType getComponentType(AnnotatedType type) {
      requireNonNull(type);
      if (type instanceof AnnotatedArrayType) {
         return ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
      } else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = type instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) type).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) type).getAnnotatedBounds();
         if (bounds.length > 0) {
            AnnotatedType componentType = getComponentType(bounds[0]);
            // We synthesize a new wildcard type. So a wildcard type <? extends Number[]> will
            // return a component type of <? extends Number> instead of simply Number. Similarly, a
            // type variable <T extends Number[]> returns a component type of <? extends Number>.
            return componentType != null ? newExtendsAnnotatedWildcardType(componentType) : null;
         }
      }
      // type is not an array type
      return null;
   }
   
   public static AnnotatedType getErasedType(AnnotatedType type) {
      return newAnnotatedType(Types.getErasure(type.getType()), type.getDeclaredAnnotations());
   }

   /**
    * Returns the generic superclass of the given type. If the given type is one of the eight
    * primitive types or {@code void}, if it is {@code Object}, or if it is an interface then
    * {@code null} is returned. If the given type is an array type then {@code Object} is the
    * returned superclass.
    * 
    * <p>If the given type is a wildcard or type variable then its first upper bound, if not an
    * interface, is its superclass. If the first upper bound is an interface then, like other
    * interface types, {@code null} is returned.
    *
    * @param type a generic type
    * @return the superclass of the given type
    * 
    * @see Class#getAnnotatedSuperclass()
    */
   public static AnnotatedType getAnnotatedSuperclass(AnnotatedType type) {
      // TODO: implement and revise doc!
      return null;
//      requireNonNull(type);
//      if (type instanceof Class) {
//         return ((Class<?>) type).getGenericSuperclass();
//      } else if (type instanceof ParameterizedType) {
//         Class<?> superClass = getRawType(((ParameterizedType) type).getRawType()).getSuperclass();
//         if (superClass == null) {
//            return null;
//         }
//         Type superType = resolveSuperType(type, superClass);
//         assert superType != null;
//         return superType;
//      } else if (type instanceof GenericArrayType) {
//         return Object.class;
//      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
//         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
//               : ((TypeVariable<?>) type).getBounds();
//         assert bounds.length > 0;
//         return isInterface(bounds[0]) ? null : bounds[0];
//      } else {
//         throw new IllegalArgumentException("Unrecognized Type: " + type);
//      }
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
    * @param type a generic type
    * @return the interfaces directly implemented by the given type
    * 
    * @see Class#getAnnotatedInterfaces()
    */
   public static AnnotatedType[] getAnnotatedInterfaces(AnnotatedType type) {
      // TODO: implement and revise doc!
      return null;
//      requireNonNull(type);
//      if (type instanceof Class) {
//         return ((Class<?>) type).getGenericInterfaces();
//      } else if (type instanceof ParameterizedType) {
//         Class<?> interfaces[] =
//               Types.getRawType(((ParameterizedType) type).getRawType()).getInterfaces();
//         if (interfaces.length == 0) {
//            return interfaces;
//         }
//         int len = interfaces.length;
//         Type genericInterfaces[] = new Type[len];
//         for (int i = 0; i < len; i++) {
//            genericInterfaces[i] = resolveSuperType(type, interfaces[i]);
//            assert genericInterfaces[i] != null;
//         }
//         return genericInterfaces;
//      } else if (type instanceof GenericArrayType) {
//         return ARRAY_INTERFACES;
//      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
//         Type bounds[] = type instanceof WildcardType ? ((WildcardType) type).getUpperBounds()
//               : ((TypeVariable<?>) type).getBounds();
//         assert bounds.length > 0;
//         List<Type> interfaceBounds =
//               Arrays.stream(bounds).filter(Types::isInterface).collect(Collectors.toList());
//         return interfaceBounds.toArray(new Type[interfaceBounds.size()]);
//      } else {
//         throw new IllegalArgumentException("Unrecognized Type: " + type);
//      }
   }

   // TODO: javadoc
   // JLS 4.10
   public static AnnotatedType[] getAnnotatedDirectSupertypes(AnnotatedType type) {
      // TODO: implement me!
      return null;
   }

   // TODO: javadoc
   // JLS 4.10.4
   public static AnnotatedType[] getAnnotatedLeastUpperBounds(AnnotatedType... types) {
      return getAnnotatedLeastUpperBounds(Arrays.asList(types));
   }

   // TODO: javadoc
   // JLS 4.10.4
   public static AnnotatedType[] getAnnotatedLeastUpperBounds(
         Iterable<? extends AnnotatedType> types) {
      // TODO: implement me!
      return null;
   }

//   /**
//    * Returns the owner of the given type. The owner is the type's declaring class. If the given
//    * type is a top-level type then the owner is {@code null}. Array types, wildcard types, and
//    * type variables do not have owners, though their component types / bounds might. So this method
//    * return {@code null} if given such a type.
//    * 
//    * <p>For non-static inner classes, the owner could be a parameterized type. In other cases, the
//    * owner type will be a raw type (e.g. a {@code Class} token)
//    *
//    * @param type the generic type
//    * @return the owner of the given type or {@code null} if it has no owner
//    * 
//    * @see Class#getDeclaringClass()
//    * @see ParameterizedType#getOwnerType()
//    */
//   public static AnnotatedType getOwnerType(AnnotatedType type) {
//      requireNonNull(type);
//      if (type instanceof Class) {
//         return ((Class<?>) type).getDeclaringClass();
//      } else if (type instanceof ParameterizedType) {
//         ParameterizedType parameterizedType = (ParameterizedType) type; 
//         Type ownerType = parameterizedType.getOwnerType();
//         return ownerType != null
//               ? ownerType : getRawType(parameterizedType.getRawType()).getDeclaringClass();
//      }
//      return null;
//   }

   /**
    * Returns true if both of the given types refer to the same type or are equivalent.
    *
    * @param a a type
    * @param b another type
    * @return true if the two given types are equals; false otherwise
    */
   public static boolean equals(AnnotatedType a, AnnotatedType b) {
      if (requireNonNull(a) == requireNonNull(b)) {
         return true;
      } else if (a instanceof AnnotatedParameterizedType) {
         if (!(b instanceof AnnotatedParameterizedType)) {
            return false;
         }
         AnnotatedParameterizedType p1 = (AnnotatedParameterizedType) a;
         AnnotatedParameterizedType p2 = (AnnotatedParameterizedType) b;
         // TODO: improve this when there's a way to access annotated owner type
         ParameterizedType pt1 = (ParameterizedType) p1.getType();
         ParameterizedType pt2 = (ParameterizedType) p2.getType();
         Type owner1 = pt1.getOwnerType();
         Type owner2 = pt2.getOwnerType();
         if (!(owner1 == null ? owner2 == null : Types.equals(owner1, owner2))) {
            return false;
         }
         if (!Types.equals(pt1.getRawType(), pt2.getRawType())) {
            return false;
         }
         if (!Arrays.equals(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())) {
            return false;
         }
         AnnotatedType[] args1 = p1.getAnnotatedActualTypeArguments();
         AnnotatedType[] args2 = p2.getAnnotatedActualTypeArguments();
         if (args1.length != args2.length) {
            return false;
         }
         for (int i = 0; i < args1.length; i++) {
            if (!equals(args1[i], args2[i])) {
               return false;
            }
         }
         return true;
      } else if (a instanceof AnnotatedArrayType) {
         if (!(b instanceof AnnotatedArrayType)) {
            return false;
         }
         AnnotatedArrayType a1 = (AnnotatedArrayType) a;
         AnnotatedArrayType a2 = (AnnotatedArrayType) b;
         return Arrays.equals(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())
               && equals(a1.getAnnotatedGenericComponentType(),
                     a2.getAnnotatedGenericComponentType());
      } else if (a instanceof AnnotatedWildcardType) {
         if (!(b instanceof AnnotatedWildcardType)) {
            return false;
         }
         AnnotatedWildcardType w1 = (AnnotatedWildcardType) a;
         AnnotatedWildcardType w2 = (AnnotatedWildcardType) b;
         if (!Arrays.equals(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())) {
            return false;
         }
         AnnotatedType[] bounds1 = w1.getAnnotatedLowerBounds();
         AnnotatedType[] bounds2 = w2.getAnnotatedLowerBounds();
         if (bounds1.length != bounds2.length) {
            return false;
         }
         for (int i = 0; i < bounds1.length; i++) {
            if (!equals(bounds1[i], bounds2[i])) {
               return false;
            }
         }
         bounds1 = w1.getAnnotatedUpperBounds();
         bounds2 = w2.getAnnotatedUpperBounds();
         if (bounds1.length != bounds2.length) {
            return false;
         }
         for (int i = 0; i < bounds1.length; i++) {
            if (!equals(bounds1[i], bounds2[i])) {
               return false;
            }
         }
         return true;
      } else if (a instanceof AnnotatedTypeVariable) {
         return b instanceof AnnotatedTypeVariable && Types.equals(a.getType(), b.getType())
               && Arrays.equals(a.getDeclaredAnnotations(), b.getDeclaredAnnotations());
      } else {
         return Types.equals(a.getType(), b.getType())
               && Arrays.equals(a.getDeclaredAnnotations(), b.getDeclaredAnnotations());
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
   public static final int hashCode(AnnotatedType type) {
      // NB: AnnotatedType implementations in JRE do not override equals and hashCode and just
      // use identity-based implementations inherited from Object.
      requireNonNull(type);
      int hash = (Types.hashCode(type.getType())
            + 37 * Arrays.hashCode(type.getDeclaredAnnotations()));
      if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pt = (AnnotatedParameterizedType) type;
         // TODO: owner type and annotations? what if getType() doesn't return a ParameterizedType?
         return hash ^ hashCode(pt.getAnnotatedActualTypeArguments());
      } else if (type instanceof AnnotatedArrayType) {
         AnnotatedArrayType gat = (AnnotatedArrayType) type;
         return hash ^ hashCode(gat.getAnnotatedGenericComponentType());
      } else if (type instanceof AnnotatedTypeVariable) {
         // don't need to also include bounds here, so the hash so far suffices
         return hash;
      } else if (type instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType wt = (AnnotatedWildcardType) type;
         return hash ^ hashCode(wt.getAnnotatedLowerBounds())
               ^ hashCode(wt.getAnnotatedUpperBounds());
      } else {
         return hash;
      }      
   }
   
   private static int hashCode(AnnotatedType types[]) {
      int result = 1;
      for (AnnotatedType type : types) {
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
   public static String toString(AnnotatedType type) {
      requireNonNull(type);
      StringBuilder sb = new StringBuilder();
      toStringBuilder(type, sb);
      return sb.toString();
   }
   
   private static void toStringBuilder(AnnotatedType type, StringBuilder sb) {
      if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pt = (AnnotatedParameterizedType) type;
         // TODO: improve when there is a way to get owner type annotations
         Type owner = Types.getOwnerType(pt.getType());
         if (owner == null) {
            for (Annotation a : type.getDeclaredAnnotations()) {
               sb.append(Annotations.toString(a));
               sb.append(" ");
            }
            sb.append(Types.toString(Types.getErasure(pt.getType())));
         } else {
            sb.append(Types.toString(owner));
            sb.append(".");
            for (Annotation a : type.getDeclaredAnnotations()) {
               sb.append(Annotations.toString(a));
               sb.append(" ");
            }
            Class<?> rawType = Types.getErasure(pt.getType());
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
         AnnotatedType args[] = pt.getAnnotatedActualTypeArguments();
         if (args.length > 0) {
            sb.append("<");
            boolean first = true;
            for (AnnotatedType arg : args) {
               if (first) {
                  first = false;
               } else {
                  sb.append(",");
               }
               toStringBuilder(arg, sb);
            }
            sb.append(">");
         }
      } else if (type instanceof AnnotatedArrayType) {
         // Doing the nested arrays recursively would result in the reverse order we want since
         // annotations are root component type first, and then outer-most to inner-most. So we 
         // start by finding root component type.
         AnnotatedType componentType = type;
         while (componentType instanceof AnnotatedArrayType) {
            AnnotatedArrayType arrayType = (AnnotatedArrayType) componentType;
            componentType = arrayType.getAnnotatedGenericComponentType();
         }
         toStringBuilder(componentType, sb);
         // then outer-most to inner-most
         while (type instanceof AnnotatedArrayType) {
            Annotation annotations[] = type.getDeclaredAnnotations();
            if (annotations.length > 0) {
               sb.append(" ");
               for (Annotation a : annotations) {
                  sb.append(Annotations.toString(a));
                  sb.append(" ");
               }
            }
            sb.append("[]");
            AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
            type = arrayType.getAnnotatedGenericComponentType();
         }
      } else if (type instanceof AnnotatedWildcardType) {
         for (Annotation a : type.getDeclaredAnnotations()) {
            sb.append(Annotations.toString(a));
            sb.append(" ");
         }
         AnnotatedWildcardType wc = (AnnotatedWildcardType) type;
         AnnotatedType bounds[] = wc.getAnnotatedLowerBounds();
         if (bounds.length > 0) {
            sb.append("? super ");
         } else {
            bounds = wc.getAnnotatedUpperBounds();
            if (bounds.length == 1 && bounds[0].getType() == Object.class
                  && bounds[0].getDeclaredAnnotations().length == 0) {
               sb.append("?");
               return;
            }
            sb.append("? extends ");
         }
         boolean first = true;
         for (AnnotatedType bound : bounds) {
            if (first) {
               first = false;
            } else {
               sb.append("&");
            }
            toStringBuilder(bound, sb);
         }
      } else {
         for (Annotation a : type.getDeclaredAnnotations()) {
            sb.append(Annotations.toString(a));
            sb.append(" ");
         }
         sb.append(Types.toString(type.getType()));
      }
   }
   
   /**
    * Determines if a given annotated type is assignable from another. This uses the same rules as
    * {@link Types#isAssignable} (Assignment Conversions, JLS 5.2) for determining whether the types
    * are assignable and uses {@linkplain TypeAnnotationChecker#isAssignable(Collection, Collection)
    * the checker} for determining whether the types' annotations are also assignable.
    * 
    * <p>This crawls the types recursively, testing for assignment-compatibility of enclosed
    * annotated types where appropriate. For example, if given parameterized types,
    * {@code Collection<@NotNull String>} and {@code List<@Nullable String>}, the actual type
    * arguments (and their annotations) are examined. In the example, assuming this checker
    * disallows assigning {@code @Nullable} to {@code NotNull}, false would be returned because the
    * type arguments are not compatible.
    * 
    * <p>Similarly, this crawls up a type's hierarchy. So if a type were declared using
    * {@code class TypeA extends @Blah TypeB}, then {@code TypeA} implicitly is annotated with
    * {@code @Blah}, even if that annotation is not present on the given {@link AnnotatedType}. 
    *
    * @param target the LHS of assignment
    * @param source the RHS of assignment
    * @param checker a type annotation checker
    * @return true if the assignment is allowed
    */
   public static boolean isAssignable(AnnotatedType target, AnnotatedType source,
         TypeAnnotationChecker checker) {
      // TODO
      return false;
   }
   
   /**
    * Determines if a given annotated type is strictly assignable to another. This is just like
    * {@link #isAssignable(AnnotatedType, AnnotatedType, TypeAnnotationChecker)} except that it is
    * more restrictive. In particular, it uses the same rules as {@link Class#isAssignableFrom} and
    * {@link Types#isAssignableStrict}, which only consider Identity Conversion (JLS 5.1.1) and
    * Widening Reference Conversion (JLS 5.1.4) when determining if a type can be assigned to
    * another.
    * 
    * @param target the LHS of assignment
    * @param source the RHS of assignment
    * @param checker a type annotation checker
    * @return true if the assignment is allowed
    */
   public static boolean isAssignableStrict(AnnotatedType target, AnnotatedType source,
         TypeAnnotationChecker checker) {
      return isAssignable(target, Collections.emptyList(), source, Collections.emptyList(), checker,
            false);
   }

   private static boolean isAssignable(AnnotatedType target, List<Annotation> extraTargetAnnotations,
         AnnotatedType source, List<Annotation> extraSourceAnnotations,
         TypeAnnotationChecker checker, boolean allowUncheckedConverion) {
      if (requireNonNull(target) == requireNonNull(source)) {
         return true;
      }
      Type targetType = target.getType();
      Type sourceType = source.getType();
      if (targetType instanceof Class && sourceType instanceof Class) {
         return ((Class<?>) targetType).isAssignableFrom((Class<?>) sourceType);
      } else if (targetType == Object.class) {
         return checkAnnotations(target, extraTargetAnnotations, source, extraSourceAnnotations,
               checker);
      } else if (source instanceof AnnotatedWildcardType
            || source instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = source instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) source).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) source).getAnnotatedBounds();
         // TODO: what about annotations on the wildcard/variable?
         for (AnnotatedType bound : bounds) {
            if (isAssignable(target, extraTargetAnnotations,
                  bound, join(extraSourceAnnotations, source.getAnnotations()),
                  checker, allowUncheckedConverion)) {
               return true;
            }
         }
         // they might still be assignable if they refer to the same type variable 
         return source instanceof AnnotatedTypeVariable && target instanceof AnnotatedTypeVariable
               && Types.equals(target.getType(), source.getType());
      } else if (target instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType toParamType = (AnnotatedParameterizedType) target;
         Class<?> toRawType = Types.getErasure(targetType);
         if (sourceType instanceof Class) {
            Class<?> fromClass = (Class<?>) sourceType;
            if (fromClass.getTypeParameters().length > 0) {
               // Both types are generic, but RHS has no type arguments (e.g. raw). This requires
               // an unchecked cast, so no go
               return false;
            }
            if (!toRawType.isAssignableFrom(fromClass)) {
               // Raw types aren't even compatible? Abort!
               return false;
            }
         } else if (sourceType instanceof ParameterizedType) {
            ParameterizedType fromParamType = (ParameterizedType) sourceType;
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
         AnnotatedType resolvedToType = resolveSuperType(source, toRawType);
         AnnotatedType args[] = toParamType.getAnnotatedActualTypeArguments();
         AnnotatedType resolvedArgs[] = getActualTypeArguments(resolvedToType);
         if (resolvedArgs.length == 0) {
            // assigning from raw type to parameterized type requires unchecked cast, so no go
            return false;
         }
         assert args.length == resolvedArgs.length;
         // check each type argument
         for (int i = 0, len = args.length; i < len; i++) {
            AnnotatedType toArg = args[i];
            AnnotatedType fromArg = resolvedArgs[i];
            if (toArg instanceof AnnotatedWildcardType) {
               AnnotatedWildcardType wildcardArg = (AnnotatedWildcardType) toArg;
               for (AnnotatedType upperBound : wildcardArg.getAnnotatedUpperBounds()) {
                  if (!isAssignable(upperBound, Collections.emptyList(), fromArg,
                        Collections.emptyList(), checker, allowUncheckedConverion)) {
                     return false;
                  }
               }
               for (AnnotatedType lowerBound : wildcardArg.getAnnotatedLowerBounds()) {
                  if (!isAssignable(fromArg, Collections.emptyList(), lowerBound, 
                        Collections.emptyList(), checker, allowUncheckedConverion)) {
                     return false;
                  }
               }
            } else if (!equals(toArg, fromArg)) {
               return false;
            }
         }
         return true;
      } else if (target instanceof AnnotatedArrayType) {
         AnnotatedArrayType toArrayType = (AnnotatedArrayType) target;
         if (!checkAnnotations(target, extraTargetAnnotations, source, extraSourceAnnotations,
               checker)) {
            return false;
         }
         if (sourceType instanceof Class) {
            Class<?> fromClass = (Class<?>) sourceType;
            return fromClass.isArray() && isAssignable(
                  toArrayType.getAnnotatedGenericComponentType(), Collections.emptyList(),
                  newAnnotatedType(fromClass), Collections.emptyList(), checker,
                  allowUncheckedConverion);
         } else if (source instanceof AnnotatedArrayType) {
            return isAssignable(toArrayType.getAnnotatedGenericComponentType(),
                  Collections.emptyList(),
                  ((AnnotatedArrayType) source).getAnnotatedGenericComponentType(),
                  Collections.emptyList(), checker, allowUncheckedConverion);
         } else {
            return false;
         }
      } else if (target instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType toWildcard = (AnnotatedWildcardType) target;
         AnnotatedType lowerBounds[] = toWildcard.getAnnotatedLowerBounds();
         if (lowerBounds.length == 0) {
            // Can only assign to a wildcard type based on its lower bounds
            return false;
         }
         assert toWildcard.getAnnotatedUpperBounds().length == 1;
         assert toWildcard.getAnnotatedUpperBounds()[0].getType() == Object.class;
         assert toWildcard.getAnnotatedUpperBounds()[0].getAnnotations().length == 0;
         for (AnnotatedType bound : lowerBounds) {
            if (!isAssignable(bound, join(extraTargetAnnotations, target.getAnnotations()), source,
                  extraSourceAnnotations, checker, allowUncheckedConverion)) {
               return false;
            }
         }
         return true;
      } else if (target instanceof AnnotatedTypeVariable) {
         // We don't actually know the type bound to this variable. So we can only assign to it from
         // another instance of the same type variable or some other variable or wildcard that
         // extends it. Both of those cases are handled above (check for `from` being a TypeVariable
         // or WildcardType). So if we get here, it's not assignable.
         return false;
      } else {
         Class<?> toClass = (Class<?>) target.getType();
         if (source instanceof AnnotatedArrayType) {
            if (toClass == Cloneable.class || toClass == Serializable.class) {
               if (!checkAnnotations(target, extraTargetAnnotations, source, extraSourceAnnotations,
                     checker)) {
                  return false;
               }
               return true;
            }
            AnnotatedArrayType fromArrayType = (AnnotatedArrayType) target;
            return toClass.isArray()
                  && isAssignableStrict(newAnnotatedType(toClass.getComponentType()),
                        fromArrayType.getAnnotatedGenericComponentType(), checker);
         } else if (target instanceof AnnotatedParameterizedType) {
            // TODO: fix this
            Class<?> fromRaw = Types.getErasure(((AnnotatedParameterizedType) target).getType());
            return toClass.isAssignableFrom(fromRaw);
         } else {
            return false;
         }
      }
   }
   
   private static <T> List<T> join(List<T> head, T[] tail) {
      return new AbstractList<T>() {
         @Override
         public T get(int index) {
            int s = head.size();
            return index < s ? head.get(index) : tail[index - s];
         }

         @Override
         public int size() {
            return head.size() + tail.length;
         }
      };
   }

   private static boolean checkAnnotations(
         AnnotatedType target, List<Annotation> extraTargetAnnotations,
         AnnotatedType source, List<Annotation> extraSourceAnnotations,
         TypeAnnotationChecker checker) {
      return checker.isAssignable(getAllAnnotations(target, extraTargetAnnotations),
            getAllAnnotations(source, extraSourceAnnotations));
   }
   
   private static Collection<Annotation> getAllAnnotations(AnnotatedType type,
         List<Annotation> extras) {
      Map<Class<? extends Annotation>, Annotation> allAnnotations = new HashMap<>();
      extras.forEach(a -> allAnnotations.putIfAbsent(a.annotationType(), a));
      addAllAnnotationsFromHierarchy(type, allAnnotations);
      return allAnnotations.values();
   }
   
   private static void addAllAnnotationsFromHierarchy(AnnotatedType type,
         Map<Class<? extends Annotation>, Annotation> allAnnotations) {
      addAll(type.getAnnotations(), allAnnotations);
      if (type instanceof AnnotatedWildcardType) {
         // add annotations for upper bounds
         AnnotatedType[] bounds = ((AnnotatedWildcardType) type).getAnnotatedUpperBounds();
         for (AnnotatedType b : bounds) {
            addAllAnnotationsFromHierarchy(b, allAnnotations);
         }
      } else if (type instanceof AnnotatedTypeVariable) {
         // add annotations for bounds
         AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) type;
         AnnotatedType[] bounds = typeVar.getAnnotatedBounds();
         for (AnnotatedType b : bounds) {
            addAllAnnotationsFromHierarchy(b, allAnnotations);
         }
         // as well as annotations present on the type variable declaration
         addAll(((TypeVariable<?>) typeVar.getType()).getAnnotations(), allAnnotations); 
      } else {
         // add annotations for super-types
         Class<?> raw = Types.getErasure(type.getType());
         AnnotatedType s = raw.getAnnotatedSuperclass();
         if (s != null) {
            addAllAnnotationsFromHierarchy(s, allAnnotations);
         }
         for (AnnotatedType i : raw.getAnnotatedInterfaces()) {
            addAllAnnotationsFromHierarchy(i, allAnnotations);
         }
      }
   }
   
   private static void addAll(Annotation[] annotations,
         Map<Class<? extends Annotation>, Annotation> allAnnotations) {
      for (Annotation a : annotations) {
         allAnnotations.putIfAbsent(a.annotationType(), a);
      }
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
   public static AnnotatedType resolveTypeVariable(AnnotatedType context,
         TypeVariable<?> variable) {
      // TODO: implement and revise doc!
      return null;
//      GenericDeclaration declaration = variable.getGenericDeclaration();
//      if (!(declaration instanceof Class)) {
//         return null; // can only resolve variables declared on classes
//      }
//      while (true) {
//         Type componentType = getComponentType(context);
//         if (componentType == null) {
//            break;
//         }
//         context = componentType;
//      }
//      Type superType = resolveSuperType(context, (Class<?>) declaration);
//      if (superType == null || superType instanceof Class) {
//         return null; // cannot resolve
//      }
//      TypeVariable<?> vars[] = ((Class<?>) declaration).getTypeParameters();
//      Type actualArgs[] = ((ParameterizedType) superType).getActualTypeArguments();
//      assert actualArgs.length == vars.length;
//      for (int i = 0, len = vars.length; i < len; i++) {
//         if (equals(vars[i], variable)) {
//            Type value = actualArgs[i];
//            // if actual type argument equals the type variable itself, it isn't resolved
//            return equals(vars[i], value) ? null : value;
//         }
//      }
//      throw new AssertionError("should not be reachable");
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
   public static AnnotatedType resolveType(AnnotatedType context, AnnotatedType typeToResolve) {
      // TODO: implement and revise doc!
      return null;
//      Map<TypeVariableWrapper, Type> resolvedVariableValues = new HashMap<>();
//      Set<TypeVariableWrapper> resolvedVariables = new HashSet<>();
//      resolveTypeVariables(context, typeToResolve, resolvedVariableValues, resolvedVariables);
//      return replaceTypeVariablesInternal(typeToResolve, resolvedVariableValues);
   }
   
//   private static void resolveTypeVariables(AnnotatedType context, Type type,
//         Map<TypeVariableWrapper, AnnotatedType> resolvedVariableValues,
//         Set<TypeVariableWrapper> resolvedVariables) {
//      if (type instanceof Class) {
//         // no-op
//      } else if (type instanceof ParameterizedType) {
//         for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
//            resolveTypeVariables(context, arg, resolvedVariableValues, resolvedVariables);
//         }
//      } else if (type instanceof GenericArrayType) {
//         resolveTypeVariables(context, ((GenericArrayType) type).getGenericComponentType(),
//               resolvedVariableValues, resolvedVariables);
//      } else if (type instanceof WildcardType) {
//         WildcardType wt = (WildcardType) type;
//         for (Type bound : wt.getUpperBounds()) {
//            resolveTypeVariables(context, bound, resolvedVariableValues, resolvedVariables);
//         }
//         for (Type bound : wt.getLowerBounds()) {
//            resolveTypeVariables(context, bound, resolvedVariableValues, resolvedVariables);
//         }
//      } else if (type instanceof TypeVariable) {
//         TypeVariable<?> tv = (TypeVariable<?>) type;
//         TypeVariableWrapper wrapper = wrap(tv);
//         if (tv.getGenericDeclaration() instanceof Class) {
//            // don't bother re-resolving occurrences of variables we've already seen
//            if (resolvedVariables.add(wrapper)) {
//               Type resolvedValue = resolveTypeVariable(context, tv);
//               if (resolvedValue != null) {
//                  resolvedVariableValues.put(wrapper, resolvedValue);
//               }
//            }
//         }
//      } else {
//         throw new IllegalArgumentException("Unrecognized Type: " + type);
//      }
//   }
   
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
   public static AnnotatedType replaceTypeVariable(AnnotatedType type, TypeVariable<?> typeVariable,
         AnnotatedType typeValue) {
      // TODO: implement and revise doc!
      return null;
//      Map<TypeVariableWrapper, AnnotatedType> wrappedVariables =
//            Collections.singletonMap(wrap(typeVariable), typeValue); 
//      checkTypeValue(typeVariable, typeValue, wrappedVariables);
//      return replaceTypeVariablesInternal(type, wrappedVariables);
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
   public static AnnotatedType replaceTypeVariables(AnnotatedType type,
         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      // TODO: implement and revise doc!
      return null;
//      // wrap the variables to make sure their hashCode and equals are well-behaved
//      Map<TypeVariableWrapper, AnnotatedType> wrappedVariables =
//            new HashMap<>(typeVariables.size() * 4 / 3, 0.75f);
//      for (Entry<TypeVariable<?>, AnnotatedType> entry : typeVariables.entrySet()) {
//         TypeVariable<?> typeVariable = entry.getKey();
//         AnnotatedType typeValue = entry.getValue();
//         wrappedVariables.put(wrap(typeVariable), typeValue);
//      }
//      // check type bounds
//      checkTypeValues(wrappedVariables);
//      return replaceTypeVariablesInternal(type, wrappedVariables);
   }
   
//   private static AnnotatedType replaceTypeVariablesInternal(AnnotatedType type,
//         Map<TypeVariableWrapper, AnnotatedType> typeVariables) {
//      if (typeVariables.isEmpty()) {
//         return type;
//      }
//      
//      if (type instanceof Class) {
//         return type;
//      }
//      
//      if (type instanceof ParameterizedType) {
//         ParameterizedType pType = (ParameterizedType) type;
//         Type initialOwner = pType.getOwnerType();
//         Type resolvedOwner = initialOwner == null ? null
//               : replaceTypeVariablesInternal(initialOwner, typeVariables);
//         Type initialRaw = pType.getRawType();
//         Type resolvedRaw = initialRaw == null ? null
//               : replaceTypeVariablesInternal(initialRaw, typeVariables);
//         boolean different = initialOwner != resolvedOwner || initialRaw != resolvedRaw;
//         Type initialArgs[] = pType.getActualTypeArguments();
//         List<Type> resolvedArgs = new ArrayList<>(initialArgs.length);
//         for (Type initial : initialArgs) {
//            Type resolved = replaceTypeVariablesInternal(initial, typeVariables);
//            resolvedArgs.add(resolved);
//            if (initial != resolved) {
//               different = true;
//            }
//         }
//         return different
//               ? new ParameterizedTypeImpl(resolvedOwner, resolvedRaw, resolvedArgs)
//               : type;
//         
//      } else if (type instanceof GenericArrayType) {
//         Type initialComponent = ((GenericArrayType) type).getGenericComponentType();
//         Type resolvedComponent = replaceTypeVariablesInternal(initialComponent, typeVariables);
//         return resolvedComponent == initialComponent ? type
//               : new GenericArrayTypeImpl(resolvedComponent);
//         
//      } else if (type instanceof WildcardType) {
//         WildcardType wtType = (WildcardType) type;
//         boolean different = false;
//         Type initialUpperBounds[] = wtType.getUpperBounds();
//         List<Type> resolvedUpperBounds = new ArrayList<>(initialUpperBounds.length);
//         for (Type initial : initialUpperBounds) {
//            Type resolved = replaceTypeVariablesInternal(initial, typeVariables);
//            resolvedUpperBounds.add(resolved);
//            if (initial != resolved) {
//               different = true;
//            }
//         }
//         Type initialLowerBounds[] = wtType.getLowerBounds();
//         List<Type> resolvedLowerBounds = new ArrayList<>(initialLowerBounds.length);
//         for (Type initial : initialLowerBounds) {
//            Type resolved = replaceTypeVariablesInternal(initial, typeVariables);
//            resolvedLowerBounds.add(resolved);
//            if (initial != resolved) {
//               different = true;
//            }
//         }
//         return different
//               ? new WildcardTypeImpl(resolvedUpperBounds, resolvedLowerBounds)
//               : type;
//         
//      } else if (type instanceof TypeVariable) {
//         Type resolvedType = typeVariables.get(wrap((TypeVariable<?>) type));
//         return resolvedType != null ? resolvedType : type;
//         
//      } else {
//         throw new IllegalArgumentException("Unrecognized Type: " + type);
//      }
//   }
//
//   private static void checkTypeValue(TypeVariable<?> variable, AnnotatedType argument,
//         Map<TypeVariableWrapper, AnnotatedType> resolvedVariables) {
//      checkTypeValue(variable, argument, resolvedVariables, -1);
//   }
//
//   private static void checkTypeValues(Map<TypeVariableWrapper, AnnotatedType> typeVariables) {
//      for (Entry<TypeVariableWrapper, AnnotatedType> entry : typeVariables.entrySet()) {
//         checkTypeValue(entry.getKey().typeVariable, entry.getValue(), typeVariables, -1);
//      }
//   }
//
//   private static void checkTypeValue(TypeVariable<?> variable, AnnotatedType argument,
//         Map<TypeVariableWrapper, AnnotatedType> resolvedVariables, int index) {
//      String id = index < 0 ? "" : (" #" + index);
//      if (argument instanceof Class && ((Class<?>) argument).isPrimitive()) {
//         throw new IllegalArgumentException("Type argument" + id + " is primitive: "
//               + argument.getTypeName());
//      }
//      for (Type bound : variable.getBounds()) {
//         // do any substitutions on owner type variables that may be referenced
//         bound = replaceTypeVariablesInternal(bound, resolvedVariables);
//         if (!isAssignable(bound, argument)) {
//            throw new IllegalArgumentException("Argument" + id + ", "
//                  + argument.getTypeName() + " does not extend bound "+ bound.getTypeName());
//         }
//      }
//   }
   
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
   public static AnnotatedType resolveSuperType(AnnotatedType type, Class<?> superClass) {
      // TODO: implement and revise doc!
      return null;
//      requireNonNull(type);
//      requireNonNull(superClass);
//      Map<TypeVariableWrapper, AnnotatedType> typeVariables = new HashMap<>();
//      AnnotatedType superType = findGenericSuperType(type, superClass, typeVariables);
//      return superType != null ? replaceTypeVariablesInternal(superType, typeVariables) : null;
   }

//   /**
//    * Finds the generic super type for the given generic type and super-type token and accumulates
//    * type variables and actual type arguments in the given map.
//    *
//    * @param type the generic type
//    * @param superClass the class token for the super-type being queried
//    * @param typeVariables a map of type variables that accumulates type variables and actual type
//    *       arguments as types are traversed from the given type to the given super-type 
//    * @return the generic type corresponding to the given super-type token as returned by
//    *       {@link Class#getGenericSuperclass()} or {@link Class#getGenericInterfaces()} 
//    */
//   private static AnnotatedType findGenericSuperType(AnnotatedType type, Class<?> superClass,
//         Map<TypeVariableWrapper, AnnotatedType> typeVariables) {
//      if (type instanceof Class) {
//         Class<?> clazz = (Class<?>) type;
//         // if this is a raw type reference to a generic type, just forget generic type information
//         // and use raw types from here on out
//         boolean useRawTypes = clazz.getTypeParameters().length > 0; 
//         return findGenericSuperType(clazz, superClass, useRawTypes, typeVariables);
//          
//      } else if (type instanceof GenericArrayType) {
//         if (superClass == Object.class || superClass == Serializable.class
//               || superClass == Cloneable.class) {
//            return superClass;
//         }
//         if (!superClass.isArray()) {
//            return null;
//         }
//         Type resolvedComponentType = findGenericSuperType(
//               ((GenericArrayType) type).getGenericComponentType(), superClass.getComponentType(),
//               typeVariables);
//         if (resolvedComponentType instanceof Class) {
//            return getArrayType((Class<?>) resolvedComponentType);
//         } else if (resolvedComponentType == null) {
//            return null;
//         } else {
//            return newGenericArrayType(resolvedComponentType);
//         }
//         
//      } else if (type instanceof ParameterizedType) {
//         Class<?> rawType = getRawType(type);
//         ParameterizedType pType = (ParameterizedType) type;
//         if (rawType == superClass) {
//            return type;
//         }
//         Type superType = findGenericSuperType(rawType, superClass, false, typeVariables);
//         mergeTypeVariables(pType, rawType, typeVariables);
//         return superType;
//         
//      } else if (type instanceof WildcardType || type instanceof TypeVariable) {
//         Type bounds[] = type instanceof WildcardType
//               ? ((WildcardType) type).getUpperBounds()
//               : ((TypeVariable<?>) type).getBounds();
//         for (Type bound : bounds) {
//            Type superType = findGenericSuperType(bound, superClass, typeVariables);
//            if (superType != null) {
//               return superType;
//            }
//         }
//         return null;
//         
//      } else {
//         throw new IllegalArgumentException("Unrecognized Type: " + type);
//      }
//   }
//   
//   private static AnnotatedType findGenericSuperType(Class<?> clazz, Class<?> superClass,
//         boolean useRawTypes, Map<TypeVariableWrapper, AnnotatedType> typeVariables) {
//      if (!superClass.isAssignableFrom(clazz)) {
//         return null;
//      }
//      if (superClass.getTypeParameters().length == 0) {
//         return superClass;
//      }
//      if (useRawTypes) {
//         return superClass;
//      }
//      return superClass.isInterface()
//             ? findGenericInterface(clazz, superClass, typeVariables, new HashSet<>())
//             : findGenericSuperclass(clazz, superClass, typeVariables);
//
//   }
//   
//   private static AnnotatedType findGenericSuperclass(Class<?> clazz, Class<?> superClass,
//         Map<TypeVariableWrapper, AnnotatedType> typeVariables) {
//      Class<?> actualSuper = clazz.getSuperclass();
//      assert actualSuper != null;
//      Type genericSuper = clazz.getGenericSuperclass();
//      Type ret;
//      if (actualSuper == superClass) {
//         ret = genericSuper;
//      } else {
//         // recurse until we find it
//         ret = findGenericSuperclass(actualSuper, superClass, typeVariables);
//         if (ret == null) {
//            return null;
//         }
//      }
//      if (genericSuper instanceof ParameterizedType) {
//         mergeTypeVariables((ParameterizedType) genericSuper, actualSuper, typeVariables);
//      }
//      return ret;
//   }
//
//   private static AnnotatedType findGenericInterface(Class<?> clazz, Class<?> intrface,
//         Map<TypeVariableWrapper, AnnotatedType> typeVariables, Set<Class<?>> alreadyChecked) {
//      if (alreadyChecked.contains(clazz)) {
//         return null;
//      }
//      Class<?> actualInterfaces[] = clazz.getInterfaces();
//      Type genericInterfaces[] = clazz.getGenericInterfaces();
//      assert actualInterfaces.length > 0;
//      Class<?> actualSuper = null;
//      Type genericSuper = null;
//      Type ret = null;
//      // not quite breadth-first -- but first, shallowly check all interfaces before we
//      // check their super-interfaces
//      for (int i = 0, len = actualInterfaces.length; i < len; i++) {
//         if (actualInterfaces[i] == intrface) {
//            actualSuper = actualInterfaces[i];
//            genericSuper = genericInterfaces[i];
//            ret = genericSuper;
//         }
//      }
//      if (ret == null) {
//         // didn't find it: check super-interfaces
//         for (int i = 0, len = actualInterfaces.length; i < len; i++) {
//            ret = findGenericInterface(actualInterfaces[i], intrface, typeVariables, alreadyChecked);
//            if (ret != null) {
//               actualSuper = actualInterfaces[i];
//               genericSuper = genericInterfaces[i];
//            }
//         }
//      }
//      if (ret == null) {
//         // still didn't find it: check super-class's interfaces
//         if ((actualSuper = clazz.getSuperclass()) == null) {
//            return null; // no super-class
//         }
//         genericSuper = clazz.getGenericSuperclass();
//         ret = findGenericInterface(clazz.getSuperclass(), intrface, typeVariables, alreadyChecked);
//      }
//      if (ret == null) {
//         alreadyChecked.add(clazz);
//         return null;
//      }
//      if (genericSuper instanceof ParameterizedType) {
//         mergeTypeVariables((ParameterizedType) genericSuper, actualSuper, typeVariables);
//      }
//      return ret;
//   }
//   
//   private static void mergeTypeVariables(AnnotatedParameterizedType type, Class<?> rawType,
//         Map<TypeVariableWrapper, AnnotatedType> typeVariables) {
//      Type ownerType = type.getOwnerType();
//      if (ownerType instanceof ParameterizedType) {
//         mergeTypeVariables((ParameterizedType) ownerType, getRawType(ownerType), typeVariables);
//      }
//      Map<TypeVariableWrapper, Type> currentVars = new HashMap<>();
//      TypeVariable<?> vars[] = rawType.getTypeParameters();
//      Type values[] = type.getActualTypeArguments();
//      assert vars.length == values.length;
//      for (int i = 0, len = vars.length; i < len; i++) {
//         currentVars.put(wrap(vars[i]), values[i]);
//      }
//      // update any existing type variable values in case they refer to these new variables
//      for (Entry<TypeVariableWrapper, Type> entry : typeVariables.entrySet()) {
//         entry.setValue(replaceTypeVariablesInternal(entry.getValue(), currentVars));
//      }
//      typeVariables.putAll(currentVars);
//   }
   
   public static AnnotatedArrayType newAnnotatedArrayType(GenericArrayType type,
         Annotation... annotations) {
      return newAnnotatedArrayType(type, Arrays.asList(annotations));
   }
   
   public static AnnotatedArrayType newAnnotatedArrayType(GenericArrayType type,
         Iterable<? extends Annotation> annotations) {
      return newAnnotatedArrayType(newAnnotatedType(type.getGenericComponentType()), annotations);
   }
   
   public static AnnotatedArrayType newAnnotatedArrayType(AnnotatedType componentType,
         Annotation... annotations) {
      return newAnnotatedArrayType(componentType, Arrays.asList(annotations));
   }

   /**
    * Creates a new {@link AnnotatedArrayType} object with the given component type.
    *
    * @param componentType the component type of the array
    * @param annotations the annotations on this array type
    * @return an annotated array type with the given component type
    * @throws NullPointerException if the given argument is {@code null}
    * @throws IllegalArgumentException if the given component type is {@code void.class} or a
    *       wildcard type 
    */
   public static AnnotatedArrayType newAnnotatedArrayType(AnnotatedType componentType,
         Iterable<? extends Annotation> annotations) {
      requireNonNull(componentType);
      return new AnnotatedArrayTypeImpl(componentType, annotations);
   }
   
   /**
    * Returns the actual type arguments if the given type is a parameterized type. Otherwise, it
    * returns an empty array.
    *
    * @param type the generic type
    * @return the actual type arguments if the given types is a parameterized type; an empty array
    *       otherwise
    */
   public static AnnotatedType[] getActualTypeArguments(AnnotatedType type) {
      requireNonNull(type);
      if (type instanceof AnnotatedParameterizedType) {
         return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
      } else {
         return EMPTY_TYPES;
      }
   }
   
   public static AnnotatedType newAnnotatedType(Type type, Annotation... annotations) {
      return newAnnotatedType(type, Arrays.asList(annotations));
   }

   public static AnnotatedType newAnnotatedType(Class<?> clazz, Annotation... annotations) {
      return newAnnotatedType(clazz, Arrays.asList(annotations));
   }

   public static AnnotatedType newAnnotatedType(Class<?> clazz,
         Iterable<? extends Annotation> annotations) {
      if (clazz.isArray()) {
         return newAnnotatedArrayType(newAnnotatedType(clazz.getComponentType()), annotations);
      } else {
         return new AnnotatedTypeImpl(clazz, annotations);
      }
   }

   public static AnnotatedType newAnnotatedType(Type type,
         Iterable<? extends Annotation> annotations) {
      if (type instanceof Class) {
         return newAnnotatedType((Class<?>) type, annotations);
      } else if (type instanceof ParameterizedType) {
         return newAnnotatedParameterizedType((ParameterizedType) type, annotations);
      } else if (type instanceof GenericArrayType) {
         return newAnnotatedArrayType((GenericArrayType) type, annotations);
      } else if (type instanceof WildcardType) {
         return newAnnotatedWildcardType((WildcardType) type, annotations);
      } else if (type instanceof TypeVariable) {
         return newAnnotatedTypeVariable((TypeVariable<?>) type, annotations);
      } else {
         throw new UnknownTypeException(type);
      }
   }
   
   private static List<AnnotatedType> asAnnotatedTypes(Type[] types) {
      List<AnnotatedType> a = new ArrayList<>(types.length);
      for (Type t : types) {
         a.add(newAnnotatedType(t));
      }
      return Collections.unmodifiableList(a);
   }
   
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(ParameterizedType type,
         Annotation... annotations) {
      return newAnnotatedParameterizedType(type, Arrays.asList(annotations));
   }
   
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(ParameterizedType type,
         Iterable<? extends Annotation> annotations) {
      Type owner = type.getOwnerType();
      return owner == null
            ? newAnnotatedParameterizedType(newAnnotatedType(type.getRawType()),
                  asAnnotatedTypes(type.getActualTypeArguments()),
                  annotations)
            : newAnnotatedParameterizedType(
                  newAnnotatedParameterizedType((ParameterizedType) owner),
                  newAnnotatedType(type.getRawType()),
                  asAnnotatedTypes(type.getActualTypeArguments()),
                  annotations);
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
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(AnnotatedType rawType,
         List<? extends AnnotatedType> typeArguments, Annotation... annotations) {
      // TODO: implement and revise doc!
      return null;
//      return newParameterizedTypeInternal(null, rawType, Arrays.asList(typeArguments));
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
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(AnnotatedType rawType, 
         List<? extends AnnotatedType> typeArguments, Iterable<? extends Annotation> annotations) {
      // TODO: implement and revise doc!
      return null;
//      return newParameterizedTypeInternal(null, rawType, typeArguments);
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
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(
         AnnotatedParameterizedType ownerType, AnnotatedType rawType,
         List<? extends AnnotatedType> typeArguments, Annotation... annotations) {
      // TODO: implement and revise doc!
      return null;
//      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
//            Arrays.asList(typeArguments));
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
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(
         AnnotatedParameterizedType ownerType, AnnotatedType rawType,
         List<? extends AnnotatedType> typeArguments, Iterable<? extends Annotation> annotations) {
      // TODO: implement and revise doc!
      return null;
//      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
//            typeArguments);
   }
   
//   private static AnnotatedParameterizedType newParameterizedTypeInternal(
//         AnnotatedParameterizedType ownerType, AnnotatedType rawType,
//         List<AnnotatedType> typeArguments) {
//      requireNonNull(rawType);
//      for (Type type : typeArguments) {
//         requireNonNull(type);
//      }
//      List<Type> copyOfArguments = new ArrayList<>(typeArguments); // defensive copy
//      Type owner;
//      if (ownerType != null) {
//         if (rawType.getDeclaringClass() != getRawType(ownerType)) {
//            throw new IllegalArgumentException("Owner type " + ownerType.getTypeName() + " does not"
//                  + " match actual owner of given raw type " + rawType.getTypeName());
//         } else if ((rawType.getModifiers() & Modifier.STATIC) != 0) {
//            throw new IllegalArgumentException("Given raw type " + rawType.getTypeName()
//                  + " is static so cannot have a parameterized owner type");
//         }
//         owner = ownerType;
//      } else {
//         Class<?> ownerClass = rawType.getDeclaringClass();
//         if (ownerClass != null && !typeArguments.isEmpty()
//               && (rawType.getModifiers() & Modifier.STATIC) == 0
//               && ownerClass.getTypeParameters().length > 0) {
//            throw new IllegalArgumentException("Non-static parameterized type "
//               + rawType.getTypeName() + " must have parameterized owner");
//         }
//         owner = ownerClass;
//      }
//      TypeVariable<?> typeVariables[] = rawType.getTypeParameters();
//      int len = typeVariables.length;
//      if (len != copyOfArguments.size()) {
//         throw new IllegalArgumentException("Given type " + rawType.getTypeName() + " has " + len
//               + " type variable(s), but " + copyOfArguments.size()
//               + " argument(s) were specified");
//      }
//      Map<TypeVariableWrapper, Type> resolvedVariables = new HashMap<>();
//      // add current type variables, in case there are recursive bounds
//      for (int i = 0; i < len; i++) {
//         resolvedVariables.put(wrap(typeVariables[i]), copyOfArguments.get(i));
//      }      
//      if (ownerType != null) {
//         // also resolve owners' variables
//         collectTypeParameters(ownerType, resolvedVariables);
//      }
//      for (int i = 0; i < len; i++) {
//         TypeVariable<?> variable = typeVariables[i];
//         Type argument = copyOfArguments.get(i);
//         checkTypeValue(variable, argument, resolvedVariables);
//      }
//      if (ownerType == null && len == 0) {
//         throw new IllegalArgumentException("A ParameterizedType must have either a parameterized"
//               + " owner or its own type parameters");
//      }
//      return new ParameterizedTypeImpl(owner, rawType, copyOfArguments);
//   }
//   
//   private static void collectTypeParameters(AnnotatedParameterizedType type,
//         Map<TypeVariableWrapper, AnnotatedType> typeParameters) {
//      Type owner = type.getOwnerType();
//      if (owner instanceof ParameterizedType) {
//         collectTypeParameters((ParameterizedType) owner, typeParameters);
//      }
//      Type args[] = type.getActualTypeArguments();
//      TypeVariable<?> params[] = getTypeParameters(type.getRawType());
//      assert args.length == params.length;
//      for (int i = 0, len = args.length; i < len; i++) {
//         typeParameters.put(wrap(params[i]), args[i]);
//      }
//   }
   
   /**
    * Creates a new {@link WildcardType} with an upper bound, i.e.&nbsp;{@code ? extends T}.
    *
    * @param bound the upper bound for the wildcard type
    * @param annotations optional annotations that are present on the wildcard type
    * @return a new wildcard type with the given bound
    * @throws NullPointerException if the given bound is {@code null}
    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
    *       type
    */
   public static AnnotatedWildcardType newExtendsAnnotatedWildcardType(AnnotatedType bound,
         Annotation... annotations) {
      return newAnnotatedWildcardTypeInternal(bound, Arrays.asList(annotations), true);
   }
   
   public static AnnotatedWildcardType newExtendsAnnotatedWildcardType(AnnotatedType bound,
         Iterable<? extends Annotation> annotations) {
      return newAnnotatedWildcardTypeInternal(bound, annotations, true);
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
   public static AnnotatedWildcardType newSuperAnnotatedWildcardType(AnnotatedType bound,
         Annotation... annotations) {
      return newAnnotatedWildcardTypeInternal(bound, Arrays.asList(annotations), false);
   }

   public static AnnotatedWildcardType newSuperAnnotatedWildcardType(AnnotatedType bound,
         Iterable<? extends Annotation> annotations) {
      return newAnnotatedWildcardTypeInternal(bound, annotations, false);
   }
   
   public static AnnotatedWildcardType newAnnotatedWildcardType(WildcardType type,
         Annotation... annotations) {
      return newAnnotatedWildcardType(type, Arrays.asList(annotations));
   }
   
   public static AnnotatedWildcardType newAnnotatedWildcardType(WildcardType type,
         Iterable<? extends Annotation> annotations) {
      boolean isUpperBound;
      Type[] bounds = type.getLowerBounds();
      if (bounds.length == 0) {
         bounds = type.getUpperBounds();
         isUpperBound = true;
      } else {
         isUpperBound = false;
      }
      assert bounds.length == 1;
      return newAnnotatedWildcardTypeInternal(newAnnotatedType(bounds[0]), annotations,
            isUpperBound);
   }

   private static AnnotatedWildcardType newAnnotatedWildcardTypeInternal(AnnotatedType bound,
         Iterable<? extends Annotation> annotations, boolean isUpperBound) {
      requireNonNull(bound);
      Type boundType = bound.getType();
      if (boundType instanceof Class) {
         Class<?> boundClass = (Class<?>) boundType;
         if (boundClass.isPrimitive()) {
            throw new IllegalArgumentException("Bound for a WildcardType cannot be primitive");
         }
      } else if (boundType instanceof WildcardType) {
         throw new IllegalArgumentException("Bound for a WildcardType cannot be a WildcardType");
      }
      return new AnnotatedWildcardTypeImpl(bound, isUpperBound, createAnnotations(annotations));
   }
   
   public static AnnotatedTypeVariable newAnnotatedTypeVariable(TypeVariable<?> typeVariable,
         Annotation... annotations) {
      return newAnnotatedTypeVariable(typeVariable, Arrays.asList(annotations));
   }
   
   public static AnnotatedTypeVariable newAnnotatedTypeVariable(TypeVariable<?> typeVariable,
         Iterable<? extends Annotation> annotations) {
      return new AnnotatedTypeVariableImpl(typeVariable, annotations);
   }
   
   static Collection<Type> toTypes(Collection<? extends AnnotatedType> coll) {
      return new TransformingCollection<>(coll, AnnotatedType::getType);
   }
   
   private static List<Annotation> createAnnotations(Iterable<? extends Annotation> annotations) {
      Stream<? extends Annotation> stream = annotations instanceof Collection
            ? ((Collection<? extends Annotation>) annotations).stream()
            : StreamSupport.stream(annotations.spliterator(), false);
      Map<Class<? extends Annotation>, List<Annotation>> grouped =
            stream.collect(Collectors.groupingBy(Annotation::annotationType));
      List<Annotation> result = new ArrayList<>(grouped.size());
      for (Entry<Class<? extends Annotation>, List<Annotation>> entry : grouped.entrySet()) {
         Class<? extends Annotation> type = entry.getKey();
         List<Annotation> a = entry.getValue();
         // make sure this annotation is allowed on a type use
         Target target = type.getDeclaredAnnotation(Target.class);
         boolean valid = false;
         if (target != null) {
            for (ElementType t : target.value()) {
               if (t == ElementType.TYPE_USE) {
                  valid = true;
                  break;
               }
            }
         }
         if (!valid) {
            throw new IllegalArgumentException("Annotation " + type
                  + " cannot be used for a type use");
         }
         if (a.size() == 1) {
            result.add(a.get(0));
         } else {
            // If there are multiple annotations of this type, it must be repeatable
            Repeatable r = type.getDeclaredAnnotation(Repeatable.class);
            if (r == null) {
               throw new IllegalArgumentException("Annotation " + type + " is not repeatable but"
                     + a.size() + " occurrences specified");
            }
            result.add(Annotations.create(r.value(),
                  MapBuilder.<String, Object>forHashMap().put("value", a).build()));
         }
      }
      return result;
   }
   
   /**
    * Implements {@link AnnotatedType}.
    */
   private static class AnnotatedTypeImpl implements AnnotatedType, Serializable {
      private static final long serialVersionUID = -1507646636725356215L;
      
      private final Type type;
      private final Annotation annotations[];
      
      AnnotatedTypeImpl(Type type, Iterable<? extends Annotation> annotations) {
         this.type = type;
         this.annotations = Iterables.toArray(annotations,
               new Annotation[Iterables.trySize(annotations).orElse(0)]);
      }

      @Override
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
         return getDeclaredAnnotation(annotationClass);
      }

      @Override
      public Annotation[] getAnnotations() {
         return getDeclaredAnnotations();
      }

      @Override
      public Annotation[] getDeclaredAnnotations() {
         return annotations.clone();
      }

      @Override
      public Type getType() {
         return type;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof AnnotatedType) {
            return AnnotatedTypes.equals(this, (AnnotatedType) o);
         }
         return false;
      }

      @Override
      public int hashCode() {
         return AnnotatedTypes.hashCode(this);
      }

      @Override
      public String toString() {
         return AnnotatedTypes.toString(this);
      }
   }
   
   /**
    * Implements {@link AnnotatedParameterizedType}.
    */
   private static class AnnotatedParameterizedTypeImpl extends AnnotatedTypeImpl
         implements AnnotatedParameterizedType {
      private static final long serialVersionUID = -4933098144775956311L;

      private final AnnotatedType[] typeArguments;

      AnnotatedParameterizedTypeImpl(AnnotatedType ownerType, AnnotatedType rawType,
            List<AnnotatedType> typeArguments, List<Annotation> annotations) {
         super(new Types.ParameterizedTypeImpl(ownerType.getType(), rawType.getType(), toTypes(typeArguments)), annotations);
         this.typeArguments = typeArguments.toArray(new AnnotatedType[typeArguments.size()]);
      }

      @Override
      public AnnotatedType[] getAnnotatedActualTypeArguments() {
         return typeArguments.clone();
      }
   }
   
   /**
    * Implements {@link AnnotatedArrayType}.
    */
   private static class AnnotatedArrayTypeImpl extends AnnotatedTypeImpl
         implements AnnotatedArrayType {
      private static final long serialVersionUID = -8335550068623986776L;
      
      private final AnnotatedType componentType;
      
      AnnotatedArrayTypeImpl(AnnotatedType componentType,
            Iterable<? extends Annotation> annotations) {
         super(Types.getArrayType(componentType.getType()), annotations);
         this.componentType = componentType;
      }
      
      @Override
      public AnnotatedType getAnnotatedGenericComponentType() {
         return componentType;
      }
   }
   
   /**
    * Implements {@link AnnotatedWildcardType}.
    */
   private static class AnnotatedWildcardTypeImpl extends AnnotatedTypeImpl
         implements AnnotatedWildcardType {
      private static final long serialVersionUID = -5371665313248454547L;
      
      private final AnnotatedType upperBounds[];
      private final AnnotatedType lowerBounds[];
      
      AnnotatedWildcardTypeImpl(AnnotatedType bound, boolean isUpperBound,
            Iterable<? extends Annotation> annotations) {
         super(new Types.WildcardTypeImpl(bound.getType(), isUpperBound), annotations);
         upperBounds = new AnnotatedType[] { isUpperBound ? bound : OBJECT };
         lowerBounds = isUpperBound ? EMPTY_TYPES : new AnnotatedType[] { bound };
      }
      
      AnnotatedWildcardTypeImpl(List<AnnotatedType> upperBounds, List<AnnotatedType> lowerBounds,
            Iterable<? extends Annotation> annotations) {
         super(new Types.WildcardTypeImpl(toTypes(upperBounds), toTypes(lowerBounds)), annotations);
         this.upperBounds = upperBounds.toArray(new AnnotatedType[upperBounds.size()]);
         this.lowerBounds = lowerBounds.isEmpty() ? EMPTY_TYPES
               : lowerBounds.toArray(new AnnotatedType[lowerBounds.size()]);
      }

      @Override
      public AnnotatedType[] getAnnotatedLowerBounds() {
         return lowerBounds.clone();
      }

      @Override
      public AnnotatedType[] getAnnotatedUpperBounds() {
         return upperBounds.clone();
      }
   }
   
   /**
    * Implements {@link AnnotatedTypeVariable}.
    */
   private static class AnnotatedTypeVariableImpl extends AnnotatedTypeImpl
         implements AnnotatedTypeVariable {
      private static final long serialVersionUID = 2528423947615752140L;
      
      AnnotatedTypeVariableImpl(TypeVariable<?> typeVar,
            Iterable<? extends Annotation> annotations) {
         super(typeVar, annotations);
      }

      @Override
      public AnnotatedType[] getAnnotatedBounds() {
         return ((TypeVariable<?>) getType()).getAnnotatedBounds();
      }
   }
}
