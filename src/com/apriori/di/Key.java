package com.apriori.di;

import com.apriori.reflect.TypeRef;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * A key for binding injected types to their implementations. The key can be
 * used for both halves of the binding -- the injected type and the definition
 * of the binding target.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type represented by the key
 */
// TODO: implement me!
// TODO: javadoc!
public class Key<T> {
   
   public static <T> Key<T> of(Class<T> clazz) {
      return null;
   }
   public static <T> Key<T> of(TypeRef<T> typeRef) {
      return null;
   }
   public static Key<?> of(Type type) {
      return null;
   }
   public Key<T> annotatedWith(Class<? extends Annotation> annotationClass) {
      return null;
   }
   public Key<T> annotatedWith(Class<? extends Annotation> annotationClass,
         Map<String, Object> annotationAttributes) {
      return null;
   }
   public Key<T> annotatedWith(Annotation annotation) {
      return null;
   }
   public Key<T> withoutAnnotations() {
      return null;
   }
   public Key<T> withoutAnnotation(Class<? extends Annotation> annotationClass) {
      return null;
   }
   public Key<T> withoutAnnotation(Annotation annotation) {
      return null;
   }
   public Key<T> withoutAnyAnnotationAttributes() {
      return null;
   }
   public Key<T> withoutAnnotationAttributes(Class<? extends Annotation> annotationClass) {
      return null;
   }
   public Key<T> withoutAnnotationAttributes(Annotation annotation) {
      return null;
   }
   public Key<T> forSelector(Object selector) {
      return null;
   }
   
   public Type getType() {
      return null;
   }
   public Class<T> getRawType() {
      return null;
   }
   public Object getSelector() {
      return null;
   }
   public Annotation getAnnotation() {
      return null;
   }
   public boolean hasAnnotations() {
      return false;
   }
   public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
      return false;
   }
   public boolean hasAnnotation(Annotation annotation) {
      return false;
   }
   public boolean hasAnyAnnotationAttributes() {
      return false;
   }
   public boolean hasAnnotationAttributes(Class<? extends Annotation> annotationClass) {
      return false;
   }
   public boolean hasAnnotationAttributes(Annotation annotation) {
      return false;
   }
   public boolean hasSelector() {
      return false;
   }
}