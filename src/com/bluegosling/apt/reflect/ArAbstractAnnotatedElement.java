package com.bluegosling.apt.reflect;

import static com.bluegosling.apt.ProcessingEnvironments.elements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * An abstract base class for implementations of {@link ArAnnotatedElement}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class ArAbstractAnnotatedElement implements ArAnnotatedElement {

   private final Element element;
   
   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected ArAbstractAnnotatedElement(Element element) {
      this.element = element;
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
      return toAnnotations(elements().getAllAnnotationMirrors(element));
   }

   @Override
   public List<ArAnnotation> getDeclaredAnnotations() {
      return toAnnotations(element.getAnnotationMirrors());
   }
   
   @Override
   public Element asElement() {
      return element;
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
   
   /**
    * Converts a list of {@link AnnotationMirror}s to a list of {@link ArAnnotation}s.
    * 
    * @param mirrors the annotation mirrors
    * @return a list of annotations
    */
   static List<ArAnnotation> toAnnotations(List<? extends AnnotationMirror> mirrors) {
      List<ArAnnotation> annotations = new ArrayList<ArAnnotation>(mirrors.size());
      for (AnnotationMirror mirror : mirrors) {
         annotations.add(ArAnnotation.forAnnotationMirror(mirror));
      }
      return annotations;
   }
}
