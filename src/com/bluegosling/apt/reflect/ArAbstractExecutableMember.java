package com.bluegosling.apt.reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * An abstract base class for implementations of {@link ArExecutableMember}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class ArAbstractExecutableMember extends ArAbstractMember implements ArGenericDeclaration, ArExecutableMember {

   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected ArAbstractExecutableMember(ExecutableElement element) {
      super(element);
   }
   
   @Override
   public ExecutableElement asElement() {
      return (ExecutableElement) super.asElement();
   }

   @Override
   public List<ArTypeVariable<?>> getTypeVariables() {
      List<? extends TypeParameterElement> parameters = asElement().getTypeParameters();
      List<ArTypeVariable<?>> ret = new ArrayList<ArTypeVariable<?>>(parameters.size());
      for (TypeParameterElement parameter : parameters) {
         ret.add(ArTypeVariable.forElement(parameter));
      }
      return Collections.unmodifiableList(ret);
   }
  
   @Override
   public List<ArParameter<?>> getParameters() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<ArParameter<?>> ret = new ArrayList<ArParameter<?>>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(ArParameter.forElement(parameter));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public List<ArClass> getParameterTypes() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<ArClass> ret = new ArrayList<ArClass>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(ArClass.forTypeMirror(parameter.asType()));
      }
      return Collections.unmodifiableList(ret);
   }

   @Override
   public List<ArType> getGenericParameterTypes() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<ArType> ret = new ArrayList<ArType>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(ArTypes.forTypeMirror(parameter.asType()));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public boolean isVarArgs() {
      return asElement().isVarArgs();
   }
   
   @Override
   public List<List<ArAnnotation>> getParameterAnnotations() {
      List<? extends VariableElement> parameters = asElement().getParameters();
      List<List<ArAnnotation>> ret = new ArrayList<List<ArAnnotation>>(parameters.size());
      for (VariableElement parameter : parameters) {
         ret.add(ArAbstractAnnotatedElement.toAnnotations(parameter.getAnnotationMirrors()));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public List<ArClass> getExceptionTypes() {
      List<? extends TypeMirror> exceptions = asElement().getThrownTypes();
      List<ArClass> ret = new ArrayList<ArClass>(exceptions.size());
      for (TypeMirror exception : exceptions) {
         ret.add(ArClass.forTypeMirror(exception));
      }
      return Collections.unmodifiableList(ret);
   }

   @Override
   public List<ArType> getGenericExceptionTypes() {
      List<? extends TypeMirror> exceptions = asElement().getThrownTypes();
      List<ArType> ret = new ArrayList<ArType>(exceptions.size());
      for (TypeMirror exception : exceptions) {
         ret.add(ArTypes.forTypeMirror(exception));
      }
      return Collections.unmodifiableList(ret);
   }
   
   @Override
   public boolean equals(Object o) {
      if (getClass().isInstance(o)) {
         ArAbstractExecutableMember other = (ArAbstractExecutableMember) o;
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
   
   /**
    * Allows sub-classes to hook in an optional return type into the output of {@link #toString()}
    * and {@link #toGenericString()}.
    * 
    * @param sb the target for appending the return type's string representation
    * @param includeGenerics whether or not the string representation should include generic type
    *       information about the return type (if false, just use erased type)
    */
   abstract void appendReturnType(StringBuilder sb, boolean includeGenerics);
   
   private String toString(boolean includeGenerics) {
      StringBuilder sb = new StringBuilder();
      ArModifier.appendModifiers(sb, getModifiers());
      if (includeGenerics) {
         int l = sb.length();
         ArTypeVariable.appendTypeParameters(sb, getTypeVariables());
         if (sb.length() != l) {
            sb.append(" ");
         }
      }
      int l = sb.length();
      appendReturnType(sb, includeGenerics);
      if (sb.length() != l) {
         sb.append(" ");
      }
      sb.append(getName());
      sb.append("(");
      List<ArParameter<?>> parameters = getParameters();
      boolean first = true;
      for (ArParameter<?> parameter : parameters) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(includeGenerics ? parameter.toGenericString() : parameter.toString());
      }
      sb.append(")");
      List<? extends ArType> exceptions = includeGenerics ? getGenericExceptionTypes() : getExceptionTypes();
      if (!exceptions.isEmpty()) {
         sb.append(" throws ");
         first = true;
         for (ArType type : exceptions) {
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
