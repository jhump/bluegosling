package com.apriori.util;

/**
 * A sequence that does not throw checked exceptions from calls to {@link #next()}.
 *
 * @param <T> the type of elements in the sequence
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface UncheckedSequence<T> extends Sequence<T, RuntimeException> {
}
