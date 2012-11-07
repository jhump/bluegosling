package com.apriori.di;

import com.apriori.reflect.Annotations;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

//TODO: javadoc!
public class AnnotationSpec<T extends Annotation> {
   private final Class<T> annotationType;
   private final Map<String, Object> attributes;
   
   private AnnotationSpec(Class<T> annotationType, Map<String, Object> attributes) {
      this.annotationType = annotationType;
      this.attributes = attributes;
   }
   
   public static <T extends Annotation> AnnotationSpec<T> fromAnnotationType(Class<T> annotationType) {
      return new AnnotationSpec(annotationType, null);
   }

   public static <T extends Annotation> AnnotationSpec<T> fromAnnotationValues(Class<T> annotationType,
         Map<String, Object> attributes) {
      // TODO: validate attributes
      return new AnnotationSpec(annotationType, attributes);
   }
   
   public static <T extends Annotation> AnnotationSpec<T> fromAnnotation(T annotation) {
      // TODO: get attributes!
      Map<String, Object> attributes = null;
      return new AnnotationSpec(annotation.annotationType(), attributes);
   }

   public Class<T> annotationType() {
      return annotationType;
   }
   
   public boolean hasAttributes() {
      return attributes != null;
   }
   
   public Map<String, Object> attributes() {
      return attributes == null ? null : Collections.unmodifiableMap(attributes);
   }
   
   public T asAnnotation() {
      return Annotations.create(annotationType, attributes);
   }
   public AnnotationSpec<T> withoutAttributes() {
      return new AnnotationSpec<T>(annotationType, null);
   }
   
   @Override public boolean equals(Object other) {
     // TODO
      return false;
   }
   
   @Override public int hashCode() {
      // TODO
      return 0;
   }
   
   @Override public String toString() {
      // TODO
      return "";
   }
}
