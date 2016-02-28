package com.bluegosling.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

/**
 * A type mirror for a {@linkplain Types#capture(TypeMirror) captured} type that is backed by core
 * reflection. The actual {@link AnnotatedTypeVariable} behind this mirror will be a
 * {@link AnnotatedCapturedType.CapturedTypeVariable}.
 *
 * @see CoreReflectionTypeVariable
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionCapturedType extends CoreReflectionBaseTypeMirror<AnnotatedTypeVariable>
implements TypeVariable {
   
   private final CapturedTypeParameterElement element;
   private final AnnotatedWildcardType capturedWildcard;
   private final TypeMirror extendsBound;
   private final TypeMirror superBound;
   
   CoreReflectionCapturedType(AnnotatedTypeVariable captureVariable) {
      super(captureVariable);
      AnnotatedCapturedType.CapturedTypeVariable typeVar =
            (AnnotatedCapturedType.CapturedTypeVariable) captureVariable.getType();
      this.capturedWildcard = typeVar.capturedWildcard();
      this.element = new CapturedTypeParameterElement(this, captureVariable);
      this.extendsBound = CoreReflectionTypes.toTypeMirrorOrObject
            (capturedWildcard.getAnnotatedUpperBounds());
      this.superBound = CoreReflectionTypes.toTypeMirrorOrNull(
            capturedWildcard.getAnnotatedUpperBounds()); 
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
   public TypeParameterElement asElement() {
      return element;
   }

   @Override
   public TypeMirror getUpperBound() {
      return extendsBound;
   }

   @Override
   public TypeMirror getLowerBound() {
      return superBound;
   }
   
   /**
    * A type parameter element that represents a captured wildcard.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class CapturedTypeParameterElement
   extends CoreReflectionBase<AnnotatedTypeVariable> implements TypeParameterElement {
      private static Name CAPTURED_NAME = CoreReflectionName.of("<captured wildcard>");
      
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
         return genericElement;
      }
   }
   
   /**
    * The generic element that declared a captured type variable. Since the captured type variable
    * is not actually declared like real type variables, this is a dummy element that is implemented
    * to behave in the same was as an annotation processing environment.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class DummyGenericElement implements CoreReflectionMarker, Element {
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
         return CoreReflectionName.EMPTY;
      }

      @Override
      public Element getEnclosingElement() {
         return DummyPackageElement.INSTANCE;
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

   private enum DummyPackageElement implements CoreReflectionMarker, PackageElement {
      INSTANCE;
      
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
         return new CoreReflectionPackageType(this);
      }

      @Override
      public ElementKind getKind() {
         return ElementKind.PACKAGE;
      }

      @Override
      public Set<Modifier> getModifiers() {
         return Collections.emptySet();
      }

      @Override
      public Name getSimpleName() {
         return CoreReflectionName.EMPTY;
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
         return v.visitPackage(this, p);
      }

      @Override
      public Name getQualifiedName() {
         return CoreReflectionName.EMPTY;
      }

      @Override
      public boolean isUnnamed() {
         // NB: matches behavior of processing environment, wherein only the real unnamed package
         // returns true and other dummy objects (like the packages that contain unknown generic
         // declarations) return false even though their qualified name is empty. 
         return false;
      }
   }
}
