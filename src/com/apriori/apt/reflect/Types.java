package com.apriori.apt.reflect;

import com.apriori.apt.reflect.Type.Kind;

import javax.lang.model.type.TypeMirror;

/**
 * Container of helper methods for working with {@link Type}s.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class Types {
   private Types() {
   }
   
   /**
    * Returns a type that corresponds to the specified type mirror.
    * 
    * @param mirror a type mirror
    * @return a corresponding type
    * @throws NullPointerException if the specified type mirror is null
    */
   public static Type forTypeMirror(TypeMirror mirror) {
      return ReflectionVisitors.TYPE_MIRROR_VISITOR.visit(mirror);
   }

   /**
    * Downcasts the specified type as a {@link Class}.
    * 
    * @param type a type
    * @return a class
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not {@link Type.Kind#CLASS}
    */
   public static Class asClass(Type type) {
      if (type.getTypeKind() != Kind.CLASS) {
         throw new IllegalArgumentException("Specified type is not a Class");
      }
      return (Class) type;
   }

   /**
    * Downcasts the specified type as a {@link GenericArrayType}.
    * 
    * @param type a type
    * @return a generic array type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link Type.Kind#GENERIC_ARRAY_TYPE}
    */
   public static GenericArrayType asGenericArrayType(Type type) {
      if (type.getTypeKind() != Kind.GENERIC_ARRAY_TYPE) {
         throw new IllegalArgumentException("Specified type is not a GenericArrayType");
      }
      return (GenericArrayType) type;
   }

   /**
    * Downcasts the specified type as a {@link ParameterizedType}.
    * 
    * @param type a type
    * @return a parameterized type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link Type.Kind#PARAMETERIZED_TYPE}
    */
   public static ParameterizedType asParameterizedType(Type type) {
      if (type.getTypeKind() != Kind.PARAMETERIZED_TYPE) {
         throw new IllegalArgumentException("Specified type is not a ParameterizedType");
      }
      return (ParameterizedType) type;
   }

   /**
    * Downcasts the specified type as a {@link TypeVariable}.
    * 
    * @param type a type
    * @return a type variable
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link Type.Kind#TYPE_VARIABLE}
    */
   public static TypeVariable<?> asTypeVariable(Type type) {
      if (type.getTypeKind() != Kind.TYPE_VARIABLE) {
         throw new IllegalArgumentException("Specified type is not a TypeVariable");
      }
      return (TypeVariable<?>) type;
   }
   
   /**
    * Downcasts the specified type as a {@link WildcardType}.
    * 
    * @param type a type
    * @return a wildcard type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link Type.Kind#WILDCARD_TYPE}
    */
   public static WildcardType asWildcardType(Type type) {
      if (type.getTypeKind() != Kind.WILDCARD_TYPE) {
         throw new IllegalArgumentException("Specified type is not a WildcardType");
      }
      return (WildcardType) type;
   }
}