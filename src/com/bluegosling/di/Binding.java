package com.bluegosling.di;

import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
   
   interface TargetImplementation<T, D extends T> extends Target<T> {
      Key<D> key();
      TargetInstanceSource<D> instanceSource();
   }

   interface TargetProvider<T, P extends Provider<? extends T>> extends Target<T> {
      Key<P> key();
      TargetInstanceSource<P> instanceSource();
   }
   
   interface TargetSelectionProvider<T, P extends SelectionProvider<? extends T>> extends Target<T> {
      Key<P> key();
      TargetInstanceSource<P> instanceSource();
   }
   
   interface TargetAdapter<T> extends Target<T> {
      BindAdapter<T, ?> adapter();
   }
   
   enum TargetKind {
      IMPLEMENTATION,
      PROVIDER,
      SELECTION_PROVIDER,
      ADAPTER
   }
   
   interface TargetVisitor<T, R, P> {
      R visitImplementation(TargetImplementation<T, ?> target, P p);
      R visitProvider(TargetProvider<T, ?> target, P p);
      R visitSelectionProvider(TargetSelectionProvider<T, ?> target, P p);
      R visitAdapter(TargetAdapter<T> target, P p);
   }

   interface TargetInstanceSource<T> {
      TargetInstanceSourceKind getKind();
      <R, P> R accept(TargetInstanceSourceVisitor<T, R, P> visitor, P p);
      Set<Key<?>> dependencies();
   }
   
   interface TargetInstanceSourceConstructor<T> extends TargetInstanceSource<T> {
      Constructor<T> constructor();
   }
   
   interface TargetInstanceSourceField<T> extends TargetInstanceSource<T> {
      Key<?> ownerKey();
      Field field();
   }
   
   interface TargetInstanceSourceMethod<T> extends TargetInstanceSource<T> {
      Key<?> ownerKey();
      Method method();
   }
   
   enum TargetInstanceSourceKind {
      CONSTRUCTOR,
      FIELD,
      METHOD
   }
   
   interface TargetInstanceSourceVisitor<T, R, P> {
      R visitConstructor(TargetInstanceSourceConstructor<T> target, P p);
      R visitField(TargetInstanceSourceField<T> target, P p);
      R visitMethod(TargetInstanceSourceMethod<T> target, P p);
   }
   
   interface BindAdapter<F, T> {
      T continueBinding(Key<F> key);
      void generateProviderCode(Key<F> key, Writer writer);
      ConflictResolver<? super T> conflictResolver();
   }
   
}
