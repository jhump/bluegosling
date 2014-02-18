package com.apriori.apt.reflect;

import com.apriori.apt.AbstractProcessor;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

// TODO: javadoc
public abstract class ArProcessor extends AbstractProcessor {
   
   protected ArRoundEnvironment roundEnv;
   
   @Override
   public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
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
   
   protected abstract boolean process(Set<ArClass> annotationTypes);

}
