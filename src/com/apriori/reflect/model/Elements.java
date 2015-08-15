package com.apriori.reflect.model;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * An extension of the standard {@link javax.lang.model.util.Elements} interface that provides
 * methods for inter-op with core reflection. For example, there are methods for querying for an
 * {@link javax.lang.model.element.Element} corresponding to a {@link #getTypeElement(Class)
 * java.lang.Class} or a {@link #getFieldElement(Field) java.lang.reflect.Field}.
 * 
 * <p>This interface provides factory methods to return instances backed by an
 * {@linkplain #fromProcessingEnvironment annotation processing environment} or by
 * {@linkplain #fromCoreReflection core reflection}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Elements extends javax.lang.model.util.Elements {
   TypeElement getTypeElement(Class<?> clazz);
   PackageElement getPackageElement(Package pkg);
   VariableElement getParameterElement(Parameter parameter);
   VariableElement getFieldElement(Field field);
   
   default VariableElement getEnumConstantElement(Enum<?> en) {
      Field f;
      try {
         f = en.getDeclaringClass().getField(en.name());
      } catch (NoSuchFieldException e) {
         throw new AssertionError(e);
      }
      return getFieldElement(f);
   }
   
   ExecutableElement getExecutableElement(Executable element);
   TypeParameterElement getTypeParameterElement(TypeVariable<?> typeVar);
   
   static Elements fromProcessingEnvironment(ProcessingEnvironment env) {
      javax.lang.model.util.Elements base = env.getElementUtils();
      if (base instanceof Elements) {
         return (Elements) base;
      }
      return new ProcessingEnvironmentElements(env);
   }
   
   static Elements fromCoreReflection() {
      return CoreReflectionElements.INSTANCE;
   }
}
