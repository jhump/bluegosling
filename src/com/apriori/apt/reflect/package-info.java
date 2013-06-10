/**
 * This package provides an alternate set of APIs for interacting with annotations and types from an
 * annotation processor. The layout of classes and interfaces here closely resembles those in
 * {@code java.lang.reflect}, which many programmers will feel are more natural and easier to use
 * than the APIs provided in the {@code javax.lang.model.element} and {@code java.lang.model.type}
 * packages.
 * 
 * <p>Most controversial among the class names here is certainly that of
 * {@link com.apriori.apt.reflect.Class}, since it "collides" with that of {@link java.lang.Class
 * java.lang.Class}. At the possible cost of confusion with the APIs in {@code java.lang.reflect},
 * these names were chosen to make the code read like reflective code. Also, since types being
 * processed by an annotation processor typically can't be represented as runtime types (since they
 * aren't yet compiled), code is unlikely to mix references to {@code java.lang.reflect} and
 * {@code com.apriori.apt.reflect}, so the name shouldn't cause problems in the way of import
 * conflicts or requiring a lot of fully-qualified type name references.
 * 
 * <p>To make the most of the APIs in this package, your annotation processor should extend
 * {@link com.apriori.apt.AbstractProcessor} (instead of {@link javax.annotation.processing.AbstractProcessor
 * javax.annotation.processing.AbstractProcessor}). This alternate base class provides a reference to
 * a {@link com.apriori.apt.RoundEnvironment}, which is very similar to the standard {@link
 * javax.annotation.processing.RoundEnvironment javax.annotation.processing.RoundEnvironment} except
 * that its API is defined in terms of this set of reflection APIs (instead of in terms of elements
 * and type mirrors).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.apt.reflect;