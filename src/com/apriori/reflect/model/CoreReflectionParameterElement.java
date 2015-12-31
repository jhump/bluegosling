package com.apriori.reflect.model;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link VariableElement} backed by a core reflection {@link Parameter}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionParameterElement extends CoreReflectionBaseElement<Parameter>
implements VariableElement {
   
   CoreReflectionParameterElement(Parameter param) {
      super(param, param.getName());
   }
   
   @Override
   public TypeMirror asType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedType());
   }

   @Override
   public ElementKind getKind() {
      return ElementKind.PARAMETER;
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
      return CoreReflectionElements.INSTANCE.getExecutableElement(base().getDeclaringExecutable());
   }
}
