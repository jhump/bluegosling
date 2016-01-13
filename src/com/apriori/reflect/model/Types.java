package com.apriori.reflect.model;

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

/**
 * An extension of the standard {@link javax.lang.model.util.Types} interface that provides
 * methods for inter-op with core reflection. For example, there are methods for querying for a
 * {@link TypeMirror} corresponding to the various {@link #getTypeMirror(Type) Type} and
 * {@link #getTypeMirror(AnnotatedType) AnnotatedType} implementations.
 * 
 * <p>This interface provides factory methods to return instances backed by an
 * {@linkplain #fromProcessingEnvironment annotation processing environment} or by
 * {@linkplain #fromCoreReflection core reflection}.
 * 
 * <p><strong>Note:</strong> An instance backed by an annotation processing environment will <em>not
 * retain annotations</em> when converting {@link AnnotatedType} objects into {@link TypeMirror}s.
 * So, in such an environment, {@link #getTypeMirror(AnnotatedType)} and
 * {@link #getTypeMirror(Type)} will return the same type mirror. This is due to a limitation in the 
 * underlying {@link javax.lang.model.util.Types} API, which provides no means to manufacture type
 * mirror instances that have annotations.
 *
 * @see #fromProcessingEnvironment(ProcessingEnvironment)
 * @see #fromCoreReflection()
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Types extends javax.lang.model.util.Types {
   /**
    * Returns a type mirror that represents the same type and has the same annotations as given.
    * 
    * @param type an annotated type
    * @return a type mirror that represents the same type and has the same annotations as given
    */
   TypeMirror getTypeMirror(AnnotatedType type);
   
   /**
    * Returns a type mirror that represents the same type as given.
    * 
    * @param type a type
    * @return a type mirror that represents the same type as given
    */
   TypeMirror getTypeMirror(Type type);

   /**
    * Returns a type mirror that represents the same type as the given reference type. If the given
    * class token represents a primitive type then an exception is thrown.
    * 
    * @param clazz a class token
    * @return a type mirror that represents the same type as the given class token
    * @throws IllegalArgumentException if the given class token does not represent a reference type
    */
   ReferenceType getReferenceTypeMirror(Class<?> clazz);

   /**
    * Returns a type mirror that represents the same type as the given declared type. If the given
    * class token represents a primitive type or an array type then an exception is thrown.
    * 
    * @param clazz a class token
    * @return a type mirror that represents the same type as the given class token
    * @throws IllegalArgumentException if the given class token does not represent a declared type
    */
   DeclaredType getDeclaredTypeMirror(Class<?> clazz);
   
   /**
    * Returns a type mirror that represents the same type as the given primitive type. If the given
    * class token does not represent a primitive type then an exception is thrown.
    * 
    * @param clazz a class token
    * @return a type mirror that represents the same type as the given class token
    * @throws IllegalArgumentException if the given class token does not represent a primitive type
    */
   PrimitiveType getPrimitiveTypeMirror(Class<?> clazz);
   
   /**
    * Returns a type mirror that represents the same type as the given array type. If the given
    * class token does not represent an array type then an exception is thrown.
    * 
    * @param clazz a class token
    * @return a type mirror that represents the same type as the given class token
    * @throws IllegalArgumentException if the given class token does not represent an array type
    */
   ArrayType getArrayTypeMirror(Class<?> clazz);

   /**
    * Returns an array type mirror that represents the same type and has the same annotations as
    * given.
    * 
    * @param arrayType an annotated array type
    * @return a type mirror that represents the same type and has the same annotations as given
    */
   ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType);
   
   /**
    * Returns an array type mirror that represents the same type as given.
    * 
    * @param arrayType an array type
    * @return a type mirror that represents the same type as given
    */
   ArrayType getArrayTypeMirror(GenericArrayType arrayType);
   
   /**
    * Returns a declared type mirror that represents the same type and has the same annotations as
    * given.
    * 
    * @param parameterizedType an annotated parameterized type
    * @return a type mirror that represents the same type and has the same annotations as given
    */
   DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType);
   
   /**
    * Returns a declared type mirror that represents the same type as given.
    * 
    * @param parameterizedType a parameterized type
    * @return a type mirror that represents the same type as given
    */
   DeclaredType getParameterizedTypeMirror(ParameterizedType parameterizedType);
   
   /**
    * Returns a wildcard type mirror that represents the same type and has the same annotations as
    * given.
    * 
    * @param wildcardType an annotated wildcard type
    * @return a type mirror that represents the same type and has the same annotations as given
    */
   WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType);
   
   /**
    * Returns a wildcard type mirror that represents the same type as given.
    * 
    * @param wildcardType a wildcard type
    * @return a type mirror that represents the same type as given
    */
   WildcardType getWildcardTypeMirror(java.lang.reflect.WildcardType wildcardType);
   
   /**
    * Returns a type variable mirror that represents the same type and has the same annotations as
    * given.
    * 
    * @param typeVar an annotated type variable
    * @return a type mirror that represents the same type and has the same annotations as given
    */
   TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar);
   
   /**
    * Returns a type variable mirror that represents the same type as given.
    * 
    * @param typeVar a type variable
    * @return a type mirror that represents the same type as given
    */
   TypeVariable getTypeVariableMirror(java.lang.reflect.TypeVariable<?> typeVar);

   /**
    * Returns a {@link Types} utility class that is backed by an annotation processing
    * environment.
    *
    * @param env an annotation processing environment
    * @return a {@link Types} utility class backed by the given environment
    */
   static Types fromProcessingEnvironment(ProcessingEnvironment env) {
      javax.lang.model.util.Types base = env.getTypeUtils();
      if (base instanceof Types) {
         return (Types) base;
      }
      return new ProcessingEnvironmentTypes(env);
   }
   
   /**
    * Returns a {@link Types} utility class that is backed by core reflection.
    *
    * @return a {@link Types} utility class backed by core reflection
    */
   static Types fromCoreReflection() {
      return CoreReflectionTypes.INSTANCE;
   }
}
