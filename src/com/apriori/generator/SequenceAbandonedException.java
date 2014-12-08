package com.apriori.generator;

/**
 * An exception thrown in the generator thread to indicate that the corresponding sequence has been
 * abandoned. This means that the generator has no more consumers and the generator thread
 * should exit.
 * 
 * <p>Implementations of generators should generally not try to handle this exception but should
 * instead simply terminate gracefully, performing clean-up and releasing any held resources.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SequenceAbandonedException extends RuntimeException {
   private static final long serialVersionUID = -3179764007932633994L;

   SequenceAbandonedException() {
   }
}