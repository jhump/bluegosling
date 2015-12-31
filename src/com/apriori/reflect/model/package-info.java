/**
 * Extensions of elements and type mirrors for use with core reflection.
 * 
 * <p>The interfaces herein define new factory methods for adapting reflection APIs and
 * {@linkplain java.lang.Class class tokens} to the element and type mirror models. These can be
 * useful in annotation processors.
 * 
 * <p>But also herein is an implementation of element and type mirror models that are backed by core
 * reflection. This allows type-crawling / reflective code to be written once and then used from
 * either context:
 * <ul>
 * <li>From an annotation processor, using the models provided by the
 *    {@linkplain javax.annotation.processing.ProcessingEnvironment processing environment}.</li>
 * <li>From the runtime of a standard java application, using models backed by core reflection.</li>
 * </ul>
 * <p>This package should be a suitable implementation for
 * <a href="http://openjdk.java.net/jeps/119">JEP 119</a>. However, unlike the proposed JEP, this
 * package does not expose extra reflective operations, such as instantiating objects, invoking
 * methods, or querying/updating field values.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.reflect.model;

