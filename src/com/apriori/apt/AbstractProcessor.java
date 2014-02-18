package com.apriori.apt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * An abstract annotation processor that makes for a better base class than the standard. This
 * class provides the following two new features:
 * <ul>
 * <li>This processor sets up thread-local state about the current processing environment. This
 * makes it easier for APIs to work with elements and type mirrors without requiring that callers
 * supply the environment. See {@link ProcessingEnvironments} for more information.</li>
 * <li>The set of supported annotation types can be defined by referencing actual annotation
 * class tokens using a {@literal @}{@link SupportedAnnotationClasses} annotation on the processor.
 * Note that this does not provide any wildcard facilities, but can be used in conjunction with the
 * standard {@literal @}{@link SupportedAnnotationTypes} annotation, which doess.</li>
 * </ul>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractProcessor extends javax.annotation.processing.AbstractProcessor {

   /**
    * {@inheritDoc}
    * 
    * <p>In addition to setting a protected field for access by the processor, this also sets up
    * thread-local state with the current environment.
    *
    * @see ProcessingEnvironments
    */
   @Override
   public final void init(ProcessingEnvironment env) {
      super.init(env);
      ProcessingEnvironments.setup(env);
   }
   
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
      Set<String> ret = new HashSet<String>(super.getSupportedAnnotationTypes());
      SupportedAnnotationClasses ann = getClass().getAnnotation(SupportedAnnotationClasses.class);
      if (ann != null) {
         for (java.lang.Class<? extends java.lang.annotation.Annotation> annClass : ann.value()) {
            ret.add(annClass.getName());
         }
      }
      return Collections.unmodifiableSet(ret);
   }
}
