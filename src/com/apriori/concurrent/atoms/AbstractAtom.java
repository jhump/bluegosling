package com.apriori.concurrent.atoms;

import com.apriori.collections.HamtPersistentSet;
import com.apriori.collections.Immutables;
import com.apriori.collections.PersistentSet;
import com.apriori.tuples.Pair;
import com.apriori.util.Predicate;

import java.util.concurrent.atomic.AtomicReference;

// TODO: javadoc
public abstract class AbstractAtom<T> implements Atom<T> {

   private final AtomicReference<PersistentSet<Pair<Object, Watcher<?, ? super T>>>> watchers =
         new AtomicReference<PersistentSet<Pair<Object, Watcher<?, ? super T>>>>(
               new HamtPersistentSet<Pair<Object, Watcher<?, ? super T>>>());

   private final Predicate<? super T> validator;
   
   AbstractAtom() {
      this.validator = null;
   }
   
   AbstractAtom(Predicate<? super T> validator) {
      this.validator = validator;
   }
   
   protected void validate(T value) {
      if (validator != null && !validator.test(value)) {
         throw new IllegalArgumentException("value " + value + " is not valid for this atom");
      }
   }

   protected void notify(T oldValue, T newValue) {
      for (Pair<?, Watcher<?, ? super T>> watcherEntry : Immutables.asIfMutable(watchers.get())) {
         @SuppressWarnings("unchecked")
         Watcher<Object, T> watcher = (Watcher<Object, T>) watcherEntry.getSecond();
         notify(watcherEntry.getFirst(), watcher, oldValue, newValue);
      }
   }
   
   private <K> void notify(K key, Watcher<? super K, ? super T> watcher, T oldValue, T newValue) {
      try {
         watcher.changed(key, this, oldValue, newValue);
      } catch (Exception e) {
         // TODO: log?
      }
   }
   
   @Override
   public <K> boolean addWatcher(K key, Watcher<? super K, ? super T> watcher) {
      while (true) {
         Pair<Object, Watcher<?, ? super T>> pair =
               Pair.<Object, Watcher<?, ? super T>>create(key, watcher);
         PersistentSet<Pair<Object, Watcher<?, ? super T>>> oldSet = watchers.get();
         if (oldSet.contains(pair)) {
            return false;
         }
         PersistentSet<Pair<Object, Watcher<?, ? super T>>> newSet = oldSet.add(pair);
         if (watchers.compareAndSet(oldSet, newSet)) {
            return true;
         }
      }
   }

   @Override
   public <K> boolean removeWatcher(K key, Watcher<? super K, ? super T> watcher) {
      while (true) {
         Pair<Object, Watcher<?, ? super T>> pair =
               Pair.<Object, Watcher<?, ? super T>>create(key, watcher);
         PersistentSet<Pair<Object, Watcher<?, ? super T>>> oldSet = watchers.get();
         if (!oldSet.contains(pair)) {
            return false;
         }
         PersistentSet<Pair<Object, Watcher<?, ? super T>>> newSet = oldSet.remove(pair);
         if (watchers.compareAndSet(oldSet, newSet)) {
            return true;
         }
      }
   }

}
