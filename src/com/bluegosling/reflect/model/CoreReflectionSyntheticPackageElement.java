package com.bluegosling.reflect.model;

import java.lang.annotation.Annotation;
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
import javax.lang.model.type.TypeMirror;

/**
 * A {@link PackageElement} for which no {@link Package} object can be loaded. This basically
 * represents the package name; it will have no annotations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionSyntheticPackageElement implements CoreReflectionMarker, PackageElement {
   private final Name qualifiedName;
   private final Name simpleName;
   
   CoreReflectionSyntheticPackageElement(String packageName) {
      qualifiedName = CoreReflectionName.of(packageName);
      int pos = packageName.lastIndexOf('.');
      if (pos == -1) {
         simpleName = qualifiedName;
      } else {
         simpleName = CoreReflectionName.of(packageName.substring(0, pos));
      }
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
   public List<? extends AnnotationMirror> getAnnotationMirrors() {
      return Collections.emptyList();
   }

   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      return null;
   }

   @Override
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitPackage(this, p);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
      return (A[]) Array.newInstance(annotationType, 0);
   }

   @Override
   public Name getQualifiedName() {
      return qualifiedName;
   }

   @Override
   public Name getSimpleName() {
      return simpleName;
   }

   @Override
   public List<? extends Element> getEnclosedElements() {
      return CoreReflectionPackages.getTopLevelTypesAsElements(qualifiedName.toString());
   }

   @Override
   public boolean isUnnamed() {
      return qualifiedName.length() == 0;
   }

   @Override
   public Element getEnclosingElement() {
      return null;
   }
   
   @Override
   public boolean equals(Object o) {
      return o instanceof CoreReflectionSyntheticPackageElement
            && qualifiedName.equals(((CoreReflectionSyntheticPackageElement) o).qualifiedName);
   }
   
   @Override
   public int hashCode() {
      return qualifiedName.hashCode();
   }
   
   @Override
   public String toString() {
      return "package " + qualifiedName;
   }
}
