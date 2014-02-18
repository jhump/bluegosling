package com.apriori.collections;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An abstract base class for {@link RandomAccessNavigableMap} implementations. Sub-classes must
 * implement most of the methods needed to implement a normal {@link AbstractNavigableMap}. The
 * main differences follow:
 * <ul>
 *    <li>Methods {@code firstEntry()} and {@code lastEntry()} do not need to be implemented. This
 *    abstract map implements them in terms of random access, e.g. {@code getEntry(0)}</li>
 *    <li>New random access methods, {@link #getEntry(int)} and {@link #removeEntry(int)}, must be
 *    implemented.</li>
 * </ul>
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public abstract class AbstractRandomAccessNavigableMap<K, V> extends AbstractNavigableMap<K, V>
      implements RandomAccessNavigableMap<K, V> {

   /**
    * Constructs a new, empty map that orders keys according to their {@linkplain Comparable natural
    * ordering}.
    */
   protected AbstractRandomAccessNavigableMap() {
      this(null);
   }
   
   /**
    * Constructs a new, empty map that orders keys using the specified comparator.
    * 
    * @param comparator determines ordering of keys in the map
    */
   protected AbstractRandomAccessNavigableMap(Comparator<? super K> comparator) {
      super(comparator);
   }

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
      return new SubMapByIndices(startIndex, endIndex);
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
         if (nextIndex > 0) {
            next = getEntry(nextIndex);
         }
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
   
   protected class RandomAccessEntrySet extends EntrySet implements RandomAccessSet<Entry<K, V>> {

      @Override
      public Entry<K, V> get(int index) {
         return AbstractRandomAccessNavigableMap.this.getEntry(index);
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
   
   /**
    * A sub-map view, defined by a range of indices.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class SubMapByIndices extends AbstractRandomAccessNavigableMap<K, V> {
      private final AbstractRandomAccessNavigableMap<K, V>.SubMapByIndices parent;
      private final int startIndex;
      private int endIndex;
      private Entry<K, V> startNode;
      private Entry<K, V> endNode;
      
      SubMapByIndices(int startIndex, int endIndex) {
         this(startIndex, endIndex, null);
      }
      
      SubMapByIndices(int startIndex, int endIndex,
            AbstractRandomAccessNavigableMap<K, V>.SubMapByIndices parent) {
         super(AbstractRandomAccessNavigableMap.this.comparator);
         this.startIndex = startIndex;
         this.endIndex = endIndex;
         this.parent = parent;
         resetModCount();
      }
      
      private AbstractRandomAccessNavigableMap<K, V> outer() {
         return AbstractRandomAccessNavigableMap.this;
      }
      
      void checkModCount() {
         if (this.modCount != AbstractRandomAccessNavigableMap.this.modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      void resetModCount() {
         this.modCount = outer().modCount;
      }
      
      void contractAfterRemove() {
         endIndex--;
         // TODO: could be a little more efficient by only clearing this when really required,
         // based on what key was removed (e.g. only clear endNode when last item in submap is
         // removed and only clear startNode when first item is removed)
         endNode = startNode = null;
         resetModCount();
         if (parent != null) {
            parent.contractAfterRemove();
         }
      }
      
      private void expandAfterAdd(int expandBy) {
         if (expandBy == 0) {
            return;
         }
         endIndex += expandBy;
         // TODO: could be a little more efficient by only clearing this when really required,
         // based on where key was added (e.g. only clear endNode when we store an item after the
         // end of the submap)
         endNode = null;
         resetModCount();
         if (parent != null) {
            parent.expandAfterAdd(expandBy);
         }
      }
      
      private Entry<K, V> startNode() {
         if (startNode == null && startIndex != 0) {
            startNode = outer().getEntry(startIndex - 1);
         }
         return startNode;
      }
      
      private Entry<K, V> endNode() {
         if (endNode == null && endIndex != outer().size()) {
            endNode = outer().getEntry(endIndex);
         }
         return endNode;
      }
      
      boolean isKeyInRange(K key) {
         if (endIndex == startIndex) {
            return false;
         }
         Entry<K, V> start = startNode();
         Entry<K, V> end = endNode();
         return (start == null || comparator.compare(key, start.getKey()) > 0)
               && (end == null || comparator.compare(key, end.getKey()) < 0);
      }
      
      boolean isInSubRange(int index) {
         return index >= startIndex && index < endIndex;
      }
      
      int adjustIndex(int index) {
         return startIndex + index;
      }
      
      @Override
      public int size() {
         checkModCount();
         return endIndex - startIndex;
      }

      @Override
      public boolean isEmpty() {
         checkModCount();
         return startIndex == endIndex;
      }

      @Override
      public Entry<K, V> lowerEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Entry<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.getKey()) >= 0) {
            return lastEntry();
         }
         Entry<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.getKey()) <= 0) {
            return null;
         }
         Entry<K, V> candidate = outer().lowerEntry(key);
         return start == null || comparator.compare(candidate.getKey(), start.getKey()) > 0
               ? candidate : null;
      }

      @Override
      public Entry<K, V> floorEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Entry<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.getKey()) >= 0) {
            return lastEntry();
         }
         Entry<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.getKey()) <= 0) {
            return null;
         }
         Entry<K, V> candidate = outer().floorEntry(key);
         return start == null || comparator.compare(candidate.getKey(), start.getKey()) > 0
               ? candidate : null;
      }

      @Override
      public Entry<K, V> ceilingEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Entry<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.getKey()) <= 0) {
            return firstEntry();
         }
         Entry<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.getKey()) >= 0) {
            return null;
         }
         Entry<K, V> candidate = outer().ceilingEntry(key);
         return end == null || comparator.compare(candidate.getKey(), end.getKey()) < 0
               ? candidate : null;
      }

      @Override
      public Entry<K, V> higherEntry(K key) {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Entry<K, V> start = startNode();
         if (start != null && comparator.compare(key, start.getKey()) <= 0) {
            return firstEntry();
         }
         Entry<K, V> end = endNode();
         if (end != null && comparator.compare(key, end.getKey()) >= 0) {
            return null;
         }
         Entry<K, V> candidate = outer().higherEntry(key);
         return end == null || comparator.compare(candidate.getKey(), end.getKey()) < 0
               ? candidate : null;
      }

      @Override
      public Entry<K, V> firstEntry() {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Entry<K, V> start = startNode();
         return start == null ? outer().firstEntry() : outer().higherEntry(start.getKey());
      }

      @Override
      public Entry<K, V> lastEntry() {
         checkModCount();
         if (isEmpty()) {
            return null;
         }
         Entry<K, V> end = endNode();
         return end == null ? outer().lastEntry() : outer().lowerEntry(end.getKey());
      }

      @Override
      public Entry<K, V> pollFirstEntry() {
         checkModCount();
         Entry<K, V> first = firstEntry();
         if (first == null) {
            return null;
         }
         remove(first.getKey());
         contractAfterRemove();
         return first;
      }

      @Override
      public Entry<K, V> pollLastEntry() {
         checkModCount();
         Entry<K, V> last = lastEntry();
         if (last == null) {
            return null;
         }
         remove(last.getKey());
         contractAfterRemove();
         return last;
      }

      @Override
      public boolean containsKey(Object key) {
         checkModCount();
         return isInSubRange(outer().indexOfKey(key));
      }

      @Override
      protected Entry<K, V> getEntry(Object key) {
         // This method is unused because we've overridden all methods that otherwise need it.
         // But we implement it for the benefit of possible sub-classes that may want to use it.
         checkModCount();
         // getEntry() is spec'ed to throw ClassCastException if the comparator doesn't like the
         // specified object, so it's okay to cast here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         return isKeyInRange(k) ? outer().getEntry(key) : null;
      }

      @Override
      public V get(Object key) {
         checkModCount();
         // get() is spec'ed to throw ClassCastException if the comparator doesn't like the
         // specified object, so it's okay to cast here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         return isKeyInRange(k) ? outer().get(key) : null;
      }

      @Override
      public V put(K key, V value) {
         checkModCount();
         if (isKeyInRange(key)) {
            int sz = outer().size();
            V ret = outer().put(key, value);
            int delta = outer().size() - sz;
            assert delta == 0 || delta == 1;
            expandAfterAdd(delta);
            return ret;
         } else {
            throw new IllegalArgumentException("key " + key + " out of range for sub-map");
         }
      }

      @Override
      public Entry<K, V> removeEntry(Object key) {
         // This method is unused because we've overridden all methods that otherwise need it.
         // But we implement it for the benefit of possible sub-classes that may want to use it.
         checkModCount();
         // removeEntry() is spec'ed to throw ClassCastException if the comparator doesn't like the
         // specified object, so it's okay to cast here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         if (isKeyInRange(k)) {
            int sz = outer().size();
            Entry<K, V> ret = outer().removeEntry(key);
            int delta = outer().size() - sz;
            if (delta != 0) {
               assert delta == -1;
               contractAfterRemove();
            }
            return ret;
         }
         return null;
      }

      @Override
      public V remove(Object key) {
         checkModCount();
         // remove() is spec'ed to throw ClassCastException if the comparator doesn't like the
         // specified object, so it's okay to cast here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         if (isKeyInRange(k)) {
            int sz = outer().size();
            V ret = outer().remove(key);
            int delta = outer().size() - sz;
            if (delta != 0) {
               assert delta == -1;
               contractAfterRemove();
            }
            return ret;
         }
         return null;
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         checkModCount();
         for (K key : m.keySet()) {
            if (!isKeyInRange(key)) {
               throw new IllegalArgumentException("key " + key + " out of range for sub-map");
            }
         }
         int sz = outer().size();
         outer().putAll(m);
         int delta = outer().size() - sz;
         assert delta >= 0 && delta <= m.size();
         expandAfterAdd(delta);
      }

      @Override
      public void clear() {
         for (Iterator<Entry<K, V>> iter = listIterator(); iter.hasNext();) {
            iter.next();
            iter.remove();
         }
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator() {
         checkModCount();
         return new SubMapByIndicesIterator<K, V>(
               new EntryListIteratorImpl(adjustIndex(0)), this);
      }

      @Override
      public ListIterator<Entry<K, V>> listIterator(int index) {
         checkModCount();
         rangeCheckWide(index);
         return new SubMapByIndicesIterator<K, V>(
               new EntryListIteratorImpl(adjustIndex(index)), this);
      }

      @Override
      public Entry<K, V> getEntry(int index) {
         checkModCount();
         rangeCheck(index);
         return outer().getEntry(adjustIndex(index));
      }

      @Override
      public Entry<K, V> removeEntry(int index) {
         checkModCount();
         rangeCheck(index);
         return outer().removeEntry(adjustIndex(index));
      }

      @Override
      public int indexOfKey(Object key) {
         checkModCount();
         int index = outer().indexOfKey(key);
         return isInSubRange(index) ? index - startIndex : -1;
      }

      @Override
      public RandomAccessNavigableMap<K, V> subMapByIndices(int fromIndex, int toIndex) {
         checkModCount();
         rangeCheckWide(fromIndex);
         rangeCheckWide(toIndex);
         return new SubMapByIndices(adjustIndex(fromIndex), adjustIndex(toIndex), this);
      }

      @Override
      public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         if (comparator.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("fromKey (" + fromKey + ") > toKey (" + toKey + ")");
         }
         K presentLowerBound = fromInclusive ? ceilingKey(fromKey) : higherKey(fromKey);
         K presentUpperBound = toInclusive ? floorKey(toKey) : lowerKey(toKey);
         int fromIndex = presentLowerBound == null ? size() : indexOfKey(presentLowerBound);
         int toIndex = presentUpperBound == null ? 0 : indexOfKey(presentUpperBound) + 1;
         return new SubMapByIndicesAndByValueBounds(adjustIndex(fromIndex), adjustIndex(toIndex),
               fromKey, fromInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
               toKey, toInclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE, this);
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         K presentUpperBound = inclusive ? floorKey(toKey) : lowerKey(toKey);
         int toIndex = presentUpperBound == null ? 0 : indexOfKey(presentUpperBound) + 1;
         return new SubMapByIndicesAndByValueBounds(startIndex, adjustIndex(toIndex), null,
               BoundType.NO_BOUND, toKey, inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE,
               this);
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         K presentLowerBound = inclusive ? ceilingKey(fromKey) : higherKey(fromKey);
         int fromIndex = presentLowerBound == null ? size() : indexOfKey(presentLowerBound);
         return new SubMapByIndicesAndByValueBounds(adjustIndex(fromIndex), endIndex, fromKey,
               inclusive ? BoundType.INCLUSIVE : BoundType.EXCLUSIVE, null, BoundType.NO_BOUND,
               this);
      }
   }
   
   /**
    * Implements an iterator over a {@linkplain #subMapByIndices(int, int) sub-map (by indices)} of
    * elements for the various set views of this map.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <K> the type of key in the map
    * @param <V> the type of element in the map
    */
   private static class SubMapByIndicesIterator<K, V> implements ListIterator<Entry<K, V>> {
      private final ListIterator<Entry<K, V>> iterator;
      private final AbstractRandomAccessNavigableMap<K, V>.SubMapByIndices submap;
      
      SubMapByIndicesIterator(ListIterator<Entry<K, V>> iterator,
            AbstractRandomAccessNavigableMap<K, V>.SubMapByIndices subset) {
         this.iterator = iterator;
         this.submap = subset;
      }

      @Override
      public boolean hasNext() {
         return submap.isInSubRange(iterator.nextIndex());
      }

      @Override
      public Entry<K, V> next() {
         if (submap.isInSubRange(iterator.nextIndex())) {
            return iterator.next();
         }
         throw new NoSuchElementException();
      }

      @Override
      public boolean hasPrevious() {
         return submap.isInSubRange(iterator.previousIndex());
      }

      @Override
      public Entry<K, V> previous() {
         if (submap.isInSubRange(iterator.previousIndex())) {
            return iterator.previous();
         }
         throw new NoSuchElementException();
      }

      @Override
      public int nextIndex() {
         return submap.adjustIndex(iterator.nextIndex());
      }

      @Override
      public int previousIndex() {
         return submap.adjustIndex(iterator.previousIndex());
      }

      @Override
      public void remove() {
         iterator.remove();
         submap.contractAfterRemove();
      }

      @Override
      public void set(Entry<K, V> e) {
         throw new UnsupportedOperationException("set");
      }

      @Override
      public void add(Entry<K, V> e) {
         throw new UnsupportedOperationException("add");
      }
   }
   
   /**
    * A sub-map view for a {@link SubMapByIndices}. This view has constraints on both the key ranges
    * as well as index ranges.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class SubMapByIndicesAndByValueBounds extends SubMapByIndices {
      private final K lowerBound;
      private final BoundType lowerBoundType;
      private final K upperBound;
      private final BoundType upperBoundType;
      
      SubMapByIndicesAndByValueBounds(int startIndex, int endIndex,
            K startKey, BoundType startBoundType, K endKey, BoundType endBoundType,
            AbstractRandomAccessNavigableMap<K, V>.SubMapByIndices parent) {
         AbstractRandomAccessNavigableMap.this.super(startIndex, endIndex, parent);
         this.lowerBound = startKey;
         this.lowerBoundType = startBoundType;
         this.upperBound = endKey;
         this.upperBoundType = endBoundType;
      }
      
      /**
       * Returns true if the specified key is within the sub-map's bounds.
       * 
       * @param key a key
       * @return true if the key is within the sub-map's bounds
       */
      protected boolean isKeyWithinBounds(Object key) {
         // unchecked cast may cause ClassCastException in comparisons but that's acceptable here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         
         if (lowerBoundType == BoundType.NO_BOUND) {
            return CollectionUtils.isInRangeHigh(k, true, upperBound,
                  upperBoundType == BoundType.INCLUSIVE, comparator);
         } else if (upperBoundType == BoundType.NO_BOUND) {
            return CollectionUtils.isInRangeLow(k, true, lowerBound,
                  lowerBoundType == BoundType.INCLUSIVE, comparator);
         } else {
            return CollectionUtils.isInRange(k, lowerBound, lowerBoundType == BoundType.INCLUSIVE,
                  upperBound, upperBoundType == BoundType.INCLUSIVE, comparator);
         }
      }
      
      @Override boolean isKeyInRange(K key) {
         return isKeyWithinBounds(key) && super.isKeyInRange(key);
      }
      
      @Override
      public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         if (!isKeyWithinBounds(fromKey)) {
            throw new IllegalArgumentException("key " + fromKey + " out of range for sub-map");
         }
         if (!isKeyWithinBounds(toKey)) {
            throw new IllegalArgumentException("key " + toKey + " out of range for sub-map");
         }
         if (comparator.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("from key (" + fromKey + ") > to key (" + toKey + ")");
         }
         return super.subMap(fromKey, fromInclusive, toKey, toInclusive);
      }

      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         if (!isKeyWithinBounds(toKey)) {
            throw new IllegalArgumentException("key " + toKey + " out of range for sub-map");
         }
         return super.headMap(toKey, inclusive);
      }

      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         if (!isKeyWithinBounds(fromKey)) {
            throw new IllegalArgumentException("key " + fromKey + " out of range for sub-map");
         }
         return super.tailMap(fromKey, inclusive);
      }
   }
   
   /**
    * A sub-map view, with key value bounds, not indices.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected class RandomAccessSubMap extends AbstractRandomAccessNavigableMap<K, V> {
      
      private final K lowerBound;
      private final BoundType lowerBoundType;
      private final K upperBound;
      private final BoundType upperBoundType;
      private int indexOffset = -1;
      private int size = -1;
      
      /**
       * Constructs a new sub-map view with the given bounds.
       * 
       * @param lowerBound the lower bound, ignored when
       *       {@code lowerBoundType == }{@link AbstractNavigableMap.BoundType#NO_BOUND NO_BOUND}
       * @param lowerBoundType the lower bound type
       * @param upperBound the upper bound, ignored when
       *       {@code upperBoundType == }{@link AbstractNavigableMap.BoundType#NO_BOUND NO_BOUND}
       * @param upperBoundType the upper bound type
       */
      protected RandomAccessSubMap(K lowerBound, BoundType lowerBoundType, K upperBound,
            BoundType upperBoundType) {
         super(AbstractRandomAccessNavigableMap.this.comparator);
         this.lowerBound = lowerBound;
         this.lowerBoundType = lowerBoundType;
         this.upperBound = upperBound;
         this.upperBoundType = upperBoundType;
      }

      /**
       * Returns true if the specified key is within the sub-map's bounds.
       * 
       * @param key a key
       * @return true if the key is within the sub-map's bounds
       */
      protected boolean isInRange(Object key) {
         // unchecked cast may cause ClassCastException in comparisons but that's acceptable here
         @SuppressWarnings("unchecked")
         K k = (K) key;
         
         if (lowerBoundType == BoundType.NO_BOUND) {
            return CollectionUtils.isInRangeHigh(k, true, upperBound,
                  upperBoundType == BoundType.INCLUSIVE, comparator);
         } else if (upperBoundType == BoundType.NO_BOUND) {
            return CollectionUtils.isInRangeLow(k, true, lowerBound,
                  lowerBoundType == BoundType.INCLUSIVE, comparator);
         } else {
            return CollectionUtils.isInRange(k, lowerBound, lowerBoundType == BoundType.INCLUSIVE,
                  upperBound, upperBoundType == BoundType.INCLUSIVE, comparator);
         }
      }
      
      private AbstractRandomAccessNavigableMap<K, V> outer() {
         return AbstractRandomAccessNavigableMap.this;
      }
      
      /**
       * {@inheritDoc}
       * 
       * <p>This checks if the key is in range and returns {@code null} if not. If it is in range,
       * then the main map's {@link #getEntry(Object)} method is called.
       */
      @Override
      protected Entry<K, V> getEntry(Object key) {
         return isInRange(key) ? outer().getEntry(key) : null;
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This checks if the key is in range and returns {@code null} if not. If it is in range,
       * then the main map's {@link #removeEntry(Object)} method is called.
       */
      @Override
      protected Entry<K, V> removeEntry(Object key) {
         return isInRange(key) ? outer().removeEntry(key) : null;
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This implementation actually counts the number of entries within the sub-map's bounds
       * using the {@link #entrySet()}'s iterator. This runs in linear time, O(n), but the result
       * is memo-ized between calls. It will only count the number of entries again if it detects
       * that the main map's {@link #modCount} has changed since the last time the entries were
       * counted.
       */
      @Override
      public int size() {
         if (size == -1 || modCount != outer().modCount) {
            int sz = 0;
            for (Iterator<Entry<K, V>> iter = entrySet().iterator(); iter.hasNext(); ) {
               iter.next();
               sz++;
            }
            size = sz;
            if (modCount != outer().modCount) {
               modCount = outer().modCount;
               indexOffset = -1;
            }
         }
         return size;
      }

      private int getIndexOffset() {
         if (indexOffset == -1 || modCount != outer().modCount) {
            Entry<K, V> first = firstEntry();
            indexOffset = first == null ? 0
                  : outer().indexOfKey(first.getKey());
            if (modCount != outer().modCount) {
               modCount = outer().modCount;
               size = -1;
            }
         }
         return indexOffset;
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This checks if the key is in range and throws an {@link IllegalArgumentException} if
       * not. If it is in range, then the main map's {@link #put(Object, Object)} method is called.
       */
      @Override
      public V put(K key, V value) {
         if (!isInRange(key)) {
            throw new IllegalArgumentException("Key " + key + " outside of submap range");
         }
         return outer().put(key,  value);
      }

      /**
       * {@inheritDoc}
       * 
       * <p>This checks that if <em>all</em> keys in the specified map are in range and throws an
       * {@link IllegalArgumentException} if any of them are not. If all keys are in range, then the
       * main map's {@link #putAll(Map)} method is called.
       */
      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         for (K k : m.keySet()) {
            if (!isInRange(k)) {
               throw new IllegalArgumentException("Key " + k + " outside of submap range");
            }
         }
         outer().putAll(m);
      }

      /**
       * Removes all elements within the sub-map's bounds.
       * 
       * <p>This uses the same implementation as the {@linkplain AbstractNavigableMap#clear()
       * default}. It also updates its memo-ized size to zero (so {@link #size()} will not need to
       * count elements unless items are actually added to the map).
       */
      @Override
      public void clear() {
         super.clear();
         size = 0;
         modCount = outer().modCount;
      }

      /**
       * Provides a new sub-map view (sub-sub-map?). If the specified bounds are not within this
       * sub-map's bounds then a {@link IllegalArgumentException} is thrown. This delegates to the
       * main map's implementation after range-checking the specified bounds.
       */
      @Override
      public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
            boolean toInclusive) {
         if (!isInRange(toKey)) {
            throw new IllegalArgumentException("to key " + toKey + " is outside sub-map range");
         }
         if (!isInRange(fromKey)) {
            throw new IllegalArgumentException("from key " + fromKey + " is outside sub-map range");
         }
         if (comparator.compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("from key (" + fromKey + ") > to key (" + toKey + ")");
         }
         return outer().subMap(fromKey, fromInclusive, toKey, toInclusive);
      }

      /**
       * Provides a new view of the head of this sub-map. If the specified upper bound is not within
       * this sub-map's bounds then a {@link IllegalArgumentException} is thrown. This delegates to
       * the main map's implementation after range-checking the specified bound.
       * 
       * <p>If this sub-map has no lower bound, then the main map's {@link #headMap(Object, boolean)}
       * method is used. If this sub-map <em>does</em> have a lower bound, then the main map's
       * {@link #subMap(Object, boolean, Object, boolean)} method is used and supplied this
       * sub-map's lower bound and the specified upper bound.
       */
      @Override
      public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         if (!isInRange(toKey)) {
            throw new IllegalArgumentException("to key " + toKey + " is outside sub-map range");
         }
         return lowerBoundType == BoundType.NO_BOUND
               ? outer().headMap(toKey, inclusive)
               : outer().subMap(lowerBound, lowerBoundType == BoundType.INCLUSIVE,
                     toKey, inclusive);
      }

      /**
       * Provides a new view of the tail of this sub-map. If the specified lower bound is not within
       * this sub-map's bounds then a {@link IllegalArgumentException} is thrown. This delegates to
       * the main map's implementation after range-checking the specified bound.
       * 
       * <p>If this sub-map has no upper bound, then the main map's {@link #tailMap(Object, boolean)}
       * method is used. If this sub-map <em>does</em> have an upper bound, then the main map's
       * {@link #subMap(Object, boolean, Object, boolean)} method is used and supplied the specified
       * lower bound and this sub-map's upper bound.
       */
      @Override
      public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         if (!isInRange(fromKey)) {
            throw new IllegalArgumentException("from key " + fromKey + " is outside sub-map range");
         }
         return upperBoundType == BoundType.NO_BOUND
               ? outer().tailMap(fromKey, inclusive)
               : outer().subMap(fromKey, inclusive, upperBound,
                     upperBoundType == BoundType.INCLUSIVE);
      }
      
      /**
       * Finds the entry in this sub-map with the greatest key that is less than the specified key.
       * This first calls the main map's implementation of {@link #lowerEntry(Object)}. If it is not
       * null then it is range checked and returned if within the sub-map's bounds. If it is less
       * than the sub-map's lower bound then null is returned. If it is greater than the sub-map's
       * upper bound, then the sub-map's {@link #lastEntry()} is returned.
       */
      @Override
      public Entry<K, V> lowerEntry(K key) {
         Entry<K, V> candidate = outer().lowerEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return lastEntry();
         }
         return candidate; 
      }

      /**
       * Finds the entry in this sub-map with the greatest key that is less than or equal to the
       * specified key. This first calls the main map's implementation of {@link #floorEntry(Object)}.
       * If it is not null then it is range checked and returned if within the sub-map's bounds. If
       * it is less than the sub-map's lower bound then null is returned. If it is greater than the
       * sub-map's upper bound, then the sub-map's {@link #lastEntry()} is returned.
       */
      @Override
      public Entry<K, V> floorEntry(K key) {
         Entry<K, V> candidate = outer().floorEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return lastEntry();
         }
         return candidate;
      }

      /**
       * Finds the entry in this sub-map with the smallest key that is greater than or equal to the
       * specified key. This first calls the main map's implementation of {@link
       * #ceilingEntry(Object)}. If it is not null then it is range checked and returned if within
       * the sub-map's bounds. If it is greater than the sub-map's upper bound then null is
       * returned. If it is less than the sub-map's lower bound, then the sub-map's {@link
       * #firstEntry()} is returned.
       */
      @Override
      public Entry<K, V> ceilingEntry(K key) {
         Entry<K, V> candidate = outer().ceilingEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return firstEntry();
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }

      /**
       * Finds the entry in this sub-map with the smallest key that is greater than the specified
       * key. This first calls the main map's implementation of {@link #higherEntry(Object)}. If it
       * is not null then it is range checked and returned if within the sub-map's bounds. If it is
       * greater than the sub-map's upper bound then null is returned. If it is less than the
       * sub-map's lower bound, then the sub-map's {@link #firstEntry()} is returned.
       */
      @Override
      public Entry<K, V> higherEntry(K key) {
         Entry<K, V> candidate = outer().higherEntry(key);
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return firstEntry();
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }

      /**
       * Finds the entry in the map with the smallest key or {@code null} if the sub-map is empty.
       * If the sub-map has no lower bound, the main map's first entry is queried. If the sub-map's
       * lower bound is inclusive, then this finds the entry using the main map's {@link
       * #ceilingEntry(Object)} method and passing the lower bound. If exclusive, the main map's
       * {@link #higherEntry(Object)} method is used instead. If the resulting entry is non-null and
       * within the sub-map's bounds then it is returned. If, however, it is null or greater than
       * the sub-map's upper bound, {@code null} is returned.
       */
      @Override
      public Entry<K, V> firstEntry() {
         Entry<K, V> candidate;
         switch (lowerBoundType) {
            case NO_BOUND:
               candidate = outer().firstEntry();
               break;
            case INCLUSIVE:
               candidate = outer().ceilingEntry(lowerBound);
               break;
            case EXCLUSIVE:
               candidate = outer().higherEntry(lowerBound);
               break;
            default:
               throw new AssertionError();
         }
         if (candidate == null) {
            return null;
         }
         if (upperBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeHigh(candidate.getKey(), true, upperBound,
                     upperBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }

      /**
       * Finds the entry in the map with the greatest key or {@code null} if the sub-map is empty.
       * If the sub-map has no upper bound, the main map's last entry is queried. If the sub-map's
       * upper bound is inclusive, then this finds the entry using the main map's {@link
       * #floorEntry(Object)} method and passing the upper bound. If exclusive, the main map's
       * {@link #lowerEntry(Object)} method is used instead. If the resulting entry is non-null and
       * within the sub-map's bounds then it is returned. If, however, it is null or less than the
       * sub-map's lower bound, {@code null} is returned.
       */
      @Override
      public Entry<K, V> lastEntry() {
         Entry<K, V> candidate;
         switch (upperBoundType) {
            case NO_BOUND:
               candidate = outer().lastEntry();
               break;
            case INCLUSIVE:
               candidate = outer().floorEntry(upperBound);
               break;
            case EXCLUSIVE:
               candidate = outer().lowerEntry(upperBound);
               break;
            default:
               throw new AssertionError();
         }
         if (candidate == null) {
            return null;
         }
         if (lowerBoundType != BoundType.NO_BOUND &&
               !CollectionUtils.isInRangeLow(candidate.getKey(), true, lowerBound,
                     lowerBoundType == BoundType.INCLUSIVE, comparator)) {
            return null;
         }
         return candidate;
      }
      
      /**
       * Returns the main map's modification count. This allows iterators over sub-map views to
       * properly fail-fast.
       * 
       * <p>Since a {@link RandomAccessSubMap} <em>extends</em> {@link AbstractRandomAccessNavigableMap},
       * it also inherits a {@link AbstractNavigableMap#modCount} field. For sub-maps, that field is
       * compared to the main maps' value to detect when the sub-map's memo-ized size and index
       * bounds are stale and elements need to be counted.
       * 
       * @return {@inheritDoc}
       */
      @Override protected int getModCount() {
         return outer().getModCount();
      }

      @Override
      public Entry<K, V> getEntry(int index) {
         rangeCheck(index);
         return outer().getEntry(index + getIndexOffset());
      }

      @Override
      public Entry<K, V> removeEntry(int index) {
         rangeCheck(index);
         return outer().removeEntry(index + getIndexOffset());
      }
      
      @Override
      public RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex) {
         rangeCheckWide(startIndex);
         rangeCheckWide(endIndex);
         if (startIndex > endIndex) {
            throw new IndexOutOfBoundsException();
         }
         return new SubMapByIndicesAndByValueBounds(startIndex, endIndex, lowerBound, lowerBoundType,
               upperBound, upperBoundType, null);
      }
   }
}
