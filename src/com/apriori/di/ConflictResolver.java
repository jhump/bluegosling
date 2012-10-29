package com.apriori.di;

import com.apriori.di.Binding.Target;

import java.util.Iterator;
import java.util.Set;

//TODO: javadoc!
public interface ConflictResolver<T> {
   <S extends T> Binding.Target<S> resolve(Key<S> key, Set<Binding.Target<S>> bindings);
   
   enum ResolutionStrategy implements ConflictResolver<Object> {
      REJECT() {
         @Override
         public <S> Target<S> resolve(Key<S> key, Set<Target<S>> bindings) {
            // TODO throw some exception to indicate more than one binding
            return null;
         }
      },
      KEEP_FIRST() {
         @Override
         public <S> Target<S> resolve(Key<S> key, Set<Target<S>> bindings) {
            return bindings.iterator().next();
         }
      },
      KEEP_LAST() {
         @Override
         public <S> Target<S> resolve(Key<S> key, Set<Target<S>> bindings) {
            Target<S> last = null;
            for (Iterator<Target<S>> iter = bindings.iterator(); iter.hasNext(); last = iter.next());
            return last;
         }
      }
   }
}
