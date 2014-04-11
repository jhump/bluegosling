package com.apriori.di;

import com.apriori.reflect.TypeRef;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
// TODO: extra validation about accidentally binding more than once
// TODO: capture stack traces in binding calls for better validation messages
@UsingScope(scope = Singleton.class, impl = SingletonScoper.class)
public abstract class AbstractBinding implements Binding {
   private boolean bindingsBuilt;
   private final Map<Key<?>, Key<?>> unresolvedBindings = new HashMap<Key<?>, Key<?>>();
   private final Map<Key<?>, Target<?>> resolvedBindings = new HashMap<Key<?>, Target<?>>();
   private final Set<BindBuilderImpl<?>> unfinishedBuilders = new LinkedHashSet<BindBuilderImpl<?>>();

   @Override
   public final Set<Key<?>> boundKeys() {
      maybeDefineBindings();
      return Collections.unmodifiableSet(resolvedBindings.keySet());
   }
   
   @Override
   public final <T> Target<T> targetForKey(Key<T> key) {
      maybeDefineBindings();
      @SuppressWarnings("unchecked")
      Target<T> ret = (Target<T>) resolvedBindings.get(key);
      return ret;
   }
   
   private void maybeDefineBindings() {
      if (!bindingsBuilt) {
         defineBindings();
         if (!unfinishedBuilders.isEmpty()) {
            unfinishedBuilders.iterator().next().finish();
         }
         resolveBindingChains();
         bindingsBuilt = true;
      }
   }
   
   private void resolveBindingChains() {
      while (!unresolvedBindings.isEmpty()) {
         resolve(unresolvedBindings.keySet().iterator().next(), new HashSet<Key<?>>());
      }
   }
   
   private Target<?> resolve(Key<?> keyToResolve, Set<Key<?>> keySequence) {
      Key<?> targetKey = unresolvedBindings.remove(keyToResolve);
      Target<?> target;
      if (unresolvedBindings.containsKey(targetKey)) {
         if (!keySequence.add(keyToResolve)) {
            throw new IllegalStateException("circular bindings detected");
         }
         target = resolve(targetKey, keySequence);
         keySequence.remove(keyToResolve);
      } else {
         target = resolvedBindings.get(targetKey);
         if (target == null) {
            // TODO: make target from targetKey
         }
      }
      resolvedBindings.put(keyToResolve, target);
      return target;
   }
   
   /**
    * Configures the bindings defined in this object.
    */
   protected abstract void defineBindings();
   
   protected <T> BindBuilder<T> bind(Key<T> key) {
      if (bindingsBuilt) {
         throw new IllegalStateException("Bindings already built");
      }
      return new BindBuilderImpl<T>(key);
   }
   
   protected <T> BindBuilder<T> bind(Class<T> clazz) {
      return bind(Key.of(clazz));
   }
   
   protected <T> BindBuilder<T> bind(TypeRef<T> typeRef) {
      return bind(Key.of(typeRef));
   }
   
   protected BindBuilder<?> bind(Type type) {
      return bind(Key.of(type));
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
      ScopeBuilder<T> to(Constructor<T> cons);
      ScopeBuilder<T> to(Field field); // need to check type of field
      ScopeBuilder<T> to(Method factoryMethod); // need to check return type of method
      ScopeBuilder<T> to(Key<?> otherKey, Field field); // need to check type of field
      ScopeBuilder<T> to(Key<?> otherKey, Method factoryMethod); // need to check return type of method
      ScopeBuilder<T> toProvider(Key<? extends Provider<? extends T>> key);
      ScopeBuilder<T> toProvider(Class<? extends Provider<? extends T>> clazz);
      ScopeBuilder<T> toProvider(TypeRef<? extends Provider<? extends T>> typeRef);
      ScopeBuilder<T> toProvider(Type type); // need to do runtime check on underlying type for this!
      ScopeBuilder<T> toProvider(Constructor<Provider<? extends T>> cons);
      ScopeBuilder<T> toProvider(Field field); // need to check type of field
      ScopeBuilder<T> toProvider(Method factoryMethod); // need to check return type of method
      ScopeBuilder<T> toProvider(Key<?> otherKey, Field field); // need to check type of field
      ScopeBuilder<T> toProvider(Key<?> otherKey, Method factoryMethod); // need to check return type of method
      ScopeBuilder<T> toSelectionProvider(Key<? extends SelectionProvider<? extends T>> key);
      ScopeBuilder<T> toSelectionProvider(Class<? extends SelectionProvider<? extends T>> clazz);
      ScopeBuilder<T> toSelectionProvider(TypeRef<? extends SelectionProvider<? extends T>> typeRef);
      ScopeBuilder<T> toSelectionProvider(Type type); // need to do runtime check on underlying type for this!
      ScopeBuilder<T> toSelectionProvider(Constructor<SelectionProvider<? extends T>> cons);
      ScopeBuilder<T> toSelectionProvider(Field field); // need to check type of field
      ScopeBuilder<T> toSelectionProvider(Method factoryMethod); // need to check return type of method
      ScopeBuilder<T> toSelectionProvider(Key<?> otherKey, Field field); // need to check type of field
      ScopeBuilder<T> toSelectionProvider(Key<?> otherKey, Method factoryMethod); // need to check return type of method
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
         // TODO - create target
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

      @Override
      public ScopeBuilder<T> to(Constructor<T> cons) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> to(Field field) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> to(Method factoryMethod) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Type type) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Constructor<Provider<? extends T>> cons) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Field field) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Method factoryMethod) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Type type) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Constructor<SelectionProvider<? extends T>> cons) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Field field) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Method factoryMethod) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> to(Key<?> otherKey, Field field) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> to(Key<?> otherKey, Method factoryMethod) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Key<?> otherKey, Field field) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toProvider(Key<?> otherKey, Method factoryMethod) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Key<?> otherKey, Field field) {
         // TODO Auto-generated method stub
         return this;
      }

      @Override
      public ScopeBuilder<T> toSelectionProvider(Key<?> otherKey, Method factoryMethod) {
         // TODO Auto-generated method stub
         return this;
      }
   }
}