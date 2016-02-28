package com.bluegosling.reflect.model;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

/**
 * A {@link PrimitiveType} backed by a core reflection {@link AnnotatedType}. The
 * {@linkplain AnnotatedType#getType() underlying type} is a class token that
 * {@linkplain Class#isPrimitive() is primitive}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionPrimitiveType extends CoreReflectionBaseTypeMirror<AnnotatedType>
implements PrimitiveType {
   
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
