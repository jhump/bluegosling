package com.bluegosling.di;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on an {@link InjectedEntryPoint} that binds a scope annotation to
 * an implementation. {@link Provision}s and {@link Binding}s can also be annotated
 * with this to indicate additional scopes defined in the entire transitive closure
 * of binding definitions.
 * 
 * <p>This overrides the scope annotation's own {@link ScopeImplementor}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface UsingScope {
   /**
    * The scope annotation that is to be bound.
    */
   Class<? extends Annotation> scope();
   
   /**
    * The scope implementation.
    */
   Class<? extends Scoper> impl();
   
   // TODO: javadoc
   @Target(ElementType.TYPE)
   @Retention(RetentionPolicy.RUNTIME)
   @interface Factory {
      Class<? extends Annotation> scope();
      Class<? extends ScoperFactory> impl();
   }
}
