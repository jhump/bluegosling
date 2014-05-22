package com.apriori.apt.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.tools.JavaCompiler;

/**
 * An annotation on a test class or method that defines the classes to process.
 * 
 * <p>When using {@link AnnotationProcessorTestRunner} to run a test, these
 * annotations are provided to the {@link JavaCompiler} as the set of classes whose
 * annotations should be processed.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ClassesToProcess {
   /**
    * The classes to process.
    * @return the classes to process
    */
   Class<?>[] value();
   
   /**
    * A flag indicating whether the named classes <em>replace</em> the current set
    * of classes or are <em>appended</em> to them. This is only used for annotated methods.
    * If this flag is false, any classes defined by an annotation on the test <em>class</em>
    * are ignored and only the classes named on the method are used. But, if true,
    * the method's set of classes is incremental, meaning they are in addition to
    * the classes named on the test class.
    * @return true if the given classes are incremental to those named on the test class
    */
   boolean incremental() default false;
}
