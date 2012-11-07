package com.apriori.di;

import com.apriori.reflect.TypeRef;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * An abstract base class for bindings. Sub-classes need only implement
 * {@link #defineBindings()} and therein use the various {@link #bind}
 * methods to define bindings. The handling of the {@link Binding} interface
 * methods is automatically handled by this class.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: implement me!
// TODO: finish javadoc!
@UsingScope(scope = Singleton.class, impl = SingletonScoper.class)
public abstract class AbstractBinding implements Binding {
   private boolean bindingsBuilt;
   private final Map<Key<?>, Target<?>> bindings = new HashMap<Key<?>, Target<?>>();
   private final Set<BindBuilderImpl<?>> unfinishedBuilders = new LinkedHashSet<BindBuilderImpl<?>>();

   @Override
   public final Set<Key<?>> boundKeys() {
      maybeDefineBindings();
      return Collections.unmodifiableSet(bindings.keySet());
   }
   
   @Override
   public final <T> Target<T> targetForKey(Key<T> key) {
      maybeDefineBindings();
      @SuppressWarnings("unchecked")
      Target<T> ret = (Target<T>) bindings.get(key);
      return ret;
   }
   
   private void maybeDefineBindings() {
      if (!bindingsBuilt) {
         defineBindings();
         if (!unfinishedBuilders.isEmpty()) {
            // TODO: bind these keys, unscoped (to themselves if no target)
         }
         resolveBindingChains();
         bindingsBuilt = true;
      }
   }
   
   private void resolveBindingChains() {
      //TODO
   }
   
   /**
    * Configures the bindings defined in this object.
    */
   protected abstract void defineBindings();
   
   protected <T> BindBuilder<T> bind(Key<T> key) {
      return null;
   }
   
   protected <T> BindBuilder<T> bind(Class<T> clazz) {
      return null;
   }
   
   protected <T> BindBuilder<T> bind(TypeRef<T> typeRef) {
      return null;
   }
   
   protected BindBuilder<?> bind(Type type) {
      return null;
   }
   
   public interface ScopeBuilder<T> {
      void unscoped();
      void singleton();
      void inScope(Class<? extends Annotation> annotationClass);
      void inScope(Class<? extends Annotation> annotationClass,
            Map<String, Object> annotationValues);
      void inScope(Annotation annotation);
      void inScope(Scoper scope);
   }

   /**
    * A builder for bindings. This is used to create bindings with APIs that
    * are easier to read and write than always constructing raw {@link Key}s.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @param <T> the type of value being bound
    */
   public interface BindBuilder<T> extends ScopeBuilder<T> {
      BindBuilder<T> annotatedWith(Class<? extends Annotation> annotationClass);
      BindBuilder<T> annotatedWith(Class<? extends Annotation> annotationClass,
            Map<String, Object> annotationValues);
      BindBuilder<T> annotatedWith(Annotation annotation);
      BindBuilder<T> forSelector(Object selector);
      BindBuilder<T> withResolver(Class<? extends ConflictResolver<? super T>> resolver);
      <A> A withAdapter(BindAdapter<T, A> adapter);
      BindBuilder<T> catchAll();
      ScopeBuilder<T> to(Key<? extends T> key);
      ScopeBuilder<T> to(Class<? extends T> clazz);
      ScopeBuilder<T> to(TypeRef<? extends T> typeRef);
      ScopeBuilder<T> to(Type type); // need to do runtime check on underlying type for this!
      ScopeBuilder<T> toProvider(Key<? extends Provider<? extends T>> key);
      ScopeBuilder<T> toProvider(Class<? extends Provider<? extends T>> clazz);
      ScopeBuilder<T> toProvider(TypeRef<? extends Provider<? extends T>> typeRef);
      ScopeBuilder<T> toSelectionProvider(Key<? extends SelectionProvider<? extends T>> key);
      ScopeBuilder<T> toSelectionProvider(Class<? extends SelectionProvider<? extends T>> clazz);
      ScopeBuilder<T> toSelectionProvider(TypeRef<? extends SelectionProvider<? extends T>> typeRef);
   }
   
   private class BindBuilderImpl<T> implements BindBuilder<T> {

      private Key<T> key;
      private boolean catchAll;
      private Class<? extends ConflictResolver<? super T>> resolver;
      private BindAdapter<T, ?> adapter;
      private Key<? extends T> target;
      private Key<? extends Provider<? extends T>> targetProvider;
      private Key<? extends SelectionProvider<? extends T>> targetSelectionProvider;
      private ScopeSpec scope;
      
      BindBuilderImpl(Key<T> key) {
         this.key = key;
      }
      
      void finish() {
         // TODO
      }
      
      @Override
      public void unscoped() {
         finish();
      }

      @Override
      public void singleton() {
         scope = ScopeSpec.fromAnnotation(AnnotationSpec.fromAnnotationType(Singleton.class));
         finish();
      }

      @Override
      public void inScope(Class<? extends Annotation> annotationClass) {
         scope = ScopeSpec.fromAnnotation(AnnotationSpec.fromAnnotationType(annotationClass));
         finish();
      }

      @Override
      public void inScope(Class<? extends Annotation> annotationClass,
            Map<String, Object> annotationValues) {
         scope = ScopeSpec.fromAnnotation(AnnotationSpec.fromAnnotationValues(annotationClass,
               annotationValues));
         finish();
      }

      @Override
      public void inScope(Annotation annotation) {
         scope = ScopeSpec.fromAnnotation(AnnotationSpec.fromAnnotation(annotation));
         finish();
         
      }

      @Override
      public void inScope(Scoper scope) {
         this.scope = ScopeSpec.fromScoper(scope);
         finish();
      }

      @Override
      public BindBuilder<T> annotatedWith(Class<? extends Annotation> annotationClass) {
         key = key.annotatedWith(annotationClass);
         return this;
      }

      @Override
      public BindBuilder<T> annotatedWith(Class<? extends Annotation> annotationClass,
            Map<String, Object> annotationValues) {
         key = key.annotatedWith(annotationClass, annotationValues);
         return this;
      }

      @Override
      public BindBuilder<T> annotatedWith(Annotation annotation) {
         key = key.annotatedWith(annotation);
         return this;
      }

      @Override
      public BindBuilder<T> forSelector(Object selector) {
         key = key.forSelector(selector);
         return this;
      }

      @Override
      public BindBuilder<T> catchAll() {
         catchAll = true;
         return this;
      }

      @Override
      public BindBuilder<T> withResolver(Class<? extends ConflictResolver<? super T>> resolver) {
         this.resolver = resolver;
         return this;
      }

      @Override
      public <A> A withAdapter(BindAdapter<T, A> adapter) {
         this.adapter = adapter;
         return adapter.continueBinding(key);
      }

      @Override
      public ScopeBuilder<T> to(Key<? extends T> key) {
         target = key;
         return this;
      }

      @Override
      public ScopeBuilder<T> to(Class<? extends T> clazz) {
         target = Key.of(clazz);
         return this;
      }

      @Override
      public ScopeBuilder<T> to(TypeRef<? extends T> typeRef) {
         target = Key.of(typeRef);
         return this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public ScopeBuilder<T> to(Type type) {
         // TODO validate that type is actually a match or is compatible
         target = (Key<T>) Key.of(type);
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Key<? extends Provider<? extends T>> key) {
         targetProvider = key;
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Class<? extends Provider<? extends T>> clazz) {
         targetProvider = Key.of(clazz);
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(TypeRef<? extends Provider<? extends T>> typeRef) {
         targetProvider = Key.of(typeRef);
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Key<? extends SelectionProvider<? extends T>> key) {
         targetSelectionProvider = key;
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(
            Class<? extends SelectionProvider<? extends T>> clazz) {
         targetSelectionProvider = Key.of(clazz);
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(
            TypeRef<? extends SelectionProvider<? extends T>> typeRef) {
         targetSelectionProvider = Key.of(typeRef);
         return this;
      }
   }
}