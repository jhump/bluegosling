package com.apriori.reflect;

// TODO: doc!
public class DispatchSettings {
   private final boolean castReturnTypes;
   private final boolean castArguments;
   private final boolean expandVarArgs;
   private final boolean ignoreAmbiguities;
   
   public DispatchSettings(boolean castReturnTypes, boolean castArguments, boolean expandVarArgs,
         boolean ignoreAmbiguities) {
      this.castReturnTypes = castReturnTypes;
      this.castArguments = castArguments;
      this.expandVarArgs = expandVarArgs;
      this.ignoreAmbiguities = ignoreAmbiguities;
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
   
   
}
