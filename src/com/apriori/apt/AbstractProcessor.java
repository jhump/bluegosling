package com.apriori.apt;

import com.apriori.apt.reflect.Class;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

//TODO: javadoc!
public abstract class AbstractProcessor extends javax.annotation.processing.AbstractProcessor {
   
   protected RoundEnvironment roundEnv;
   
   @Override
   public final void init(ProcessingEnvironment env) {
      super.init(env);
      ProcessingEnvironments.setup(env);
   }
   
   // Synchronized, just in case, since we modify instance state for the duration
   // of this method (roundEnvironment). If another thread for any reason tries to
   // call this method while another is still executing it would corrupt that state
   @Override
   public synchronized final boolean process(Set<? extends TypeElement> annotations,
         javax.annotation.processing.RoundEnvironment env) {
      roundEnv = new RoundEnvironment(env);
      try {
         Set<Class> classes = new HashSet<Class>();
         for (TypeElement annotation : annotations) {
            classes.add(Class.forElement(annotation));
         }
         return process(classes);
      } finally {
         roundEnv = null;
      }
   }
   
   protected abstract boolean process(Set<Class> annotationTypes);
   
   @Override
   public Set<String> getSupportedAnnotationTypes() {
      Set<String> ret = new HashSet<String>(super.getSupportedAnnotationTypes());
      SupportedAnnotationClasses ann = getClass().getAnnotation(SupportedAnnotationClasses.class);
      if (ann != null) {
         for (java.lang.Class<? extends java.lang.annotation.Annotation> annClass : ann.value()) {
            ret.add(annClass.getName());
         }
      }
      return ret;
   }
}
