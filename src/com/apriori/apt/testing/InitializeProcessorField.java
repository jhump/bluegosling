package com.apriori.apt.testing;

import org.junit.Before;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;

/**
 * Indicates the name of a field on the test class that holds a {@link Processor} that should be
 * {@linkplain Processor#init(ProcessingEnvironment) initialized} with the current
 * {@link ProcessingEnvironment} prior to running each test case.
 * 
 * <p>This annotation should <em>not</em> be used in conjunction with {@link ProcessorUnderTest}.
 * Use one or the other. This annotation is useful if you need to construct or initialize the
 * processor programmatically in a {@link Before @Before} method.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface InitializeProcessorField {
   /**
    * The name of a field on the annotated test class. The field's value, when running a test case,
    * must be an instance of {@link Processor}.
    */
   String value();
}
