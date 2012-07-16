/**
 * APIs to simplify implementation of annotation processors and annotation processing tools.
 *
 * <p>Chief among the APIs is a reflection API that wraps the {@code javax.lang.model.element} and
 * {@code java.lang.model.type} APIs in a way that looks more like those in {@code java.lang.reflect}.
 * This is intended to make programming annotation processors simpler.
 * 
 * <p>Another major component here is a JUnit4 test runner that allows for unit testing of annotation
 * processors by running test cases in an actual annotation processing environment (by invoking the
 * Java compiler).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.apt;