package com.bluegosling.collections;

import com.bluegosling.collections.Tables5D.Table5DImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.Supplier;

//TODO: javadoc
public interface Table5D<L, S, P, R, C, V> {
   interface Cell5D<L, S, P, R, C, V> {
      L getVolumeKey();
      S getSectionKey();
      P getPageKey();
      R getRowKey();
      C getColumnKey();
      V getValue();
      V setValue(V newValue);
   }
   int size();
   boolean isEmpty();
   TableMapView<L, Table4D<S, P, R, C, V>> asMap();
   Table5D<L, S, P, C, R, V> transpose();
   Table5D<S, P, R, C, L, V> rotate();
   Table4D<S, P, R, C, V> getVolume(L volumeKey);
   Table3D<P, R, C, V> getSection(L volumeKey, S sectionKey);
   Table2D<R, C, V> getPage(L volumeKey, S sectionKey, P pageKey);
   Map<C, V> getRow(L volumeKey, S sectionKey, P pageKey, R rowKey);
   Map<C, V> getColumn(L volumeKey, S sectionKey, P pageKey, C columnKey);
   Table4D<L, P, R, C, V> getSections(S sectionKey);
   Table4D<L, S, R, C, V> getPages(P pageKey);
   Table4D<L, S, P, C, V> getRows(R rowKey);
   Table4D<L, S, P, R, V> getColumns(C columnKey);
   V get(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey, Object columnKey);
   V remove(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey, Object columnKey);
   V put(L volumeKey, S sectionKey, P pageKey, R rowKey, C columnKey, V value);
   void putAll(
         Table5D<? extends L, ? extends S, ? extends P, ? extends R, ? extends C, ? extends V> table);
   boolean containsCell(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey,
         Object columnKey);
   boolean containsColumn(Object volumeKey, Object sectionKey, Object pageKey, Object columnKey);
   boolean containsRow(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey);
   boolean containsPage(Object volumeKey, Object sectionKey, Object pageKey);
   boolean containsSection(Object volumeKey, Object sectionKey);
   boolean containsVolume(Object volumeKey);
   boolean containsColumnKey(Object columnKey);
   boolean containsRowKey(Object rowKey);
   boolean containsPageKey(Object pageKey);
   boolean containsSectionKey(Object sectionKey);
   Set<L> volumeKeySet();
   Set<S> sectionKeySet();
   Map<L, Set<S>> sectionIndex();
   Set<P> pageKeySet();
   Table2D<L, S, Set<P>> pageIndex();
   Set<R> rowKeySet();
   Table3D<L, S, P, Set<R>> rowIndex();
   Set<C> columnKeySet();
   Table4D<L, S, P, R, Set<C>> columnIndex();
   Collection<V> values();
   Set<Cell5D<L, S, P, R, C, V>> cellSet();
   void clear();
   
   static <L, S, P, R, C, V> Table5D<L, S, P, R, C, V> newHashTable5D() {
      return newCustomTable5D(() -> new HashMap<>());
   }

   static <L, S, P, R, C, V> Table5D<L, S, P, R, C, V> newLinkedHashTable5D() {
      return newCustomTable5D(() -> new LinkedHashMap<>());
   }

   static <L, S, P, R, C, V> Table5D<L, S, P, R, C, V> newTreeTable5D() {
      return newCustomTable5D(() -> new TreeMap<>());
   }

   static <L, S, P, R, C, V> Table5D<L, S, P, R, C, V> newCustomTable5D(
         Supplier<Map<Object, Object>> mapMaker) {
      return newCustomTable5D(h -> mapMaker.get());
   }

   static <L, S, P, R, C, V> Table5D<L, S, P, R, C, V> newCustomTable5D(
         IntFunction<Map<Object, Object>> mapMaker) {
      return new Table5DImpl<>(mapMaker);
   }
}
