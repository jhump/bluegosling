package com.apriori.reflect.model;


import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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

/**
 * An {@link ExecutableElement} backed by a core reflection {@link Executable}. This represents
 * either a method or a constructor.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionExecutableElement extends CoreReflectionBaseElement<Executable>
implements ExecutableElement {

   CoreReflectionExecutableElement(Executable executable) {
      super(executable, executable instanceof Constructor ? "<init>" : executable.getName());
   }
   
   @Override
   public TypeMirror asType() {
      return new CoreReflectionExecutableType(base());
   }

   @Override
   public Set<Modifier> getModifiers() {
      EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);
      if (isDefault()) {
         result.add(Modifier.DEFAULT);
      }
      CoreReflectionModifiers.addToSet(base().getModifiers(), result);
      return Collections.unmodifiableSet(result);
   }

   @Override
   public Element getEnclosingElement() {
      return CoreReflectionElements.INSTANCE.getTypeElement(base().getDeclaringClass());
   }

   @Override
   public List<? extends Element> getEnclosedElements() {
      return Collections.emptyList();
   }

   @Override
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitExecutable(this, p);
   }

   @Override
   public ElementKind getKind() {
      return base() instanceof Constructor ? ElementKind.CONSTRUCTOR : ElementKind.METHOD;
   }

   @Override
   public List<? extends TypeParameterElement> getTypeParameters() {
      TypeVariable<?>[] typeVars = base().getTypeParameters();
      List<TypeParameterElement> result = new ArrayList<>(typeVars.length);
      for (TypeVariable<?> typeVar : typeVars) {
         result.add(CoreReflectionElements.INSTANCE.getTypeParameterElement(typeVar));
      }
      return result;
   }

   @Override
   public TypeMirror getReturnType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedReturnType());
   }

   @Override
   public List<? extends VariableElement> getParameters() {
      Parameter[] params = base().getParameters();
      List<VariableElement> result = new ArrayList<>(params.length);
      for (Parameter param : params) {
         result.add(CoreReflectionElements.INSTANCE.getParameterElement(param));
      }
      return result;
   }

   @Override
   public TypeMirror getReceiverType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedReceiverType());
   }

   @Override
   public boolean isVarArgs() {
      return base().isVarArgs();
   }

   @Override
   public boolean isDefault() {
      return base() instanceof Method && ((Method) base()).isDefault();
   }

   @Override
   public List<? extends TypeMirror> getThrownTypes() {
      AnnotatedType[] exTypes = base().getAnnotatedExceptionTypes();
      List<TypeMirror> result = new ArrayList<>(exTypes.length);
      for (AnnotatedType type : exTypes) {
         result.add(CoreReflectionTypes.INSTANCE.getTypeMirror(type));
      }
      return result;
   }

   @Override
   public AnnotationValue getDefaultValue() {
      Object v = base() instanceof Method
            ? ((Method) base()).getDefaultValue()
            : null;
      return v == null ? null : AnnotationMirrors.CORE_REFLECTION_INSTANCE.getAnnotationValue(v);
   }
}
