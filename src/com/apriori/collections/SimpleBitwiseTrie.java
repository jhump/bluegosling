package com.apriori.collections;

import com.apriori.collections.BitSequence.BitOrder;

import java.util.Comparator;

/**
 * A trie structure that most closely resembles a binary tree. Each node in the trie has only two
 * children. Unlike in a binary search tree, nodes do not have keys and left and right children do
 * not indicate order relative to the node's key. Instead, keys are decomposed into a sequence of
 * bits, and each node represents a single bit in the sequence. Left and right child nodes represent
 * the next bit in the sequence, either 0 or 1.
 * 
 * <p>This class requires calling code to supply a {@link BitConverter} to decompose a key object
 * into its constituent sequence of bits. For purposes of ordering, the first bit in the sequence is
 * considered to be the most significant bit. A {@link Comparator} can also be provided to break
 * ties in the event that two different key objects produce the same sequence of bits. However, this
 * is discouraged since it can induce linear runtime complexity in methods that usually are constant
 * time. Ideally, all objects result in distinct bit sequences. The {@link #comparator()} method
 * does not return the supplied comparator. Instead it returns a comparator that would order objects
 * in the actual way they are ordered in this structure. The returned comparator first compares the
 * bit sequences for each key and then uses the supplied comparator only to break ties when two keys
 * result in the same bit sequence. 
 * 
 * <p><a name="xor-metric">In addition to the standard {@linkplain CompositeTrie trie} interface,
 * this class provides methods for searching for the nearest key using an XOR-distance metric.</a>
 * These methods are {@link #nearestEntry(Object)} and {@link #nearestKey(Object)}. The XOR-distance
 * is, simply, the result of the bitwise-XOR operation between the bits of two keys. The first bit
 * in the sequence is the most significant and the last bit of the longer sequence (in the event the
 * keys are different lengths) is the least significant. When keys are different lengths, the bits
 * not present in the shorter key are considered to be zero (so minimizing XOR-distance means
 * preferring paths with unset bits).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of key in the map
 * @param <V> the type of value in the map
 */
//TODO: javadoc
//TODO: implement me and remove abstract modifier (don't forget serialization and cloning)
public abstract class SimpleBitwiseTrie<K, V> implements NavigableCompositeTrie<K, Boolean, V> {
   
   static final Comparator<Boolean> COMPONENT_COMPARATOR = new Comparator<Boolean>() {
      @Override public int compare(Boolean b1, Boolean b2) {
         boolean bool1 = b1;
         boolean bool2 = b2;
         if (bool1 == bool2) {
            return 0;
         } else if (bool1) {
            return 1;
         } else {
            return -1;
         }
      }
   };
   
   static class ValueNode<K, V> {
      K key;
      V value;
      ValueNode<K, V> next;
      
      ValueNode(K key, V value) {
         this.key = key;
         this.value = value;
      }
   }
   
   public K nearestKey(K key) {
      // TODO
      return null;
   }

   public Entry<K, V> nearestEntry(K key) {
      // TODO
      return null;
   }

   static class WrappedComparator<K> implements Comparator<K> {
      private final BitConverter<? super K> bitConverter;
      private final Comparator<? super K> tieBreaker;
      
      WrappedComparator(BitConverter<? super K> bitConverter, Comparator<? super K> tieBreaker) {
         this.bitConverter = bitConverter;
         this.tieBreaker = tieBreaker;
      }
      
      @Override public int compare(K o1, K o2) {
         BitSequence bs1 = bitConverter.getComponents(o1);
         BitSequence bs2 = bitConverter.getComponents(o2);
         BitStream stream1 = bs1.bitStream();
         BitStream stream2 = bs2.bitStream();
         while (stream1.remaining() > 0 && stream2.remaining() > 0) {
            int len1 = stream1.remaining();
            int len2 = stream2.remaining();
            int len = len1 < len2 ? len1 : len2;
            if (len > 64) {
               len = 64;
            }
            long l1 = stream1.next(len, BitOrder.MSB);
            long l2 = stream2.next(len, BitOrder.MSB);
            if (l1 < l2) {
               return -1;
            } else if (l2 < l1) {
               return 1;
            }
         }
         if (stream1.remaining() > 0) {
            return 1;
         } else if (stream2.remaining() > 0) {
            return -1;
         }
         // resolve ties with the specified comparator
         return tieBreaker.compare(o1, o2);
      }
      
      @Override public boolean equals(Object o) {
         if (o instanceof WrappedComparator) {
            WrappedComparator<?> other = (WrappedComparator<?>) o;
            return (tieBreaker == null ? other.tieBreaker == null
                  : tieBreaker.equals(other.tieBreaker))
                  && bitConverter.equals(other.bitConverter);
         }
         return false;
      }
      
      @Override public int hashCode() {
         return (tieBreaker == null ? 0 : tieBreaker.hashCode()) ^ bitConverter.hashCode();
      }
   }
}
