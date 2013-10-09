package com.apriori.concurrent;

/**
 * An unchecked exception that is thrown when a lock acquisition is attempted that would result in
 * deadlock.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class DeadlockException extends RuntimeException {
   public DeadlockException() {
   }

   public DeadlockException(String message) {
      super(message);
   }

   public DeadlockException(String message, Throwable cause) {
      super(message, cause);
   }

   public DeadlockException(Throwable cause) {
      super(cause);
   }
}
