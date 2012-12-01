package com.apriori.apt.reflect;

import static com.apriori.apt.ProcessingEnvironments.elements;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * An abstract base class for implementations of {@link AnnotatedElement}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractAnnotatedElement implements AnnotatedElement {

   private final Element element;
   
   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected AbstractAnnotatedElement(Element element) {
      this.element = element;
   }
   
   @Override
   public Annotation getAnnotation(Class annotationClass) {
      for (Annotation annotation : getAnnotations()) {
         if (annotation.annotationType().equals(annotationClass)) {
            return annotation;
         }
      }
      return null;
   }

   @Override
   public Annotation getAnnotation(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass) {
      for (Annotation annotation : getAnnotations()) {
         if (annotation.annotationType().getName().equals(annotationClass.getName())) {
            return annotation;
         }
      }
      return null;
   }
   
   @Override
   public <T extends java.lang.annotation.Annotation> T getAnnotationBridge(
         java.lang.Class<T> annotationClass) {
      Annotation a = getAnnotation(annotationClass);
      return a == null ? null : AnnotationBridge.createBridge(a, annotationClass);
   }

   @Override
   public boolean isAnnotationPresent(Class annotationClass) {
      return getAnnotation(annotationClass) != null;
   }

   @Override
   public boolean isAnnotationPresent(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass) {
      return getAnnotation(annotationClass) != null;
   }

   @Override
   public List<Annotation> getAnnotations() {
      return toAnnotations(elements().getAllAnnotationMirrors(element));
   }

   @Override
   public List<Annotation> getDeclaredAnnotations() {
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
    * Converts a list of {@link AnnotationMirror}s to a list of {@link Annotation}s.
    * 
    * @param mirrors the annotation mirrors
    * @return a list of annotations
    */
   static List<Annotation> toAnnotations(List<? extends AnnotationMirror> mirrors) {
      List<Annotation> annotations = new ArrayList<Annotation>(mirrors.size());
      for (AnnotationMirror mirror : mirrors) {
         annotations.add(Annotation.forAnnotationMirror(mirror));
      }
      return annotations;
   }
}
