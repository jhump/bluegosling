package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;


class CoreReflectionPrimitiveType extends CoreReflectionBaseTypeMirror implements PrimitiveType {
   
   private final TypeKind kind;

   CoreReflectionPrimitiveType(AnnotatedType base, TypeKind kind) {
      super(base);
      assert ((Class<?>) base.getType()).isPrimitive();
      this.kind = kind;
   }

   @Override
   public TypeKind getKind() {
      return kind;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitPrimitive(this, p);
   }
}
