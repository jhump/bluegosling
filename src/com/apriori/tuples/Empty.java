package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;

//TODO: javadoc
public class Empty extends AbstractTuple implements Serializable {

   private static final long serialVersionUID = -8355558216675899868L;

   private static Object[] EMPTY = new Object[0];
   
   public static Empty INSTANCE = new Empty();
   
   private Empty() {
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
   public <T> Empty transformAll(Function<Object, T> function) {
      return this;
   }

}
