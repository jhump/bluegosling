package com.apriori.reflect.model;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;


class CoreReflectionConstructorElement extends CoreReflectionBaseElement
implements ExecutableElement {

   CoreReflectionConstructorElement(Constructor<?> ctor) {
      super(ctor, "<init>");
   }
   
   @Override
   protected Constructor<?> base() {
      return (Constructor<?>) super.base();
   }

   @Override
   public TypeMirror asType() {
      // TODO: implement me
      return null;
   }

   @Override
   public ElementKind getKind() {
      // TODO: implement me
      return null;
   }

   @Override
   public Set<Modifier> getModifiers() {
      // TODO: implement me
      return null;
   }

   @Override
   public Element getEnclosingElement() {
      // TODO: implement me
      return null;
   }

   @Override
   public List<? extends Element> getEnclosedElements() {
      // TODO: implement me
      return null;
   }

   @Override
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      // TODO: implement me
      return null;
   }

   @Override
   public List<? extends TypeParameterElement> getTypeParameters() {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeMirror getReturnType() {
      // TODO: implement me
      return null;
   }

   @Override
   public List<? extends VariableElement> getParameters() {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeMirror getReceiverType() {
      // TODO: implement me
      return null;
   }

   @Override
   public boolean isVarArgs() {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean isDefault() {
      // TODO: implement me
      return false;
   }

   @Override
   public List<? extends TypeMirror> getThrownTypes() {
      // TODO: implement me
      return null;
   }

   @Override
   public AnnotationValue getDefaultValue() {
      // TODO: implement me
      return null;
   }

}
