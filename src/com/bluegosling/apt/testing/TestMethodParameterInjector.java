package com.bluegosling.apt.testing;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Utility for injecting arguments into test methods. This allows tests to define various incoming
 * parameters to receive information about the processing environment. The methods on this interface
 * are used to inject values for the parameters at runtime.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <C> a type that represents context for injection and supplies the injected values
 * 
 * @see TestEnvironment
 * @see TestMethodProcessor
 * @see AnnotationProcessorTestRunner
 */
interface TestMethodParameterInjector<C> {
   
   /**
    * Gets the parameter values to use for invoking the specified method.
    * 
    * @param m the method whose parameter values are returned
    * @param context the current context which contains values that will be injected
    * @return parameter values, injected from the context
    * @throws IllegalArgumentException if the specified method contains an argument whose
    *       type cannot be injected
    * @throws ClassCastException if the specified method accepts an argument whose type
    *       could be injected but isn't actually cast-able from the runtime type of
    *       the object in context
    */
   Object[] getInjectedParameters(Method m, C context);
   
   /**
    * Validates that the specified method has injectable parameter types. If it
    * has invalid types, {@link IllegalArgumentException}s will be added to the
    * specified list of errors that contain messages describing the invalid
    * argument.
    * 
    * @param m the method to verify
    * @param errors the list of errors to which to add validation errors
    */
   void validateParameterTypes(Method m, List<Throwable> errors);
   
}
