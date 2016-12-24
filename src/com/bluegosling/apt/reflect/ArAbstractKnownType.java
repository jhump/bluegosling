package com.bluegosling.apt.reflect;

import javax.lang.model.type.TypeMirror;

/**
 * An abstract super-class for {@link ArType}s that represent known types (as opposed to wildcards
 * or type variables, which can be a bound to a range of types, both known and unknown). Array types
 * do not extend this super-class because, the way they are modeled in this package, their component
 * type is not necessarily known.
 * 
 * <p>Known types always have {@link ArClass} counterparts. (Unknown types can also be converted to
 * {@link ArClass} instances, but that requires that their erasure be computed.)
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class ArAbstractKnownType extends ArType {
   private final ArClass clazz;
   
   ArAbstractKnownType(TypeMirror mirror) {
      super(mirror);
      this.clazz = ArClass.forTypeMirror(mirror);
   }
   
   /**
    * Returns the class that corresponds to this type.
    * 
    * @return the class that corresponds to this type
    */
   public ArClass asClass() {
      return clazz;
   }

   @Override
   public TypeMirror asTypeMirror() {
      return delegate();
   }
}
