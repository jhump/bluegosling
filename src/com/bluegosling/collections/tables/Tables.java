package com.bluegosling.collections.tables;

import com.bluegosling.collections.TransformingIterator;
import com.bluegosling.collections.TransformingMap;
import com.bluegosling.collections.tables.Table2D.Cell2D;
import com.bluegosling.collections.tables.Table3D.Cell3D;
import com.bluegosling.collections.tables.Table4D.Cell4D;
import com.bluegosling.collections.tables.Table5D.Cell5D;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility methods and implementation classes for the various {@code Table} interfaces.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
// TODO: cloning, serialization
final class Tables {
   private Tables() {
   }
   
   /**
    * Returns true if the given 2-dimensional table is equal to the other given object. The two
    * objects are equal if the other object is also a 2-dimensional table with all of the same
    * mappings.
    *
    * @param table a table
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Table2D<?, ?, ?> table, Object o) {
      if (!(o instanceof Table2D)) {
         return false;
      }
      Table2D<?, ?, ?> other = (Table2D<?, ?, ?>) o;
      return table.asMap().equals(other.asMap());
   }
   
   /**
    * Returns true if the given 3-dimensional table is equal to the other given object. The two
    * objects are equal if the other object is also a 3-dimensional table with all of the same
    * mappings.
    *
    * @param table a table
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Table3D<?, ?, ?, ?> table, Object o) {
      if (!(o instanceof Table3D)) {
         return false;
      }
      Table3D<?, ?, ?, ?> other = (Table3D<?, ?, ?, ?>) o;
      return table.asMap().equals(other.asMap());
   }

   /**
    * Returns true if the given 4-dimensional table is equal to the other given object. The two
    * objects are equal if the other object is also a 4-dimensional table with all of the same
    * mappings.
    *
    * @param table a table
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Table4D<?, ?, ?, ?, ?> table, Object o) {
      if (!(o instanceof Table4D)) {
         return false;
      }
      Table4D<?, ?, ?, ?, ?> other = (Table4D<?, ?, ?, ?, ?>) o;
      return table.asMap().equals(other.asMap());
   }

   /**
    * Returns true if the given 5-dimensional table is equal to the other given object. The two
    * objects are equal if the other object is also a 5-dimensional table with all of the same
    * mappings.
    *
    * @param table a table
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Table5D<?, ?, ?, ?, ?, ?> table, Object o) {
      if (!(o instanceof Table5D)) {
         return false;
      }
      Table5D<?, ?, ?, ?, ?, ?> other = (Table5D<?, ?, ?, ?, ?, ?>) o;
      return table.asMap().equals(other.asMap());
   }
   
   /**
    * Returns true if the given 2-dimensional table cell is equal to the other given object. The two
    * objects are equal if the other object is also a 2-dimensional table cell with the same keys
    * and same value.
    *
    * @param cell a table cell
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Cell2D<?, ?, ?> cell, Object o) {
      if (!(o instanceof Cell2D)) {
         return false;
      }
      Cell2D<?, ?, ?> other = (Cell2D<?, ?, ?>) o;
      return Objects.equals(cell.getRowKey(), other.getRowKey())
            && Objects.equals(cell.getColumnKey(), other.getColumnKey())
            && Objects.equals(cell.getValue(), other.getValue());
   }

   /**
    * Computes the hash code for the given 2-dimensional table cell.
    *
    * @param cell a table cell
    * @return the hash code for the cell
    */
   static int hashCode(Cell2D<?, ?, ?> cell) {
      return Objects.hashCode(cell.getRowKey()) ^ Objects.hashCode(cell.getColumnKey())
            ^ Objects.hashCode(cell.getValue());
   }

   /**
    * Creates a string representation of the given 2-dimensional table cell.
    *
    * @param cell a table cell
    * @return a string representation of the cell
    */
   static String toString(Cell2D<?, ?, ?> cell) {
      return "[ " + cell.getRowKey() + ", " + cell.getColumnKey() + " ] => " + cell.getValue();
   }

   /**
    * Returns true if the given 3-dimensional table cell is equal to the other given object. The two
    * objects are equal if the other object is also a 3-dimensional table cell with the same keys
    * and same value.
    *
    * @param cell a table cell
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Cell3D<?, ?, ?, ?> cell, Object o) {
      if (!(o instanceof Cell3D)) {
         return false;
      }
      Cell3D<?, ?, ?, ?> other = (Cell3D<?, ?, ?, ?>) o;
      return Objects.equals(cell.getPageKey(), other.getPageKey())
            && Objects.equals(cell.getRowKey(), other.getRowKey())
            && Objects.equals(cell.getColumnKey(), other.getColumnKey())
            && Objects.equals(cell.getValue(), other.getValue());
   }

   /**
    * Computes the hash code for the given 3-dimensional table cell.
    *
    * @param cell a table cell
    * @return the hash code for the cell
    */
   static int hashCode(Cell3D<?, ?, ?, ?> cell) {
      return Objects.hashCode(cell.getPageKey()) ^ Objects.hashCode(cell.getRowKey())
            ^ Objects.hashCode(cell.getColumnKey()) ^ Objects.hashCode(cell.getValue());
   }

   /**
    * Creates a string representation of the given 3-dimensional table cell.
    *
    * @param cell a table cell
    * @return a string representation of the cell
    */
   static String toString(Cell3D<?, ?, ?, ?> cell) {
      return "[ " + cell.getPageKey() + ", " + cell.getRowKey() + ", " + cell.getColumnKey()
            + " ] => " + cell.getValue();
   }
   
   /**
    * Returns true if the given 4-dimensional table cell is equal to the other given object. The two
    * objects are equal if the other object is also a 4-dimensional table cell with the same keys
    * and same value.
    *
    * @param cell a table cell
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Cell4D<?, ?, ?, ?, ?> cell, Object o) {
      if (!(o instanceof Cell4D)) {
         return false;
      }
      Cell4D<?, ?, ?, ?, ?> other = (Cell4D<?, ?, ?, ?, ?>) o;
      return Objects.equals(cell.getSectionKey(), other.getSectionKey())
            && Objects.equals(cell.getPageKey(), other.getPageKey())
            && Objects.equals(cell.getRowKey(), other.getRowKey())
            && Objects.equals(cell.getColumnKey(), other.getColumnKey())
            && Objects.equals(cell.getValue(), other.getValue());
   }

   /**
    * Computes the hash code for the given 4-dimensional table cell.
    *
    * @param cell a table cell
    * @return the hash code for the cell
    */
   static int hashCode(Cell4D<?, ?, ?, ?, ?> cell) {
      return Objects.hashCode(cell.getSectionKey()) ^ Objects.hashCode(cell.getPageKey())
            ^ Objects.hashCode(cell.getRowKey()) ^ Objects.hashCode(cell.getColumnKey())
            ^ Objects.hashCode(cell.getValue());
   }

   /**
    * Creates a string representation of the given 4-dimensional table cell.
    *
    * @param cell a table cell
    * @return a string representation of the cell
    */
   static String toString(Cell4D<?, ?, ?, ?, ?> cell) {
      return "[ " + cell.getSectionKey() + ", " + cell.getPageKey() + ", " + cell.getRowKey() + ", "
            + cell.getColumnKey() + " ] => " + cell.getValue();
   }
   
   /**
    * Returns true if the given 5-dimensional table cell is equal to the other given object. The two
    * objects are equal if the other object is also a 5-dimensional table cell with the same keys
    * and same value.
    *
    * @param cell a table cell
    * @param o another object
    * @return true if the two objects are equal
    */
   static boolean equals(Cell5D<?, ?, ?, ?, ?, ?> cell, Object o) {
      if (!(o instanceof Cell5D)) {
         return false;
      }
      Cell5D<?, ?, ?, ?, ?, ?> other = (Cell5D<?, ?, ?, ?, ?, ?>) o;
      return Objects.equals(cell.getVolumeKey(), other.getVolumeKey())
            && Objects.equals(cell.getSectionKey(), other.getSectionKey())
            && Objects.equals(cell.getPageKey(), other.getPageKey())
            && Objects.equals(cell.getRowKey(), other.getRowKey())
            && Objects.equals(cell.getColumnKey(), other.getColumnKey())
            && Objects.equals(cell.getValue(), other.getValue());
   }

   /**
    * Computes the hash code for the given 5-dimensional table cell.
    *
    * @param cell a table cell
    * @return the hash code for the cell
    */
   static int hashCode(Cell5D<?, ?, ?, ?, ?, ?> cell) {
      return Objects.hashCode(cell.getVolumeKey()) ^ Objects.hashCode(cell.getSectionKey())
            ^ Objects.hashCode(cell.getPageKey()) ^ Objects.hashCode(cell.getRowKey())
            ^ Objects.hashCode(cell.getColumnKey()) ^ Objects.hashCode(cell.getValue());
   }

   /**
    * Creates a string representation of the given 5-dimensional table cell.
    *
    * @param cell a table cell
    * @return a string representation of the cell
    */
   static String toString(Cell5D<?, ?, ?, ?, ?, ?> cell) {
      return "[ " + cell.getVolumeKey() + ", " + cell.getSectionKey() + ", " + cell.getPageKey()
            + ", " + cell.getRowKey() + ", " + cell.getColumnKey() + " ] => " + cell.getValue();
   }
   
   /**
    * An immutable 2-dimensional table cell.
    *
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ImmutableCell2D<R, C, V> implements Cell2D<R, C, V> {
      private final R rowKey;
      private final C columnKey;
      private final V value;
      
      ImmutableCell2D(R rowKey, C columnKey, V value) {
         this.rowKey = rowKey;
         this.columnKey = columnKey;
         this.value = value;
      }
      
      @Override
      public R getRowKey() {
         return rowKey;
      }

      @Override
      public C getColumnKey() {
         return columnKey;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public V setValue(V newValue) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return Tables.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return Tables.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Tables.toString(this);
      }
   }

   /**
    * An immutable 3-dimensional table cell.
    *
    * @param <P> the type of page key
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ImmutableCell3D<P, R, C, V> implements Cell3D<P, R, C, V> {
      private final P pageKey;
      private final R rowKey;
      private final C columnKey;
      private final V value;
      
      ImmutableCell3D(P pageKey, R rowKey, C columnKey, V value) {
         this.pageKey = pageKey;
         this.rowKey = rowKey;
         this.columnKey = columnKey;
         this.value = value;
      }
      
      @Override
      public P getPageKey() {
         return pageKey;
      }
      
      @Override
      public R getRowKey() {
         return rowKey;
      }

      @Override
      public C getColumnKey() {
         return columnKey;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public V setValue(V newValue) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return Tables.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return Tables.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Tables.toString(this);
      }
   }
   
   /**
    * An immutable 4-dimensional table cell.
    *
    * @param <S> the type of section key
    * @param <P> the type of page key
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ImmutableCell4D<S, P, R, C, V> implements Cell4D<S, P, R, C, V> {
      private final S sectionKey;
      private final P pageKey;
      private final R rowKey;
      private final C columnKey;
      private final V value;
      
      ImmutableCell4D(S sectionKey, P pageKey, R rowKey, C columnKey, V value) {
         this.sectionKey = sectionKey;
         this.pageKey = pageKey;
         this.rowKey = rowKey;
         this.columnKey = columnKey;
         this.value = value;
      }
      
      @Override
      public S getSectionKey() {
         return sectionKey;
      }
      
      @Override
      public P getPageKey() {
         return pageKey;
      }
      
      @Override
      public R getRowKey() {
         return rowKey;
      }

      @Override
      public C getColumnKey() {
         return columnKey;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public V setValue(V newValue) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return Tables.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return Tables.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Tables.toString(this);
      }
   }
   
   /**
    * An immutable 5-dimensional table cell.
    *
    * @param <L> the type of volume key
    * @param <S> the type of section key
    * @param <P> the type of page key
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ImmutableCell5D<L, S, P, R, C, V> implements Cell5D<L, S, P, R, C, V> {
      private final L volumeKey;
      private final S sectionKey;
      private final P pageKey;
      private final R rowKey;
      private final C columnKey;
      private final V value;
      
      ImmutableCell5D(L volumeKey, S sectionKey, P pageKey, R rowKey, C columnKey, V value) {
         this.volumeKey = volumeKey;
         this.sectionKey = sectionKey;
         this.pageKey = pageKey;
         this.rowKey = rowKey;
         this.columnKey = columnKey;
         this.value = value;
      }
      
      @Override
      public L getVolumeKey() {
         return volumeKey;
      }
      
      @Override
      public S getSectionKey() {
         return sectionKey;
      }
      
      @Override
      public P getPageKey() {
         return pageKey;
      }
      
      @Override
      public R getRowKey() {
         return rowKey;
      }

      @Override
      public C getColumnKey() {
         return columnKey;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public V setValue(V newValue) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return Tables.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return Tables.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Tables.toString(this);
      }
   }
   
   /**
    * A {@link TransformingMap} that implements the {@link TableMapView} interface.
    *
    * @param <K> the type of keys in the map
    * @param <VI> the type of values in the underlying map (input values)
    * @param <VO> the type of values exposed by this map (output values)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class TransformingTableMapView<K, VI, VO> extends TransformingMap.ValuesOnly<K, VI, VO> 
         implements TableMapView<K, VO> {

      public TransformingTableMapView(TableMapView<K, VI> internal,
            Function<? super VI, ? extends VO> valueFunction) {
         super(internal, (k, v) -> valueFunction.apply(v));
      }
      
      @Override
      protected TableMapView<K, VI> internal() {
         return (TableMapView<K, VI>) super.internal();
      }

      @Override
      public VO getViewForKey(K key) {
         return valueFunction().apply(key, internal().getViewForKey(key));
      }
   }

   /**
    * A {@link Map} wrapper that throws {@link UnsupportedOperationException} for all operations
    * that insert or update mappings. Queries and removals are allowed.
    *
    * @param <K> the type of key in the map
    * @param <V> the type of value in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class MapWithoutPut<K, V> implements Map<K, V> {
      private final Map<K, V> base;

      MapWithoutPut(Map<K, V> base) {
         this.base = base;
      }
      
      @Override
      public int size() {
         return base.size();
      }

      @Override
      public boolean isEmpty() {
         return base.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         return base.containsKey(key);
      }

      @Override
      public boolean containsValue(Object value) {
         return base.containsValue(value);
      }

      @Override
      public V get(Object key) {
         return base.get(key);
      }

      @Override
      public V put(K key, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V remove(Object key) {
         return base.remove(key);
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         base.clear();
      }

      @Override
      public Set<K> keySet() {
         return base.keySet();
      }

      @Override
      public Collection<V> values() {
         return base.values();
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
         Set<Entry<K, V>> entries = base.entrySet();
         return new Set<Entry<K, V>>() {
            @Override
            public int size() {
               return entries.size();
            }

            @Override
            public boolean isEmpty() {
               return entries.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
               return entries.contains(o);
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
               return new TransformingIterator<>(entries.iterator(),
                     e -> new AbstractMap.SimpleImmutableEntry<>(e));
            }

            @Override
            public Object[] toArray() {
               Object ret[] = entries.toArray();
               fixupArray(ret);
               return ret;
            }

            @Override
            public <T> T[] toArray(T[] a) {
               T ret[] = entries.toArray(a);
               fixupArray(ret);
               return ret;
            }
            
            private void fixupArray(Object array[]) {
               for (int i = 0, len = array.length; i < len; i++) {
                  array[i] = new AbstractMap.SimpleImmutableEntry<>((Entry<?, ?>) array[i]);
               }
            }

            @Override
            public boolean add(Entry<K, V> e) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
               return entries.remove(o);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
               return entries.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends Entry<K, V>> c) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
               return entries.retainAll(c);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
               return entries.removeAll(c);
            }

            @Override
            public void clear() {
               entries.clear();
            }
            
         };
      }
      
      @Override
      public V getOrDefault(Object key, V defaultValue) {
         return base.getOrDefault(key, defaultValue);
      }

      @Override
      public void forEach(BiConsumer<? super K, ? super V> action) {
         base.forEach(action);
      }

      @Override
      public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V putIfAbsent(K key, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(Object key, Object value) {
         return base.remove(key, value);
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V replace(K key, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public int hashCode() {
         return base.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         return base.equals(obj);
      }

      @Override
      public String toString() {
         return base.toString();
      }
   }
   
   /**
    * Like {@link MapWithoutPut}, except it wraps a {@link TableMapView}, not just a plain
    * {@link Map}. It doesn't actually implement
    *
    * @param <K> the type of key in the map
    * @param <V> the type of value in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class TableMapViewWithoutPut<K, V> extends MapWithoutPut<K, V>
         implements TableMapView<K, V> {

      TableMapViewWithoutPut(Map<K, V> base) {
         super(base);
      }
      
      @Override
      public V getViewForKey(K key) {
         // #get(Object) already returns a view that disallows puts, so we can just use that
         return get(key);
      }
   }
   
   /**
    * An empty, immutable {@link TableMapView}.
    *
    * @param <K> the type of keys
    * @param <V> the type of values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class EmptyTableMapView<K, V> implements TableMapView<K, V> {
      private final V empty;
      
      /**
       * Constructs a new empty view with the given object representing empty values.
       *
       * @param empty an empty value
       */
      EmptyTableMapView(V empty) {
         this.empty = empty;
      }

      @Override
      public int size() {
         return 0;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public boolean containsKey(Object key) {
         return false;
      }

      @Override
      public boolean containsValue(Object value) {
         return false;
      }

      @Override
      public V put(K key, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Set<K> keySet() {
         return Collections.emptySet();
      }

      @Override
      public Collection<V> values() {
         return Collections.emptyList();
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
         return Collections.emptySet();
      }

      @Override
      public V getViewForKey(K key) {
         return empty;
      }

      @Override
      public V get(Object key) {
         return empty;
      }

      @Override
      public V remove(Object key) {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof TableMapView && ((TableMapView<?, ?>) o).isEmpty();
      }
      
      @Override
      public int hashCode() {
         return 0;
      }
      
      @Override
      public String toString() {
         return "{ }";
      }
   }
   
   /**
    * A map that synchronizes all operations using the monitor of the given lock. This is similar to
    * {@link Collections#synchronizedMap(Map)} except that instead of using its own monitor, it uses
    * the given object.
    *
    * @param <K> the type of keys in the map
    * @param <V> the type of values in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class SynchronizedMap<K, V> implements Map<K, V> {
      final Map<K, V> map;
      final Object lock;
      
      /**
       * Creates a map that synchronizes access to the given map using the given object as a lock.
       *
       * @param map the underlying map
       * @param lock the object used as a lock
       */
      SynchronizedMap(Map<K, V> map, Object lock) {
         this.map = map;
         this.lock = lock;
      }

      @Override
      public int size() {
         synchronized (lock) {
            return map.size();
         }
      }

      @Override
      public boolean isEmpty() {
         synchronized (lock) {
            return map.isEmpty();
         }
      }

      @Override
      public boolean containsKey(Object key) {
         synchronized (lock) {
            return map.containsKey(key);
         }
      }

      @Override
      public boolean containsValue(Object value) {
         synchronized (lock) {
            return map.containsValue(value);
         }
      }

      @Override
      public V get(Object key) {
         synchronized (lock) {
            return map.get(key);
         }
      }

      @Override
      public V put(K key, V value) {
         synchronized (lock) {
            return map.put(key, value);
         }
      }

      @Override
      public V remove(Object key) {
         synchronized (lock) {
            return map.remove(key);
         }
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         synchronized (lock) {
            map.putAll(m);
         }
      }

      @Override
      public void clear() {
         synchronized (lock) {
            map.clear();
         }
      }

      @Override
      public Set<K> keySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(map.keySet(), lock);
         }
      }

      @Override
      public Collection<V> values() {
         synchronized (lock) {
            return new SynchronizedCollection<>(map.values(), lock);
         }
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(map.entrySet(), lock);
         }
      }

      @Override
      public V getOrDefault(Object key, V defaultValue) {
         synchronized (lock) {
            return map.getOrDefault(key, defaultValue);
         }
      }

      @Override
      public void forEach(BiConsumer<? super K, ? super V> action) {
         synchronized (lock) {
            map.forEach(action);
         }
      }

      @Override
      public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
         synchronized (lock) {
            map.replaceAll(function);
         }
      }

      @Override
      public V putIfAbsent(K key, V value) {
         synchronized (lock) {
            return map.putIfAbsent(key, value);
         }
      }

      @Override
      public boolean remove(Object key, Object value) {
         synchronized (lock) {
            return map.remove(key, value);
         }
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         synchronized (lock) {
            return map.replace(key, oldValue, newValue);
         }
      }

      @Override
      public V replace(K key, V value) {
         synchronized (lock) {
            return map.replace(key, value);
         }
      }

      @Override
      public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
         synchronized (lock) {
            return map.computeIfAbsent(key, mappingFunction);
         }
      }

      @Override
      public V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         synchronized (lock) {
            return map.computeIfPresent(key, remappingFunction);
         }
      }

      @Override
      public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         synchronized (lock) {
            return map.compute(key, remappingFunction);
         }
      }

      @Override
      public V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
         synchronized (lock) {
            return map.merge(key, value, remappingFunction);
         }
      }

      @Override
      public int hashCode() {
         synchronized (lock) {
            return map.hashCode();
         }
      }

      @Override
      public boolean equals(Object obj) {
         synchronized (lock) {
            return map.equals(obj);
         }
      }

      @Override
      public String toString() {
         synchronized (lock) {
            return map.toString();
         }
      }
      
      @Override
      protected Object clone() throws CloneNotSupportedException {
         synchronized (lock) {
            return super.clone();
         }
      }
   }
   
   /**
    * A map-view of a table that synchronizes all access using the monitor of a given object. The
    * map's values are themselves table or map views. The views returned for these values will also
    * be synchronized on the same object. 
    *
    * @param <K> the type of keys in the map
    * @param <V> the type of values in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class SynchronizedTableMapView<K, V> extends TransformingTableMapView<K, V, V> {

      private final Object lock;
      
      /**
       * Creates a new table-map-view that synchronizes on the given lock.
       *
       * @param map the underlying map
       * @param lock the object used as a lock
       * @param synchronize a function that returns a synchronized view of a value
       */
      SynchronizedTableMapView(TableMapView<K, V> map, Object lock, Function<V, V> synchronize) {
         super(map, synchronize);
         this.lock = lock;
      }
      
      @Override
      public V getOrDefault(Object key, V defaultValue) {
         synchronized (lock) {
            return super.getOrDefault(key, defaultValue);
         }
      }

      @Override
      public void forEach(BiConsumer<? super K, ? super V> action) {
         synchronized (lock) {
            super.forEach(action);
         }
      }

      @Override
      public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
         synchronized (lock) {
            super.replaceAll(function);
         }
      }

      @Override
      public V putIfAbsent(K key, V value) {
         synchronized (lock) {
            return super.putIfAbsent(key, value);
         }
      }

      @Override
      public boolean remove(Object key, Object value) {
         synchronized (lock) {
            return super.remove(key, value);
         }
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         synchronized (lock) {
            return super.replace(key, oldValue, newValue);
         }
      }

      @Override
      public V replace(K key, V value) {
         synchronized (lock) {
            return super.replace(key, value);
         }
      }

      @Override
      public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
         synchronized (lock) {
            return super.computeIfAbsent(key, mappingFunction);
         }
      }

      @Override
      public V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         synchronized (lock) {
            return super.computeIfPresent(key, remappingFunction);
         }
      }

      @Override
      public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         synchronized (lock) {
            return super.compute(key, remappingFunction);
         }
      }

      @Override
      public V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
         synchronized (lock) {
            return super.merge(key, value, remappingFunction);
         }
      }

      @Override
      public V getViewForKey(K key) {
         synchronized (lock) {
            return super.getViewForKey(key);
         }
      }

      @Override
      public boolean containsKey(Object key) {
         synchronized (lock) {
            return super.containsKey(key);
         }
      }

      @Override
      public V get(Object key) {
         synchronized (lock) {
            return super.get(key);
         }
      }

      @Override
      public V remove(Object key) {
         synchronized (lock) {
            // we don't need to call super because we don't need this return value
            // in a synchronized wrapper
            return internal().remove(key);
         }
      }

      @Override
      public Set<K> keySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(super.keySet(), lock);
         }
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(super.entrySet(), lock);
         }
      }

      @Override
      public int size() {
         synchronized (lock) {
            return super.size();
         }
      }

      @Override
      public boolean isEmpty() {
         synchronized (lock) {
            return super.isEmpty();
         }
      }

      @Override
      public boolean containsValue(Object value) {
         synchronized (lock) {
            return super.containsValue(value);
         }
      }

      @Override
      public V put(K key, V value) {
         synchronized (lock) {
            return super.put(key, value);
         }
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         synchronized (lock) {
            super.putAll(m);
         }
      }

      @Override
      public void clear() {
         synchronized (lock) {
            super.clear();
         }
      }

      @Override
      public Collection<V> values() {
         synchronized (lock) {
            return new SynchronizedCollection<>(super.values(), lock);
         }
      }

      @Override
      public boolean equals(Object o) {
         synchronized (lock) {
            return super.equals(o);
         }
      }

      @Override
      public int hashCode() {
         synchronized (lock) {
            return super.hashCode();
         }
      }

      @Override
      public String toString() {
         synchronized (lock) {
            return super.toString();
         }
      }

      @Override
      protected Object clone() throws CloneNotSupportedException {
         synchronized (lock) {
            return super.clone();
         }
      }
   }

   /**
    * A collection that synchronizes all access using the monitor of a given lock object.
    *
    * @param <E> the type of elements in the collection
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class SynchronizedCollection<E> implements Collection<E> {
      private final Collection<E> coll;
      private final Object lock;
      
      /**
       * Creates a new collection that synchronizes all access using the given lock. This is very
       * similar to {@link Collections#synchronizedCollection(Collection)} except that it
       * synchronizes on the given lock instead of on itself.
       *
       * @param coll the underlying collection
       * @param lock the object used as a lock
       */
      SynchronizedCollection(Collection<E> coll, Object lock) {
         this.coll = coll;
         this.lock = lock;
      }

      @Override
      public int size() {
         synchronized (lock) {
            return coll.size();
         }
      }

      @Override
      public boolean isEmpty() {
         synchronized (lock) {
            return coll.isEmpty();
         }
      }

      @Override
      public boolean contains(Object o) {
         synchronized (lock) {
            return coll.contains(o);
         }
      }

      @Override
      public Iterator<E> iterator() {
         synchronized (lock) {
            return new SynchronizedIterator<>(coll.iterator(), lock);
         }
      }

      @Override
      public Object[] toArray() {
         synchronized (lock) {
            return coll.toArray();
         }
      }

      @Override
      public <T> T[] toArray(T[] a) {
         synchronized (lock) {
            return coll.toArray(a);
         }
      }

      @Override
      public boolean add(E e) {
         synchronized (lock) {
            return coll.add(e);
         }
      }

      @Override
      public boolean remove(Object o) {
         synchronized (lock) {
            return coll.remove(o);
         }
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         synchronized (lock) {
            return coll.containsAll(c);
         }
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         synchronized (lock) {
            return coll.addAll(c);
         }
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         synchronized (lock) {
            return coll.retainAll(c);
         }
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         synchronized (lock) {
            return coll.removeAll(c);
         }
      }

      @Override
      public void clear() {
         synchronized (lock) {
            coll.clear();
         }
      }
      
      @Override
      public boolean equals(Object o) {
         synchronized (lock) {
            return coll.equals(o);
         }
      }
      
      @Override
      public int hashCode() {
         synchronized (lock) {
            return coll.hashCode();
         }
      }

      @Override
      public String toString() {
         synchronized (lock) {
            return coll.toString();
         }
      }
      
      @Override
      protected Object clone() throws CloneNotSupportedException {
         synchronized (lock) {
            return super.clone();
         }
      }
   }
   
   /**
    * A set that synchronizes all access using the monitor of a given lock object.
    *
    * @param <E> the type of elements in the set
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {
      
      /**
       * Creates a new set that synchronizes all access using the given lock. This is very similar
       * to {@link Collections#synchronizedSet(Set)} except that it synchronizes on the given lock
       * instead of on itself.
       *
       * @param set the underlying set
       * @param lock the object used as a lock
       */
      SynchronizedSet(Set<E> set, Object lock) {
         super(set, lock);
      }
   }

   /**
    * An iterator that synchronizes all operations on the monitor of a given object.
    *
    * @param <E> the type of elements returned by the iterator
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class SynchronizedIterator<E> implements Iterator<E> {
      private final Iterator<E> iter;
      private final Object lock;
      
      /**
       * Creates a new iterator that synchronizes all operations using the given object as a lock.
       *
       * @param iter the underlying iterator
       * @param lock the object used as a lock
       */
      SynchronizedIterator(Iterator<E> iter, Object lock) {
         this.iter = iter;
         this.lock = lock;
      }

      @Override
      public boolean hasNext() {
         synchronized (lock) {
            return iter.hasNext();
         }
      }

      @Override
      public E next() {
         synchronized (lock) {
            return iter.next();
         }
      }
      
      @Override
      public void remove() {
         synchronized (lock) {
            iter.remove();
         }
      }

      @Override
      public void forEachRemaining(Consumer<? super E> action) {
         synchronized (lock) {
            iter.forEachRemaining(action);
         }
      }

      @Override
      public int hashCode() {
         synchronized (lock) {
            return iter.hashCode();
         }
      }

      @Override
      public boolean equals(Object obj) {
         synchronized (lock) {
            return iter.equals(obj);
         }
      }

      @Override
      public String toString() {
         synchronized (lock) {
            return iter.toString();
         }
      }
      
      @Override
      protected Object clone() throws CloneNotSupportedException {
         synchronized (lock) {
            return super.clone();
         }
      }
   }
   
   /**
    * A read-only wrapper around a {@link TableMapView}. The underlying map can be modified outside
    * of the wrapper, but the wrapper throws {@link UnsupportedOperationException} for all methods
    * that might modify the map. Since a table-map-view contains map and table views as its values,
    * the values are also decorated with read-only wrappers.
    *
    * @param <K> the type of keys in the map
    * @param <V> the type of values in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class UnmodifiableTableMapView<K, V> extends TransformingTableMapView<K, V, V> {

      /**
       * Creates a new table-map-view that does not allow modifications.
       *
       * @param map the underlying map
       * @param unmodifiable a function that creates an unmodifiable view of a value
       */
      UnmodifiableTableMapView(TableMapView<K, V> map, Function<V, V> unmodifiable) {
         super(map, unmodifiable);
      }
      
      @Override
      public V remove(Object key) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Set<K> keySet() {
         return Collections.unmodifiableSet(super.keySet());
      }

      @Override
      public Collection<V> values() {
         return Collections.unmodifiableCollection(super.values());
      }

      @Override
      public Set<Entry<K, V>> entrySet() {
         return Collections.unmodifiableSet(super.entrySet());
      }
      
      @Override
      public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V putIfAbsent(K key, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(Object key, Object value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V replace(K key, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V computeIfPresent(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V compute(K key,
            BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V merge(K key, V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
         throw new UnsupportedOperationException();
      }
   }
}
