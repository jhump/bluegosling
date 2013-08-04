package com.apriori.util;

import com.apriori.reflect.TypeRef;

import java.lang.reflect.TypeVariable;

/**
 * Utility methods for determining if types are derived from one another. A type, {@code TypeA}, is
 * derived from another type, {@code TypeB}, if any of the following are true:
 * <ul>
 * <li>{@code TypeA} implements {@link DerivedFrom DerivedFrom}{@code <TypeB>}</li>
 * <li>{@code TypeA} implements {@link DerivedFrom DerivedFrom}{@code <TypeC>} and {@code TypeC} is
 * derived from {@code TypeB}</li>
 * <li>{@code TypeA} is annotated with {@link IsDerivedFrom @IsDerivedFrom(TypeB.class)}</li>
 * <li>{@code TypeA} is annotated with {@link IsDerivedFrom @IsDerivedFrom(TypeC.class)} and
 * {@code TypeC} is derived from {@code TypeB}</li>
 * </ul>
 */
//TODO: unify the two methods so derivation via interface and via annotation are equivalent
//TODO: javadoc
//TODO: tests
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
   public static boolean isDerivedFrom(Class<? extends DerivedFrom<?>> derivedType, Class<?> type) {
      if (type.equals(derivedType)) {
         return true;
      }
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
   
   public static boolean isDerivedViaAnnotationFrom(Class<?> derivedType, Class<?> type) {
      if (type.equals(derivedType)) {
         return true;
      }
      IsDerivedFrom derivedFromAnnotation = derivedType.getAnnotation(IsDerivedFrom.class);
      while (derivedFromAnnotation != null) {
         Class<?> derivedFrom = derivedFromAnnotation.value();
         if (derivedFrom.equals(type)) {
            return true;
         } else {
            derivedFromAnnotation = derivedFrom.getAnnotation(IsDerivedFrom.class);
         }
      }
      return false;  
   }
}
