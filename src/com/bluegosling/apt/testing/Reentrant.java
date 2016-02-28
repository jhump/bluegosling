package com.bluegosling.apt.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a test method as being re-entrant. A re-entrant method may be invoked
 * more than once for a single run of the test case: once for each round of annotation
 * processing. If this annotation is absent, the method is only invoked during the
 * first round of processing.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Reentrant {
}
