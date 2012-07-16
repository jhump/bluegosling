/**
 * This package provides an alternate set of APIs for interacting with annotations and types from an
 * annotation processor. The layout of classes and interfaces here closely resembles those in
 * {@code java.lang.reflect}, which many programmers will feel are more natural and easier to use than
 * the APIs provided in the {@code javax.lang.model.element} and {@code java.lang.model.type} packages.
 * 
 * <p>To make the most of the APIs in this package, your annotation processor should extend
 * {@link com.apriori.apt.AbstractProcessor} instead of {@link javax.annotation.processing.AbstractProcessor}.
 * This alternate base class provides a reference to a {@link com.apriori.apt.RoundEnvironment}, which is
 * very similar to the standard {@link javax.annotation.processing.RoundEnvironment} except that its API
 * is defined in terms of this set of reflection of APIs instead of elements and type mirrors. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.apt.reflect;