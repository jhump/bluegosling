package com.apriori.apt.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a test class or method that defines the output files to validate.
 * 
 * <p>When using {@link AnnotationProcessorTestRunner} to run a test, these
 * annotations are used to describe expected output files. The named file(s) must be
 * retrievable using {@link Class#getResource(String)} and thus must be on the class
 * path. The contents of the specified files and resources must match exactly or the
 * test will fail.
 * 
 * <p>If you need more control over how an output is compared to its "golden"
 * representation, for certain types of leniency (like if the output can be ordering
 * agnostic, case-insensitive, whitepsace agnostic, etc.), you must perform comparisons
 * manually, and this annotation will not help.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see AnnotationProcessorTestRunner
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ValidateGeneratedFiles {
   /**
    * A set of resources to validate. After processing completes, the contents of these
    * resources will be compared to the contents of corresponding output files.
    */
   OutputFiles[] value();

   /**
    * A flag indicating whether the named files <em>replace</em> the current set
    * of files or are <em>appended</em> to them. This is only used for annotated methods.
    * If this flag is false, any files defined by an annotation on the test <em>class</em>
    * are ignored and only the files named on the method are used. But, if true,
    * the method's set of files is incremental, meaning they are in addition to
    * the files named on the class.
    */
   boolean incremental() default false;
}
