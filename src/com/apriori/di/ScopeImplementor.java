package com.apriori.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 * An annotation on a {@link Scope} that indicates an associated implementation.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScopeImplementor {
   /**
    * The class that implements this scope by default. It can be overridden by
    * providing another implementation with the {@link UsingScope} annotation.
    */
   Class<? extends Scoper> value();
}