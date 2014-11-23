package com.apriori.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for an injectable wrapper method. The method should not be static and should
 * be abstract. A concrete version of the function will be generated in the {@linkplain
 * InjectedEntryPoint injection entry point}. If the function has arguments, they should have the
 * same types and qualifier annotations as the called method's injection dependencies. Any other
 * dependencies not found in the argument list will be automatically injected.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Calls {
   /**
    * The name of the method that will be called in the generated injection entry point. The
    * name should be specified in the same syntax as used by javadoc <code>{{@literal @}link}</code>s
    * with the one exception that inner classes should be denoted using dollar signs ({@code $})
    * instead of dots (as if referencing the class name for use in {@link Class#forName(String)}).
    * As in javadoc, a hash ({@code #}) should be used to separate the class from the
    * method. Types in the parameter list do not need to be fully-qualified as long as
    * no ambiguity results from using short names. The parameter list can be left out
    * completely if the named method is not overloaded. A value that begins with a hash
    * is assumed to be a member of the current class (just like in javadoc).
    */
   String value();
}