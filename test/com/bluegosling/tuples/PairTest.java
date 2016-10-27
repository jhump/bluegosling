package com.bluegosling.tuples;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Test cases for {@link Pair}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class PairTest {
   
   private final Pair<String, Integer> p = Pair.of("a", 1);
   
   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(p.contains(null));
      assertFalse(p.contains("b"));
      assertFalse(p.contains(new Object()));
      assertTrue(p.contains("a"));
      assertTrue(p.contains(1));
      assertTrue(Pair.of("a", null).contains(null));
      assertTrue(Pair.of(null, "a").contains(null));
   }
   
   @Test public void isEmpty() {
      assertFalse(p.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(2, p.size());
   }
   
   @Test public void iterator() {
      Iterator<Object> iter = p.iterator();
      assertTrue(iter.hasNext());
      assertEquals("a", iter.next());
      assertTrue(iter.hasNext());
      assertEquals(1, iter.next());
      assertFalse(iter.hasNext());
   }
   
   @Test public void asList() {
      List<?> list = p.asList();
      assertEquals(2, list.size());
      assertEquals("a", list.get(0));
      assertEquals(1, list.get(1));
      assertEquals(Arrays.<Object>asList("a", 1), list);
   }
   
   @Test public void toArray() {
      assertArrayEquals(new Object[] { "a", 1 }, p.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(p.equals(p));
      assertTrue(p.equals(Pair.of("a", 1)));
      assertTrue(p.equals(Triple.of("a", "b", 1).removeSecond()));
      assertTrue(p.equals(Single.of(1).insertFirst("a")));
      assertFalse(p.equals(Empty.INSTANCE));
      assertFalse(p.equals(Single.of("a")));
      assertFalse(p.equals(Pair.of("a", "b")));
      assertFalse(p.equals(Arrays.<Object>asList("a", 1)));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(Arrays.<Object>asList("a", 1).hashCode(), p.hashCode());
   }
   
   // accessors / mutators

   @Test public void get() {
      assertEquals("a", p.getFirst());
      assertEquals((Integer) 1, p.getSecond());
   }
   
   @Test public void addAndInsert() {
      Triple<String, Integer, Double> t = p.add(42.0);
      assertEquals(Triple.of("a", 1, 42.0), t);
      assertEquals(t, p.insertThird(42.0));
      assertEquals(Triple.of(42.0, "a", 1), p.insertFirst(42.0));
      assertEquals(Triple.of("a", 42.0, 1), p.insertSecond(42.0));
   }
   
   @Test public void remove() {
      assertEquals(Single.of(1), p.removeFirst());
      assertEquals(Single.of("a"), p.removeSecond());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Pair.of("abcdefg", "abcdefg"), p.transformAll(f));
      assertEquals(Pair.of("abcdefg", 1), p.transformFirst(f));
      assertEquals(Pair.of("a", "abcdefg"), p.transformSecond(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(p);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Pair<?, ?> deserialized = (Pair<?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(p, deserialized);
   }

   // separate, combine
   
   @Test public void separate() {
      assertEquals(Pair.of(Collections.emptyList(), Collections.emptyList()),
            Pair.separate(Collections.<Pair<Integer, String>>emptyList()));

      List<Pair<Integer, String>> pairs =
            Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"));
      assertEquals(Pair.of(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c")),
            Pair.separate(pairs));
   }

   private <A, B> void assertCombinedEquals(List<Pair<A, B>> expectedPairs,
         Collection<? extends A> a, Collection<? extends B> b) {
      assertEquals(expectedPairs, Pair.combine(a, b));
      assertEquals(expectedPairs, Pair.combine(Pair.of(a, b)));
   }
   
   @Test public void combine() {
      assertCombinedEquals(Collections.<Pair<Integer, String>>emptyList(),
            Collections.<Integer>emptyList(), Collections.<String>emptyList());

      assertCombinedEquals(
            Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c")),
            Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"));
   }

   private void assertCombineFails(Collection<?> a, Collection<?> b) {
      try {
         Pair.combine(a, b);
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }

      try {
         Pair.combine(Pair.of(a, b));
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }
}

   @Test public void combine_unequalSizes() {
      assertCombineFails(Collections.emptySet(), Collections.singleton("abc"));
      assertCombineFails(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList("a", "b", "c"));
   }
}
