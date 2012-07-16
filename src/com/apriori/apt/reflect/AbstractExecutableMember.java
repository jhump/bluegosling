package com.apriori.apt.reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * An abstract base class for implementations of {@link ExecutableMember}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractExecutableMember extends AbstractMember implements GenericDeclaration, ExecutableMember {

   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected AbstractExecutableMember(ExecutableElement element) {
      super(element);
   }
   
   @Override
   public ExecutableElement asElement() {
      return (ExecutableElement) super.asElement();
   }

   @Override
   public List<TypeVariable<?>> getTypeVariables() {
      List<? extends TypeParameterElement> parameters = asElement().getTypeParameters();
      List<TypeVariable<?>> ret = new ArrayList<TypeVariable<?>>(parameters.size());
      for (TypeParameterElement parameter : parameters) {
         ret.add(TypeVariable.forElement(parameter));
      }
      return Collections.unmodifiableList(ret);
   }
  
   @Override
   public List<Parameter<?>> getParameters() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<Parameter<?>> ret = new ArrayList<Parameter<?>>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(Parameter.forElement(parameter));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public List<Class> getParameterTypes() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<Class> ret = new ArrayList<Class>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(Class.forTypeMirror(parameter.asType()));
      }
      return Collections.unmodifiableList(ret);
   }

   @Override
   public List<Type> getGenericParameterTypes() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<Type> ret = new ArrayList<Type>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(Types.forTypeMirror(parameter.asType()));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public boolean isVarArgs() {
      return asElement().isVarArgs();
   }
   
   @Override
   public List<List<Annotation>> getParameterAnnotations() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<List<Annotation>> ret = new ArrayList<List<Annotation>>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(AbstractAnnotatedElement.toAnnotations(parameter.getAnnotationMirrors()));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public List<Class> getExceptionTypes() {
      List<? extends TypeMirror> exceptions = asElement().getThrownTypes();
      List<Class> ret = new ArrayList<Class>(exceptions.size());
      for (TypeMirror exception : exceptions) {
         ret.add(Class.forTypeMirror(exception));
      }
      return Collections.unmodifiableList(ret);
   }

   @Override
   public List<Type> getGenericExceptionTypes() {
      List<? extends TypeMirror> exceptions = asElement().getThrownTypes();
      List<Type> ret = new ArrayList<Type>(exceptions.size());
      for (TypeMirror exception : exceptions) {
         ret.add(Types.forTypeMirror(exception));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public boolean equals(Object o) {
      if (getClass().isInstance(o)) {
         AbstractExecutableMember other = (AbstractExecutableMember) o;
         return getDeclaringClass().equals(other.getDeclaringClass())
               && getName().equals(other.getName())
               && getGenericParameterTypes().equals(other.getGenericParameterTypes());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * (29 * getDeclaringClass().hashCode() + getName().hashCode())
            ^ getGenericParameterTypes().hashCode();
   }

   @Override
   public String toString() {
      return toString(false);
   }
   
   @Override
   public String toGenericString() {
      return toString(true);
   }
   
   abstract void appendReturnType(StringBuilder sb, boolean includeGenerics);
   
   private String toString(boolean includeGenerics) {
      StringBuilder sb = new StringBuilder();
      Modifier.appendModifiers(sb, getModifiers());
      if (includeGenerics) {
         TypeVariable.appendTypeParameters(sb, getTypeVariables());
         sb.append(" ");
      }
      appendReturnType(sb, includeGenerics);
      sb.append(getName());
      sb.append("(");
      List<? extends Type> parameters = includeGenerics ? getGenericParameterTypes() : getParameterTypes();
      boolean first = true;
      for (Type type : parameters) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(type.toTypeString());
      }
      sb.append(")");
      List<? extends Type> exceptions = includeGenerics ? getGenericExceptionTypes() : getExceptionTypes();
      if (!exceptions.isEmpty()) {
         sb.append(" throws ");
         first = true;
         for (Type type : exceptions) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            sb.append(type.toTypeString());
         }
      }
      return sb.toString();
   }
}
