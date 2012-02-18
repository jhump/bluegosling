package com.apriori.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A type token that represents a generic type. Unlike class tokens, which don't
 * adequately support generic classes, this can be used to better represent
 * generic types. Furthermore, it provides access to reifiable generic type
 * information: values for generic type parameters that are known at compile-time.
 * 
 * <p>When using class tokens to assist with type safety, the limitation is that
 * their generic type parameter is always a raw type:</p>
 * 
 * <p><code>
 * // <em>Not valid; won't compile:</em><br/>
 * Class&lt;List&lt;String&gt;&gt; clazz = List&lt;String&gt;.class;<br/>
 * <br/>
 * // <em>Compiles, but type of expression has reference to raw type</em><br/>
 * Class&lt;List&gt; clazz = List.class;<br/>
 * <br/>
 * // <em>This won't even compile. Argghh!</em><br/>
 * Class&lt;List&lt;?&gt;&gt; clazz = List.class;
 * </code></p>
 * 
 * <p>{@code TypeRef} to the rescue!</p>
 * 
 * <p><code>
 * // <em>Compiles, is valid, and is type safe. Yay!</em><br/>
 * Class&lt;List&lt;String&gt;&gt; clazz =
 *     new TypeRef&lt;List&lt;String&gt;&gt;() { }.asClass();
 * </code></p>
 * 
 * <p>Note that, due to type erasure, the actual class instance returned is the
 * same, regardless of generic signature:</p>
 * 
 * <p><code>
 * Class&lt;List&lt;String&gt;&gt; clazz1 =
 *     new TypeRef&lt;List&lt;String&gt;&gt;() { }.asClass();<br/>
 * Class&lt;List&lt;Integer&gt;&gt; clazz2 =
 *     new TypeRef&lt;List&lt;Integer&gt;&gt;() { }.asClass();<br/>
 * // <em>Maybe not intuitive, but sadly true:</em><br/>
 * <strong>clazz1 == clazz2</strong>
 * </code></p>
 * 
 * <p>So you have to be careful using this class tokens. For example, they cannot
 * be used as keys in a map to distinguish between {@code List<String>}
 * and {@code List<Integer>} values (a la Typesafe Heterogeneous
 * Container pattern) since the key values for these will be the same object
 * instance.</p>
 * 
 * <p>The following snippets show examples of extracting reifiable generic
 * type information from a {@code TypeRef}.
 * 
 * <p><code>
 * // <em>Consider this complex type references:</em><br/>
 * TypeRef&lt;?&gt; mapType =<br/>
 * &nbsp; new TypeRef&lt;<br/>
 * &nbsp; &nbsp; Map&lt;<br/>
 * &nbsp; &nbsp; &nbsp; Comparable&lt;? super Number&gt;,<br/>
 * &nbsp; &nbsp; &nbsp; List&lt;Set&lt;String&gt;&gt;<br/>
 * &nbsp; &nbsp; &gt;<br/>
 * &nbsp; &gt;() { };<br/>
 * <br/>
 * // <em>We'll then extract as much information as possible:</em><br/>
 * TypeRef&lt;?&gt; comparableType =
 *   mapType.resolveTypeVariable("K");<br/>
 * &nbsp; TypeRef&lt;?&gt; numberType =
 *   comparableType.resolveTypeVariable("T");<br/>
 * TypeRef&lt;?&gt; listType =
 *   mapType.resolveTypeVariable("V");<br/>
 * &nbsp; TypeRef&lt;?&gt; setType =
 *   listType.resolveTypeVariable("E");<br/>
 * &nbsp; &nbsp; TypeRef&lt;?&gt; stringType =
 *   setType.resolveTypeVariable("E");<br/>
 * <br/>
 * // <em>The following implications are then true:</em><br/>
 * mapType.asClass() <strong>=&gt;</strong> Map.class<br/>
 * mapType.getTypeVariableNames() <strong>=&gt;</strong>
 *   [ "K", "V" ]<br/>
 * comparableType.asClass() <strong>=&gt;</strong> Comparable.class<br/>
 * comparableType.getTypeVariableNames() <strong>=&gt;</strong> [ "T" ]<br/>
 * numberType == null <em>// wildcards cannot be resolved at compile
 *    time</em><br/>
 * listType.asClass() <strong>=&gt;</strong> List.class<br/>
 * listType.getTypeVariableNames() <strong>=&gt;</strong> [ "E" ]<br/>
 * setType.asClass() <strong>=&gt;</strong> Set.class<br/>
 * setType.getTypeVariableNames() <strong>=&gt;</strong> [ "E" ]<br/>
 * stringType.asClass() <strong>=&gt;</strong> String.class<br/>
 * stringType.getTypeVariableNames() <strong>=&gt;</strong> [ ]
 * </code></p>
 * 
 * @author jhumphries
 *
 * @param <T> the type represented by this token
 */
public abstract class TypeRef<T> {

   private static class TypeVar {
      private TypeVariable<Class<?>> typeVariable;
      private TypeRef<?> typeRef;
      
      public TypeVar(TypeVariable<Class<?>> typeVariable, TypeRef<?> typeRef) {
         this.typeVariable = typeVariable;
         this.typeRef = typeRef;
      }
      
      public TypeVariable<Class<?>> getTypeVarable() {
         return typeVariable;
      }
      
      public TypeRef<?> getTypeRef() {
         return typeRef;
      }
   }
   
   private static class GenericClass {
      private Class<?> clazz;
      private Type type;

      public GenericClass(Class<?> clazz) {
         this(clazz, clazz);
      }
      
      public GenericClass(Class<?> clazz, Type type) {
         this.clazz = clazz;
         this.type = type;
      }
      
      public Class<?> asClass() {
         return clazz;
      }
      
      public Type asType() {
         return type;
      }
   }
   
   private static class ConcreteTypeRef<T> extends TypeRef<T> {
      @SuppressWarnings("synthetic-access")
      public ConcreteTypeRef(GenericClass genericClass, Collection<GenericClass> searchBases, boolean includesCurrent) {
         super(genericClass, searchBases, includesCurrent);
      }
      
      @SuppressWarnings("synthetic-access")
      public ConcreteTypeRef(Class<T> clazz) {
         super(new GenericClass(clazz), Collections.singleton(new GenericClass(clazz)), true);
      }
      
      @SuppressWarnings("synthetic-access")
      public ConcreteTypeRef(Type type) {
         super(type, Collections.<GenericClass> emptySet(), false);
      }
   }
   
   public static <T> TypeRef<? super T> forClass(Class<T> clazz) {
      if (clazz == null) {
         throw new NullPointerException();
      }
      return new ConcreteTypeRef<T>(clazz);
   }
   
   public static TypeRef<?> forType(Type type) {
      if (type == null) {
         throw new NullPointerException();
      }
      return new ConcreteTypeRef<Object>(type);
   }

   private static Type findGenericSuperclass(Class<?> superclass, Class<?> baseclass) {
      Class<?> curr = baseclass;
      while (curr.getSuperclass() != superclass) {
         curr = curr.getSuperclass();
         if (curr == null) {
            return null;
         }
      }
      return curr.getGenericSuperclass();
   }
   
   private static Type findGenericInterface(Class<?> iface, Class<?> baseclass) {
      Class<?> ifaces[] = baseclass.getInterfaces();
      // first see if the interface is in this set
      for (int i = 0, len = ifaces.length; i < len; i++) {
         if (iface == ifaces[i]) {
            return baseclass.getGenericInterfaces()[i];
         }
      }
      // no? recurse
      for (Class<?> anInterface : ifaces) {
         Type t = findGenericInterface(iface, anInterface);
         if (t != null) {
            return t;
         }
      }
      // still not found? look in super-class's interfaces
      baseclass = baseclass.getSuperclass();
      if (baseclass == null) {
         return null;
      } else {
         return findGenericInterface(iface, baseclass);
      }
   }
   
   /**
    * Finds the generic type information for the specified super type (which
    * could be either a superclass or interface) of the specified base class.
    * 
    * @param superclass the super type whose information will be returned
    * @param baseclass the base class whose type hierarchy is searched
    * @return the generic type information for {@code superclass} or null
    *       if it cannot be found
    */
   private static Type findGenericSuperType(Class<?> superclass, Class<?> baseclass) {
      if (superclass == Object.class) {
         return Object.class;
      } else if (superclass.isInterface()) {
         return findGenericInterface(superclass, baseclass);
      } else {
         return findGenericSuperclass(superclass, baseclass);
      }
   }

   /**
    * Recursively determines the component type for the given class. If the
    * specified class is an array, even a nested/multi-dimensional array, it's
    * root (non-array) component type is returned. If the specified class is
    * not an array then it is returned.
    * 
    * @param aClass the class whose component type will be returned
    * @return the component type of the specified class
    */
   private static Class<?> getBaseComponentType(Class<?> aClass) {
      if (aClass.isArray()) {
         return getBaseComponentType(aClass.getComponentType());
      } else {
         return aClass;
      }
   }
   
   /** The generic class that this {@code TypeRef} represents. */
   private final GenericClass genericClass;
   
   /**
    * The generic type variables for this type, as a map of variable names to objects
    * that are either {@code TypeRef}s (for variables that can be resolved) or
    * {@code TypeVariable}s (for those that cannot be resolved).
    */
   private final LinkedHashMap<String, TypeVar> typeVariables = new LinkedHashMap<String, TypeVar>();

   /**
    * The leaves of the type hierarchy used to lookup generic type information. */
   private final Collection<GenericClass> searchBases;
   
   /**
    * A flag that indicates whether {@code searchBases} already includes the leaf
    * of the type hierarchy represented by {@code clazz}.
    */
   private final boolean includesCurrent;
   
   /**
    * Constructs a new type reference. This is protected so that construction
    * looks like so:
    * <p><code>
    * TypeRef&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt; type =<br/>
    * &nbsp; new TypeRef&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt;()
    * <strong>{ }</strong>;
    * </code></p>
    * Note the curly braces used to construct an anonymous sub-class of
    * {@code TypeRef}.
    * 
    * <p>The generic type must be specified. An exception will be raised
    * otherwise:
    * <p><code>
    * // <em>Bad! Generic type references another type parameter</em><br/>
    * // <em>instead of being adequately specified:</em><br/>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;<strong>E</strong>&gt;() { };<br/>
    * <br/>
    * // <em>Bad! Same problem, but with an array type:</em><br/>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;<strong>E</strong>[]&gt;() { };<br/>
    * <br/>
    * // <em>Good! If the generic type is also a parameterized, it is</em><br/>
    * // <em>okay for <strong>its</strong> parameter to remain</em>
    * // <em>unspecified:</em><br/>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;Map&lt;<strong>E</strong>, String&gt;&gt;()
    *     { };<br/>
    * <br/>
    * // <em>Good! You can even use wildcards and bounds:</em><br/>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;Map&lt;<strong>?</strong>,
    *     <strong>?</strong>&gt;&gt;() { };<br/>
    * TypeRef&lt;?&gt; type =
    *     new TypeRef&lt;List&lt;<strong>? extends Number</strong>&gt;()
    *     { };<br/>
    * </code></p>
    * 
    * @throws IllegalArgumentException if the generic type parameter is not
    *    adequately specified. Sadly, this must be a runtime exception instead
    *    of a compile error due to type erasure.
    */
   protected TypeRef() {
      searchBases = Collections.singleton(new GenericClass(getClass()));
      includesCurrent = false;
      Type type = findTypeForTypeVariable(TypeRef.class.getTypeParameters()[0]);
      if (type == null) {
         // could not find generic type info!
         throw new IllegalArgumentException("type parameter not fully specified");
      }
      Class<?> clazz = resolveType(type);
      if (clazz == null) {
         // could not reify generic type variable!
         throw new IllegalArgumentException("type parameter not fully specified");
      }
      genericClass = new GenericClass(clazz, type);
      populateTypeVariables();
   }

   /**
    * Constructs a new {@code TypeRef}. Private since it is only used internally.
    * 
    * @param genericClass the generic class for the newly constructed {@code TypeRef}
    * @param searchBases the leaves of the type hierarchies used to lookup
    *       generic type information
    *  @param includesCurrent whether the specified {@code searchBases}
    *       includes the leaf of the type hierarchy of the specified {@code clazz}
    */
   private TypeRef(GenericClass genericClass, Collection<GenericClass> searchBases, boolean includesCurrent) {
      assert genericClass!= null;
      assert searchBases != null;
      
      this.genericClass = genericClass;
      this.searchBases = searchBases;
      this.includesCurrent = includesCurrent;
      populateTypeVariables();
   }

   private TypeRef(Type type, Collection<GenericClass> searchBases, boolean includesCurrent) {
      assert type != null;
      assert searchBases != null;
      
      this.searchBases = searchBases;
      this.includesCurrent = includesCurrent;
      Class<?> clazz = resolveType(type);
      genericClass = new GenericClass(clazz, type);
      populateTypeVariables();
   }
   
   /**
    * Finds the {@code Type} that is the actual type argument for a given
    * {@code TypeVariable}.
    * 
    * @param typeVar the type variable
    * @return corresponding type or null if one cannot be found
    */
   private Type findTypeForTypeVariable(TypeVariable<?> typeVar) {
      GenericDeclaration gd = typeVar.getGenericDeclaration();
      if (!(gd instanceof Class)) {
         // if type variable belongs to a constructor or class, we won't
         // be able to resolve it
         return null;
      }
      Class<?> superclass = (Class<?>) gd;
      // find index of type variable
      TypeVariable<?> tvArray[] = superclass.getTypeParameters();
      int i = 0;
      for (; tvArray[i] != typeVar; i++);
      // and get actual type argument for this variable, if possible
      Type ancestorType = null;
      for (GenericClass searchBase : searchBases) {
         Class<?> searchClass = searchBase.asClass();
         if (searchClass == superclass) {
            ancestorType = searchBase.asType();
         } else if (superclass.isAssignableFrom(searchClass)) {
            ancestorType = findGenericSuperType(superclass, searchClass);
            if (ancestorType != null) {
               break;
            }
         }
      }
      if (ancestorType instanceof ParameterizedType) {
         ParameterizedType ptAncestor = (ParameterizedType) ancestorType;
         return ptAncestor.getActualTypeArguments()[i];
      } else {
         // possible use of raw type or variable's declaration isn't
         // actually in our type hierarchy...
         return null;
      }
   }
   
   /**
    * Tries to resolve the specified generic type information into a
    * {@code Class} token.
    * 
    * @param aType the type to resolve
    * @return the resolved class or null if {@code aType} cannot be resolved
    */
   private Class<?> resolveType(Type aType) {
      if (aType instanceof Class) {
         return (Class<?>) aType;
      } else if (aType instanceof GenericArrayType) {
         GenericArrayType gat = (GenericArrayType) aType;
         Class<?> componentType = resolveType(gat.getGenericComponentType());
         if (componentType == null) {
            return null;
         }
         // create a Class token for this array type
         return Array.newInstance(componentType, 0).getClass();
      } else if (aType instanceof ParameterizedType) {
         ParameterizedType pt = (ParameterizedType) aType;
         return resolveType(pt.getRawType());
      } else if (aType instanceof TypeVariable<?>) {
         TypeVariable<?> tv = (TypeVariable<?>) aType;
         Type tvType = findTypeForTypeVariable(tv);
         if (tvType== null) {
            return null;
         } else {
            return resolveType(tvType);
         }
      } else {
         // wildcards not allowed...
         return null;
      }
   }
   
   /**
    * Returns the {@code ParameterizedType} for the specified type. If the
    * specified type is an array (even a multi-dimensonal array), then a
    * {@code ParameterizedType} for its root (non-array) component type will
    * be returned.
    * 
    * @param aType the type
    * @return the {@code ParameterizedType} that represents {@code aType} or null
    *       if none can be found
    */
   private ParameterizedType getParameterizedType(Type aType) {
      if (aType instanceof ParameterizedType)  {
         return (ParameterizedType) aType;
      } else if (aType instanceof GenericArrayType) {
         GenericArrayType gat = (GenericArrayType) aType;
         return getParameterizedType(gat.getGenericComponentType());
      } else if (aType instanceof TypeVariable) {
         TypeVariable<?> typeVar = (TypeVariable<?>) aType;
         return getParameterizedType(findTypeForTypeVariable(typeVar));
      } else {
         // wildcards cannot be parameterized types...
         return null;
      }
   }
   
   private <S> void populateTypeVariables() {
      TypeVariable<?> vars[] = getBaseComponentType(asClass()).getTypeParameters();
      for (int i = 0, len = vars.length; i < len; i++) {
         @SuppressWarnings("unchecked")
         TypeVariable<Class<?>> tv = (TypeVariable<Class<?>>) vars[i];
         TypeRef<?> tr;
         ParameterizedType pType = getParameterizedType(asType());
         if (pType == null || pType.getActualTypeArguments().length <= i) {
            tr = null;
         } else {
            Type newType = pType.getActualTypeArguments()[i];
            @SuppressWarnings("unchecked")
            Class<S> newClass = (Class<S>) resolveType(newType);
            if (newClass == null) {
               tr = null;
            } else {
               tr = new ConcreteTypeRef<S>(new GenericClass(newClass, newType), searchBases, false);
            }
         }
         typeVariables.put(tv.getName(), new TypeVar(tv, tr));
      }
   }

   /**
    * Returns the names of this type's generic type variables (as declared in code).
    * 
    * @return list of type variable names
    */
   public List<String> getTypeVariableNames() {
      return Collections.unmodifiableList(new ArrayList<String>(typeVariables.keySet()));
   }

   /**
    * Returns the {@code TypeVariable} for the specified generic type
    * variable name.
    * 
    * @param variableName the variable name
    * @return the {@code TypeVariable} for {@code variableName}
    * @throws IllegalArgumentException if this type has no type variable
    *       with the specified name
    */
   public TypeVariable<Class<?>> getTypeVariable(String variableName) {
      if (variableName == null) {
         throw new NullPointerException();
      }
      TypeVar tv = typeVariables.get(variableName);
      if (tv == null) {
         throw new IllegalArgumentException(variableName + " is not a type variable of " + asClass());
      }
      return tv.getTypeVarable();
   }
   
   public boolean canResolveTypeVariable(String variableName) {
      if (variableName == null) {
         throw new NullPointerException();
      }
      TypeVar tv = typeVariables.get(variableName);
      if (tv == null) {
         throw new IllegalArgumentException(variableName + " is not a type variable of " + asClass());
      }
      return tv.getTypeRef() != null;
   }
   
   /**
    * Resolves the specified type variable into a {@code TypeRef}.
    * 
    * @param variableName the variable name
    * @return a {@code TypeRef} that represents the type of {@code variableName}
    * @throws IllegalArgumentException if this type has no type variable
    *       with the specified name
    */
   public TypeRef<?> resolveTypeVariable(String variableName) {
      if (variableName == null) {
         throw new NullPointerException();
      }
      TypeVar tv = typeVariables.get(variableName);
      if (tv == null) {
         throw new IllegalArgumentException(variableName + " is not a type variable of " + asClass());
      }
      TypeRef<?> ret = tv.getTypeRef();
      if (ret == null) {
         throw new IllegalStateException(variableName + " of " + asClass() + " cannot be resolved");
      }
      return ret;
   }
   
   public boolean isResolved() {
      for (TypeVar v : typeVariables.values()) {
         TypeRef<?> ref = v.getTypeRef();
         if (ref == null || !ref.isResolved()) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Returns a {@code Class} representation of this type. Unlike using normal
    * class tokens (e.g. {@code MyType.class}), this token properly supports
    * generic classes.
    * 
    * @return a {@code Class}
    */
   public Class<T> asClass() {
      // this 2-step process is necessary to get the compiler to let us cast
      // our Class<?> to a Class<T>
      @SuppressWarnings("rawtypes")
      Class rawClazz = genericClass.asClass();
      @SuppressWarnings("unchecked")
      Class<T> ret = rawClazz;
      return ret;
   }
   
   public Type asType() {
      return genericClass.asType();
   }
   
   private <S> TypeRef<S> newTypeRefForSuperType(Type superType, Class<S> superClass) {
      Collection<GenericClass> superTypeSearchBases;
      if (includesCurrent) {
         superTypeSearchBases = searchBases;
      } else {
         superTypeSearchBases = new ArrayList<GenericClass>(searchBases.size() + 1);
         superTypeSearchBases.add(genericClass);
         superTypeSearchBases.addAll(searchBases);
      }
      return new ConcreteTypeRef<S>(new GenericClass(superClass, superType), superTypeSearchBases, true);
   }

   public TypeRef<? super T> superTypeRef() {
      Class<?> clazz = asClass();
      @SuppressWarnings("unchecked")
      Class<? super T> superclass = (Class<? super T>) clazz.getSuperclass();
      if (superclass == null) {
         return null;
      } else {
         return newTypeRefForSuperType(clazz.getGenericSuperclass(), superclass);
      }
   }
   
   public List<TypeRef<? super T>> interfaceTypeRefs() {
      Class<?> clazz = asClass();
      @SuppressWarnings("unchecked")
      Class<? super T> ifaces[] = (Class<? super T>[]) clazz.getInterfaces();
      List<TypeRef<? super T>> ret = new ArrayList<TypeRef<? super T>>(ifaces.length);
      for (int i = 0, len = ifaces.length; i < len; i++) {
         ret.add(newTypeRefForSuperType(clazz.getGenericInterfaces()[i], ifaces[i]));
      }
      return ret;
   }
   
   public TypeRef<? super T> superTypeRefFor(Class<?> superclass) {
      Class<?> clazz = asClass();
      if (superclass == null) {
         throw new NullPointerException();
      }
      if (!superclass.isAssignableFrom(clazz)) {
         throw new IllegalArgumentException(superclass.getName() + " is not a super type of " + clazz.getName());
      }
      @SuppressWarnings("unchecked")
      Class<? super T> someClass = (Class<? super T>) superclass;
      return newTypeRefForSuperType(findGenericSuperType(superclass, clazz), someClass);
   }

   public boolean isSubTypeOf(TypeRef<?> ref) {
      if (ref == null) {
         throw new NullPointerException();
      }
      if (!ref.asClass().isAssignableFrom(asClass())) {
         return false;
      }
      // TODO
      return false;
   }

   public boolean isSuperTypeOf(TypeRef<?> ref) {
      if (ref == null) {
         throw new NullPointerException();
      }
      if (!asClass().isAssignableFrom(ref.asClass())) {
         return false;
      }
      // TODO
      return false;
   }
   
   /**
    * Compares this object to another. This object is equal to another object if
    * they are the same instance <strong>or</strong> if both objects are
    * {@code TypeRef} instances and represent the same types.
    * 
    * <p>For two {@code TypeRef} instances to represent the same types, they must
    * resolve to the same {@code Class} and have the same generic type parameters.
    * Note that two seemingly equal {@code TypeRef}s with <em>unresolved</em> type
    * variables (for example, two instances that aren't the same instance but both
    * represent {@code List<?>}) are considered <strong>not</strong> equal.
    * 
    * @param other the object against which this object is compared
    */
   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else if (other instanceof TypeRef) {
         TypeRef<?> otherRef = (TypeRef<?>) other;
         if (asClass().equals(otherRef.asClass())) {
            if (typeVariables.size() == otherRef.typeVariables.size()) {
               Iterator<Map.Entry<String, TypeVar>> iter = typeVariables.entrySet().iterator();
               Iterator<Map.Entry<String, TypeVar>> otherIter = otherRef.typeVariables.entrySet().iterator();
               while (iter.hasNext()) {
                  Map.Entry<String, TypeVar> var = iter.next();
                  Map.Entry<String, TypeVar> otherVar = otherIter.next();
                  if (var.getKey().equals(otherVar.getKey())) {
                     TypeRef<?> varRef = var.getValue().getTypeRef();
                     TypeRef<?> otherVarRef = otherVar.getValue().getTypeRef();
                     // if either is null then they are wildcards (?) so we can't know if
                     // one is equal to the other (? not necessarily equal to ?) so we must
                     // return false
                     if (varRef == null || otherVarRef == null || !varRef.equals(otherVarRef)) {
                        return false;
                     }
                  } else {
                     return false;
                  }
               }
               return true;
            }
         }
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      int hashCode = asClass().hashCode();
      for (TypeVar var : typeVariables.values()) {
         TypeRef<?> ref = var.getTypeRef();
         if (ref != null) {
            hashCode = hashCode * 37 + ref.hashCode();
         }
      }
      return hashCode;
   }
   
   private void toString(StringBuilder sb) {
      sb.append(asClass().getName());
      if (typeVariables.size() > 0) {
         sb.append("<");
         boolean first = true;
         for (Map.Entry<String, TypeVar> var : typeVariables.entrySet()) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            sb.append(var.getKey());
            sb.append("=");
            TypeRef<?> ref = var.getValue().getTypeRef();
            if (ref == null) {
               sb.append("?");
            } else {
               ref.toString(sb);
            }
         }
         sb.append(">");
      }
    }
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      toString(sb);
      return sb.toString();
   }
}