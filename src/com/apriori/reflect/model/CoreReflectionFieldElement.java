package com.apriori.reflect.model;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;


class CoreReflectionFieldElement extends CoreReflectionBaseElement implements VariableElement {
   
   CoreReflectionFieldElement(Field field) {
      super(field, field.getName());
   }
   
   @Override
   protected Field base() {
      return (Field) super.base();
   }

   @Override
   public TypeMirror asType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedType());
   }

   @Override
   public ElementKind getKind() {
      return base().isEnumConstant() ? ElementKind.ENUM_CONSTANT : ElementKind.FIELD;
   }

   @Override
   public Set<Modifier> getModifiers() {
      return CoreReflectionModifiers.asSet(base().getModifiers());
   }

   @Override
   public List<? extends Element> getEnclosedElements() {
      return Collections.emptyList();
   }

   @Override
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitVariable(this, p);
   }

   @Override
   public Object getConstantValue() {
      return null;
   }

   @Override
   public Element getEnclosingElement() {
      return CoreReflectionElements.INSTANCE.getTypeElement(base().getDeclaringClass());
   }
}
