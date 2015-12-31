package com.apriori.reflect.model;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * Utility methods for converting between representations of modifiers in
 * {@linkplain java.lang.reflect.Modifier core reflection} and in a
 * {@link Modifier processing environment}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionModifiers {
   
   /**
    * Adds modifiers in the given bitmask (core reflection representation) to the given set.
    *
    * @param modifiers a bitmask of modifiers (for example, from {@link Member#getModifiers()})
    * @param modifierSet a set of modifiers
    */
   static void addToSet(int modifiers, Set<Modifier> modifierSet) {
      if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
         modifierSet.add(Modifier.ABSTRACT);
      }
      if (java.lang.reflect.Modifier.isFinal(modifiers)) {
         modifierSet.add(Modifier.FINAL);
      }
      if (java.lang.reflect.Modifier.isNative(modifiers)) {
         modifierSet.add(Modifier.NATIVE);
      }
      if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
         modifierSet.add(Modifier.PRIVATE);
      }
      if (java.lang.reflect.Modifier.isProtected(modifiers)) {
         modifierSet.add(Modifier.PROTECTED);
      }
      if (java.lang.reflect.Modifier.isPublic(modifiers)) {
         modifierSet.add(Modifier.PUBLIC);
      }
      if (java.lang.reflect.Modifier.isStatic(modifiers)) {
         modifierSet.add(Modifier.STATIC);
      }
      if (java.lang.reflect.Modifier.isStrict(modifiers)) {
         modifierSet.add(Modifier.STRICTFP);
      }
      if (java.lang.reflect.Modifier.isSynchronized(modifiers)) {
         modifierSet.add(Modifier.SYNCHRONIZED);
      }
      if (java.lang.reflect.Modifier.isTransient(modifiers)) {
         modifierSet.add(Modifier.TRANSIENT);
      }
      if (java.lang.reflect.Modifier.isVolatile(modifiers)) {
         modifierSet.add(Modifier.VOLATILE);
      }
   }
   
   /**
    * Converts the given bitmask of modifiers (core reflection representation) into a set.
    *
    * @param modifiers a bitmask of modifiers (for example, from {@link Member#getModifiers()})
    * @return a set of the modifiers indicated in the given bitmask
    */
   static Set<Modifier> asSet(int modifiers) {
      EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);
      addToSet(modifiers, result);
      return Collections.unmodifiableSet(result);
   }
}
