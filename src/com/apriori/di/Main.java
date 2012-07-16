package com.apriori.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for an injectable static main method. The method should <em>not</em> have
 * the standard {@code public static void main(String args[])} signature. In fact, it
 * should not be static. The instance will be constructed (and injected) and any needed
 * parameters are also injected. The generated {@linkplain InjectedEntryPoint injection
 * entry point} will have a {@code static main} method generated that creates the
 * instance and parameters and then invokes this method.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Main {
}