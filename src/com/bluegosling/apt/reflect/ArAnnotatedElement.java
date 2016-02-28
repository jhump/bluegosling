package com.bluegosling.apt.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.function.Function;

import javax.lang.model.element.Element;

/**
 * An element that can have annotations. This includes representations of things in the Java
 * programming language which can be annotated, like packages, types, fields, constructors, methods,
 * and parameters.
 * 
 * <p>The Java programming language also allows local variables (including exception variables
 * defined in catch blocks) to be annotated, but support for this during annotation processing is
 * incomplete, so there currently are no such implementations.
 *
 * <p>This interface is analogous to {@link AnnotatedElement}, except that it represents a
 * source-level type (during annotation processing) instead of an actual runtime type.  Unlike
 * {@code AnnotatedElement}, this interface can be used to inspect annotations whose retention
 * policy is {@link RetentionPolicy#CLASS} or {@link RetentionPolicy#SOURCE}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotatedElement
 */
public interface ArAnnotatedElement {
   /**
    * Gets the annotation from the object that corresponds to the specified annotation type.
    * This will return an inherited annotation if one exists.
    * 
    * @param annotationClass the annotation type
    * @return the corresponding annotation or {@code null} if there is no such annotation
    */
   ArAnnotation getAnnotation(ArClass annotationClass);
   
   /**
    * Gets the annotation from the object that corresponds to the specified annotation type.
    * This will return an inherited annotation if one exists.
    * 
    * @param annotationClass the annotation type
    * @return the corresponding annotation or {@code null} if there is no such annotation
    */
   ArAnnotation getAnnotation(Class<? extends Annotation> annotationClass);
   
   /**
    * Gets an annotation bridge for the specified annotation type.
    * 
    * @param <T> the type of the annotation
    * @param annotationClass the annotation type
    * @return the corresponding {@linkplain ArAnnotationBridge annotation bridge} or {@code null} if
    *       there is no such annotation
    *       
    * @see #getAnnotation(Class)
    * @see ArAnnotationBridge#createBridge(ArAnnotation, Class)
    */
   <T extends Annotation> T getAnnotationBridge(
         Class<T> annotationClass);
   
   /**
    * Determines whether an annotation of the specified type exists on the object. This
    * includes inherited annotations.
    * 
    * @param annotationClass the annotation type
    * @return true if an annotation of the specified type is present on the object
    */
   boolean isAnnotationPresent(ArClass annotationClass);
   
   /**
    * Determines whether an annotation of the specified type exists on the object. This
    * includes inherited annotations.
    * 
    * @param annotationClass the annotation type
    * @return true if an annotation of the specified type is present on the object
    */
   boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

   /**
    * Gets all annotations for this object. This includes both annotations declared on the object
    * as well as any inherited annotations.
    * 
    * @return the list of annotations
    */
   List<ArAnnotation> getAnnotations();
   
   /**
    * Gets the annotations declared on this object.
    * 
    * @return the list of annotations
    */
   List<ArAnnotation> getDeclaredAnnotations();
   
   /**
    * Returns the underlying {@link Element} for this object.
    * 
    * @return the underlying element
    */
   Element asElement();
   
   /**
    * A function that transforms a {@link Element} to an {@link ArAnnotatedElement}.
    * 
    * <p>The function will return instances of {@link ArClass}, {@link ArField}, {@link ArMethod},
    * {@link ArConstructor}, {@link ArParameter}, or {@link ArPackage}. If the specified input element
    * does not represent one of these types of elements then {@code null} is returned.
    */
   Function<Element, ArAnnotatedElement> FROM_ELEMENT =
         (e) -> ReflectionVisitors.ANNOTATED_ELEMENT_VISITOR.visit(e);
}
