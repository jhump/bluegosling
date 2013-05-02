package com.apriori.collections;

import com.apriori.util.Function;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

// TODO: tests!
// TODO: javadoc
/**
 * An {@link ArrayList} that is also an {@link AssociativeList}. This collection maintains
 * bidirectional mappings to/from list indices and associative keys using a {@link HashMap} (key to
 * index) and a {@link TreeMap} (index to key).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the list
 * @param <K> the type used for associative keys
 */
public class AssociativeArrayList<E, K> extends ArrayList<E> implements AssociativeList<E, K> {

   private static final long serialVersionUID = 324645810355942807L;
   
   HashMap<K, Integer> indexByKey = new HashMap<K, Integer>();
   TreeMap<Integer, K> keyByIndex = new TreeMap<Integer, K>();

   public AssociativeArrayList() {
   }

   public AssociativeArrayList(int initialCapacity) {
      super(initialCapacity);
   }

   public AssociativeArrayList(Collection<? extends E> coll) {
      super(coll);
   }

   public AssociativeArrayList(Map<? extends K, ? extends E> mappedEntries) {
      super(mappedEntries.size());
      addAll(mappedEntries);
   }

   public AssociativeArrayList(AssociativeList<? extends E, ? extends K> mappedEntries) {
      super(mappedEntries.size());
      addAll(mappedEntries);
   }

   static <K, V> Map<K, V> createMap(int expectedSize) {
      return new HashMap<K, V>(expectedSize * 100 / 75);
   }
   
   private void rangeCheckWide(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      } else if (index > size()) {
         throw new IndexOutOfBoundsException("" + index + " > " + size());
      }
   }
   
   private void rangeCheck(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      } else if (index >= size()) {
         throw new IndexOutOfBoundsException("" + index + " >= " + size());
      }
   }
   
   private void shiftElements(int startIndex, int shiftBy) {
      rangeCheck(startIndex);
      // adjust index mappings to accommodate inserted or removed items
      Map<Integer, K> remainingKeys = keyByIndex.tailMap(startIndex, true);
      Map<Integer, K> newKeys = createMap(remainingKeys.size());
      for (Iterator<Map.Entry<Integer, K>> iter = remainingKeys.entrySet().iterator();
            iter.hasNext();) {
         Map.Entry<Integer, K> entry = iter.next();
         newKeys.put(entry.getKey() + shiftBy, entry.getValue());
         iter.remove();
      }
      for (Map.Entry<Integer, K> entry : newKeys.entrySet()) {
         keyByIndex.put(entry.getKey(), entry.getValue());
         indexByKey.put(entry.getValue(), entry.getKey());
      }
   }
   
   @Override
   public int add(E element, K key) {
      int index = size();
      super.add(element);
      return createKey(key, index);
   }

   @Override
   public int add(int index, E element, K key) {
      shiftElements(index, 1);
      super.add(index, element);
      return createKey(key, index);
   }

   @Override
   public Map<K, Integer> addAll(Map<? extends K, ? extends E> mappedElements) {
      int index = size();
      Map<K, Integer> previousIndices = createMap(mappedElements.size());
      for (Map.Entry<? extends K, ? extends E> entry : mappedElements.entrySet()) {
         super.add(entry.getValue());
         int previousIndex = createKey(entry.getKey(), index++);
         if (previousIndex != -1) {
            previousIndices.put(entry.getKey(), previousIndex);
         }
      }
      return Collections.unmodifiableMap(previousIndices);
   }

   @Override
   public Map<K, Integer> addAll(int index, Map<? extends K, ? extends E> mappedElements) {
      shiftElements(index, mappedElements.size());
      Map<K, Integer> previousIndices = createMap(mappedElements.size());
      for (Map.Entry<? extends K, ? extends E> entry : mappedElements.entrySet()) {
         super.add(index, entry.getValue());
         int previousIndex = createKey(entry.getKey(), index++);
         if (previousIndex != -1) {
            previousIndices.put(entry.getKey(), previousIndex);
         }
      }
      return Collections.unmodifiableMap(previousIndices);
   }

   @Override
   public Map<K, Integer> addAll(AssociativeList<? extends E, ? extends K> mappedElements) {
      int index = size();
      super.addAll(mappedElements);
      Map<K, Integer> previousIndices = createMap(mappedElements.asMap().size());
      for (ListIterator<? extends K> iter = mappedElements.keyIterator(); iter.hasNext(); ) {
         K key = iter.next();
         int previousIndex = createKey(key, iter.previousIndex() + index);
         if (previousIndex != -1) {
            previousIndices.put(key, previousIndex);
         }
      }
      return Collections.unmodifiableMap(previousIndices);
   }

   @Override
   public Map<K, Integer> addAll(int index, AssociativeList<? extends E, ? extends K> mappedElements) {
      shiftElements(index, mappedElements.size());
      super.addAll(index, mappedElements);
      Map<K, Integer> previousIndices = createMap(mappedElements.asMap().size());
      for (ListIterator<? extends K> iter = mappedElements.keyIterator(); iter.hasNext(); ) {
         K key = iter.next();
         int previousIndex = createKey(key, iter.previousIndex() + index);
         if (previousIndex != -1) {
            previousIndices.put(key, previousIndex);
         }
      }
      return Collections.unmodifiableMap(previousIndices);
   }

   @Override
   public int createKey(K key, int index) {
      rangeCheck(index);
      Integer previousIndex = indexByKey.put(key, index);
      if (previousIndex != null) {
         keyByIndex.remove(previousIndex);
      }
      keyByIndex.put(index, key);
      return previousIndex == null ? -1 : previousIndex;
   }

   @Override
   public E getKey(K key) {
      Integer index = indexByKey.get(key);
      return index == null ? null : get(index);
   }

   @Override
   public int getKeyIndex(K key) {
      Integer index = indexByKey.get(key);
      return index == null ? -1 : index;
   }
   
   @Override
   public Map.Entry<Integer, E> removeKey(K key) {
      int index = forgetKey(key);
      if (index == -1) {
         return null;
      }
      E removedElement = remove(index);
      return mapEntry(index, removedElement);
   }

   @Override
   public int forgetKey(K key) {
      Integer previousIndex = indexByKey.get(key);
      if (previousIndex != null) {
         keyByIndex.remove(previousIndex);
         return previousIndex;
      }
      return -1;
   }

   @Override
   public K clearKey(int index) {
      rangeCheck(index);
      K key = keyByIndex.remove(index);
      if (key != null) {
         indexByKey.remove(key);
         return key;
      }
      return null;
   }

   @Override
   public boolean hasKey(int index) {
      rangeCheck(index);
      return keyByIndex.containsKey(index);
   }

   @Override
   public boolean containsKey(K key) {
      return indexByKey.containsKey(key);
   }
   
   @Override
   public ListIterator<K> keyIterator() {
      return new KeyIterator(keyedEntryIterator());
   }

   @Override
   public ListIterator<Map.Entry<K, E>> keyedEntryIterator() {
      return new KeyedEntryIterator();
   }

   @Override
   public int nextKeyedIndex(int start) {
      rangeCheckWide(start);
      Integer index = keyByIndex.navigableKeySet().ceiling(start);
      return index == null ? -1 : index;
   }

   @Override
   public Map<K, E> asMap() {
      return new MapImpl();
   }
   
   @Override
   public AssociativeList<E, K> subList(int fromIndex, int toIndex) {
      return new SubListImpl(fromIndex, toIndex, super.subList(fromIndex, toIndex));
   }
   
   @Override
   public void add(int index, E element) {
      shiftElements(index, 1);
      super.add(index, element);
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> coll) {
      shiftElements(index, coll.size());
      return super.addAll(index, coll);
   }

   @Override
   public boolean remove(Object o) {
      return CollectionUtils.removeObject(o,  iterator(),  true);
   }

   @Override
   public E remove(int index) {
      clearKey(index);
      shiftElements(index, -1);
      return super.remove(index);
   }
   
   @Override
   protected void removeRange(int fromIndex, int toIndex) {
      super.removeRange(fromIndex, toIndex);
      Map<Integer, K> removedIndices = keyByIndex.subMap(fromIndex, true, toIndex, false);
      for (K key : removedIndices.values()) {
         indexByKey.remove(key);
      }
      removedIndices.clear();
      shiftElements(toIndex, toIndex - fromIndex);
   }
   
   @Override
   @SuppressWarnings("unchecked")
   public AssociativeArrayList<E, K> clone() {
      AssociativeArrayList<E, K> copy = (AssociativeArrayList<E, K>) super.clone();
      copy.indexByKey = (HashMap<K, Integer>) indexByKey.clone();
      copy.keyByIndex = (TreeMap<Integer, K>) keyByIndex.clone();
      return copy;
   }
   
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject(); // this writes the list thanks to super-class
      out.writeObject(indexByKey);
   }
   
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject(); // this reads the list thanks to super-class
      indexByKey = (HashMap<K, Integer>) in.readObject();
      keyByIndex = new TreeMap<Integer, K>();
      for (Map.Entry<K, Integer> entry : indexByKey.entrySet()) {
         Integer index = entry.getValue();
         // if the value is null, out of range, or a dup, then the serialized form is bad
         if (index == null || index < 0 || index >= size() || keyByIndex.containsKey(index)) {
            throw new IOException("invalid associative index found in serialized form");
         }
         keyByIndex.put(entry.getValue(), entry.getKey());
      }
   }

   /** Creates a {@link Map.Entry} for use in implementing map-related methods. */
   static <K, V> Map.Entry<K, V> mapEntry(final K key, final V value) {
      return new Map.Entry<K, V>() {
         @Override public K getKey() {
            return key;
         }

         @Override public V getValue() {
            return value;
         }

         @Override public V setValue(V newValue) {
            throw new UnsupportedOperationException();
         }
         
         @Override public int hashCode() {
            return key.hashCode();
         }
         
         @Override public boolean equals(Object o) {
            if (o instanceof Map.Entry) {
               Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
               return key == null ? other.getKey() == null : key.equals(other.getKey())
                     && value == null ? other.getValue() == null : value.equals(other.getValue());
            }
            return false;
         }
         
         @Override public String toString() {
            return String.valueOf(key) + " -> " + String.valueOf(value);
         }
      };
   }
   
   /**
    * A list iterator that moves from one associative mapping to the next, skipping unmapped
    * list indices.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class KeyedEntryIterator implements ListIterator<Map.Entry<K, E>> {

      private List<E> list;
      private NavigableMap<Integer, K> keyByIndexMap;
      private Integer nextIndex = keyByIndexMap.navigableKeySet().ceiling(0);
      private Integer prevIndex = null;
      private final int myModCount;
      
      KeyedEntryIterator() {
         this(AssociativeArrayList.this, AssociativeArrayList.this.keyByIndex);
      }

      @SuppressWarnings("synthetic-access")
      KeyedEntryIterator(List<E> list, NavigableMap<Integer, K> keyByIndex) {
         this.list = list;
         this.keyByIndexMap = keyByIndex;
         myModCount = modCount;
      }
      
      @SuppressWarnings("synthetic-access")
      private void checkModCount() {
         if (myModCount != modCount) {
            throw new ConcurrentModificationException();
         }
      }
      
      @Override
      public boolean hasNext() {
         checkModCount();
         return nextIndex != null;
      }

      @Override
      public Map.Entry<K, E> next() {
         checkModCount();
         if (nextIndex == null) {
            throw new NoSuchElementException();
         }
         prevIndex = nextIndex;
         nextIndex = keyByIndexMap.navigableKeySet().ceiling(nextIndex + 1);
         return mapEntry(keyByIndexMap.get(prevIndex), list.get(prevIndex));
      }

      @Override
      public boolean hasPrevious() {
         checkModCount();
         return prevIndex != null;
      }

      @Override
      public Map.Entry<K, E> previous() {
         checkModCount();
         if (prevIndex == null) {
            throw new NoSuchElementException();
         }
         nextIndex = prevIndex;
         prevIndex = keyByIndexMap.navigableKeySet().floor(prevIndex - 1);
         return mapEntry(keyByIndexMap.get(nextIndex), list.get(nextIndex));
      }

      @Override
      public int nextIndex() {
         checkModCount();
         return nextIndex == null ? size() : nextIndex;
      }

      @Override
      public int previousIndex() {
         checkModCount();
         return prevIndex == null ? -1 : prevIndex;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void set(Map.Entry<K, E> e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(Map.Entry<K, E> e) {
         throw new UnsupportedOperationException();
      }
   }
   
   /**
    * A list iterator that moves from one associative mapping to the next, skipping unmapped
    * list indices. This differs from {@link KeyedEntryIterator} in that it only returns keys, not
    * key+value pairs.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class KeyIterator implements ListIterator<K> {

      private final ListIterator<Map.Entry<K, E>> entryIterator;
      
      KeyIterator(ListIterator<Map.Entry<K, E>> entryIterator) {
         this.entryIterator = entryIterator;
      }
      
      @Override
      public boolean hasNext() {
         return entryIterator.hasNext();
      }

      @Override
      public K next() {
         return entryIterator.next().getKey();
      }

      @Override
      public boolean hasPrevious() {
         return entryIterator.hasPrevious();
      }

      @Override
      public K previous() {
         return entryIterator.previous().getKey();
      }

      @Override
      public int nextIndex() {
         return entryIterator.nextIndex();
      }

      @Override
      public int previousIndex() {
         return entryIterator.previousIndex();
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }

      @Override
      public void set(K e) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void add(K e) {
         throw new UnsupportedOperationException();
      }
   }
   
   /**
    * Implements the map view of the associative list.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class MapImpl implements Map<K, E> {

      MapImpl() {
      }

      @Override
      public int size() {
         return indexByKey.size();
      }

      @Override
      public boolean isEmpty() {
         return indexByKey.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         return indexByKey.containsKey(key);
      }

      @Override
      public boolean containsValue(Object o) {
         for (E element : values()) {
            if (element == null ? o == null : element.equals(o)) {
               return true;
            }
         }
         return false;
      }

      @Override
      public E get(Object key) {
         Integer index = indexByKey.get(key);
         return index == null ? null : AssociativeArrayList.this.get(index);
      }

      @Override
      public E put(K key, E value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public E remove(Object key) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Map<? extends K, ? extends E> m) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Set<K> keySet() {
         return Collections.unmodifiableSet(indexByKey.keySet());
      }

      @Override
      public Collection<E> values() {
         return Collections.unmodifiableCollection(
               new TransformingCollection<Integer, E>(indexByKey.values(),
                     new Function<Integer, E>() {
                        @Override public E apply(Integer input) {
                           return AssociativeArrayList.this.get(input);
                        }
                     }));
      }

      @Override
      public Set<Map.Entry<K, E>> entrySet() {
         return Collections.unmodifiableSet(
               new TransformingSet<Map.Entry<K, Integer>, Map.Entry<K, E>>(indexByKey.entrySet(),
                     new Function<Map.Entry<K, Integer>, Map.Entry<K, E>>() {
                        @Override public Map.Entry<K, E> apply(Map.Entry<K, Integer> input) {
                           return mapEntry(input.getKey(),
                                 AssociativeArrayList.this.get(input.getValue()));
                        }
                     }));
      }
   }
   
   /**
    * Implements the map view for sub-lists of the associative list.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubListMapImpl implements Map<K, E> {
      
      final SubListImpl subList;

      SubListMapImpl(SubListImpl subList) {
         this.subList = subList;
      }

      @Override
      public int size() {
         return subList.subKeyByIndex.size();
      }

      @Override
      public boolean isEmpty() {
         return subList.subKeyByIndex.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         Integer index = indexByKey.get(key);
         return index != null && subList.isInRange(index);
      }

      @Override
      public boolean containsValue(Object o) {
         for (E element : values()) {
            if (element == null ? o == null : element.equals(o)) {
               return true;
            }
         }
         return false;
      }

      @Override
      public E get(Object key) {
         Integer index = indexByKey.get(key);
         return index == null || !subList.isInRange(index) ? null
               : AssociativeArrayList.this.get(index);
      }

      @Override
      public E put(K key, E value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public E remove(Object key) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void putAll(Map<? extends K, ? extends E> m) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Set<K> keySet() {
         return Collections.unmodifiableSet(
               new TransformingSet<Map.Entry<Integer, K>, K>(indicesAndKeys(),
                     new Function<Map.Entry<Integer, K>, K>() {
                        @Override public K apply(Map.Entry<Integer, K> input) {
                           return input.getValue();
                        }
                     }));
      }

      @Override
      public Collection<E> values() {
         return Collections.unmodifiableCollection(
               new TransformingCollection<Map.Entry<Integer, K>, E>(indicesAndKeys(),
                     new Function<Map.Entry<Integer, K>, E>() {
                        @Override public E apply(Map.Entry<Integer, K> input) {
                           return AssociativeArrayList.this.get(input.getKey());
                        }
                     }));
      }
      
      @Override
      public Set<Map.Entry<K, E>> entrySet() {
         return Collections.unmodifiableSet(
               new TransformingSet<Map.Entry<Integer, K>, Map.Entry<K, E>>(indicesAndKeys(),
                     new Function<Map.Entry<Integer, K>, Map.Entry<K, E>>() {
                        @Override public Map.Entry<K, E> apply(Map.Entry<Integer, K> input) {
                           return mapEntry(input.getValue(),
                                 AssociativeArrayList.this.get(input.getKey()));
                        }
                     }));
      }
      
      private Set<Map.Entry<Integer, K>> indicesAndKeys() {
         // We don't want to return subKeyByIndex or an actual subset since that field can be
         // changed as the sublist is mutated. Instead, we want to delegate each method call to that
         // field so we pick up changes when that map gets reset. So this is basically a dumb
         // "forwarding" set.
         return new Set<Map.Entry<Integer, K>>() {
            @Override
            public int size() {
               return subList.subKeyByIndex.size();
            }

            @Override
            public boolean isEmpty() {
               return subList.subKeyByIndex.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
               return subList.subKeyByIndex.entrySet().contains(o);
            }

            @Override
            public Iterator<Map.Entry<Integer, K>> iterator() {
               return subList.subKeyByIndex.entrySet().iterator();
            }

            @Override
            public Object[] toArray() {
               return subList.subKeyByIndex.entrySet().toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
               return subList.subKeyByIndex.entrySet().toArray(a);
            }

            @Override
            public boolean add(Map.Entry<Integer, K> e) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> c) {
               return subList.subKeyByIndex.entrySet().containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends Map.Entry<Integer, K>> c) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
               throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> c) {
               throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
               throw new UnsupportedOperationException();
            }
         };
      }
   }
   
   /**
    * Implements the sublist view of the associative list.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubListImpl implements AssociativeList<E, K> {

      private final List<E> subList;
      private final int fromIndex;
      int toIndex;
      NavigableMap<Integer, K> subKeyByIndex;
      
      SubListImpl(int fromIndex, int toIndex, List<E> subList) {
         this.subList = subList;
         this.fromIndex = fromIndex;
         this.toIndex = toIndex;
         resetMap();
      }
      
      void resetMap() {
         subKeyByIndex = keyByIndex.subMap(fromIndex, true, toIndex, false);
      }
      
      boolean isInRange(int index) {
         return index >= fromIndex && index < toIndex;
      }
      
      private void subRangeCheck(int index) {
         if (index < 0) {
            throw new IndexOutOfBoundsException("" + index + " < 0");
         } else if (index >= size()) {
            throw new IndexOutOfBoundsException("" + index + " >= " + size());
         }
      }
      
      @Override
      public int size() {
         return subList.size();
      }

      @Override
      public boolean isEmpty() {
         return subList.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return subList.contains(o);
      }

      @Override
      public Iterator<E> iterator() {
         return listIterator();
      }

      @Override
      public Object[] toArray() {
         return subList.toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return subList.toArray(a);
      }

      @Override
      public boolean add(E e) {
         subList.add(e);
         toIndex++;
         resetMap();
         return true;
      }

      @Override
      public boolean remove(Object o) {
         return CollectionUtils.removeObject(o, iterator(), true);
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return subList.containsAll(c);
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         if (subList.addAll(c)) {
            toIndex += c.size();
            resetMap();
            return true;
         }
         return false;
      }

      @Override
      public boolean addAll(int index, Collection<? extends E> c) {
         if (subList.addAll(index, c)) {
            toIndex += c.size();
            resetMap();
            return true;
         }
         return false;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), true);
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return CollectionUtils.filter(c, iterator(), false);
      }

      @Override
      public void clear() {
         subList.clear();
         toIndex = fromIndex;
         resetMap();
      }

      @Override
      public E get(int index) {
         return subList.get(index);
      }

      @Override
      public E set(int index, E element) {
         return subList.set(index, element);
      }

      @Override
      public void add(int index, E element) {
         subList.add(index, element);
         toIndex++;
         resetMap();
      }

      @Override
      public E remove(int index) {
         E ret = subList.remove(index);
         toIndex--;
         resetMap();
         return ret;
      }

      @Override
      public int indexOf(Object o) {
         return subList.indexOf(o);
      }

      @Override
      public int lastIndexOf(Object o) {
         return subList.lastIndexOf(o);
      }

      @Override
      public ListIterator<E> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<E> listIterator(int index) {
         final ListIterator<E> delegate = subList.listIterator(index);
         // need to expand or contract the index bounds when adding or removing elements
         return new ListIterator<E>() {

            @Override public boolean hasNext() {
               return delegate.hasNext();
            }

            @Override public E next() {
               return delegate.next();
            }

            @Override public boolean hasPrevious() {
               return delegate.hasPrevious();
            }

            @Override public E previous() {
               return delegate.previous();
            }

            @Override public int nextIndex() {
               return delegate.nextIndex();
            }

            @Override public int previousIndex() {
               return delegate.previousIndex();
            }

            @Override public void remove() {
               delegate.remove();
               toIndex--;
               resetMap();
            }

            @Override public void set(E e) {
               delegate.set(e);
            }

            @Override public void add(E e) {
               delegate.add(e);
               toIndex++;
               resetMap();
            }
         };
      }

      @Override
      public int add(E element, K key) {
         return add(size(), element, key);
      }

      @Override
      public int add(int index, E element, K key) {
         int ret = AssociativeArrayList.this.add(index + fromIndex, element, key);
         toIndex++;
         resetMap();
         return ret - fromIndex;
      }

      @Override
      public Map<K, Integer> addAll(Map<? extends K, ? extends E> mappedElements) {
         return addAll(size(), mappedElements);
      }
      
      private Map<K, Integer> adjust(Map<K, Integer> indices) {
         Map<K, Integer> ret = createMap(indices.size());
         for (Map.Entry<K, Integer> entry : indices.entrySet()) {
            ret.put(entry.getKey(), entry.getValue() - fromIndex);
         }
         return Collections.unmodifiableMap(ret);
      }

      @Override
      public Map<K, Integer> addAll(int index, Map<? extends K, ? extends E> mappedElements) {
         Map<K, Integer> ret = AssociativeArrayList.this.addAll(index + fromIndex, mappedElements);
         toIndex += mappedElements.size();
         resetMap();
         return adjust(ret);
      }

      @Override
      public Map<K, Integer> addAll(AssociativeList<? extends E, ? extends K> mappedElements) {
         return addAll(size(), mappedElements);
      }

      @Override
      public Map<K, Integer> addAll(int index,
            AssociativeList<? extends E, ? extends K> mappedElements) {
         Map<K, Integer> ret = AssociativeArrayList.this.addAll(index + fromIndex, mappedElements);
         toIndex += mappedElements.size();
         resetMap();
         return adjust(ret);
      }

      @Override
      public int createKey(K key, int index) {
         subRangeCheck(index);
         return AssociativeArrayList.this.createKey(key, index + fromIndex);
      }

      @Override
      public E getKey(K key) {
         int index = getKeyIndex(key);
         return index == -1 ? null : get(index);
      }

      @Override
      public int getKeyIndex(K key) {
         int index = AssociativeArrayList.this.getKeyIndex(key);
         return (index == -1 || !isInRange(index)) ? -1 : index - fromIndex;
      }

      @Override
      public Map.Entry<Integer, E> removeKey(K key) {
         int index = getKeyIndex(key);
         if (index == -1) {
            return null;
         }
         Map.Entry<Integer, E> removed = AssociativeArrayList.this.removeKey(key);
         return mapEntry(removed.getKey() - fromIndex, removed.getValue());
      }

      @Override
      public int forgetKey(K key) {
         int index = getKeyIndex(key);
         return index == -1 ? -1 : AssociativeArrayList.this.forgetKey(key) - fromIndex;
      }

      @Override
      public K clearKey(int index) {
         subRangeCheck(index);
         return AssociativeArrayList.this.clearKey(index + fromIndex);
      }

      @Override
      public boolean hasKey(int index) {
         subRangeCheck(index);
         return AssociativeArrayList.this.hasKey(index + fromIndex);
      }

      @Override
      public boolean containsKey(K key) {
         return getKeyIndex(key) != -1;
      }

      @Override
      public ListIterator<K> keyIterator() {
         return new KeyIterator(keyedEntryIterator());
      }

      @Override
      public ListIterator<Map.Entry<K, E>> keyedEntryIterator() {
         return new KeyedEntryIterator(subList, subKeyByIndex);
      }

      @Override
      public int nextKeyedIndex(int start) {
         if (start == size()) {
            return -1;
         }
         subRangeCheck(start);
         int ret = AssociativeArrayList.this.nextKeyedIndex(start + fromIndex);
         return (ret == -1 || !isInRange(ret)) ? -1 : ret - fromIndex;
      }

      @Override
      public Map<K, E> asMap() {
         return new SubListMapImpl(this);
      }

      @Override
      public AssociativeList<E, K> subList(int from, int to) {
         return new SubListImpl(fromIndex + from, fromIndex + to, subList.subList(from, to));
      }
   }
}

