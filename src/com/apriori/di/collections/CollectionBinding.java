package com.apriori.di.collections;

import com.apriori.di.AbstractBinding.ScopeBuilder;
import com.apriori.di.Binding.BindAdapter;
import com.apriori.di.Key;
import com.apriori.di.SelectionProvider;
import com.apriori.reflect.TypeRef;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.inject.Provider;

public class CollectionBinding {

   public interface ElementBindBuilder<T> {
      ScopeBuilder<T> to(Key<? extends T> key);
      ScopeBuilder<T> to(Class<? extends T> clazz);
      ScopeBuilder<T> to(TypeRef<? extends T> typeRef);
      ScopeBuilder<T> toProvider(Key<? extends Provider<? extends T>> key);
      ScopeBuilder<T> toProvider(Class<? extends Provider<? extends T>> clazz);
      ScopeBuilder<T> toProvider(TypeRef<? extends Provider<? extends T>> typeRef);
      ScopeBuilder<T> toSelectionProvider(Key<? extends SelectionProvider<? extends T>> key);
      ScopeBuilder<T> toSelectionProvider(Class<? extends SelectionProvider<? extends T>> clazz);
      ScopeBuilder<T> toSelectionProvider(TypeRef<? extends SelectionProvider<? extends T>> typeRef);
   }

   public interface MapKeyBindBuilder<K, V> {
      ElementBindBuilder<V> from(K key);
   }

   public static <T> BindAdapter<Collection<T>, ElementBindBuilder<T>> forCollection() {
      // TODO
      return null;
   }

   public static <T> BindAdapter<List<T>, ElementBindBuilder<T>> forList() {
      // TODO
      return null;
   }

   public static <T> BindAdapter<Set<T>, ElementBindBuilder<T>> forSet() {
      // TODO
      return null;
   }
   
   public static <T> BindAdapter<SortedSet<T>, ElementBindBuilder<T>> forSortedSet() {
      // TODO
      return null;
   }

   public static <T> BindAdapter<NavigableSet<T>, ElementBindBuilder<T>> forNavigableSet() {
      // TODO
      return null;
   }

   public static <K, V> BindAdapter<Map<K, V>, MapKeyBindBuilder<K, V>> forMap() {
      // TODO
      return null;
   }

   public static <K, V> BindAdapter<SortedMap<K, V>, MapKeyBindBuilder<K, V>> forSortedMap() {
      // TODO
      return null;
   }

   public static <K, V> BindAdapter<NavigableMap<K, V>, MapKeyBindBuilder<K, V>> forNavigableMap() {
      // TODO
      return null;
   }
}
