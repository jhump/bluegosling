package com.apriori.apt.reflect;

import com.apriori.apt.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

//TODO: javadoc!
public class ParameterizedType implements Type {
   
   private final DeclaredType declaredType;
   
   private ParameterizedType(DeclaredType declaredType) {
      if (declaredType == null) {
         throw new NullPointerException();
      }
      this.declaredType = declaredType;
   }
   
   public static ParameterizedType forTypeMirror(DeclaredType declaredType) {
      return new ParameterizedType(declaredType);
   }
   
   @Override
   public Type.Kind getTypeKind() {
      return Type.Kind.PARAMETERIZED_TYPE;
   }

   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitParameterizedType(this,  p);
   }
   
   @Override
   public TypeMirror asTypeMirror() {
      return declaredType;
   }

   public List<? extends Type> getActualTypeArguments() {
      List<? extends TypeMirror> args = declaredType.getTypeArguments();
      List<Type> ret = new ArrayList<Type>(args.size());
      for (TypeMirror mirror : args) {
         ret.add(Types.forTypeMirror(mirror));
      }
      return ret;
   }
   
   public Type getOwnerType() {
      TypeMirror owner = declaredType.getEnclosingType();
      return owner.getKind() == TypeKind.NONE ? null : Types.forTypeMirror(owner);
   }
   
   public Class getRawType() {
      Element erasedType = TypeUtils.get().asElement(TypeUtils.get().erasure(declaredType));
      Class ret = ReflectionVisitors.CLASS_VISITOR.visit(erasedType);
      if (ret == null) {
         throw new MirroredTypeException(declaredType);
      }
      return ret;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ParameterizedType) {
         ParameterizedType other = (ParameterizedType) o;
         Type ownerType = getOwnerType();
         return (ownerType == null ? other.getOwnerType() == null : ownerType.equals(other.getOwnerType()))
               && getRawType().equals(other.getRawType())
               && getActualTypeArguments().equals(other.getActualTypeArguments());
      }
      return false;
   }

   @Override
   public int hashCode() {
      Type ownerType = getOwnerType();
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
      for (Type typeArg : getActualTypeArguments()) {
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
