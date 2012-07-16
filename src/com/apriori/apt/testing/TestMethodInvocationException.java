package com.apriori.apt.testing;

import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;

/**
 * An unchecked exception used to wrap exceptions that are thrown by test code.
 * It must be unchecked since {@link Processor#process(Set, RoundEnvironment)}
 * cannot throw checked exceptions.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@SuppressWarnings("serial")
class TestMethodInvocationException extends RuntimeException {
   TestMethodInvocationException(Throwable cause) {
      super(cause);
   }
}
