package com.apriori.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;

/**
 * A {@link UnionType} that is backed by multiple core reflection {@link AnnotatedType}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionUnionType implements CoreReflectionMarker, UnionType {
   private final List<TypeMirror> alternatives;
   
   CoreReflectionUnionType(AnnotatedType[] bounds) {
      List<TypeMirror> list = new ArrayList<>();
      for (AnnotatedType at : bounds) {
         list.add(CoreReflectionTypes.INSTANCE.getTypeMirror(at));
      }
      this.alternatives = Collections.unmodifiableList(list);
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
   public TypeKind getKind() {
      return TypeKind.UNION;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitUnion(this, p);
   }

   @Override
   public List<? extends TypeMirror> getAlternatives() {
      return alternatives;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof CoreReflectionUnionType) {
         CoreReflectionUnionType other = (CoreReflectionUnionType) o;
         return alternatives.equals(other.alternatives);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return alternatives.hashCode();
   }
   
   @Override
   public String toString() {
      return alternatives.stream().map(Object::toString).collect(Collectors.joining("|"));
   }
}
