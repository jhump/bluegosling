package com.apriori.apt;

import javax.lang.model.util.Elements;

// TODO: javadoc!
public final class ElementUtils {

   private static final ThreadLocal<Elements> threadLocal = new ThreadLocal<Elements>();
   
   static void set(Elements elements) {
      threadLocal.set(elements);
   }
   
   public static Elements get() {
      Elements elements = threadLocal.get();
      if (elements == null) {
         throw new IllegalArgumentException("ElementUtils has not been setup on this thread");
      }
      return elements;
   }
   
   /** Prevents instantiation. */
   private ElementUtils() {
   }
}
