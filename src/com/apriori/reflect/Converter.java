package com.apriori.reflect;

import java.lang.reflect.Array;
import java.util.function.Function;


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

   /**
    * Returns a converter that applies the given function on the object to convert and returns its
    * result. 
    *
    * @param function the function that performs the conversion
    * @return a converter that uses the given function
    */
   public static <I, O> Converter<I, O> fromFunction(Function<I, O> function) {
      return (o, caster) -> function.apply(o);
   }

   /**
    * Returns a converter that converts an array by using a given converter to convert each element
    * in the array. The result of conversion will be an array in which each element is the result
    * of applying the given converter to the corresponding element in the input array.
    *
    * @param outputComponentType the element type of the result of conversion.
    * @param converter the converter used to convert a single element
    * @return a converter that converts an array by using the given converter on each element
    */
   public static <I, O> Converter<I[], O[]> forArray(final Class<O> outputComponentType,
         final Converter<? super I, ? extends O> converter) {
      return new Converter<I[], O[]>() {
         @Override
         public O[] convert(I[] in, Caster<?> caster) {
            @SuppressWarnings("unchecked")
            O out[] = (O[]) Array.newInstance(outputComponentType, in.length);
            for (int i = 0, len = in.length; i < len; i++) {
               out[i] = converter.convert(in[i], caster);
            }
            return out;
         }
      };
   }
}