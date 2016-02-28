package com.bluegosling.di;

import java.util.concurrent.ConcurrentMap;

import javax.inject.Provider;

//TODO: javadoc!
public abstract class AbstractConcurrentScoper extends AbstractScoper {

   @Override protected abstract ConcurrentMap<Key<?>, Object> scopedInstances();
   
   @Override
   @SuppressWarnings("unchecked")
   public <T> T scopedInstance(Key<T> key, Provider<T> unscopedProvider) {
      ConcurrentMap<Key<?>, Object> map = scopedInstances();
      Object obj = map.get(key);
      if (obj == null) {
         obj = unscopedProvider.get();
         Object priorObj = map.putIfAbsent(key, obj == null ? NULL_SENTINEL : obj);
         // in case there was a race and something else made it into the map first
         if (priorObj == null) {
            return (T) obj;
         }
         obj = priorObj;
      }
      return (T) (obj == NULL_SENTINEL ? null : obj);
   }

}
