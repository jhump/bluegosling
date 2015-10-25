/**
 * Utility classes for unsafe things. This includes a way to access and use {@link sun.misc.Unsafe}.
 * 
 * <p>It also includes several classes that provide atomic access to fields within a class (like
 * using {@code java.util.concurrent.atomic}, but being able to lay the fields out inside of an
 * object instead of chasing a pointer). These are similar to the field updaters in
 * {@code java.util.concurrent.atomic} but they can be unsafe in that they rely on the compiler's
 * enforcement of the type system and do not perform per-access runtime checks. If unchecked casts
 * are used with them, for example, the heap could be irreparably damaged/corrupted. 
 */
package com.apriori.concurrent.unsafe;