package com.apriori.collections;

import static java.util.Objects.requireNonNull;

import com.apriori.collections.Tables3D.SynchronizedTable3D;
import com.apriori.collections.Tables3D.Table3DImpl;
import com.apriori.collections.Tables3D.UnmodifiableTable3D;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.Supplier;

//TODO: javadoc
public interface Table3D<P, R, C, V> {
   interface Cell3D<P, R, C, V> {
      P getPageKey();
      R getRowKey();
      C getColumnKey();
      V getValue();
      V setValue(V newValue);
   }
   int size();
   boolean isEmpty();
   TableMapView<P, Table2D<R, C, V>> asMap();
   Table3D<P, C, R, V> transpose();
   Table3D<R, C, P, V> rotate();
   Table2D<R, C, V> getPage(P pageKey);
   Map<C, V> getRow(P pageKey, R rowKey);
   Map<R, V> getColumn(P pageKey, C columnKey);
   Table2D<P, C, V> getRows(R rowKey);
   Table2D<P, R, V> getColumns(C columnKey);
   V get(Object pageKey, Object rowKey, Object columnKey);
   V remove(Object pageKey, Object rowKey, Object columnKey);
   V put(P pageKey, R rowKey, C columnKey, V value);
   void putAll(Table3D<? extends P, ? extends R, ? extends C, ? extends V> table);
   boolean containsCell(Object pageKey, Object rowKey, Object columnKey);
   boolean containsColumn(Object pageKey, Object columnKey);
   boolean containsRow(Object pageKey, Object rowKey);
   boolean containsPage(Object pageKey);
   boolean containsColumnKey(Object columnKey);
   boolean containsRowKey(Object rowKey);
   Set<P> pageKeySet();
   Set<R> rowKeySet();
   Map<P, Set<R>> rowIndex();
   Set<C> columnKeySet();
   Table2D<P, R, Set<C>> columnIndex();
   Collection<V> values();
   Set<Cell3D<P, R, C, V>> cellSet();
   void clear();

   static <P, R, C, V> Table3D<P, R, C, V> newHashTable3D() {
      return newCustomTable3D((Supplier<Map<Object, Object>>) HashMap::new);
   }

   static <P, R, C, V> Table3D<P, R, C, V> newLinkedHashTable3D() {
      return newCustomTable3D((Supplier<Map<Object, Object>>) LinkedHashMap::new);
   }

   static <P, R, C, V> Table3D<P, R, C, V> newTreeTable3D() {
      return newCustomTable3D(TreeMap::new);
   }

   static <P, R, C, V> Table3D<P, R, C, V> newCustomTable3D(
         Supplier<Map<Object, Object>> mapMaker) {
      requireNonNull(mapMaker);
      return newCustomTable3D(h -> mapMaker.get());
   }

   static <P, R, C, V> Table3D<P, R, C, V> newCustomTable3D(
         IntFunction<Map<Object, Object>> mapMaker) {
      return new Table3DImpl<>(requireNonNull(mapMaker));
   }

   @SuppressWarnings("unchecked")
   static <P, R, C, V> Table3D<P, R, C, V> empty() {
      return (Table3D<P, R, C, V>) Tables3D.EMPTY;
   }
   
   static <P, R, C, V> Table3D<P, R, C, V> synchronizedTable(Table3D<P, R, C, V> table) {
      return new SynchronizedTable3D<>(requireNonNull(table));
   }

   static <P, R, C, V> Table3D<P, R, C, V> unmodifiableTable(Table3D<P, R, C, V> table) {
      return new UnmodifiableTable3D<>(requireNonNull(table));
   }
}