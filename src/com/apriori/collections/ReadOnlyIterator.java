package com.apriori.collections;

import java.util.Iterator;

// TODO: javadoc
public abstract class ReadOnlyIterator<E> implements Iterator<E> {
   @Override public final void remove() {
      throw new UnsupportedOperationException();
   }
}
