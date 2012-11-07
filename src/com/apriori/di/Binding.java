package com.apriori.di;

import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import javax.inject.Provider;

/**
 * A set of bindings to be used during code generation. Classes that implement
 * this interface are not intended to be used at application runtime but are only
 * used by the DI tools to generate other artifacts that the application will use.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Binding {
   Set<Key<?>> boundKeys();
   <T> Target<T> targetForKey(Key<T> key);
   
   interface Target<T> {
      TargetKind getKind();
      <R, P> R accept(TargetVisitor<T, R, P> visitor, P p);
      ScopeSpec scope();
      ConflictResolver<? super T> conflictResolver();
   }
   
   interface TargetImplementation<T> extends Target<T> {
      Key<? extends T> key();
      Constructor<T> constructor();
   }

   interface TargetProvider<T, P extends Provider<? extends T>> extends Target<T> {
      Key<P> key();
      Constructor<P> constructor();
   }
   
   interface TargetSelectionProvider<T, P extends SelectionProvider<? extends T>> extends Target<T> {
      Key<P> key();
      Constructor<P> constructor();
   }
   
   interface TargetFactoryMethod<T> extends Target<T> {
      Method method();
   }
   
   interface TargetAdapter<T> extends Target<T> {
      BindAdapter<T, ?> adapter();
   }
   
   enum TargetKind {
      IMPLEMENTATION,
      PROVIDER,
      SELECTION_PROVIDER,
      FACTORY_METHOD,
      ADAPTER
   }
   
   interface TargetVisitor<T, R, P> {
      R visitImplementation(TargetImplementation<T> target, P p);
      R visitProvider(TargetProvider<T, ?> target, P p);
      R visitSelectionProvider(TargetSelectionProvider<T, ?> target, P p);
      R visitFactoryMethod(TargetFactoryMethod<T> target, P p);
      R visitAdapter(TargetAdapter<T> target, P p);
   }

   interface BindAdapter<F, T> {
      T continueBinding(Key<F> key);
      void generateProviderCode(Key<F> key, Writer writer);
      ConflictResolver<? super T> conflictResolver();
   }
   
}