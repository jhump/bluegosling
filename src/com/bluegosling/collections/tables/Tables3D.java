package com.bluegosling.collections.tables;

import com.bluegosling.collections.MoreIterators;
import com.bluegosling.collections.tables.Table2D.Cell2D;
import com.bluegosling.collections.tables.Tables.ImmutableCell2D;
import com.bluegosling.collections.tables.Tables.MapWithoutPut;
import com.bluegosling.collections.tables.Tables.SynchronizedCollection;
import com.bluegosling.collections.tables.Tables.SynchronizedMap;
import com.bluegosling.collections.tables.Tables.SynchronizedSet;
import com.bluegosling.collections.tables.Tables.SynchronizedTableMapView;
import com.bluegosling.collections.tables.Tables.TableMapViewWithoutPut;
import com.bluegosling.collections.tables.Tables.TransformingTableMapView;
import com.bluegosling.collections.tables.Tables.UnmodifiableTableMapView;
import com.bluegosling.collections.tables.Tables2D.SynchronizedTable2D;
import com.bluegosling.collections.tables.Tables2D.Table2DImpl;
import com.bluegosling.collections.tables.Tables2D.TransformingTable2D;
import com.bluegosling.collections.tables.Tables2D.UnmodifiableTable2D;
import com.bluegosling.collections.tables.Tables4D.Table4DImpl;
import com.bluegosling.collections.views.TransformingIterator;
import com.bluegosling.collections.views.TransformingMap;
import com.bluegosling.collections.views.TransformingSet;
import com.google.common.collect.Iterators;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntFunction;

//TODO: doc
//TODO: tests
final class Tables3D {
   private Tables3D() {
   }

   static final TableMapView<Object, Table2D<Object, Object, Object>> EMPTY_MAP_VIEW =
         new Tables.EmptyTableMapView<>(Table2D.empty());
   
   static final Table3D<Object, Object, Object, Object> EMPTY =
         new Table3D<Object, Object, Object, Object>() {
            @Override
            public int size() {
               return 0;
            }

            @Override
            public boolean isEmpty() {
               return true;
            }

            @Override
            public TableMapView<Object, Table2D<Object, Object, Object>> asMap() {
               return EMPTY_MAP_VIEW;
            }

            @Override
            public Table3D<Object, Object, Object, Object> transpose() {
               return EMPTY;
            }

            @Override
            public Table3D<Object, Object, Object, Object> rotate() {
               return EMPTY;
            }

            @Override
            public Table2D<Object, Object, Object> getPage(Object pageKey) {
               return Tables2D.EMPTY;
            }

            @Override
            public Map<Object, Object> getRow(Object pageKey, Object rowKey) {
               return Collections.emptyMap();
            }

            @Override
            public Map<Object, Object> getColumn(Object pageKey, Object columnKey) {
               return Collections.emptyMap();
            }

            @Override
            public Table2D<Object, Object, Object> getRows(Object rowKey) {
               return Tables2D.EMPTY;
            }

            @Override
            public Table2D<Object, Object, Object> getColumns(Object columnKey) {
               return Tables2D.EMPTY;
            }

            @Override
            public Object get(Object pageKey, Object rowKey, Object columnKey) {
               return null;
            }

            @Override
            public Object remove(Object pageKey, Object rowKey, Object columnKey) {
               throw new UnsupportedOperationException();
            }

            @Override
            public Object put(Object pageKey, Object rowKey, Object columnKey, Object value) {
               throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(
                  Table3D<? extends Object, ? extends Object, ? extends Object, ? extends Object> table) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsCell(Object pageKey, Object rowKey, Object columnKey) {
               return false;
            }

            @Override
            public boolean containsColumn(Object pageKey, Object columnKey) {
               return false;
            }

            @Override
            public boolean containsRow(Object pageKey, Object rowKey) {
               return false;
            }

            @Override
            public boolean containsPage(Object pageKey) {
               return false;
            }

            @Override
            public boolean containsColumnKey(Object columnKey) {
               return false;
            }

            @Override
            public boolean containsRowKey(Object rowKey) {
               return false;
            }

            @Override
            public Set<Object> pageKeySet() {
               return Collections.emptySet();
            }

            @Override
            public Set<Object> rowKeySet() {
               return Collections.emptySet();
            }

            @Override
            public Map<Object, Set<Object>> rowIndex() {
               return Collections.emptyMap();
            }

            @Override
            public Set<Object> columnKeySet() {
               return Collections.emptySet();
           }

            @Override
            public Table2D<Object, Object, Set<Object>> columnIndex() {
               return Table2D.empty();
            }

            @Override
            public Collection<Object> values() {
               return Collections.emptyList();
            }

            @Override
            public Set<Cell3D<Object, Object, Object, Object>> cellSet() {
               return Collections.emptySet();
            }

            @Override
            public void clear() {
               throw new UnsupportedOperationException();
            }
      
            @Override
            public boolean equals(Object o) {
               return o instanceof Table3D && ((Table3D<?, ?, ?, ?>) o).isEmpty();
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
    * A 2-dimensional table implementation that is an index view of the keys of a 3-dimensional
    * table.
    *
    * @param <P> the type of page keys in the underlying table
    * @param <R> the type of row keys in the underlying table
    * @param <C> the type of column keys in the underlying table
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table2DIndexImpl<P, R, C, V> implements Table2D<P, R, Set<C>> {
      final Table3D<P, R, C, V> base;
      
      Table2DIndexImpl(Table3D<P, R, C, V> base) {
         this.base = base;
      }

      @Override
      public int size() {
         int sz[] = new int[1];
         base.rowIndex().forEach((p, set) -> sz[0] += set.size());
         return sz[0];
      }

      @Override
      public boolean isEmpty() {
         return base.isEmpty();
      }

      @Override
      public TableMapView<P, Map<R, Set<C>>> asMap() {
         return new TransformingTableMapView<>(base.asMap(), v -> v.columnIndex());
      }

      @Override
      public Table2D<R, P, Set<C>> transpose() {
         return base.rotate().transpose().columnIndex();
      }

      @Override
      public Map<R, Set<C>> getRow(P rowKey) {
         return base.getPage(rowKey).columnIndex();
      }

      @Override
      public Map<P, Set<C>> getColumn(R columnKey) {
         return base.getRows(columnKey).columnIndex();
      }

      @Override
      public Set<C> get(Object rowKey, Object columnKey) {
         Table2D<R, C, ?> tbl = base.asMap().get(rowKey);
         if (tbl == null) {
            return null;
         }
         Map<C, ?> map = tbl.asMap().get(columnKey);
         return map == null ? null : map.keySet();
      }

      @Override
      public Set<C> remove(Object rowKey, Object columnKey) {
         Table2D<R, C, ?> tbl = base.asMap().get(rowKey);
         if (tbl == null) {
            return null;
         }
         Map<C, ?> map = tbl.asMap().remove(columnKey);
         return map == null ? null : map.keySet();
      }

      @Override
      public Set<C> put(P rowKey, R columnKey, Set<C> value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Table2D<? extends P, ? extends R, ? extends Set<C>> table) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         return base.containsRow(rowKey, columnKey);
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         return base.containsRowKey(columnKey);
      }

      @Override
      public boolean containsRow(Object rowKey) {
         return base.containsPage(rowKey);
      }

      @Override
      public Set<P> rowKeySet() {
         return base.pageKeySet();
      }

      @Override
      public Set<R> columnKeySet() {
         return base.rowKeySet();
      }

      @Override
      public Map<P, Set<R>> columnIndex() {
         return TransformingMap.transformingValues(base.asMap(), v -> v.rowKeySet());
      }

      @Override
      public Collection<Set<C>> values() {
         return new AbstractCollection<Set<C>>() {
            @Override
            public Iterator<Set<C>> iterator() {
               return MoreIterators.flatMap(base.asMap().values().iterator(),
                     (Table2D<R, C, ?> t) -> new TransformingIterator<>(
                           t.asMap().values().iterator(), (Map<C, ?> m) -> m.keySet()));
            }

            @Override
            public int size() {
               return Iterators.size(iterator());
            }
         };
      }

      @Override
      public Set<Cell2D<P, R, Set<C>>> cellSet() {
         return new AbstractSet<Cell2D<P, R, Set<C>>>() {
            @Override
            public Iterator<Cell2D<P, R, Set<C>>> iterator() {
               return MoreIterators.flatMap(base.asMap().entrySet().iterator(),
                     (Entry<P, Table2D<R, C, V>> e1) ->
                           new TransformingIterator<>(e1.getValue().asMap().entrySet().iterator(),
                                 (Entry<R, Map<C, V>> e2) ->
                                       new ImmutableCell2D<>(e1.getKey(), e2.getKey(),
                                             e2.getValue().keySet())));
            }

            @Override
            public int size() {
               return Iterators.size(iterator());
            }
         };
      }

      @Override
      public void clear() {
         base.clear();
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
   
   static class Table2DWithoutPut<R, C, V> implements Table2D<R, C, V> {

      private final Table2D<R, C, V> base;
      
      Table2DWithoutPut(Table2D<R, C, V> base) {
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
      public TableMapView<R, Map<C, V>> asMap() {
         return new TableMapViewWithoutPut<>(base.asMap());
      }

      @Override
      public Table2D<C, R, V> transpose() {
         return new Table2DWithoutPut<>(base.transpose());
      }

      @Override
      public Map<C, V> getRow(R rowKey) {
         return new MapWithoutPut<>(base.getRow(rowKey));
      }

      @Override
      public Map<R, V> getColumn(C columnKey) {
         return new MapWithoutPut<>(base.getColumn(columnKey));
      }

      @Override
      public V get(Object rowKey, Object columnKey) {
         return base.get(rowKey, columnKey);
      }

      @Override
      public V remove(Object rowKey, Object columnKey) {
         return base.remove(rowKey, columnKey);
      }

      @Override
      public V put(R rowKey, C columnKey, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Table2D<? extends R, ? extends C, ? extends V> table) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsCell(Object rowKey, Object columnKey) {
         return base.containsCell(rowKey, columnKey);
      }

      @Override
      public boolean containsColumn(Object columnKey) {
         return base.containsColumn(columnKey);
      }

      @Override
      public boolean containsRow(Object rowKey) {
         return base.containsRow(rowKey);
      }

      @Override
      public Set<R> rowKeySet() {
         return base.rowKeySet();
      }

      @Override
      public Set<C> columnKeySet() {
         return base.columnKeySet();
      }

      @Override
      public Map<R, Set<C>> columnIndex() {
         return base.columnIndex();
      }

      @Override
      public Collection<V> values() {
         return base.values();
      }

      @Override
      public Set<Cell2D<R, C, V>> cellSet() {
         return base.cellSet();
      }

      @Override
      public void clear() {
         base.clear();
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
    * A view of a {@link Table3D} as a map of sub-tables.
    *
    * @param <P> the type of page key
    * @param <R> the type of row key
    * @param <C> the type of column key
    * @param <V> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table3DMapViewImpl<P, R, C, V> extends AbstractMap<P, Table2D<R, C, V>>
         implements TableMapView<P, Table2D<R, C, V>> {
      
      private final Table3DImpl<P, R, C, V> table;
      
      Table3DMapViewImpl(Table3DImpl<P, R, C, V> table) {
         this.table = table;
      }

      @Override
      public Set<P> keySet() {
         return table.pageKeySet();
      }

      @Override
      public Table2D<R, C, V> getViewForKey(P pageKey) {
         return new Table2DImpl<R, C, V>(table, pageKey, 2);
      }

      @Override
      public Table2D<R, C, V> get(Object o) {
         return new Table2DWithoutPut<>(new Table2DImpl<R, C, V>(table, o, 2));
      }
      
      @Override
      public Table2D<R, C, V> remove(Object o) {
         Table2D<R, C, V> removed = get(o);
         if (removed == null) {
            return null;
         }
         Table2D<R, C, V> ret = Table2D.newLinkedHashTable2D();
         for (Iterator<Cell2D<R, C, V>> iter = removed.cellSet().iterator(); iter.hasNext(); ) {
            Cell2D<R, C, V> cell = iter.next();
            ret.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            iter.remove();
         }
         return ret;
      }
      
      @Override
      public Set<Entry<P, Table2D<R, C, V>>> entrySet() {
         return new TransformingSet<>(table.pageKeySet(),
               k -> new AbstractMap.SimpleImmutableEntry<>(k, getViewForKey(k)));
      }
   }
   
   /**
    * A 3-dimensional table implementation that is backed by a nested map. The actual nested map may
    * have greater than three dimensions (in which case one or more levels is filtered).
    *
    * @param <P> the type of page keys in the table
    * @param <R> the type of row keys in the table
    * @param <C> the type of column keys in the table
    * @param <V> the type of values in the table
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table3DImpl<P, R, C, V> extends NestedMap<Object, V>
         implements Table3D<P, R, C, V> {

      Table3DImpl(IntFunction<Map<Object, Object>> mapMaker) {
         super(mapMaker, 2);
      }

      Table3DImpl(Table4DImpl<?, ?, ?, ?, V> other, Object filterKey, int filterHeight) {
         super(other, filterKey, filterHeight);
         assert this.height() == 2;
      }

      private Table3DImpl(Table3DImpl<?, ?, ?, V> other, boolean transpose) {
         super(other, transpose);
         assert this.height() == 2;
      }

      @Override
      public TableMapView<P, Table2D<R, C, V>> asMap() {
         return new Table3DMapViewImpl<>(this);
      }

      @Override
      public Table3D<P, C, R, V> transpose() {
         return new Table3DImpl<>(this, true);
      }

      @Override
      public Table3D<R, C, P, V> rotate() {
         return new Table3DImpl<>(this, false);
      }

      @Override
      public Table2D<R, C, V> getPage(P pageKey) {
         return new Table2DImpl<>(this, pageKey, 2);
      }

      @Override
      public Map<C, V> getRow(P pageKey, R rowKey) {
         return getPage(pageKey).getRow(rowKey);
      }

      @Override
      public Map<R, V> getColumn(P pageKey, C columnKey) {
         return getPage(pageKey).getColumn(columnKey);
      }

      @Override
      public Table2D<P, C, V> getRows(R rowKey) {
         return new Table2DImpl<>(this, rowKey, 1);
      }

      @Override
      public Table2D<P, R, V> getColumns(C columnKey) {
         return new Table2DImpl<>(this, columnKey, 0);
      }

      @Override
      public V get(Object pageKey, Object rowKey, Object columnKey) {
         return get(makeKeys(pageKey, rowKey, columnKey));
      }

      @Override
      public V remove(Object pageKey, Object rowKey, Object columnKey) {
         return remove(makeKeys(pageKey, rowKey, columnKey));
      }

      @Override
      public V put(P pageKey, R rowKey, C columnKey, V value) {
         return put(makeKeys(pageKey, rowKey, columnKey), value);
      }

      @Override
      public void putAll(Table3D<? extends P, ? extends R, ? extends C, ? extends V> table) {
         for (Cell3D<? extends P, ? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
            put(cell.getPageKey(), cell.getRowKey(), cell.getColumnKey(), cell.getValue());
         }
      }

      @Override
      public boolean containsCell(Object pageKey, Object rowKey, Object columnKey) {
         return containsKey(makeKeys(pageKey, rowKey, columnKey));
      }

      @Override
      public boolean containsColumn(Object pageKey, Object columnKey) {
         return containsKey(makeKeys(pageKey, 2, columnKey, 0));
      }

      @Override
      public boolean containsRow(Object pageKey, Object rowKey) {
         return containsKey(makeKeys(pageKey, 2, rowKey, 1));
      }

      @Override
      public boolean containsPage(Object pageKey) {
         return containsKey(makeKeys(pageKey, 2));
      }

      @Override
      public boolean containsColumnKey(Object columnKey) {
         return containsKey(makeKeys(columnKey, 0));
      }

      @Override
      public boolean containsRowKey(Object rowKey) {
         return containsKey(makeKeys(rowKey, 1));
      }

      @Override
      public Set<P> pageKeySet() {
         @SuppressWarnings("unchecked")
         Set<P> ret = (Set<P>) keysAtHeight(2);
         return ret;
      }

      @Override
      public Set<R> rowKeySet() {
         @SuppressWarnings("unchecked")
         Set<R> ret = (Set<R>) keysAtHeight(1);
         return ret;
      }

      @Override
      public Map<P, Set<R>> rowIndex() {
         return TransformingMap.transformingValues(asMap(), Table2D::rowKeySet);
      }

      @Override
      public Set<C> columnKeySet() {
         @SuppressWarnings("unchecked")
         Set<C> ret = (Set<C>) keysAtHeight(0);
         return ret;
      }

      @Override
      public Table2D<P, R, Set<C>> columnIndex() {
         return new Table2DIndexImpl<>(this);
      }

      @Override
      public Set<Cell3D<P, R, C, V>> cellSet() {
         return new AbstractSet<Cell3D<P, R, C, V>>() {
            @Override
            public Iterator<Cell3D<P, R, C, V>> iterator() {
               return Table3DImpl.this.<Cell3D<P, R, C, V>>entryIterator((k, e) -> {
                  return new Cell3D<P, R, C, V>() {
                     @Override
                     public P getPageKey() {
                        @SuppressWarnings("unchecked")
                        P ret = (P) k.getKey(2);
                        return ret;
                     }

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
               return Table3DImpl.this.size();
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
   
   static class SynchronizedTable3D<P, R, C, V> implements Table3D<P, R, C, V> {
      private final Table3D<P, R, C, V> table;
      private final Object lock;
      
      SynchronizedTable3D(Table3D<P, R, C, V> table) {
         this.table = table;
         this.lock = this;
      }
      
      SynchronizedTable3D(Table3D<P, R, C, V> table, Object lock) {
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
      public TableMapView<P, Table2D<R, C, V>> asMap() {
         synchronized (lock) {
            return new SynchronizedTableMapView<>(table.asMap(), lock,
                  t -> new SynchronizedTable2D<>(t, lock));
         }
      }

      @Override
      public Table3D<P, C, R, V> transpose() {
         synchronized (lock) {
            return table.transpose();
         }
      }

      @Override
      public Table3D<R, C, P, V> rotate() {
         synchronized (lock) {
            return table.rotate();
         }
      }

      @Override
      public Table2D<R, C, V> getPage(P pageKey) {
         synchronized (lock) {
            return new SynchronizedTable2D<>(table.getPage(pageKey), lock);
         }
      }

      @Override
      public Map<C, V> getRow(P pageKey, R rowKey) {
         synchronized (lock) {
            return new SynchronizedMap<>(table.getRow(pageKey, rowKey), lock);
         }
      }

      @Override
      public Map<R, V> getColumn(P pageKey, C columnKey) {
         synchronized (lock) {
            return new SynchronizedMap<>(table.getColumn(pageKey, columnKey), lock);
         }
      }

      @Override
      public Table2D<P, C, V> getRows(R rowKey) {
         synchronized (lock) {
            return new SynchronizedTable2D<>(table.getRows(rowKey), lock);
         }
      }

      @Override
      public Table2D<P, R, V> getColumns(C columnKey) {
         synchronized (lock) {
            return new SynchronizedTable2D<>(table.getColumns(columnKey), lock);
         }
      }

      @Override
      public V get(Object pageKey, Object rowKey, Object columnKey) {
         synchronized (lock) {
            return table.get(pageKey, rowKey, columnKey);
         }
      }

      @Override
      public V remove(Object pageKey, Object rowKey, Object columnKey) {
         synchronized (lock) {
            return table.remove(pageKey, rowKey, columnKey);
         }
      }

      @Override
      public V put(P pageKey, R rowKey, C columnKey, V value) {
         synchronized (lock) {
            return table.put(pageKey, rowKey, columnKey, value);
         }
      }

      @Override
      public void putAll(Table3D<? extends P, ? extends R, ? extends C, ? extends V> tbl) {
         synchronized (lock) {
            table.putAll(tbl);
         }
      }

      @Override
      public boolean containsCell(Object pageKey, Object rowKey, Object columnKey) {
         synchronized (lock) {
            return table.containsCell(pageKey, rowKey, columnKey);
         }
      }

      @Override
      public boolean containsColumn(Object pageKey, Object columnKey) {
         synchronized (lock) {
            return table.containsColumn(pageKey, columnKey);
         }
      }

      @Override
      public boolean containsRow(Object pageKey, Object rowKey) {
         synchronized (lock) {
            return table.containsRow(pageKey, rowKey);
         }
      }

      @Override
      public boolean containsPage(Object pageKey) {
         synchronized (lock) {
            return table.containsPage(pageKey);
         }
      }

      @Override
      public boolean containsColumnKey(Object columnKey) {
         synchronized (lock) {
            return table.containsColumnKey(columnKey);
         }
      }

      @Override
      public boolean containsRowKey(Object rowKey) {
         synchronized (lock) {
            return table.containsRowKey(rowKey);
         }
      }

      @Override
      public Set<P> pageKeySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(table.pageKeySet(), lock);
         }
      }

      @Override
      public Set<R> rowKeySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(table.rowKeySet(), lock);
         }
      }

      @Override
      public Map<P, Set<R>> rowIndex() {
         synchronized (lock) {
            return new SynchronizedMap<>(
                  new TransformingMap.ValuesOnly<>(table.rowIndex(),
                        (k, s) -> new SynchronizedSet<>(s, lock)),
                  lock);
         }
      }

      @Override
      public Set<C> columnKeySet() {
         synchronized (lock) {
            return new SynchronizedSet<>(table.columnKeySet(), lock);
         }
      }

      @Override
      public Table2D<P, R, Set<C>> columnIndex() {
         synchronized (lock) {
            return new SynchronizedTable2D<>(
                  new TransformingTable2D<>(table.columnIndex(),
                        s -> new SynchronizedSet<>(s, lock)),
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
      public Set<Cell3D<P, R, C, V>> cellSet() {
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
      public int hashCode() {
         synchronized (lock) {
            return table.hashCode();
         }
      }

      @Override
      public boolean equals(Object obj) {
         synchronized (lock) {
            return table.equals(obj);
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
   
   static class UnmodifiableTable3D<P, R, C, V> implements Table3D<P, R, C, V> {
      private final Table3D<P, R, C, V> table;
      
      UnmodifiableTable3D(Table3D<P, R, C, V> table) {
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
      public TableMapView<P, Table2D<R, C, V>> asMap() {
         return new UnmodifiableTableMapView<>(table.asMap(), Table2D::unmodifiableTable);
      }

      @Override
      public Table3D<P, C, R, V> transpose() {
         return new UnmodifiableTable3D<>(table.transpose());
      }

      @Override
      public Table3D<R, C, P, V> rotate() {
         return new UnmodifiableTable3D<>(table.rotate());
      }

      @Override
      public Table2D<R, C, V> getPage(P pageKey) {
         return new UnmodifiableTable2D<>(table.getPage(pageKey));
      }

      @Override
      public Map<C, V> getRow(P pageKey, R rowKey) {
         return Collections.unmodifiableMap(table.getRow(pageKey, rowKey));
      }

      @Override
      public Map<R, V> getColumn(P pageKey, C columnKey) {
         return Collections.unmodifiableMap(table.getColumn(pageKey, columnKey));
      }

      @Override
      public Table2D<P, C, V> getRows(R rowKey) {
         return new UnmodifiableTable2D<>(table.getRows(rowKey));
      }

      @Override
      public Table2D<P, R, V> getColumns(C columnKey) {
         return new UnmodifiableTable2D<>(table.getColumns(columnKey));
      }

      @Override
      public V get(Object pageKey, Object rowKey, Object columnKey) {
         return table.get(pageKey, rowKey, columnKey);
      }

      @Override
      public V remove(Object pageKey, Object rowKey, Object columnKey) {
         throw new UnsupportedOperationException();
      }

      @Override
      public V put(P pageKey, R rowKey, C columnKey, V value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Table3D<? extends P, ? extends R, ? extends C, ? extends V> tbl) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean containsCell(Object pageKey, Object rowKey, Object columnKey) {
         return table.containsCell(pageKey, rowKey, columnKey);
      }

      @Override
      public boolean containsColumn(Object pageKey, Object columnKey) {
         return table.containsColumn(pageKey, columnKey);
      }

      @Override
      public boolean containsRow(Object pageKey, Object rowKey) {
         return table.containsRow(pageKey, rowKey);
      }

      @Override
      public boolean containsPage(Object pageKey) {
         return table.containsPage(pageKey);
      }

      @Override
      public boolean containsColumnKey(Object columnKey) {
         return table.containsColumnKey(columnKey);
      }

      @Override
      public boolean containsRowKey(Object rowKey) {
         return table.containsRowKey(rowKey);
      }

      @Override
      public Set<P> pageKeySet() {
         return Collections.unmodifiableSet(table.pageKeySet());
      }

      @Override
      public Set<R> rowKeySet() {
         return Collections.unmodifiableSet(table.rowKeySet());
      }

      @Override
      public Map<P, Set<R>> rowIndex() {
         return TransformingMap.ReadOnly.transformingValues(table.rowIndex(),
               s -> Collections.unmodifiableSet(s));
      }

      @Override
      public Set<C> columnKeySet() {
         return Collections.unmodifiableSet(table.columnKeySet());
      }

      @Override
      public Table2D<P, R, Set<C>> columnIndex() {
         return new UnmodifiableTable2D<>(table.columnIndex());
      }

      @Override
      public Collection<V> values() {
         return Collections.unmodifiableCollection(table.values());
      }

      @Override
      public Set<Cell3D<P, R, C, V>> cellSet() {
         return Collections.unmodifiableSet(table.cellSet());
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean equals(Object o) {
         return table.equals(o);
      }
      
      @Override
      public int hashCode() {
         return table.hashCode();
      }
      
      @Override
      public String toString() {
         return table.toString();
      }
   }
}
