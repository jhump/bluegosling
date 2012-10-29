package com.apriori.apt.reflect;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

/**
 * An array type with generic type information. This generally indicates that the array's component
 * type is a parameterized type or type variable. This is analogous to {@link java.lang.reflect.GenericArrayType
 * java.lang.reflect.GenericArrayType}, except that it represents types
 * in Java source (during annotation processing) vs. representing runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.GenericArrayType
 */
public class GenericArrayType implements Type {
   
   private final ArrayType arrayType;
   
   private GenericArrayType(ArrayType arrayType) {
      if (arrayType == null) {
         throw new NullPointerException();
      }
      this.arrayType = arrayType;
   }
   
   /**
    * Creates a generic array type from the specified type mirror.
    * 
    * @param arrayType the type mirror
    * @return a generic array type
    * @throws NullPointerException if the specified type mirror is null
    */
   // TODO: throw IllegalArgumentException if the specified type should be represented by a
   // Class instead of a GenericArrayType (i.e. its component type has no references to type
   // parameter or parameterized types)
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
   
   /**
    * Gets the component type of the array.
    * 
    * @return the component type
    * 
    * @see java.lang.reflect.GenericArrayType#getGenericComponentType()
    */
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
