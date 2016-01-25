package com.apriori.apt.testing;

/**
 * A {@linkplain CheckedProcessingTask processing task} that does not throw a checked exception.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@FunctionalInterface
public interface ProcessingTask<T> extends CheckedProcessingTask<T, RuntimeException> {
}
