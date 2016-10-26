package com.bluegosling.collections.sets;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A set that can also provide the canonical instance for a given value.
 *
 * @param <E> the type of elements in the set
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface CanonicalSet<E> extends Set<E> {
   /**
    * Returns the canonical representation for the given value or {@code null} if the value is
    * not present in this set.
    *
    * @param element a value
    * @return the canonical representation for the given value or {@code null}
    */
   E canonicalize(Object element);
   
   /**
    * Creates a new canonical set backed by the given map. The map should not be used to store
    * arbitrary mappings as it is expected that a value is always equal to its corresponding key.
    * As items are added to this canonical set, that is how mappings are added to the backing map.
    * The values in the map are also used to {@linkplain #canonicalize(Object) canonicalize} the
    * contents of the set.
    *
    * @param <E> the type of elements in the set
    * @param map a map
    * @return a canonical set backed by the given map
    */
   static <E> CanonicalSet<E> newCanonicalSetFromMap(Map<E, E> map) {
      if (!map.isEmpty()) {
         throw new IllegalArgumentException();
      }
      return new CanonicalSet<E>() {
         @Override
         public int size() {
            return map.size();
         }

         @Override
         public boolean isEmpty() {
            return map.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return map.containsKey(o);
         }

         @Override
         public Iterator<E> iterator() {
            return map.keySet().iterator();
         }

         @Override
         public Object[] toArray() {
            return map.keySet().toArray();
         }

         @Override
         public <T> T[] toArray(T[] a) {
            return map.keySet().toArray(a);
         }

         @Override
         public boolean add(E e) {
            boolean ret = map.containsKey(e);
            map.put(e, e);
            return ret;
         }

         @Override
         public boolean remove(Object o) {
            return map.keySet().remove(o);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return map.keySet().containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends E> c) {
            boolean ret = false;
            for (E e : c) {
               if (add(e)) {
                  ret = true;
               }
            }
            return ret;
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            return map.keySet().retainAll(c);
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            return map.keySet().retainAll(c);
         }

         @Override
         public void clear() {
            map.clear();
         }

         @Override
         public E canonicalize(Object element) {
            return map.get(element);
         }
         
         @Override
         public boolean equals(Object o) {
            return map.keySet().equals(o);
         }
         
         @Override
         public int hashCode() {
            return map.keySet().hashCode();
         }
         
         @Override
         public String toString() {
            return map.keySet().toString();
         }
      };
   }
}
