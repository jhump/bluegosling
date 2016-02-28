package com.bluegosling.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a selector object. A selector object allows dynamic bindings. The
 * selector is provided at runtime and can be injected along with other parameters
 * to provision methods, constructors, or injected fields so that the appropriate
 * instance is used.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Selector {
}
