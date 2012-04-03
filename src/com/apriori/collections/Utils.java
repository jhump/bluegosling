// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility functions for implementing standard collection interfaces without the use of
 * {@link java.util.AbstractCollection} and its abstract sub-classes.
 * 
 * <p>The collections in this package, as a programming exercise, were written from scratch
 * instead of relying on the abstract base classes provided in the Java Collections Framework.
 * For common functionality that might have otherwise been inherited from one of these classes,
 * static methods in this class are used.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class Utils {

   /** Prevents instantiation. */
   private Utils() {
   }
   
   /**
    * Constructs a string representation of the specified collection.
    * 
    * <p>Empty collections are represented as "[ ]". A collection with a single
    * element would be represented as {@code "[ item ]"}, where <em>"item"</em> is the
    * value of {@code String.valueOf(theOneItem)}. Collections with multiple
    * items follow the same pattern, with multiple items separated by a comma
    * and a single space, like so: {@code "[ item1, item2, item3 ]"}.
    *
    * @param coll the collection
    * @return a string representation of {@code coll}
    */
   public static String toString(Collection<?> coll) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      for (Object item : coll) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(" ");
         sb.append(String.valueOf(item));
      }
      sb.append(" ]");
      return sb.toString();
   }

   /**
    * Implements <em>equals</em> per the contract defined by {@link List#equals(Object)}.
    * 
    * @param list the list
    * @param o an object to compare to the list
    * @return true if {@code o} equals {@code list}
    */
   public static boolean equals(List<?> list, Object o) {
      if (o == null || !(o instanceof List<?>))
         return false;
      List<?> l = (List<?>) o;
      if (l.size() != list.size())
         return false;
      Iterator<?> iter1 = l.iterator();
      Iterator<?> iter2 = list.iterator();
      while (iter1.hasNext()) {
         Object e1 = iter1.next();
         Object e2 = iter2.next();
         if (e1 == null ? e2 != null : !e1.equals(e2))
            return false;
      }
      return true;
   }

   /**
    * Computes the hash code for a list per the contract defined by {@link List#hashCode()}.
    * 
    * @param list the list
    * @return the hash code for {@code list}
    */
   public static int hashCode(List<?> list) {
      int ret = 1;
      for (Object e : list) {
         ret = 31 * ret + (e == null ? 0 : e.hashCode());
      }
      return ret;
   }

   /**
    * Implements <em>equals</em> per the contract defined by {@link Set#equals(Object)}.
    * 
    * @param set the set
    * @param o an object to compare to the list
    * @return true if {@code o} equals {@code list}
    */
   public static boolean equals(Set<?> set, Object o) {
      if (o instanceof Set) {
         Set<?> other = (Set<?>) o;
         if (other.size() == set.size()) {
            for (Object item : set) {
               if (!other.contains(item)) {
                  return false;
               }
            }
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   /**
    * Computes the hash code for a set per the contract defined by {@link Set#hashCode()}.
    * 
    * @param set the set
    * @return the hash code for {@code list}
    */
   public static int hashCode(Set<?> set) {
      int hashCode = 0;
      for (Object item : set) {
         if (item != null) {
            hashCode += item.hashCode();
         }
      }
      return hashCode;
   }

}
