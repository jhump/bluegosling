package com.apriori.reflect;


/**
 * Converts a value from one form to another, possibly with the aid of a {@link Caster}.
 *
 * @param <I> the input type of the converter
 * @param <O> the output type of the converter
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Converter<I, O> {
   /**
    * Converts an object.
    * 
    * @param in the object to convert
    * @param caster a {@link Caster}, in case needed for conversion
    * @return the converted result
    */
   O convert(I in, Caster<?> caster);
}