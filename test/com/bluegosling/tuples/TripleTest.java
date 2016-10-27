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
 * Test cases for {@link Triple}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TripleTest {

   private final Triple<String, Integer, Double> t = Triple.of("a", 1, 42.0);

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(t.contains(null));
      assertFalse(t.contains("b"));
      assertFalse(t.contains(new Object()));
      assertTrue(t.contains("a"));
      assertTrue(t.contains(1));
      assertTrue(t.contains(42.0));
      assertTrue(Triple.of("a", 1, null).contains(null));
      assertTrue(Triple.of("a", null, 1).contains(null));
      assertTrue(Triple.of(null, "a", 1).contains(null));
   }
   
   @Test public void isEmpty() {
      assertFalse(t.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(3, t.size());
   }
   
   @Test public void iterator() {
      Iterator<Object> iter = t.iterator();
      assertTrue(iter.hasNext());
      assertEquals("a", iter.next());
      assertTrue(iter.hasNext());
      assertEquals(1, iter.next());
      assertTrue(iter.hasNext());
      assertEquals(42.0, iter.next());
      assertFalse(iter.hasNext());
   }
   
   @Test public void asList() {
      List<?> list = t.asList();
      assertEquals(3, list.size());
      assertEquals("a", list.get(0));
      assertEquals(1, list.get(1));
      assertEquals(42.0, list.get(2));
      assertEquals(Arrays.<Object>asList("a", 1, 42.0), list);
   }
   
   @Test public void toArray() {
      assertArrayEquals(new Object[] { "a", 1, 42.0 }, t.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(t.equals(t));
      assertTrue(t.equals(Triple.of("a", 1, 42.0)));
      assertTrue(t.equals(Quadruple.of("XYZ", "a", 1, 42.0).removeFirst()));
      assertTrue(t.equals(Pair.of(1, 42.0).insertFirst("a")));
      assertFalse(t.equals(Empty.INSTANCE));
      assertFalse(t.equals(Single.of("a")));
      assertFalse(t.equals(Pair.of("a", 1)));
      assertFalse(t.equals(Triple.of("a", 1, 43.43)));
      assertFalse(t.equals(Arrays.<Object>asList("a", 1, 42.0)));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(Arrays.<Object>asList("a", 1, 42.0).hashCode(), t.hashCode());
   }
   
   // accessors / mutators

   @Test public void get() {
      assertEquals("a", t.getFirst());
      assertEquals((Integer) 1, t.getSecond());
      assertEquals((Double) 42.0, t.getThird());
   }
   
   @Test public void addAndInsert() {
      Quadruple<String, Integer, Double, String> q = t.add("foobar");
      assertEquals(Quadruple.of("a", 1, 42.0, "foobar"), q);
      assertEquals(q, t.insertFourth("foobar"));
      assertEquals(Quadruple.of("foobar", "a", 1, 42.0), t.insertFirst("foobar"));
      assertEquals(Quadruple.of("a", "foobar", 1, 42.0), t.insertSecond("foobar"));
      assertEquals(Quadruple.of("a", 1, "foobar", 42.0), t.insertThird("foobar"));
   }
   
   @Test public void remove() {
      assertEquals(Pair.of(1, 42.0), t.removeFirst());
      assertEquals(Pair.of("a", 42.0), t.removeSecond());
      assertEquals(Pair.of("a", 1), t.removeThird());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Triple.of("abcdefg", "abcdefg", "abcdefg"), t.transformAll(f));
      assertEquals(Triple.of("abcdefg", 1, 42.0), t.transformFirst(f));
      assertEquals(Triple.of("a", "abcdefg", 42.0), t.transformSecond(f));
      assertEquals(Triple.of("a", 1, "abcdefg"), t.transformThird(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(t);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Triple<?, ?, ?> deserialized = (Triple<?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(t, deserialized);
   }

   // separate, combine
   
   @Test public void separate() {
      assertEquals(
            Triple.of(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
            Triple.separate(Collections.<Triple<Integer, String, Double>>emptyList()));

      List<Triple<Integer, String, Double>> trios = Arrays.asList(Triple.of(1, "a", 101.0),
            Triple.of(2, "b", 222.0), Triple.of(3, "c", 330.3));
      assertEquals(
            Triple.of(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
                  Arrays.asList(101.0, 222.0, 330.3)),
            Triple.separate(trios));
   }

   private <A, B, C> void assertCombinedEquals(List<Triple<A, B, C>> expectedTrios,
         Collection<? extends A> a, Collection<? extends B> b, Collection<? extends C> c) {
      assertEquals(expectedTrios, Triple.combine(a, b, c));
      assertEquals(expectedTrios, Triple.combine(Triple.of(a, b, c)));
   }
   
   @Test public void combine() {
      assertCombinedEquals(Collections.<Triple<Integer, String, Double>>emptyList(),
            Collections.<Integer>emptyList(), Collections.<String>emptyList(),
            Collections.<Double>emptyList());

      assertCombinedEquals(
            Arrays.asList(Triple.of(1, "a", 101.0), Triple.of(2, "b", 222.0),
                  Triple.of(3, "c", 330.3)),
            Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3));
   }

   private void assertCombineFails(Collection<?> a, Collection<?> b, Collection<?> c) {
      try {
         Triple.combine(a, b, c);
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }

      try {
         Triple.combine(Triple.of(a, b, c));
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }
}

   @Test public void combine_unequalSizes() {
      assertCombineFails(Collections.emptySet(), Collections.emptySet(),
            Collections.singleton("abc"));
      assertCombineFails(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c", "d", "e"),
            Arrays.asList(101.0, 222.0, 330.3));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3, 400.4, 555.5));
   }
}
