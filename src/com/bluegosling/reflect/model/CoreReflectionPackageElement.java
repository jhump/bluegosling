package com.bluegosling.reflect.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link PackageElement} backed by a core reflection {@link Package}. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionPackageElement extends CoreReflectionBaseElement<Package>
implements PackageElement {
   private final Name qualifiedName;

   CoreReflectionPackageElement(Package pkg) {
      super(pkg, extractSimpleName(pkg.getName()));
      this.qualifiedName = CoreReflectionName.of(pkg.getName());
   }
   
   private static String extractSimpleName(String qualifiedName) {
      int pos = qualifiedName.lastIndexOf('.');
      return pos == -1 ? qualifiedName : qualifiedName.substring(0, pos);
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
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitPackage(this, p);
   }

   @Override
   public Name getQualifiedName() {
      return qualifiedName;
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
}
