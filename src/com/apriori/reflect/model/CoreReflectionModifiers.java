package com.apriori.reflect.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.lang.model.element.Modifier;


class CoreReflectionModifiers {
   
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
   
   static Set<Modifier> asSet(int modifiers) {
      EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);
      addToSet(modifiers, result);
      return Collections.unmodifiableSet(result);
   }
}
