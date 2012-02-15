package com.apriori.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

   /** The {@code Type} that this {@code TypeRef} represents. */
   private final Type type;
   
   /** The resolved {@code Class} for {@code type}. */
   private final Class<?> clazz;

   /** The leaf of the type hierarchy used to lookup generic type information. */
   private final Class<?> searchBase;
   
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
      searchBase = getClass();
      type = findTypeForTypeVariable(TypeRef.class.getTypeParameters()[0]);
      if (type == null) {
         // could not find generic type info!
         throw new IllegalArgumentException("type parameter not fully specified");
      }
      clazz = resolveType(type);
      if (clazz == null) {
         // could not reify generic type variable!
         throw new IllegalArgumentException("type parameter not fully specified");
      }
   }

   /**
    * Constructs a new {@code TypeRef}. Private since it is only used internally.
    * 
    * @param type the {@code Type} for this {@code TypeRef}
    * @param clazz the resolved {@code Class} for {@code type}
    * @param searchBase the leaf of the type hierarchy that is used to lookup
    *       generic type information
    */
   private TypeRef(Type type, Class<?> clazz, Class<?> searchBase) {
      this.type = type;
      this.clazz = clazz;
      this.searchBase = searchBase;
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
      Type ancestorType = findGenericSuperclass(superclass);
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
    * Finds the generic type information for the specified superclass. This
    * searches the type hierarchy of {@code typeRefLeaf}.
    * 
    * @param superclass the superclass whose information will be returned
    * @return the generic type information for {@code superclass} or null
    *       if it cannot be found
    */
   private Type findGenericSuperclass(Class<?> superclass) {
      Class<?> curr = searchBase;
      while (curr.getSuperclass() != superclass) {
         curr = curr.getSuperclass();
         if (curr == null) {
            return null;
         }
      }
      return curr.getGenericSuperclass();
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
         Type tvResolved = findTypeForTypeVariable(tv);
         if (tvResolved == null) {
            return null;
         } else {
            return resolveType(tvResolved);
         }
      } else {
         // wildcards not allowed...
         return null;
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

   /**
    * Returns the type variables for the current type. If the current type is
    * an array (even a multi-dimensonal array) then the type variables for its
    * root (non-array) component type are returned.
    * 
    * @return an array of type variables which could be empty if the current
    *       type has no generic type variables
    */
   private TypeVariable<Class<?>>[] getTypeVariableArray() {
      @SuppressWarnings("rawtypes")
      TypeVariable tv[] = getBaseComponentType(clazz).getTypeParameters();
      @SuppressWarnings("unchecked")
      TypeVariable<Class<?>> tvGeneric[] = tv;
      return tvGeneric;
   }
   
   /**
    * Returns the names of this type's generic type variables (as declared in code).
    * 
    * @return list of type variable names
    */
   public List<String> getTypeVariableNames() {
      TypeVariable<Class<?>> typeVars[] = getTypeVariableArray();
      List<String> ret = new ArrayList<String>(typeVars.length);
      for (TypeVariable<Class<?>> tv : typeVars) {
         ret.add(tv.getName());
      }
      return Collections.unmodifiableList(ret);
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
      for (TypeVariable<Class<?>> tv : getTypeVariableArray()) {
         if (tv.getName().equals(variableName)) {
            return tv;
         }
      }
      throw new IllegalArgumentException(variableName + " is not a type variable of " + clazz);
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
      TypeVariable<Class<?>> tvArray[] = getTypeVariableArray();
      int i = 0;
      for (TypeVariable<Class<?>> tv : tvArray) {
         if (tv.getName().equals(variableName)) {
            break;
         }
         i++;
      }
      if (i >= tvArray.length) {
         throw new IllegalArgumentException(variableName + " is not a type variable of " + clazz);
      }
      
      ParameterizedType pType = getParameterizedType(type);
      if (pType == null || pType.getActualTypeArguments().length <= i) {
         return null;
      }
      Type newType = pType.getActualTypeArguments()[i];
      Class<?> newClass = resolveType(newType);
      if (newClass == null) {
         return null;
      }
      @SuppressWarnings({ "rawtypes", "synthetic-access", "unchecked" })
      TypeRef ret = new TypeRef(newType, newClass, searchBase) {};
      return ret;
   }
   
   /**
    * Returns a {@code Class} representation of this type. Unlike using normal
    * class tokens (e.g. {@code MyType.class}), this token properly supports
    * generic classes.
    * 
    * @return a {@code Class}
    */
   public Class<T> asClass() {
      // this 2-step process is necessary to get the comiler to let us cast
      // our Class<?> to a Class<T>
      @SuppressWarnings("rawtypes")
      Class rawClazz = this.clazz;
      @SuppressWarnings("unchecked")
      Class<T> ret = rawClazz;
      return ret;
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
         if (clazz.equals(otherRef.clazz)) {
            TypeVariable<Class<?>> vars[] = getTypeVariableArray();
            TypeVariable<Class<?>> otherVars[] = otherRef.getTypeVariableArray();
            if (vars.length == otherVars.length) {
               for (int i = 0, len = vars.length; i < len; i++) {
                  if (vars[i].getName().equals(otherVars[i].getName())) {
                     TypeRef<?> varRef = resolveTypeVariable(vars[i].getName());
                     TypeRef<?> otherVarRef = otherRef.resolveTypeVariable(vars[i].getName());
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
      int hashCode = clazz.hashCode();
      TypeVariable<Class<?>> vars[] = getTypeVariableArray();
      for (TypeVariable<Class<?>> var : vars) {
         TypeRef<?> ref = resolveTypeVariable(var.getName());
         if (ref != null) {
            hashCode = hashCode * 37 + ref.hashCode();
         }
      }
      return hashCode;
   }
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(clazz.getName());
      TypeVariable<Class<?>> vars[] = getTypeVariableArray();
      if (vars.length > 0) {
         sb.append("<");
         boolean first = true;
         for (TypeVariable<Class<?>> var : vars) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            sb.append(var.getName());
            sb.append("=");
            TypeRef<?> ref = resolveTypeVariable(var.getName());
            if (ref == null) {
               sb.append("?");
            } else {
               sb.append(ref.toString());
            }
         }
         sb.append(">");
      }
      return sb.toString();
   }
}