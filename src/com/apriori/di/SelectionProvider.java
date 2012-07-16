package com.apriori.di;

import javax.inject.Provider;

/**
 * An object that provides an injected value given a selector. This is
 * similar to {@link Provider} except that it requires a selector
 * parameter, for distinguishing between multiple instances of the
 * same type.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <T> the type of value provided
 */
public interface SelectionProvider<T> {
   T get(Object selector);
}
