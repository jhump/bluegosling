package com.apriori.apt.testing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * An exception thrown if compilation fails. If a test case fails due to a problem in the
 * Java compilation step (vs. an exception or assertion in the test case itself), an
 * instance of this exception is thrown.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@SuppressWarnings("serial")
public class CompilationFailedException extends RuntimeException {

   private final Collection<Diagnostic<JavaFileObject>> diagnostics;

   /**
    * Creates a new exception with the specified message and set of error diagnostics.
    * 
    * @param message an error message
    * @param diagnostics diagnostics emitted by the compiler with a kind of
    *       {@link javax.tools.Diagnostic.Kind#ERROR}
    */
   public CompilationFailedException(String message, Collection<Diagnostic<JavaFileObject>> diagnostics) {
      super(message);
      this.diagnostics = new ArrayList<Diagnostic<JavaFileObject>>(diagnostics);
   }
   
   /**
    * Creates a new exception with the specified message, cause, and set of error diagnostics.
    * 
    * @param message an error message
    * @param cause the cause of the failure
    * @param diagnostics diagnostics emitted by the compiler with a kind of
    *       {@link javax.tools.Diagnostic.Kind#ERROR}
    */
   public CompilationFailedException(String message, Throwable cause, Collection<Diagnostic<JavaFileObject>> diagnostics) {
      super(message, cause);
      this.diagnostics = new ArrayList<Diagnostic<JavaFileObject>>(diagnostics);
   }
   
   /**
    * Creates a new exception with the specified cause and set of error diagnostics.
    * 
    * @param cause the cause of the failure
    * @param diagnostics diagnostics emitted by the compiler with a kind of
    *       {@link javax.tools.Diagnostic.Kind#ERROR}
    */
   public CompilationFailedException(Throwable cause, Collection<Diagnostic<JavaFileObject>> diagnostics) {
      super(cause);
      this.diagnostics = new ArrayList<Diagnostic<JavaFileObject>>(diagnostics);
   }
   
   /**
    * Retrieves the error diagnostics associated with this compilation failure.
    * 
    * @return the set of error diagnostics emitted from the compiler
    */
   public Collection<Diagnostic<JavaFileObject>> getDiagnostics() {
      return Collections.unmodifiableCollection(diagnostics);
   }
}
