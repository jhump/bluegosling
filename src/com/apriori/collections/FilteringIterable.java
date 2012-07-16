package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

//TODO: javadoc
public class FilteringIterable<E> implements Iterable<E> {

   private final Iterable<E> iterable;
   private final Predicate<E> predicate;
   
   public FilteringIterable(Iterable<E> iterable, Predicate<E> predicate) {
      this.iterable = iterable;
      this.predicate = predicate;
   }
   
   public Collection<E> capture() {
      ArrayList<E> copy = new ArrayList<E>();
      for (E e : this) {
         copy.add(e);
      }
      return Collections.unmodifiableCollection(copy);
   }
   
   protected Iterable<E> internal() {
      return iterable;
   }
   
   protected Predicate<E> predicate() {
      return predicate;
   }
   
   @Override
   public Iterator<E> iterator() {
      return new FilteringIterator<E>(internal().iterator(), predicate());
   }

}
