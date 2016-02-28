package com.bluegosling.concurrent.atoms;

/**
 * An unchecked exception thrown from non-idempotent transactions that encounter serialization
 * errors due to concurrent access.
 * 
 * <p>This exception will not happen for transactions whose isolation level is
 * {@linkplain Transaction.IsolationLevel#READ_COMMITTED read committed}. For other isolation
 * levels, it is possible that an atom is modified concurrently before an in-process transaction has
 * the chance to lock or pin it. When the in-process transaction then goes to lock or pin that atom,
 * it detects that the atom has been concurrently modified and that the transaction cannot continue
 * without violating its isolation level. Idempotent transactions will simply be retried. But
 * non-idempotent transactions cannot be retried, so this exception is thrown.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TransactionIsolationException extends RuntimeException {

   private static final long serialVersionUID = 3957146576789743305L;

   /**
    * Constructs a new exception with a {@code null} message and no cause.
    */
   public TransactionIsolationException() {
   }

   /**
    * Constructs a new exception with the given message and no cause.
    *
    * @param message a message
    */
   public TransactionIsolationException(String message) {
      super(message);
   }

   /**
    * Constructs a new exception with a {@code null} message and the given cause.
    *
    * @param cause the cause of this exception
    */
   public TransactionIsolationException(Throwable cause) {
      super(cause);
   }
   
   /**
    * Constructs a new exception with the given message and the given cause.
    *
    * @param message a message
    * @param cause the cause of this exception
    */
   public TransactionIsolationException(String message, Throwable cause) {
      super(message, cause);
   }
}
