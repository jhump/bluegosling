package com.apriori.util;

/**
 * A sequence that does not throw checked exceptions from calls to {@link #next()}.
 *
 * @param <T> the type of elements in the sequence
 * @param <U> the optional type of input to the sequence ({@code Void} if not needed)
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface UncheckedSequence<T, U> extends Sequence<T, U, RuntimeException> {
}
