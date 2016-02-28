package com.bluegosling.apt.reflect;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * A type variable. Type variables are declared on generic classes and interfaces and then used in
 * type specifications in the class. For example, the class {@link ArrayList} has a type parameter
 * ({@code E}) and a method that is declared as {@code boolean add(E)}. In the method signature, the
 * generic type of the one parameter is a reference to the type variable. This is analogous to
 * {@link TypeVariable}, except that it represents types in Java source (during annotation
 * processing) vs. representing runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see TypeVariable
 */
public class ArTypeVariable<D extends ArGenericDeclaration> implements ArType {
   private final TypeParameterElement element;
   
   private ArTypeVariable(TypeParameterElement element) {
      if (element == null) {
         throw new NullPointerException();
      }
      this.element = element;
   }
   
   /**
    * Creates a type variable from the specified element.
    * 
    * @param element the type parameter element
    * @return a type variable
    * @throws NullPointerException if the specified element is null
    */
   public static ArTypeVariable<?> forElement(TypeParameterElement element) {
      return new ArTypeVariable<ArGenericDeclaration>(element);
   }
   
   /**
    * Creates a type variable from the specified type mirror.
    * 
    * @param typeMirror the type mirror
    * @return a wildcard type
    * @throws NullPointerException if the specified type mirror is null
    */
   public static ArTypeVariable<?> forTypeMirror(javax.lang.model.type.TypeVariable typeMirror) {
      ArTypeVariable<?> ret =
            ReflectionVisitors.TYPE_VARIABLE_VISITOR.visit(typeMirror.asElement());
      if (ret == null) {
         throw new MirroredTypeException(typeMirror);
      }
      return ret;
   }
   
   @Override
   public ArType.Kind getTypeKind() {
      return Kind.TYPE_VARIABLE;
   }
   
   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitTypeVariable(this,  p);
   }
   
   /**
    * Returns the underlying {@link Element} represented by this type.
    * 
    * @return the underlying element
    */
   public TypeParameterElement asElement() {
      return element;
   }
   
   @Override
   public TypeMirror asTypeMirror() {
      return element.asType();
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
      List<? extends TypeMirror> elementBounds = element.getBounds();
      int size = elementBounds.size();
      List<ArType> bounds = new ArrayList<ArType>(size == 0 ? 1 : size);
      for (TypeMirror mirror : elementBounds) {
         bounds.add(ArTypes.forTypeMirror(mirror));
      }
      if (bounds.isEmpty()) {
         bounds.add(ArClass.forObject());
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
         (D) ReflectionVisitors.GENERIC_DECLARATION_VISITOR.visit(element.getGenericElement());
      if (decl == null) {
         if (element.getKind() == ElementKind.OTHER
               && element.getSimpleName().toString().toLowerCase().contains("capture")) {
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
      return element.getSimpleName().toString();
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ArTypeVariable) {
         ArTypeVariable<?> other = (ArTypeVariable<?>) o;
         // Have to use getGenericDeclaration().toGenericString() because a cycle (infinite
         // recursion) could occur if we just use getGenericDeclaration() since their equals() could
         // be defined in terms of their type variables. For example, Method's equals() is defined
         // in terms of its parameter types, which may well refer to a type variable. 
         return getName().equals(other.getName())
               && getBounds().equals(other.getBounds())
               && getGenericDeclaration().toGenericString()
                     .equals(other.getGenericDeclaration().toGenericString());
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      // Have to use getGenericDeclaration().toGenericString() because a cycle (infinite recursion)
      // could occur if we just use getGenericDeclaration() since their hashCode() could be defined
      // in terms of their type variables.
      return 31 * (31 * getName().hashCode() + getBounds().hashCode())
            + getGenericDeclaration().toGenericString().hashCode();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getName());
      if (!element.getBounds().isEmpty()) {
         boolean first = true;
         for (ArType type : getBounds()) {
            if (first) {
               sb.append(" extends ");
               first = false;
            } else {
               sb.append("&");
            }
            sb.append(type.toTypeString());
         }
      }
      return sb.toString();
   }

   @Override
   public String toTypeString() {
      return getName();
   }
   
   static void appendTypeParameters(StringBuilder sb, List<ArTypeVariable<?>> typeVariables) {
      if (!typeVariables.isEmpty()) {
         sb.append("<");
         boolean first = true;
         for (ArTypeVariable<?> typeVariable : typeVariables) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            sb.append(typeVariable.toString());
         }
         sb.append(">");
      }
   }
}
