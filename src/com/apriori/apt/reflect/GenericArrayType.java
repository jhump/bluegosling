package com.apriori.apt.reflect;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

//TODO: javadoc!!
public class GenericArrayType implements Type {
   
   private final ArrayType arrayType;
   
   private GenericArrayType(ArrayType arrayType) {
      if (arrayType == null) {
         throw new NullPointerException();
      }
      this.arrayType = arrayType;
   }
   
   public static GenericArrayType forTypeMirror(ArrayType arrayType) {
      return new GenericArrayType(arrayType);
   }
   
   @Override
   public Type.Kind getTypeKind() {
      return Type.Kind.GENERIC_ARRAY_TYPE;
   }

   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitGenericArrayType(this,  p);
   }
   
   @Override
   public TypeMirror asTypeMirror() {
      return arrayType;
   }
   
   public Type getGenericComponentType() {
      return Types.forTypeMirror(arrayType.getComponentType());
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof GenericArrayType) {
         GenericArrayType other = (GenericArrayType) o;
         return getGenericComponentType().equals(other.getGenericComponentType());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * getGenericComponentType().hashCode();
   }

   @Override
   public String toString() {
      return toTypeString();
   }

   @Override
   public String toTypeString() {
      return getGenericComponentType().toTypeString() + "[]";
   }
}
