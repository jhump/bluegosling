package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedTypeVariable;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

/**
 * A {@link TypeVariable} that is backed by a core reflection {@link AnnotatedTypeVariable}.
 *
 * @see CoreReflectionCapturedType
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionTypeVariable extends CoreReflectionBaseTypeMirror<AnnotatedTypeVariable>
implements TypeVariable {
   private final CoreReflectionTypeParameterElement element;
   private final TypeMirror bound;
   
   CoreReflectionTypeVariable(AnnotatedTypeVariable base) {
      super(base);
      java.lang.reflect.TypeVariable<?> typeVar =
            (java.lang.reflect.TypeVariable<?>) base.getType();
      assert !(typeVar instanceof AnnotatedCapturedType.CapturedTypeVariable);
      
      this.element = new CoreReflectionTypeParameterElement(this, typeVar);
      this.bound = CoreReflectionTypes.toTypeMirrorOrObject(typeVar.getAnnotatedBounds());
   }

   CoreReflectionTypeVariable(CoreReflectionTypeParameterElement element,
         AnnotatedTypeVariable base) {
      super(base);
      this.element = element;

      java.lang.reflect.TypeVariable<?> typeVar =
            (java.lang.reflect.TypeVariable<?>) base.getType();
      assert !(typeVar instanceof AnnotatedCapturedType.CapturedTypeVariable);
      this.bound = CoreReflectionTypes.toTypeMirrorOrObject(typeVar.getAnnotatedBounds());
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
      return bound;
   }

   @Override
   public TypeMirror getLowerBound() {
      return CoreReflectionNullType.INSTANCE;
   }
}
