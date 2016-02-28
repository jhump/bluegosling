package com.bluegosling.di;

import javax.inject.Provider;

/**
 * An object that scopes injected instances. Generally, something like a
 * {@code ConcurrentHashMap} will be used to store the instances. The life cycle
 * of the scope and duration of instances stored is up to the scope implementor.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Scoper {
   <T> T scopedInstance(Key<T> key, Provider<T> unscopedProvider);
}
