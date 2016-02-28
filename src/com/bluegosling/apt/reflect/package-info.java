/**
 * This package provides an alternate set of APIs for interacting with annotations and types from an
 * annotation processor. The layout of classes and interfaces here closely resembles those in
 * {@code java.lang.reflect}, which many programmers will feel are more natural and easier to use
 * than the APIs provided in the {@code javax.lang.model.element} and {@code java.lang.model.type}
 * packages.
 * 
 * <p>All classes in this package have names that match similar classes in the standard reflection
 * APIs, but with an {@code Ar} prefix -- short for "<strong>A</strong>nnotation processing
 * <strong>R</strong>eflection". The prefix is meant to prevent import collisions with the
 * actual reflection types and to prevent confusion when looking at variable declarations.
 * 
 * <p>To make the most of the APIs in this package, your annotation processor should extend
 * {@link com.bluegosling.apt.reflect.ArProcessor}. This alternate base class provides a reference to
 * a {@link com.bluegosling.apt.reflect.ArRoundEnvironment}, which is very similar to the standard
 * {@link javax.annotation.processing.RoundEnvironment} except that its API is defined in terms of
 * this set of reflection APIs (instead of in terms of elements and type mirrors).
 * 
 * <p><strong>NOTE:</strong> APIs in this package requires the executing thread to be {@linkplain
 * com.bluegosling.apt.ProcessingEnvironments#setup(javax.annotation.processing.ProcessingEnvironment)
 * setup for the current processing environment}. If you are running from the same thread on which
 * the processor was invoked and the processor extends {@link com.bluegosling.apt.AbstractProcessor}
 * then this will have already been done for you.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.bluegosling.apt.reflect;
