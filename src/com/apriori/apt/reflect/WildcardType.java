package com.apriori.apt.reflect;

import javax.lang.model.type.TypeMirror;

/**
 * A wildcard type. Wildcards indicates unknown (but optionally bounded) values for type parameters.
 * This is analogous to {@link java.lang.reflect.WildcardType java.lang.reflect.WildcardType},
 * except that it represents types in Java source (during annotation processing) vs. representing
 * runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.WildcardType
 */
public class WildcardType implements Type {
   private final javax.lang.model.type.WildcardType modelType;
   
   private WildcardType(javax.lang.model.type.WildcardType modelType) {
      if (modelType == null) {
         throw new NullPointerException();
      }
      this.modelType = modelType;
   }
   
   /**
    * Creates a wildcard type from the specified type mirror.
    * 
    * @param mirror the type mirror
    * @return a wildcard type
    * @throws NullPointerException if the specified type mirror is null
    */
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
   
   /**
    * Returns a bound that the wildcard type must extend. This will be the type referenced in an
    * optional {@code extends} clause on the wildcard. For example, in the type
    * {@code List<? extends Number>}, the type parameter is a wildcard type with a bound of
    * {@code Number}. If no explicit bound exists for the wildcard then {@code java.lang.Object} is
    * the implied bound.
    * 
    * <p>This method varies from the analogous method in {@link java.lang.reflect.WildcardType
    * java.lang.reflect.WildcardType} since that method's return value implies that wildcard types
    * can have multiple bounds. As of Java 7, wildcard types can only have a single upper bound.
    * (though {@link TypeVariable}s can have more than one). Also the word {@code extends} is used
    * in the name as it is more explicit than the word {@code upper} (which is arguably confusing).
    * 
    * @return the wildcard's upper bound
    * 
    * @see java.lang.reflect.WildcardType#getUpperBounds()
    * @see javax.lang.model.type.WildcardType#getExtendsBound()
    */
   public Type getExtendsBound() {
      TypeMirror bound = modelType.getExtendsBound();
      if (bound == null) {
         return Class.forJavaLangObject();
      } else {
         return Types.forTypeMirror(bound);
      }
   }
   
   /**
    * Returns a bound of which the wildcard type must be a super-type. This will be the type
    * referenced in an optional {@code super} clause on the wildcard. For example, in the type
    * {@code List<? super Integer>}, the type parameter is a wildcard type with a bound of
    * {@code Integer}.
    * 
    * <p>This method varies from the analogous method in {@link java.lang.reflect.WildcardType
    * java.lang.reflect.WildcardType} since that method's return value implies that wildcard types
    * can have multiple bounds. As of Java 7, wildcard types can only have at most one lower bound.
    * Also the word {@code super} is used in the name as it is more explicit than the word
    * {@code lower} (which is arguably confusing).
    * 
    * @return the wildcard's lower bound or {@code null} if it has no lower bound
    * 
    * @see java.lang.reflect.WildcardType#getLowerBounds()
    * @see javax.lang.model.type.WildcardType#getSuperBound()
    */
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
