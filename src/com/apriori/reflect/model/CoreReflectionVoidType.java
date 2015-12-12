package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;


class CoreReflectionVoidType extends CoreReflectionBaseTypeMirror<AnnotatedType> implements NoType {

   CoreReflectionVoidType(AnnotatedType base) {
      super(base);
      assert ((Class<?>) base.getType()) == void.class;
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
