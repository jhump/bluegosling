package com.bluegosling.collections;

import com.bluegosling.collections.Tables.MapWithoutPut;
import com.bluegosling.collections.Tables.SynchronizedCollection;
import com.bluegosling.collections.Tables.SynchronizedMap;
import com.bluegosling.collections.Tables.SynchronizedSet;
import com.bluegosling.collections.Tables.SynchronizedTableMapView;
import com.bluegosling.collections.Tables.TransformingTableMapView;
import com.bluegosling.collections.Tables.UnmodifiableTableMapView;
import com.bluegosling.collections.Tables3D.Table3DImpl;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

// TODO: doc
// TODO: tests
final class Tables2D {
   private Tables2D() {
   }
   
   /**
    * An empty, immutable {@link TableMapView} for a 2-dimensional table.
    */
   static final TableMapView<Object, Map<Object, Object>> EMPTY_MAP_VIEW =
         new Tables.EmptyTableMapView<>(Collections.emptyMap());

   /**
    * An empty, immutable {@link Table2D}.
    */
   static final Table2D<Object, Object, Object> EMPTY = new Table2D<Object, Object, Object>() {
      @Override
      public int size() {
         return 0;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public TableMapView<Object, Map<Object, Object>> asMap() {
         return EMPTY_MAP_VIEW;
      }

      @Override
      public Table2D<Object, Object, Object> transpose() {
         return EMPTY;
      }

      @Override
      public Map<Object, Object> getRow(Object rowKey) {
         return Collections.emptyMap();
      }

      @Override
      public Map<Object, Object> getColumn(Object columnKey) {
         return Collections.emptyMap();
      }

      @Override
      public Object get(Object rowKey, Object columnKey) {
         return null;
      }

      @Override
      public Object remove(Object rowKey, Object columnKey) {
         throw new UnsupportedOperationException();
      }

      @Override
      public Object put(Object rowKey, Object columnKey, Object value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Table2D<? extends Object, ? extends Object, ? extends Object> table) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         return false;
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         return false;
      }

      @Override
      public boolean containsRow(Object rowKey) {
         return false;
      }

      @Override
      public Set<Object> rowKeySet() {
         return Collections.emptySet();
      }

      @Override
      public Set<Object> columnKeySet() {
         return Collections.emptySet();
      }

      @Override
      public Map<Object, Set<Object>> columnIndex() {
         return Collections.emptyMap();
      }

      @Override
      public Collection<Object> values() {
         return Collections.emptyList();
      }

      @Override
      public Set<Cell2D<Object, Object, Object>> cellSet() {
         return Collections.emptySet();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Table2D && ((Table2D<?, ?, ?>) o).isEmpty();
      }
      
      @Override
      public int hashCode() {
         return 0;
      }
      
      @Override
      public String toString() {
         return "{ }";
      }
   };

   /**
    * A {@link Map} implementation that represents a leaf level in a nested map. Since a nested map
    * can be transposed and rotated, the "leaf" level may not actually be the most deeply nested
    * level in the actual nesting structure.
    *
    * @param <C> the type of columns in the enclosing 2D table which are keys in the map
    * @param <V> the type of values in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class LeafMapImpl<C, V> extends NestedMap<Object, V> implements Map<C, V> {
      
      LeafMapImpl(Table2DImpl<?, ?, V> other, Object filterKey, int filterHeight) {
         super(other, filterKey, filterHeight);
         assert this.height() == 0;
      }

      @Override
      public boolean containsKey(Object key) {
         return containsKey(makeKeys(key));
      }

      @Override
      public boolean containsValue(Object value) {
         return values().contains(value);
      }

      @Override
      public V get(Object key) {
         return get(makeKeys(key));
      }

      @Override
      public V put(C key, V value) {
         return put(makeKeys(key), value);
      }

      @Override
      public V remove(Object key) {
         return remove(makeKeys(key));
      }

      @Override
      public void putAll(Map<? extends C, ? extends V> m) {
         for (Entry<? extends C, ? extends V> entry : m.entrySet()) {
            put(makeKeys(entry.getKey()), entry.getValue());
         }
      }

      @Override
      public Set<C> keySet() {
         @SuppressWarnings("unchecked")
         Set<C> ret = (Set<C>) keysAtHeight(0);
         return ret;
      }
      
      @Override
      public Set<Entry<C, V>> entrySet() {
         return new AbstractSet<Entry<C, V>>() {
            @Override
            public Iterator<Entry<C, V>> iterator() {
               return LeafMapImpl.this.<Entry<C, V>>entryIterator((k, e) -> new Entry<C, V>() {
                  @Override
                  public C getKey() {
                     @SuppressWarnings("unchecked")
                     C ret = (C) k.getKey(0);
                     return ret;
                  }

                  @Override
                  public V getValue() {
                     return e.getValue();
                  }

                  @Override
                  public V setValue(V value) {
                     return e.setValue(value);
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
               });
            }
            
            @Override
            public boolean contains(Object o) {
               if (!(o instanceof Entry)) {
                  return false;
               }
               Entry<?, ?> other = (Entry<?, ?>) o;
               V v = get(other.getKey());
               if (v == null) {
                  return containsKey(other.getKey()) && other.getValue() == null;
               } else {
                  return v.equals(other.getValue());
               }
            }
            
            @Override
            public boolean remove(Object o) {
               if (!(o instanceof Entry)) {
                  return false;
               }
               Entry<?, ?> other = (Entry<?, ?>) o;
               V v = get(other.getKey());
               if (v == null) {
                  if (containsKey(other.getKey()) && other.getValue() == null) {
                     LeafMapImpl.this.remove(other.getKey());
                     return true;
                  }
               } else if (v.equals(other.getValue())) {
                  LeafMapImpl.this.remove(other.getKey());
                  return true;
               }
               return false;
            }

            @Override
            public int size() {
               return LeafMapImpl.this.size();
            }
         };
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
   }
   
   /**
    * A view of a {@link Table2D} as a map of maps.
    *
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table2DMapViewImpl<R, C, V> extends AbstractMap<R, Map<C, V>>
         implements TableMapView<R, Map<C, V>> {
      
      private final Table2DImpl<R, C, V> table;
      
      Table2DMapViewImpl(Table2DImpl<R, C, V> table) {
         this.table = table;
      }

      @Override
      public Set<R> keySet() {
         return table.rowKeySet();
      }

      @Override
      public Map<C, V> getViewForKey(R rowKey) {
         return new LeafMapImpl<C, V>(table, rowKey, 1);
      }

      @Override
      public Map<C, V> get(Object o) {
         return new MapWithoutPut<>(new LeafMapImpl<C, V>(table, o, 1));
      }
      
      @Override
      public Map<C, V> remove(Object o) {
         Map<C, V> removed = get(o);
         if (removed == null) {
            return Collections.emptyMap();
         }
         Map<C, V> ret = new LinkedHashMap<>();
         for (Iterator<Entry<C, V>> iter = removed.entrySet().iterator(); iter.hasNext(); ) {
            Entry<C, V> entry = iter.next();
            ret.put(entry.getKey(), entry.getValue());
            iter.remove();
         }
         return Collections.unmodifiableMap(ret);
      }
      
      @Override
      public Set<Entry<R, Map<C, V>>> entrySet() {
         return new TransformingSet<>(table.rowKeySet(),
               k -> new AbstractMap.SimpleImmutableEntry<>(k, getViewForKey(k)));
      }
   }

   /**
    * A 2-dimensional table implementation that is backed by a nested map. The actual nested map may
    * have greater than two dimensions (in which case one or more levels is filtered).
    *
    * @param <R> the type of row keys in the table
    * @param <C> the type of column keys in the table
    * @param <V> the type of values in the table
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table2DImpl<R, C, V> extends NestedMap<Object, V> implements Table2D<R, C, V> {
      
      Table2DImpl(IntFunction<Map<Object, Object>> mapMaker) {
         super(mapMaker, 1);
      }

      Table2DImpl(Table3DImpl<?, ?, ?, V> other, Object filterKey, int filterHeight) {
         super(other, filterKey, filterHeight);
         assert this.height() == 1;
      }

      private Table2DImpl(Table2DImpl<C, R, V> other) {
         super(other, true);
         assert this.height() == 1;
      }

      @Override
      public TableMapView<R, Map<C, V>> asMap() {
         return new Table2DMapViewImpl<>(this); 
      }

      @Override
      public Table2D<C, R, V> transpose() {
         return new Table2DImpl<C, R, V>(this);
      }

      @Override
      public Map<C, V> getRow(R rowKey) {
         return new LeafMapImpl<>(this, rowKey, 1);
      }

      @Override
      public Map<R, V> getColumn(C columnKey) {
         return new LeafMapImpl<>(this, columnKey, 0);
      }

      @Override
      public V get(Object rowKey, Object columnKey) {
         return get(makeKeys(rowKey, columnKey));
      }

      @Override
      public V remove(Object rowKey, Object columnKey) {
         return remove(makeKeys(rowKey, columnKey));
      }

      @Override
      public V put(R rowKey, C columnKey, V value) {
         return put(makeKeys(rowKey, columnKey), value);
      }

      @Override
      public void putAll(Table2D<? extends R, ? extends C, ? extends V> table) {
         for (Cell2D<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
            put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
         }
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         return containsKey(makeKeys(rowKey, columnKey));
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         return containsKey(makeKeys(columnKey, 0));
      }

      @Override
      public boolean containsRow(Object rowKey) {
         return containsKey(makeKeys(rowKey, 1));
      }

      @Override
      public Set<R> rowKeySet() {
         @SuppressWarnings("unchecked")
         Set<R> ret = (Set<R>) keysAtHeight(1);
         return ret;
      }

      @Override
      public Set<C> columnKeySet() {
         @SuppressWarnings("unchecked")
         Set<C> ret = (Set<C>) keysAtHeight(0);
         return ret;
      }

      @Override
      public Map<R, Set<C>> columnIndex() {
         return TransformingMap.transformingValues(asMap(), Map::keySet);
      }
      
      @Override
      public Set<Cell2D<R, C, V>> cellSet() {
         return new AbstractSet<Cell2D<R, C, V>>() {
            @Override
            public Iterator<Cell2D<R, C, V>> iterator() {
               return Table2DImpl.this.<Cell2D<R, C, V>>entryIterator((k, e) -> {
                  return new Cell2D<R, C, V>() {
                     @Override
                     public R getRowKey() {
                        @SuppressWarnings("unchecked")
                        R ret = (R) k.getKey(1);
                        return ret;
                     }

                     @Override
                     public C getColumnKey() {
                        @SuppressWarnings("unchecked")
                        C ret = (C) k.getKey(0);
                        return ret;
                     }

                     @Override
                     public V getValue() {
                        return e.getValue();
                     }

                     @Override
                     public V setValue(V newValue) {
                        return e.setValue(newValue);
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
                  };
               });
            }

            @Override
            public int size() {
               return Table2DImpl.this.size();
            }
         };
      }
      
      @Override
      public boolean equals(Object o) {
         return Tables.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return asMap().hashCode();
      }
      
      @Override
      public String toString() {
         return asMap().toString();
      }
   }
   
   /**
    * A {@link Table2D} that synchronizes all operations via a given lock.
    *
    * @param <R> the type of row keys
    * @param <C> the type of column keys
    * @param <V> the type of values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static final class SynchronizedTable2D<R, C, V> implements Table2D<R, C, V> {
      private final Object lock;
      private final Table2D<R, C, V> table;
      
      /**
       * Creates a synchronized wrapper for the given table. All operations are synchronized on
       * {@code this}.
       *
       * @param table an unsynchronized table
       * @see Collections#synchronizedMap(Map)
       */
      SynchronizedTable2D(Table2D<R, C, V> table) {
         this.table = table;
         this.lock = this;
      }

      /**
       * Creates a synchronized wrapper for the given table that synchronizes on the given lock.
       *
       * @param table an unsynchronized table
       * @param lock an object whose intrinsic lock is used to synchronize operations
       */
      SynchronizedTable2D(Table2D<R, C, V> table, Object lock) {
         this.table = table;
         this.lock = lock;
      }

      @Override
      public int size() {
         synchronized (lock) {
            return table.size();
         }
      }

      @Override
      public boolean isEmpty() {
         synchronized (lock) {
            return table.isEmpty();
         }
      }

      @Override
      public TableMapView<R, Map<C, V>> asMap() {
         synchronized (lock) {
            return new SynchronizedTableMapView<>(table.asMap(), lock,
                  m -> new SynchronizedMap<>(m, lock));
         }
      }

      @Override
      public Table2D<C, R, V> transpose() {
         synchronized (lock) {
            return new SynchronizedTable2D<>(table.transpose(), lock);
         }
      }

      @Override
      public Map<C, V> getRow(R rowKey) {
         synchronized (lock) {
            return new SynchronizedMap<>(table.getRow(rowKey), lock);
         }
      }

      @Override
      public Map<R, V> getColumn(C columnKey) {
         synchronized (lock) {
            return new SynchronizedMap<>(table.getColumn(columnKey), lock);
         }
      }

      @Override
      public V get(Object rowKey, Object columnKey) {
         synchronized (lock) {
            return table.get(rowKey, columnKey);
         }
      }

      @Override
      public V remove(Object rowKey, Object columnKey) {
         synchronized (lock) {
            return table.remove(rowKey, columnKey);
         }
      }

      @Override
      public V put(R rowKey, C columnKey, V value) {
         synchronized (lock) {
            return table.put(rowKey, columnKey, value);
         }
      }

      @Override
      public void putAll(Table2D<? extends R, ? extends C, ? extends V> tbl) {
         synchronized (lock) {
            table.putAll(tbl);
         }
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         synchronized (lock) {
            return table.containsCell(rowKey, columnKey);
         }
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         synchronized (lock) {
            return table.containsColumn(columnKey);
         }
      }

      @Override
      public boolean containsRow(Object rowKey) {
         synchronized (lock) {
            return table.containsRow(rowKey);
         }
      }

      @Override
      public Set<R> rowKeySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(table.rowKeySet(), lock);
         }
      }

      @Override
      public Set<C> columnKeySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(table.columnKeySet(), lock);
         }
      }

      @Override
      public Map<R, Set<C>> columnIndex() {
         synchronized (lock) {
            return new SynchronizedMap<>(
                  new TransformingMap.ValuesOnly<>(table.columnIndex(),
                        (k, s) -> new SynchronizedSet<>(s, lock)),
                  lock);
         }
      }

      @Override
      public Collection<V> values() {
         synchronized (lock) {
            return new SynchronizedCollection<>(table.values(), lock);
         }
      }

      @Override
      public Set<Cell2D<R, C, V>> cellSet() {
         synchronized (lock) {
            return new SynchronizedSet<>(table.cellSet(), lock);
         }
      }

      @Override
      public void clear() {
         synchronized (lock) {
            table.clear();
         }
      }
      
      @Override
      public boolean equals(Object o) {
         synchronized (lock) {
            return table.equals(o);
         }
      }
      
      @Override
      public int hashCode() {
         synchronized (lock) {
            return table.hashCode();
         }
      }
      
      @Override
      public String toString() {
         synchronized (lock) {
            return table.toString();
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
    * An unmodifiable view of a {@link Table2D}. All operations that might mutate the table throw
    * {@link UnsupportedOperationException}. Since the object is just a view, any changes to the
    * underlying table are visible through this unmodifiable view.
    *
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @see Collections#unmodifiableMap(Map)
    */
   static class UnmodifiableTable2D<R, C, V> implements Table2D<R, C, V> {
      private final Table2D<R, C, V> table;
      
      UnmodifiableTable2D(Table2D<R, C, V> table) {
         this.table = table;
      }

      @Override
      public int size() {
         return table.size();
      }

      @Override
      public boolean isEmpty() {
         return table.isEmpty();
      }

      @Override
      public TableMapView<R, Map<C, V>> asMap() {
         return new UnmodifiableTableMapView<>(table.asMap(), Collections::unmodifiableMap);
      }

      @Override
      public Table2D<C, R, V> transpose() {
         return new UnmodifiableTable2D<>(table.transpose());
      }

      @Override
      public Map<C, V> getRow(R rowKey) {
         return Collections.unmodifiableMap(table.getRow(rowKey));
      }

      @Override
      public Map<R, V> getColumn(C columnKey) {
         return Collections.unmodifiableMap(table.getColumn(columnKey));
      }

      @Override
      public V get(Object rowKey, Object columnKey) {
         return table.get(rowKey, columnKey);
      }

      @Override
      public V remove(Object rowKey, Object columnKey) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V put(R rowKey, C columnKey, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Table2D<? extends R, ? extends C, ? extends V> tbl) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         return table.containsCell(rowKey, columnKey);
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         return table.containsColumn(columnKey);
      }

      @Override
      public boolean containsRow(Object rowKey) {
         return table.containsRow(rowKey);
      }

      @Override
      public Set<R> rowKeySet() {
         return Collections.unmodifiableSet(table.rowKeySet());
      }

      @Override
      public Set<C> columnKeySet() {
         return Collections.unmodifiableSet(table.columnKeySet());
      }

      @Override
      public Map<R, Set<C>> columnIndex() {
         return TransformingMap.ReadOnly.transformingValues(table.columnIndex(),
               s -> Collections.unmodifiableSet(s));
      }

      @Override
      public Collection<V> values() {
         return Collections.unmodifiableCollection(table.values());
      }

      @Override
      public Set<Cell2D<R, C, V>> cellSet() {
         return Collections.unmodifiableSet(table.cellSet());
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }
   }
   
   static class TransformingTable2D<R, C, VI, VO> implements Table2D<R, C, VO> {
      private final Table2D<R, C, VI> table;
      private final Function<? super VI, ? extends VO> fn;
      
      TransformingTable2D(Table2D<R, C, VI> table, Function<? super VI, ? extends VO> fn) {
         this.table = table;
         this.fn = fn;
      }

      @Override
      public int size() {
         return table.size();
      }

      @Override
      public boolean isEmpty() {
         return table.isEmpty();
      }

      @Override
      public TableMapView<R, Map<C, VO>> asMap() {
         return new TransformingTableMapView<>(table.asMap(),
               m -> new TransformingMap.ValuesOnly<>(m, (k, v) -> fn.apply(v)));
      }

      @Override
      public Table2D<C, R, VO> transpose() {
         return new TransformingTable2D<>(table.transpose(), fn);
      }

      @Override
      public Map<C, VO> getRow(R rowKey) {
         return new TransformingMap.ValuesOnly<>(table.getRow(rowKey), (k, v) -> fn.apply(v));
      }

      @Override
      public Map<R, VO> getColumn(C columnKey) {
         return new TransformingMap.ValuesOnly<>(table.getColumn(columnKey), (k, v) -> fn.apply(v));
      }

      @Override
      public VO get(Object rowKey, Object columnKey) {
         VI v = table.get(rowKey, columnKey);
         return v == null && !table.containsCell(rowKey, columnKey) ? null : fn.apply(v);
      }

      @Override
      public VO remove(Object rowKey, Object columnKey) {
         if (table.containsCell(rowKey, columnKey)) {
            return fn.apply(table.remove(rowKey, columnKey));
         }
         return null;
      }

      @Override
      public VO put(R rowKey, C columnKey, VO value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Table2D<? extends R, ? extends C, ? extends VO> tbl) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         return table.containsCell(rowKey, columnKey);
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         return table.containsColumn(columnKey);
      }

      @Override
      public boolean containsRow(Object rowKey) {
         return table.containsRow(rowKey);
      }

      @Override
      public Set<R> rowKeySet() {
         return table.rowKeySet();
      }

      @Override
      public Set<C> columnKeySet() {
         return table.columnKeySet();
      }

      @Override
      public Map<R, Set<C>> columnIndex() {
         return table.columnIndex();
      }

      @Override
      public Collection<VO> values() {
         return new TransformingCollection<>(table.values(), fn);
      }

      @Override
      public Set<Cell2D<R, C, VO>> cellSet() {
         Function<? super VI, ? extends VO> function = this.fn;
         return new TransformingSet<Cell2D<R, C, VI>, Cell2D<R, C, VO>>(table.cellSet(),
               c -> new Cell2D<R, C, VO>() {
                  @Override
                  public R getRowKey() {
                     return c.getRowKey();
                  }
      
                  @Override
                  public C getColumnKey() {
                     return c.getColumnKey();
                  }
      
                  @Override
                  public VO getValue() {
                     return function.apply(c.getValue());
                  }
      
                  @Override
                  public VO setValue(VO newValue) {
                     throw new UnsupportedOperationException();
                  }
               });
      }

      @Override
      public void clear() {
         table.clear();
      }
      
      @Override
      public boolean equals(Object o) {
         return Tables.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return asMap().hashCode();
      }

      @Override
      public String toString() {
         return asMap().toString();
      }
   }
}
