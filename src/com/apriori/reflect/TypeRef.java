package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import com.apriori.collections.TransformingList;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A type token that represents a generic type. Unlike class tokens, which don't adequately support
 * generic classes, this can be used to represent generic types, and it provides access to reifiable
 * generic type information: values for generic type parameters that are known at compile-time. So
 * it can fully represent wildcard types, type variables, and parameterized types.
 * 
 * <p>This class is effectively a wrapper around the {@link Type} interface and provides useful
 * methods to do interesting things related to generic types. In a way, it takes the static utility
 * methods from {@link Types} and incorporates them into a type-safe and object-oriented API.
 * 
 * <p>This class implements {@link AnnotatedElement}, but the annotations do not represent
 * annotations on type uses. Rather, they represent annotations on the declared types that a given
 * {@link TypeRef} represents (see {@link Types#getAnnotations(Type)}). For annotations on type
 * use, you instead want {@link AnnotatedTypeRef}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <T> the type represented by this token
 * 
 * @see Types
 */
public abstract class TypeRef<T> implements AnnotatedElement {
   /**
    * A non-abstract {@code TypeRef}, used internally when building resolved {@code TypeRef}s for
    * generic type variables.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @param <T> the type represented by this token
    */
   private static class ConcreteTypeRef<T> extends TypeRef<T> {
      static final TypeRef<Object> OBJECT = new ConcreteTypeRef<>(Object.class);
      static final List<TypeRef<?>> ARRAY_INTERFACES = Collections.unmodifiableList(
            Arrays.asList(new ConcreteTypeRef<>(Cloneable.class),
                  new ConcreteTypeRef<>(Serializable.class)));
      
      @SuppressWarnings("synthetic-access")
      private ConcreteTypeRef(Type type) {
         super(type);
      }
   }

   /**
    * Constructs a type token that represents a specified class.
    * 
    * @param <T> the type represented by the returned token
    * @param clazz the class token for the type
    * @return a {@code TypeRef} token for the given type.
    */
   public static <T> TypeRef<T> forClass(Class<T> clazz) {
      return forTypeInternal(clazz);
   }

   /**
    * Constructs a type token that represents a specified {@code java.lang.reflect.Type}.
    * 
    * @param type the type
    * @return a {@code TypeRef} token for the given type
    */
   public static TypeRef<?> forType(Type type) {
      return forTypeInternal(type);
   }
   
   private static <T> TypeRef<T> forTypeInternal(Type type) {
      if (type == Object.class) {
         @SuppressWarnings("unchecked")
         TypeRef<T> typeRef = (TypeRef<T>) ConcreteTypeRef.OBJECT;
         return typeRef;
      } else {
         @SuppressWarnings("synthetic-access")
         TypeRef<T> typeRef = new ConcreteTypeRef<T>(type);
         return typeRef;
      }
   }

   /**
    * Creates a {@link TypeRef} for an array type with the given component type.
    *
    * @param componentType the component type
    * @return an array type with the given component type
    */
   public static <T> TypeRef<T[]> arrayTypeRef(TypeRef<T> componentType) {
      Type arrayType = componentType.type instanceof Class
            ? Types.getArrayType((Class<?>) componentType.type)
            : Types.newGenericArrayType(componentType.type);
      return forTypeInternal(arrayType);
   }

   /**
    * Returns a super-type of the given type, as a {@link TypeRef}. This could be an ancestor class
    * of the given type or an interface implemented by the given type.
    * 
    * <p>This method is similar to {@link #resolveSuperTypeRef(Class)} except that, being a static
    * method, can better express the relationships between the two types and provide a more precise
    * return type.
    *
    * @param subType the type for whom a super-type is queried
    * @param superType the super type
    * @return a {@code TypeRef} representation of the specified super type
    * @throws NullPointerException if either of the given types is {@code null}
    * @throws IllegalArgumentException if the given super type is not actually a super type of the
    *       given sub-type (which the compiler will only allow when using arguments with raw types)
    */
   // Constraints on type variables mean this should be safe. Limitations in Java generics prevent
   // this from being possible, without unchecked cast, as a non-static method
   @SuppressWarnings("unchecked")
   public static <S, T extends S> TypeRef<S> resolveSuperTypeRef(TypeRef<T> subType,
         Class<S> superType) {
      return (TypeRef<S>) subType.resolveSuperTypeRef(superType);
   }
   
   /** The type that this {@code TypeRef} represents. */
   private final Type type;
   
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
   protected TypeRef() {
      Type genericType = Types.resolveSuperType(getClass().getGenericSuperclass(), TypeRef.class);
      assert genericType != null;
      if (genericType instanceof Class) {
         throw new IllegalArgumentException("No type argument given");
      }
      this.type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
   }
   
   /**
    * Constructs a new {@code TypeRef}. Private since it is only used internally.
    * 
    * @param type the generic type to wrap
    */
   private TypeRef(Type type) {
      this.type = requireNonNull(type);
   }

   /**
    * Returns the names of this type's generic type variables (as declared in code).
    * 
    * @return list of type variable names; an empty list if this type has no parameters
    */
   public List<String> getTypeParameterNames() {
      return new TransformingList.ReadOnly<>(Arrays.asList(Types.getTypeParameters(type)),
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
      return Collections.unmodifiableList(Arrays.asList(Types.getTypeParameters(type)));
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
      Optional<TypeVariable<Class<?>>> var = Arrays.stream(Types.getTypeParameters(type))
            .filter(tv -> tv.getName().equals(variableName)).findFirst();
      if (!var.isPresent()) {
         throw new IllegalArgumentException(variableName
               + " is not a type variable of " + getRawType());
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
   public TypeRef<?> resolveTypeParameterNamed(String parameterName) {
      TypeVariable<?> parameter = getTypeParameterNamed(parameterName);
      Type resolvedParameter = Types.resolveTypeVariable(type, parameter);
      if (resolvedParameter == null) {
         resolvedParameter = parameter;
      }
      return forTypeInternal(resolvedParameter);
   }
   
   /**
    * Returns true if this type token is resolved, which means that the actual type is known.
    * Resolved types have an underlying type that is a class, a generic array type, or a
    * parameterized type. Other types (wildcard types and type variables) are unresolved.
    *
    * @return true if this type token is resolved or false otherwise
    */
   public boolean isResolved() {
      return type instanceof Class || type instanceof GenericArrayType
            || type instanceof ParameterizedType;
   }

   /**
    * Returns true if this type token is fully resolved, which means that this type {@linkplain
    * #isFullyResolved() is resolved} and so are the values for all type arguments.
    * 
    * @return true if this type token is fully resolved or false otherwise
    */
   public boolean isFullyResolved() {
      if (type instanceof Class) {
         return ((Class<?>) type).getTypeParameters().length == 0;
      } else if (type instanceof ParameterizedType) {
         for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
            TypeRef<?> argRef = forTypeInternal(arg);
            if (!argRef.isFullyResolved()) {
               return false;
            }
         }
      } else if (type instanceof GenericArrayType) {
         return getComponentTypeRef().isFullyResolved();
      } else {
         return false;
      }
      if (!isResolved()) {
         return false;
      }
      for (Type arg : Types.getActualTypeArguments(type)) {
         TypeRef<?> argRef = forTypeInternal(arg);
         if (!argRef.isFullyResolved()) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns a {@code Class} representation of this type. Unlike using normal class tokens (e.g.
    * {@code MyType.class}), this token can encode type arguments. This type can only be represented
    * as a class if it {@linkplain #isResolved() is resolved}.
    * 
    * <p>This is a convenience for using generic types with APIs that require class tokens:
    * <pre>
    * // <em>Won't compile. Generic types are invariant so List != List&lt;String&gt;</em>
    * Class&lt;List&lt;String&gt;&gt; clazz = List.class;
    * 
    * // <em>Won't compile. Bad syntax -- can't specify type parameters</em>
    * // <em>with a class literal</em>
    * Class&lt;List&lt;String&gt;&gt; clazz = List&lt;String&gt;.class;
    * 
    * // <em>TypeRef to the rescue!</em>
    * Class&lt;List&lt;String&gt;&gt; clazz =
    *     new TypeRef&lt;List&lt;String&gt;&gt;() { }.asClass();
    * </pre>
    * Note that a {@code Class<List<String>>} is the same instance as a {@code Class<List<Number>>}.
    * Under the hood, they are both {@code List.class}. So using the returned class tokens as keys
    * in a map will not work since different class tokens for the same raw type cannot be
    * distinguished. So use caution when, where, and how you use this method.
    * 
    * @return a {@code Class}
    * @throws IllegalStateException if this type is not resolved
    */
   public Class<T> asClass() {
      if (!isResolved()) {
         throw new IllegalStateException("Unresolved type cannot be represented as a class");
      }
      @SuppressWarnings("unchecked") // kind of sort of unsafe, but we intentionally allow it...
      Class<T> ret = (Class<T>) getRawType();
      return ret;
   }
   
   /**
    * Returns the raw type that corresponds to this generic type. For example, the raw type for a
    * {@code List<String>} is simply {@code List}. The raw type for wildcards and type variables
    * will be their first upper bound.
    *
    * @return the raw type that corresponds to this generic type
    * @see Types#getRawType(Type)
    */
   public Class<? super T> getRawType() {
      // this eyesore is to make the compiler let us cast our Class<?> to a Class<? super T>
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Class<? super T> ret = (Class) Types.getRawType(type);
      return ret;
   }

   /**
    * Returns the {@link Type} representation of this type token.
    * 
    * @return a generic type
    */
   public Type asType() {
      return type;
   }
   
   /**
    * Determines if this type is an interface.
    *
    * @return true if this type represents an interface
    * @see Types#isInterface(Type)
    */
   public boolean isInterface() {
      return Types.isInterface(type);
   }
   
   /**
    * Determines if this type is an array.
    *
    * @return true if this type represents an array type
    * @see Types#isArray(Type)
    */
   public boolean isArray() {
      return Types.isArray(type);
   }

   /**
    * Determines if this type is an enum.
    *
    * @return true if this type represents an enum type
    * @see Types#isEnum(Type)
    */
   public boolean isEnum() {
      return Types.isEnum(type);
   }

   /**
    * Determines if this type is an annotation.
    *
    * @return true if this type represents an annotation type
    * @see Types#isAnnotation(Type)
    */
   public boolean isAnnotation() {
      return Types.isAnnotation(type);
   }

   /**
    * Determines if this type is any of the eight primitive types or {@code void}.
    *
    * @return true if this type represents a primitive type
    * @see Types#isPrimitive(Type)
    */
   public boolean isPrimitive() {
      return Types.isPrimitive(type);
   }

   /**
    * Returns the component type, as a {@link TypeRef}, if this represents an array type. If this
    * type is not an array type then {@code null} is returned.
    *
    * @return the component type of this array type or {@code null} if this is not an array type
    * @see Types#getComponentType(Type)
    */
   public TypeRef<?> getComponentTypeRef() {
      Type componentType = Types.getComponentType(type); 
      return componentType != null ? forTypeInternal(componentType) : null;
   }
   
   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
      return Types.getAnnotation(type, annotationClass);
   }

   @Override
   public Annotation[] getAnnotations() {
      return Types.getAnnotations(type);
   }

   @Override
   public Annotation[] getDeclaredAnnotations() {
      return Types.getDeclaredAnnotations(type);
   }
   
   /**
    * Gets the owner type, as a {@link TypeRef}. The owner type is the declaring type of a nested
    * or inner class. Array types will not have an owner type, but their component type might.
    * Wildcard types and type variables also have no owner type, but super-types representing their
    * upper bounds might. If this type has no owner type then {@code null} is returned.
    *
    * @return this type's owner type or {@code null} if it has no owner type
    */
   public TypeRef<?> getOwnerTypeRef() {
      Type ownerType = Types.getOwnerType(type);
      return ownerType != null ? forTypeInternal(ownerType) : null;
   }

   /**
    * Returns superclass of the current type, as a {@code TypeRef}.
    * 
    * <p>If the super-class is a generic type, its type arguments will be resolved based on the
    * context of the current type. For example, the super-type for a class {@code ListOfSomeType
    * extends ArrayList<SomeType>} will be {@code ArrayList<SomeType>}, and the resulting type
    * reference will have the type argument {@code E} resolved to {@code SomeType}.
    * 
    * @return the current type's superclass or {@code null} if the current type has no superclass
    *         (like if it is {@code Object}, an interface, a primitive, or {@code void})
    * @see Types#getGenericSuperclass(Type)
    */
   public TypeRef<? super T> getSuperclassTypeRef() {
      Type superClass = Types.getGenericSuperclass(type);
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
    * @see Types#getGenericInterfaces(Type)
    */
   public List<TypeRef<? super T>> getInterfaceTypeRefs() {
      if (Types.isArray(type)) {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         List<TypeRef<? super T>> ifaces = (List) ConcreteTypeRef.ARRAY_INTERFACES;
         return ifaces;
      }
      Type ifaces[] = Types.getGenericInterfaces(type);
      return Collections.unmodifiableList(
            Arrays.stream(ifaces).map(TypeRef::forTypeInternal).collect(Collectors.toList()));
   }
   
   /**
    * Returns a super type of the current type, as a {@code TypeRef}. This could be an ancestor
    * class of the current type or an interface implemented by the current type.
    * 
    * <p>Limitations on the expressiveness of method type variables prevent a more precise return
    * type. Consider instead using the static version, {@link #resolveSuperTypeRef(TypeRef, Class)}. 
    * 
    * @param superclass the super type
    * @return a {@code TypeRef} representation of the specified super type
    * @throws NullPointerException if the specified type is {@code null}
    * @throws IllegalArgumentException if the specified type is not actually a super type of the
    *       current type
    * @see Types#resolveSuperType(Type, Class)
    */
   public TypeRef<? super T> resolveSuperTypeRef(Class<?> superclass) {
      Type superType = Types.resolveSuperType(type, superclass);
      if (superType == null) {
         throw new IllegalArgumentException(
               Types.toString(superclass) + " is not a super type of " + this);
      }
      return forTypeInternal(superType);
   }

   /**
    * Determines if this is a sub-type of the specified type token. This has the same caveats as
    * does {@link #equals(Object)} in that wildcard and type variables are not "captured" and
    * wildcard bounds are not checked for compatibility. If either this type token or the specified
    * other token is not fully resolved then this will return false, even if the Java compiler might
    * treat it as a sub-type due to compatible bounds.
    * 
    * @param ref a {@code Type Ref}
    * @return true if this represents a sub-type of {@code ref}
    */
   public boolean isSubTypeOf(TypeRef<?> ref) {
      return Types.isAssignableStrict(ref.type, this.type);
   }

   /**
    * Determines if this is a super type of the specified type token. This has the same caveats as
    * does {@link #equals(Object)} in that wildcard and type variables are not "captured" and 
    * wildcard bounds are not checked for compatibility. If either this type token or the specified
    * other token are is fully resolved then this will return false, even if the Java compiler might
    * treat it as a super type due to compatible bounds.
    * 
    * @param ref a {@code TypeRef}
    * @return true if this represents a super type of {@code ref}
    * @see Types#isAssignableStrict(Type, Type)
    */
   public boolean isAssignableFrom(TypeRef<?> ref) {
      return Types.isAssignableStrict(this.type, ref.type);
   }

   /**
    * Compares this object to another. This object is equal to another object if they are both
    * {@link TypeRef}s and represent the same types.
    * 
    * @see Types#equals(Object) 
    */
   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else if (other instanceof TypeRef) {
         TypeRef<?> otherRef = (TypeRef<?>) other;
         return Types.equals(this.type, otherRef.type);
      }
      return false;
   }

   @Override
   public int hashCode() {
      if (hashCode == -1) {
         hashCode = Types.hashCode(type);
      }
      return hashCode;
   }

   @Override
   public String toString() {
      return Types.toString(type);
   }
}
