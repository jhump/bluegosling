package com.bluegosling.concurrent.unsafe;

/**
 * A base class for classes that provide unsafe access to atomic operations on fields of another
 * type. This is mostly a marker class in the hierarchy.
 *
 * @param <T> the type that owns the fields acted on
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class UnsafeFieldUpdater<T> {
}
