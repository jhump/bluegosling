package com.apriori.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;

/**
 * An abstract base type for {@link AnnotatedConstruct}s backed by core reflection. The construct is
 * backed by a core reflection {@link AnnotatedElement}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class CoreReflectionBase<E extends AnnotatedElement>
implements CoreReflectionMarker, AnnotatedConstruct {
   private final E base;
   
   CoreReflectionBase(E base) {
      this.base = base;
   }
   
   protected E base() {
      return base;
   }

   @Override
   public List<? extends AnnotationMirror> getAnnotationMirrors() {
      Annotation[] annotations = base.getAnnotations();
      List<AnnotationMirror> mirrors = new ArrayList<>(annotations.length);
      for (Annotation a : annotations) {
         mirrors.add(AnnotationMirrors.CORE_REFLECTION_INSTANCE.getAnnotationAsMirror(a));
      }
      return mirrors;
   }

   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      return base.getAnnotation(annotationType);
   }

   @Override
   public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
      return base.getAnnotationsByType(annotationType);
   }
   
   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      }
      if (o.getClass() != getClass()) {
         return false;
      }
      CoreReflectionBase<?> other = (CoreReflectionBase<?>) o;
      return base.equals(other.base);
   }
   
   @Override
   public int hashCode() {
      return base.hashCode();
   }
   
   @Override
   public String toString() {
      return base.toString();
   }
}
