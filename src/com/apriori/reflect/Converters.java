package com.apriori.reflect;

import com.apriori.util.Function;

import java.lang.reflect.Array;

// TODO: javadoc
public final class Converters {
   private Converters() {}
   
   public static <I, O> Converter<I, O> fromFunction(final Function<I, O> function) {
      return new Converter<I, O>() {
         @Override
         public O convert(I in, Caster<?> caster) {
            return function.apply(in);
         }
      };
   }

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
