package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import com.apriori.collections.TransformingList;

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
 * generic classes, this can be used to better represent generic types. Furthermore, it provides
 * access to reifiable generic type information: values for generic type parameters that are known
 * at compile-time.
 * 
 * <p>
 * When using class tokens to assist with type safety, the limitation is that their generic type
 * parameter is always a raw type:
 * </p>
 * 
 * <p>
 * <code>
 * // <em>Not valid; won't compile:</em><br>
 * Class&lt;List&lt;String&gt;&gt; clazz = List&lt;String&gt;.class;<br>
 * <br>
 * // <em>Compiles, but type of expression has reference to raw type</em><br>
 * Class&lt;List&gt; clazz = List.class;<br>
 * <br>
 * // <em>This won't even compile. Argghh!</em><br>
 * Class&lt;List&lt;?&gt;&gt; clazz = List.class;
 * </code>
 * </p>
 * 
 * <p>
 * {@code TypeRef} to the rescue!
 * </p>
 * 
 * <p>
 * <code>
 * // <em>Compiles, is valid, and is type safe. Yay!</em><br>
 * Class&lt;List&lt;String&gt;&gt; clazz =
 *     new TypeRef&lt;List&lt;String&gt;&gt;(){}.asClass();
 * </code>
 * </p>
 * 
 * <p>
 * Note that, due to type erasure, the actual class instance returned is the same, regardless of
 * generic signature:
 * </p>
 * 
 * <p>
 * <code>
 * Class&lt;List&lt;String&gt;&gt; clazz1 =
 *     new TypeRef&lt;List&lt;String&gt;&gt;(){}.asClass();<br>
 * Class&lt;List&lt;Integer&gt;&gt; clazz2 =
 *     new TypeRef&lt;List&lt;Integer&gt;&gt;(){}.asClass();<br>
 * // <em>Maybe not intuitive, but sadly true:</em><br>
 * // <strong>clazz1 == clazz2</strong>
 * </code>
 * </p>
 * 
 * <p>
 * So you have to be careful using these class tokens. For example, they cannot be used as keys in a
 * map to distinguish between {@code List<String>} and {@code List<Integer>} values (a la the
 * "Typesafe Heterogeneous Container" pattern) since the key values for these will be the same
 * object instance.
 * </p>
 * 
 * <p>
 * The following snippets show examples of extracting reifiable generic type information from a
 * {@code TypeRef}. Consider this complex type reference:
 * <p>
 * <code>
 * TypeRef&lt;?&gt; mapType =<br>
 * &nbsp; new TypeRef&lt;<br>
 * &nbsp; &nbsp; Map&lt;<br>
 * &nbsp; &nbsp; &nbsp; Comparable&lt;? super Number&gt;,<br>
 * &nbsp; &nbsp; &nbsp; List&lt;Set&lt;String&gt;&gt;<br>
 * &nbsp; &nbsp; &gt;<br>
 * &nbsp; &gt;(){};<br>
 * </code>
 * </p>
 * We'll then extract as much information as possible:
 * <p>
 * <code>
 * TypeRef&lt;?&gt; comparableType =
 *   mapType.resolveTypeVariable("K");<br>
 * &nbsp; // <em>This throws exception since wildcards can't be resolved:</em><br>
 * &nbsp; TypeRef&lt;?&gt; numberType =
 *   comparableType.resolveTypeVariable("T");<br>
 * TypeRef&lt;?&gt; listType =
 *   mapType.resolveTypeVariable("V");<br>
 * &nbsp; TypeRef&lt;?&gt; setType =
 *   listType.resolveTypeVariable("E");<br>
 * &nbsp; &nbsp; TypeRef&lt;?&gt; stringType =
 *   setType.resolveTypeVariable("E");<br>
 * </code>
 * </p>
 * The following implications are then true:
 * <p>
 * <code>
 * mapType.asClass() <strong>=&gt;</strong> Map.class<br>
 * mapType.getTypeVariableNames() <strong>=&gt;</strong>
 *   [ "K", "V" ]<br>
 * comparableType.asClass() <strong>=&gt;</strong> Comparable.class<br>
 * comparableType.getTypeVariableNames() <strong>=&gt;</strong> [ "T" ]<br>
 * listType.asClass() <strong>=&gt;</strong> List.class<br>
 * listType.getTypeVariableNames() <strong>=&gt;</strong> [ "E" ]<br>
 * setType.asClass() <strong>=&gt;</strong> Set.class<br>
 * setType.getTypeVariableNames() <strong>=&gt;</strong> [ "E" ]<br>
 * stringType.asClass() <strong>=&gt;</strong> String.class<br>
 * stringType.getTypeVariableNames() <strong>=&gt;</strong> [ ]
 * </code>
 * </p>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <T> the type represented by this token
 */
// TODO: redo doc
public abstract class TypeRef<T> implements AnnotatedElement {
   private static final TypeVariable<?> TYPE_REF_VARIABLE = TypeRef.class.getTypeParameters()[0];

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
   public static <S, T extends S> TypeRef<S> findSuperTypeRef(TypeRef<T> subType,
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
    * Constructs a new type reference. This is protected so that construction looks like so:
    * <p>
    * <code>
    * TypeRef&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt; type =<br>
    * &nbsp; new TypeRef&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt;()
    * <strong>{ }</strong>;
    * </code>
    * </p>
    * Note the curly braces used to construct an anonymous sub-class of {@code TypeRef}.
    * 
    * <p>
    * The generic type must be specified. An exception will be raised otherwise:
    * <p>
    * <code>
    * // <em>Bad! Generic type references another type parameter</em><br>
    * // <em>instead of being adequately specified:</em><br>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;<strong>E</strong>&gt;() { };<br>
    * <br>
    * // <em>Bad! Same problem, but with an array type:</em><br>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;<strong>E</strong>[]&gt;() { };<br>
    * <br>
    * // <em>Good! If the generic type is also parameterized, it is</em><br>
    * // <em>okay for <strong>its</strong> parameter to remain unspecified:</em><br>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;Map&lt;<strong>E</strong>, String&gt;&gt;()
    *     { };<br>
    * <br>
    * // <em>Good! You can even use wildcards and bounds:</em><br>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;Map&lt;<strong>?</strong>,
    *     <strong>?</strong>&gt;&gt;() { };<br>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;List&lt;<strong>? extends Number</strong>&gt;()
    *     { };<br>
    * </code>
    * </p>
    * 
    * @throws IllegalArgumentException if the generic type parameter is not adequately specified.
    *            Sadly, this must be a runtime exception instead of a compile error due to type
    *            erasure.
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
    * @return list of type variable names
    */
   public List<String> getTypeParameterNames() {
      return new TransformingList.ReadOnly<>(getTypeParameters(), tv -> tv.getName());
   }
   
   // TODO: doc
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
      Optional<TypeVariable<Class<?>>> var = getTypeParameters().stream()
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
   public TypeRef<?> resolveTypeParameter(String parameterName) {
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
   
   // TODO: doc!
   public Class<? super T> getRawType() {
      // this eyesore is to make the compiler let us cast our Class<?> to a Class<? super T>
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Class<? super T> ret = (Class) Types.getRawType(type);
      return ret;
   }

   /**
    * Returns a {@code java.lang.reflect.Type} representation of this type token.
    * 
    * @return a {@code Type}
    */
   public Type asType() {
      return type;
   }
   
   // TODO: doc
   public boolean isArray() {
      return Types.isArray(type);
   }

   // TODO: doc
   public boolean isEnum() {
      return Types.isEnum(type);
   }

   // TODO: doc
   public boolean isAnnotation() {
      return Types.isAnnotation(type);
   }

   // TODO: doc
   public boolean isPrimitive() {
      return Types.isPrimitive(type);
   }

   // TODO: doc
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
    */
   public List<TypeRef<? super T>> getInterfaceTypeRefs() {
      Type ifaces[] = Types.getGenericInterfaces(type);
      return Collections.unmodifiableList(
            Arrays.stream(ifaces).map(TypeRef::forTypeInternal).collect(Collectors.toList()));
   }
   
   /**
    * Returns a super type of the current type, as a {@code TypeRef}. This could be an ancestor
    * class of the current type or an interface implemented by the current type.
    * 
    * <p>Limitations on the expressiveness of method type variables prevent a more precise return
    * type. Consider instead using the static version, {@link #findSuperTypeRef(TypeRef, Class)}. 
    * 
    * @param superclass the super type
    * @return a {@code TypeRef} representation of the specified super type
    * @throws NullPointerException if the specified type is {@code null}
    * @throws IllegalArgumentException if the specified type is not actually a super type of the
    *       current type
    */
   public TypeRef<? super T> resolveSuperTypeRef(Class<?> superclass) {
      Type superType = Types.resolveSuperType(type, superclass);
      if (superType == null) {
         throw new IllegalArgumentException(); // TODO: message
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
      return Types.isAssignableFrom(ref.type, this.type);
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
    */
   public boolean isAssignableFrom(TypeRef<?> ref) {
      return Types.isAssignableFrom(this.type, ref.type);
   }

   /**
    * Compares this object to another. This object is equal to another object if they are the same
    * instance <strong>or</strong> if both objects are {@code TypeRef} instances and represent the
    * same types.
    * 
    * <p>
    * For two {@code TypeRef} instances to represent the same types, they must resolve to the same
    * {@code Class} and have the same generic type parameters. Note that two seemingly equal
    * {@code TypeRef}s with <em>unresolved</em> type variables (for example, two instances that
    * aren't the same instance but both represent {@code List<?>}) are considered
    * <strong>not</strong> equal.
    * 
    * @param other the object against which this object is compared
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
