package com.apriori.apt.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a test class or method that defines the list of options to use.
 * 
 * This is used by {@link AnnotationProcessorTestRunner} to determine what options to
 * pass to the Java compiler when creating the environment for an annotation processor
 * test.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface OptionsForProcessing {
   /**
    * The list of options.
    * @return the list of options
    */
   String[] value();
   
   /**
    * A flag indicating whether these options <em>replace</em> the current set of options
    * are <em>appended</em> to them. This is only used for annotated methods. If this flag
    * is false, any options defined by an annotation on the test <em>class</em> are ignored
    * and only the options defined on the method are used. But, if true, the method's set
    * of options is incremental, meaning they are appened to the list of options defined
    * on the class.
    * @return true if the given options are incremental to those defined on the test class
    */
   boolean incremental() default false;
}
