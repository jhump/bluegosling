package com.apriori.collections;

import java.util.List;

// TODO: javadoc
public interface ConcurrentList<E> extends List<E> {
   boolean replace(int index, E expectedValue, E newValue);
   boolean remove(int index, E expectedValue);
   boolean addAfter(int index, E expectedPriorValue, E addition);
   boolean addBefore(int index, E expectedNextValue, E addition);
   
   @Override ConcurrentList<E> subList(int fromIndex, int toIndex);
}
