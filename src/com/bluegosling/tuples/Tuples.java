package com.bluegosling.tuples;

import java.util.Collection;
import java.util.Comparator;

/**
 * Utility methods for creating and using {@link Tuple} instances.
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
    * exactly one, two, three, four, or five elements then a {@link Single}, {@link Pair},
    * {@link Triple}, {@link Quadruple}, or {@link Quintuple} will be returned, respectively. If the
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
            return Single.of(array[0]);
         case 2:
            return Pair.of(array[0], array[1]);
         case 3:
            return Triple.of(array[0], array[1], array[2]);
         case 4:
            return Quadruple.of(array[0], array[1], array[2], array[3]);
         case 5:
            return Quintuple.of(array[0], array[1], array[2], array[3], array[4]);
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
   
   // TODO: javadoc & tests
   
   public Comparator<Tuple> tupleNaturalOrdering() {
      return new Comparator<Tuple>() {
         @Override public int compare(Tuple o1, Tuple o2) {
            int c = 0;
            Object o1Array[] = o1.toArray();
            Object o2Array[] = o2.toArray();
            for (int i = 0, len = Math.max(o1.size(), o2.size()); c == 0 && i < len; i++) {
               if (i >= o1.size()) {
                  return -1;
               } else if (i >= o2.size()) {
                  return 1;
               }
               @SuppressWarnings("unchecked")
               Comparable<Object> o1comparable = (Comparable<Object>) o1Array[i];
               c = o1comparable.compareTo(o2Array[i]);
            }
            return c;
         }
      };
   }
   
   public <A extends Comparable<A>, B extends Comparable<B>>
   Comparator<Pair<A, B>> pairNaturalOrdering() {
      return new Comparator<Pair<A, B>>() {
         @Override public int compare(Pair<A, B> o1, Pair<A, B> o2) {
            int c = o1.getFirst().compareTo(o2.getFirst());
            return c != 0 ? c : o1.getSecond().compareTo(o2.getSecond());
         }
      };
   }

   public <A extends Comparable<A>, B extends Comparable<B>, C extends Comparable<C>>
   Comparator<Triple<A, B, C>> trioNaturalOrdering() {
      return new Comparator<Triple<A, B, C>>() {
         @Override public int compare(Triple<A, B, C> o1, Triple<A, B, C> o2) {
            int c = o1.getFirst().compareTo(o2.getFirst());
            if (c == 0) {
               c = o1.getSecond().compareTo(o2.getSecond());
            }
            return c != 0 ? c : o1.getThird().compareTo(o2.getThird());
         }
      };
   }

   public <A extends Comparable<A>, B extends Comparable<B>, C extends Comparable<C>,
            D extends Comparable<D>>
   Comparator<Quadruple<A, B, C, D>> quartetNaturalOrdering() {
      return new Comparator<Quadruple<A, B, C, D>>() {
         @Override public int compare(Quadruple<A, B, C, D> o1, Quadruple<A, B, C, D> o2) {
            int c = o1.getFirst().compareTo(o2.getFirst());
            if (c == 0) {
               c = o1.getSecond().compareTo(o2.getSecond());
            }
            if (c == 0) {
               c = o1.getThird().compareTo(o2.getThird());
            }
            return c != 0 ? c : o1.getFourth().compareTo(o2.getFourth());
         }
      };
   }
   
   public <A extends Comparable<A>, B extends Comparable<B>, C extends Comparable<C>,
            D extends Comparable<D>, E extends Comparable<E>>
   Comparator<Quintuple<A, B, C, D, E>> quintetNaturalOrdering() {
      return new Comparator<Quintuple<A, B, C, D, E>>() {
         @Override public int compare(Quintuple<A, B, C, D, E> o1, Quintuple<A, B, C, D, E> o2) {
            int c = o1.getFirst().compareTo(o2.getFirst());
            if (c == 0) {
               c = o1.getSecond().compareTo(o2.getSecond());
            }
            if (c == 0) {
               c = o1.getThird().compareTo(o2.getThird());
            }
            if (c == 0) {
               c = o1.getFourth().compareTo(o2.getFourth());
            }
            return c != 0 ? c : o1.getFifth().compareTo(o2.getFifth());
         }
      };
   }

   public <A extends Comparable<A>, B extends Comparable<B>, C extends Comparable<C>,
            D extends Comparable<D>, E extends Comparable<E>>
   Comparator<NTuple<A, B, C, D, E>> nTupleNaturalOrdering() {
      return new Comparator<NTuple<A, B, C, D, E>>() {
         @Override public int compare(NTuple<A, B, C, D, E> o1, NTuple<A, B, C, D, E> o2) {
            int c = 0;
            for (int i = 0, len = Math.max(o1.size(), o2.size()); c == 0 && i < len; i++) {
               if (i >= o1.size()) {
                  return -1;
               } else if (i >= o2.size()) {
                  return 1;
               }
               @SuppressWarnings("unchecked")
               Comparable<Object> o1comparable = (Comparable<Object>) o1.get(i);
               c = o1comparable.compareTo(o2.get(i));
            }
            return c;
         }
      };
   }
}
