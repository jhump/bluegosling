package com.apriori.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A map whose keys and/or values are the results of applying functions to the keys and/or values of
 * another map. This map is simply a wrapper. Changes to the underlying map are visible in the
 * transformed set. Accessing the map's contents may incur calls to the transforming function.
 * 
 * <p>Functions that perform the transformations should be deterministic so that a stable,
 * unchanging map does not appear to be mutating when accessed through this transforming wrapper.
 * 
 * <p>Since transformations can only be done in one direction, some operations are not supported.
 * Namely, {@link #put(Object, Object)} and {@link #putAll(Map)} throw
 * {@link UnsupportedOperationException}.
 * 
 * <p><strong>Note</strong>: If keys are being transformed, the key function should produce a 1-to-1
 * mapping from elements of the input type to elements of the output type. If the function should
 * ever produce the same output value for two <em>unequal</em> input values, then the invariant that
 * is core to the map -- that its keys are unique -- could be broken for the transformed map.
 * Failure to comply with this constraint may yield maps that appear to have duplicates. Because of
 * this  requirement, caution is advised when using a {@link TransformingMap} that transforms keys.
 * A safer way to transform a map's keys is to make a snapshot that is transformed and dump those
 * into a new map which can enforce uniqueness. That could be done with code like the
 * following:<pre>
 * Map&lt;Type1, ValueType&gt; input;
 * Function&lt;Type1, Type2&gt; function;
 * // take a snapshot to produce a transformed map that will never
 * // violate map invariants
 * Map&lt;Type2, ValueType&gt; transformed = new HashMap&lt;&gt;(
 *       TransformingMap.transformingKeys(input, function));</pre>
 * <p>Also maps with transformed keys (due to transformations only working in one direction) have
 * some methods that are implemented in terms of the map's {@linkplain #entrySet() entry iterator}
 * and thus may have worse performance than the underlying map's implementation. These methods
 * include {@link #get(Object)}, {@link #containsKey(Object)} and {@link #remove(Object)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <KI> the "input" key type; the type of keys in the wrapped map
 * @param <VI> the "input" value type; the type of values in the wrapped map
 * @param <KO> the "output" key type; the type of keys in this map
 * @param <VO> the "output" value type; the type of values in this map
 * 
 * @see TransformingMap.ReadOnly
 * @see TransformingMap.Bidi
 */
// TODO: tests
public class TransformingMap<KI, VI, KO, VO> implements Map<KO, VO> {
   
   /**
    * Creates a transforming wrapper that only transforms map values. Since keys are not being
    * transformed, the returned map is more efficient than one where keys are being transformed.
    *
    * @param internal the map being wrapped
    * @param function the function used to produce transformed map values
    * @return a view of the given map, but with values transformed via the given function
    */
   public static <K, VI, VO> TransformingMap<K, VI, K, VO> transformingValues(Map<K, VI> internal,
         Function<? super VI, ? extends VO> function) {
      return new TransformingMap.ValuesOnly<>(internal, (k, v) -> function.apply(v));
   }

   /**
    * Creates a transforming wrapper that only transforms map values. Since keys are not being
    * transformed, the returned map is more efficient than one where keys are being transformed.
    * 
    * <p>Some operations may pass a type other than {@code K} to the given two-argument function. In
    * particular, {@link #get(Object)} and {@link #remove(Object)} will compute the transformed
    * result using the given object as the key. This could result in {@link ClassCastException}
    * being thrown in some cases. For example, if a map contained keys of type {@code HashSet<T>},
    * and the given function requires input keys to also be {@code HashSet<T>}. In this case, if the
    * map is queried using a {@code TreeSet<T>}, a mapping could be found even though the key is the
    * wrong type due to the specification for {@link Set#equals(Object)}, and an exception could
    * result.
    * 
    * @param internal the map being wrapped
    * @param function a function used to produce transformed map values given both the original key
    *       and value as its inputs
    * @return a view of the given map, but with values transformed via the given function
    */
   public static <K, VI, VO> TransformingMap<K, VI, K, VO> transformingValues(Map<K, VI> internal,
         BiFunction<? super K, ? super VI, ? extends VO> function) {
      return new TransformingMap.ValuesOnly<>(internal, function);
   }

   /**
    * Creates a transforming wrapper that only transforms map keys.
    *
    * @param internal the map being wrapped
    * @param function the function used to produce transformed map keys
    * @return a view of the given map, but with keys transformed via the given function
    */
   public static <KI, KO, V> TransformingMap<KI, V, KO, V> transformingKeys(Map<KI, V> internal,
         Function<? super KI, ? extends KO> function) {
      return new TransformingMap<>(internal, function, (k, v) -> v);
   }

   private final Map<KI, VI> internal;
   private final Function<? super KI, ? extends KO> keyFunction;
   private final BiFunction<? super KI, ? super VI, ? extends VO> valueFunction;

   /**
    * Constructs a transforming wrapper that may transform both keys and values.
    *
    * @param internal the map being wrapped
    * @param keyFunction the function used to produce transformed map keys
    * @param valueFunction the function used to produce transformed map values
    */
   public TransformingMap(Map<KI, VI> internal,
         Function<? super KI, ? extends KO> keyFunction,
         BiFunction<? super KI, ? super VI, ? extends VO> valueFunction) {
      this.internal = internal;
      this.keyFunction = keyFunction;
      this.valueFunction = valueFunction;
   }

   /**
    * The underlying map whose keys and/or values are being transformed.
    *
    * @return the underlying map
    */
   protected Map<KI, VI> internal() {
      return internal;
   }
   
   /**
    * The function used to compute transformed keys.
    *
    * @return the function used to compute transformed keys
    */
   protected Function<? super KI, ? extends KO> keyFunction() {
      return keyFunction;
   }

   /**
    * The function used to compute transformed values.
    *
    * @return the function used to compute transformed values
    */
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

   /**
    * A {@link TransformingMap} that only transforms map values, not keys. Due to this constraint,
    * it provides more efficient implementations for some methods.
    *
    * @param <K> the type of key in the map
    * @param <VI> the "input" value type
    * @param <VO> the "output" value type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ValuesOnly<K, VI, VO> extends TransformingMap<K, VI, K, VO> {
      ValuesOnly(Map<K, VI> internal, BiFunction<? super K, ? super VI, ? extends VO> fn) {
         super(internal, Function.identity(), fn);
      }
      
      @Override
      public boolean containsKey(Object key) {
         return internal().containsKey(key);
      }

      @Override
      public VO get(Object key) {
         VI v = internal().get(key);
         // could cause a ClassCastException in function but won't pollute heap
         @SuppressWarnings("unchecked")
         K k = (K) key;
         return v == null && !internal().containsKey(key) ? null : valueFunction().apply(k, v);
      }

      @Override
      public VO remove(Object key) {
         if (internal().containsKey(key)) {
            // could cause a ClassCastException in function but won't pollute heap
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return valueFunction().apply(k, internal().remove(key));
         } else {
            return null;
         }
      }

      @Override
      public Set<K> keySet() {
         return internal().keySet();
      }

      @Override
      public Set<Entry<K, VO>> entrySet() {
         return new TransformingSet<Entry<K, VI>, Entry<K, VO>>(internal().entrySet(),
               e -> new TransformingMapEntry<>(e, keyFunction(), valueFunction())) {

            @Override public boolean contains(Object o) {
               if (!(o instanceof Entry)) {
                  return false;
               }
               Entry<?, ?> other = (Entry<?, ?>) o;
               // could cause a ClassCastException in function but won't pollute heap
               @SuppressWarnings("unchecked")
               K k = (K) other.getKey();
               VI v = ValuesOnly.this.internal().get(k);
               if (v == null && !containsKey(k)) {
                  return false;
               }
               return Objects.equals(valueFunction().apply(k, v), other.getValue());
            }
            
            @Override public boolean remove(Object o) {
               if (!(o instanceof Entry)) {
                  return false;
               }
               Entry<?, ?> other = (Entry<?, ?>) o;
               // could cause a ClassCastException in function but won't pollute heap
               @SuppressWarnings("unchecked")
               K k = (K) other.getKey();
               VI v = ValuesOnly.this.internal().get(k);
               if (v == null && !containsKey(k)) {
                  return false;
               }
               if (Objects.equals(valueFunction().apply(k, v), other.getValue())) {
                  ValuesOnly.this.internal().remove(other.getKey());
                  return true;
               }
               return false;
            }
         };
      }
   }
   
   /**
    * An unmodifiable transforming map. All mutations operations throw
    * {@link UnsupportedOperationException}.
    *
    * @param <KI> the "input" key type; the type of keys in the wrapped map
    * @param <VI> the "input" value type; the type of values in the wrapped map
    * @param <KO> the "output" key type; the type of keys in this map
    * @param <VO> the "output" value type; the type of values in this map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class ReadOnly<KI, VI, KO, VO> extends TransformingMap<KI, VI, KO, VO> {

      /**
       * Creates a new unmodifiable map that only transforms map values.
       *
       * @param internal the map being wrapped
       * @param function the function used to produce transformed map values
       * @return a view of the given map, but with values transformed via the given function
       * 
       * @see TransformingMap#transformingValues(Map, Function)
       */
      public static <K, VI, VO> ReadOnly<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            Function<? super VI, ? extends VO> function) {
         return new ReadOnly.ValuesOnly<>(internal, (k, v) -> function.apply(v));
      }

      /**
       * Creates a new unmodifiable map that only transforms map values.
       *
       * @param internal the map being wrapped
       * @param function a function used to produce transformed map values given both the original
       *       key and value as its inputs
       * @return a view of the given map, but with values transformed via the given function
       * 
       * @see TransformingMap#transformingValues(Map, BiFunction)
       */
      public static <K, VI, VO> ReadOnly<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            BiFunction<? super K, ? super VI, ? extends VO> function) {
         return new ReadOnly.ValuesOnly<>(internal, function);
      }

      /**
       * Creates a new unmodifiable map that only transforms map keys.
       *
       * @param internal the map being wrapped
       * @param function the function used to produce transformed map keys
       * @return a view of the given map, but with keys transformed via the given function
       * 
       * @see TransformingMap#transformingKeys(Map, Function)
       */
      public static <KI, KO, V> ReadOnly<KI, V, KO, V> transformingKeys(Map<KI, V> internal,
            Function<? super KI, ? extends KO> function) {
         return new ReadOnly<>(internal, function, (k, v) -> v);
      }

      /**
       * Constructs a new unmodifiable map that can transform both map keys and values.
       *
       * @param internal the map being wrapped
       * @param keyFunction the function used to produce transformed map keys
       * @param valueFunction the function used to produce transformed map values
       */
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
      
      /**
       * An unmodifiable transforming map that can only transform values, not keys. Due to this
       * constraint, it provides more efficient implementations for some methods.
       *
       * @param <K> the type of key in the map
       * @param <VI> the "input" value type
       * @param <VO> the "output" value type
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      static class ValuesOnly<K, VI, VO> extends ReadOnly<K, VI, K, VO> {
         ValuesOnly(Map<K, VI> internal, BiFunction<? super K, ? super VI, ? extends VO> fn) {
            super(internal, Function.identity(), fn);
         }
         
         @Override
         public boolean containsKey(Object key) {
            return internal().containsKey(key);
         }

         @Override
         public VO get(Object key) {
            VI v = internal().get(key);
            // could cause a ClassCastException in function but won't pollute heap
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return v == null && !internal().containsKey(key) ? null : valueFunction().apply(k, v);
         }

         @Override
         public Set<K> keySet() {
            return Collections.unmodifiableSet(internal().keySet());
         }

         @Override
         public Set<Entry<K, VO>> entrySet() {
            return new TransformingSet.ReadOnly<Entry<K, VI>, Entry<K, VO>>(internal().entrySet(),
                  e -> new TransformingMapEntry<>(e, keyFunction(), valueFunction())) {

               @Override public boolean contains(Object o) {
                  if (!(o instanceof Entry)) {
                     return false;
                  }
                  Entry<?, ?> other = (Entry<?, ?>) o;
                  // could cause a ClassCastException in function but won't pollute heap
                  @SuppressWarnings("unchecked")
                  K k = (K) other.getKey();
                  VI v = ValuesOnly.this.internal().get(k);
                  if (v == null && !containsKey(k)) {
                     return false;
                  }
                  return Objects.equals(valueFunction().apply(k, v), other.getValue());
               }
            };
         }
      }
   }
   
   /**
    * A transforming map that can transform keys and values in both directions. Since this map can
    * transform in the other direction (output type -&gt; input type), all operations are supported,
    * including {@link #put(Object, Object)} and {@link #putAll(Map)}.
    * 
    * <p>Several methods are overridden to use the reverse function before delegating to the
    * underlying map. Since some of these interface methods actually accept any type of object
    * (instead of requiring the map's key type), this implementation could result in
    * {@link ClassCastException}s if objects with incorrect types are passed to them. These
    * methods are {@link #containsKey(Object)}, {@link #get(Object)}, and {@link #remove(Object)}.
    *
    * @param <KI> the "input" key type; the type of keys in the wrapped map
    * @param <VI> the "input" value type; the type of values in the wrapped map
    * @param <KO> the "output" key type; the type of keys in this map
    * @param <VO> the "output" value type; the type of values in this map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Bidi<KI, VI, KO, VO> extends TransformingMap<KI, VI, KO, VO> {
      
      /**
       * Creates a new bidirectional map that only transforms map values.
       *
       * @param internal the map being wrapped
       * @param function1 the function used to produce transformed map values
       * @param function2 the function used to inverse transformed map values
       * @return a view of the given map, but with values transformed via the given functions
       * 
       * @see TransformingMap#transformingValues(Map, Function)
       */
      public static <K, VI, VO> Bidi<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            Function<? super VI, ? extends VO> function1,
            Function<? super VO, ? extends VI> function2) {
         return new Bidi.ValuesOnly<>(internal, (k, v) -> function1.apply(v), function2);
      }

      /**
       * Creates a new bidirectional map that only transforms map values.
       *
       * @param internal the map being wrapped
       * @param function1 a function used to produce transformed map values given both the original
       *       key and value as its inputs
       * @param function2 the function used to inverse transformed map values
       * @return a view of the given map, but with values transformed via the given functions
       * 
       * @see TransformingMap#transformingValues(Map, BiFunction)
       */
      public static <K, VI, VO> Bidi<K, VI, K, VO> transformingValues(Map<K, VI> internal,
            BiFunction<? super K, ? super VI, ? extends VO> function1,
            Function<? super VO, ? extends VI> function2) {
         return new Bidi.ValuesOnly<>(internal, function1, function2);
      }

      /**
       * Creates a new bidirectional map that only transforms map keys.
       *
       * @param internal the map being wrapped
       * @param function1 a function used to produce transformed map keys
       * @param function2 the function used to inverse transformed map keys
       * @return a view of the given map, but with keys transformed via the given functions
       * 
       * @see TransformingMap#transformingKeys(Map, Function)
       */
      public static <KI, KO, V> Bidi<KI, V, KO, V> transformingKeys(Map<KI, V> internal,
            Function<? super KI, ? extends KO> function1,
            Function<? super KO, ? extends KI> function2) {
         return new Bidi<>(internal, function1, (k, v) -> v, function2, Function.identity());
      }

      private final Function<? super KO, ? extends KI> keyInverse;
      private final Function<? super VO, ? extends VI> valueInverse;
      
      /**
       * Constructs a new bidirectional map that can transform both map keys and values.
       *
       * @param internal the map being wrapped
       * @param keyFunction the function used to produce transformed map keys
       * @param valueFunction the function used to produce transformed map values
       * @param keyInverse the function used to inverse transformed map keys
       * @param valueInverse the function used to inverse transformed map values
       */
      public Bidi(Map<KI, VI> internal,
            Function<? super KI, ? extends KO> keyFunction,
            BiFunction<? super KI, ? super VI, ? extends VO> valueFunction,
            Function<? super KO, ? extends KI> keyInverse,
            Function<? super VO, ? extends VI> valueInverse) {
         super(internal, keyFunction, valueFunction);
         this.keyInverse = keyInverse;
         this.valueInverse = valueInverse;
      }

      /**
       * Returns the function that inverts key transformations.
       *
       * @return the function that inverts key transformations
       */
      protected Function<? super KO, ? extends KI> keyInverseFunction() {
         return keyInverse;
      }

      /**
       * Returns the function that inverts value transformations.
       *
       * @return the function that inverts value transformations
       */
      protected Function<? super VO, ? extends VI> valueInverseFunction() {
         return valueInverse;
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
      public boolean containsKey(Object key) {
         @SuppressWarnings("unchecked") // could cause ClassCastException below, but nothing else
         KO ko = (KO) key;
         KI ki = keyInverse.apply(ko);
         return internal().containsKey(ki);
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
      
      static class ValuesOnly<K, VI, VO> extends Bidi<K, VI, K, VO> {
         ValuesOnly(Map<K, VI> internal, BiFunction<? super K, ? super VI, ? extends VO> fn,
               Function<? super VO, ? extends VI> inverse) {
            super(internal, Function.identity(), fn, Function.identity(),
                  inverse);
         }
         
         @Override
         public boolean containsKey(Object key) {
            return internal().containsKey(key);
         }

         @Override
         public VO get(Object key) {
            VI v = internal().get(key);
            // could cause a ClassCastException in function but won't pollute heap
            @SuppressWarnings("unchecked")
            K k = (K) key;
            return v == null && !internal().containsKey(key) ? null : valueFunction().apply(k, v);
         }

         @Override
         public VO remove(Object key) {
            if (internal().containsKey(key)) {
               // could cause a ClassCastException in function but won't pollute heap
               @SuppressWarnings("unchecked")
               K k = (K) key;
               return valueFunction().apply(k, internal().remove(key));
            } else {
               return null;
            }
         }

         @Override
         public Set<K> keySet() {
            return internal().keySet();
         }

         @Override
         public Set<Entry<K, VO>> entrySet() {
            return new TransformingSet<Entry<K, VI>, Entry<K, VO>>(internal().entrySet(),
                  e -> new TransformingMapEntry<>(e, keyFunction(), valueFunction())) {

               @Override public boolean contains(Object o) {
                  if (!(o instanceof Entry)) {
                     return false;
                  }
                  Entry<?, ?> other = (Entry<?, ?>) o;
                  // could cause a ClassCastException in function but won't pollute heap
                  @SuppressWarnings("unchecked")
                  K k = (K) other.getKey();
                  VI v = ValuesOnly.this.internal().get(k);
                  if (v == null && !containsKey(k)) {
                     return false;
                  }
                  return Objects.equals(valueFunction().apply(k, v), other.getValue());
               }
               
               @Override public boolean remove(Object o) {
                  if (!(o instanceof Entry)) {
                     return false;
                  }
                  Entry<?, ?> other = (Entry<?, ?>) o;
                  // could cause a ClassCastException in function but won't pollute heap
                  @SuppressWarnings("unchecked")
                  K k = (K) other.getKey();
                  VI v = ValuesOnly.this.internal().get(k);
                  if (v == null && !containsKey(k)) {
                     return false;
                  }
                  if (Objects.equals(valueFunction().apply(k, v), other.getValue())) {
                     ValuesOnly.this.internal().remove(other.getKey());
                     return true;
                  }
                  return false;
               }
            };
         }
      }
   }
}
