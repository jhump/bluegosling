package com.bluegosling.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

/**
 * An {@link IntersectionType} that is backed by multiple core reflection {@link AnnotatedType}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionIntersectionType implements CoreReflectionMarker, IntersectionType {

   private final List<TypeMirror> bounds;

   CoreReflectionIntersectionType(AnnotatedType[] bounds) {
      List<TypeMirror> list = new ArrayList<>(bounds.length);
      for (AnnotatedType at : bounds) {
         list.add(CoreReflectionTypes.INSTANCE.getTypeMirror(at));
      }
      this.bounds = Collections.unmodifiableList(list);
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
      return TypeKind.INTERSECTION;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitIntersection(this, p);
   }

   @Override
   public List<? extends TypeMirror> getBounds() {
      return bounds;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof CoreReflectionIntersectionType) {
         CoreReflectionIntersectionType other = (CoreReflectionIntersectionType) o;
         return bounds.equals(other.bounds);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return bounds.hashCode();
   }
   
   @Override
   public String toString() {
      return bounds.stream().map(Object::toString).collect(Collectors.joining("&"));
   }
}
