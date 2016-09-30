package com.bluegosling.util;

import static com.bluegosling.reflect.Annotations.findAnnotation;

import com.bluegosling.reflect.Types;

import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for determining if types are derived from one another. A type, {@code TypeA}, is
 * derived from another type, {@code TypeB}, if any of the following are true:
 * <ul>
 * <li>{@code TypeA} implements {@link DerivedFrom}{@code <TypeB>}</li>
 * <li>{@code TypeA} implements {@link DerivedFrom}{@code <TypeC>} and {@code TypeC} is
 * derived from {@code TypeB}</li>
 * <li>{@code TypeA} is annotated with {@code @}{@link IsDerivedFrom}{@code (TypeB.class)}</li>
 * <li>{@code TypeA} is annotated with {@code @}{@link IsDerivedFrom}{@code (TypeC.class)} and
 * {@code TypeC} is derived from {@code TypeB}</li>
 * </ul>
 */
//TODO: tests
public final class Derivations {
   private Derivations() {
   }

   private static final TypeVariable<?> TYPE_VAR;
   static {
      TypeVariable<?> vars[] = DerivedFrom.class.getTypeParameters();
      if (vars.length != 1) {
         throw new AssertionError("DerivedFrom should have exactly one type parameter, not "
               + vars.length);
      }
      TYPE_VAR = vars[0];
   }
   
   /**
    * Gets the sources from which the specified type is directly derived. A type, {@code TypeA}, is
    * directly derived from another type, {@code TypeB}, if it implements 
    * {@link DerivedFrom}{@code <TypeB>} <em>or</em> if it is annotated with
    * {@link IsDerivedFrom @IsDerivedFrom}{@code (TypeB.class)}.
    * 
    * <p>The returned set will have between zero and two entries. It could have two entries if the
    * specified type both implements {@link DerivedFrom} and is annotated with {@link
    * IsDerivedFrom @IsDerivedFrom} and the two referenced sources differ. If the class neither
    * implements that interface nor is annotated then the returned set will be empty.
    * 
    * @param derivedType the type to check
    * @return the set of types from which the specified type is directly derived
    */
   public static Set<Class<?>> getDirectDerivationSources(Class<?> derivedType) {
      HashSet<Class<?>> sources = new HashSet<Class<?>>(3);
      Class<?> source1 = derivedFromInterface(derivedType);
      if (source1 != null) {
         sources.add(source1);
      }
      Class<?> source2 = derivedFromAnnotation(derivedType);
      if (source2 != null) {
         sources.add(source2);
      }
      return sources;
   }

   /**
    * Gets all sources, both direct and indirect, from which the specified type is derived. This
    * will return all types for which {@link #isDerivedFrom(Class, Class)} would return true. Since
    * a class is considered "derived from" itself, then the returned set also includes the specified
    * type.
    * 
    * <p>This includes any types from which the specified type is directly derived. It also includes
    * all types from which those direct sources are derived, etc.
    * 
    * @param derivedType the type to check
    * @return the set of all types from which the specified type is derived, directly and indirectly
    */
   public static Set<Class<?>> getAllDerivationSources(Class<?> derivedType) {
      ArrayDeque<Class<?>> sourceQueue = new ArrayDeque<Class<?>>();
      HashSet<Class<?>> sources = new HashSet<Class<?>>(3);

      sourceQueue.add(derivedType);
      while (!sourceQueue.isEmpty()) {
         Class<?> source = sourceQueue.remove();
         sources.add(source);
         Class<?> source1 = derivedFromInterface(source);
         if (source1 != null) {
            sourceQueue.add(source1);
         }
         Class<?> source2 = derivedFromAnnotation(source);
         if (source2 != null) {
            sourceQueue.add(source2);
         }
      }
      
      return sources;
   }

   /**
    * Returns true if the specified type is derived from the other.
    * 
    * @param derivedType the type to check
    * @param type the possible derivation source
    * @return true if {@code derivedType} is derived from {@code type}; false otherwise
    */
   public static boolean isDerivedFrom(Class<?> derivedType, Class<?> type) {
      if (type.equals(derivedType)) {
         return true;
      }
      Class<?> source1, source2;
      return ((source1 = derivedFromInterface(derivedType)) != null
                  && isDerivedFrom(source1, type))
            || ((source2 = derivedFromAnnotation(derivedType)) != null
                  && isDerivedFrom(source2, type));
   }
   
   private static Class<?> derivedFromInterface(Class<?> derivedType) {
      if (!DerivedFrom.class.isAssignableFrom(derivedType)) {
         return null;
      }
      return Types.getErasure(Types.resolveTypeVariable(derivedType, TYPE_VAR));
   }

   private static Class<?> derivedFromAnnotation(Class<?> derivedType) {
      IsDerivedFrom derivedFromAnnotation = findAnnotation(derivedType, IsDerivedFrom.class);
      return derivedFromAnnotation == null ? null : derivedFromAnnotation.value();
   }
}
