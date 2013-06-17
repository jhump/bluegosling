package com.apriori.tuples;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for implementations of {@code Tuple}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractTuple implements Tuple {

   @Override
   public Iterator<Object> iterator() {
      return Arrays.asList(toArray()).iterator();
   }
   
   @Override
   public List<?> asList() {
      return Collections.unmodifiableList(Arrays.asList(toArray()));
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return Arrays.asList(toArray()).toArray(a);
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
