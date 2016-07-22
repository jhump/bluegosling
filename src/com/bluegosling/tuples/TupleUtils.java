package com.bluegosling.tuples;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.bluegosling.util.ValueType;

/**
 * Contains methods for implementations of {@code Tuple}. This is not an abstract base class because
 * the concrete tuple classes are {@linkplain ValueType value types}, so may not have super-classes.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class TupleUtils {

   static Iterator<Object> iterator(Tuple t) {
      return Arrays.asList(t.toArray()).iterator();
   }

   static List<?> asList(Tuple t) {
      return Collections.unmodifiableList(Arrays.asList(t.toArray()));
   }

   @SuppressWarnings("unchecked") // reflective creation of array requires cast but is safe
   static <T> T[] toArray(Tuple t, T[] a) {
      int size = t.size();
      if (a.length < size) {
         a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
      }
      System.arraycopy(t.toArray(), 0, a, 0, size);
      if (a.length > size) {
         a[size] = null;
      }
      return a;
   }
   
   static boolean equals(Tuple t, Object o) {
      if (o instanceof Tuple) {
         return Arrays.equals(t.toArray(), ((Tuple) o).toArray());
      }
      return false;
   }
   
   static int hashCode(Tuple t) {
      return Arrays.hashCode(t.toArray());
   }
   
   static String toString(Tuple t) {
      StringBuilder sb = new StringBuilder();
      sb.append(t.getClass().getSimpleName());
      sb.append("(");
      boolean first = true;
      for (Object o : t.toArray()) {
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
