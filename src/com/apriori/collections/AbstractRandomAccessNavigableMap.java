package com.apriori.collections;

import java.util.AbstractList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

// TODO: javadoc
// TODO: tests
public abstract class AbstractRandomAccessNavigableMap<K, V> extends AbstractNavigableMap<K, V>
      implements RandomAccessNavigableMap<K, V> {

   /**
    * Checks that the specified index is greater than or equal to zero and less than this map's
    * {@link #size}.
    *
    * @param index an index to check
    */
   protected void rangeCheck(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index >= size()) {
         throw new IndexOutOfBoundsException(index + " >= " + size());
      }
   }

   /**
    * Checks that the specified index is greater than or equal to zero and less than or equal to
    * this map's {@link #size}. This is for certain operations where an index equal to the size
    * (<em>after</em> the last valid index in the map) is allowed.
    *
    * @param index an index to check
    */
   protected void rangeCheckWide(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index > size()) {
         throw new IndexOutOfBoundsException(index + " > " + size());
      }
   }
   
   @Override
   public int indexOfKey(Object key) {
      return CollectionUtils.findObject(key, keySet().listIterator());
   }

   @Override
   public ListIterator<Entry<K, V>> listIterator() {
      return new EntryListIteratorImpl(0);
   }

   @Override
   public ListIterator<Entry<K, V>> listIterator(int index) {
      rangeCheckWide(index);
      return new EntryListIteratorImpl(index);
   }
   
   @Override
   public Entry<K, V> firstEntry() {
      return getEntry(0);
   }

   @Override
   public Entry<K, V> lastEntry() {
      return getEntry(size() - 1);
   }

   @Override
   public RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex) {
      rangeCheckWide(startIndex);
      rangeCheckWide(endIndex);
      if (startIndex > endIndex) {
         throw new IndexOutOfBoundsException();
      }
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessSet<K> keySet() {
      return navigableKeySet();
   }

   @Override public List<V> values() {
      return new ValueList();
   }

   @Override public RandomAccessSet<Entry<K, V>> entrySet() {
      return new RandomAccessEntrySet();
   }
   
   @Override public RandomAccessNavigableMap<K, V> descendingMap() {
      return new DescendingRandomAccessMap<K, V>(this);
   }

   @Override public RandomAccessNavigableSet<K> navigableKeySet() {
      return new RandomAccessKeySet();
   }

   @Override public RandomAccessNavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }

   @Override public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
         boolean toInclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   @Override public RandomAccessNavigableMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   @Override public RandomAccessNavigableMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
   }
   
   protected abstract class BaseListIteratorImpl<T> extends BaseIteratorImpl<T>
         implements ListIterator<T> {

      boolean lastFetchedViaNext;
      int nextIndex;
      
      BaseListIteratorImpl(int nextIndex) {
         this.nextIndex = nextIndex;
      }
      
      protected Entry<K, V> retreat(K previousKey) {
         return lowerEntry(previousKey);
      }
      
      @Override
      public boolean hasPrevious() {
         checkModCount();
         return nextIndex > 0;
      }
      
      @Override
      public T next() {
         T ret = super.next();
         lastFetchedViaNext = true;
         nextIndex++;
         return ret;
      }

      @Override
      public T previous() {
         checkModCount();
         if (nextIndex <= 0) {
            throw new NoSuchElementException();
         }
         next = retreat(next.getKey());
         lastFetched = next;
         lastFetchedViaNext = false;
         nextIndex--;
         return compute(next);
      }

      @Override
      public int nextIndex() {
         checkModCount();
         return nextIndex;
      }

      @Override
      public int previousIndex() {
         checkModCount();
         return nextIndex - 1;
      }

      @Override
      public void remove() {
         super.remove();
         if (lastFetchedViaNext) {
            nextIndex--;
         }
      }
      
      @Override
      public void set(T e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(T e) {
         throw new UnsupportedOperationException();
      }
   }
   
   protected class EntryListIteratorImpl extends BaseListIteratorImpl<Entry<K, V>> {
      EntryListIteratorImpl(int nextIndex) {
         super(nextIndex);
      }

      @Override
      protected Entry<K, V> compute(Entry<K, V> entry) {
         return entry;
      }
   }
   
   protected class KeyListIteratorImpl extends BaseListIteratorImpl<K> {
      KeyListIteratorImpl(int nextIndex) {
         super(nextIndex);
      }

      @Override
      protected K compute(Entry<K, V> entry) {
         return entry.getKey();
      }
   }

   protected class ValueListIteratorImpl extends BaseListIteratorImpl<V> {
      ValueListIteratorImpl(int nextIndex) {
         super(nextIndex);
      }

      @Override
      protected V compute(Entry<K, V> entry) {
         return entry.getValue();
      }
   }
   
   protected class RandomAccessEntrySet extends EntrySet implements RandomAccessSet<Entry<K, V>>{

      @Override
      public Entry<K, V> get(int index) {
         return AbstractRandomAccessNavigableMap.this.removeEntry(index);
      }

      @Override
      public int indexOf(Object o) {
         return CollectionUtils.findObject(o, listIterator());
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator() {
         return new EntryListIteratorImpl(0);
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator(int index) {
         rangeCheckWide(index);
         return new EntryListIteratorImpl(index);
      }

      @Override
      public Entry<K, V> remove(int index) {
         return AbstractRandomAccessNavigableMap.this.removeEntry(index);
      }

      @Override
      public RandomAccessSet<Entry<K, V>> subSetByIndices(int fromIndex, int toIndex) {
         return AbstractRandomAccessNavigableMap.this.subMapByIndices(fromIndex,  toIndex)
               .entrySet();
      }

      @Override
      public List<Entry<K, V>> asList() {
         return new RandomAccessSetList<Entry<K,V>>(this);
      }
   }

   protected class RandomAccessKeySet extends KeySet implements RandomAccessNavigableSet<K> {
      @Override
      public K get(int index) {
         Entry<K, V> entry = AbstractRandomAccessNavigableMap.this.getEntry(index);
         return entry == null ? null : entry.getKey();
      }

      @Override
      public int indexOf(Object o) {
         return AbstractRandomAccessNavigableMap.this.indexOfKey(o);
      }

      @Override
      public ListIterator<K> listIterator() {
         return new KeyListIteratorImpl(0);
      }

      @Override
      public ListIterator<K> listIterator(int index) {
         rangeCheckWide(index);
         return new KeyListIteratorImpl(index);
      }

      @Override
      public K remove(int index) {
         return AbstractRandomAccessNavigableMap.this.removeEntry(index).getKey();
      }

      @Override
      public List<K> asList() {
         return new RandomAccessSetList<K>(this);
      }

      @Override
      public RandomAccessNavigableSet<K> descendingSet() {
         return new DescendingRandomAccessSet<K>(this);
      }

      @Override
      public RandomAccessNavigableSet<K> subSetByIndices(int fromIndex, int toIndex) {
         return AbstractRandomAccessNavigableMap.this.subMapByIndices(fromIndex, toIndex)
               .navigableKeySet();
      }
      
      @Override
      public RandomAccessNavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement,
            boolean toInclusive) {
         return AbstractRandomAccessNavigableMap.this.subMap(fromElement, fromInclusive,
               toElement, toInclusive).navigableKeySet();
      }

      @Override
      public RandomAccessNavigableSet<K> headSet(K toElement, boolean inclusive) {
         return AbstractRandomAccessNavigableMap.this.headMap(toElement, inclusive)
               .navigableKeySet();
      }

      @Override
      public RandomAccessNavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return AbstractRandomAccessNavigableMap.this.tailMap(fromElement, inclusive)
               .navigableKeySet();
      }

      @Override
      public RandomAccessNavigableSet<K> subSet(K fromElement, K toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      @Override
      public RandomAccessNavigableSet<K> headSet(K toElement) {
         return headSet(toElement, false);
      }

      @Override
      public RandomAccessNavigableSet<K> tailSet(K fromElement) {
         return tailSet(fromElement, true);
      }
   }
   
   protected class ValueList extends AbstractList<V> {

      @Override
      public V get(int index) {
         return AbstractRandomAccessNavigableMap.this.getEntry(index).getValue();
      }

      @Override
      public int size() {
         return AbstractRandomAccessNavigableMap.this.size();
      }
      
      @Override
      public V remove(int index) {
         return AbstractRandomAccessNavigableMap.this.removeEntry(index).getValue();
      }
   }
}
