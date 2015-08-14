package com.apriori.reflect.model;

import static java.util.Objects.requireNonNull;

import javax.lang.model.element.Name;

class CoreReflectionName implements Name {
   private final String s;
   
   CoreReflectionName(String s) {
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
      if (o instanceof CoreReflectionName) {
         CoreReflectionName other = (CoreReflectionName) o;
         return s.equals(other.s);
      }
      return false;
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
