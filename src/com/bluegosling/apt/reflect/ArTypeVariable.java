package com.bluegosling.apt.reflect;

import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.TypeVariable;

import javax.lang.model.element.TypeParameterElement;

/**
 * A type variable, or a use of a type parameter. Type parameters are declared on generic classes
 * and interfaces (methods, too) and then used in type specifications in that class (or method). For
 * example, the class {@code ArrayList} has a type parameter, {@code E}, and a method that is
 * declared as {@code boolean add(E)}. In the method signature, the generic type of the single
 * parameter is a type variable that refers to the type parameter {@code E}. This class is analogous
 * to {@link AnnotatedTypeVariable}, except that it represents types in Java source (during
 * annotation processing) vs. representing runtime types. In some ways it is like
 * {@link TypeVariable} except that this class represents <em>use</em> of a type variable and not
 * its original declaration (the so-called {@linkplain ArTypeParameter type parameter}).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see TypeVariable
 * @see AnnotatedTypeVariable
 * @see javax.lang.model.type.TypeVariable
 */
public class ArTypeVariable extends ArType {

   private ArTypeVariable(javax.lang.model.type.TypeVariable typeMirror) {
      super(typeMirror);
   }
   
   /**
    * Creates a type variable from the specified type mirror.
    * 
    * @param typeMirror the type mirror
    * @return a wildcard type
    * @throws NullPointerException if the specified type mirror is null
    */
   public static ArTypeVariable forTypeMirror(javax.lang.model.type.TypeVariable typeMirror) {
      return new ArTypeVariable(typeMirror);
   }
   
   @Override
   public ArType.Kind getTypeKind() {
      return Kind.TYPE_VARIABLE;
   }
   
   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitTypeVariable(this,  p);
   }
   
   public ArTypeParameter<?> asTypeParameter() {
      TypeParameterElement element = (TypeParameterElement) asTypeMirror().asElement();
      return ArTypeParameter.forElement(element);
   }
   
   @Override
   public javax.lang.model.type.TypeVariable asTypeMirror() {
      return (javax.lang.model.type.TypeVariable) delegate();
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ArTypeVariable) {
         ArTypeVariable other = (ArTypeVariable) o;
         return asTypeParameter().equals(other.asTypeParameter());
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      // Have to use getGenericDeclaration().toGenericString() because a cycle (infinite recursion)
      // could occur if we just use getGenericDeclaration() since their hashCode() could be defined
      // in terms of their type variables.
      return asTypeParameter().hashCode();
   }

   @Override
   public String toString() {
      return asTypeParameter().getName();
   }
}
