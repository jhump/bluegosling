package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedArrayType;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;


class CoreReflectionArrayType extends CoreReflectionBaseTypeMirror<AnnotatedArrayType>
implements ArrayType {
   private final TypeMirror component;
   
   CoreReflectionArrayType(AnnotatedArrayType base) {
      super(base);
      this.component =
            CoreReflectionTypes.INSTANCE.getTypeMirror(base.getAnnotatedGenericComponentType());
   }

   @Override
   public TypeKind getKind() {
      return TypeKind.ARRAY;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitArray(this, p);
   }

   @Override
   public TypeMirror getComponentType() {
      return component;
   }
}
