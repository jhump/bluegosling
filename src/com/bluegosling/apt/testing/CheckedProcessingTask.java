package com.bluegosling.apt.testing;

/**
 * A task that can be run from the context of the annotation processing phase of a compiler.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@FunctionalInterface
public interface CheckedProcessingTask<T, X extends Throwable> {
   /**
    * Runs the task. The specified parameter gives access to the current processing environment.
    *
    * @param env the environment
    * @return the result of the task
    * @throws X in the event of problems encountered while processing
    */
   T run(TestEnvironment env) throws X;
}
