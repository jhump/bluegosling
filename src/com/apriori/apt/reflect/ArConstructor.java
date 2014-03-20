package com.apriori.apt.reflect;

import java.lang.reflect.Constructor;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

/**
 * A constructor. This is analogous to {@link Constructor}, except that it represents constructors
 * in Java source (during annotation processing) vs. representing constructors of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Constructor
 */
public class ArConstructor extends ArAbstractExecutableMember {

   private ArConstructor(ExecutableElement element) {
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
   public static ArConstructor forElement(ExecutableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.CONSTRUCTOR) {
         throw new IllegalArgumentException("Invalid element kind. Expected CONSTRUCTOR; got "
               + element.getKind().name());
      }
      return new ArConstructor(element);
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
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public List<ArTypeVariable<ArConstructor>> getConstructorTypeVariables() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<ArTypeVariable<ArConstructor>>
      return ((List) getTypeVariables());
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
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public List<ArParameter<ArConstructor>> getConstructorParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<ArParameter<ArConstructor>>
      return ((List) getParameters());
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
