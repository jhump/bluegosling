package com.apriori.apt.reflect;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//TODO: javadoc!
public enum Modifier {
   PRIVATE,
   PROTECTED,
   PUBLIC,
   PACKAGE_PRIVATE,
   STATIC,
   ABSTRACT,
   FINAL,
   TRANSIENT,
   VOLATILE,
   NATIVE,
   STRICTFP,
   SYNCHRONIZED;
   
   private final static Map<javax.lang.model.element.Modifier, Modifier> elementMods =
         new HashMap<javax.lang.model.element.Modifier, Modifier>();
   static {
      elementMods.put(javax.lang.model.element.Modifier.PRIVATE, PRIVATE);
      elementMods.put(javax.lang.model.element.Modifier.PROTECTED, PROTECTED);
      elementMods.put(javax.lang.model.element.Modifier.PUBLIC, PUBLIC);
      elementMods.put(javax.lang.model.element.Modifier.STATIC, STATIC);
      elementMods.put(javax.lang.model.element.Modifier.ABSTRACT, ABSTRACT);
      elementMods.put(javax.lang.model.element.Modifier.FINAL, FINAL);
      elementMods.put(javax.lang.model.element.Modifier.TRANSIENT, TRANSIENT);
      elementMods.put(javax.lang.model.element.Modifier.VOLATILE, VOLATILE);
      elementMods.put(javax.lang.model.element.Modifier.NATIVE, NATIVE);
      elementMods.put(javax.lang.model.element.Modifier.STRICTFP, STRICTFP);
      elementMods.put(javax.lang.model.element.Modifier.SYNCHRONIZED, SYNCHRONIZED);
   }
   
   public static EnumSet<Modifier> fromElementModifiers(Set<javax.lang.model.element.Modifier> mods) {
      EnumSet<Modifier> ret = EnumSet.noneOf(Modifier.class);
      for (javax.lang.model.element.Modifier mod : mods) {
         ret.add(elementMods.get(mod));
      }
      if (!ret.contains(Modifier.PUBLIC) && !ret.contains(Modifier.PROTECTED)
            && !ret.contains(Modifier.PRIVATE)) {
         ret.add(Modifier.PACKAGE_PRIVATE);
      }
      return ret;
   }
   
   static void appendModifiers(StringBuilder sb, Set<Modifier> modifiers) {
      if (!modifiers.isEmpty()) {
         for (Modifier modifier : modifiers) {
            if (modifier != PACKAGE_PRIVATE) {
               sb.append(modifier.name().toLowerCase());
               sb.append(" ");
            }
         }
      }
   }
}
