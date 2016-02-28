package com.bluegosling.di;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Singleton;

/**
 * The implementation of the {@link Singleton} scope.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SingletonScoper extends AbstractConcurrentScoper {
   
   private final ConcurrentHashMap<Key<?>, Object> scopedInstances =
         new ConcurrentHashMap<Key<?>, Object>();
   
   @Override
   protected ConcurrentMap<Key<?>, Object> scopedInstances() {
      return scopedInstances;
   }
}
