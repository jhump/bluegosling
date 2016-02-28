package com.bluegosling.collections;

import static java.util.Objects.requireNonNull;

import com.bluegosling.collections.Tables2D.SynchronizedTable2D;
import com.bluegosling.collections.Tables2D.Table2DImpl;
import com.bluegosling.collections.Tables2D.TransformingTable2D;
import com.bluegosling.collections.Tables2D.UnmodifiableTable2D;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

// TODO: javadoc
public interface Table2D<R, C, V> {
   interface Cell2D<R, C, V> {
      R getRowKey();
      C getColumnKey();
      V getValue();
      V setValue(V newValue);
   }
   int size();
   boolean isEmpty();
   TableMapView<R, Map<C, V>> asMap();
   Table2D<C, R, V> transpose();
   default Table2D<C, R, V> rotate() {
      // rotation with only 2 dimensions is same as transpose
      return transpose();
   }
   Map<C, V> getRow(R rowKey);
   Map<R, V> getColumn(C columnKey);
   V get(Object rowKey, Object columnKey);
   V remove(Object rowKey, Object columnKey);
   V put(R rowKey, C columnKey, V value);
   void putAll(Table2D<? extends R, ? extends C, ? extends V> table);
   boolean containsCell(Object rowKey, Object columnKey);
   boolean containsColumn(Object columnKey);
   boolean containsRow(Object rowKey);
   Set<R> rowKeySet();
   Set<C> columnKeySet();
   Map<R, Set<C>> columnIndex();
   Collection<V> values();
   Set<Cell2D<R, C, V>> cellSet();
   void clear();
   
   static <R, C, V> Table2D<R, C, V> newHashTable2D() {
      return newCustomTable2D(() -> new HashMap<>());
   }

   static <R, C, V> Table2D<R, C, V> newLinkedHashTable2D() {
      return newCustomTable2D(() -> new LinkedHashMap<>());
   }

   static <R, C, V> Table2D<R, C, V> newTreeTable2D() {
      return newCustomTable2D(() -> new TreeMap<>());
   }

   static <R, C, V> Table2D<R, C, V> newCustomTable2D(Supplier<Map<Object, Object>> mapMaker) {
      requireNonNull(mapMaker);
      return newCustomTable2D(h -> mapMaker.get());
   }

   static <R, C, V> Table2D<R, C, V> newCustomTable2D(IntFunction<Map<Object, Object>> mapMaker) {
      return new Table2DImpl<>(requireNonNull(mapMaker));
   }
   
   @SuppressWarnings("unchecked")
   static <R, C, V> Table2D<R, C, V> empty() {
      return (Table2D<R, C, V>) Tables2D.EMPTY;
   }
   
   static <R, C, V> Table2D<R, C, V> synchronizedTable(Table2D<R, C, V> table) {
      return new SynchronizedTable2D<>(requireNonNull(table));
   }

   static <R, C, V> Table2D<R, C, V> unmodifiableTable(Table2D<R, C, V> table) {
      return new UnmodifiableTable2D<>(requireNonNull(table));
   }
   
   static <R, C, VI, VO> Table2D<R, C, VO> transform(Table2D<R, C, VI> table,
         Function<? super VI, ? extends VO> fn) {
      return new TransformingTable2D<>(table, fn);
   }
}
