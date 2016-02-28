package com.bluegosling.tuples;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for implementations of {@code Tuple}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractTuple implements Tuple {

   @Override
   public Iterator<Object> iterator() {
      return Arrays.asList(toArray()).iterator();
   }
   
   @Override
   public List<?> asList() {
      return Collections.unmodifiableList(Arrays.asList(toArray()));
   }

   @Override
   @SuppressWarnings("unchecked") // reflective creation of array requires cast but is safe
   public <T> T[] toArray(T[] a) {
      int size = size();
      if (a.length < size) {
         a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
      }
      System.arraycopy(toArray(), 0, a, 0, size);
      if (a.length > size) {
         a[size] = null;
      }
      return a;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof Tuple) {
         return Arrays.equals(toArray(), ((Tuple) o).toArray());
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return Arrays.hashCode(toArray());
   }
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getClass().getSimpleName());
      sb.append("(");
      boolean first = true;
      for (Object o : toArray()) {
         if (first) {
            first = false;
         } else {
            sb.append(", ");
         }
         sb.append(o);
      }
      sb.append(")");
      return sb.toString();
   }
}
