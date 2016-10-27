package com.bluegosling.tuples;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.bluegosling.util.ValueType;

/**
 * A tuple that has no elements. Since tuples are immutable, there is no need for more than one
 * instance of this class, so all instances are just references to the constant {@link #INSTANCE}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@ValueType
public final class Empty implements Tuple, Serializable {

   private static final long serialVersionUID = -8355558216675899868L;

   private static Object[] EMPTY = new Object[0];
   
   /**
    * The singleton instance of this class.
    */
   public static Empty INSTANCE = new Empty();
   
   private Empty() {
   }
   
   @Override
   public boolean contains(Object o) {
      return false;
   }
   
   @Override
   public int size() {
      return 0;
   }
   
   @Override
   public boolean isEmpty() {
      return true;
   }
   
   @Override
   public Object[] toArray() {
      return EMPTY;
   }

   @Override
   public <T> Single<T> add(T t) {
      return Single.of(t);
   }

   @Override
   public <T> Single<T> insertFirst(T t) {
      return add(t);
   }

   @Override
   public <T> Empty transformAll(Function<Object, ? extends T> function) {
      return this;
   }
   
   @Override
   public Iterator<Object> iterator() {
      return TupleUtils.iterator(this);
   }
   
   @Override
   public <T> T[] toArray(T[] a) {
      return TupleUtils.toArray(this, a);
   }
   
   @Override
   public List<?> asList() {
      return TupleUtils.asList(this);
   }
   
   @Override
   public boolean equals(Object o) {
      return TupleUtils.equals(this, o);
   }

   @Override
   public int hashCode() {
      return TupleUtils.hashCode(this);
   }

   @Override
   public String toString() {
      return TupleUtils.toString(this);
   }

   /**
    * Enforces that there exists only one instance of this class.
    * 
    * @return {@link #INSTANCE}
    */
   private Object readResolve() {
      return INSTANCE; 
  }
}
