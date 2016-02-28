package com.bluegosling.di;

import java.util.Map;

import javax.inject.Provider;

//TODO: javadoc!
public abstract class AbstractScoper implements Scoper {

   protected static final Object NULL_SENTINEL = new Object();
   
   protected abstract Map<Key<?>, Object> scopedInstances();
   
   @Override
   @SuppressWarnings("unchecked")
   public <T> T scopedInstance(Key<T> key, Provider<T> unscopedProvider) {
      Map<Key<?>, Object> map = scopedInstances();
      Object obj = map.get(key);
      if (obj == null) {
         obj = unscopedProvider.get();
         map.put(key, obj == null ? NULL_SENTINEL : obj);
         return (T) obj;
      }
      return (T) (obj == NULL_SENTINEL ? null : obj);
   }

}
