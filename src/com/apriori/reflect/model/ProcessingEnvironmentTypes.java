package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * An implementation of {@link Types} backed by an annotation processing environment. This delegates
 * most methods to a {@link javax.lang.model.util.Types} instance.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class ProcessingEnvironmentTypes implements Types {
   private final javax.lang.model.util.Types base;
   private final Elements elementUtils;
   
   ProcessingEnvironmentTypes(ProcessingEnvironment env) {
      this.base = env.getTypeUtils();
      this.elementUtils = new ProcessingEnvironmentElements(env.getElementUtils(), this);
   }
   
   ProcessingEnvironmentTypes(javax.lang.model.util.Types base, Elements elementUtils) {
      this.base = base;
      this.elementUtils = elementUtils;
   }
   
   Elements getElementUtils() {
      return elementUtils;
   }

   @Override
   public Element asElement(TypeMirror t) {
      return base.asElement(t);
   }

   @Override
   public boolean isSameType(TypeMirror t1, TypeMirror t2) {
      return base.isSameType(t1, t2);
   }

   @Override
   public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
      return base.isSubtype(t1, t2);
   }

   @Override
   public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
      return base.isAssignable(t1, t2);
   }

   @Override
   public boolean contains(TypeMirror t1, TypeMirror t2) {
      return base.contains(t1, t2);
   }

   @Override
   public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
      return base.isSubsignature(m1, m2);
   }

   @Override
   public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
      return base.directSupertypes(t);
   }

   @Override
   public TypeMirror erasure(TypeMirror t) {
      return base.erasure(t);
   }

   @Override
   public TypeElement boxedClass(PrimitiveType p) {
      return base.boxedClass(p);
   }

   @Override
   public PrimitiveType unboxedType(TypeMirror t) {
      return base.unboxedType(t);
   }

   @Override
   public TypeMirror capture(TypeMirror t) {
      return base.capture(t);
   }

   @Override
   public PrimitiveType getPrimitiveType(TypeKind kind) {
      return base.getPrimitiveType(kind);
   }

   @Override
   public NullType getNullType() {
      return base.getNullType();
   }

   @Override
   public NoType getNoType(TypeKind kind) {
      return base.getNoType(kind);
   }

   @Override
   public ArrayType getArrayType(TypeMirror componentType) {
      return base.getArrayType(componentType);
   }

   @Override
   public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
      return base.getWildcardType(extendsBound, superBound);
   }

   @Override
   public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
      return base.getDeclaredType(typeElem, typeArgs);
   }

   @Override
   public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
         TypeMirror... typeArgs) {
      return base.getDeclaredType(containing, typeElem, typeArgs);
   }

   @Override
   public TypeMirror asMemberOf(DeclaredType containing, Element element) {
      return base.asMemberOf(containing, element);
   }

   @Override
   public TypeMirror getTypeMirror(AnnotatedType type) {
      // TODO: implement me
      return null;
   }

   @Override
   public ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType) {
      // TODO: implement me
      return null;
   }

   @Override
   public DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType) {
      // TODO: implement me
      return null;
   }

   @Override
   public WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType) {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar) {
      // TODO: implement me
      return null;
   }
}
