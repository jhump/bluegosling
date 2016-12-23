package com.bluegosling.apt.reflect;

import java.lang.reflect.Parameter;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * A representation of a parameter to an executable member, like a constructor or method. This is
 * analogous to {@link Parameter}, except that it represents parameters in Java source (during
 * annotation processing) vs. representing parameters in runtime types.
 * 
 * <p>Unlike reflection-based parameters, there is no way to determine if the parameter name is
 * synthesized. For types whose source code is being processed, the names are always available. But,
 * for elements that represent types on the compiler's class path, parameter names may not be
 * available and instead be synthesized.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <M> the type of executable member (constructor or method) to which the parameter belongs
 * 
 * @see Parameter
 * @see VariableElement
 */
public class ArParameter<M extends ArExecutableMember>
      extends ArAbstractAnnotatedElement<VariableElement> {

   private ArParameter(VariableElement element) {
      super(element);
   }
   
   /**
    * Returns a parameter based on the specified element.
    * 
    * @param element the element
    * @return a parameter
    * @throws NullPointerException if the specified element is null
    * @throws IllegalArgumentException if the specified element does not represent a
    *       parameter
    */
   public static ArParameter<?> forElement(VariableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.PARAMETER) {
         throw new IllegalArgumentException("Invalid element kind. Expected PARAMETER; got "
               + element.getKind().name());
      }
      return new ArParameter<ArExecutableMember>(element);
   }
   

   /**
    * Returns a method parameter based on the specified element.
    * 
    * @param element the element
    * @return a method parameter
    * @throws NullPointerException if the specified element is null
    * @throws IllegalArgumentException if the specified element does not represent a
    *       parameter of a method
    */
   public static ArParameter<ArMethod> forMethodParameterElement(VariableElement element) {
      @SuppressWarnings("unchecked") // we check the type before letting this ref escape
      ArParameter<ArMethod> param = (ArParameter<ArMethod>) forElement(element);
      ExecutableElement parent = (ExecutableElement) element.getEnclosingElement();
      if (parent.getKind() != ElementKind.METHOD) {
         throw new IllegalArgumentException("Invalid enclosing kind. Expected METHOD; got "
               + parent.getKind().name());
      }
      return param;
   }

   /**
    * Returns a constructor parameter based on the specified element.
    * 
    * @param element the element
    * @return a constructor parameter
    * @throws NullPointerException if the specified element is null
    * @throws IllegalArgumentException if the specified element does not represent a
    *       parameter of a constructor
    */
   public static ArParameter<ArConstructor> forConstructorParameterElement(
         VariableElement element) {
      @SuppressWarnings("unchecked") // we'll check the type before letting this ref escape
      ArParameter<ArConstructor> param = (ArParameter<ArConstructor>) forElement(element);
      ExecutableElement parent = (ExecutableElement) element.getEnclosingElement();
      if (parent.getKind() != ElementKind.CONSTRUCTOR) {
         throw new IllegalArgumentException("Invalid enclosing kind. Expected CONSTRUCTOR; got "
               + parent.getKind().name());
      }
      return param;
   }

   @Override
   public VariableElement asElement() {
      return (VariableElement) super.asElement();
   }
   
   /**
    * Returns the parameter name.
    * 
    * @return the name of the parameter
    */
   public String getName() {
      return asElement().getSimpleName().toString();
   }
   
   /**
    * Returns the type of this parameter. This is a raw type with generic type information erased.
    * For full generic type information, use {@link #getGenericType()}.
    * 
    * @return the type of the parameter
    */
   public ArClass getType() {
      return ArClass.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns the type of this parameter. This includes all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the type of the parameter
    */
   public ArType getGenericType() {
      return ArTypes.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns the constructor or method to which this parameter belongs.
    * 
    * @return the executable member to which the parameter belongs
    */
   public M getEnclosingMember() {
      @SuppressWarnings("unchecked")
      M ret =
         (M) ReflectionVisitors.EXECUTABLE_MEMBER_VISITOR.visit(asElement().getEnclosingElement());
      if (ret == null) {
         throw new AssertionError("Unable to determine enclosing member for parameter");
      }
      return ret;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ArParameter) {
         ArParameter<?> other = (ArParameter<?>) o;
         return getEnclosingMember().equals(other.getEnclosingMember())
               && getName().equals(other.getName())
               && getGenericType().equals(other.getGenericType());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * (29 * getEnclosingMember().hashCode() + getName().hashCode())
            ^ getGenericType().hashCode();
   }

   @Override
   public String toString() {
      return getGenericType().toString() + " " + getName();
   }
}
