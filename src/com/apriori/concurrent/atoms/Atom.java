package com.apriori.concurrent.atoms;

// TODO: javadoc
public interface Atom<T> {
   interface Watcher<K, T> {
      void changed(K key, Atom<? extends T> atom, T oldValue, T newValue);
   }
   
   T get();
   <K> boolean addWatcher(K key, Watcher<? super K, ? super T> watcher);
   <K> boolean removeWatcher(K key, Watcher<? super K, ? super T> watcher);
}
