package com.apriori.collections;

import com.apriori.collections.BitSequence.BitOrder;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

//TODO: javadoc
//TODO: implement me (don't forget serialization and cloning)
public class ArrayMappedStringTrie<V> extends ArrayMappedBitwiseTrie<CharSequence, V> {
   
   // TODO: extract class and make it serializable
   private static BitConverter<CharSequence> bitConverterFromCollator(final Collator collator) {
      return new BitConverter<CharSequence>() {
         @Override public BitSequence getComponents(CharSequence key) {
            return BitSequences.fromBytes(collator.getCollationKey(key.toString()).toByteArray(),
                  BitOrder.MSB);
         }
      };
   }
   
   // TODO: extract class and make it serializable
   private static Comparator<CharSequence> comparatorFromCollator(final Collator collator) {
      return new Comparator<CharSequence>() {
         @Override public int compare(CharSequence o1, CharSequence o2) {
            return collator.compare(o1.toString(), o2.toString());
         }
      };
   }
   
   private Collator collator;
   
   public ArrayMappedStringTrie() {
      this(Collator.getInstance());
   }
   
   public ArrayMappedStringTrie(Locale locale) {
      this(Collator.getInstance(locale));
   }
   
   public ArrayMappedStringTrie(Collator collator) {
      super(bitConverterFromCollator(collator), comparatorFromCollator(collator));
      this.collator = collator;
   }
   
   public ArrayMappedStringTrie(Map<? extends CharSequence, ? extends V> map) {
      this();
      putAll(map);
   }
   
   public ArrayMappedStringTrie(Locale locale, Map<? extends CharSequence, ? extends V> map) {
      this(locale);
      putAll(map);
   }
   
   public ArrayMappedStringTrie(Collator collator, Map<? extends CharSequence, ? extends V> map) {
      this(collator);
      putAll(map);
   }
   
   public Collator collator() {
      return collator;
   }
   
   @Override public Comparator<? super CharSequence> comparator() {
      return comparatorFromCollator(collator);
   }
}
