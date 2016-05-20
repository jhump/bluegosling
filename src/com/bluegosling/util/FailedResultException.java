package com.bluegosling.util;

/**
 * An exception used to wrap the error in a {@link Result}, for result objects whose cause of
 * failure is not a sub-type of {@link Throwable}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FailedResultException extends RuntimeException {
   private static final long serialVersionUID = -1899493906776184603L;
   
   private final Object failure;
   
   FailedResultException(Object failure) {
      super("Failed result: " + failure);
      this.failure = failure;
   }
   
   public Object getFailure() {
      return failure;
   }
}
