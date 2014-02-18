package com.apriori.apt;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * An annotation used to indicate what annotation types an annotation processor supports. This
 * annotation can be used, along with the similar {@link SupportedAnnotationTypes} annotation,
 * to declaratively define the annotation types.
 *
 * @see AbstractProcessor#getSupportedAnnotationTypes()
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedAnnotationClasses {
   /**
    * The set of supported annotation types, indicated via their class tokens.
    */
   Class<? extends Annotation>[] value();
}
