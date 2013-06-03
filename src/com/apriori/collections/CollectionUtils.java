// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
// TODO: update Transforming*/Filtering* to use methods in this class
final class CollectionUtils {
   /** Prevents instantiation. */
   private CollectionUtils() {
   }
   
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
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static final <T extends Comparable<T>> Comparator<T> naturalOrdering() {
      return (Comparator) NATURAL_ORDERING;
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
         return other.size() == set.size() && set.containsAll(other);
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
   // TODO: add javadoc about using this to implement Collection.removeAll/retainAll
   public static boolean filter(Collection<?> items, Iterator<?> iter, boolean remove) {
      boolean modified = false;
      while (iter.hasNext()) {
         if (items.contains(iter.next()) == remove) {
            iter.remove();
            modified = true;
         }
      }
      return modified;
   }

   /**
    * Removes from a collection all items retrieved from a specified iterator. This can be used to
    * implement {@link Collection#removeAll(Collection)} for a collection that has a fast (e.g.
    * logarithmic or constant time) {@linkplain Collection#remove(Object) remove} operation. For
    * collections with slower (e.g. linear) remove operations, {@link #filter(Collection, Iterator, boolean)}
    * will be the better choice.
    * 
    * <p>Examples:<pre>
    * // Using CollectionUtils.removeAll()
    * {@literal @}Override public boolean removeAll(Collection&lt;?&gt; c) {
    *   return CollectionUtils.removeAll(this, c.iterator());
    * } 
    * 
    * // Alternate implementation instead using CollectionUtils.filter()
    * {@literal @}Override public boolean removeAll(Collection&lt;?&gt; c) {
    *   return CollectionUtils.filter(c, iterator(), true);
    * } 
    * </pre>
    * 
    * @param collection the collection from which items are to be removed
    * @param itemsToRemove the items to remove
    * @return true if the collection was modified (e.g. one or more items actually removed)
    */
   public static boolean removeAll(Collection<?> collection, Iterator<?> itemsToRemove) {
      boolean modified = false;
      while (itemsToRemove.hasNext()) {
         Object o = itemsToRemove.next();
         // in case collection allows duplicates, we need to repeat remove operation until it
         // returns false to make sure we get all of them
         while (true) {
            if (collection.remove(o)) {
               modified = true;
            } else {
               // all removed, move on to next
               break;
            }
         }
      }
      return modified;
   }

   /**
    * Removes from a set all items retrieved from a specified iterator.
    * 
    * <p>The only difference between this method and {@link #removeAll(Collection, Iterator)} is
    * that this version assumes the collection cannot have duplicates (after all, it is a set). So
    * it invokes {@link Set#remove(Object)} only once per item to remove. Since other collections
    * may have duplicates, the other version must invoke the method until it returns false
    * (indicating that no more occurrences are in the collection).
    * 
    * @param collection the collection from which items are to be removed
    * @param itemsToRemove the items to remove
    * @return true if the collection was modified (e.g. one or more items actually removed)
    * 
    * @see #removeAll(Collection, Iterator)
    */
   public static boolean removeAll(Set<?> collection, Iterator<?> itemsToRemove) {
      boolean modified = false;
      while (itemsToRemove.hasNext()) {
         if (collection.remove(itemsToRemove.next())) {
            modified = true;
         }
      }
      return modified;
   }
   
   // TODO: javadoc
   public static boolean containsAll(Collection<?> collectionToCheck, Collection<?> items) {
      for (Object o : items) {
         if (!collectionToCheck.contains(o)) {
            return false;
         }
      }
      return true;
   }

   // TODO: javadoc
   // TODO: update collections to use this where appropriate
   // TODO: consider replacing usages of findObject() with this 
   public static boolean contains(Iterator<?> iteratorToCheck, Object item) {
      while (iteratorToCheck.hasNext()) {
         Object o = iteratorToCheck.next();
         if (o == null ? item == null : o.equals(item)) {
            return true;
         }
      }
      return false;
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

   /**
    * Fills the specified array using the contents of the specified collection.
    * 
    * @param coll a collection of elements
    * @param array an array to populate
    */
   public static void copyToArray(Iterable<?> coll, Object[] array) {
      int idx = 0;
      for (Object o : coll) {
         array[idx++] = o;
      }
   }
   
   /**
    * Implements {@link Collection#toArray()}. This is based on the collection's
    * {@linkplain Collection#iterator() iterator}.
    * 
    * @param coll the collection to convert to an array
    * @return an array with the same elements as the specified collection
    */
   public static Object[] toArray(Collection<?> coll) {
      Object ret[] = new Object[coll.size()];
      copyToArray(coll, ret);
      return ret;
   }

   /**
    * Implements {@link Collection#toArray(Object[])}. This is based on the collection's
    * {@linkplain Collection#iterator() iterator}.
    * 
    * @param coll the collection to convert to an array
    * @param array the array to fill (or whose component type is to be used to allocate a new array)
    * @param <T> the type of the specified array.
    * @return an array with the same elements as the specified collection
    */
   public static <T> T[] toArray(Collection<?> coll, T[] array) {
      int size = coll.size();
      array = ArrayUtils.ensureCapacity(array, size);
      copyToArray(coll, array);
      if (array.length > size) {
         array[size] = null;
      }
      return array;
   }

   /**
    * Returns an iterator that traverses elements in the opposite order of the specified iterator.
    * In other words, {@link ListIterator#next()} return the <em>previous</em> element and vice
    * versa.
    * 
    * <p>The returned iterator will support all operations that the underlying iterator supports,
    * including {@code add} and {@link remove}. Adding multiple elements in a row from the reversed
    * iterator effectively adds them in reverse order.
    * 
    * @param iter an iterator
    * @return a reversed iterator
    */
   public static <E> ListIterator<E> reverseIterator(final ListIterator<E> iter) {
      // wrap the list iterator with a simple version that
      // just iterates backwards
      return new ListIterator<E>() {
         private boolean added;
         
         @Override
         public void add(E e) {
            iter.add(e);
            // Add places item before the result returned by subsequent call to next() which means
            // newly added element is returned by call to previous(). To reverse (and make sure
            // multiple additions effectively inserts items in the right place in reverse order),
            // we have to adjust the cursor:
            iter.previous();
            // Underlying iterator would now allow a remove() or set() operation and act on the item
            // we just added, but proper behavior is to disallow the operation. We have to manage
            // that ourselves.
            added = true;
         }
   
         @Override
         public boolean hasNext() {
            return iter.hasPrevious();
         }
   
         @Override
         public boolean hasPrevious() {
            return iter.hasNext();
         }
   
         @Override
         public E next() {
            added = false; // reset
            return iter.previous();
         }
   
         @Override
         public int nextIndex() {
            return iter.previousIndex();
         }
   
         @Override
         public E previous() {
            added = false; // reset
            return iter.next();
         }
   
         @Override
         public int previousIndex() {
            return iter.nextIndex();
         }
   
         @Override
         public void remove() {
            if (added) {
               throw new IllegalStateException("Cannot remove item after call to add()");
            }
            iter.remove();
         }
   
         @Override
         public void set(E e) {
            if (added) {
               throw new IllegalStateException("Cannot set item after call to add()");
            }
            iter.set(e);
         }
      };
   }

   /**
    * Finds an item by iterating through the specified iterator.
    * 
    * @param item the object to find
    * @param iter the iterator to examine
    * @return list index of the item or -1 if the item was not found
    */
   public static int findObject(Object item, ListIterator<?> iter) {
      while (iter.hasNext()) {
         Object o = iter.next();
         if (item == null ? o == null : item.equals(o)) {
            return iter.previousIndex();
         }
      }
      return -1;
   }

   /**
    * Removes a specified object using an iterator. This helper method implements
    * {@link #remove(Object)}, {@link #removeAll(Object)}, {@link #removeFirstOccurrence(Object)},
    * and even {@link #removeLastOccurrence(Object)} (the lattermost of which uses a
    * {@link reverseIterator} to find the last occurrence instead of the first).
    * 
    * @param item the item to remove
    * @param iter the iterator from which to remove the item
    * @param justFirst true if just removing the first matching item or false if removing all
    *           matching items
    * @return true if the item was found and removed or false if the item was not found in the
    *         iterator
    */
   public static boolean removeObject(Object item, Iterator<?> iter, boolean justFirst) {
      boolean modified = false;
      while (iter.hasNext()) {
         Object o = iter.next();
         if (item == null && o == null) {
            iter.remove();
            if (justFirst)
               return true;
            if (!modified)
               modified = true;
         }
         else if (item != null && item.equals(o)) {
            iter.remove();
            if (justFirst)
               return true;
            if (!modified)
               modified = true;
         }
      }
      return modified;
   }
}
