package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

// TODO: javadoc
// TODO: tests
public class TransformingMap<KI, VI, KO, VO> implements Map<KO, VO> {
   
   public static <K, VI, VO> TransformingMap<K, VI, K, VO> transformingValues(Map<K, VI> internal,
         Function<? super VI, ? extends VO> function) {
      return new TransformingMap.ValuesOnly<>(internal, function);
   }

   public static <K, VI, VO> TransformingMap<K, VI, K, VO> transformingValues(Map<K, VI> internal,
         BiFunction<? super K, ? super VI, ? extends VO> function) {
      return new TransformingMap<>(internal, Function.identity(), function);
   }

   public static <KI, KO, V> TransformingMap<KI, V, KO, V> transformingKeys(Map<KI, V> internal,
         Function<? super KI, ? extends KO> function) {
      return new TransformingMap<>(internal, function, (k, v) -> v);
   }

   private final Map<KI, VI> internal;
   private final Function<? super KI, ? extends KO> keyFunction;
   private final BiFunction<? super KI, ? super VI, ? extends VO> valueFunction;

   public TransformingMap(Map<KI, VI> internal,
         Function<? super KI, ? extends KO> keyFunction,
         BiFunction<? super KI, ? super VI, ? extends VO> valueFunction) {
      this.internal = internal;
      this.keyFunction = keyFunction;
      this.valueFunction = valueFunction;
   }

   protected Map<KI, VI> internal() {
      return internal;
   }
   
   protected Function<? super KI, ? extends KO> keyFunction() {
      return keyFunction;
   }

   protected BiFunction<? super KI, ? super VI, ? extends VO> valueFunction() {
      return valueFunction;
   }

   @Override
   public int size() {
      return internal().size();
   }

   @Override
   public boolean isEmpty() {
      return internal().isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return keySet().contains(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return values().contains(value);
   }

   @Override
   public VO get(Object key) {
      for (Entry<KO, VO> entry : entrySet()) {
         if (Objects.equals(key, entry.getKey())) {
            return entry.getValue();
         }
      }
      return null;
   }

   @Override
   public VO put(KO key, VO value) {
      throw new UnsupportedOperationException("put");
   }

   @Override
   public VO remove(Object key) {
      for (Iterator<Entry<KO, VO>> iter = entrySet().iterator(); iter.hasNext(); ) {
         Entry<KO, VO> entry = iter.next();
         if (Objects.equals(key, entry.getKey())) {
            iter.remove();
            return entry.getValue();
         }
      }
      return null;
   }

   @Override
   public void putAll(Map<? extends KO, ? extends VO> m) {
      throw new UnsupportedOperationException("putAll");
   }

   @Override
   public void clear() {
      internal().clear();
   }

   @Override
   public Set<KO> keySet() {
      return new TransformingSet<>(internal().keySet(), keyFunction());
   }

   @Override
   public Collection<VO> values() {
      return new TransformingCollection<>(internal().entrySet(),
            e -> valueFunction().apply(e.getKey(), e.getValue()));
   }

   @Override
   public Set<Entry<KO, VO>> entrySet() {
      return new TransformingSet<>(internal().entrySet(),
            e -> new TransformingMapEntry<>(e, keyFunction(), valueFunction()));
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
   
   static class ValuesOnly<K, VI, VO> extends TransformingMap<K, VI, K, VO> {
      private final Function<? super VI, ? extends VO> fn;
      
      ValuesOnly(Map<K, VI> internal, Function<? super VI, ? extends VO> fn) {
         super(internal, null, (k, v) -> fn.apply(v));
         this.fn = fn;
      }
      
      protected Function<? super VI, ? extends VO> valueOnlyFunction() {
         return fn;
      }
      
      @Override
      public boolean containsKey(Object key) {
         return internal().containsKey(key);
      }

      @Override
      public VO get(Object key) {
         VI v = internal().get(key);
         return v == null && !internal().containsKey(key) ? null : fn.apply(internal().get(key));
      }

      @Override
      public VO remove(Object key) {
         return fn.apply(internal().remove(key));
      }

      @Override
      public Set<K> keySet() {
         return internal().keySet();
      }

      @Override
      public Set<Entry<K, VO>> entrySet() {
         // TODO: more efficient
         return new TransformingSet<>(internal().entrySet(),
               e -> new TransformingMapEntry<>(e, keyFunction(), valueFunction()));
      }
   }
   
   public static class ReadOnly<KI, VI, KO, VO> extends TransformingMap<KI, VI, KO, VO> {

      public static <K, VI, VO> ReadOnly<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            Function<? super VI, ? extends VO> function) {
         return new ReadOnly<>(internal, Function.identity(), (k, v) -> function.apply(v));
      }

      public static <K, VI, VO> ReadOnly<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            BiFunction<? super K, ? super VI, ? extends VO> function) {
         return new ReadOnly<>(internal, Function.identity(), function);
      }

      public static <KI, KO, V> ReadOnly<KI, V, KO, V> transformingKeys(Map<KI, V> internal,
            Function<? super KI, ? extends KO> function) {
         return new ReadOnly<>(internal, function, (k, v) -> v);
      }

      public ReadOnly(Map<KI, VI> internal,
            Function<? super KI, ? extends KO> keyFunction,
            BiFunction<? super KI, ? super VI, ? extends VO> valueFunction) {
         super(internal, keyFunction, valueFunction);
      }
      
      @Override
      public VO remove(Object key) {
         throw new UnsupportedOperationException("remove");
      }
      
      @Override
      public void clear() {
         throw new UnsupportedOperationException("clear");
      }
      
      @Override
      public Set<KO> keySet() {
         return new TransformingSet.ReadOnly<>(internal().keySet(), k -> keyFunction().apply(k));
      }

      @Override
      public Collection<VO> values() {
         return new TransformingSet.ReadOnly<>(internal().entrySet(),
               e -> valueFunction().apply(e.getKey(), e.getValue()));
      }

      @Override
      public Set<Entry<KO, VO>> entrySet() {
         return new TransformingSet.ReadOnly<>(internal().entrySet(),
               e -> new TransformingMapEntry<>(e, keyFunction(), valueFunction()));
      }
      
      // TODO: ValuesOnly
   }
   
   public static class Bidi<KI, VI, KO, VO> extends TransformingMap<KI, VI, KO, VO> {
      
      public static <K, VI, VO> Bidi<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            Function<? super VI, ? extends VO> function1,
            Function<? super VO, ? extends VI> function2) {
         return new Bidi<>(internal, Function.identity(), (k, v) -> function1.apply(v),
               Function.identity(), function2);
      }

      public static <K, VI, VO> Bidi<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            BiFunction<? super K, ? super VI, ? extends VO> function1,
            Function<? super VO, ? extends VI> function2) {
         return new Bidi<>(internal, Function.identity(), function1,
               Function.identity(), function2);
      }

      public static <KI, KO, V> Bidi<KI, V, KO, V> transformingKeys(Map<KI, V> internal,
            Function<? super KI, ? extends KO> function1,
            Function<? super KO, ? extends KI> function2) {
         return new Bidi<>(internal, function1, (k, v) -> v, function2, Function.identity());
      }

      private final Function<? super KO, ? extends KI> keyInverse;
      private final Function<? super VO, ? extends VI> valueInverse;
      
      public Bidi(Map<KI, VI> internal,
            Function<? super KI, ? extends KO> keyFunction,
            BiFunction<? super KI, ? super VI, ? extends VO> valueFunction,
            Function<? super KO, ? extends KI> keyInverse,
            Function<? super VO, ? extends VI> valueInverse) {
         super(internal, keyFunction, valueFunction);
         this.keyInverse = keyInverse;
         this.valueInverse = valueInverse;
      }


      @Override
      public VO get(Object key) {
         @SuppressWarnings("unchecked") // could cause ClassCastException below, but nothing else
         KO ko = (KO) key;
         KI ki = keyInverse.apply(ko);
         VI vi = internal().get(ki);
         if (vi == null && !internal().containsKey(ki)) {
            return null;
         }
         return valueFunction().apply(ki, vi);
      }

      @Override
      public VO put(KO key, VO value) {
         KI ki = keyInverse.apply(key);
         VI vi = valueInverse.apply(value);
         return valueFunction().apply(ki, internal().put(ki, vi));
      }

      @Override
      public VO remove(Object key) {
         @SuppressWarnings("unchecked") // could cause ClassCastException below, but nothing else
         KO ko = (KO) key;
         KI ki = keyInverse.apply(ko);
         if (!internal().containsKey(ki)) {
            return null;
         } 
         VI vi = internal().remove(ki);
         return valueFunction().apply(ki, vi);
      }

      @Override
      public void putAll(Map<? extends KO, ? extends VO> m) {
         internal().putAll(new TransformingMap<>(m, keyInverse, (k, v) -> valueInverse.apply(v)));
      }
      
      @Override
      public Set<KO> keySet() {
         return new TransformingSet.Bidi<>(internal().keySet(), keyFunction(), keyInverse);
      }

      @Override
      public Set<Entry<KO, VO>> entrySet() {
         return new TransformingSet.Bidi<>(internal().entrySet(),
               e -> new TransformingMapEntry.Bidi<>(
                     e, keyFunction(), valueFunction(), (k, v) -> valueInverse.apply(v)),
               e -> new TransformingMapEntry.Bidi<>(
                     e, keyInverse, (k, v) -> valueInverse.apply(v), valueFunction()));
      }
      
      // TODO: ValuesOnly
   }
}
