package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;


class CoreReflectionTypeVariable extends CoreReflectionBaseTypeMirror<AnnotatedTypeVariable>
implements TypeVariable {
   
   private final CoreReflectionTypeParameterElement element;
   
   CoreReflectionTypeVariable(AnnotatedTypeVariable base) {
      super(base);
      assert !(base.getType() instanceof AnnotatedCapturedType.CapturedTypeVariable);
      this.element = new CoreReflectionTypeParameterElement(this,
            (java.lang.reflect.TypeVariable<?>) base.getType());
   }

   CoreReflectionTypeVariable(CoreReflectionTypeParameterElement element,
         AnnotatedTypeVariable base) {
      super(base);
      this.element = element;
   }

   @Override
   public TypeKind getKind() {
      return TypeKind.TYPEVAR;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitTypeVariable(this, p);
   }

   @Override
   public Element asElement() {
      return element;
   }

   @Override
   public TypeMirror getUpperBound() {
      java.lang.reflect.TypeVariable<?> typeVar =
            (java.lang.reflect.TypeVariable<?>) base().getType();
      AnnotatedType[] bounds = typeVar.getAnnotatedBounds();
      if (bounds.length > 1) {
         return new CoreReflectionIntersectionType(bounds);
      } else {
         assert bounds.length == 1;
         return CoreReflectionTypes.INSTANCE.getTypeMirror(bounds[0]);
      }
   }

   @Override
   public TypeMirror getLowerBound() {
      return null;
   }
}
