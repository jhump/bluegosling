package com.bluegosling.apt.reflect;

import java.lang.reflect.AnnotatedType;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A primitive type. In core reflection, this would be represented by a {@link Class} or, for
 * annotated types, by an {@link AnnotatedType} that does not implement any of that interface's
 * sub-interfaces (e.g. not a parameterized type, array type, type variable, or wildcard type). A
 * primitive type is any of Java's seven numeric primitive types, {@code boolean}, or {@code void}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: doc
public class ArPrimitiveType extends ArAbstractKnownType {

   ArPrimitiveType(TypeMirror mirror) {
      super(mirror);
      if (mirror.getKind() != TypeKind.VOID && !mirror.getKind().isPrimitive()) {
         throw new IllegalArgumentException(
               "Given type is not primitive: " + mirror.toString());
      }
   }
   
   public static ArPrimitiveType forTypeMirror(TypeMirror type) {
      return new ArPrimitiveType(type);
   }
   
   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitPrimitiveType(this, p);
   }

   @Override
   public Kind getTypeKind() {
      return Kind.PRIMITIVE_TYPE;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof ArAbstractKnownType) {
         ArAbstractKnownType other = (ArAbstractKnownType) o;
         return asClass().equals(other.asClass());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return asClass().hashCode();
   }

   @Override
   public String toString() {
      return asClass().getCanonicalName();
   }
}
