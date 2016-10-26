package com.bluegosling.collections;

import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

//TODO: javadoc
//TODO: tests
public class TransformingMapEntry<KI, VI, KO, VO> implements Entry<KO, VO> {
   
   public static <K, VI, VO> TransformingMapEntry<K, VI, K, VO> transformValue(
         Entry<K, VI> internal, Function<? super VI, ? extends VO> function) {
      return new TransformingMapEntry<>(internal, Function.identity(), (k, v) -> function.apply(v));
   }

   public static <K, VI, VO> TransformingMapEntry<K, VI, K, VO> transformValue(Entry<K, VI> internal,
         BiFunction<? super K, ? super VI, ? extends VO> function) {
      return new TransformingMapEntry<>(internal, Function.identity(), function);
   }

   public static <KI, KO, V> TransformingMapEntry<KI, V, KO, V> transformKey(Entry<KI, V> internal,
         Function<? super KI, ? extends KO> function) {
      return new TransformingMapEntry<>(internal, function, (k, v) -> v);
   }

   private final Entry<KI, VI> internal;
   private final Function<? super KI, ? extends KO> keyFunction;
   private final BiFunction<? super KI, ? super VI, ? extends VO> valueFunction;

   public TransformingMapEntry(Entry<KI, VI> internal,
         Function<? super KI, ? extends KO> keyFunction,
         BiFunction<? super KI, ? super VI, ? extends VO> valueFunction) {
      this.internal = internal;
      this.keyFunction = keyFunction;
      this.valueFunction = valueFunction;
   }

   protected Entry<KI, VI> internal() {
      return internal;
   }
   
   protected Function<? super KI, ? extends KO> keyFunction() {
      return keyFunction;
   }

   protected BiFunction<? super KI, ? super VI, ? extends VO> valueFunction() {
      return valueFunction;
   }

   @Override
   public KO getKey() {
      return keyFunction().apply(internal().getKey());
   }

   @Override
   public VO getValue() {
      return valueFunction().apply(internal().getKey(), internal().getValue());
   }

   @Override
   public VO setValue(VO value) {
      throw new UnsupportedOperationException("setValue");
   }
   
   @Override
   public boolean equals(Object o) {
      return MapUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return MapUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return MapUtils.toString(this);
   }
   
   public static class Bidi<KI, VI, KO, VO> extends TransformingMapEntry<KI, VI, KO, VO> {

      public static <K, VI, VO> Bidi<K, VI, K, VO> transformValue(
            Entry<K, VI> internal, Function<? super VI, ? extends VO> function1,
            Function<? super VO, ? extends VI> function2) {
         return new Bidi<>(internal, Function.identity(), (k, v) -> function1.apply(v),
               (k, v) -> function2.apply(v));
      }

      public static <K, VI, VO> Bidi<K, VI, K, VO> transformValue(Entry<K, VI> internal,
            BiFunction<? super K, ? super VI, ? extends VO> function1,
            Function<? super VO, ? extends VI> function2) {
         return new Bidi<>(internal, Function.identity(), function1, (k, v) -> function2.apply(v));
      }
      
      private final BiFunction<? super KO, ? super VO, ? extends VI> valueInverse;

      public Bidi(Entry<KI, VI> internal,
            Function<? super KI, ? extends KO> keyFunction,
            BiFunction<? super KI, ? super VI, ? extends VO> valueFunction,
            BiFunction<? super KO, ? super VO, ? extends VI> valueInverse) {
         super(internal, keyFunction, valueFunction);
         this.valueInverse = valueInverse;
      }
      
      @Override
      public VO setValue(VO value) {
         VI prev = internal().setValue(valueInverse.apply(getKey(), value));
         return valueFunction().apply(internal().getKey(), prev);
      }
   }
}
