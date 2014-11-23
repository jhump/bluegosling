package com.apriori.concurrent;

/**
 * An exception indicating that a reentrant lock acquisition was attempted but is not unsupported.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ReentranceException extends RuntimeException {
   private static final long serialVersionUID = 6514106228938676699L;

   public ReentranceException() {
   }

   public ReentranceException(String message) {
      super(message);
   }

   public ReentranceException(Throwable cause) {
      super(cause);
   }

   public ReentranceException(String message, Throwable cause) {
      super(message, cause);
   }
}
