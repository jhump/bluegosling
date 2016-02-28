package com.bluegosling.reflect.model;

import static java.util.Objects.requireNonNull;

import javax.lang.model.element.Name;

/**
 * A {@link Name} that is simply backed by a string.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionName implements Name { 
   static final Name EMPTY = new CoreReflectionName("");
   
   static Name of(String s) {
      return s.isEmpty() ? EMPTY : new CoreReflectionName(s);
   }
   
   private final String s;
   
   private CoreReflectionName(String s) {
      this.s = requireNonNull(s);
   }

   @Override
   public int length() {
      return s.length();
   }

   @Override
   public char charAt(int index) {
      return s.charAt(index);
   }

   @Override
   public CharSequence subSequence(int start, int end) {
      return s.subSequence(start, end);
   }

   @Override
   public boolean contentEquals(CharSequence cs) {
      return s.contentEquals(cs);
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof CoreReflectionName
            && s.equals(((CoreReflectionName) o).s);
   }
   
   @Override
   public int hashCode() {
      return s.hashCode();
   }
   
   @Override
   public String toString() {
      return s;
   }
}
