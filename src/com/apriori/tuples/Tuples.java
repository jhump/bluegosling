package com.apriori.tuples;

import java.util.Collection;

// TODO: javadoc
public final class Tuples {
   private Tuples() {
   }
   
   public static Tuple fromCollection(Collection<?> coll) {
      return fromArray(coll.toArray());
   }
   
   public static Tuple fromArray(Object... array) {
      switch (array.length) {
         case 0:
            return Empty.INSTANCE;
         case 1:
            return Unit.create(array[0]);
         case 2:
            return Pair.create(array[0], array[1]);
         case 3:
            return Trio.create(array[0], array[1], array[2]);
         case 4:
            return Quartet.create(array[0], array[1], array[2], array[3]);
         case 5:
            return Quintet.create(array[0], array[1], array[2], array[3], array[4]);
         case 6:
            return NTuple.create(array[0], array[1], array[2], array[3], array[4], array[5]);
         default:
            return NTuple.create(array[0], array[1], array[2], array[3], array[4], array[5],
                  array[6], getRemaining(array, 6));
      }
   }

   private static Object[] getRemaining(Object array[], int startingAt) {
      int len = array.length - startingAt;
      Object ret[] = new Object[len];
      if (len > 0) {
         System.arraycopy(array, startingAt, ret, 0, len);
      }
      return ret;
   }
}
