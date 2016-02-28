package com.bluegosling.reflect;

import java.util.Collection;
import java.util.Collections;

// TODO: javadoc
public class BlasterException extends RuntimeException {
   
   private static final long serialVersionUID = -4332616623477889374L;

   public static class BlasterExceptionCause {
      private final Throwable cause;
      private final Object target;
      
      BlasterExceptionCause(Throwable cause, Object target) {
         this.cause = cause;
         this.target = target;
      }
      
      public Throwable getCause() {
         return cause;
      }
      
      public Object getTarget() {
         return target;
      }
   }
   
   private final Collection<BlasterExceptionCause> causes;
   
   BlasterException(Collection<BlasterExceptionCause> causes) {
      super("Operation failed for some target(s)");
      this.causes = Collections.unmodifiableCollection(causes);
   }
   
   public Collection<BlasterExceptionCause> getCauses() {
      return causes;
   }
}
