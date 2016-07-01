package com.bluegosling.collections.immutable;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;


/**
 * Utility methods related to immutable collections.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class Immutables {
   private Immutables() {
   }
   
   /**
    * Safely upcasts the element type of an immutable collection. This operation is safe thanks to
    * the input being immutable. This can help adapt instances of immutable collection to otherwise
    * invariant collection types, without the need for unchecked casts.
    *
    * @param collection an immutable collection
    * @param <E> the element type of the collection
    * @param <S> the source type of the collection being re-cast
    * @param <T> the target type to which the collection is re-cast
    * @return the input collection, but with its element type re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <E, S extends ImmutableCollection<? extends E>, T extends ImmutableCollection<E>>
   T cast(S collection) {
      return (T) collection;
   }

   /**
    * Safely upcasts the key and/or value types of an immutable map. This operation is safe thanks
    * to the input being immutable. This can help adapt instances of immutable map to otherwise
    * invariant map types, without the need for unchecked casts.
    *
    * @param map an immutable map
    * @param <K> the key type of the map
    * @param <V> the value type of the map
    * @param <S> the source type of the map being re-cast
    * @param <T> the target type to which the map is re-cast
    * @return the input map, but with its key or value type (or both) re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <K, V, S extends ImmutableMap<? extends K, ? extends V>, T extends ImmutableMap<K, V>>
   T cast(S map) {
      return (T) map;
   }

   /**
    * Safely upcasts the element type of an immutable collection. This operation is safe thanks to
    * the input being immutable. This can help adapt instances of immutable collection to otherwise
    * invariant collection types, without the need for unchecked casts.
    *
    * @param collection an immutable collection
    * @param <E> the element type of the collection
    * @param <S> the source type of the collection being re-cast
    * @param <T> the target type to which the collection is re-cast
    * @return the input collection, but with its element type re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <E, S extends PersistentCollection<? extends E>, T extends PersistentCollection<E>>
   T cast(S collection) {
      return (T) collection;
   }

   /**
    * Safely upcasts the key and/or value types of an immutable map. This operation is safe thanks
    * to the input being immutable. This can help adapt instances of immutable map to otherwise
    * invariant map types, without the need for unchecked casts.
    *
    * @param map an immutable map
    * @param <K> the key type of the map
    * @param <V> the value type of the map
    * @param <S> the source type of the map being re-cast
    * @param <T> the target type to which the map is re-cast
    * @return the input map, but with its key or value type (or both) re-cast
    */
   @SuppressWarnings("unchecked") // safe thanks to the type bounds and immutability
   public static <K, V, S extends PersistentMap<? extends K, ? extends V>, T extends PersistentMap<K, V>>
   T cast(S map) {
      return (T) map;
   }
}
