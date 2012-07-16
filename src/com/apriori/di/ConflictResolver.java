package com.apriori.di;

import java.util.Set;

//TODO: javadoc!
public interface ConflictResolver<T> {
   <S extends T> Binding.Target<S> resolve(Key<S> key, Set<Binding.Target<S>> bindings);
}
