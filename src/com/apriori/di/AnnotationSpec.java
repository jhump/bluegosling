package com.apriori.di;

import com.apriori.reflect.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Predicate;

//TODO: javadoc!
public class AnnotationSpec<T extends Annotation> {
   
   private static final Predicate<Method> EXCLUDE_NONBINDING_ATTRIBUTES =
         (m) -> !m.isAnnotationPresent(NonBinding.class);
         
   private final Class<T> annotationType;
   private final T annotation;
   
   private AnnotationSpec(Class<T> annotationType, T annotation) {
      if (annotationType == null) {
         throw new NullPointerException();
      }
      this.annotationType = annotationType;
      this.annotation = annotation;
   }
   
   public static <T extends Annotation> AnnotationSpec<T> fromAnnotationType(Class<T> annotationType) {
      return new AnnotationSpec<T>(annotationType, null);
   }

   public static <T extends Annotation> AnnotationSpec<T> fromAnnotationValues(Class<T> annotationType,
         Map<String, Object> attributes) {
      return fromAnnotation(Annotations.create(annotationType, attributes));
   }
   
   public static <T extends Annotation> AnnotationSpec<T> fromAnnotation(T annotation) {
      @SuppressWarnings("unchecked")
      Class<T> clazz = (Class<T>) annotation.annotationType();
      return new AnnotationSpec<T>(clazz, annotation);
   }

   public Class<T> annotationType() {
      return annotationType;
   }
   
   public boolean hasAttributes() {
      return annotation != null;
   }
   
   public Map<String, Object> attributes() {
      return annotation == null ? null
            : Annotations.toMap(annotation, EXCLUDE_NONBINDING_ATTRIBUTES);
   }
   
   public T asAnnotation() {
      return annotation;
   }
   public AnnotationSpec<T> withoutAttributes() {
      return new AnnotationSpec<T>(annotationType, null);
   }
   
   @Override public boolean equals(Object o) {
      if (o instanceof AnnotationSpec) {
         AnnotationSpec<?> other = (AnnotationSpec<?>) o;
         if ((annotation == null || other.annotation == null) && annotation != other.annotation) {
            return false;
         }
         if (annotation == null) {
            return Annotations.equal(annotation, other.annotation, EXCLUDE_NONBINDING_ATTRIBUTES);
         } else {
            return annotationType.equals(other.annotationType);
         }
      }
      return false;
   }
   
   @Override public int hashCode() {
      if (annotation != null) {
         return Annotations.hashCode(annotation, EXCLUDE_NONBINDING_ATTRIBUTES);
      } else {
         return annotationType.hashCode();
      }
   }
   
   @Override public String toString() {
      if (annotation != null) {
         return Annotations.toString(annotation, EXCLUDE_NONBINDING_ATTRIBUTES);
      } else {
         return "@" + annotationType.getName();
      }
   }
}
