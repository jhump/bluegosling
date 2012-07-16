package com.apriori.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * A marker for command-line arguments. This should be used to annotate a
 * {@code List<String>} into which command-line arguments are injected.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Qualifier
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandLineArguments {
}