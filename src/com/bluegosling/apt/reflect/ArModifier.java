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
 * @see javax.lang.model.element.Modifier
 */
public enum ArModifier {
   /**
    * The {@code private} modifier.
    * 
    * @see javax.lang.model.element.Modifier#PRIVATE
    * @see java.lang.reflect.Modifier#PRIVATE
    * @see java.lang.reflect.Modifier#isPrivate(int)
    */
   PRIVATE(Modifier.PRIVATE, javax.lang.model.element.Modifier.PRIVATE),

   /**
    * The {@code protected} modifier.
    * 
    * @see javax.lang.model.element.Modifier#PROTECTED
    * @see java.lang.reflect.Modifier#PROTECTED
    * @see java.lang.reflect.Modifier#isProtected(int)
    */
   PROTECTED(Modifier.PROTECTED, javax.lang.model.element.Modifier.PROTECTED),
   
   /**
    * The {@code public} modifier.
    * 
    * @see javax.lang.model.element.Modifier#PUBLIC
    * @see java.lang.reflect.Modifier#PUBLIC
    * @see java.lang.reflect.Modifier#isPublic(int)
    */
   PUBLIC(Modifier.PUBLIC, javax.lang.model.element.Modifier.PUBLIC),

   /**
    * A psuedo-modifier that represents package-private (aka "default") access. It's not an
    * actual modifier in Java but rather indicates the absence of {@code private},
    * {@code protected}, or {@code public} modifiers. It's represented by an enum here to make
    * testing for this type of access easier and more explicit.
    */
   PACKAGE_PRIVATE(0, null),
   
   /**
    * The {@code static} modifier.
    * 
    * @see javax.lang.model.element.Modifier#STATIC
    * @see java.lang.reflect.Modifier#STATIC
    * @see java.lang.reflect.Modifier#isStatic(int)
    */
   STATIC(Modifier.STATIC, javax.lang.model.element.Modifier.STATIC),

   /**
    * The {@code abstract} modifier.
    * 
    * @see javax.lang.model.element.Modifier#ABSTRACT
    * @see java.lang.reflect.Modifier#ABSTRACT
    * @see java.lang.reflect.Modifier#isAbstract(int)
    */
   ABSTRACT(Modifier.ABSTRACT, javax.lang.model.element.Modifier.ABSTRACT),

   /**
    * The {@code default} modifier.
    * 
    * @see javax.lang.model.element.Modifier#DEFAULT
    */
   DEFAULT(0, javax.lang.model.element.Modifier.DEFAULT),

   /**
    * The {@code final} modifier.
    * 
    * @see javax.lang.model.element.Modifier#FINAL
    * @see java.lang.reflect.Modifier#FINAL
    * @see java.lang.reflect.Modifier#isFinal(int)
    */
   FINAL(Modifier.FINAL, javax.lang.model.element.Modifier.FINAL),

   /**
    * The {@code transient} modifier.
    * 
    * @see javax.lang.model.element.Modifier#TRANSIENT
    * @see java.lang.reflect.Modifier#TRANSIENT
    * @see java.lang.reflect.Modifier#isTransient(int)
    */
   TRANSIENT(Modifier.TRANSIENT, javax.lang.model.element.Modifier.TRANSIENT),

   /**
    * The {@code volatile} modifier.
    * 
    * @see javax.lang.model.element.Modifier#VOLATILE
    * @see java.lang.reflect.Modifier#VOLATILE
    * @see java.lang.reflect.Modifier#isVolatile(int)
    */
   VOLATILE(Modifier.VOLATILE, javax.lang.model.element.Modifier.VOLATILE),

   /**
    * The {@code native} modifier.
    * 
    * @see javax.lang.model.element.Modifier#NATIVE
    * @see java.lang.reflect.Modifier#NATIVE
    * @see java.lang.reflect.Modifier#isNative(int)
    */
   NATIVE(Modifier.NATIVE, javax.lang.model.element.Modifier.NATIVE),

   /**
    * The {@code strictfp} modifier.
    * 
    * @see javax.lang.model.element.Modifier#STRICTFP
    * @see java.lang.reflect.Modifier#STRICT
    * @see java.lang.reflect.Modifier#isStrict(int)
    */
   STRICTFP(Modifier.STRICT, javax.lang.model.element.Modifier.STRICTFP),

   /**
    * The {@code synchronized} modifier.
    * 
    * @see javax.lang.model.element.Modifier#SYNCHRONIZED
    * @see java.lang.reflect.Modifier#SYNCHRONIZED
    * @see java.lang.reflect.Modifier#isSynchronized(int)
    */
   SYNCHRONIZED(Modifier.SYNCHRONIZED, javax.lang.model.element.Modifier.SYNCHRONIZED);
   
   private final static EnumMap<javax.lang.model.element.Modifier, ArModifier> elementToMod =
         new EnumMap<>(javax.lang.model.element.Modifier.class);
   private final static Map<Integer, ArModifier> intToMod = new HashMap<>();
   static {
      for (ArModifier m : values()) {
         javax.lang.model.element.Modifier e = m.asElementModifier();
         if (e != null) {
            elementToMod.put(e, m);
         }
         int i = m.asInt();
         if (i != 0) {
            intToMod.put(i, m);
         }
      }
   }
   
   private final int intMod;
   private final javax.lang.model.element.Modifier elementMod;
   
   ArModifier(int intMod, javax.lang.model.element.Modifier elementMod) {
      this.intMod = intMod;
      this.elementMod = elementMod;
   }

   /**
    * Returns the corresponding bit mask for this modifier.
    * 
    * @return the bit mask for this modifier
    */
   public int asInt() {
      return intMod;
   }
   
   /**
    * Returns the corresponding {@link javax.lang.model.element.Modifier javax.lang.model.element.Modifier}.
    * 
    * @return the corresponding {@code javax.lang.model.element.Modifier}
    */
   public javax.lang.model.element.Modifier asElementModifier() {
      return elementMod;
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
         ret |= mod.asInt();
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
   
   // TODO(jh): doc
   public static EnumSet<ArModifier> fromBitFieldWithVisibility(int modifiers) {
      EnumSet<ArModifier> ret = fromBitfield(modifiers);
      if (!ret.contains(ArModifier.PUBLIC) && !ret.contains(ArModifier.PROTECTED)
            && !ret.contains(ArModifier.PRIVATE)) {
         ret.add(ArModifier.PACKAGE_PRIVATE);
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

   // TODO(jh): doc
   public static EnumSet<ArModifier> fromElementModifiersWithVisibility(
         Set<javax.lang.model.element.Modifier> mods) {
      EnumSet<ArModifier> ret = fromElementModifiers(mods);
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
