package com.apriori.apt.testing;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;

/**
 * An unchecked exception used to wrap exceptions that are thrown by a processor invocation (which
 * may include test code or a {@link ProcessingTask}. It must be unchecked since
 * {@link Processor#process(Set, RoundEnvironment)} cannot throw checked exceptions.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@SuppressWarnings("serial")
class ProcessorInvocationException extends RuntimeException {
   ProcessorInvocationException(Throwable cause) {
      super(requireNonNull(cause));
   }
}
