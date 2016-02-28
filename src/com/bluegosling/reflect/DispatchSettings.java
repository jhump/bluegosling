package com.bluegosling.reflect;

import java.lang.reflect.Modifier;

/**
 * Settings related to dispatch of interface methods when using a {@link Caster}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Caster
 */
// TODO: doc!
public class DispatchSettings {
   
   /**
    * A level of method visibility, for example {@code public}, {@code private}, etc.
    */
   public enum Visibility {
      /**
       * A method that is marked with the keyword {@code private}.
       */
      PRIVATE() {
         @Override
         public boolean isVisible(int modifiers) {
            // everything is private or higher
            return true;
         }
      },
      
      /**
       * A method with no visibility keyword, aka default visibility or "package protected".
       */
      PACKAGE_PRIVATE() {
         @Override
         public boolean isVisible(int modifiers) {
            // if not private, then it is package-private or higher
            return !Modifier.isPrivate(modifiers);
         }
      },
      
      /**
       * A method that is marked with the keyword {@code protected}.
       */
      PROTECTED() {
         @Override
         public boolean isVisible(int modifiers) {
            return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
         }
      },
      
      /**
       * A method that is marked with the keyword {@code public}.
       */
      PUBLIC() {
         @Override
         public boolean isVisible(int modifiers) {
            return Modifier.isPublic(modifiers);
         }
      };
      
      /**
       * Returns true if the given modifiers indicate a method that has this level of visibility
       * or higher. Private is the lowest level of visibility, so every method has that level of
       * visibility or higher. Public is the highest level.
       */
      public abstract boolean isVisible(int modifiers);
   }
   
   private final boolean castReturnTypes;
   private final boolean castArguments;
   private final boolean expandVarArgs;
   private final boolean ignoreAmbiguities;
   private final Visibility visibility;
   
   DispatchSettings(boolean castReturnTypes, boolean castArguments, boolean expandVarArgs,
         boolean ignoreAmbiguities, Visibility visibility) {
      this.castReturnTypes = castReturnTypes;
      this.castArguments = castArguments;
      this.expandVarArgs = expandVarArgs;
      this.ignoreAmbiguities = ignoreAmbiguities;
      this.visibility = visibility;
   }

   public boolean isCastingReturnTypes() {
      return castReturnTypes;
   }

   public boolean isCastingArguments() {
      return castArguments;
   }
   
   public boolean isExpandingVarArgs() {
      return expandVarArgs;
   }
   
   public boolean isIgnoringAmbiguities() {
      return ignoreAmbiguities;
   }
   
   public Visibility visibility() {
      return visibility;
   }
}
