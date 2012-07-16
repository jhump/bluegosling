package com.apriori.apt.reflect;

import java.util.EnumSet;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

//TODO: javadoc!!
public class Field extends AbstractMember {

   private static final EnumSet<ElementKind> ALLOWED_KINDS = EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT);
   private Field(VariableElement element) {
      super(element);
   }
   
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
   
   public Class getType() {
      return Class.forTypeMirror(asElement().asType());
   }
   
   public Type getGenericType() {
      return Types.forTypeMirror(asElement().asType());
   }
   
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
