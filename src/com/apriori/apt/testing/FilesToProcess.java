package com.apriori.apt.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a test class or method that defines the files to process.
 * 
 * <p>When using {@link AnnotationProcessorTestRunner} to run a test, these
 * annotations are used to define the input files in a {@link TestJavaFileManager}
 * that provides the mock file system for the annotation processors' environment.
 * The named file(s) must be retrievable using {@link Class#getResource(String)}
 * and thus must be on the class path.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FilesToProcess {
   /**
    * A set of resources to process. These will be loaded into the in-memory file system
    * prior to starting the processing environment. Any Java source files specified (i.e.
    * file names ending in {@code ".java"}) will be specified as inputs to the Java
    * compiler.
    * @return the set of resources to process
    */
   InputFiles[] value();
   
   /**
    * A flag indicating whether the named files <em>replace</em> the current set
    * of files or are <em>appended</em> to them. This is only used for annotated methods.
    * If this flag is false, any files defined by an annotation on the test <em>class</em>
    * are ignored and only the files named on the method are used. But, if true,
    * the method's set of files is incremental, meaning they are in addition to
    * the files named on the class.
    * @return true if the given resources are incremental to those named on the test class
    */
   boolean incremental() default false;
}
