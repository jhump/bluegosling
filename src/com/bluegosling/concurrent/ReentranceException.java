package com.bluegosling.concurrent;

/**
 * An exception indicating that a reentrant lock acquisition was attempted but is not unsupported.
 *
 * <p>Most non-reentrant synchronization mechanisms lack the book-keeping necessary to throw such an
 * exception. But for those that can do the book-keeping but disallow re-entrance for some other
 * reason, this exception can be thrown.
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
