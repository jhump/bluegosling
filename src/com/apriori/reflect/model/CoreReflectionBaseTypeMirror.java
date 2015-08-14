package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.TypeMirror;


abstract class CoreReflectionBaseTypeMirror extends CoreReflectionBase implements TypeMirror {

   CoreReflectionBaseTypeMirror(AnnotatedType base) {
      super(base);
   }

   @Override
   protected AnnotatedType base() {
      return (AnnotatedType) super.base();
   }
   
   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      }
      if (o.getClass() != getClass()) {
         return false;
      }
      CoreReflectionBaseTypeMirror other = (CoreReflectionBaseTypeMirror) o;
      return AnnotatedTypes.equals(base(), other.base());
   }
   
   @Override
   public int hashCode() {
      return AnnotatedTypes.hashCode(base());
   }
   
   @Override
   public String toString() {
      return AnnotatedTypes.toString(base());
   }
}
