package com.bluegosling.collections.tries;

import static com.bluegosling.collections.tries.SimpleBitwiseTrie.COMPONENT_COMPARATOR;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.tries.SimpleBitwiseTrie.WrappedComparator;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

//TODO: javadoc
//TODO: implement me (don't forget serialization and cloning)
public class ArrayMappedBitwiseTrie<K, V> implements NavigableCompositeTrie<K, Boolean, V> {

   private final BitConverter<? super K> bitConverter;
   private final Comparator<? super K> comparator;
   
   public ArrayMappedBitwiseTrie(BitConverter<? super K> bitConverter) {
      this(bitConverter, null);
   }

   public ArrayMappedBitwiseTrie(BitConverter<? super K> bitConverter,
         Comparator<? super K> comparator) {
      if (bitConverter == null) {
         throw new NullPointerException();
      }
      this.bitConverter = bitConverter;
      if (comparator == null) {
         this.comparator = CollectionUtils.naturalOrder();
      } else {
         this.comparator = comparator;
      }
   }

   @Override public BitConverter<? super K> componentizer() {
      return bitConverter;
   }
   
   @Override public Comparator<Boolean> componentComparator() {
      return COMPONENT_COMPARATOR;
   }

   @Override public Comparator<? super K> comparator() {
      return new WrappedComparator<K>(bitConverter,
            comparator == Comparator.naturalOrder() ? null : comparator);
   }

   @Override
   public int size() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public boolean isEmpty() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean containsKey(Object key) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean containsValue(Object value) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public V get(Object key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public V put(K key, V value) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public V remove(Object key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void clear() {
      // TODO Auto-generated method stub
      
   }

   @Override
   public Set<K> keySet() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<V> values() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> lowerEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K lowerKey(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K floorKey(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K ceilingKey(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K higherKey(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> firstEntry() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> lastEntry() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> pollFirstEntry() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Entry<K, V> pollLastEntry() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableSet<K> navigableKeySet() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableSet<K> descendingKeySet() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K firstKey() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K lastKey() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMapByKey(K prefix) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMapByKey(K prefix, int numComponents) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(Boolean prefix) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(Iterable<Boolean> prefix) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(Iterable<Boolean> prefix, int numComponents) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> descendingMap() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> subMap(K fromKey, boolean fromInclusive, K toKey,
         boolean toInclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> headMap(K toKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> tailMap(K fromKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> subMap(K fromKey, K toKey) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> headMap(K toKey) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> tailMap(K fromKey) {
      // TODO Auto-generated method stub
      return null;
   }
}
