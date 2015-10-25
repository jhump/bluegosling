package com.apriori.collections;

import static com.apriori.collections.SimpleBitwiseTrie.COMPONENT_COMPARATOR;

import com.apriori.collections.SimpleBitwiseTrie.ValueNode;
import com.apriori.collections.SimpleBitwiseTrie.WrappedComparator;

import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

//TODO: javadoc
//TODO: implement me (don't forget serialization and cloning)
public class CompactBitwiseTrie<K, V> implements NavigableCompositeTrie<K, Boolean, V> {

   private static class TrieNode<K, V> {
      ValueNode<K, V> value;
      TrieNodeEdge<K, V> s0;
      TrieNodeEdge<K, V> s1;
      
      TrieNode() {}
   }
   
   private static class TrieNodeEdge<K, V> {
      BitSequence prefix;
      TrieNode<K, V> node;
      
      TrieNodeEdge() {}
   }
   
   private final BitConverter<? super K> bitConverter;
   private final Comparator<? super K> comparator;
   TrieNode<K, V> root = new TrieNode<K, V>();
   int modCount;
   int size;
   
   public CompactBitwiseTrie(BitConverter<? super K> bitConverter) {
      this(bitConverter, (Comparator<? super K>) null);
   }

   public CompactBitwiseTrie(BitConverter<? super K> bitConverter,
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

   public CompactBitwiseTrie(BitConverter<? super K> bitConverter,
         Map<? extends K, ? extends V> map) {
      this(bitConverter);
      putAll(map);
   }
   
   public CompactBitwiseTrie(BitConverter<? super K> bitConverter,
         Comparator<? super K> comparator, Map<? extends K, ? extends V> map) {
      this(bitConverter, comparator);
      putAll(map);
   }
   
   @Override public BitConverter<? super K> componentizer() {
      return bitConverter;
   }
   
   private ValueNode<K, V> firstNode(TrieNode<K, V> node) {
      // Go down lowest (left-most) path until we encounter a value.
      while (node != null && node.value != null) {
         node = node.s0 != null ? node.s0.node : node.s1.node;
      }
      // Return first value node in the linked list.
      return node != null ? node.value : null;
   }

   private ValueNode<K, V> lastNode(TrieNode<K, V> node) {
      // Descend down highest (right-most) path to a leaf.
      while (true) {
         if (node.s1 != null) {
            node = node.s1.node;
         } else if (node.s0 != null) {
            node = node.s0.node;
         } else {
            break;
         }
      }
      // Return the last value node in the linked list.
      ValueNode<K, V> result = node.value;
      while (result.next != null) {
         result = result.next;
      }
      return result;
   }
   
   BitStream stream(K key) {
      return bitConverter.getComponents(key).bitStream();
   }
   
   private TrieNodeEdge<K, V> split(ValueNode<K, V> newValue, BitStream stream,
         TrieNodeEdge<K, V> existingEdge) {
      int prefixLen = 0;
      BitStream prefixStream = existingEdge.prefix.bitStream();
      boolean streamBit = false;
      boolean lastBitDifferent = false;
      while (stream.remaining() > 0 && prefixStream.remaining() > 0) {
         streamBit = stream.next();
         if (streamBit == prefixStream.next()) {
            prefixLen++;
         } else {
            lastBitDifferent = true;
            break;
         }
      }
      if (!lastBitDifferent && prefixStream.remaining() == 0) {
         // prefix has matched fully, no split needed
         return null;
      }
      // split existing edge
      TrieNodeEdge<K, V> newEdge = new TrieNodeEdge<K, V>();
      if (prefixLen > 0) {
         newEdge.prefix = BitSequences.subSequence(existingEdge.prefix, 0, prefixLen);
      }
      if (lastBitDifferent) {
         // Splits the existing prefix into two and adds a branch for the supplied stream:
         // I.e.
         //                |                                | (cp)
         // turns this:    |            into this:          |
         //                | (ep)                          / \ 
         //                |                         (ss) /   \ (es)
         //                |                             /     \
         //                * (en)                  (sn) *       * (en)
         // where:
         //  ep === prefix for existing edge (gets split into cp and es)
         //  en === node for existing edge
         //  cp === common prefix, shared by existing prefix and supplied stream
         //  es === existing suffix (existing prefix with common prefix chopped off the top)
         //  ss === supplied suffix (supplied stream with common prefix chopped off the top)
         //  sn === supplied node
         TrieNode<K, V> newNode = new TrieNode<K, V>();
         TrieNodeEdge<K, V> streamEdge = new TrieNodeEdge<K, V>();
         if (stream.remaining() > 0) {
            streamEdge.prefix = stream.nextAsSequence(stream.remaining());
         }
         streamEdge.node = new TrieNode<K, V>();
         streamEdge.node.value = newValue;
         TrieNodeEdge<K, V> otherEdge = new TrieNodeEdge<K, V>();
         if (prefixStream.remaining() > 0) {
            otherEdge.prefix = prefixStream.nextAsSequence(prefixStream.remaining());
         }
         otherEdge.node = existingEdge.node;
         if (streamBit) {
            newNode.s1 = streamEdge;
            newNode.s0 = otherEdge;
         } else {
            newNode.s0 = streamEdge;
            newNode.s1 = otherEdge;
         }
         newEdge.node = newNode;
      } else {
         // Stream matched part of existing prefix. So we split the existing prefix into two and
         // place new value at the midpoint
         // I.e.
         //                |                                | (cp)
         // turns this:    |            into this:          |
         //                | (ep)                           * (sn) 
         //                |                                |
         //                |                                | (es)
         //                |                                |
         //                * (en)                           * (en)
         // where:
         //  ep === prefix for existing edge (gets split into ss and es)
         //  en === node for existing edge
         //  cp === common prefix (same as supplied stream)
         //  sn === supplied node
         //  es === existing suffix (existing prefix with common prefix chopped off the top)
         TrieNode<K, V> newNode = new TrieNode<K, V>();
         newNode.value = newValue;
         TrieNodeEdge<K, V> prefixRemainder = new TrieNodeEdge<K, V>();
         prefixRemainder.node = existingEdge.node;
         if (prefixStream.next()) {
            newNode.s1 = prefixRemainder;
         } else {
            newNode.s0 = prefixRemainder;
         }
         if (prefixStream.remaining() > 0) {
            prefixRemainder.prefix = prefixStream.nextAsSequence(prefixStream.remaining());
         }
         newEdge.node = newNode;
      }
      return newEdge;
   }
   
   private TrieNodeEdge<K, V> consolidate(TrieNodeEdge<K, V> existingEdge) {
      if (existingEdge.node.value == null) {
         if (existingEdge.node.s0 == null && existingEdge.node.s1 == null) {
            // no outgoing edge needed
            return null;
         } else if (existingEdge.node.s1 == null) {
            TrieNodeEdge<K, V> newEdge = new TrieNodeEdge<K, V>();
            newEdge.prefix = BitSequences.concat(existingEdge.prefix, BitSequences.singleton(false),
                  existingEdge.node.s0.prefix);
            newEdge.node = existingEdge.node.s0.node;
            return newEdge;
         } else if (existingEdge.node.s0 == null) {
            TrieNodeEdge<K, V> newEdge = new TrieNodeEdge<K, V>();
            newEdge.prefix = BitSequences.concat(existingEdge.prefix, BitSequences.singleton(true),
                  existingEdge.node.s1.prefix);
            newEdge.node = existingEdge.node.s0.node;
            return newEdge;
         }
      }
      return existingEdge; // no change
   }
   
   private ValueNode<K, V> insertNode(ValueNode<K, V> newValue, BitStream stream,
         TrieNode<K, V> node) {
      if (stream.remaining() > 0) {
         if (stream.next()) {
            // bit is set
            if (node.s1 != null) {
               if (node.s1.prefix != null) {
                  TrieNodeEdge<K, V> newEdge = split(newValue, stream, node.s1);
                  if (newEdge != null) {
                     node.s1 = newEdge;
                     return null;
                  }
               }
               return insertNode(newValue, stream, node.s1.node);
            } else {
               TrieNode<K, V> newNode = new TrieNode<K, V>();
               newNode.value = newValue;
               TrieNodeEdge<K, V> newEdge = new TrieNodeEdge<K, V>();
               if (stream.remaining() > 0) {
                  newEdge.prefix = stream.nextAsSequence(stream.remaining());
               }
               newEdge.node = newNode;
               node.s1 = newEdge;
            }
         } else {
            // bit is clear
            if (node.s0 != null) {
               if (node.s0.prefix != null) {
                  TrieNodeEdge<K, V> newEdge = split(newValue, stream, node.s0);
                  if (newEdge != null) {
                     node.s0 = newEdge;
                     return null; // no prior node
                  }
               }
               return insertNode(newValue, stream, node.s0.node);
            } else {
               TrieNode<K, V> newNode = new TrieNode<K, V>();
               newNode.value = newValue;
               TrieNodeEdge<K, V> newEdge = new TrieNodeEdge<K, V>();
               if (stream.remaining() > 0) {
                  newEdge.prefix = stream.nextAsSequence(stream.remaining());
               }
               newEdge.node = newNode;
               node.s0 = newEdge;
            }
         }
      } else if (node.value == null) {
         node.value = newValue;
         return null; // no prior node
      }
      // insert the value into the correct position in the list
      int c = comparator.compare(newValue.key, node.value.key);
      if (c < 0) {
         ValueNode<K, V> tmp = node.value;
         node.value = newValue;
         newValue.next = tmp;
         return null;
      } else if (c == 0) {
         ValueNode<K, V> ret = node.value;
         node.value = newValue; // replace
         newValue.next = ret.next;
         ret.next = null;
         return ret;
      } else {
         ValueNode<K, V> v = node.value;
         while (v.next != null) {
            c = comparator.compare(newValue.key, v.next.key);
            if (c < 0) {
               ValueNode<K, V> tmp = v.next;
               v.next = newValue;
               v.next.next = tmp;
               return null;
            } else if (c == 0) {
               ValueNode<K, V> ret = v.next;
               v.next = newValue; // replace
               newValue.next = ret.next;
               ret.next = null;
               return ret;
            }
            v = v.next;
         }
         if (v.next == null) {
            v.next = newValue;
         }
      }
      return null;
   }

   private ValueNode<K, V> removeNode(K key, BitStream stream, TrieNode<K, V> node) {
      if (stream.remaining() > 0) {
         if (stream.next()) {
            if (node.s1 != null
                  && stream.nextAsSequence(node.s1.prefix.length()).equals(node.s1.prefix)) {
               ValueNode<K, V> removed = removeNode(key, stream, node.s1.node);
               if (removed != null) {
                  node.s1 = consolidate(node.s1);
               }
               return removed;
            }
         } else {
            if (node.s0 != null
                  && stream.nextAsSequence(node.s0.prefix.length()).equals(node.s0.prefix)) {
               ValueNode<K, V> removed = removeNode(key, stream, node.s0.node);
               if (removed != null) {
                  node.s0 = consolidate(node.s0);
               }
               return removed;
            }
         }
      } else if (node.value != null) {
         // search for value in the list
         int c = comparator.compare(key, node.value.key);
         if (c < 0) {
            return null; // not here
         } else if (c == 0) {
            // found it
            ValueNode<K, V> ret = node.value;
            node.value = node.value.next;
            ret.next = null;
            return ret;
         } else if (c > 0) {
            ValueNode<K, V> v = node.value;
            while (v.next != null) {
               c = comparator.compare(key, v.next.key);
               if (c < 0) {
                  return null; // not here
               } else if (c == 0) {
                  // found it
                  ValueNode<K, V> ret = v.next;
                  v.next = v.next.next;
                  ret.next = null;
                  return ret;
               }
               v = v.next;
            }
         }
      }
      return null;
   }
   
   ValueNode<K, V> findNode(K key, BitStream stream, boolean acceptNearest,
         TrieNode<K, V> node) {
      if (stream.remaining() > 0) {
         if (stream.next()) {
            // bit is set
            if (node.s1 != null) {
               if (node.s1.prefix != null) {
                  if ((node.s1.prefix.length() > stream.remaining()
                        || !stream.nextAsSequence(node.s1.prefix.length()).equals(node.s1.prefix))
                        && !acceptNearest) {
                     return null; // prefix on this edge doesn't match the stream
                  }
               }
               return findNode(key, stream, acceptNearest, node.s1.node);
            } else if (acceptNearest) {
               if (node.s0 != null) {
                  if (node.s0.prefix != null) {
                     // consume the bits that would correspond to edge's prefix bits
                     int newIndex = node.s0.prefix.length() > stream.remaining()
                           ? stream.currentIndex() + stream.remaining()
                           : stream.currentIndex() + node.s0.prefix.length();
                     stream.jumpTo(newIndex);
                  }
                  return findNode(key, stream, acceptNearest, node.s0.node);
               } else {
                  return node.value; // this is the closest we can find
               }
            } else {
               return null;
            }
         } else {
           // bit is clear (symmetric with above)
            if (node.s0 != null) {
               if (node.s0.prefix != null) {
                  if ((node.s0.prefix.length() > stream.remaining()
                        || !stream.nextAsSequence(node.s0.prefix.length()).equals(node.s0.prefix))
                        && !acceptNearest) {
                     return null; // prefix on this edge doesn't match the stream
                  }
               }
               return findNode(key, stream, acceptNearest, node.s0.node);
            } else if (acceptNearest) {
               if (node.s1 != null) {
                  if (node.s1.prefix != null) {
                     // consume the bits that would correspond to edge's prefix bits
                     int newIndex = node.s1.prefix.length() > stream.remaining()
                           ? stream.currentIndex() + stream.remaining()
                           : stream.currentIndex() + node.s1.prefix.length();
                     stream.jumpTo(newIndex);
                  }
                  return findNode(key, stream, acceptNearest, node.s1.node);
               } else {
                  return node.value; // this is the closest we can find
               }
            } else {
               return null;
            }
         }
      } else if (node.value != null) {
         return node.value;
      } else if (acceptNearest) {
         // if value on this node is null then we must have a child (in fact, it should actually
         // have both children, but the root of the trie is an exception that may have only one)
         TrieNodeEdge<K, V> edgeToTraverse = node.s0 == null ? node.s1 : node.s0;
         if (edgeToTraverse.prefix != null) {
            // consume the bits that would correspond to edge's prefix bits
            int newIndex = edgeToTraverse.prefix.length() > stream.remaining()
                  ? stream.currentIndex() + stream.remaining()
                  : stream.currentIndex() + edgeToTraverse.prefix.length();
            stream.jumpTo(newIndex);
         }
         return findNode(key, stream, acceptNearest, node.s0 == null ? node.s1.node : node.s0.node);
      } else {
         return null;
      }
   }
   
   private ValueNode<K, V> findNode(Object key) {
      // this won't pollute heap but may result in ClassCastException, which is okay since that is
      // doc'ed as possible thrown exception by all methods that call this
      @SuppressWarnings("unchecked")
      K castKey = (K) key;
      return findNode(castKey, stream(castKey), false, root);
   }
   
   private Entry<K, V> mapEntry(final ValueNode<K, V> value) {
      return new EntryImpl(value);
   }

   public K nearestKey(K key) {
      ValueNode<K, V> nearest = findNode(key, bitConverter.getComponents(key).bitStream(), true, root);
      return nearest == null ? null : nearest.key;
   }

   public Entry<K, V> nearestEntry(K key) {
      ValueNode<K, V> nearest = findNode(key, bitConverter.getComponents(key).bitStream(), true, root);
      return nearest == null ? null : mapEntry(nearest);
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean isEmpty() {
      return size == 0;
   }
   
   @Override
   public boolean containsKey(Object key) {
      return findNode(key) != null;
   }
   
   private boolean findValue(Object value, TrieNode<K, V> node) {
      for (ValueNode<K, V> valueNode = node.value; valueNode != null; valueNode = valueNode.next) {
         Object v = valueNode.value;
         if (value == null ? v == null : value.equals(v)) {
            return true;
         }
      }
      if (node.s0 != null && findValue(value, node.s0.node)) {
         return true;
      }
      if (node.s1 != null && findValue(value, node.s1.node)) {
         return true;
      }
      return false;
   }

   @Override
   public boolean containsValue(Object value) {
      return findValue(value, root);
   }

   @Override
   public V get(Object key) {
      ValueNode<K, V> node = findNode(key);
      return node == null ? null : node.value;
   }

   @Override
   public V put(K key, V value) {
      ValueNode<K, V> priorNode = insertNode(new ValueNode<K, V>(key, value), stream(key), root);
      if (priorNode != null) {
         modCount++;
         size++;
         return priorNode.value;
      }
      return null;
   }

   @Override
   public V remove(Object key) {
      // this won't pollute heap but may result in ClassCastException, which is okay since that is
      // doc'ed as possible thrown exception by Map#remove(Object)
      @SuppressWarnings("unchecked")
      K castKey = (K) key;
      ValueNode<K, V> priorNode = removeNode(castKey, stream(castKey), root);
      if (priorNode != null) {
         modCount++;
         size--;
         return priorNode.value;
      }
      return null;
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
         put(entry.getKey(), entry.getValue());
      }
   }

   @Override public void clear() {
      size = 0;
      root = new TrieNode<K, V>();
      modCount++;
   }

   @Override public Set<K> keySet() {
      return navigableKeySet();
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
      Entry<K, V> entry = lowerEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> floorEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K floorKey(K key) {
      Entry<K, V> entry = floorEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> ceilingEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K ceilingKey(K key) {
      Entry<K, V> entry = ceilingEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> higherEntry(K key) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public K higherKey(K key) {
      Entry<K, V> entry = higherEntry(key);
      return entry == null ? null : entry.getKey();
   }

   @Override
   public Entry<K, V> firstEntry() {
      ValueNode<K, V> valueNode = firstNode(root);
      return valueNode == null ? null : mapEntry(valueNode);
   }

   @Override
   public Entry<K, V> lastEntry() {
      ValueNode<K, V> valueNode = lastNode(root);
      return valueNode == null ? null : mapEntry(valueNode);
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
      return navigableKeySet().descendingSet();
   }
   
   @Override public Comparator<Boolean> componentComparator() {
      return COMPONENT_COMPARATOR;
   }

   @Override public Comparator<? super K> comparator() {
      return new WrappedComparator<K>(bitConverter,
            comparator == Comparator.naturalOrder() ? null : comparator);
   }

   @Override
   public K firstKey() {
      ValueNode<K, V> valueNode = firstNode(root);
      return valueNode == null ? null : valueNode.key;
   }

   @Override
   public K lastKey() {
      ValueNode<K, V> valueNode = lastNode(root);
      return valueNode == null ? null : valueNode.key;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMapByKey(K prefix) {
      return prefixMap(bitConverter.getComponents(prefix));
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMapByKey(K prefix, int numComponents) {
      return prefixMap(bitConverter.getComponents(prefix), numComponents);
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(Boolean prefix) {
      return prefixMap(BitSequences.singleton(prefix));
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(Iterable<Boolean> prefix) {
      return prefixMap(BitSequences.fromIterator(prefix.iterator()));
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(Iterable<Boolean> prefix,
         int numComponents) {
      return prefixMap(BitSequences.fromIterator(prefix.iterator()), numComponents);
   }
   
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(BitSequence prefix) {
      return prefixMap(prefix, prefix.length());
   }
   
   public NavigableCompositeTrie<K, Boolean, V> prefixMap(BitSequence prefix, int numBits) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> descendingMap() {
      return new DescendingCompositeTrie<K, Boolean, V>(this);
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
      return subMap(fromKey, true, toKey, false);
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   @Override
   public NavigableCompositeTrie<K, Boolean, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
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
   
   private class EntryImpl implements Map.Entry<K, V> {
      private final K key;
      private V value;
      private ValueNode<K, V> valueNode;
      private int myModCount;
      
      EntryImpl(ValueNode<K, V> valueNode) {
         this.valueNode = valueNode;
         this.key = valueNode.key;
         this.value = valueNode.value;
         this.myModCount = modCount;
      }
      
      private boolean checkNode() {
         if (myModCount == modCount) {
            return true;
         }
         valueNode = findNode(key, stream(key), false, root);
         if (valueNode != null) {
            myModCount = modCount;
            return true;
         }
         return false;
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
         if (checkNode()) {
            throw new ConcurrentModificationException("entry no longer exists in map");
         }
         V ret = this.value;
         this.value = valueNode.value = value;
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
   
   class IteratorImpl implements Iterator<ValueNode<K, V>> {

      @Override
      public boolean hasNext() {
         // TODO Auto-generated method stub
         return false;
      }

      @Override
      public ValueNode<K, V> next() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public void remove() {
         // TODO Auto-generated method stub
         
      }
   }
}
