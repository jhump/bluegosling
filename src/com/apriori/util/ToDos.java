package com.apriori.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A container for one or more "TODO" annotations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.PACKAGE,
   ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface ToDos {
   ToDo[] value();

   /**
    * An optional name of a person responsible for these tasks. This can indicate a single
    * individual responsible for all contained {@link ToDo} annotations. If the sub-tasks have
    * different responsible individuals, then this attribute should be omitted and
    * {@link ToDo#who()} attributes should be used instead.
    */
   String who() default "";
}
