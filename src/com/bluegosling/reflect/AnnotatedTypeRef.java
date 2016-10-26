package com.bluegosling.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bluegosling.collections.TransformingCollection;
import com.bluegosling.collections.TransformingList;

/**
 * A type token that represents an annotated type. This is very much like {@link TypeRef} in its
 * support of generic types, but also provides access to annotations on and within the type use.
 * 
 * <p>This class is effectively a wrapper around the {@link AnnotatedType} interface and provides
 * useful methods to do interesting things related to types. In a way, it takes the static utility
 * methods from {@link AnnotatedTypes} and incorporates them into a type-safe and object-oriented
 * API.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <T> the type represented by this token
 * 
 * @see AnnotatedTypes
 * @see TypeRef
 */
public abstract class AnnotatedTypeRef<T> implements AnnotatedElement {
   /**
    * A non-abstract {@code TypeRef}, used internally when building resolved {@code TypeRef}s for
    * generic type variables.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @param <T> the type represented by this token
    */
   private static class ConcreteTypeRef<T> extends AnnotatedTypeRef<T> {
      @SuppressWarnings("synthetic-access")
      private ConcreteTypeRef(AnnotatedType type) {
         super(type);
      }
   }

   /**
    * Constructs a type token that represents a specified {@code java.lang.reflect.Type}.
    * 
    * @param type the type
    * @return a {@code TypeRef} token for the given type
    */
   public static AnnotatedTypeRef<?> forType(AnnotatedType type) {
      return forTypeInternal(type);
   }

   private static <T> AnnotatedTypeRef<T> forTypeInternal(AnnotatedType type) {
      @SuppressWarnings("synthetic-access")
      AnnotatedTypeRef<T> typeRef = new ConcreteTypeRef<T>(type);
      return typeRef;
   }

   /**
    * Creates a {@link TypeRef} for an array type with the given component type.
    *
    * @param componentType the component type
    * @return an array type with the given component type
    */
   public static <T> AnnotatedTypeRef<T[]> arrayTypeRef(AnnotatedTypeRef<T> componentType,
         Annotation... annotations) {
      return forTypeInternal(
            AnnotatedTypes.newAnnotatedArrayType(componentType.asType(), annotations));
   }

   /**
    * Returns a supertype of the given type, as a {@link TypeRef}. This could be an ancestor class
    * of the given type or an interface implemented by the given type.
    * 
    * <p>This method is similar to {@link #resolveSupertypeRef(Class)} except that, being a static
    * method, can better express the relationships between the two types and provide a more precise
    * return type.
    *
    * @param subtype the type for whom a supertype is queried
    * @param supertype the supertype
    * @return a {@code TypeRef} representation of the specified supertype
    * @throws NullPointerException if either of the given types is {@code null}
    * @throws IllegalArgumentException if the given supertype is not actually a supertype of the
    *       given subtype (which the compiler will only allow when using arguments with raw types)
    */
   // Constraints on type variables mean this should be safe. Limitations in Java generics prevent
   // this from being possible, without unchecked cast, as a non-static method
   // TODO: should this return AnnotatedTypeRef<? extends S> instead since parameterized type is a
   // subtype of the raw type represented by given class token?
   @SuppressWarnings("unchecked")
   public static <S, T extends S> AnnotatedTypeRef<S> resolveSupertypeRef(
         AnnotatedTypeRef<T> subtype, Class<S> supertype) {
      return (AnnotatedTypeRef<S>) subtype.resolveSupertypeRef(supertype);
   }
   
   /** The annotated type that this {@code AnnotatedTypeRef} represents. */
   private final AnnotatedType type;
   
   /**
    * A memo-ized hash code. Computing the hash code can be non-trivial so it's memo-ized to improve
    * performance.
    */
   private transient int hashCode = -1;

   /**
    * Constructs a new type reference. This is protected so that construction requires the use of
    * a sub-class:
    * <pre>
    * TypeRef&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt; type =
    *   new TypeRef&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt;() <strong>{ }</strong>;
    * </pre>
    * Notice the curly braces, used to construct an anonymous sub-class of {@code TypeRef}.
    * 
    * <p>The generic type must be specified. An exception will be raised otherwise. For example, the
    * following invocation will generated and exception:
    * <pre>
    * TypeRef&lt;?&gt; type = new TypeRef() { };
    * </pre>
    * 
    * @throws IllegalArgumentException if the generic type parameter is not specified
    */
   protected AnnotatedTypeRef() {
      AnnotatedType genericType = AnnotatedTypes.resolveSupertype(
            getClass().getAnnotatedSuperclass(), AnnotatedTypeRef.class);
      assert genericType != null;
      if (!(genericType instanceof AnnotatedParameterizedType)) {
         throw new IllegalArgumentException("No type argument given");
      }
      this.type = ((AnnotatedParameterizedType) genericType).getAnnotatedActualTypeArguments()[0];
   }
   
   /**
    * Constructs a new {@code TypeRef}. Private since it is only used internally.
    * 
    * @param type the generic type to wrap
    */
   private AnnotatedTypeRef(AnnotatedType type) {
      this.type = requireNonNull(type);
   }

   /**
    * Returns the names of this type's generic type variables (as declared in code).
    * 
    * @return list of type variable names; an empty list if this type has no parameters
    */
   public List<String> getTypeParameterNames() {
      return new TransformingList.ReadOnly<>(Arrays.asList(Types.getTypeParameters(type.getType())),
            tv -> tv.getName());
   }
   
   /**
    * Returns this type's type variables if it has any. If the underlying generic type is a class
    * token or a parameterized type, this returns the parameters for that type. Wildcard types,
    * type variables, and generic array types have no type parameters.
    *
    * @return list of type parameters; an empty list if this type has no parameters
    */
   public List<TypeVariable<Class<?>>> getTypeParameters() {
      return Collections.unmodifiableList(Arrays.asList(Types.getTypeParameters(type.getType())));
   }

   /**
    * Returns the {@code TypeVariable} for the specified type parameter name.
    * 
    * @param variableName the variable name
    * @return the {@code TypeVariable} for {@code variableName}
    * @throws NullPointerException if the specified name is {@code null}
    * @throws IllegalArgumentException if this type has no type variable with the specified name
    */
   public TypeVariable<Class<?>> getTypeParameterNamed(String variableName) {
      requireNonNull(variableName);
      Optional<TypeVariable<Class<?>>> var = Arrays.stream(Types.getTypeParameters(type.getType()))
            .filter(tv -> tv.getName().equals(variableName)).findFirst();
      if (!var.isPresent()) {
         throw new IllegalArgumentException(variableName
               + " is not a type variable of " + Types.getErasure(type.getType()));
      }
      return var.get();
   }

   /**
    * Resolves the specified type parameter into a {@code TypeRef}.
    * 
    * @param parameterName the parameter name
    * @return a {@code TypeRef} that represents the type of {@code variableName}
    * @throws NullPointerException if the specified name is {@code null}
    * @throws IllegalArgumentException if this type has no type variable with the specified name
    */
   public AnnotatedTypeRef<?> resolveTypeParameterNamed(String parameterName) {
      TypeVariable<?> parameter = getTypeParameterNamed(parameterName);
      AnnotatedType resolvedParameter = AnnotatedTypes.resolveTypeVariable(type, parameter);
      if (resolvedParameter == null) {
         resolvedParameter = AnnotatedTypes.newAnnotatedTypeVariable(parameter);
      }
      return forTypeInternal(resolvedParameter);
   }
   
   /**
    * Returns true if this type token is resolved, which means that the actual type is known.
    * Resolved types have an underlying type that is a class, a generic array type, or a
    * parameterized type. Other types (wildcard types and type variables) are unresolved. A raw type
    * use of a parameterized type is considered unresolved.
    *
    * @return true if this type token is resolved or false otherwise
    */
   public boolean isResolved() {
      Type t = type.getType();
      if (t instanceof Class) {
         return ((Class<?>) t).getTypeParameters().length == 0;
      }
      return t instanceof GenericArrayType
            || t instanceof ParameterizedType;
   }

   /**
    * Returns true if this type token is fully resolved, which means that this type {@linkplain
    * #isResolved() is resolved} and so are the values for all type arguments (if it is a
    * parameterized type) or for the component type (if it is an array type). A raw type use of a
    * parameterized type is considered unresolved.
    * 
    * @return true if this type token is fully resolved or false otherwise
    */
   public boolean isFullyResolved() {
      return isFullyResolved(type.getType());
   }
   
   private static boolean isFullyResolved(Type type) {
      if (type instanceof Class) {
         return ((Class<?>) type).getTypeParameters().length == 0;
      } else if (type instanceof ParameterizedType) {
         ParameterizedType pType = (ParameterizedType) type;
         Type owner = pType.getOwnerType();
         if (owner != null && !isFullyResolved(owner)) {
            return false;
         }
         for (Type arg : pType.getActualTypeArguments()) {
            if (!isFullyResolved(arg)) {
               return false;
            }
         }
         return true;
      } else if (type instanceof GenericArrayType) {
         return isFullyResolved(((GenericArrayType) type).getGenericComponentType());
      } else {
         return false;
      }
   }

   /**
    * Returns the {@link AnnotatedType} representation of this type token.
    * 
    * @return a generic type
    */
   public AnnotatedType asType() {
      return type;
   }
   
   
   // TODO: doc
   public TypeRef<T> asTypeRef() {
      @SuppressWarnings("unchecked")
      TypeRef<T> ret = (TypeRef<T>) TypeRef.forType(type.getType());
      return ret;
   }

   /**
    * Returns the component type, as a {@link TypeRef}, if this represents an array type. If this
    * type is not an array type then {@code null} is returned.
    *
    * @return the component type of this array type or {@code null} if this is not an array type
    * @see AnnotatedTypes#getComponentType(AnnotatedType)
    */
   public AnnotatedTypeRef<?> getComponentTypeRef() {
      AnnotatedType componentType = AnnotatedTypes.getComponentType(type); 
      return componentType != null ? forTypeInternal(componentType) : null;
   }
   
   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
      return type.getAnnotation(annotationClass);
   }

   @Override
   public Annotation[] getAnnotations() {
      return type.getAnnotations();
   }

   @Override
   public Annotation[] getDeclaredAnnotations() {
      return type.getDeclaredAnnotations();
   }
   
   /**
    * Gets the owner type, as a {@link TypeRef}. The owner type is the declaring type of a nested
    * or inner class. Array types will not have an owner type, but their component type might.
    * Wildcard types and type variables also have no owner type, but supertypes representing their
    * upper bounds might. If this type has no owner type then {@code null} is returned.
    *
    * @return this type's owner type or {@code null} if it has no owner type
    */
   public AnnotatedTypeRef<?> getOwnerTypeRef() {
      AnnotatedType ownerType = AnnotatedTypes.getOwnerType(type);
      return ownerType != null ? forTypeInternal(ownerType) : null;
   }

   /**
    * Returns superclass of the current type, as a {@code TypeRef}.
    * 
    * <p>If the super-class is a generic type, its type arguments will be resolved based on the
    * context of the current type. For example, the supertype for a class {@code ListOfSomeType
    * extends ArrayList<SomeType>} will be {@code ArrayList<SomeType>}, and the resulting type
    * reference will have the type argument {@code E} resolved to {@code SomeType}.
    * 
    * @return the current type's superclass or {@code null} if the current type has no superclass
    *         (like if it is {@code Object}, an interface, a primitive, or {@code void})
    * @see AnnotatedTypes#getAnnotatedSuperclass(AnnotatedType)
    */
   public AnnotatedTypeRef<? super T> getSuperclassTypeRef() {
      AnnotatedType superClass = AnnotatedTypes.getAnnotatedSuperclass(type);
      return superClass != null ? forTypeInternal(superClass) : null;
   }

   /**
    * Returns the interfaces implemented by the current type, as {@code TypeRef}s. This list only
    * includes interfaces directly implemented by this type, not their super-interfaces or
    * interfaces implemented by ancestor classes. The contents and order of the returned list is the
    * same as the contents and order of elements in the array returned by
    * {@code typeRef.getRawType().getInterfaces()}. If the current type is an interface then this
    * returns its super-interfaces.
    * 
    * <p>If any of the interfaces is a generic type, its type arguments will be resolved based on
    * the context of the current type. For example, the interface for a class {@code ListOfSomeType
    * implements List<SomeType>} will be {@code List<SomeType>}, and the resulting type
    * reference will have the type argument {@code E} resolved to {@code SomeType}.
    * 
    * @return the current type's interfaces or an empty list if the current type does not directly
    *         implement any interfaces
    * @see AnnotatedTypes#getAnnotatedInterfaces(AnnotatedType)
    */
   public List<AnnotatedTypeRef<? super T>> getInterfaceTypeRefs() {
      AnnotatedType ifaces[] = AnnotatedTypes.getAnnotatedInterfaces(type);
      return Collections.unmodifiableList(
            Arrays.stream(ifaces).map(AnnotatedTypeRef::forTypeInternal)
                  .collect(Collectors.toList()));
   }
   
   /**
    * Returns a supertype of the current type, as a {@code TypeRef}. This could be an ancestor
    * class of the current type or an interface implemented by the current type.
    * 
    * @param superclass the supertype
    * @return a {@code TypeRef} representation of the specified supertype
    * @throws NullPointerException if the specified type is {@code null}
    * @throws IllegalArgumentException if the specified type is not actually a supertype of the
    *       current type
    * @see AnnotatedTypes#resolveSupertype(AnnotatedType, Class)
    */
   public AnnotatedTypeRef<? super T> resolveSupertypeRef(Class<?> superclass) {
      AnnotatedType supertype = AnnotatedTypes.resolveSupertype(type, superclass);
      if (supertype == null) {
         throw new IllegalArgumentException(
               Types.toString(superclass) + " is not a supertype of " + this);
      }
      return forTypeInternal(supertype);
   }

   /**
    * Determines if this is a <em>proper</em> subtype of the specified type token.
    * 
    * @param ref a {@code TypeRef}
    * @return true if this represents a subtype of the given type
    * @see AnnotatedTypes#isSubtype(AnnotatedType, AnnotatedType, TypeAnnotationChecker)
    */
   public boolean isSubtypeOf(AnnotatedTypeRef<?> ref, TypeAnnotationChecker checker) {
      return AnnotatedTypes.isSubtype(this.type, ref.type, checker);
   }

   // TODO: doc
   public boolean isSubtypeStrictOf(AnnotatedTypeRef<?> ref, TypeAnnotationChecker checker) {
      return AnnotatedTypes.isSubtypeStrict(this.type, ref.type, checker);
   }

   /**
    * Determines if the type this token represents can be assigned from the given type. This does
    * not consider assignment conversions other than reference widening conversion, so this can be
    * used to determine if this type is a supertype of the given type.
    * 
    * @param ref a {@code TypeRef}
    * @return true if this type is assignable from the given type
    * @see AnnotatedTypes#isAssignableStrict(AnnotatedType, AnnotatedType, TypeAnnotationChecker)
    */
   public boolean isAssignableStrictFrom(AnnotatedTypeRef<?> ref, TypeAnnotationChecker checker) {
      return AnnotatedTypes.isAssignableStrict(ref.type, this.type, checker);
   }

   // TODO: docs

   public boolean isAssignableFrom(AnnotatedTypeRef<?> ref, TypeAnnotationChecker checker) {
      return AnnotatedTypes.isAssignable(ref.type, this.type, checker);
   }

   public Collection<AnnotatedTypeRef<? super T>> getDirectSupertypeTypeRefs() {
      AnnotatedType[] supertypes = AnnotatedTypes.getAnnotatedDirectSupertypes(this.type);
      return new TransformingCollection.ReadOnly<>(Arrays.asList(supertypes),
            t -> {
               @SuppressWarnings("unchecked") // all of these types are supertypes of T
               AnnotatedTypeRef<? super T> tr =
                     (AnnotatedTypeRef<? super T>) AnnotatedTypeRef.forType(t);
               return tr;
            });
   }
   
   public boolean isFunctionalInterface() {
      return Types.isFunctionalInterface(type.getType());
   }

   /**
    * Compares this object to another. This object is equal to another object if they are both
    * {@link TypeRef}s and represent the same types.
    * 
    * @see AnnotatedTypes#equals(AnnotatedType, AnnotatedType) 
    */
   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else if (other instanceof AnnotatedTypeRef) {
         AnnotatedTypeRef<?> otherRef = (AnnotatedTypeRef<?>) other;
         return AnnotatedTypes.equals(this.type, otherRef.type);
      }
      return false;
   }

   @Override
   public int hashCode() {
      if (hashCode == -1) {
         hashCode = AnnotatedTypes.hashCode(type);
      }
      return hashCode;
   }

   @Override
   public String toString() {
      return AnnotatedTypes.toString(type);
   }
}
