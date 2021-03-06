package com.bluegosling.reflect.model;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

/**
 * A {@linkplain TypeKind#PACKAGE package type}. This may be backed by a core reflection
 * {@link Package} or by a {@linkplain CoreReflectionSyntheticPackageElement synthetic package
 * element} (if no package could be loaded).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionPackageType implements CoreReflectionMarker, NoType {
   
   private final PackageElement pkg;
   
   CoreReflectionPackageType(PackageElement pkg) {
      this.pkg = pkg;
   }

   @Override
   public TypeKind getKind() {
      return TypeKind.PACKAGE;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitNoType(this, p);
   }

   @Override
   public List<? extends AnnotationMirror> getAnnotationMirrors() {
      return pkg.getAnnotationMirrors();
   }

   @Override
   public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      return pkg.getAnnotation(annotationType);
   }

   @Override
   public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
      return getAnnotationsByType(annotationType);
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof CoreReflectionPackageType) {
         CoreReflectionPackageType other = (CoreReflectionPackageType) o;
         return pkg.equals(other.pkg);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return pkg.hashCode();
   }
   
   @Override
   public String toString() {
      return pkg.toString();
   }
}
