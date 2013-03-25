package com.apriori.collections;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

// TODO: javadoc
public interface RandomAccessSet<E> extends Set<E> {

   E get(int index);
   
   int indexOf(Object o);
   
   ListIterator<E> listIterator();
   
   ListIterator<E> listIterator(int index);
   
   E remove(int index);
   
   RandomAccessSet<E> subList(int fromIndex, int toIndex);
   
   List<E> asList();
}