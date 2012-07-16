package com.apriori.apt;

import javax.lang.model.util.Types;

//TODO: javadoc!
public final class TypeUtils {

   private static final ThreadLocal<Types> threadLocal = new ThreadLocal<Types>();
   
   static void set(Types types) {
      threadLocal.set(types);
   }
   
   public static Types get() {
      Types types = threadLocal.get();
      if (types == null) {
         throw new IllegalArgumentException("TypeUtils has not been setup on this thread");
      }
      return types;
   }
   
   /** Prevents instantiation. */
   private TypeUtils() {
   }
}
