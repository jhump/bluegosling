package com.apriori.apt.reflect;

import java.util.EnumSet;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

/**
 * A field. This is analogous to {@link java.lang.reflect.Field java.lang.reflect.Field},
 * except that it represents fields in Java source (during annotation processing) vs.
 * representing fields of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.Field
 */
public class Field extends AbstractMember {

   private static final EnumSet<ElementKind> ALLOWED_KINDS = EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT);
   
   private Field(VariableElement element) {
      super(element);
   }
   
   /**
    * Returns a field based on the specified element.
    * 
    * @param element the element
    * @return a field
    * @throws NullPointerException if the specified element is null
    * @throws IllegalArgumentException if the specified element does not represent a field or
    *       enum constant
    */
   public static Field forElement(VariableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (!ALLOWED_KINDS.contains(element.getKind())) {
         throw new IllegalArgumentException("Invalid element kind. Expected one of "
               + ALLOWED_KINDS + "; got " + element.getKind().name());
      }
      return new Field(element);
   }
   
   @Override
   public VariableElement asElement() {
      return (VariableElement) super.asElement();
   }
   
   /**
    * Returns the type of this field. This is a raw type with generic type information erased. For
    * full generic type information, use {@link #getGenericType()}.
    * 
    * @return the type of the field
    * 
    * @see java.lang.reflect.Field#getType()
    */
   public Class getType() {
      return Class.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns the type of this field. This includes all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the type of the field
    * 
    * @see java.lang.reflect.Field#getGenericType()
    */
   public Type getGenericType() {
      return Types.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns true if this field is an enum constant for an enum type.
    * 
    * @return true if this field is an enum constant; false otherwise
    * 
    * @see java.lang.reflect.Field#isEnumConstant()
    */
   public boolean isEnumConstant() {
      return asElement().getKind() == ElementKind.ENUM_CONSTANT;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof Field) {
         Field other = (Field) o;
         return getDeclaringClass().equals(other.getDeclaringClass())
               && getName().equals(other.getName());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 29 * getDeclaringClass().hashCode() + getName().hashCode();
   }

   @Override
   public String toString() {
      return toString(false);
   }
   
   /**
    * Returns a string representation of this field that includes generic type information and
    * possible references to the enclosing class's type parameters. This is similar to
    * {@link #toString()} except that it includes generic type information instead of just
    * indicating erased types.
    * 
    * @return a string representation of the field that includes generic type information
    */
   public String toGenericString() {
      return toString(true);
   }
   
   private String toString(boolean includeGenerics) {
      StringBuilder sb = new StringBuilder();
      Modifier.appendModifiers(sb, getModifiers());
      Type type = includeGenerics ? getGenericType() : getType();
      sb.append(type.toTypeString());
      sb.append(" ");
      sb.append(getName());
      return sb.toString();
   }
}
