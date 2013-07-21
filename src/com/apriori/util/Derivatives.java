package com.apriori.util;

import com.apriori.reflect.TypeRef;

import java.lang.reflect.TypeVariable;

/** Utility methods for working with instances of {@link DerivedFrom}. */
// TODO: javadoc
// TODO: tests
public final class Derivatives {
   private Derivatives() {
   }

   private static final String TYPE_VAR_NAME = getTypeVarName();
   
   private static String getTypeVarName() {
      @SuppressWarnings("rawtypes") // compiler can't get type args from class token
      TypeVariable<Class<DerivedFrom>> vars[] = DerivedFrom.class.getTypeParameters();
      if (vars.length != 1) {
         throw new AssertionError("DerivedFrom should have exactly one type parameter, not "
               + vars.length);
      }
      return vars[0].getName();
   }
   
   @SuppressWarnings("unchecked") // in the one unchecked cast, we've just checked the type
   public static boolean isDerivedFrom(Class<?> type, Class<? extends DerivedFrom<?>> derivedType) {
      TypeRef<? extends DerivedFrom<?>> derivedTypeRef = TypeRef.forClass(derivedType);
      while (derivedTypeRef != null) {
         @SuppressWarnings("rawtypes") // can only get raw type from class token...
         TypeRef<DerivedFrom> derivedFrom =
               TypeRef.getSuperTypeRef(derivedTypeRef, DerivedFrom.class);
         if (derivedFrom.canResolveTypeVariable(TYPE_VAR_NAME)) {
            TypeRef<?> argTypeRef = derivedFrom.resolveTypeVariable(TYPE_VAR_NAME);
            Class<?> argType = argTypeRef.asClass();
            if (argType.equals(type)) {
               return true;
            } else if (DerivedFrom.class.isAssignableFrom(argType)) {
               // keep navigating the hierarchy to see if the type is indirectly derived
               derivedTypeRef = (TypeRef<? extends DerivedFrom<?>>) argTypeRef;
            } else {
               return false;
            }
         }
      }
      return false;
   }
}
