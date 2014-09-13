package com.apriori.reflect;

import java.lang.reflect.AnnotatedType;

/**
 * Numerous utility methods for using, constructing, and inspecting annotated types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotatedType
 */
// TODO
public final class AnnotatedTypes {
//   
//   static final AnnotatedType EMPTY_TYPES[] = new AnnotatedType[0];
//   static final AnnotatedTypeVariable EMPTY_TYPE_VARIABLES[] = new AnnotatedTypeVariable[0];
//   static final Class<?> ARRAY_INTERFACES[] = new Class<?>[] { Cloneable.class, Serializable.class };
//   static final Annotation EMPTY_ANNOTATIONS[] = new Annotation[0];
//
//   private AnnotatedTypes() {}
//   
//   /**
//    * Returns the component type of the given array type. If the given type does not represent an
//    * array type then {@code null} is returned.
//    *
//    * @param type a generic type
//    * @return the component type of given array type or {@code null} if the given type does not
//    *       represent an array type
//    */
//   public static AnnotatedType getComponentType(AnnotatedType type) {
//      requireNonNull(type);
//      if (type instanceof AnnotatedArrayType) {
//         return ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
//      } else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
//         AnnotatedType bounds[] = type instanceof AnnotatedWildcardType
//               ? ((AnnotatedWildcardType) type).getAnnotatedUpperBounds()
//               : ((AnnotatedTypeVariable) type).getAnnotatedBounds();
//         if (bounds.length > 0) {
//            AnnotatedType componentType = getComponentType(bounds[0]);
//            // We synthesize a new wildcard type. So a wildcard type <? extends Number[]> will
//            // return a component type of <? extends Number> instead of simply Number. Similarly, a
//            // type variable <T extends Number[]> returns a component type of <? extends Number>.
//            return componentType != null ? newExtendsWildcardType(componentType) : null;
//         }
//      }
//      // type is not an array type
//      return null;
//   }
//
//   /**
//    * Returns the generic superclass of the given type. If the given type is one of the eight
//    * primitive types or {@code void}, if it is {@code Object}, or if it is an interface then
//    * {@code null} is returned. If the given type is an array type then {@code Object} is the
//    * returned superclass.
//    * 
//    * <p>If the given type is a wildcard or type variable (and is not an interface) then its first
//    * upper bound is its superclass.
//    * 
//    * <p>This differs from {@link #getSuperclass(Type)} in that it can return a non-raw type. For
//    * example if a wildcard's bound is a parameterized type or if a class extends a parameterized
//    * type (e.g. {@code class MyClass extends ArrayList<String>}) then a parameterized type is
//    * returned. 
//    *
//    * @param type a generic type
//    * @return the superclass of the given type
//    * 
//    * @see Class#getAnnotatedSuperclass()
//    */
//   public static AnnotatedType getAnnotatedSuperclass(AnnotatedType type) {
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
//   }
//   
//   /**
//    * Returns the generic interfaces implemented by the given type. If the given type is an
//    * interface then the interfaces it directly extends are returned. If the given type is an array
//    * then an array containing {@code Serializable} and {@code Cloneable} is returned. If the given
//    * type is a class that does not directly implement any interfaces (including primitive types)
//    * then an empty array is returned.
//    * 
//    * <p>If the given type is a wildcard or type variable, then this returns an array containing any
//    * upper bounds that are interfaces.
//    * 
//    * <p>This differs from {@link #getSuperclass(Type)} in that it can return a non-raw type. For
//    * example if a wildcard type has an interface bound that is a parameterized type or if a class
//    * implements a parameterized type (e.g. {@code class MyClass implements List<String>}) then a
//    * parameterized type is returned. 
//    *
//    * @param type a generic type
//    * @return the interfaces directly implemented by the given type
//    * 
//    * @see Class#getAnnotatedInterfaces()
//    */
//   public static AnnotatedType[] getAnnotatedInterfaces(AnnotatedType type) {
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
//   }
//
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
//
//   /**
//    * Returns true if both of the given types refer to the same type or are equivalent.
//    *
//    * @param a a type
//    * @param b another type
//    * @return true if the two given types are equals; false otherwise
//    */
//   public static boolean equals(AnnotatedType a, AnnotatedType b) {
//      return requireNonNull(a) == requireNonNull(b)
//            || (Types.equals(a.getType(), b.getType())
//                  && Arrays.equals(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())
//                  && Arrays.equals(a.getAnnotations(), b.getAnnotations()));
//   }
//   
//   /**
//    * Computes a hash code for the given generic type. The generic type interfaces do not document
//    * {@code equals(Object)} or {@code hashCode()} definitions. So this method computes a stable
//    * hash, regardless of the underlying type implementation.
//    *
//    * @param type a generic type
//    * @return a hash code for the given type
//    */
//   static final int hashCode(AnnotatedType type) {
//      requireNonNull(type);
//      return Types.hashCode(type.getType())
//            + 31 * Arrays.hashCode(type.getAnnotations())
//            + 37 * Arrays.hashCode(type.getDeclaredAnnotations());
//   }
//   
//   /**
//    * Constructs a string representation of the given type. Since the generic type interfaces do not
//    * document a {@code toString()} definition, this method can be used to construct a suitable
//    * string representation, regardless of the underlying type implementation.
//    *
//    * @param type a generic type
//    * @return a string representation of the given type
//    */
//   static String toString(AnnotatedType type) {
//      requireNonNull(type);
//      StringBuilder sb = new StringBuilder();
//      toStringBuilder(type, sb);
//      return sb.toString();
//   }
//   
//   private static void toStringBuilder(AnnotatedType type, StringBuilder sb) {
//      if (type instanceof Class) {
//         Class<?> clazz = (Class<?>) type;
//         // prefer canonical name, fall back to basic name if no canonical name available
//         String name = clazz.getCanonicalName();
//         if (name != null) {
//            sb.append(name);
//         } else if (clazz.isArray()) {
//            toStringBuilder(clazz.getComponentType(), sb);
//            sb.append("[]");
//         } else {
//            sb.append(clazz.getName());
//         }
//      } else if (type instanceof ParameterizedType) {
//         ParameterizedType pt = (ParameterizedType) type;
//         Type owner = pt.getOwnerType();
//         if (owner == null) {
//            toStringBuilder(pt.getRawType(), sb);
//         } else {
//            toStringBuilder(owner, sb);
//            sb.append(".");
//            Class<?> rawType = getRawType(pt.getRawType());
//            String simpleName = rawType.getSimpleName();
//            if (simpleName.isEmpty()) {
//               // Anonymous class? This shouldn't really be possible: the Java language doesn't
//               // allow parameterized anonymous classes. But just in case: use the class name suffix
//               // (e.g. "$1") as its simple name
//               Class<?> enclosing = rawType.getEnclosingClass();
//               assert rawType.getName().startsWith(enclosing.getName());
//               simpleName = rawType.getName().substring(enclosing.getName().length());
//               assert !simpleName.isEmpty();
//            }
//            sb.append(simpleName);
//         }
//         Type args[] = pt.getActualTypeArguments();
//         if (args.length > 0) {
//            sb.append("<");
//            boolean first = true;
//            for (Type arg : args) {
//               if (first) {
//                  first = false;
//               } else {
//                  sb.append(",");
//               }
//               toStringBuilder(arg, sb);
//            }
//            sb.append(">");
//         }
//      } else if (type instanceof GenericArrayType) {
//         toStringBuilder(((GenericArrayType) type).getGenericComponentType(), sb);
//         sb.append("[]");
//      } else if (type instanceof TypeVariable) {
//         sb.append(((TypeVariable<?>) type).getName());
//      } else if (type instanceof WildcardType) {
//         WildcardType wc = (WildcardType) type;
//         Type bounds[] = wc.getLowerBounds();
//         if (bounds.length > 0) {
//            sb.append("? super ");
//         } else {
//            bounds = wc.getUpperBounds();
//            if (bounds.length == 1 && bounds[0] == Object.class) {
//               sb.append("?");
//               return;
//            }
//            sb.append("? extends ");
//         }
//         boolean first = true;
//         for (Type bound : bounds) {
//            if (first) {
//               first = false;
//            } else {
//               sb.append("&");
//            }
//            toStringBuilder(bound, sb);
//         }
//      } else {
//         // WTF?
//         sb.append(type.getTypeName());
//      }
//   }
//   
//   /**
//    * Resolves the given type variable in the context of the given type. For example, if the given
//    * type variable is {@code Collection.<E>} and the given type is the parameterized type
//    * {@code List<Optional<String>>}, then this will return {@code Optional<String>}.
//    * 
//    * <p>If the given type variable cannot be resolved then {@code null} is returned. For example,
//    * if the type variable given is {@code Map.<K>} and the given type is {@link List<Number>}, then
//    * the variable cannot be resolved.
//    *
//    * @param context the generic type whose context is used to resolve the given variable
//    * @param variable the type variable to resolve
//    * @return the resolved value of the given variable or {@code null} if it cannot be resolved
//    */
//   public static AnnotatedType resolveTypeVariable(AnnotatedType context,
//         TypeVariable<?> variable) {
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
//   }
//
//   /**
//    * Resolves the given type in the context of another type. Any type variable references in the
//    * type will be resolved using the given context. For example, if the given type is
//    * {@code Map<? extends K, ? extends V>} (where {@code K} and {@code V} are the type variables
//    * of interface {@code Map}) and the given context is the parameterized type
//    * {@code TreeMap<String, List<String>>} then the type returned will be
//    * {@code Map<? super String, ? super List<String>>}.
//    * 
//    * <p>If any type variables present in the given type cannot be resolved, they will be unchanged
//    * and continue to refer to type variables in the returned type.
//    *
//    * @param context the generic type whose context is used to resolve the given type
//    * @param typeToResolve the generic type to resolve
//    * @return the resolved type
//    */
//   public static AnnotatedType resolveType(AnnotatedType context, Type typeToResolve) {
//      Map<TypeVariableWrapper, Type> resolvedVariableValues = new HashMap<>();
//      Set<TypeVariableWrapper> resolvedVariables = new HashSet<>();
//      resolveTypeVariables(context, typeToResolve, resolvedVariableValues, resolvedVariables);
//      return replaceTypeVariablesInternal(typeToResolve, resolvedVariableValues);
//   }
//   
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
//   
//   /**
//    * Computes a new version of the given type by replacing all occurrences of a given type variable
//    * with a given value for that variable.
//    *
//    * @param type the type to be resolved
//    * @param typeVariable the type variable 
//    * @param typeValue the value that will replace the type variable
//    * @return the given type, but with references to the given type variable replaced with the given
//    *       value
//    */
//   public static AnnotatedType replaceTypeVariable(AnnotatedType type, TypeVariable<?> typeVariable,
//         AnnotatedType typeValue) {
//      Map<TypeVariableWrapper, AnnotatedType> wrappedVariables =
//            Collections.singletonMap(wrap(typeVariable), typeValue); 
//      checkTypeValue(typeVariable, typeValue, wrappedVariables);
//      return replaceTypeVariablesInternal(type, wrappedVariables);
//   }
//   
//   /**
//    * Computes a new version of the given type by replacing all occurrences of mapped type variables
//    * with the given mapped values. This provides a way to more efficiently resolve a batch of type
//    * variables instead of iteratively resolving one variable at a time.
//    *
//    * @param type the type to be resolved
//    * @param typeVariables a map of type variables to values
//    * @return the given type, but with references to the given type variables replaced with the
//    *       given mapped values
//    */
//   public static AnnotatedType replaceTypeVariables(AnnotatedType type,
//         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
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
//   }
//   
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
//   
//   /**
//    * For the given generic type, computes the generic super-type corresponding to the given raw
//    * class token. If the given generic type is not actually assignable to the given super-type
//    * token then {@code null} is returned. 
//    * 
//    * <p>For example, if the given generic type is {@code List<String>} and the given raw class
//    * token is {@code Collection.class}, then this method will resolve type parameters and return a
//    * parameterized type: {@code Collection<String>}.
//    * 
//    * <p>If the given generic type is a raw class token but represents a type with type parameters,
//    * then raw types are returned. For example, if the generic type is {@code HashMap.class} and
//    * the given raw class token is {@code Map.class}, then this method simply returns the raw type
//    * {@code Map.class}. This is also done if any super-type traversed uses raw types. For example,
//    * if the given type's super-class were defined as {@code class Xyz extends HashMap}, then the
//    * type arguments to {@code HashMap} are lost due to raw type usage and a raw type is returned.
//    * 
//    * <p>If the given generic type is a raw class token that does <em>not</em> have any type
//    * parameters, then the returned value can still be a generic type. For example, if the given
//    * type is {@code Xyz.class} and that class is defined as {@code class Xyz extends
//    * ArrayList<String>}, then querying for a super-type of {@code List.class} will return a
//    * parameterized type: {@code List<String>}.
//    * 
//    * <p>If the given generic type is a wildcard type or type variable, its super-types include all
//    * upper bounds (and their super-types), and the type's hierarchy is traversed as such.
//    * 
//    * <p>Technically, a generic array type's only super-types are {@code Object}, {@code Cloneable},
//    * and {@code Serializable}. However, since array types are co-variant, this method can resolve
//    * other super-types to which the given type is assignable. For example, if the given type is
//    * {@code HashMap<String, Number>[]} and the given raw class token queried is {@code Map[].class}
//    * then this method will resolve type parameters and return a generic type:
//    * {@code Map<String, Number>[]}.
//    *
//    * @param type a generic type
//    * @param superClass a class token for the super-type to query
//    * @return a generic type that represents the given super-type token resolved in the context of
//    *       the given type or {@code null} if the given token is not a super-type of the given
//    *       generic type
//    */
//   public static AnnotatedType resolveSuperType(AnnotatedType type, Class<?> superClass) {
//      requireNonNull(type);
//      requireNonNull(superClass);
//      Map<TypeVariableWrapper, AnnotatedType> typeVariables = new HashMap<>();
//      AnnotatedType superType = findGenericSuperType(type, superClass, typeVariables);
//      return superType != null ? replaceTypeVariablesInternal(superType, typeVariables) : null;
//   }
//
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
//      // TODO: iterative instead of recursive?
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
//      // TODO: iterative and breadth-first instead of recursive?
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
//   
//   /**
//    * Creates a new {@link GenericArrayType} object with the given component type. The component
//    * type should not be a {@link Class} since that would be a normal (e.g. not generic) array
//    * type. Also, the component type should not be a wildcard type since the component type of an
//    * array must be knowable.
//    *
//    * @param componentType the component type of the array.
//    * @return a generic array type with the given component type
//    * @throws NullPointerException if the given argument is {@code null}
//    * @throws IllegalArgumentException if the given component type is not a
//    *       {@link ParameterizedType}, {@link TypeVariable}, or {@link GenericArrayType} 
//    */
//   public static AnnotatedArrayType newGenericArrayType(AnnotatedType componentType) {
//      requireNonNull(componentType);
//      if (!(componentType instanceof ParameterizedType) && !(componentType instanceof TypeVariable)
//            && !(componentType instanceof GenericArrayType)) {
//         throw new IllegalArgumentException("GenericArrayType component should be a"
//               + " ParameterizedType, TypeVariable, or GenericArrayType");
//      }
//      return new GenericArrayTypeImpl(componentType);
//   }
//   
//   private static final Map<Class<?>, String> PRIMITIVE_CLASS_SYMBOLS;
//   static {
//      Map<Class<?>, String> symbols = new HashMap<>();
//      symbols.put(boolean.class, "Z");
//      symbols.put(byte.class, "B");
//      symbols.put(char.class, "C");
//      symbols.put(short.class, "S");
//      symbols.put(int.class, "I");
//      symbols.put(long.class, "J");
//      symbols.put(float.class, "F");
//      symbols.put(double.class, "D");
//      PRIMITIVE_CLASS_SYMBOLS = Collections.unmodifiableMap(symbols);
//   }
//   
//   /**
//    * Returns the actual type arguments if the given type is a parameterized type. Otherwise, it
//    * returns an empty array.
//    *
//    * @param type the generic type
//    * @return the actual type arguments if the given types is a parameterized type; an empty array
//    *       otherwise
//    */
//   public static AnnotatedType[] getActualTypeArguments(AnnotatedType type) {
//      requireNonNull(type);
//      if (type instanceof AnnotatedParameterizedType) {
//         return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
//      } else {
//         return EMPTY_TYPES;
//      }
//   }
//   
//   /**
//    * Creates a new {@link ParameterizedType} for a generic top-level or static type. In this case,
//    * there is no enclosing instance of a generic type, so the resulting parameterized type has no
//    * {@linkplain ParameterizedType#getOwnerType() owner type}.
//    *
//    * @param rawType the class of the parameterized type
//    * @param typeArguments the actual type arguments to the parameterized type (should not be empty)
//    * @return a new parameterized type with the given properties
//    * @throws NullPointerException if either of the given arguments is {@code null}
//    * @throws IllegalArgumentException if the raw type is a non-generic type or if no type arguments
//    *       are given, if the wrong number of type arguments are given, or if any of the type
//    *       arguments is outside the bounds for the corresponding type variable
//    */
//   public static AnnotatedParameterizedType newParameterizedType(AnnotatedType rawType,
//         AnnotatedType... typeArguments) {
//      return newParameterizedTypeInternal(null, rawType, Arrays.asList(typeArguments));
//   }
//   
//   /**
//    * Creates a new {@link ParameterizedType} for a generic top-level or static type. In this case,
//    * there is no enclosing instance of a generic type, so the resulting parameterized type has no
//    * {@linkplain ParameterizedType#getOwnerType() owner type}.
//    *
//    * @param rawType the class of the parameterized type
//    * @param typeArguments the actual type arguments to the parameterized type (should not be empty)
//    * @return a new parameterized type with the given properties
//    * @throws NullPointerException if either of the given arguments is {@code null}
//    * @throws IllegalArgumentException if the raw type is a non-generic type or if no type arguments
//    *       are given, if the wrong number of type arguments are given, or if any of the type
//    *       arguments is outside the bounds for the corresponding type variable
//    */
//   public static AnnotatedParameterizedType newParameterizedType(AnnotatedType rawType, 
//         List<AnnotatedType> typeArguments) {
//      return newParameterizedTypeInternal(null, rawType, typeArguments);
//   }
//   
//   /**
//    * Creates a new {@link ParameterizedType} for a generic enclosed type. This is necessary for
//    * representing non-static enclosed types whose enclosing type is generic, e.g. {@code
//    * GenericType<T, U>.EnclosedType}. The actual type arguments may be empty if the given type is
//    * not generic, and only an enclosing type is generic.
//    *
//    * @param ownerType the generic enclosing type
//    * @param rawType the class of the parameterized type
//    * @param typeArguments the actual type arguments to the parameterized type or an empty list if
//    *       the raw type is not a generic type
//    * @return a new parameterized type with the given properties
//    * @throws NullPointerException if any of the given arguments is {@code null}
//    * @throws IllegalArgumentException if the given raw type's owner does not match the given owner
//    *       type, if the wrong number of type arguments are given, or if any of the type arguments
//    *       is outside the bounds for the corresponding type variable
//    */
//   public static AnnotatedParameterizedType newParameterizedType(
//         AnnotatedParameterizedType ownerType, AnnotatedType rawType,
//         AnnotatedType... typeArguments) {
//      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
//            Arrays.asList(typeArguments));
//   }
//
//   /**
//    * Creates a new {@link ParameterizedType} for a generic enclosed type. This is necessary for
//    * representing non-static enclosed types whose enclosing type is generic, e.g. {@code
//    * GenericType<T, U>.EnclosedType}. The actual type arguments may be empty if the given raw type
//    * is not generic and only its owner type is generic.
//    *
//    * @param ownerType the generic enclosing type
//    * @param rawType the class of the parameterized type
//    * @param typeArguments the actual type arguments to the parameterized type or an empty list if
//    *       the raw type is not a generic type
//    * @return a new parameterized type with the given properties
//    * @throws NullPointerException if any of the given arguments is {@code null} or if any of the
//    *       elements of {@code typeArguments} is {@code null}
//    * @throws IllegalArgumentException if the given raw type's owner does not match the given owner
//    *       type, if the wrong number of type arguments are given, if any type argument is a
//    *       primitive type or {@code void}, or if any of the type arguments is outside the bounds
//    *       for the corresponding type variable
//    */
//   public static AnnotatedParameterizedType newParameterizedType(AnnotatedParameterizedType ownerType,
//         AnnotatedType rawType, List<AnnotatedType> typeArguments) {
//      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
//            typeArguments);
//   }
//   
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
//   
//   /**
//    * Creates a new {@link WildcardType} with an upper bound, i.e.&nbsp;{@code ? extends T}.
//    *
//    * @param bound the upper bound for the wildcard type
//    * @return a new wildcard type with the given bound
//    * @throws NullPointerException if the given bound is {@code null}
//    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
//    *       type
//    */
//   public static AnnotatedWildcardType newExtendsWildcardType(AnnotatedType bound) {
//      return newWildcardTypeInternal(bound, true);
//   }
//
//   /**
//    * Creates a new {@link WildcardType} with a lower bound, i.e.&nbsp;{@code ? super T}.
//    *
//    * @param bound the lower bound for the wildcard type
//    * @return a new wildcard type with the given bound
//    * @throws NullPointerException if the given bound is {@code null}
//    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
//    *       type
//    */
//   public static AnnotatedWildcardType newSuperWildcardType(AnnotatedType bound) {
//      return newWildcardTypeInternal(bound, false);
//   }
//      
//   private static AnnotatedWildcardType newWildcardTypeInternal(AnnotatedType bound,
//         boolean isUpperBound) {
//      requireNonNull(bound);
//      if (bound instanceof Class) {
//         Class<?> boundClass = (Class<?>) bound;
//         if (boundClass.isPrimitive()) {
//            throw new IllegalArgumentException("Bound for a WildcardType cannot be primitive");
//         }
//      } else if (bound instanceof WildcardType) {
//         throw new IllegalArgumentException("Bound for a WildcardType cannot be a WildcardType");
//      }
//      return new WildcardTypeImpl(bound, isUpperBound);
//   }
//   
//   private static TypeVariableWrapper wrap(TypeVariable<?> typeVariable) {
//      return new TypeVariableWrapper(typeVariable);
//   }
//
//   /**
//    * A wrapper around {@link TypeVariable}s to ensure consistent equals and hash code, for use
//    * as keys in a hash map. Since {@link TypeVariable} does not define semantics for equals and
//    * hash code, we wrap them before putting into a map instead of relying on unknown behavior of
//    * underlying implementation.
//    *
//    * @author Joshua Humphries (jhumphries131@gmail.com)
//    */
//   private static class TypeVariableWrapper {
//      final TypeVariable<?> typeVariable;
//      
//      TypeVariableWrapper(TypeVariable<?> typeVariable) {
//         this.typeVariable = typeVariable;
//      }
//      
//      @Override public boolean equals(Object o) {
//         if (o instanceof TypeVariableWrapper) {
//            return Types.equals(typeVariable, ((TypeVariableWrapper) o).typeVariable);
//         }
//         return false;
//      }
//      
//      @Override public int hashCode() {
//         return Types.hashCode(typeVariable);
//      }
//      
//      @Override public String toString() {
//         return Types.toString(typeVariable);
//      }
//   }
//   
//   /**
//    * Implements {@link ParameterizedType}.
//    */
//   private static class AnnotatedParameterizedTypeImpl implements AnnotatedParameterizedType, Serializable {
//      private static final long serialVersionUID = -4933098144775956311L;
//
//      private final Type ownerType;
//      private final Type rawType;
//      private final List<Type> typeArguments;
//
//      AnnotatedParameterizedTypeImpl(AnnotatedType ownerType, AnnotatedType rawType,
//            List<AnnotatedType> typeArguments) {
//         this.ownerType = ownerType;
//         this.rawType = rawType;
//         this.typeArguments = typeArguments;
//      }
//
//      @Override
//      public Type[] getActualTypeArguments() {
//         return typeArguments.toArray(new Type[typeArguments.size()]);
//      }
//
//      @Override
//      public Type getRawType() {
//         return rawType;
//      }
//
//      @Override
//      public Type getOwnerType() {
//         return ownerType;
//      }
//
//      @Override
//      public boolean equals(Object o) {
//         if (o instanceof ParameterizedType) {
//            return Types.equals(this, (ParameterizedType) o);
//         }
//         return false;
//      }
//
//      @Override
//      public int hashCode() {
//         return Types.hashCode(this);
//      }
//
//      @Override
//      public String toString() {
//         return Types.toString(this);
//      }
//   }
//   
//   /**
//    * Implements {@link GenericArrayType}.
//    */
//   private static class GenericArrayTypeImpl implements GenericArrayType, Serializable {
//      private static final long serialVersionUID = -8335550068623986776L;
//      
//      private final Type componentType;
//      
//      GenericArrayTypeImpl(Type componentType) {
//         this.componentType = componentType;
//      }
//      
//      @Override
//      public Type getGenericComponentType() {
//         return componentType;
//      }
//      
//      @Override
//      public boolean equals(Object o) {
//         if (o instanceof GenericArrayType) {
//            return Types.equals(this, (GenericArrayType) o);
//         }
//         return false;
//      }
//      
//      @Override
//      public int hashCode() {
//         return Types.hashCode(this);
//      }
//      
//      @Override
//      public String toString() {
//         return Types.toString(this);
//      }
//   }
//   
//   /**
//    * Implements {@link WildcardType}.
//    */
//   private static class WildcardTypeImpl implements WildcardType, Serializable {
//      private static final long serialVersionUID = -5371665313248454547L;
//      
//      private final Type upperBounds[];
//      private final Type lowerBounds[];
//      
//      WildcardTypeImpl(Type bound, boolean isUpperBound) {
//         upperBounds = new Type[] { isUpperBound ? bound : Object.class };
//         lowerBounds = isUpperBound ? EMPTY_TYPES : new Type[] { bound };
//      }
//      
//      WildcardTypeImpl(List<Type> upperBounds, List<Type> lowerBounds) {
//         this.upperBounds = upperBounds.toArray(new Type[upperBounds.size()]);
//         this.lowerBounds = lowerBounds.isEmpty() ? EMPTY_TYPES
//               : lowerBounds.toArray(new Type[lowerBounds.size()]);
//      }
//      
//      @Override
//      public Type[] getUpperBounds() {
//         return upperBounds.clone();
//      }
//
//      @Override
//      public Type[] getLowerBounds() {
//         return lowerBounds.clone();
//      }
//      
//      @Override
//      public boolean equals(Object o) {
//         if (o instanceof WildcardType) {
//            return Types.equals(this, (WildcardType) o);
//         }
//         return false;
//      }
//      
//      @Override
//      public int hashCode() {
//         return Types.hashCode(this);
//      }
//      
//      @Override
//      public String toString() {
//         return Types.toString(this);
//      }
//   }
}
