package com.apriori.concurrent.atoms;

import com.apriori.util.Function;

// TODO: javadoc
public interface SynchronousAtom<T> extends Atom<T> {

   T set(T value);
   
   T apply(Function<? super T, ? extends T> function);
   
}
