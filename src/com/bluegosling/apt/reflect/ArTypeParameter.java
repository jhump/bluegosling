package com.bluegosling.apt.reflect;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

/**
 * A type parameter declaration. Type parameters can be declared on generic classes, interfaces,
 * methods, and constructors. This class represents the declaration of the type parameter. Uses of
 * it generic types are represented by {@link ArTypeVariable} (using similar terminology as
 * {@link TypeParameterElement} and {@link TypeVariable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.TypeVariable
 * @see TypeParameterElement
 *
 * @param <D> the type of element that declared the type parameter
 */
// TODO: doc
public class ArTypeParameter<D extends ArGenericDeclaration>
      extends ArAbstractAnnotatedElement<TypeParameterElement> {
   
   private ArTypeParameter(TypeParameterElement element) {
      super(element);
   }
   
   /**
    * Creates a type variable from the specified element.
    * 
    * @param element the type parameter element
    * @return a type variable
    * @throws NullPointerException if the specified element is null
    */
   public static ArTypeParameter<?> forElement(TypeParameterElement element) {
      return new ArTypeParameter<ArGenericDeclaration>(element);
   }
   
   public ArTypeVariable asType() {
      return ArTypeVariable.forTypeMirror(asTypeMirror());
   }
   
   public TypeVariable asTypeMirror() {
      return (TypeVariable) delegate().asType();
   }

   /**
    * Returns the upper bounds (aka "extends" bounds) for this type variable. For example in the
    * declaration {@code Interface<T extends SomeClass & Cloneable & Serializable>}, the type
    * variable {@code T} has three bounds: {@code SomeClass}, {@code Cloneable}, and
    * {@code Serializable}. If a type variable has no bounds defined then the implicit upper bound
    * is {@code java.lang.Object}.
    * 
    * @return the upper bounds for this type variable
    */
   public List<? extends ArType> getBounds() {
      List<? extends TypeMirror> elementBounds = delegate().getBounds();
      int size = elementBounds.size();
      List<ArType> bounds = new ArrayList<ArType>(size == 0 ? 1 : size);
      for (TypeMirror mirror : elementBounds) {
         bounds.add(ArTypes.forTypeMirror(mirror));
      }
      if (bounds.isEmpty()) {
         bounds.add(ArDeclaredType.forObject());
      }
      return bounds;
   }
   
   /**
    * Returns the declaration for this type variable. This will be either a {@code Class},
    * {@link ArMethod}, or {@link ArConstructor}, depending on where the type variable was declared.
    * 
    * <p>If the type variable is a {@linkplain Types#capture captured wildcard} then this method
    * returns {@code null}.
    * 
    * @return the generic declaration that declared this type variable
    */
   public D getGenericDeclaration() {
      @SuppressWarnings("unchecked")
      D decl =
         (D) ReflectionVisitors.GENERIC_DECLARATION_VISITOR.visit(delegate().getGenericElement());
      if (decl == null) {
         if (delegate().getKind() == ElementKind.OTHER
               && delegate().getSimpleName().toString().toLowerCase().contains("capture")) {
            // captured wildcard
            return null;
         }
         throw new AssertionError("Unable to determine generic declaration for type variable");
      }
      return decl;
   }
   
   /**
    * Returns the name of the type variable. For example, in an interface declared as
    * {@code Interface<T>}, the one type variable's name is {@code T}.
    * 
    * @return the name of the type variable
    */
   public String getName() {
      return delegate().getSimpleName().toString();
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof ArTypeParameter) {
         ArTypeParameter<?> other = (ArTypeParameter<?>) o;
         // Have to use getGenericDeclaration().toString() because a cycle (infinite
         // recursion) could occur if we just use getGenericDeclaration() since their equals() could
         // be defined in terms of their type variables. For example, Method's equals() is defined
         // in terms of its parameter types, which may well refer to a type variable. 
         return getName().equals(other.getName())
               && getBounds().equals(other.getBounds())
               && getGenericDeclaration().toString()
                     .equals(other.getGenericDeclaration().toString());
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      // Have to use getGenericDeclaration().toGenericString() because a cycle (infinite recursion)
      // could occur if we just use getGenericDeclaration() since their hashCode() could be defined
      // in terms of their type variables.
      return 31 * (31 * getName().hashCode() + getBounds().hashCode())
            + getGenericDeclaration().toString().hashCode();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getName());
      if (!delegate().getBounds().isEmpty()) {
         boolean first = true;
         for (ArType type : getBounds()) {
            if (first) {
               sb.append(" extends ");
               first = false;
            } else {
               sb.append("&");
            }
            sb.append(type.toString());
         }
      }
      return sb.toString();
   }

   static void appendTypeParameters(StringBuilder sb,
         List<? extends ArTypeParameter<?>> typeParameters) {
      if (!typeParameters.isEmpty()) {
         sb.append("<");
         boolean first = true;
         for (ArTypeParameter<?> typeParameter : typeParameters) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            sb.append(typeParameter.toString());
         }
         sb.append(">");
      }
   }
}
