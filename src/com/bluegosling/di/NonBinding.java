package com.bluegosling.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * A marker of attributes on {@link Qualifier}s and {@link OptionalQualifier}s.
 * If present, those attributes are ignored when comparing two annotations for
 * equality as bind keys.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NonBinding {
}
