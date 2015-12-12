package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

class AnnotatedCapturedType implements AnnotatedTypeVariable {
   static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
   
   private final AnnotatedWildcardType wildcard;
   private final TypeVariable<?> typeVariable;
   
   AnnotatedCapturedType(AnnotatedWildcardType wildcard) {
      this.wildcard = wildcard;
      this.typeVariable = new CapturedTypeVariable(wildcard);
   }
   
   AnnotatedWildcardType capturedWildcard() {
      return wildcard;
   }

   @Override
   public Type getType() {
      return typeVariable;
   }

   @Override
   public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
      return wildcard.getAnnotation(annotationClass);
   }

   @Override
   public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
      return wildcard.isAnnotationPresent(annotationClass);
   }

   @Override
   public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
      return wildcard.getAnnotationsByType(annotationClass);
   }

   @Override
   public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
      return wildcard.getDeclaredAnnotation(annotationClass);
   }

   @Override
   public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
      return wildcard.getDeclaredAnnotationsByType(annotationClass);
   }

   @Override
   public Annotation[] getAnnotations() {
      return wildcard.getAnnotations();
   }

   @Override
   public Annotation[] getDeclaredAnnotations() {
      return wildcard.getDeclaredAnnotations();
   }

   @Override
   public AnnotatedType[] getAnnotatedBounds() {
      return wildcard.getAnnotatedUpperBounds();
   }
   
   @Override
   public int hashCode() {
      return AnnotatedTypes.hashCode(this);
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof AnnotatedType && AnnotatedTypes.equals(this, (AnnotatedType) obj);
   }

   @Override
   public String toString() {
      return AnnotatedTypes.toString(this);
   }
   
   static class CaptureGenericDeclaration implements GenericDeclaration {
      private final TypeVariable<CaptureGenericDeclaration> capture;
      
      public CaptureGenericDeclaration(TypeVariable<CaptureGenericDeclaration> capture) {
         this.capture = capture;
      }

      @Override
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
         return null;
      }

      @Override
      public Annotation[] getAnnotations() {
         return EMPTY_ANNOTATIONS;
      }

      @Override
      public Annotation[] getDeclaredAnnotations() {
         return EMPTY_ANNOTATIONS;
      }

      @Override
      public TypeVariable<?>[] getTypeParameters() {
         return new TypeVariable<?>[] { capture };
      }
   }
   
   static class CapturedTypeVariable implements TypeVariable<CaptureGenericDeclaration> {
      private final AnnotatedWildcardType annotatedWildcard;
      private final WildcardType wildcard;
      private final CaptureGenericDeclaration declaration;
      
      CapturedTypeVariable(AnnotatedWildcardType wildcard) {
         this.annotatedWildcard = wildcard;
         this.wildcard = (WildcardType) wildcard.getType();
         this.declaration = new CaptureGenericDeclaration(this);
      }

      AnnotatedWildcardType capturedWildcard() {
         return annotatedWildcard;
      }

      @Override
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
         return null;
      }

      @Override
      public Annotation[] getAnnotations() {
         return EMPTY_ANNOTATIONS;
      }

      @Override
      public Annotation[] getDeclaredAnnotations() {
         return EMPTY_ANNOTATIONS;
      }

      @Override
      public Type[] getBounds() {
         return wildcard.getUpperBounds();
      }

      @Override
      public CaptureGenericDeclaration getGenericDeclaration() {
         return declaration;
      }

      @Override
      public String getName() {
         return "<captured wildcard>";
      }

      @Override
      public AnnotatedType[] getAnnotatedBounds() {
         return annotatedWildcard.getAnnotatedUpperBounds();
      }
   }
}