package com.bluegosling.reflect.caster;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A conversion strategy, which indicates how dispatch of interface methods from a {@link Caster}
 * will convert one type to another if need be.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Caster
 */
// TODO: javadoc members
class ConversionStrategy<I, O> {
   
   /**
    * A simple conversion pair. This is just a pair of types: "from" and "to". These are used as
    * keys in a map of implicit numeric primitive conversions.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   // TODO: javadoc members
   private static class Conversion<I, O> {
      public final Class<I> from;
      public final Class<O> to;
   
      Conversion(Class<I> from, Class<O> to) {
         this.from = from;
         this.to = to;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof Conversion) {
            Conversion<?, ?> other = (Conversion<?, ?>) o;
            return from.equals(other.from) && to.equals(other.to);
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return from.hashCode() * 31 + to.hashCode();
      }
   }
   
   // TODO: javadoc these static fields
   private static final Map<Class<?>, Class<?>> autoBoxTypes = new HashMap<Class<?>, Class<?>>();
   private static final Map<Class<?>, Class<?>> autoUnboxTypes = new HashMap<Class<?>, Class<?>>();
   private static final Map<Conversion<?, ?>, Function<?, ?>> conversions =
         new HashMap<Conversion<?, ?>, Function<?, ?>>();
   
   /*
    * Populate the three maps above.
    */
   static {
      autoBoxTypes.put(void.class, Void.class);
      autoBoxTypes.put(boolean.class, Boolean.class);
      autoBoxTypes.put(byte.class, Byte.class);
      autoBoxTypes.put(char.class, Character.class);
      autoBoxTypes.put(short.class, Short.class);
      autoBoxTypes.put(int.class, Integer.class);
      autoBoxTypes.put(long.class, Long.class);
      autoBoxTypes.put(float.class, Float.class);
      autoBoxTypes.put(double.class, Double.class);
      
      autoUnboxTypes.put(Void.class, void.class);
      autoUnboxTypes.put(Boolean.class, boolean.class);
      autoUnboxTypes.put(Byte.class, byte.class);
      autoUnboxTypes.put(Character.class, char.class);
      autoUnboxTypes.put(Short.class, short.class);
      autoUnboxTypes.put(Integer.class, int.class);
      autoUnboxTypes.put(Long.class, long.class);
      autoUnboxTypes.put(Float.class, float.class);
      autoUnboxTypes.put(Double.class, double.class);
      
      addConversion(byte.class, short.class, new Function<Byte, Short>() {
         @Override public Short apply(Byte input) {
            return (short) input.byteValue();
         }
      });
      addConversion(byte.class, int.class, new Function<Byte, Integer>() {
         @Override public Integer apply(Byte input) {
            return (int) input.byteValue();
         }
      });
      addConversion(byte.class, long.class, new Function<Byte, Long>() {
         @Override public Long apply(Byte input) {
            return (long) input.byteValue();
         }
      });
      addConversion(byte.class, float.class, new Function<Byte, Float>() {
         @Override public Float apply(Byte input) {
            return (float) input.byteValue();
         }
      });
      addConversion(byte.class, double.class, new Function<Byte, Double>() {
         @Override public Double apply(Byte input) {
            return (double) input.byteValue();
         }
      });
      addConversion(char.class, int.class, new Function<Character, Integer>() {
         @Override public Integer apply(Character input) {
            return (int) input.charValue();
         }
      });
      addConversion(char.class, long.class, new Function<Character, Long>() {
         @Override public Long apply(Character input) {
            return (long) input.charValue();
         }
      });
      addConversion(char.class, float.class, new Function<Character, Float>() {
         @Override public Float apply(Character input) {
            return (float) input.charValue();
         }
      });
      addConversion(char.class, double.class, new Function<Character, Double>() {
         @Override public Double apply(Character input) {
            return (double) input.charValue();
         }
      });
      addConversion(short.class, int.class, new Function<Short, Integer>() {
         @Override public Integer apply(Short input) {
            return (int) input.shortValue();
         }
      });
      addConversion(short.class, long.class, new Function<Short, Long>() {
         @Override public Long apply(Short input) {
            return (long) input.shortValue();
         }
      });
      addConversion(short.class, float.class, new Function<Short, Float>() {
         @Override public Float apply(Short input) {
            return (float) input.shortValue();
         }
      });
      addConversion(short.class, double.class, new Function<Short, Double>() {
         @Override public Double apply(Short input) {
            return (double) input.shortValue();
         }
      });
      addConversion(int.class, long.class, new Function<Integer, Long>() {
         @Override public Long apply(Integer input) {
            return (long) input.intValue();
         }
      });
      addConversion(int.class, float.class, new Function<Integer, Float>() {
         @Override public Float apply(Integer input) {
            return (float) input.intValue();
         }
      });
      addConversion(int.class, double.class, new Function<Integer, Double>() {
         @Override public Double apply(Integer input) {
            return (double) input.intValue();
         }
      });
      addConversion(long.class, float.class, new Function<Long, Float>() {
         @Override public Float apply(Long input) {
            return (float) input.longValue();
         }
      });
      addConversion(long.class, double.class, new Function<Long, Double>() {
         @Override public Double apply(Long input) {
            return (double) input.longValue();
         }
      });
      addConversion(float.class, double.class, new Function<Float, Double>() {
         @Override public Double apply(Float input) {
            return (double) input.floatValue();
         }
      });
   }
   
   static <I, O> void addConversion(Class<I> from, Class<O> to, Function<I, O> converter) {
      conversions.put(new Conversion<I, O>(from, to), converter);
   }
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   static <I, O> Function<I, O> getConversion(Class<I> from, Class<O> to) {
      Function f = conversions.get(new Conversion<I, O>(from, to));
      return f;
   }
   
   private final Class<O> outputType;
   private boolean requiresCast;
   private boolean requiresAutoBoxOrUnbox;
   private Converter<? super I, ? extends O> converter;
   
   private ConversionStrategy(Class<O> outputType) {
      this.outputType = outputType;
   }
   
   void setRequiresCast() {
      requiresCast = true;
   }
   
   void setRequiresAutoBoxOrUnbox() {
      requiresAutoBoxOrUnbox  = true;
   }
   
   void setConverter(Converter<? super I, ? extends O> converter) {
      this.converter = converter;
   }
   
   boolean doesRequireCast() {
      return requiresCast;
   }
   
   boolean doesRequireAutoBoxOrUnbox() {
      return requiresAutoBoxOrUnbox;
   }
   
   Converter<? super I, ? extends O> getConverter() {
      return converter;
   }
   
   ConversionStrategy<I[], O[]> forArray() {
      @SuppressWarnings("unchecked")
      Class<O[]> arrayOutputType = (Class<O[]>) Array.newInstance(outputType, 0).getClass();
      ConversionStrategy<I[], O[]> newStrategy = new ConversionStrategy<I[], O[]>(arrayOutputType);
      newStrategy.requiresCast = this.requiresCast;
      newStrategy.requiresAutoBoxOrUnbox = this.requiresAutoBoxOrUnbox;
      if (this.converter != null) {
         newStrategy.converter = Converter.forArray(outputType, this.converter);
      }
      return newStrategy;
   }

   /**
    * Creates a conversion strategy for converting one type of value to another. If the first type
    * is assignable to the second then the conversion strategy is a no-op (since no conversion is
    * necessary). Supported conversions include "casting" one type to an interface type and numeric
    * type "widening" (i.e.g converting an int to a long).
    * 
    * @param from a source type
    * @param to a target type
    * @param castArguments if true then "casting" source types to target interface types is
    *       permitted
    * @return the conversion strategy
    */
   static <I, O> ConversionStrategy<I, O> getConversionStrategy(Class<I> from,
         final Class<O> to, boolean castArguments) {
      ConversionStrategy<I, O> strategy = new ConversionStrategy<I, O>(to);
      if (to.isAssignableFrom(from)) {
         return strategy;
      }
      // types aren't assignable, so we need to figure out if we can convert
      Function<I, O> function = getConversion(from, to);
      if (function != null) {
         strategy.setConverter(Converter.fromFunction(function));
      } else if (to.equals(autoBoxTypes.get(from)) || to.equals(autoUnboxTypes.get(from))) {
         // conversion not needed since reflection always resorts to boxed
         // types, but we need to know if auto-boxing/unboxing was required to
         // rank dispatch candidates
         strategy.setRequiresAutoBoxOrUnbox();
      } else if (castArguments && to.isInterface()) {
         strategy.setRequiresCast();
         strategy.setConverter(new Converter<I, O>() {
            @Override
            public O convert(I in, Caster<?> caster) {
               return caster.to(to).cast(in);
            }
         });
      } else {
         // incompatible!!
         return null;
      }
      return strategy;
   }
}
