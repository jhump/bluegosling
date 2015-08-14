package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public interface Types extends javax.lang.model.util.Types {
   TypeMirror getTypeMirror(AnnotatedType type);
   
   default TypeMirror getTypeMirror(Type type) {
      return getTypeMirror(AnnotatedTypes.newAnnotatedType(type));
   }

   default ReferenceType getReferenceTypeMirror(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is a primitive: " + clazz.getName());
      }
      return (ReferenceType) getTypeMirror(AnnotatedTypes.newAnnotatedType(clazz));
   }

   default DeclaredType getDeclaredTypeMirror(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is a primitive: " + clazz.getName());
      } else if (clazz.isArray()) {
         throw new IllegalArgumentException("Given class is an array: " + clazz.getName());
      }
      return (DeclaredType) getTypeMirror(AnnotatedTypes.newAnnotatedType(clazz));
   }
   
   default PrimitiveType getPrimitiveTypeMirror(Class<?> clazz) {
      if (!clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is not a primitive: " + clazz.getName());
      }
      return (PrimitiveType) getTypeMirror(AnnotatedTypes.newAnnotatedType(clazz));
   }
   
   default ArrayType getArrayTypeMirror(Class<?> clazz) {
      if (!clazz.isArray()) {
         throw new IllegalArgumentException("Given class is not an array: " + clazz.getName());
      }
      return getArrayTypeMirror((AnnotatedArrayType) AnnotatedTypes.newAnnotatedType(clazz));
   }

   ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType);
   
   default ArrayType getArrayTypeMirror(GenericArrayType arrayType) {
      return getArrayTypeMirror(AnnotatedTypes.newAnnotatedArrayType(arrayType));
   }
   
   DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType);
   
   default DeclaredType getParameterizedTypeMirror(ParameterizedType parameterizedType) {
      return getParameterizedTypeMirror(
            AnnotatedTypes.newAnnotatedParameterizedType(parameterizedType));
   }
   
   WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType);
   
   default WildcardType getWildcardTypeMirror(java.lang.reflect.WildcardType wildcardType) {
      return getWildcardTypeMirror(AnnotatedTypes.newAnnotatedWildcardType(wildcardType)); 
   }
   
   TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar);
   
   default TypeVariable getTypeVariableMirror(java.lang.reflect.TypeVariable<?> typeVar) {
      return getTypeVariableMirror(AnnotatedTypes.newAnnotatedTypeVariable(typeVar));
   }

   static Types fromProcessingEnvironment(ProcessingEnvironment env) {
      javax.lang.model.util.Types base = env.getTypeUtils();
      if (base instanceof Types) {
         return (Types) base;
      }
      return new ProcessingEnvironmentTypes(env);
   }
   
   static Types fromCoreReflection() {
      return CoreReflectionTypes.INSTANCE;
   }
}
