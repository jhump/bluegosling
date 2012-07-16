package com.apriori.di;

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
      Key<? extends T> key();
      Key<? extends Provider<? extends T>> providerKey();
      Key<? extends SelectionProvider<? extends T>> selectionProviderKey();
      ScopeSpec scope();
      
      @SuppressWarnings("rawtypes")
      Class<? extends ConflictResolver> conflictResolver();
   }
}