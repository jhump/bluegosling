package com.bluegosling.apt.reflect;

import java.lang.reflect.Method;
import java.util.List;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

/**
 * A method. This is analogous to {@link Method}, except that it represents methods in Java source
 * (during annotation processing) vs. representing methods of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Method
 */
public class ArMethod extends ArAbstractExecutableMember {

   private ArMethod(ExecutableElement element) {
      super(element);
   }
   
   /**
    * Returns a method based on the specified element.
    * 
    * @param element the element
    * @return a method
    * @throws NullPointerException if the specified element is null
    * @throws IllegalArgumentException if the specified element does not represent a
    *       method
    */
   public static ArMethod forElement(ExecutableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.METHOD) {
         throw new IllegalArgumentException("Invalid element kind. Expected METHOD; got "
               + element.getKind().name());
      }
      return new ArMethod(element);
   }

   /**
    * Returns the method's list of type variables. This is the same as
    * {@link #getTypeVariables()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of type variables
    * 
    * @see #getTypeVariables()
    */
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public List<ArTypeVariable<ArMethod>> getMethodTypeVariables() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<ArTypeVariable<ArMethod>>
      return ((List) getTypeVariables());
   }
   
   /**
    * Returns the method's list of parameters. This is the same as
    * {@link #getParameters()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of parameters
    * 
    * @see #getParameters()
    */
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public List<ArParameter<ArMethod>> getMethodParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<ArParameter<ArMethod>>
      return ((List) getParameters());
   }
   
   /**
    * Returns the method's return type. The returned type is a raw type with any generic type
    * information erased. For full type information, use {@link #getGenericReturnType()}.
    * 
    * @return the method's return type
    * 
    * @see java.lang.reflect.Method#getReturnType()
    */
   public ArClass getReturnType() {
      return ArClass.forTypeMirror(asElement().getReturnType());
   }

   /**
    * Returns the method's return type. The returned type includes all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the method's return type
    * 
    * @see java.lang.reflect.Method#getGenericReturnType()
    */
   public ArType getGenericReturnType() {
      return ArTypes.forTypeMirror(asElement().getReturnType());
   }
   
   /**
    * Returns the default value for an annotation method. If this method is not an
    * annotation method then {@code null} is returned.
    * 
    * <p>The type of the value is the same as found in the map returned by
    * {@link ArAnnotation#getAnnotationAttributes()}.
    * 
    * @return the default value for this annotation method or {@code null} if this is
    *       not an annotation method
    */
   public Object getDefaultValue() {
      AnnotationValue val = asElement().getDefaultValue();
      return val == null ? null : ReflectionVisitors.ANNOTATION_VALUE_VISITOR.visit(val);
   }
   
   @Override 
   void appendReturnType(StringBuilder sb, boolean includeGenerics) {
      ArType returnType = includeGenerics ? getGenericReturnType() : getReturnType();
      sb.append(returnType.toTypeString());
   }
}