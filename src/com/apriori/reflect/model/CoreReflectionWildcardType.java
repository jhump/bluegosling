package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedWildcardType;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;

/**
 * A {@link WildcardType} that is backed by a core reflection {@link AnnotatedWildcardType}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionWildcardType extends CoreReflectionBaseTypeMirror<AnnotatedWildcardType>
implements WildcardType {
   private final TypeMirror extendsBound;
   private final TypeMirror superBound;

   CoreReflectionWildcardType(AnnotatedWildcardType wildcardType) {
      super(wildcardType);
      this.extendsBound = CoreReflectionTypes.toTypeMirrorOrObject
            (wildcardType.getAnnotatedUpperBounds());
      this.superBound = CoreReflectionTypes.toTypeMirrorOrNull(
            wildcardType.getAnnotatedUpperBounds()); 
   }
   
   @Override
   public TypeKind getKind() {
      return TypeKind.WILDCARD;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitWildcard(this, p);
   }

   @Override
   public TypeMirror getExtendsBound() {
      return extendsBound;
   }

   @Override
   public TypeMirror getSuperBound() {
      return superBound;
   }
}
