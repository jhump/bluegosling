package com.bluegosling.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker interface for "value types". A value type is a concrete class that is both final and
 * immutable (but not an enum). In addition to not having sub-classes (since they are final), value
 * types may also not have super-classes (other than {@code Object}). Furthermore, a value type's
 * {@code equals}, {@code hashCode}, and {@code toString} methods should be implemented solely in
 * terms of its field values. 
 * 
 * <p>Value types could essentially passed around by value/copy instead of by reference. Code that
 * uses value types should not perform any operations that rely on the value's address. For example,
 * this means no testing for reference equality ({@code ==}), no usage of
 * {@link System#identityHashCode}, and no usage of the value as a synchronizer/lock.
 * 
 * <p>For more information, see the JRE's description of 
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/doc-files/ValueBased.html">
 * value-based classes</a>.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ValueType {
}
