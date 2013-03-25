package com.apriori.di;

import com.apriori.reflect.TypeRef;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
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
// TODO: equals and hashcode
// TODO: javadoc!
public class Key<T> {

   private final TypeRef<T> typeRef;
   private final Object selector;
   private Map<Class<? extends Annotation>, AnnotationSpec<?>> annotations;
   
   private Key(TypeRef<T> typeRef) {
      this(typeRef, null);
   }
   
   private Key(TypeRef<T> typeRef, Object selector) {
      this(typeRef, selector, Collections.<Class<? extends Annotation>,
            AnnotationSpec<?>> emptyMap());
   }
   
   private Key(TypeRef<T> typeRef, Object selector,
         Map<Class<? extends Annotation>, AnnotationSpec<?>> annotations) {
      if (typeRef == null) {
         throw new NullPointerException();
      }
      this.typeRef = typeRef;
      this.selector = selector;
      this.annotations = annotations;
   }
   
   public static <T> Key<T> of(Class<T> clazz) {
      return of(TypeRef.forClass(clazz));
   }
   
   public static <T> Key<T> of(TypeRef<T> typeRef) {
      return new Key<T>(typeRef);
   }
   
   public static Key<?> of(Type type) {
      return of(TypeRef.forType(type));
   }
   
   public Key<T> annotatedWith(Class<? extends Annotation> annotationClass) {
      Map<Class<? extends Annotation>, AnnotationSpec<?>> newAnnotations =
            new HashMap<Class<? extends Annotation>, AnnotationSpec<?>>(annotations);
      newAnnotations.put(annotationClass, AnnotationSpec.fromAnnotationType(annotationClass));
      return new Key<T>(typeRef, selector, newAnnotations);
   }
   
   public Key<T> annotatedWith(Class<? extends Annotation> annotationClass,
         Map<String, Object> annotationAttributes) {
      Map<Class<? extends Annotation>, AnnotationSpec<?>> newAnnotations =
            new HashMap<Class<? extends Annotation>, AnnotationSpec<?>>(annotations);
      newAnnotations.put(annotationClass,
            AnnotationSpec.fromAnnotationValues(annotationClass, annotationAttributes));
      return new Key<T>(typeRef, selector, newAnnotations);
   }
   
   public Key<T> annotatedWith(Annotation annotation) {
      Map<Class<? extends Annotation>, AnnotationSpec<?>> newAnnotations =
            new HashMap<Class<? extends Annotation>, AnnotationSpec<?>>(annotations);
      newAnnotations.put(annotation.annotationType(), AnnotationSpec.fromAnnotation(annotation));
      return new Key<T>(typeRef, selector, newAnnotations);
   }
   
   public Key<T> withoutAnnotations() {
      return new Key<T>(typeRef, selector);
   }
   
   public Key<T> withoutAnnotation(Class<? extends Annotation> annotationClass) {
      Map<Class<? extends Annotation>, AnnotationSpec<?>> newAnnotations =
            new HashMap<Class<? extends Annotation>, AnnotationSpec<?>>(annotations);
      newAnnotations.remove(annotationClass);
      return new Key<T>(typeRef, selector, newAnnotations);
   }
   
   public Key<T> withoutAnnotation(Annotation annotation) {
      return withoutAnnotation(annotation.annotationType());
   }
   
   public Key<T> withoutAnyAnnotationAttributes() {
      Map<Class<? extends Annotation>, AnnotationSpec<?>> newAnnotations =
            new HashMap<Class<? extends Annotation>, AnnotationSpec<?>>(annotations);
      for (Map.Entry<Class<? extends Annotation>, AnnotationSpec<?>> entry
            : newAnnotations.entrySet()) {
         entry.setValue(entry.getValue().withoutAttributes());
      }
      return new Key<T>(typeRef, selector, newAnnotations);
   }
   
   public Key<T> withoutAnnotationAttributes(Class<? extends Annotation> annotationClass) {
      AnnotationSpec<?> spec = annotations.get(annotationClass);
      if (spec == null) {
         return this;
      }
      Map<Class<? extends Annotation>, AnnotationSpec<?>> newAnnotations =
            new HashMap<Class<? extends Annotation>, AnnotationSpec<?>>(annotations);
      newAnnotations.put(annotationClass, spec.withoutAttributes());
      return new Key<T>(typeRef, selector, newAnnotations);
   }
   
   public Key<T> withoutAnnotationAttributes(Annotation annotation) {
      return withoutAnnotationAttributes(annotation.annotationType());
   }
   
   public Key<T> forSelector(Object newSelector) {
      return new Key<T>(typeRef, newSelector, annotations);
   }
   
   public Type getType() {
      return typeRef.asType();
   }
   
   public Class<T> getRawType() {
      return typeRef.asClass();
   }
   
   public Object getSelector() {
      return selector;
   }
   
   public AnnotationSpec<?> getAnnotation(Class<? extends Annotation> annotationClass) {
      return annotations.get(annotationClass);
   }
   
   public boolean hasAnnotations() {
      return !annotations.isEmpty();
   }
   
   public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
      return annotations.containsKey(annotationClass);
   }
   
   public boolean hasAnnotation(Annotation annotation) {
      return hasAnnotation(annotation.annotationType());
   }
   
   public boolean hasAnyAnnotationAttributes() {
      for (AnnotationSpec<?> spec : annotations.values()) {
         if (spec.hasAttributes()) {
            return true;
         }
      }
      return false;
   }
   
   public boolean hasAnnotationAttributes(Class<? extends Annotation> annotationClass) {
      AnnotationSpec<?> spec = annotations.get(annotationClass);
      return spec != null && spec.hasAttributes();
   }
   
   public boolean hasAnnotationAttributes(Annotation annotation) {
      return hasAnnotationAttributes(annotation.annotationType());
   }
   
   public boolean hasSelector() {
      return selector != null;
   }
}