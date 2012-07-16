package com.apriori.apt.reflect;

import com.apriori.apt.reflect.Type.Kind;

import javax.lang.model.type.TypeMirror;

/**
 * Container of helper methods for working with {@link Type}s.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: javadoc!
public final class Types {
   private Types() {
   }
   
   public static Type forTypeMirror(TypeMirror mirror) {
      return ReflectionVisitors.TYPE_MIRROR_VISITOR.visit(mirror);
   }
   
   public static Class asClass(Type type) {
      if (type.getTypeKind() != Kind.CLASS) {
         throw new IllegalArgumentException("Specified type is not a Class");
      }
      return (Class) type;
   }

   public static GenericArrayType asGenericArrayType(Type type) {
      if (type.getTypeKind() != Kind.GENERIC_ARRAY_TYPE) {
         throw new IllegalArgumentException("Specified type is not a GenericArrayType");
      }
      return (GenericArrayType) type;
   }

   public static ParameterizedType asParameterizedType(Type type) {
      if (type.getTypeKind() != Kind.PARAMETERIZED_TYPE) {
         throw new IllegalArgumentException("Specified type is not a ParameterizedType");
      }
      return (ParameterizedType) type;
   }

   public static TypeVariable<?> asTypeVariable(Type type) {
      if (type.getTypeKind() != Kind.TYPE_VARIABLE) {
         throw new IllegalArgumentException("Specified type is not a TypeVariable");
      }
      return (TypeVariable<?>) type;
   }
   
   public static WildcardType asWildcardType(Type type) {
      if (type.getTypeKind() != Kind.WILDCARD_TYPE) {
         throw new IllegalArgumentException("Specified type is not a WildcardType");
      }
      return (WildcardType) type;
   }
}