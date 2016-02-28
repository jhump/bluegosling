package com.bluegosling.di;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an injected constructor parameter or field as an optional dependency. If
 * an optional dependency cannot be satisfied (i.e it is not bound and cannot be
 * injected) then a {@code null} will be used instead when invoking a constructor.
 * When initializing a field, no action is taken (so the field will retain whatever
 * value existed after construction).
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Optional {
}
