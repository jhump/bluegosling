package com.bluegosling.collections;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;

/**
 * An enum for the various {@linkplain Spliterator#characteristics() characteristics} of a
 * spliterator.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public enum SpliteratorCharacteristics {
   /**
    * The {@linkplain Spliterator#CONCURRENT concurrent} characteristic.
    */
   CONCURRENT(Spliterator.CONCURRENT),

   /**
    * The {@linkplain Spliterator#DISTINCT distinct} characteristic.
    */
   DISTINCT(Spliterator.DISTINCT),

   /**
    * The {@linkplain Spliterator#IMMUTABLE immutable} characteristic.
    */
   IMMUTABLE(Spliterator.IMMUTABLE),

   /**
    * The {@linkplain Spliterator#NONNULL non-null} characteristic.
    */
   NONNULL(Spliterator.NONNULL),
   
   /**
    * The {@linkplain Spliterator#ORDERED ordered} characteristic.
    */
   ORDERED(Spliterator.ORDERED),

   /**
    * The {@linkplain Spliterator#SIZED sized} characteristic.
    */
   SIZED(Spliterator.SIZED),

   /**
    * The {@linkplain Spliterator#SORTED sorted} characteristic.
    */
   SORTED(Spliterator.SORTED),

   /**
    * The {@linkplain Spliterator#SUBSIZED sub-sized} characteristic.
    */
   SUBSIZED(Spliterator.SUBSIZED);
   
   private static final Map<Integer, SpliteratorCharacteristics> ALL;
   static {
      Map<Integer, SpliteratorCharacteristics> allBits = new HashMap<>();
      for (SpliteratorCharacteristics ch : values()) {
         allBits.put(ch.toInt(), ch);
      }
      ALL = Collections.unmodifiableMap(allBits);
   }
   
   private final int bit;
   
   SpliteratorCharacteristics(int bit) {
      this.bit = bit;
   }
   
   /**
    * Returns the value of this characteristic as an int.
    * 
    * @return the value of this characteristic as an int
    */
   public int toInt() {
      return bit;
   }
   
   /**
    * Returns the int bitmask value for the given set of characteristics.
    * 
    * @param characteristics set of characteristics
    * @return the int bitmask value for the given set
    */
   public static int toInt(Iterable<SpliteratorCharacteristics> characteristics) {
      int i = 0;
      for (SpliteratorCharacteristics ch : characteristics) {
         i |= ch.toInt();
      }
      return i;
   }
   
   /**
    * Returns the set of characteristics represented by the given int bitmask.
    * 
    * @param characteristics bitmask of characteristics
    * @return the set of characteristics represented by the given bitmask
    */
   public static EnumSet<SpliteratorCharacteristics> fromInt(int characteristics) {
      EnumSet<SpliteratorCharacteristics> set = EnumSet.noneOf(SpliteratorCharacteristics.class);
      for (int lowest = Integer.lowestOneBit(characteristics); lowest != 0; ) {
         SpliteratorCharacteristics ch = ALL.get(lowest);
         if (ch != null) {
            set.add(ch);
         }
         characteristics &= ~lowest;
         lowest = Integer.lowestOneBit(characteristics);
      }
      return set;
   }
}