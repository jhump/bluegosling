package com.bluegosling.apt.reflect;

import com.bluegosling.apt.AbstractProcessor;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * An abstract annotation processor whose methods is defined in terms of the reflection-like API in
 * this package instead of the standard element and type mirror APIs.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class ArProcessor extends AbstractProcessor {
   
   /**
    * The environment of the current processing round. This provides a facility for querying for
    * annotated elements that are currently being processed. 
    */
   protected ArRoundEnvironment roundEnv;

   /**
    * Translates the given element and round environment into reflection-like types and then
    * delegates to {@link #process(Set)}.
    */
   @Override
   protected final boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment env) {
      roundEnv = new ArRoundEnvironment(env);
      try {
         Set<ArClass> classes = new HashSet<ArClass>();
         for (TypeElement annotation : annotations) {
            classes.add(ArClass.forElement(annotation));
         }
         return process(classes);
      } finally {
         roundEnv = null;
      }
   }
   
   /**
    * Processes annotations. This does the work of the processor. The supplied types are the
    * annotation types that this processor {@linkplain #getSupportedAnnotationTypes() supports}
    * and that were encountered during the round of compilation/processing. Implementations can use
    * the {@link #roundEnv round environment} to query for annotated elements.
    * 
    * @param annotationTypes the annotation types to process
    * @return true if the processor claims the annotations, false otherwise
    */
   protected abstract boolean process(Set<ArClass> annotationTypes);

}
