package com.apriori.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;


class CoreReflectionCapturedType extends CoreReflectionBaseTypeMirror<AnnotatedTypeVariable>
implements TypeVariable {
   
   private final CapturedTypeParameterElement element;
   
   CoreReflectionCapturedType(AnnotatedTypeVariable captureVariable) {
      super(captureVariable);
      assert captureVariable.getType() instanceof AnnotatedCapturedType.CapturedTypeVariable;
      this.element = new CapturedTypeParameterElement(this, captureVariable);
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
      // TODO: implement me
      return null;
   }

   @Override
   public TypeMirror getLowerBound() {
      // TODO: implement me
      return null;
   }
   
   private static class CapturedTypeParameterElement
   extends CoreReflectionBase<AnnotatedTypeVariable> implements TypeParameterElement {
      private static Name CAPTURED_NAME = new CoreReflectionName("<captured wildcard>");
      
      private final CoreReflectionCapturedType mirror;
      private final Element genericElement;
      
      CapturedTypeParameterElement(CoreReflectionCapturedType mirror, AnnotatedTypeVariable base) {
         super(base);
         this.mirror = mirror;
         this.genericElement = new DummyGenericElement(mirror);
      }

      @Override
      public TypeMirror asType() {
         return mirror;
      }

      @Override
      public ElementKind getKind() {
         return ElementKind.TYPE_PARAMETER;
      }

      @Override
      public Set<Modifier> getModifiers() {
         return Collections.emptySet();
      }

      @Override
      public Name getSimpleName() {
         return CAPTURED_NAME;
      }

      @Override
      public List<? extends Element> getEnclosedElements() {
         return Collections.emptyList();
      }

      @Override
      public <R, P> R accept(ElementVisitor<R, P> v, P p) {
         return v.visitTypeParameter(this, p);
      }

      @Override
      public Element getGenericElement() {
         return genericElement;
      }

      @Override
      public List<? extends TypeMirror> getBounds() {
         return Collections.singletonList(mirror.getUpperBound());
      }

      @Override
      public Element getEnclosingElement() {
         // TODO: implement me
         return null;
      }
   }
   
   private static class DummyGenericElement implements CoreReflectionMarker, Element {
      private static Name DUMMY_NAME = new CoreReflectionName("");
      
      private final TypeMirror type;
      
      DummyGenericElement(TypeMirror type) {
         this.type = type;
      }
      
      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
         return null;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
         return (A[]) Array.newInstance(annotationType, 0);
      }

      @Override
      public TypeMirror asType() {
         return type;
      }

      @Override
      public ElementKind getKind() {
         return ElementKind.OTHER;
      }

      @Override
      public Set<Modifier> getModifiers() {
         return Collections.emptySet();
      }

      @Override
      public Name getSimpleName() {
         return DUMMY_NAME;
      }

      @Override
      public Element getEnclosingElement() {
         return null;
      }

      @Override
      public List<? extends Element> getEnclosedElements() {
         return Collections.emptyList();
      }

      @Override
      public List<? extends AnnotationMirror> getAnnotationMirrors() {
         return Collections.emptyList();
      }

      @Override
      public <R, P> R accept(ElementVisitor<R, P> v, P p) {
         return v.visitUnknown(this, p);
      }
   }
}
