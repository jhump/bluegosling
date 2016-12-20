package com.bluegosling.apt.reflect;

import static com.bluegosling.apt.ProcessingEnvironments.types;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * An instance of type with information about actual type parameters. Even though a {@link ArClass}
 * has {@linkplain ArClass#getTypeVariables() type variables}, it represents a raw or erased type
 * and its type variables represent type variable declarations. A parameterized type represents
 * an actual instance -- or usage -- of a type with actual type parameter values. This class is
 * analogous to {@link ParameterizedType}, except that it represents types in Java source (during
 * annotation processing) vs. representing runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see ParameterizedType
 */
public class ArParameterizedType implements ArType {
   
   private final DeclaredType declaredType;
   
   /**
    * Creates a parameterized type from the specified type mirror.
    * 
    * @param arrayType the type mirror
    * @throws NullPointerException if the specified type mirror is null
    */
   private ArParameterizedType(DeclaredType declaredType) {
      if (declaredType == null) {
         throw new NullPointerException();
      }
      this.declaredType = declaredType;
   }
   
   /**
    * Creates a parameterized type from the specified type mirror.
    * 
    * @param declaredType the type mirror
    * @return a parameterized type
    * @throws NullPointerException if the specified type mirror is null
    * @throws IllegalArgumentException if the specified type mirror is not a parameterized type
    *       (e.g. neither it nor any enclosing type has type arguments) 
    */
   public static ArParameterizedType forTypeMirror(DeclaredType declaredType) {
      if (isParameterized(declaredType)) {
         return forParameterizedTypeMirror(declaredType);
      }
      throw new IllegalArgumentException(
            "Given type is not parameterized: " + declaredType.toString());
   }
   
   /**
    * Returns true if the given type, or any enclosing type, has type arguments.
    *
    * @param declaredType
    * @return
    */
   static boolean isParameterized(DeclaredType declaredType) {
      while (true) {
         if (!declaredType.getTypeArguments().isEmpty()) {
            return true;
         }
         TypeMirror enclosing = declaredType.getEnclosingType();
         if (enclosing.getKind() == TypeKind.NONE) {
            return false;
         }
         assert enclosing.getKind() == TypeKind.DECLARED;
         declaredType = (DeclaredType) enclosing;
      }
   }

   /**
    * Creates a parameterized type for the given type mirror, which is known to be parameterized. It
    * is up to callers to ensure that the given type is parameterized.
    *
    * @param declaredType the type mirror
    * @return a parameterized type
    * @see #isParameterized(DeclaredType)
    */
   static ArParameterizedType forParameterizedTypeMirror(DeclaredType declaredType) {
      return new ArParameterizedType(declaredType);
   }
   
   @Override
   public ArType.Kind getTypeKind() {
      return ArType.Kind.PARAMETERIZED_TYPE;
   }

   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitParameterizedType(this,  p);
   }
   
   @Override
   public TypeMirror asTypeMirror() {
      return declaredType;
   }

   /**
    * Returns a list of the actual values supplied for the type variables. If this type represents
    * a non-parameterized type but is nested within a parameterized type then the returned list
    * could be empty.
    * 
    * @return the list of type parameter values
    * 
    * @see java.lang.reflect.ParameterizedType#getActualTypeArguments()
    */
   public List<? extends ArType> getActualTypeArguments() {
      List<? extends TypeMirror> args = declaredType.getTypeArguments();
      List<ArType> ret = new ArrayList<ArType>(args.size());
      for (TypeMirror mirror : args) {
         ret.add(ArTypes.forTypeMirror(mirror));
      }
      return ret;
   }
   
   /**
    * Returns the enclosing type. If this is a top-level type then this method returns {@code null}.
    * The enclosing type could also be a {@link ArParameterizedType}, or it could be a {@link ArClass}.
    * 
    * <p>For example, if this type is {@code ParentType<T>.EnclosedType<S>} then this function would
    * return a {@link ArParameterizedType} that represents {@code ParentType<T>}. If this is a static
    * member type or a member of a type that is not parameterized, then it might look more like the
    * type {@code ParentType.EnclosedStaticType<S>}, in which case this method will return a
    * {@link ArClass} that represents {@code ParentType}.
    * 
    * @return the enclosing type
    * 
    * @see java.lang.reflect.ParameterizedType#getOwnerType()
    */
   public ArType getOwnerType() {
      TypeMirror owner = declaredType.getEnclosingType();
      return owner.getKind() == TypeKind.NONE ? null : ArTypes.forTypeMirror(owner);
   }
   
   /**
    * Returns the raw, or erased, type.
    * 
    * @return the raw type
    * 
    * @see java.lang.reflect.ParameterizedType#getRawType()
    */
   public ArClass getRawType() {
      Element erasedType = types().asElement(types().erasure(declaredType));
      ArClass ret = ReflectionVisitors.CLASS_VISITOR.visit(erasedType);
      if (ret == null) {
         throw new MirroredTypeException(declaredType);
      }
      return ret;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ArParameterizedType) {
         ArParameterizedType other = (ArParameterizedType) o;
         ArType ownerType = getOwnerType();
         return (ownerType == null ? other.getOwnerType() == null : ownerType.equals(other.getOwnerType()))
               && getRawType().equals(other.getRawType())
               && getActualTypeArguments().equals(other.getActualTypeArguments());
      }
      return false;
   }

   @Override
   public int hashCode() {
      ArType ownerType = getOwnerType();
      int ownerHashCode = ownerType == null ? 0 : ownerType.hashCode();
      return 31 * (29 * ownerHashCode + getRawType().hashCode())
            ^ getActualTypeArguments().hashCode();
   }

   @Override
   public String toString() {
      return toTypeString();
   }

   @Override
   public String toTypeString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getRawType().toTypeString());
      sb.append("<");
      boolean first = true;
      for (ArType typeArg : getActualTypeArguments()) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(typeArg.toTypeString());
      }
      sb.append(">");
      return sb.toString();
   }
}
