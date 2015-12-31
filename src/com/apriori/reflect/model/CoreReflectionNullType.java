package com.apriori.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

/**
 * The {@linkplain TypeKind#NULL null} type. Since null types cannot be annotated, there is no need
 * for more than one instance, and thus this type is a singleton.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
enum CoreReflectionNullType implements CoreReflectionMarker, NullType {
   INSTANCE;
   
   @Override
   public TypeKind getKind() {
      return TypeKind.NULL;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitNull(this, p);
   }

   @Override
   public List<? extends AnnotationMirror> getAnnotationMirrors() {
      return Collections.emptyList();
   }

   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      return null;
   }

   @SuppressWarnings("unchecked")
   @Override
   public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
      return (A[]) Array.newInstance(annotationType, 0);
   }
   
   @Override
   public String toString() {
      return ":null:";
   }
}
