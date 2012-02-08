package com.apriori.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
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
 * <p>{@code TypeReference} to the rescue!</p>
 * 
 * <p><code>
 * // <em>Compiles, is valid, and is type safe. Yay!</em><br/>
 * Class&lt;List&lt;String&gt;&gt; clazz =
 *     new TypeReference&lt;List&lt;String&gt;&gt;() { }.asClass();
 * </code></p>
 * 
 * <p>Note that, due to type erasure, the actual class instance returned is the
 * same, regardless of generic signature:</p>
 * 
 * <p><code>
 * Class&lt;List&lt;String&gt;&gt; clazz1 =
 *     new TypeReference&lt;List&lt;String&gt;&gt;() { }.asClass();<br/>
 * Class&lt;List&lt;Integer&gt;&gt; clazz2 =
 *     new TypeReference&lt;List&lt;Integer&gt;&gt;() { }.asClass();<br/>
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
 * type information from a {@code TypeReference}.
 * 
 * <p><code>
 * // <em>Consider this complex type references:</em><br/>
 * TypeReference&lt;?&gt; mapType =<br/>
 * &nbsp; new TypeReference&lt;<br/>
 * &nbsp; &nbsp; Map&lt;<br/>
 * &nbsp; &nbsp; &nbsp; Comparable&lt;? super Number&gt;,<br/>
 * &nbsp; &nbsp; &nbsp; List&lt;Set&lt;String&gt;&gt;<br/>
 * &nbsp; &nbsp; &gt;<br/>
 * &nbsp; &gt;() { };<br/>
 * <br/>
 * // <em>We'll then extract as much information as possible:</em><br/>
 * TypeReference&lt;?&gt; comparableType =
 *   mapType.resolveTypeVariable("K");<br/>
 * &nbsp; TypeReference&lt;?&gt; numberType =
 *   comparableType.resolveTypeVariable("T");<br/>
 * TypeReference&lt;?&gt; listType =
 *   mapType.resolveTypeVariable("V");<br/>
 * &nbsp; TypeReference&lt;?&gt; setType =
 *   listType.resolveTypeVariable("E");<br/>
 * &nbsp; &nbsp; TypeReference&lt;?&gt; stringType =
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
public abstract class TypeReference<T> {
   
   private final Type type;
   private final Class<?> clazz;
   private final Class<?> subclass;

   /**
    * Constructs a new type reference. This is protected so that construction
    * looks like so:
    * <p><code>
    * TypeReference&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt; type =<br/>
    * &nbsp; new TypeReference&lt;List&lt;Map&lt;String, Number&gt;&gt;&gt;()
    * <strong>{ }</strong>;
    * </code></p>
    * Note the curly braces used to construct an anonymous sub-class of
    * {@code TypeReference}.
    * 
    * <p>The generic type must be specified. An exception will be raised
    * otherwise:
    * <p><code>
    * // <em>Bad! Generic type references another type parameter</em><br/>
    * // <em>instead of being adequately specified:</em><br/>
    * TypeReference&lt;?&gt; type =
    *     new TypeReference&lt;<strong>E</strong>&gt;() { };<br/>
    * <br/>
    * // <em>Bad! Same problem, but with an array type:</em><br/>
    * TypeReference&lt;?&gt; type =
    *     new TypeReference&lt;<strong>E</strong>[]&gt;() { };<br/>
    * <br/>
    * // <em>Good! If the generic type is also a parameterized, it is</em><br/>
    * // <em>okay for <strong>its</strong> parameter to remain</em>
    * // <em>unspecified:</em><br/>
    * TypeReference&lt;?&gt; type =
    *     new TypeReference&lt;Map&lt;<strong>E</strong>, String&gt;&gt;()
    *     { };<br/>
    * <br/>
    * // <em>Good! You can even use wildcards and bounds:</em><br/>
    * TypeReference&lt;?&gt; type =
    *     new TypeReference&lt;Map&lt;<strong>?</strong>,
    *     <strong>?</strong>&gt;&gt;() { };<br/>
    * TypeReference&lt;?&gt; type =
    *     new TypeReference&lt;List&lt;<strong>? extends Number</strong>&gt;()
    *     { };<br/>
    * </code></p>
    * 
    * @throws IllegalArgumentException if the generic type parameter is not
    *    adequately specified. Sadly, this must be a runtime exception instead
    *    of a compile error due to type erasure.
    */
   protected TypeReference() {
      // Since instantiation creates an anonymous sub-class, we know this will
      // be invoked from a sub-class constructor. So find the ancestor class
      // token that represents this class (TypeReference)
      subclass = getClass();
      type = lookupTypeVar(TypeReference.class.getTypeParameters()[0]);
      if (type == null) {
         // could not find generic type info!
         throw new IllegalArgumentException("type parameter not fully specified");
      }
      clazz = resolveClass(type);
      if (clazz == null) {
         // could not reify generic type variable!
         throw new IllegalArgumentException("type parameter not fully specified");
      }
   }
   
   private TypeReference(Type type, Class<?> clazz, Class<?> subclass) {
      this.type = type;
      this.clazz = clazz;
      this.subclass = subclass;
   }
   
   private Type lookupTypeVar(TypeVariable<?> typeVar) {
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
      // and determine resolved type for that variable if possible
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
   
   private Type findGenericSuperclass(Class<?> superclass) {
      Class<?> curr = subclass;
      while (curr.getSuperclass() != superclass) {
         curr = curr.getSuperclass();
         if (curr == null) {
            return null;
         }
      }
      return curr.getGenericSuperclass();
   }
   
   private Class<?> resolveClass(Type aType) {
      if (aType instanceof Class) {
         return (Class<?>) aType;
      } else if (aType instanceof GenericArrayType) {
         GenericArrayType gat = (GenericArrayType) aType;
         Class<?> componentType = resolveClass(gat.getGenericComponentType());
         if (componentType == null) {
            return null;
         }
         // create a Class token for this array type
         return Array.newInstance(componentType, 0).getClass();
      } else if (aType instanceof ParameterizedType) {
         ParameterizedType pt = (ParameterizedType) aType;
         return resolveClass(pt.getRawType());
      } else if (aType instanceof TypeVariable<?>) {
         TypeVariable<?> tv = (TypeVariable<?>) aType;
         Type tvResolved = lookupTypeVar(tv);
         if (tvResolved == null) {
            return null;
         } else {
            return resolveClass(tvResolved);
         }
      } else {
         // wildcards not allowed...
         return null;
      }
   }
   
   /**
    * Returns the generic {@code Type} on which this reference is based.
    * 
    * @return a {@code Type}
    */
   public Type getType() {
      return this.type;
   }
   
   private static Class<?> getBaseComponentType(Class<?> aClass) {
      if (aClass.isArray()) {
         return getBaseComponentType(aClass.getComponentType());
      } else {
         return aClass;
      }
   }
   
   private TypeVariable<Class<?>>[] getTypeVariableArray() {
      @SuppressWarnings("rawtypes")
      TypeVariable tv[] = getBaseComponentType(clazz).getTypeParameters();
      @SuppressWarnings("unchecked")
      TypeVariable<Class<?>> tvGeneric[] = tv;
      return tvGeneric;
   }
   
   public List<TypeVariable<Class<?>>> getTypeVariables() {
      return Collections.unmodifiableList(Arrays.asList(getTypeVariableArray()));
   }
   
   public List<String> getTypeVariableNames() {
      TypeVariable<Class<?>> typeVars[] = getTypeVariableArray();
      List<String> ret = new ArrayList<String>(typeVars.length);
      for (TypeVariable<Class<?>> tv : typeVars) {
         ret.add(tv.getName());
      }
      return Collections.unmodifiableList(ret);
   }
   
   public TypeVariable<Class<?>> getTypeVariable(String variableName) {
      for (TypeVariable<Class<?>> tv : getTypeVariableArray()) {
         if (tv.getName().equals(variableName)) {
            return tv;
         }
      }
      throw new IllegalArgumentException(variableName + " is not a type variable of " + clazz);
   }
   
   public TypeReference<?> resolveTypeVariable(String variableName) {
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
      Class<?> newClass = resolveClass(newType);
      if (newClass == null) {
         return null;
      }
      @SuppressWarnings({ "rawtypes", "synthetic-access", "unchecked" })
      TypeReference ret = new TypeReference(newType, newClass, subclass) {};
      return ret;
   }
   
   public ParameterizedType getParameterizedType(Type aType) {
      if (aType instanceof ParameterizedType)  {
         return (ParameterizedType) aType;
      } else if (aType instanceof GenericArrayType) {
         GenericArrayType gat = (GenericArrayType) aType;
         return getParameterizedType(gat.getGenericComponentType());
      } else if (aType instanceof TypeVariable) {
         TypeVariable<?> typeVar = (TypeVariable<?>) aType;
         return getParameterizedType(lookupTypeVar(typeVar));
      } else {
         // wildcards cannot be parameterized types...
         return null;
      }
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
   
   @Override
   public boolean equals(Object other) {
      if (other instanceof TypeReference) {
         TypeReference<?> otherType = (TypeReference<?>) other;
         return this.type.equals(otherType.type) &&
               this.clazz == otherType.clazz;
      } else {
         return false;
      }
   }
   
   @Override
   public String toString() {
      return this.type.toString();
   }
   
   @Override
   public int hashCode() {
      return 37 * this.clazz.hashCode() + this.type.hashCode();
   }
}