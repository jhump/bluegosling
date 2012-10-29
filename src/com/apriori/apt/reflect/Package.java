package com.apriori.apt.reflect;

import com.apriori.apt.ElementUtils;

import javax.lang.model.element.PackageElement;

/**
 * A package. This is analogous to {@link java.lang.Package
 * java.lang.reflect.Package}, except that it represents packages
 * of Java source (during annotation processing) vs. representing packages
 * of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.Package
 */
public class Package extends AbstractAnnotatedElement {

   private Package(PackageElement element) {
      super(element);
   }
   
   /**
    * Returns a package based on the specified element.
    * 
    * @param element the element
    * @return a package
    * @throws NullPointerException if the specified element is null
    */
   public static Package forElement(PackageElement element) {
      if (element == null) {
         throw new NullPointerException();
      }
      return new Package(element);
   }
   
   /**
    * Returns the package of the specified name.
    * 
    * @param packageName a package name
    * @return a package or {@code null} if the named package doesn't exist in the class path or in
    *       the compilation units being processed
    */
   public static Package forName(String packageName) {
      return forElement(ElementUtils.get().getPackageElement(packageName));
   }
   
   @Override
   public PackageElement asElement() {
      return (PackageElement) super.asElement();
   }
   
   /**
    * Returns the fully-qualified package name.
    * 
    * @return the package's name
    * 
    * @see java.lang.Package#getName()
    */
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
