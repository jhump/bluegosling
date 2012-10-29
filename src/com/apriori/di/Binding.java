package com.apriori.di;

import java.io.Writer;
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
      boolean isProvider();
      boolean isSelectionProvider();
      boolean isAdapter();
      Key<? extends T> key();
      Key<? extends Provider<? extends T>> providerKey();
      Key<? extends SelectionProvider<? extends T>> selectionProviderKey();
      BindAdapter<T, ?> adapter();
      ScopeSpec scope();
      @SuppressWarnings("rawtypes") Class<? extends ConflictResolver> conflictResolver();
   }

   interface BindAdapter<F, T> {
      T continueBinding(Key<F> key);
      void generateProviderCode(Key<F> key, Writer writer);
      @SuppressWarnings("rawtypes") Class<? extends ConflictResolver> conflictResolver();
   }
   
}