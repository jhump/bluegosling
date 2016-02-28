package com.bluegosling.apt.reflect;

import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a modifier used in Java source code. Many of the modifiers apply to all kinds of
 * Java source elements whereas some are less generic (for example, the {@code native} modifier
 * cannot be used on a class).
 * 
 * This is similar in nature to the API in {@link Modifier} except for two main differences:
 * <ol>
 * <li>This class is used to represent modifiers in Java source (during annotation processing) vs.
 * representing modifiers on runtime types and their elements.</li>
 * <li>This class is an {@code enum} whereas the API in {@code java.lang.reflect} is a set of
 * numeric constants and utility methods that use bitmasks to extract modifier information from
 * an integer bitfield.</li>
 * </ol>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Modifier
 */
public enum ArModifier {
   /**
    * The {@code private} modifier.
    * 
    * @see javax.lang.model.element.Modifier#PRIVATE
    * @see java.lang.reflect.Modifier#PRIVATE
    * @see java.lang.reflect.Modifier#isPrivate(int)
    */
   PRIVATE,

   /**
    * The {@code protected} modifier.
    * 
    * @see javax.lang.model.element.Modifier#PROTECTED
    * @see java.lang.reflect.Modifier#PROTECTED
    * @see java.lang.reflect.Modifier#isProtected(int)
    */
   PROTECTED,
   
   /**
    * The {@code public} modifier.
    * 
    * @see javax.lang.model.element.Modifier#PUBLIC
    * @see java.lang.reflect.Modifier#PUBLIC
    * @see java.lang.reflect.Modifier#isPublic(int)
    */
   PUBLIC,

   /**
    * A psuedo-modifier that represents package-private (aka "default") access. It's not an
    * actual modifier in Java but rather indicates the absence of {@code private},
    * {@code protected}, or {@code public} modifiers. It's represented by an enum here to make
    * testing for this type of access easier and more explicit.
    */
   PACKAGE_PRIVATE,
   
   /**
    * The {@code static} modifier.
    * 
    * @see javax.lang.model.element.Modifier#STATIC
    * @see java.lang.reflect.Modifier#STATIC
    * @see java.lang.reflect.Modifier#isStatic(int)
    */
   STATIC,

   /**
    * The {@code abstract} modifier.
    * 
    * @see javax.lang.model.element.Modifier#ABSTRACT
    * @see java.lang.reflect.Modifier#ABSTRACT
    * @see java.lang.reflect.Modifier#isAbstract(int)
    */
   ABSTRACT,

   /**
    * The {@code final} modifier.
    * 
    * @see javax.lang.model.element.Modifier#FINAL
    * @see java.lang.reflect.Modifier#FINAL
    * @see java.lang.reflect.Modifier#isFinal(int)
    */
   FINAL,

   /**
    * The {@code transient} modifier.
    * 
    * @see javax.lang.model.element.Modifier#TRANSIENT
    * @see java.lang.reflect.Modifier#TRANSIENT
    * @see java.lang.reflect.Modifier#isTransient(int)
    */
   TRANSIENT,

   /**
    * The {@code volatile} modifier.
    * 
    * @see javax.lang.model.element.Modifier#VOLATILE
    * @see java.lang.reflect.Modifier#VOLATILE
    * @see java.lang.reflect.Modifier#isVolatile(int)
    */
   VOLATILE,

   /**
    * The {@code native} modifier.
    * 
    * @see javax.lang.model.element.Modifier#NATIVE
    * @see java.lang.reflect.Modifier#NATIVE
    * @see java.lang.reflect.Modifier#isNative(int)
    */
   NATIVE,

   /**
    * The {@code strictfp} modifier.
    * 
    * @see javax.lang.model.element.Modifier#STRICTFP
    * @see java.lang.reflect.Modifier#STRICT
    * @see java.lang.reflect.Modifier#isStrict(int)
    */
   STRICTFP,

   /**
    * The {@code synchronized} modifier.
    * 
    * @see javax.lang.model.element.Modifier#SYNCHRONIZED
    * @see java.lang.reflect.Modifier#SYNCHRONIZED
    * @see java.lang.reflect.Modifier#isSynchronized(int)
    */
   SYNCHRONIZED;
   
   private final static Map<javax.lang.model.element.Modifier, ArModifier> elementToMod =
         new HashMap<javax.lang.model.element.Modifier, ArModifier>();
   private final static Map<ArModifier, javax.lang.model.element.Modifier> modToElement =
         new EnumMap<ArModifier, javax.lang.model.element.Modifier>(ArModifier.class);
   private final static Map<Integer, ArModifier> intToMod =
         new HashMap<Integer, ArModifier>();
   private final static Map<ArModifier, Integer> modToInt =
         new EnumMap<ArModifier, Integer>(ArModifier.class);
   static {
      elementToMod.put(javax.lang.model.element.Modifier.PRIVATE, PRIVATE);
      elementToMod.put(javax.lang.model.element.Modifier.PROTECTED, PROTECTED);
      elementToMod.put(javax.lang.model.element.Modifier.PUBLIC, PUBLIC);
      elementToMod.put(javax.lang.model.element.Modifier.STATIC, STATIC);
      elementToMod.put(javax.lang.model.element.Modifier.ABSTRACT, ABSTRACT);
      elementToMod.put(javax.lang.model.element.Modifier.FINAL, FINAL);
      elementToMod.put(javax.lang.model.element.Modifier.TRANSIENT, TRANSIENT);
      elementToMod.put(javax.lang.model.element.Modifier.VOLATILE, VOLATILE);
      elementToMod.put(javax.lang.model.element.Modifier.NATIVE, NATIVE);
      elementToMod.put(javax.lang.model.element.Modifier.STRICTFP, STRICTFP);
      elementToMod.put(javax.lang.model.element.Modifier.SYNCHRONIZED, SYNCHRONIZED);

      modToElement.put(PRIVATE, javax.lang.model.element.Modifier.PRIVATE);
      modToElement.put(PROTECTED, javax.lang.model.element.Modifier.PROTECTED);
      modToElement.put(PUBLIC, javax.lang.model.element.Modifier.PUBLIC);
      modToElement.put(PACKAGE_PRIVATE, null);
      modToElement.put(STATIC, javax.lang.model.element.Modifier.STATIC);
      modToElement.put(ABSTRACT, javax.lang.model.element.Modifier.ABSTRACT);
      modToElement.put(FINAL, javax.lang.model.element.Modifier.FINAL);
      modToElement.put(TRANSIENT, javax.lang.model.element.Modifier.TRANSIENT);
      modToElement.put(VOLATILE, javax.lang.model.element.Modifier.VOLATILE);
      modToElement.put(NATIVE, javax.lang.model.element.Modifier.NATIVE);
      modToElement.put(STRICTFP, javax.lang.model.element.Modifier.STRICTFP);
      modToElement.put(SYNCHRONIZED, javax.lang.model.element.Modifier.SYNCHRONIZED);
      
      intToMod.put(java.lang.reflect.Modifier.PRIVATE, PRIVATE);
      intToMod.put(java.lang.reflect.Modifier.PROTECTED, PROTECTED);
      intToMod.put(java.lang.reflect.Modifier.PUBLIC, PUBLIC);
      intToMod.put(java.lang.reflect.Modifier.STATIC, STATIC);
      intToMod.put(java.lang.reflect.Modifier.ABSTRACT, ABSTRACT);
      intToMod.put(java.lang.reflect.Modifier.FINAL, FINAL);
      intToMod.put(java.lang.reflect.Modifier.TRANSIENT, TRANSIENT);
      intToMod.put(java.lang.reflect.Modifier.VOLATILE, VOLATILE);
      intToMod.put(java.lang.reflect.Modifier.NATIVE, NATIVE);
      intToMod.put(java.lang.reflect.Modifier.STRICT, STRICTFP);
      intToMod.put(java.lang.reflect.Modifier.SYNCHRONIZED, SYNCHRONIZED);

      modToInt.put(PRIVATE, java.lang.reflect.Modifier.PRIVATE);
      modToInt.put(PROTECTED, java.lang.reflect.Modifier.PROTECTED);
      modToInt.put(PUBLIC, java.lang.reflect.Modifier.PUBLIC);
      modToInt.put(PACKAGE_PRIVATE, 0);
      modToInt.put(STATIC, java.lang.reflect.Modifier.STATIC);
      modToInt.put(ABSTRACT, java.lang.reflect.Modifier.ABSTRACT);
      modToInt.put(FINAL, java.lang.reflect.Modifier.FINAL);
      modToInt.put(TRANSIENT, java.lang.reflect.Modifier.TRANSIENT);
      modToInt.put(VOLATILE, java.lang.reflect.Modifier.VOLATILE);
      modToInt.put(NATIVE, java.lang.reflect.Modifier.NATIVE);
      modToInt.put(STRICTFP, java.lang.reflect.Modifier.STRICT);
      modToInt.put(SYNCHRONIZED, java.lang.reflect.Modifier.SYNCHRONIZED);
   }

   /**
    * Returns the corresponding bit mask for this modifier.
    * 
    * @return the bit mask for this modifier
    */
   public int asInt() {
      return modToInt.get(this);
   }
   
   /**
    * Returns the corresponding {@link javax.lang.model.element.Modifier javax.lang.model.element.Modifier}.
    * 
    * @return the corresponding {@code javax.lang.model.element.Modifier}
    */
   public javax.lang.model.element.Modifier asElementModifier() {
      return modToElement.get(this);
   }

   /**
    * Converts the specified set of modifiers to an integer bitfield.
    * 
    * @param modifiers the set of modifiers
    * @return the corresponding bitfield
    */
   public static int toBitfield(EnumSet<ArModifier> modifiers) {
      int ret = 0;
      for (ArModifier mod : modifiers) {
         ret |= modToInt.get(mod);
      }
      return ret;
   }

   /**
    * Returns the modifier corresponding to the specified {@link javax.lang.model.element.Modifier
    * javax.lang.model.element.Modifier}.
    * 
    * @param mod a {@code javax.lang.model.element.Modifier}
    * @return the corresponding modifier
    */
   public static ArModifier fromElementModifier(javax.lang.model.element.Modifier mod) {
      return elementToMod.get(mod);
   }

   /**
    * Returns a set of modifiers represented by the specified bitfield.
    * 
    * @param modifiers a bitfield
    * @return the set of modifiers
    * @throws IllegalArgumentException if the bitfield has any bits set that don't correspond to
    *       actual modifiers
    */
   public static EnumSet<ArModifier> fromBitfield(int modifiers) {
      EnumSet<ArModifier> ret = EnumSet.noneOf(ArModifier.class);
      for (int i = 1; i != 0; i <<= 1) {
         if ((modifiers & i) != 0) {
            ArModifier mod = intToMod.get(i);
            if (mod == null) {
               throw new IllegalArgumentException(String.format("The specified value contains a bit"
                     + " that is not a valid modifier: %x", i));
            }
            ret.add(mod);
         }
      }
      return ret;
   }
   
   /**
    * Converts a set of {@link javax.lang.model.element.Modifier}s to a set of modifiers.
    * 
    * @param mods a set of {@code javax.lang.model.element.Modifier}
    * @return a set of modifiers
    */
   public static EnumSet<ArModifier> fromElementModifiers(Set<javax.lang.model.element.Modifier> mods) {
      EnumSet<ArModifier> ret = EnumSet.noneOf(ArModifier.class);
      for (javax.lang.model.element.Modifier mod : mods) {
         ret.add(elementToMod.get(mod));
      }
      if (!ret.contains(ArModifier.PUBLIC) && !ret.contains(ArModifier.PROTECTED)
            && !ret.contains(ArModifier.PRIVATE)) {
         ret.add(ArModifier.PACKAGE_PRIVATE);
      }
      return ret;
   }
   
   static void appendModifiers(StringBuilder sb, Set<ArModifier> modifiers) {
      if (!modifiers.isEmpty()) {
         for (ArModifier modifier : modifiers) {
            if (modifier != PACKAGE_PRIVATE) {
               sb.append(modifier.name().toLowerCase());
               sb.append(" ");
            }
         }
      }
   }
}
