package com.apriori.apt.reflect;

import com.apriori.apt.ElementUtils;

import javax.lang.model.element.PackageElement;

//TODO: javadoc!
public class Package extends AbstractAnnotatedElement {

   private Package(PackageElement element) {
      super(element);
   }
   
   public static Package forElement(PackageElement element) {
      if (element == null) {
         throw new NullPointerException();
      }
      return new Package(element);
   }
   
   public static Package forName(String packageName) {
      return forElement(ElementUtils.get().getPackageElement(packageName));
   }
   
   @Override
   public PackageElement asElement() {
      return (PackageElement) super.asElement();
   }
   
   public String getName() {
      return asElement().getQualifiedName().toString();
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof Package) {
         Package other = (Package) o;
         return getName().equals(other.getName());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return getName().hashCode();
   }

   @Override
   public String toString() {
      return getName();
   }
}
