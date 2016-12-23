package com.bluegosling.apt.reflect;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.AnnotatedConstruct;

/**
 * Anything that can have annotations. This includes not only {@linkplain ArAnnotatedElement program
 * elements} but also {@linkplain ArType type uses}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotatedConstruct
 */
// TODO: support repeatable annotations, fetch all annotations for type including extracting
// annotations from a repeatable annotations' containers
public interface ArAnnotatedConstruct {
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
}
