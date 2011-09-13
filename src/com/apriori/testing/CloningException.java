package com.apriori.testing;

/**
 * An exception that occurs during a cloning operation. This
 * is a {@code RuntimeException} so that the use of
 * {@link Cloner#clone(Object)} doesn't require extra
 * boiler-plate for {@code throws} declarations or
 * {@code try-catch} blocks.
 * 
 * @author jhumphries
 */
public class CloningException extends RuntimeException {

   private static final long serialVersionUID = -3746546051862930866L;

   /**
    * Constructs a new exception with a message.
    * 
    * @param message the exception message
    */
   public CloningException(String message) {
      super(message);
   }

   /**
    * Constructs a new exception chained to another.
    * 
    * @param cause the cause of the cloning exception
    */
   public CloningException(Throwable cause) {
      super(cause);
   }

   /**
    * Constructs a new exception with a message that is chained
    * to another.
    * 
    * @param message the exception message
    * @param cause the cause of the cloning exception
    */
   public CloningException(String message, Throwable cause) {
      super(message, cause);
   }
}
