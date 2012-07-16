package com.apriori.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * An annotation that represents an optional qualifier. This is like
 * {@link Qualifier} except that it indicates that it is not an error if
 * no explicit binding is provided for the qualifier. In such a case, a
 * binding will be used that does not contain the qualifier in the key.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OptionalQualifier {
}