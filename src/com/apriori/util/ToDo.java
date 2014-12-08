package com.apriori.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a "TODO" task for the annotated element.
 * 
 * <p>
 * Examples:<pre>
 * {@literal @}ToDo("implement serialization and cloning")
 * class MyCoolClazz {
 * }
 * 
 * {@literal @}ToDo("add tests!"),
 * {@literal @}ToDo("reconcile method names so API is more consistent"),
 * {@literal @}ToDo("add static helper methods for x, y, and z")
 * class MyCoolerClazz {
 * }
 * </pre>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.PACKAGE,
   ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD })
@Documented
@Repeatable(ToDos.class)
@ToDo("Implement annotation processor that emits TODOs during compile, possibly as warnings.")
@ToDo("Update TODO comments in this project to use @ToDo annotations instead.")
public @interface ToDo {
   /**
    * A description of the task that needs to be done.
    */
   String value();
   
   /**
    * An optional name of a person responsible for this task.
    */
   String who() default "";
}
