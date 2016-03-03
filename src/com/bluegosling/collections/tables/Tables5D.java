package com.bluegosling.collections.tables;

import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

//TODO: doc
//TODO: tests
final class Tables5D {
   private Tables5D() {
   }

   /**
    * A 5-dimensional table implementation that is backed by a nested map.
    *
    * @param <L> the type of volume keys in the table
    * @param <S> the type of section keys in the table
    * @param <P> the type of page keys in the table
    * @param <R> the type of row keys in the table
    * @param <C> the type of column keys in the table
    * @param <V> the type of values in the table
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table5DImpl<L, S, P, R, C, V> extends NestedMap<Object, V>
         implements Table5D<L, S, P, R, C, V> {
      
      Table5DImpl(IntFunction<Map<Object, Object>> mapMaker) {
         super(mapMaker, 4);
      }

      @Override
      public TableMapView<L, Table4D<S, P, R, C, V>> asMap() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table5D<L, S, P, C, R, V> transpose() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table5D<S, P, R, C, L, V> rotate() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<S, P, R, C, V> getVolume(L volumeKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<P, R, C, V> getSection(L volumeKey, S sectionKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table2D<R, C, V> getPage(L volumeKey, S sectionKey, P pageKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Map<C, V> getRow(L volumeKey, S sectionKey, P pageKey, R rowKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Map<C, V> getColumn(L volumeKey, S sectionKey, P pageKey, C columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<L, P, R, C, V> getSections(S sectionKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<L, S, R, C, V> getPages(P pageKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<L, S, P, C, V> getRows(R rowKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<L, S, P, R, V> getColumns(C columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public V get(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey,
            Object columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public V remove(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey,
            Object columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public V put(L volumeKey, S sectionKey, P pageKey, R rowKey, C columnKey, V value) {
         // TODO: implement me
         return null;
      }

      @Override
      public void putAll(
            Table5D<? extends L, ? extends S, ? extends P, ? extends R, ? extends C, ? extends V> table) {
         // TODO: implement me
         
      }

      @Override
      public boolean containsCell(Object volumeKey, Object sectionKey, Object pageKey,
            Object rowKey, Object columnKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsColumn(Object volumeKey, Object sectionKey, Object pageKey,
            Object columnKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean
            containsRow(Object volumeKey, Object sectionKey, Object pageKey, Object rowKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsPage(Object volumeKey, Object sectionKey, Object pageKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsSection(Object volumeKey, Object sectionKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsVolume(Object volumeKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsColumnKey(Object columnKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsRowKey(Object rowKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsPageKey(Object pageKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsSectionKey(Object sectionKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public Set<L> volumeKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<S> sectionKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Map<L, Set<S>> sectionIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<P> pageKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table2D<L, S, Set<P>> pageIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<R> rowKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<L, S, P, Set<R>> rowIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<C> columnKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<L, S, P, R, Set<C>> columnIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<Cell5D<L, S, P, R, C, V>> cellSet() {
         // TODO: implement me
         return null;
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
