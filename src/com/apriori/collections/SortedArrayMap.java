package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

// TODO: implement me: more efficient iterator and cloning!
// TODO: javadoc
// TODO: tests!
// TODO: implement RandomAccessNavigableMap<K, V>
public class SortedArrayMap<K, V> extends AbstractNavigableMap<K, V>
      implements Serializable, Cloneable {

   private static final long serialVersionUID = 1132076181990365854L;

   private static final int DEFAULT_INITIAL_CAPACITY = 10;
   
   private static class EntryImpl<K, V> implements Entry<K, V> {
      private final K key;
      private V value;
      EntryImpl(Entry<? extends K, ? extends V> entry) {
         this(entry.getKey(), entry.getValue());
      }
      
      EntryImpl(K key, V value) {
         this.key = key;
         this.value = value;
      }

      @Override
      public K getKey() {
         return key;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public V setValue(V value) {
         V ret = this.value;
         this.value = value;
         return ret;
      }
      
      @Override
      public boolean equals(Object o) {
         return MapUtils.equals(this,  o);
      }
      
      @Override
      public int hashCode() {
         return MapUtils.hashCode(this);
      }
      
      @Override
      public String toString() {
         return MapUtils.toString(this);
      }
   }
   
   private static <K, V> Comparator<Entry<K, V>> entryComparator(
         final Comparator<? super K> comparator) {
      return new Comparator<Entry<K, V>>() {
         @Override
         public int compare(Entry<K, V> o1, Entry<K, V> o2) {
            return comparator.compare(o1.getKey(), o2.getKey());
         }
      };
   }
   
   private transient Comparator<Entry<K, V>> entryComparator;
   private transient int size;
   private EntryImpl<K, V> data[];
   
   public SortedArrayMap() {
      this(DEFAULT_INITIAL_CAPACITY);
   }

   public SortedArrayMap(int initialCapacity) {
      this(initialCapacity, null);
   }

   public SortedArrayMap(Comparator<? super K> comparator) {
      this(DEFAULT_INITIAL_CAPACITY, comparator);
   }
   
   @SuppressWarnings("unchecked") // can't create init generic array member w/out unchecked cast...
   public SortedArrayMap(int initialCapacity, Comparator<? super K> comparator) {
      super(comparator);
      data = new EntryImpl[initialCapacity];
      entryComparator = entryComparator(this.comparator);
   }

   public SortedArrayMap(Map<? extends K, ? extends V> map) {
      this(map.size());
      putAll(map);
   }

   public SortedArrayMap(Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
      this(map.size(), comparator);
      putAll(map);
   }
   
   public SortedArrayMap(SortedMap<K, ? extends V> map) {
      this(map.size(), map.comparator());
      putAll(map);
   }
   
   private void maybeGrowBy(int spaceNeeded) {
      int requiredCapacity = size + spaceNeeded;
      if (data.length >= requiredCapacity) {
         return;
      }
      
      int totalCapacity = data.length;
      // grow by 50%
      totalCapacity += totalCapacity >> 1;
      // if 50% not enough, grow by more
      totalCapacity = Math.max(totalCapacity, requiredCapacity);

      EntryImpl<K, V> d[] = ArrayUtils.ensureCapacity(data, totalCapacity);
      System.arraycopy(data, 0, d, 0, size);
      data = d;
      modCount++;
   }
   
   /**
    * Shrinks the internal array so that it is exactly the necessary size. Attempts to add an item
    * to the set after this is called will cause it to grow the internal array to fit the new item.
    */
   public void trimToSize() {
      if (data.length != size) {
         data = Arrays.copyOf(data, size);
         modCount++;
      }
   }

   private int sortThreshold() {
      // TODO: compute threshold for number of items added to map, above which we should append
      // items to buffer and re-sort the whole thing ( O(m + n log n) where m is number of items to
      // add and n is number of items already in the map) and below which we can insert the items
      // one at a time ( O(m * n) )
      return Math.max(2, size >> 2);
   }
   
   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      if (map.isEmpty()) {
         return;
      }
      if (size == 0 || map.size() > sortThreshold()) {
         maybeGrowBy(map.size());
         int previousSize = size;
         for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            data[size++] = new EntryImpl<K, V>(entry);
         }
         if (previousSize == 0 && map instanceof SortedMap) {
            SortedMap<? extends K, ? extends V> sortedMap =
                  (SortedMap<? extends K, ? extends V>) map;
            if (comparator() == null ? sortedMap.comparator() == null
                  : comparator().equals(sortedMap.comparator())) {
               // w00t! values are already in correct order
               return;
            }
         }
         // we now need to sort the array
         Arrays.sort(data, 0, size, entryComparator);
         modCount++;
      } else {
         super.putAll(map);
      }
   }

   private int findPosition(K key) {
      return Arrays.binarySearch(data, 0, size, new EntryImpl<K, V>(key, null), entryComparator);
   }
   
   @Override
   public Entry<K, V> lowerEntry(K key) {
      int pos = findPosition(key);
      if (pos < 0) {
         pos = -(pos + 1);
      }
      pos--; // lower means the one before
      return pos < 0 ? null : data[pos];
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
      int pos = findPosition(key);
      if (pos < 0) {
         // if key isn't in the map, then the max item that is less than the key is the one
         // *before* the insert position for the key;
         pos = -(pos + 1) - 1;
      }
      return pos < 0 ? null : data[pos];
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
      int pos = findPosition(key);
      if (pos < 0) {
         // if key isn't in the map, then the min item that is greater than the key is the one
         // *at* the insert position for the key;
         pos = -(pos + 1);
      }
      return pos >= size ? null : data[pos];
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
      int pos = findPosition(key);
      if (pos < 0) {
         pos = -(pos + 1) - 1;
      }
      pos++; // higher means the one after
      return pos >= size ? null : data[pos];
   }

   @Override
   public Entry<K, V> firstEntry() {
      return data[0];
   }

   @Override
   public Entry<K, V> lastEntry() {
      return size == 0 ? null : data[size - 1];
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public V put(K key, V value) {
      int pos = findPosition(key);
      if (pos < 0) {
         maybeGrowBy(1);
         data = ArrayUtils.insertItem(new EntryImpl<K, V>(key, value), -(pos + 1), data, size);
         size++;
         modCount++;
         return null;
      } else {
         EntryImpl<K, V> entry = data[pos];
         V ret = entry.getValue();
         data[pos].setValue(value);
         return ret;
      }
   }

   @Override
   protected Entry<K, V> getEntry(Object key) {
      @SuppressWarnings("unchecked")
      int pos = findPosition((K) key);
      return pos < 0 ? null : data[pos];
   }

   @Override
   protected Entry<K, V> removeEntry(Object key) {
      @SuppressWarnings("unchecked")
      int pos = findPosition((K) key);
      if (pos < 0) {
         return null;
      }
      Entry<K, V> ret = data[pos];
      ArrayUtils.removeIndex(pos, data, size);
      size--;
      modCount++;
      return ret;
   }
   
   /**
    * Customizes de-serialization to read the set of mappings the same way as written by
    * {@link #writeObject(ObjectOutputStream)}.
    * 
    * @param in the stream from which the map is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      // TODO: check that keys are in ascending order as they are read and, if not, sort the array
      // at the end
      data = new EntryImpl[Math.max(size, 1)];
      for (int i = 0; i < size; i++) {
         K key = (K) in.readObject();
         V val = (V) in.readObject();
         data[i] = new EntryImpl<K, V>(key, val);
      }
      entryComparator = entryComparator(comparator);
   }
   
   /**
    * Customizes serialization by just writing the mappings, key then value, in order.
    * 
    * @param out the stream to which to serialize this list
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      for (int i = 0; i < size; i++) {
         Entry<K, V> entry = data[i];
         out.writeObject(entry.getKey());
         out.writeObject(entry.getValue());
      }
   }
}
