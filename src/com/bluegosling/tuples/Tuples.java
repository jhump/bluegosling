package com.bluegosling.tuples;

import com.bluegosling.collections.ImmutableMap;
import com.bluegosling.collections.PriorityQueue;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

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

   /**
    * Converts a map entry into a pair of key and value.
    *
    * @param entry a map entry
    * @return a pair of the entry's key and its value
    * @throws NullPointerException if the given entry is {@code null}
    */
   public <K, V> Pair<K, V> fromEntry(Map.Entry<? extends K, ? extends V> entry) {
      return Pair.<K, V>create(entry.getKey(), entry.getValue());
   }

   /**
    * Converts an immutable map entry into a pair of key and value.
    *
    * @param entry an immutable map entry
    * @return a pair of the entry's key and its value
    * @throws NullPointerException if the given entry is {@code null}
    */
   public <K, V> Pair<K, V> fromEntry(ImmutableMap.Entry<? extends K, ? extends V> entry) {
      return Pair.<K, V>create(entry.key(), entry.value());
   }

   /**
    * Converts a priority queue entry into a pair of element and priority.
    *
    * @param entry a priority queue entry
    * @return a pair of the entry's element and priority
    * @throws NullPointerException if the given entry is {@code null}
    */
   public <E, P> Pair<E, P> fromEntry(PriorityQueue.Entry<? extends E, ? extends P> entry) {
      return Pair.<E, P>create(entry.getElement(), entry.getPriority());
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
   Comparator<Trio<A, B, C>> trioNaturalOrdering() {
      return new Comparator<Trio<A, B, C>>() {
         @Override public int compare(Trio<A, B, C> o1, Trio<A, B, C> o2) {
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
   Comparator<Quartet<A, B, C, D>> quartetNaturalOrdering() {
      return new Comparator<Quartet<A, B, C, D>>() {
         @Override public int compare(Quartet<A, B, C, D> o1, Quartet<A, B, C, D> o2) {
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
   Comparator<Quintet<A, B, C, D, E>> quintetNaturalOrdering() {
      return new Comparator<Quintet<A, B, C, D, E>>() {
         @Override public int compare(Quintet<A, B, C, D, E> o1, Quintet<A, B, C, D, E> o2) {
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
