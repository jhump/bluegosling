package com.apriori.apt.reflect;

import java.lang.reflect.Field;
import java.util.EnumSet;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

/**
 * A field. This is analogous to {@link Field}, except that it represents fields in Java source
 * (during annotation processing) vs. representing fields of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Field
 */
public class ArField extends ArAbstractMember {

   private static final EnumSet<ElementKind> ALLOWED_KINDS =
         EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT);
   
   private ArField(VariableElement element) {
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
   public static ArField forElement(VariableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (!ALLOWED_KINDS.contains(element.getKind())) {
         throw new IllegalArgumentException("Invalid element kind. Expected one of "
               + ALLOWED_KINDS + "; got " + element.getKind().name());
      }
      return new ArField(element);
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
   public ArClass getType() {
      return ArClass.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns the type of this field. This includes all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the type of the field
    * 
    * @see java.lang.reflect.Field#getGenericType()
    */
   public ArType getGenericType() {
      return ArTypes.forTypeMirror(asElement().asType());
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
   
   /**
    * Returns the actual enum instance represented by this enum constant field.
    * 
    * @return the enum instance
    * @throws ClassNotFoundException if the declaring class cannot be loaded (which means it does
    *       not exist in compiled form on the classpath or in a fashion that is available/visible to
    *       the current thread's context class loader)
    * @throws IllegalStateException if this field is not an enum constant field
    */
   public Enum<?> asEnum() throws ClassNotFoundException {
      if (!isEnumConstant()) {
         throw new IllegalStateException("not an enum");
      }
      @SuppressWarnings({"unchecked", "rawtypes"})
      Enum<?> ret = Enum.valueOf(
            (Class<? extends Enum>) getDeclaringClass().asClass(), getName());
      return ret;
   }

   /**
    * Returns the actual enum instance represented by this enum constant field. The specified class
    * loader is used in the attempt to load the declaring class.
    * 
    * @param classLoader a class loader
    * @return the enum instance
    * @throws ClassNotFoundException if no such class could be loaded by the specified class loader
    * @throws IllegalStateException if this field is not an enum constant field
    */
   public Enum<?> asEnum(ClassLoader classLoader) throws ClassNotFoundException {
      if (!isEnumConstant()) {
         throw new IllegalStateException("not an enum");
      }
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Enum<?> ret = Enum.valueOf(
            (Class<? extends Enum>) getDeclaringClass().asClass(classLoader),
            getName());
      return ret;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof ArField) {
         ArField other = (ArField) o;
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
   @Override
   public String toGenericString() {
      return toString(true);
   }
   
   private String toString(boolean includeGenerics) {
      StringBuilder sb = new StringBuilder();
      ArModifier.appendModifiers(sb, getModifiers());
      ArType type = includeGenerics ? getGenericType() : getType();
      sb.append(type.toTypeString());
      sb.append(" ");
      sb.append(getName());
      return sb.toString();
   }
}
