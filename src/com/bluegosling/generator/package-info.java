/**
 * Utilities for simulating generators in Java. These utilities aren't necessarily practical as they
 * work using multiple threads and transferring control back and forth, as opposed to sharing the
 * same thread by capturing and restoring the stack of the generator.
 * 
 * @see com.bluegosling.generator.Generator
 */
package com.bluegosling.generator;
