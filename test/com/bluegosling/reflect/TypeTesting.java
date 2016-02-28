package com.bluegosling.reflect;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;


class TypeTesting {
   public static final Type EMPTY[] = new Type[0];

   // An implementation of Type that is not one of the known implementations (e.g. not Class,
   // ParameterizedType, GenericArrayType, WildcardType, or TypeVariable).
   public static enum InvalidType implements Type {
      INSTANCE
   }
   
   // Dummy interface. We extract generic types from its methods' return types.
   static interface Dummy<T> {
      Dummy<T> simpleTypeParam();
      <X extends Number, Y extends List<T>, Z extends Map<X, Y> & Serializable & Cloneable>
         Dummy<Z> complexTypeParam();
      
      Dummy<? extends Number> extendsWildcard();
      Dummy<? super List<T>> superWildcard();
      Dummy<? extends List<T>[]> wildcardArrayType();
      
      Dummy<Map<String, Number>[]> arrayParameterizedType();
      <X extends CharSequence> Dummy<X[]> arrayTypeParam();
      
      Dummy<List<Map<T, Integer>>> parameterizedType();
      
      <X extends Number, Y extends List<T>, Z extends Map<X, Y> & Serializable & Cloneable>
         Dummy<? extends Z[]> complexType();
   }
   
   /**
    * {@code <X extends Number, Y extends List<T>, Z extends Map<X, Y> & Serializable & Cloneable>
    * Dummy<Z> complexTypeParam()}
    */
   public static final Method GENERIC_METHOD;
   
   /** {@code List<Map<T, Integer>>} */
   public static final ParameterizedType PARAM_TYPE;
   
   /** {@code Map<String, Number>[]} */
   public static final GenericArrayType GENERIC_ARRAY_TYPE;
   
   /** {@code X[]} (where {@code X extends CharSequence}) */
   public static final GenericArrayType GENERIC_ARRAY_TYPE_VARIABLE;
   
   /** {@code T}, given {@code <T>} */
   public static final TypeVariable<?> TYPE_VAR_T;
   
   /**
    * {@code Z}, given {@code <T>} and {@code <X extends Number, Y extends List<T>, Z extends
    * Map<X, Y> & Serializable & Cloneable>} 
    */
   public static final TypeVariable<?> TYPE_VAR_Z;
   
   /** {@code ? extends Number} */
   public static final WildcardType WILDCARD_EXTENDS;
   
   /** {@code ? super List<T>}, given {@code <T>} */
   public static final WildcardType WILDCARD_SUPER;
   
   /** {@code ? extends List<T>[]}, given {@code <T>} */
   public static final WildcardType WILDCARD_ARRAY;
   
   /**
    * {@code ? extends Z[]}, given {@code <T>} and {@code <X extends Number, Y extends List<T>,
    * Z extends Map<X, Y> & Serializable & Cloneable>}
    */
   public static final Type COMPLEX_TYPE;
   
   static {
      try {
         GENERIC_METHOD = Dummy.class.getMethod("complexTypeParam");
         PARAM_TYPE = (ParameterizedType) getType("parameterizedType");
         GENERIC_ARRAY_TYPE = (GenericArrayType) getType("arrayParameterizedType");
         GENERIC_ARRAY_TYPE_VARIABLE = (GenericArrayType) getType("arrayTypeParam");
         TYPE_VAR_T = (TypeVariable<?>) getType("simpleTypeParam");
         TYPE_VAR_Z = (TypeVariable<?>) getType("complexTypeParam");
         WILDCARD_EXTENDS = (WildcardType) getType("extendsWildcard");
         WILDCARD_SUPER = (WildcardType) getType("superWildcard");
         WILDCARD_ARRAY = (WildcardType) getType("wildcardArrayType");
         COMPLEX_TYPE = getType("complexType");
      } catch (Exception e) {
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         }
         throw new RuntimeException(e);
      }
   }
   
   private static Type getType(String methodName) throws Exception {
      ParameterizedType dummyType =
            (ParameterizedType) Dummy.class.getMethod(methodName).getGenericReturnType();
      return dummyType.getActualTypeArguments()[0];
   }
}

