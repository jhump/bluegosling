package com.bluegosling.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for an injectable factory method. The method should not be static and should
 * be abstract. A concrete version of the function will be generated in the {@linkplain
 * InjectedEntryPoint injection entry point}. The function can have no arguments. If it
 * does have arguments, they should have the same types and binding annotations as the
 * returned type's injection dependencies. Any other dependencies not found in the argument
 * list will be automatically injected.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Factory {
}
