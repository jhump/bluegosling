package com.apriori.di;

import com.apriori.reflect.TypeRef;

import java.lang.reflect.Type;

import javax.inject.Provider;

/**
 * An interface for extracting injected instances at runtime. An implementation
 * of this is created during code gen and can be injected into other code if
 * they need a reference to an {@link Injector}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: java doc methods! and provide examples in class doc
public interface Injector {
   <T> T get(Class<? extends T> clazz);
   <T> T get(Key<? extends T> key);
   <T> T get(TypeRef<? extends T> typeRef);
   Object get(Type type);
   
   <T> Provider<T> getProvider(Class<? extends T> clazz);
   <T> Provider<T> getProvider(Key<? extends T> key);
   <T> Provider<T> getProvider(TypeRef<? extends T> typeRef);
   Provider<?> getProvider(Type type);
   
   boolean requiresSelector(Key<?> key);
   boolean acceptsSelector(Key<?> key);

   <T> T getWithSelector(Class<? extends T> clazz, Object selector);
   <T> T getWithSelector(Key<? extends T> key, Object selector);
   <T> T getWithSelector(TypeRef<? extends T> typeRef, Object selector);
   Object getWithSelector(Type type, Object selector);

   <T> SelectionProvider<T> getSelectionProvider(Class<? extends T> clazz);
   <T> SelectionProvider<T> getSelectionProvider(Key<? extends T> key);
   <T> SelectionProvider<T> getSelectionProvider(TypeRef<? extends T> typeRef);
   SelectionProvider<?> getSelectionProvider(Type type);

   <T> Provider<T> getProviderWithSelector(Class<? extends T> clazz, Object selector);
   <T> Provider<T> getProviderWithSelector(Key<? extends T> key, Object selector);
   <T> Provider<T> getProviderWithSelector(TypeRef<? extends T> typeRef, Object selector);
   Provider<?> getProviderWithSelector(Type type, Object selector);
}