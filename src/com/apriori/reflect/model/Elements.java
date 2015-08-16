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
 * @see #fromProcessingEnvironment(ProcessingEnvironment)
 * @see #fromCoreReflection()
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Elements extends javax.lang.model.util.Elements {
   /**
    * Returns a type element for the given class token.
    *
    * @param clazz a class
    * @return a type element for the given class
    */
   TypeElement getTypeElement(Class<?> clazz);

   /**
    * Returns a package element for the given package.
    *
    * @param pkg a package
    * @return a package element for the given package
    */
   PackageElement getPackageElement(Package pkg);
   
   /**
    * Returns a variable element for the given parameter.
    *
    * @param parameter a parameter
    * @return a variable element for the given parameter
    */
   VariableElement getParameterElement(Parameter parameter);
   
   /**
    * Returns a variable element for the given field.
    *
    * @param field a field
    * @return a variable element for the given field
    */
   VariableElement getFieldElement(Field field);
   
   /**
    * Returns a variable element for the given enum constant.
    *
    * @param field a field
    * @return a variable element for the given enum constant
    */
   default VariableElement getEnumConstantElement(Enum<?> en) {
      Field f;
      try {
         f = en.getDeclaringClass().getField(en.name());
      } catch (NoSuchFieldException e) {
         throw new AssertionError(e);
      }
      return getFieldElement(f);
   }
   
   /**
    * Returns an executable element for the given executable.
    *
    * @param executable an executable (either a method or a constructor)
    * @return an executable element for the given executable
    */
   ExecutableElement getExecutableElement(Executable executable);

   /**
    * Returns an executable element for the given executable.
    *
    * @param executable an executable
    * @return an executable element for the given executable
    */
   TypeParameterElement getTypeParameterElement(TypeVariable<?> typeVar);
   
   /**
    * Returns an {@link Elements} utility class that is backed by an annotation processing
    * environment.
    *
    * @param env an annotation processing environment
    * @return an {@link Elements} utility class backed by the given environment
    */
   static Elements fromProcessingEnvironment(ProcessingEnvironment env) {
      javax.lang.model.util.Elements base = env.getElementUtils();
      if (base instanceof Elements) {
         return (Elements) base;
      }
      return new ProcessingEnvironmentElements(env);
   }
   
   /**
    * Returns an {@link Elements} utility class that is backed by core reflection.
    *
    * @return an {@link Elements} utility class backed by core reflection
    */
   static Elements fromCoreReflection() {
      return CoreReflectionElements.INSTANCE;
   }
}
