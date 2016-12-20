package com.bluegosling.apt;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * An abstract annotation processor that may make for a better base class than the standard. This
 * class provides the following two new features:
 * <ul>
 * <li>This processor sets up thread-local state about the current processing environment. This
 * makes it easier for APIs to work with elements and type mirrors without requiring that callers
 * supply the environment. See {@link ProcessingEnvironments} for more information.</li>
 * <li>The set of supported annotation types can be defined by referencing actual annotation
 * class tokens using a {@literal @}{@link SupportedAnnotationClasses} annotation on the processor.
 * Note that this does not provide any wildcard facilities, but can be used in conjunction with the
 * standard {@literal @}{@link SupportedAnnotationTypes} annotation, which does.</li>
 * </ul>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractProcessor extends javax.annotation.processing.AbstractProcessor {
   /**
    * This method sets up thread-local state for the current environment and then delegates to
    * {@link #doProcess(Set, RoundEnvironment)}. On return, the thread-local state is reset.
    *
    * @see ProcessingEnvironments
    */
   @Override
   public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      ProcessingEnvironments.setup(processingEnv);
      try {
         return doProcess(annotations, roundEnv);
      } finally {
         ProcessingEnvironments.reset();
      }
   }
   
   /**
    * Processes the given annotations for the given round. This perform the work of the processor.
    * 
    * @param annotations annotations that the processor supports
    * @param roundEnv information about this round of processing
    * @return true if the processor claims the annotations, false otherwise
    * @see javax.annotation.processing.AbstractProcessor#process(Set, RoundEnvironment)
    */
   protected abstract boolean doProcess(Set<? extends TypeElement> annotations,
         RoundEnvironment roundEnv);

   /**
    * Returns the set of annotations supported by this processor. If the processor class is
    * annotated with {@link SupportedAnnotationTypes} or {@link SupportedAnnotationClasses} then
    * the union of all such referenced annotation types is returned. If the class has no such
    * annotations, an empty set is returned.
    *
    * @see SupportedAnnotationClasses
    */
   @Override
   public Set<String> getSupportedAnnotationTypes() {
      SupportedAnnotationTypes ann1 = getClass().getAnnotation(SupportedAnnotationTypes.class);
      SupportedAnnotationClasses ann2 = getClass().getAnnotation(SupportedAnnotationClasses.class);
      if (ann1 == null && ann2 == null && isInitialized()) {
         processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
               "No SupportedAnnotationTypes or SupportedAnnotationClasses annotation" +
                     "found on " + this.getClass().getName() + ", returning an empty set.");
      }
      int sz = (ann1 == null ? 0 : ann1.value().length)
            + (ann2 == null ? 0 : ann2.value().length);
      if (sz == 0) {
         return Collections.emptySet();
      }
      Set<String> ret = new LinkedHashSet<>(sz * 4 / 3);
      if (ann1 != null) {
         ret.addAll(Arrays.asList(ann1.value()));
      }
      if (ann2 != null) {
         for (Class<? extends Annotation> annClass : ann2.value()) {
            ret.add(annClass.getName());
         }
      }
      return Collections.unmodifiableSet(ret);
   }
}
