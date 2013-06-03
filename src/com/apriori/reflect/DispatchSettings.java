package com.apriori.reflect;

import java.lang.reflect.Modifier;

// TODO: doc!
public class DispatchSettings {
   
   public enum Visibility {
      PUBLIC() {
         @Override
         public boolean isVisible(int modifiers) {
            return Modifier.isPublic(modifiers);
         }
      },
      PROTECTED() {
         @Override
         public boolean isVisible(int modifiers) {
            return (modifiers & PROTECTED_OR_HIGHER) != 0;
         }
      },
      PACKAGE_PRIVATE() {
         @Override
         public boolean isVisible(int modifiers) {
            return (modifiers & PROTECTED_OR_HIGHER) != 0
                  || (modifiers & NOT_PACKAGE_PRIVATE) == 0;
         }
      },
      PRIVATE() {
         @Override
         public boolean isVisible(int modifiers) {
            // everything is private or higher
            return true;
         }
      };
      
      private static final int PROTECTED_OR_HIGHER = Modifier.PUBLIC | Modifier.PROTECTED;
      private static final int NOT_PACKAGE_PRIVATE = PROTECTED_OR_HIGHER | Modifier.PRIVATE;
      
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
