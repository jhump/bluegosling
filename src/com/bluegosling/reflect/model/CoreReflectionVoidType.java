package com.bluegosling.reflect.model;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

/**
 * A {@linkplain TypeKind#VOID void type} that is backed by a core reflection {@link AnnotatedType}.
 * The {@linkplain AnnotatedType#getType() underlying type} is {@code void.class}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionVoidType extends CoreReflectionBaseTypeMirror<AnnotatedType> implements NoType {

   CoreReflectionVoidType(AnnotatedType base) {
      super(base);
      assert base.getType() == void.class;
   }

   @Override
   public TypeKind getKind() {
      return TypeKind.VOID;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitNoType(this, p);
   }
}
