// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
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
public class CollectionUtils {
   
   private static final int FILTER_THRESHOLD_FOR_NEW_SET = 100;
   
   /**
    * A comparator that uses the objects' {@linkplain Comparable natural ordering}.
    */
   public static final Comparator<Object> NATURAL_ORDERING = new Comparator<Object>() {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(Object o1, Object o2) {
         return ((Comparable<Object>) o1).compareTo(o2);
      }
   };

   /** Prevents instantiation. */
   private CollectionUtils() {
   }
   
   /**
    * Constructs a string representation of the specified collection.
    * 
    * <p>Empty collections are represented as {@code "[ ]"}. A collection with a single
    * element would be represented as {@code "[ item ]"}, where <em>"item"</em> is the
    * value of {@link String#valueOf(Object) String.valueOf(theOneItem)}. Collections
    * with multiple items follow the same pattern, with multiple items separated by a
    * comma and a single space, like so: {@code "[ item1, item2, item3 ]"}.
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

   /**
    * Filters all objects in the list against a specified collection. Either removes all items that
    * are found in specified collection or removes all items that are not found in the collection.
    * Uses {@link Iterator#remove()} to remove items from the target list.
    * 
    * @param items collection of items acting as a filter
    * @param iter iterator over the target collection, from which items will be removed
    * @param remove true if <em>removing</em> all items found in the collection or false if
    *           <em>retaining</em> them
    * @return true if the list was modified and something was removed
    */
   public static boolean filter(Collection<?> items, Iterator<?> iter, boolean remove) {
      // if list is not already a set and is reasonably big, add the items
      // to a new HashSet. This should mitigate the otherwise O(n^2) runtime
      // speed and provide O(n) instead. The overhead (creating new and
      // populating new set as well as garbage collecting it later) isn't
      // worth it for small collections. If the collection already implements
      // Set, we'll live with the runtime it provides -- which is hopefully no
      // worse than O(log n), like for TreeSet, which changes this batch
      // operation from O(n) to O(n log n).
      if (!(items instanceof Set<?>) && items.size() > FILTER_THRESHOLD_FOR_NEW_SET) {
         items = new HashSet<Object>(items);
      }
      boolean modified = false;
      while (iter.hasNext()) {
         Object o = iter.next();
         if (items.contains(o) == remove) {
            iter.remove();
            if (!modified)
               modified = true;
         }
      }
      return modified;
   }
   
   /**
    * Checks if the given object (or range lower bound) lies above another
    * lower bound.
    * 
    * <p>Generally, this is used to test if an object is above the specified
    * lower bound (in which case, the parameter {@code oIncluded} will be true).
    * But we can also tell if the lower bound of one range (specified by the
    * parameters {@code o} and {@code oIncluded}) lies above the lower bound
    * of another range (specified by {@code from} and {@code fromInclusive}).
    *
    * @param o the object
    * @param oIncluded true if {@code o} needs to be included in the range
    * @param from the lower bound of the range
    * @param fromInclusive true if {@code from} is included in the range; false
    *       otherwise
    * @param comp the comparator used to compare {@code o} to {@code from}
    *       (cannot be null)
    * @return true if the specified object is greater than the bound and the
    *       bound is exclusive; true if the object is greater than or equal
    *       to the bound and the bound is inclusive; false otherwise
    */
   public static boolean isInRangeLow(Object o, boolean oIncluded, Object from,
         boolean fromInclusive, Comparator<Object> comp) {
      if (from != null) {
         int c = comp.compare(o, from);
         if (c < 0 || (c == 0 && oIncluded && !fromInclusive)) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Checks if the given object (or range upper bound) falls under another
    * upper bound.
    * 
    * <p>Generally, this is used to test if an object is under the specified
    * upper bound (in which case, the parameter {@code oIncluded} will be true).
    * But we can also tell if the upper bound of one range (specified by the
    * parameters {@code o} and {@code oIncluded}) falls under the upper bound
    * of another range (specified by {@code to} and {@code toInclusive}).
    *
    * @param o the object
    * @param oIncluded true if {@code o} needs to be included in the range
    * @param to the upper bound of the range
    * @param toInclusive true if {@code to} is included in the range, false
    *       otherwise
    * @param comp the comparator used to compare {@code o} to {@code to}
    *       (cannot be null)
    * @return true if the specified object is less than the bound and the
    *       bound is exclusive, true if the object is less than or equal
    *       to the bound and the bound is inclusive, and false otherwise
    */
   public static boolean isInRangeHigh(Object o, boolean oIncluded, Object to, boolean toInclusive,
         Comparator<Object> comp) {
      if (to != null) {
         int c = comp.compare(o, to);
         if (c > 0 || (c == 0 && oIncluded && !toInclusive)) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Checks if the given object is within the given bounds.
    *
    * @param o the object
    * @param from the lower bound of the range
    * @param fromInclusive true if {@code from} is included in the range; false
    *       otherwise
    * @param to the upper bound of the range
    * @param toInclusive true if {@code to} is included in the range; false
    *       otherwise
    * @param comp the comparator used to compare {@code o} to {@code to}
    *       (cannot be null)
    * @return true if the specified object is within the specified bounds; false
    *       otherwise
    */
   public static boolean isInRange(Object o, Object from, boolean fromInclusive, Object to,
         boolean toInclusive, Comparator<Object> comp) {
      return isInRangeLow(o, true, from, fromInclusive, comp)
            && isInRangeHigh(o, true, to, toInclusive, comp);
   }
}
