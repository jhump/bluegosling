package com.bluegosling.collections.tries;

import com.bluegosling.collections.primitive.CharIterator;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A trie that has {@link String} keys. This is nearly the same as using a
 * {@link SortedArrayCompositeTrie SortedArrayCompositeTrie&lt;CharSequence, Character, V&gt;} but
 * has a couple of useful advantages:
 * <ul>
 * <li>This trie supports proper localized sorting of keys via a {@link Collator}.</li>
 * <li>This trie is a little more efficient since it uses arrays of primitives ({@code char})
 * instead of the boxed counterpart, {@link Character}.
 * </ul>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value stored in the trie
 * 
 * @see CompactStringTrie
 * @see ArrayMappedStringTrie
 */
//TODO: javadoc
//TODO: tests
//TODO: implement me and remove abstract modifier (don't forget serialization and cloning)
public abstract class SimpleStringTrie<V>
      implements NavigableCompositeTrie<CharSequence, Character, V> {

   static final Componentizer<CharSequence, Character> COMPONENTIZER =
         key -> () -> new CharIterator() {
            private int index = 0;
            
            @Override public boolean hasNext() {
               return index < key.length();
            }

            @Override public char nextChar() {
               if (index >= key.length()) {
                  throw new NoSuchElementException();
               }
               return key.charAt(index++);
            }
         };
         
   private Collator collator;
   
   public SimpleStringTrie() {
      this(Collator.getInstance());
   }
   
   public SimpleStringTrie(Locale locale) {
      this(Collator.getInstance(locale));
   }
   
   public SimpleStringTrie(Collator collator) {
      this.collator = collator;
   }
   
   public SimpleStringTrie(Map<? extends CharSequence, ? extends V> map) {
      this();
      putAll(map);
   }
   
   public SimpleStringTrie(Locale locale, Map<? extends CharSequence, ? extends V> map) {
      this(locale);
      putAll(map);
   }
   
   public SimpleStringTrie(Collator collator, Map<? extends CharSequence, ? extends V> map) {
      this(collator);
      putAll(map);
   }
   
   public Collator collator() {
      return collator;
   }
   
   @Override public Comparator<CharSequence> comparator() {
      return collator::compare;
   }
   
   @Override public Componentizer<CharSequence, Character> componentizer() {
      return COMPONENTIZER;
   }
   
}
