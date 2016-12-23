package com.bluegosling.apt.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.AnnotatedConstruct;

/**
 * An abstract base class for implementations of {@link ArAnnotatedConstruct}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <C> the sub-type of {@link AnnotatedConstruct} represented
 */
abstract class ArAbstractAnnotatedConstruct<C extends AnnotatedConstruct>
      implements ArAnnotatedConstruct {
   
   private final C ac;
   
   /**
    * Constructs a new object based on an {@link AnnotatedConstruct}.
    * 
    * @param element the element
    */
   protected ArAbstractAnnotatedConstruct(C ac) {
      this.ac = requireNonNull(ac);
   }
   
   @Override
   public ArAnnotation getAnnotation(ArClass annotationClass) {
      for (ArAnnotation annotation : getAnnotations()) {
         if (annotation.annotationType().equals(annotationClass)) {
            return annotation;
         }
      }
      return null;
   }

   @Override
   public ArAnnotation getAnnotation(Class<? extends Annotation> annotationClass) {
      for (ArAnnotation annotation : getAnnotations()) {
         if (annotation.annotationType().getName().equals(annotationClass.getName())) {
            return annotation;
         }
      }
      return null;
   }
   
   @Override
   public <T extends Annotation> T getAnnotationBridge(Class<T> annotationClass) {
      ArAnnotation a = getAnnotation(annotationClass);
      return a == null ? null : ArAnnotationBridge.createBridge(a, annotationClass);
   }

   @Override
   public boolean isAnnotationPresent(ArClass annotationClass) {
      return getAnnotation(annotationClass) != null;
   }

   @Override
   public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
      return getAnnotation(annotationClass) != null;
   }

   @Override
   public List<ArAnnotation> getAnnotations() {
      return ArAnnotation.fromMirrors(ac.getAnnotationMirrors());
   }

   @Override
   public List<ArAnnotation> getDeclaredAnnotations() {
      return ArAnnotation.fromMirrors(ac.getAnnotationMirrors());
   }
   
   C delegate() {
      return ac;
   }
   
   // Overrides {@link Object#equals(Object)} and is abstract in order to
   // force sub-classes to implement their own version of the method.
   @Override
   public abstract boolean equals(Object o);

   // Overrides {@link Object#hashCode()} and is abstract in order to
   // force sub-classes to implement their own version of the method.
   @Override
   public abstract int hashCode();
   
   // Overrides {@link Object#toString()} and is abstract in order to
   // force sub-classes to implement their own version of the method.
   @Override
   public abstract String toString();
}
