package com.bluegosling.generator;

/**
 * An exception thrown during {@linkplain Generator generation} of a sequence of elements. This
 * exception indicates that generation is complete and there are no more elements in the
 * sequence.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public class SequenceFinishedException extends RuntimeException {
   private static final long serialVersionUID = 1224425258285484624L;

   public SequenceFinishedException() {
      super();
   }

   public SequenceFinishedException(String message) {
      super(message);
   }
   
   public SequenceFinishedException(Throwable cause) {
      super(cause);
   }
   
   public SequenceFinishedException(String message, Throwable cause) {
      super(message, cause);
   }
}
