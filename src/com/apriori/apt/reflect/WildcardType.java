package com.apriori.apt.reflect;

import javax.lang.model.type.TypeMirror;

//TODO: javadoc!
public class WildcardType implements Type {
   private final javax.lang.model.type.WildcardType modelType;
   
   private WildcardType(javax.lang.model.type.WildcardType modelType) {
      if (modelType == null) {
         throw new NullPointerException();
      }
      this.modelType = modelType;
   }
   
   public static WildcardType forTypeMirror(javax.lang.model.type.WildcardType mirror) {
      return new WildcardType(mirror);
   }
   
   @Override
   public Type.Kind getTypeKind() {
      return Type.Kind.WILDCARD_TYPE;
   }
   
   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitWildcardType(this,  p);
   }
   
   @Override
   public TypeMirror asTypeMirror() {
      return modelType;
   }
   
   public Type getExtendsBound() {
      TypeMirror bound = modelType.getExtendsBound();
      if (bound == null) {
         return Class.forJavaLangObject();
      } else {
         return Types.forTypeMirror(bound);
      }
   }
   
   public Type getSuperBound() {
      TypeMirror bound = modelType.getSuperBound();
      if (bound == null) {
         return null;
      } else {
         return Types.forTypeMirror(bound);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof WildcardType) {
         WildcardType other = (WildcardType) o;
         Type extendsBound = getExtendsBound();
         Type superBound = getSuperBound();
         return extendsBound.equals(other.getExtendsBound())
               && (superBound == null ? other.getSuperBound() == null
                     : superBound.equals(other.getSuperBound()));
      }
      return false;
   }

   @Override
   public int hashCode() {
      Type extendsBound = getExtendsBound();
      Type superBound = getSuperBound();
      return 59 * extendsBound.hashCode() + (superBound == null ? 0 : superBound.hashCode());
   }
   
   @Override
   public String toString() {
      return toTypeString();
   }

   @Override
   public String toTypeString() {
      StringBuilder sb = new StringBuilder();
      sb.append("?");
      Type superBound = getSuperBound();
      if (superBound != null) {
         sb.append(" super ");
         sb.append(superBound.toTypeString());
      } else {
         TypeMirror bound = modelType.getExtendsBound();
         if (bound != null) {
            sb.append(" extends ");
            sb.append(Types.forTypeMirror(bound).toTypeString());
         }
      }
      return sb.toString();
   }
}
