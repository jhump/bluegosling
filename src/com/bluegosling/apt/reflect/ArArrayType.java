package com.bluegosling.apt.reflect;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.GenericArrayType;

import javax.lang.model.type.ArrayType;

/**
 * An array type. This is used to represent array's where component type may be generic or may not
 * be. This is analogous to {@link AnnotatedArrayType}, except that it represents types in Java
 * source (during annotation processing) vs. representing runtime types. In some ways it is like
 * {@link GenericArrayType} except that it can represent non-generic arrays, too.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see GenericArrayType
 * @see AnnotatedArrayType
 * @see ArrayType
 */
public class ArArrayType extends ArType {
   
   private ArArrayType(ArrayType arrayType) {
      super(arrayType);
   }
   
   /**
    * Creates an array type from the specified type mirror.
    * 
    * @param arrayType the type mirror
    * @return a generic array type
    * @throws NullPointerException if the specified type mirror is null
    */
   public static ArArrayType forTypeMirror(ArrayType arrayType) {
      return new ArArrayType(arrayType);
   }
   
   @Override
   public ArrayType asTypeMirror() {
      return (ArrayType) delegate();
   }

   @Override
   public ArType.Kind getTypeKind() {
      return ArType.Kind.ARRAY_TYPE;
   }

   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitArrayType(this,  p);
   }
   
   /**
    * Gets the component type of the array.
    * 
    * @return the component type
    * 
    * @see java.lang.reflect.GenericArrayType#getGenericComponentType()
    * @see java.lang.reflect.AnnotatedArrayType#getAnnotatedGenericComponentType()
    * @see javax.lang.model.type.ArrayType#getComponentType()
    */
   public ArType getComponentType() {
      return ArTypes.forTypeMirror(asTypeMirror().getComponentType());
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof ArArrayType) {
         ArArrayType other = (ArArrayType) o;
         return getComponentType().equals(other.getComponentType());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * getComponentType().hashCode();
   }

   @Override
   public String toString() {
      return getComponentType().toString() + "[]";
   }
}
