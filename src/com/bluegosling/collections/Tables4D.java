package com.bluegosling.collections;

import com.bluegosling.collections.Tables5D.Table5DImpl;

import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

//TODO: doc
//TODO: tests
final class Tables4D {
   private Tables4D() {
   }

   /**
    * A 4-dimensional table implementation that is backed by a nested map. The actual nested map may
    * have greater than four dimensions (in which case one or more levels is filtered).
    *
    * @param <S> the type of section keys in the table
    * @param <P> the type of page keys in the table
    * @param <R> the type of row keys in the table
    * @param <C> the type of column keys in the table
    * @param <V> the type of values in the table
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Table4DImpl<S, P, R, C, V> extends NestedMap<Object, V>
         implements Table4D<S, P, R, C, V> {
      
      Table4DImpl(IntFunction<Map<Object, Object>> mapMaker) {
         super(mapMaker, 3);
      }
      
      Table4DImpl(Table5DImpl<?, ?, ?, ?, ?, V> other, Object filterKey, int filterHeight) {
         super(other, filterKey, filterHeight);
         assert this.height() == 3;
      }

      @Override
      public TableMapView<S, Table3D<P, R, C, V>> asMap() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<S, P, C, R, V> transpose() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table4D<P, R, C, S, V> rotate() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<P, R, C, V> getSection(S sectionKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table2D<R, C, V> getPage(S sectionKey, P pageKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Map<C, V> getRow(S sectionKey, P pageKey, R rowKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Map<C, V> getColumn(S sectionKey, P pageKey, C columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<S, R, C, V> getPages(P pageKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<S, P, C, V> getRows(R rowKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<S, P, R, V> getColumns(C columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public V get(Object sectionKey, Object pageKey, Object rowKey, Object columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public V remove(Object sectionKey, Object pageKey, Object rowKey, Object columnKey) {
         // TODO: implement me
         return null;
      }

      @Override
      public V put(S sectionKey, P pageKey, R rowKey, C columnKey, V value) {
         // TODO: implement me
         return null;
      }

      @Override
      public void putAll(
            Table4D<? extends S, ? extends P, ? extends R, ? extends C, ? extends V> table) {
         // TODO: implement me
         
      }

      @Override
      public boolean
            containsCell(Object sectionKey, Object pageKey, Object rowKey, Object columnKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsColumn(Object sectionKey, Object pageKey, Object columnKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsRow(Object sectionKey, Object pageKey, Object rowKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsPage(Object sectionKey, Object pageKey) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsSection(Object sectionKey) {
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
      public Set<S> sectionKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<P> pageKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Map<S, Set<P>> pageIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<R> rowKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table2D<S, P, Set<R>> rowIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<C> columnKeySet() {
         // TODO: implement me
         return null;
      }

      @Override
      public Table3D<S, P, R, Set<C>> columnIndex() {
         // TODO: implement me
         return null;
      }

      @Override
      public Set<Cell4D<S, P, R, C, V>> cellSet() {
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
