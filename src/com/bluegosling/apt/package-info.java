/**
 * APIs to simplify implementation of annotation processors and annotation processing tools.
 * 
 * <p>This package contains an {@link com.bluegosling.apt.AbstractProcessor} that feathers some more
 * convenience functionality on top of the standard
 * {@link javax.annotation.processing.AbstractProcessor}.
 * 
 * <p>Also included are two sub-packages, one with a reflection API that wraps
 * {@code javax.lang.model.element} and {@code java.lang.model.type} APIs and the other a JUnit4
 * test runner that allows for unit testing annotation processors from within an actual processing
 * environment.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.bluegosling.apt;
