package com.apriori.reflect.model;

import java.io.Writer;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;


class ProcessingEnvironmentElements implements Elements {
   private final javax.lang.model.util.Elements base;
   private final Types typeUtils;
   
   ProcessingEnvironmentElements(ProcessingEnvironment env) {
      this.base = env.getElementUtils();
      this.typeUtils = new ProcessingEnvironmentTypes(env.getTypeUtils(), this);
   }
   
   ProcessingEnvironmentElements(javax.lang.model.util.Elements base, Types typeUtils) {
      this.base = base;
      this.typeUtils = typeUtils;
   }
   
   Types getTypeUtils() {
      return typeUtils;
   }

   @Override
   public PackageElement getPackageElement(CharSequence name) {
      return base.getPackageElement(name);
   }

   @Override
   public TypeElement getTypeElement(CharSequence name) {
      return base.getTypeElement(name);
   }

   @Override
   public Map<? extends ExecutableElement, ? extends AnnotationValue>
   getElementValuesWithDefaults(AnnotationMirror a) {
      return base.getElementValuesWithDefaults(a);
   }

   @Override
   public String getDocComment(Element e) {
      return base.getDocComment(e);
   }

   @Override
   public boolean isDeprecated(Element e) {
      return base.isDeprecated(e);
   }

   @Override
   public Name getBinaryName(TypeElement type) {
      return base.getBinaryName(type);
   }

   @Override
   public PackageElement getPackageOf(Element type) {
      return base.getPackageOf(type);
   }

   @Override
   public List<? extends Element> getAllMembers(TypeElement type) {
      return base.getAllMembers(type);
   }

   @Override
   public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
      return base.getAllAnnotationMirrors(e);
   }

   @Override
   public boolean hides(Element hider, Element hidden) {
      return base.hides(hider, hidden);
   }

   @Override
   public boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
         TypeElement type) {
      return base.overrides(overrider,  overridden, type);
   }

   @Override
   public String getConstantExpression(Object value) {
      return base.getConstantExpression(value);
   }

   @Override
   public void printElements(Writer w, Element... elements) {
      base.printElements(w, elements);
   }

   @Override
   public Name getName(CharSequence cs) {
      return base.getName(cs);
   }

   @Override
   public boolean isFunctionalInterface(TypeElement type) {
      return base.isFunctionalInterface(type);
   }

   @Override
   public TypeElement getTypeElement(Class<?> clazz) {
      // TODO: implement me
      return null;
   }

   @Override
   public PackageElement getPackageElement(Package pkg) {
      // TODO: implement me
      return null;
   }

   @Override
   public VariableElement getParameterElement(Parameter parameter) {
      // TODO: implement me
      return null;
   }

   @Override
   public VariableElement getFieldElement(Field field) {
      // TODO: implement me
      return null;
   }

   @Override
   public ExecutableElement getExecutableElement(Executable executable) {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeParameterElement getTypeParameterElement(TypeVariable<?> typeVar) {
      // TODO: implement me
      return null;
   }
}
