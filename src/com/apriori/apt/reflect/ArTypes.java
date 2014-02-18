package com.apriori.apt.reflect;

import com.apriori.apt.reflect.ArType.Kind;

import javax.lang.model.type.TypeMirror;

/**
 * Container of helper methods for working with {@link ArType}s.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class ArTypes {
   private ArTypes() {
   }
   
   /**
    * Returns a type that corresponds to the specified type mirror.
    * 
    * @param mirror a type mirror
    * @return a corresponding type
    * @throws NullPointerException if the specified type mirror is null
    */
   public static ArType forTypeMirror(TypeMirror mirror) {
      return ReflectionVisitors.TYPE_MIRROR_VISITOR.visit(mirror);
   }

   /**
    * Downcasts the specified type as a {@link ArClass}.
    * 
    * @param type a type
    * @return a class
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not {@link ArType.Kind#CLASS}
    */
   public static ArClass asClass(ArType type) {
      if (type.getTypeKind() != Kind.CLASS) {
         throw new IllegalArgumentException("Specified type is not a Class");
      }
      return (ArClass) type;
   }

   /**
    * Downcasts the specified type as a {@link ArGenericArrayType}.
    * 
    * @param type a type
    * @return a generic array type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#GENERIC_ARRAY_TYPE}
    */
   public static ArGenericArrayType asGenericArrayType(ArType type) {
      if (type.getTypeKind() != Kind.GENERIC_ARRAY_TYPE) {
         throw new IllegalArgumentException("Specified type is not a GenericArrayType");
      }
      return (ArGenericArrayType) type;
   }

   /**
    * Downcasts the specified type as a {@link ArParameterizedType}.
    * 
    * @param type a type
    * @return a parameterized type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#PARAMETERIZED_TYPE}
    */
   public static ArParameterizedType asParameterizedType(ArType type) {
      if (type.getTypeKind() != Kind.PARAMETERIZED_TYPE) {
         throw new IllegalArgumentException("Specified type is not a ParameterizedType");
      }
      return (ArParameterizedType) type;
   }

   /**
    * Downcasts the specified type as a {@link ArTypeVariable}.
    * 
    * @param type a type
    * @return a type variable
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#TYPE_VARIABLE}
    */
   public static ArTypeVariable<?> asTypeVariable(ArType type) {
      if (type.getTypeKind() != Kind.TYPE_VARIABLE) {
         throw new IllegalArgumentException("Specified type is not a TypeVariable");
      }
      return (ArTypeVariable<?>) type;
   }
   
   /**
    * Downcasts the specified type as a {@link ArWildcardType}.
    * 
    * @param type a type
    * @return a wildcard type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#WILDCARD_TYPE}
    */
   public static ArWildcardType asWildcardType(ArType type) {
      if (type.getTypeKind() != Kind.WILDCARD_TYPE) {
         throw new IllegalArgumentException("Specified type is not a WildcardType");
      }
      return (ArWildcardType) type;
   }
}