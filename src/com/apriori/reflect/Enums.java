package com.apriori.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// TODO: doc
// TODO: tests
public final class Enums {
   private Enums() {
   }
   
   private static final int EXPECTED_VALUES_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC;
   
   public static <T extends Enum<T>> T[] values(Class<T> enumType) {
      try {
         Method m = enumType.getDeclaredMethod("values");
         // should be good to go, but we'll sanity check the method, just in case
         if ((m.getModifiers() & EXPECTED_VALUES_MODIFIERS) != EXPECTED_VALUES_MODIFIERS) {
            throw new AssertionError(
                  "Method " + enumType.getName() + "#values() should be public and static");
         }
         Class<?> returnType = m.getReturnType();
         if (!returnType.isArray() || returnType.getComponentType() != enumType) {
            throw new AssertionError("Method " + enumType.getName() + "#values() should return "
                  + enumType.getName() + "[]");
         }
         
         @SuppressWarnings("unchecked")
         T ret[] = (T[]) m.invoke(null);
         return ret;
         
      } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
         throw new AssertionError(e);
      }      
   }
}