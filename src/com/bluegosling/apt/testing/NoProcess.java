package com.bluegosling.apt.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a test method that should be run as a normal test, not in the context of
 * annotation processing.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface NoProcess {
}
