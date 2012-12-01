package com.apriori.apt.reflect;

import com.apriori.util.Function;

import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.lang.model.element.Element;

/**
 * An element that can have annotations. This includes representations of things
 * in the Java programming language which can be annotated, like packages, types,
 * fields, constructors, methods, and parameters.
 * 
 * <p>The Java programming language also allows local variables (including exception
 * variables defined in catch blocks) to be annotated, but support for this during
 * annotation processing is incomplete, so there currently are no such implementations.
 *
 * <p>This interface is analogous to {@link java.lang.reflect.AnnotatedElement java.lang.reflect.AnnotatedElement},
 * except that it represents a source-level type (during annotation processing) instead
 * of an actual runtime type.  Unlike {@code java.lang.reflect.AnnotatedElement}, this
 * interface can be used to inspect annotations whose retention policy is
 * {@link RetentionPolicy#CLASS} or {@link RetentionPolicy#SOURCE}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see java.lang.reflect.AnnotatedElement
 */
public interface AnnotatedElement {
   /**
    * Gets the annotation from the object that corresponds to the specified annotation type.
    * This will return an inherited annotation if one exists.
    * 
    * @param annotationClass the annotation type
    * @return the corresponding annotation or {@code null} if there is no such annotation
    */
   Annotation getAnnotation(Class annotationClass);
   
   /**
    * Gets the annotation from the object that corresponds to the specified annotation type.
    * This will return an inherited annotation if one exists.
    * 
    * @param annotationClass the annotation type
    * @return the corresponding annotation or {@code null} if there is no such annotation
    */
   Annotation getAnnotation(java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass);
   
   /**
    * Gets an annotation bridge for the specified annotation type.
    * 
    * @param <T> the type of the annotation
    * @param annotationClass the annotation type
    * @return the corresponding {@linkplain AnnotationBridge annotation bridge} or {@code null} if
    *       there is no such annotation
    *       
    * @see #getAnnotation(Class)
    * @see AnnotationBridge#createBridge(Annotation, Class)
    */
   <T extends java.lang.annotation.Annotation> T getAnnotationBridge(
         java.lang.Class<T> annotationClass);
   
   /**
    * Determines whether an annotation of the specified type exists on the object. This
    * includes inherited annotations.
    * 
    * @param annotationClass the annotation type
    * @return true if an annotation of the specified type is present on the object
    */
   boolean isAnnotationPresent(Class annotationClass);
   
   /**
    * Determines whether an annotation of the specified type exists on the object. This
    * includes inherited annotations.
    * 
    * @param annotationClass the annotation type
    * @return true if an annotation of the specified type is present on the object
    */
   boolean isAnnotationPresent(java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass);

   /**
    * Gets all annotations for this object. This includes both annotations declared on the object
    * as well as any inherited annotations.
    * 
    * @return the list of annotations
    */
   List<Annotation> getAnnotations();
   
   /**
    * Gets the annotations declared on this object.
    * 
    * @return the list of annotations
    */
   List<Annotation> getDeclaredAnnotations();
   
   /**
    * Returns the underlying {@link Element} for this object.
    * 
    * @return the underlying element
    */
   Element asElement();
   
   /**
    * A function that transforms a {@link Element} to an {@link AnnotatedElement}.
    * 
    * <p>The function will return instances of {@link Class}, {@link Field}, {@link Method},
    * {@link Constructor}, {@link Parameter}, or {@link Package}. If the specified input element
    * does not represent one of these types of elements then {@code null} is returned.
    */
   Function<Element, AnnotatedElement> FROM_ELEMENT = new Function<Element, AnnotatedElement>() {
      @Override
      public AnnotatedElement apply(Element input) {
         return ReflectionVisitors.ANNOTATED_ELEMENT_VISITOR.visit(input);
      }
   };
}