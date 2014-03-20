package com.apriori.tuples;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A tuple that has no elements. Since tuples are immutable, there is no need for more than one
 * instance of this class, so all instances are just references to the constant {@link #INSTANCE}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class Empty extends AbstractTuple implements Serializable {

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
   public <T> Unit<T> add(T t) {
      return Unit.create(t);
   }

   @Override
   public <T> Unit<T> insertFirst(T t) {
      return add(t);
   }

   @Override
   public <T> Empty transformAll(Function<Object, ? extends T> function) {
      return this;
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
