package com.bluegosling.apt.reflect;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A declared type. In core reflection, this would be represented by a {@link Class} or, for
 * annotated types, by an {@link AnnotatedType} that does not implement any of that interface's
 * sub-interfaces (e.g. not a parameterized type, array type, type variable, or wildcard type). In
 * some ways, it like {@link ParameterizedType} except that it can be used to represent raw and
 * non-generic types, too. Unlike {@link Class}, a declared type will not represent an array type
 * or a primitive type.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see ParameterizedType
 * @see DeclaredType
 */
// TODO: doc
public class ArDeclaredType extends ArAbstractKnownType {
   private ArDeclaredType(DeclaredType declaredType) {
      super(declaredType);
   }
   
   public static ArDeclaredType forTypeMirror(DeclaredType declaredType) {
      return new ArDeclaredType(declaredType);
   }
   
   public static ArDeclaredType forObject() {
      return new ArDeclaredType((DeclaredType) ArClass.forObject().asElement().asType());
   }

   static ArDeclaredType forDeclaredTypeMirror(DeclaredType declaredType) {
      return new ArDeclaredType(declaredType);
   }
   
   /**
    * Returns a list of the actual values supplied for the type variables. If this type represents
    * a non-parameterized type but is nested within a parameterized type then the returned list
    * could be empty.
    * 
    * @return the list of type parameter values
    * 
    * @see ParameterizedType#getActualTypeArguments()
    * @see DeclaredType#getTypeArguments()
    */
   public List<? extends ArType> getActualTypeArguments() {
      List<? extends TypeMirror> args = asTypeMirror().getTypeArguments();
      List<ArType> ret = new ArrayList<ArType>(args.size());
      for (TypeMirror mirror : args) {
         ret.add(ArTypes.forTypeMirror(mirror));
      }
      return ret;
   }
   
   /**
    * Returns the enclosing type. If this is a top-level type then this method returns {@code null}.
    * Just like this type, the enclosing type may or may not have type parameters.
    * 
    * @return the enclosing type
    * 
    * @see ParameterizedType#getOwnerType()
    * @see DeclaredType#getEnclosingType()
    */
   public ArDeclaredType getOwnerType() {
      TypeMirror owner = asTypeMirror().getEnclosingType();
      return owner.getKind() == TypeKind.NONE
            ? null : ArDeclaredType.forTypeMirror((DeclaredType) owner);
   }
   
   @Override
   public DeclaredType asTypeMirror() {
      return (DeclaredType) delegate();
   }

   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitDeclaredType(this, p);
   }

   @Override
   public Kind getTypeKind() {
      return Kind.DECLARED_TYPE;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof ArDeclaredType) {
         ArDeclaredType other = (ArDeclaredType) o;
         ArType ownerType = getOwnerType();
         return asClass().equals(other.asClass())
               && (ownerType == null
                     ? other.getOwnerType() == null
                     : ownerType.equals(other.getOwnerType()))
               && getActualTypeArguments().equals(other.getActualTypeArguments());
      }
      return false;
   }

   @Override
   public int hashCode() {
      ArType ownerType = getOwnerType();
      int ownerHashCode = ownerType == null ? 0 : ownerType.hashCode();
      return 31 * (29 * ownerHashCode + asClass().hashCode())
            ^ getActualTypeArguments().hashCode();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      toStringBuilder(sb);
      return sb.toString();
   }
   
   private void toStringBuilder(StringBuilder sb) {
      ArDeclaredType owner = getOwnerType();
      if (owner != null) {
         owner.toStringBuilder(sb);
         sb.append('.');
      }
      sb.append(asClass().getCanonicalName());
      List<? extends ArType> args = getActualTypeArguments();
      if (!args.isEmpty()) {
         sb.append('<');
         boolean first = true;
         for (ArType typeArg : getActualTypeArguments()) {
            if (first) {
               first = false;
            } else {
               sb.append(',');
            }
            sb.append(typeArg.toString());
         }
         sb.append('>');
      }
   }
}
