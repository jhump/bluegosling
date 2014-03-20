package com.apriori.reflect;

import java.lang.reflect.Array;
import java.util.function.Function;

// TODO: javadoc
public final class Converters {
   private Converters() {}
   
   public static <I, O> Converter<I, O> fromFunction(Function<I, O> function) {
      return (o, caster) -> function.apply(o);
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
