package com.bluegosling.reflect.model;

import com.bluegosling.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.TypeMirror;

/**
 * An abstract base type for {@link TypeMirror}s backed by core reflection. The type mirror is
 * backed by an {@link AnnotatedType}. There are notable exceptions: type mirrors that have no
 * analog in the {@link java.lang.reflect.Type} hierarchy. These include executable and package
 * types as well as intersection and union types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class CoreReflectionBaseTypeMirror<T extends AnnotatedType> extends CoreReflectionBase<T>
implements TypeMirror {

   CoreReflectionBaseTypeMirror(T base) {
      super(base);
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      }
      if (o.getClass() != getClass()) {
         return false;
      }
      CoreReflectionBaseTypeMirror<?> other = (CoreReflectionBaseTypeMirror<?>) o;
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
