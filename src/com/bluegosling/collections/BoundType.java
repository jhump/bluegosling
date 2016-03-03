package com.bluegosling.collections;

/**
 * The type of bound for a {@link AbstractNavigableMap.SubMap}. Sub-map views have both lower and
 * upper bounds. These bounds are optional and, if present, can be inclusive or exclusive.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public enum BoundType {
   /**
    * Indicates that a given bound is inclusive.
    */
   INCLUSIVE,

   /**
    * Indicates that a given bound is exclusive.
    */
   EXCLUSIVE,
   
   /**
    * Indicates that a given bound is absent.
    */
   NO_BOUND
}