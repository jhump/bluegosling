package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//TODO: javadoc
public class FilteringSet<E> extends FilteringCollection<E> implements Set<E> {

   public FilteringSet(Set<E> set, Predicate<E> predicate) {
      super(set, predicate);
   }

   @Override
   public Set<E> capture() {
      return Collections.unmodifiableSet(new HashSet<E>(this));
   }
}
