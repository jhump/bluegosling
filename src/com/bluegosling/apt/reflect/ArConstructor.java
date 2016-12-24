package com.bluegosling.apt.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A constructor. This is analogous to {@link Constructor}, except that it represents constructors
 * in Java source (during annotation processing) vs. representing constructors of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Constructor
 * @see ExecutableElement
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
    * {@link #getTypeParameters()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of type variables
    */
   @Override
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public List<ArTypeParameter<ArConstructor>> getTypeParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<ArTypeVariable<ArConstructor>>
      return (List) super.getTypeParameters();
   }

   /**
    * Returns the constructor's list of parameters. This is the same as
    * {@link #getParameters()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of parameters
    */
   @Override
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public List<ArParameter<ArConstructor>> getParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<ArParameter<ArConstructor>>
      return (List) super.getParameters();
   }
   
   /**
    * Returns the constructor's implicit receiver type or {@code null} if the constructor is for a
    * static or top-level type. The receiver of a constructor is the implicit reference to the
    * enclosing type, for a non-static nested class. So its type is always that of the enclosing
    * class. This method can be used to extract type annotations that are declared for the receiver.
    * 
    * @return the constructor's receiver type
    * 
    * @see Constructor#getAnnotatedReceiverType()
    * @see ExecutableElement#getReceiverType()
    */
   public ArType getReceiverType() {
      TypeMirror receiver = asElement().getReceiverType();
      return receiver.getKind() == TypeKind.NONE ? null : ArTypes.forTypeMirror(receiver);
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
   void appendReturnType(StringBuilder sb) {
      // Constructors have no return type
   }
}
