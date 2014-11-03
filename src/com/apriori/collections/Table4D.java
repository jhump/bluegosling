package com.apriori.collections;

import com.apriori.collections.Tables4D.Table4DImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.Supplier;

//TODO: javadoc
public interface Table4D<S, P, R, C, V> {
   interface Cell4D<S, P, R, C, V> {
      S getSectionKey();
      P getPageKey();
      R getRowKey();
      C getColumnKey();
      V getValue();
      V setValue(V newValue);
   }
   int size();
   boolean isEmpty();
   TableMapView<S, Table3D<P, R, C, V>> asMap();
   Table4D<S, P, C, R, V> transpose();
   Table4D<P, R, C, S, V> rotate();
   Table3D<P, R, C, V> getSection(S sectionKey);
   Table2D<R, C, V> getPage(S sectionKey, P pageKey);
   Map<C, V> getRow(S sectionKey, P pageKey, R rowKey);
   Map<C, V> getColumn(S sectionKey, P pageKey, C columnKey);
   Table3D<S, R, C, V> getPages(P pageKey);
   Table3D<S, P, C, V> getRows(R rowKey);
   Table3D<S, P, R, V> getColumns(C columnKey);
   V get(Object sectionKey, Object pageKey, Object rowKey, Object columnKey);
   V remove(Object sectionKey, Object pageKey, Object rowKey, Object columnKey);
   V put(S sectionKey, P pageKey, R rowKey, C columnKey, V value);
   void putAll(Table4D<? extends S, ? extends P, ? extends R, ? extends C, ? extends V> table);
   boolean containsCell(Object sectionKey, Object pageKey, Object rowKey, Object columnKey);
   boolean containsColumn(Object sectionKey, Object pageKey, Object columnKey);
   boolean containsRow(Object sectionKey, Object pageKey, Object rowKey);
   boolean containsPage(Object sectionKey, Object pageKey);
   boolean containsSection(Object sectionKey);
   boolean containsColumnKey(Object columnKey);
   boolean containsRowKey(Object rowKey);
   boolean containsPageKey(Object pageKey);
   Set<S> sectionKeySet();
   Set<P> pageKeySet();
   Map<S, Set<P>> pageIndex();
   Set<R> rowKeySet();
   Table2D<S, P, Set<R>> rowIndex();
   Set<C> columnKeySet();
   Table3D<S, P, R, Set<C>> columnIndex();
   Collection<V> values();
   Set<Cell4D<S, P, R, C, V>> cellSet();
   void clear();

   static <S, P, R, C, V> Table4D<S, P, R, C, V> newHashTable4D() {
      return newCustomTable4D((Supplier<Map<Object, Object>>) HashMap::new);
   }

   static <S, P, R, C, V> Table4D<S, P, R, C, V> newLinkedHashTable4D() {
      return newCustomTable4D((Supplier<Map<Object, Object>>) LinkedHashMap::new);
   }

   static <S, P, R, C, V> Table4D<S, P, R, C, V> newTreeTable4D() {
      return newCustomTable4D(TreeMap::new);
   }

   static <S, P, R, C, V> Table4D<S, P, R, C, V> newCustomTable4D(
         Supplier<Map<Object, Object>> mapMaker) {
      return newCustomTable4D(h -> mapMaker.get());
   }

   static <S, P, R, C, V> Table4D<S, P, R, C, V> newCustomTable4D(
         IntFunction<Map<Object, Object>> mapMaker) {
      return new Table4DImpl<>(mapMaker);
   }
}