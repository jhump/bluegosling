package com.apriori.reflect.model;

import java.io.Writer;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;


enum CoreReflectionElements implements Elements {
   INSTANCE;

   @Override
   public PackageElement getPackageElement(CharSequence name) {
      String packageName = name.toString();
      if (!CoreReflectionPackages.doesPackageExist(packageName)) {
         return null;
      }
      Package pkg = CoreReflectionPackages.getPackage(packageName);
      return pkg == null
            ? new CoreReflectionSyntheticPackageElement(name.toString())
            : new CoreReflectionPackageElement(pkg);
   }

   @Override
   public TypeElement getTypeElement(CharSequence name) {
      try {
         return new CoreReflectionTypeElement(Class.forName(name.toString()));
      } catch (ClassNotFoundException e) {
         return null;
      }
   }

   @Override
   public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
         AnnotationMirror a) {
      // TODO: implement me
      return null;
   }

   @Override
   public String getDocComment(Element e) {
      return null;
   }

   @Override
   public boolean isDeprecated(Element e) {
      return e.getAnnotation(Deprecated.class) != null;
   }

   @Override
   public Name getBinaryName(TypeElement type) {
      // TODO: implement me
      return null;
   }

   @Override
   public PackageElement getPackageOf(Element type) {
      while (type.getKind() != ElementKind.PACKAGE) {
         type = type.getEnclosingElement();
      }
      return (PackageElement) type;
   }

   @Override
   public List<? extends Element> getAllMembers(TypeElement type) {
      // TODO: implement me
      return null;
   }

   @Override
   public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
      // TODO: implement me
      return null;
   }

   @Override
   public boolean hides(Element hider, Element hidden) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
         TypeElement type) {
      // TODO: implement me
      return false;
   }

   @Override
   public String getConstantExpression(Object value) {
      // TODO: implement me
      return null;
   }

   @Override
   public void printElements(Writer w, Element... elements) {
      // TODO: implement me
      
   }

   @Override
   public Name getName(CharSequence cs) {
      return new CoreReflectionName(cs.toString());
   }
   
   @Override
   public boolean isFunctionalInterface(TypeElement type) {
      // TODO: implement me
      return false;
   }

   @Override
   public TypeElement getTypeElement(Class<?> clazz) {
      return new CoreReflectionTypeElement(clazz);
   }

   @Override
   public PackageElement getPackageElement(Package pkg) {
      return new CoreReflectionPackageElement(pkg);
   }

   @Override
   public VariableElement getParameterElement(Parameter parameter) {
      return new CoreReflectionParameterElement(parameter);
   }

   @Override
   public VariableElement getFieldElement(Field field) {
      return new CoreReflectionFieldElement(field);
   }

   @Override
   public ExecutableElement getExecutableElement(Executable executable) {
      return new CoreReflectionExecutableElement(executable);
   }

   @Override
   public TypeParameterElement getTypeParameterElement(TypeVariable<?> typeVar) {
      return new CoreReflectionTypeParameterElement(typeVar);
   }
}
