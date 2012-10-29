package com.apriori.apt.reflect;

import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

/**
 * A method. This is analogous to {@link java.lang.reflect.Method
 * java.lang.reflect.Method}, except that it represents methods
 * in Java source (during annotation processing) vs. representing methods
 * of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.Method
 */
public class Method extends AbstractExecutableMember {

   private Method(ExecutableElement element) {
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
   public static Method forElement(ExecutableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.METHOD) {
         throw new IllegalArgumentException("Invalid element kind. Expected METHOD; got "
               + element.getKind().name());
      }
      return new Method(element);
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
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<TypeVariable<Method>> getMethodTypeVariables() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<TypeVariable<Method>>
      return (List<TypeVariable<Method>>) ((List) getTypeVariables());
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
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<Parameter<Method>> getMethodParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<Parameter<Method>>
      return (List<Parameter<Method>>) ((List) getParameters());
   }
   
   /**
    * Returns the method's return type. The returned type is a raw type with any generic type
    * information erased. For full type information, use {@link #getGenericReturnType()}.
    * 
    * @return the method's return type
    * 
    * @see java.lang.reflect.Method#getReturnType()
    */
   public Class getReturnType() {
      return Class.forTypeMirror(asElement().getReturnType());
   }

   /**
    * Returns the method's return type. The returned type includes all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the method's return type
    * 
    * @see java.lang.reflect.Method#getGenericReturnType()
    */
   public Type getGenericReturnType() {
      return Types.forTypeMirror(asElement().getReturnType());
   }
   
   @Override 
   void appendReturnType(StringBuilder sb, boolean includeGenerics) {
      Type returnType = includeGenerics ? getGenericReturnType() : getReturnType();
      sb.append(returnType.toTypeString());
      sb.append(" ");
   }
}
