package com.bluegosling.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for injection. Injection is done by generating a sub-class of the
 * marked class. The generated sub-class can have various abstract methods implemented
 * with generated code that injects the appropriate instances into other methods,
 * constructors, etc. Since a sub-class will be generated for marked classes, it is
 * an error to use this annotation on final classes. It is okay to use this annotation
 * on interfaces.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Main
 * @see Factory
 * @see Calls
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectedEntryPoint {
   /**
    * The name of the generated class. If it does not include a package name then the
    * current package is assumed.
    */
   String value();
   
   /**
    * Optional list of other {@link InjectedEntryPoint} classes whose bindings and
    * providers should be used to generated injection bindings for this class.
    */
   Class<?>[] inheritBindingsFrom() default {};
}
