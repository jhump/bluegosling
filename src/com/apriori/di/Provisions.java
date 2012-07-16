package com.apriori.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the {@link Provision}s to use for an {@link InjectedEntryPoint}.
 * {@link Provision}s and {@link Binding}s can also be annotated with this
 * to indicate additional {@link Provision}s to include in the entire
 * transitive closure of binding definitions.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: add more doc and example(s) to java doc above
@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Provisions {
   Class<? extends Provision>[] value();
}