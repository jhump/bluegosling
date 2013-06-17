package com.apriori.tuples;

import java.util.Collection;

/**
 * Utility methods for creating {@link Tuple} instances.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class Tuples {
   /** Prevents instantiation. */
   private Tuples() {
   }

   /**
    * Creates a new tuple with the same items as the specified collection. The first item in the
    * tuple will be the first item returned from the collection's iterator, and so on.
    * 
    * @param coll the collection
    * @return a tuple with the same items as the collection
    * 
    * @see #fromArray(Object...)
    */
   public static Tuple fromCollection(Collection<?> coll) {
      return fromArray(coll.toArray());
   }
   
   /**
    * Creates a new tuple with the same items as the specified array. The first item in the tuple
    * will be the first item in the array, and so on.
    * 
    * If the specified array is empty, {@link Empty#INSTANCE} will be returned. If the array has
    * exactly one, two, three, four, or five elements then a {@link Unit}, {@link Pair},
    * {@link Trio}, {@link Quartet}, or {@link Quintet} will be returned, respectively. If the
    * array has more than five elements than an {@link NTuple} is returned.
    * 
    * @param array the array
    * @return a tuple with the same items as the array
    */
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
         default:
            return NTuple.create(array[0], array[1], array[2], array[3], array[4], array[5],
                  getRemaining(array, 6));
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
