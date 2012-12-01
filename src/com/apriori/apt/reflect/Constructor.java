package com.apriori.apt.reflect;

import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

/**
 * A constructor. This is analogous to {@link java.lang.reflect.Constructor
 * java.lang.reflect.Constructor}, except that it represents constructors
 * in Java source (during annotation processing) vs. representing constructors
 * of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.Constructor
 */
public class Constructor extends AbstractExecutableMember {

   private Constructor(ExecutableElement element) {
      super(element);
   }
   
   /**
    * Returns a constructor based on the specified element.
    * 
    * @param element the element
    * @return a constructor
    * @throws NullPointerException if the specified element is null
    * @throws IllegalArgumentException if the specified element does not represent a
    *       constructor
    */
   public static Constructor forElement(ExecutableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.CONSTRUCTOR) {
         throw new IllegalArgumentException("Invalid element kind. Expected CONSTRUCTOR; got " + element.getKind().name());
      }
      return new Constructor(element);
   }

   /**
    * Returns the constructor's list of type variables. This is the same as
    * {@link #getTypeVariables()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of type variables
    * 
    * @see #getTypeVariables()
    */
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<TypeVariable<Constructor>> getConstructorTypeVariables() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<Constructor<Method>>
      return (List<TypeVariable<Constructor>>) ((List) getTypeVariables());
   }

   /**
    * Returns the constructor's list of parameters. This is the same as
    * {@link #getParameters()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of parameters
    * 
    * @see #getParameters()
    */
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<Parameter<Constructor>> getConstructorParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<Parameter<Constructor>>
      return (List<Parameter<Constructor>>) ((List) getParameters());
   }

   /**
    * Returns the simple name of the declaring class, since that is how
    * constructors are "named" in Java source code.
    */
   @Override
   public String getName() {
      return getDeclaringClass().getSimpleName();
   }
   
   @Override 
   void appendReturnType(StringBuilder sb, boolean includeGenerics) {
      // Constructors have no return type
   }
}