package com.apriori.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

/**
 * Describes a "TODO" task for the annotated element. Usages should either specify the
 * {@link #value} attribute or the {@link #tasks} attribute. The latter is used when a single
 * element has more than one associated "TODO" task.
 * 
 * <p>
 * Examples:<pre>
 * {@literal @}ToDo("implement serialization and cloning")
 * class MyCoolClazz {
 * }
 * 
 * {@literal @}ToDo(tasks = {
 *    {@literal @}Task("add tests!"),
 *    {@literal @}Task("reconcile method names so API is more consistent"),
 *    {@literal @}Task("add static helper methods for x, y, and z")
 * })
 * class MyCoolerClazz {
 * }
 * </pre>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Documented
@ToDo("Implement annotation processor that can emit TODOs during compilation, possibly as\n"
		+ "warnings. That processor should also emit errors on malformed ToDo annotations. An\n"
      + "annotation is malformed if it has neither a value nor a tasks attribute, if it has both,\n"
		+ "or if it has ambiguous who attributes (e.g. at both ToDo level and at Task level)")
public @interface ToDo {
   /**
    * A description of the task that needs to be done.
    */
   String value() default "";
   
   /**
    * An optional name of a person responsible for this task. If the {@link #tasks} attribute is
    * present (indicating multiple tasks) then this can indicate the name of a single individual
    * responsible for all. If tasks have different responsible individuals, then this should be
    * omitted and {@link Task#who()} attributes should be used instead.
    */
   String who() default "";
   
   /**
    * Multiple "TODO" tasks. When a single annotated element has multiple associated "TODO" tasks,
    * this attribute should be used to define all of them instead of using {@link #value} and
    * {@link #who}, which are suitable for describing a single task.
    */
   Task[] tasks() default {};
   
   /**
    * A "TODO" task. This is useful when a single annotated element has multiple "TODO" tasks
    * associates with it.
    * 
    * @see ToDo#tasks()
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @Target({}) // can only be enclosed in ToDo and not used by itself on any element
   public @interface Task {
      /**
       * A description of the task that needs to be done.
       */
      String value();
      
      /**
       * An optional name of a person responsible for this task.
       */
      String who() default "";
   }
}
