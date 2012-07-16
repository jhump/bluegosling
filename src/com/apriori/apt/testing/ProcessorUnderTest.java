package com.apriori.apt.testing;

import org.junit.Before;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.processing.Processor;

/**
 * Indicates the type of processor to test. An instance of this type will be
 * instantiated and initialized automatically as part of setting up the
 * compiler environment when executing a test method.
 *
 * <p>This annotation should <em>not</em> be used in conjunction with {@link InitializeProcessorField}.
 * Use one or the other. This annotation is generally more useful. But if you need to construct or
 * initialize the processor programmatically in a {@link Before @Before} method, use the other
 * annotation.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see AnnotationProcessorTestRunner
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ProcessorUnderTest {
   /**
    * The processor class that will be tested.
    */
   Class<? extends Processor> value();
}
