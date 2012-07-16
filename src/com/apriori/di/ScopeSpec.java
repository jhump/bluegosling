package com.apriori.di;

import javax.inject.Scope;

// TODO: javadoc!
public class ScopeSpec {
   private final Scoper scoper;
   private final AnnotationSpec<?> annotation;
   
   private ScopeSpec(Scoper scoper, AnnotationSpec<?> annotation) {
      this.scoper = scoper;
      this.annotation = annotation;
   }
   
   public static ScopeSpec fromScoper(Scoper scoper) {
      return new ScopeSpec(scoper, null);
   }
   
   public static ScopeSpec fromAnnotation(AnnotationSpec<?> annotation) {
      if (!annotation.annotationType().isAnnotationPresent(Scope.class)) {
         throw new IllegalArgumentException("Specified annotation is not a scoping annotation");
      }
      return new ScopeSpec(null, annotation);
   }
   
   public boolean hasScoper() {
      return scoper != null;
   }

   public Scoper getScoper() {
      return scoper;
   }

   public boolean hasAnnotation() {
      return annotation != null;
   }

   public AnnotationSpec<?> getAnnotationSpec() {
      return annotation;
   }
}
