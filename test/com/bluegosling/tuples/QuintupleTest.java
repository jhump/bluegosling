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
 * Test cases for {@link Quintuple}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class QuintupleTest {

   private final Quintuple<String, Integer, Double, String, Long> q =
         Quintuple.of("a", 1, 42.0, "foobar", 0x1234L);

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(q.contains(null));
      assertFalse(q.contains("b"));
      assertFalse(q.contains(new Object()));
      assertTrue(q.contains("a"));
      assertTrue(q.contains(1));
      assertTrue(q.contains(42.0));
      assertTrue(q.contains("foobar"));
      assertTrue(q.contains(0x1234L));
      assertTrue(Quintuple.of("a", 1, 42.0, "foobar", null).contains(null));
      assertTrue(Quintuple.of("a", 1, 42.0, null, "foobar").contains(null));
      assertTrue(Quintuple.of("a", 1, null, 42.0, "foobar").contains(null));
      assertTrue(Quintuple.of("a", null, 1, 42.0, "foobar").contains(null));
      assertTrue(Quintuple.of(null, "a", 1, 42.0, "foobar").contains(null));
   }
   
   @Test public void isEmpty() {
      assertFalse(q.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(5, q.size());
   }
   
   @Test public void iterator() {
      Iterator<Object> iter = q.iterator();
      assertTrue(iter.hasNext());
      assertEquals("a", iter.next());
      assertTrue(iter.hasNext());
      assertEquals(1, iter.next());
      assertTrue(iter.hasNext());
      assertEquals(42.0, iter.next());
      assertTrue(iter.hasNext());
      assertEquals("foobar", iter.next());
      assertTrue(iter.hasNext());
      assertEquals(0x1234L, iter.next());
      assertFalse(iter.hasNext());
   }
   
   @Test public void asList() {
      List<?> list = q.asList();
      assertEquals(5, list.size());
      assertEquals("a", list.get(0));
      assertEquals(1, list.get(1));
      assertEquals(42.0, list.get(2));
      assertEquals("foobar", list.get(3));
      assertEquals(0x1234L, list.get(4));
      assertEquals(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L), list);
   }
   
   @Test public void toArray() {
      assertArrayEquals(new Object[] { "a", 1, 42.0, "foobar", 0x1234L }, q.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(q.equals(q));
      assertTrue(q.equals(Quintuple.of("a", 1, 42.0, "foobar", 0x1234L)));
      assertTrue(q.equals(NTuple.create("XYZ", "a", 1, 42.0, "foobar", 0x1234L).removeFirst()));
      assertTrue(q.equals(Quadruple.of(1, 42.0, "foobar", 0x1234L).insertFirst("a")));
      assertFalse(q.equals(Empty.INSTANCE));
      assertFalse(q.equals(Single.of("a")));
      assertFalse(q.equals(Pair.of("a", 1)));
      assertFalse(q.equals(Triple.of("a", 1, 42.0)));
      assertFalse(q.equals(Quadruple.of("a", 1, 42.0, "baz")));
      assertFalse(q.equals(Quintuple.of("a", 1, 42.0, "foobar", 0x2222L)));
      assertFalse(q.equals(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L)));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L).hashCode(), q.hashCode());
   }
   
   // accessors / mutators

   @Test public void get() {
      assertEquals("a", q.getFirst());
      assertEquals((Integer) 1, q.getSecond());
      assertEquals((Double) 42.0, q.getThird());
      assertEquals("foobar", q.getFourth());
      assertEquals((Long) 0x1234L, q.getFifth());
   }
   
   @Test public void addAndInsert() {
      assertEquals(NTuple.create("baz", "a", 1, 42.0, "foobar", 0x1234L), q.insertFirst("baz"));
      assertEquals(NTuple.create("a", "baz", 1, 42.0, "foobar", 0x1234L), q.insertSecond("baz"));
      assertEquals(NTuple.create("a", 1, "baz", 42.0, "foobar", 0x1234L), q.insertThird("baz"));
      assertEquals(NTuple.create("a", 1, 42.0, "baz", "foobar", 0x1234L), q.insertFourth("baz"));
      assertEquals(NTuple.create("a", 1, 42.0, "foobar", "baz", 0x1234L), q.insertFifth("baz"));
      assertEquals(NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz"), q.add("baz"));
   }
   
   @Test public void remove() {
      assertEquals(Quadruple.of(1, 42.0, "foobar", 0x1234L), q.removeFirst());
      assertEquals(Quadruple.of("a", 42.0, "foobar", 0x1234L), q.removeSecond());
      assertEquals(Quadruple.of("a", 1, "foobar", 0x1234L), q.removeThird());
      assertEquals(Quadruple.of("a", 1, 42.0, 0x1234L), q.removeFourth());
      assertEquals(Quadruple.of("a", 1, 42.0, "foobar"), q.removeFifth());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Quintuple.of("abcdefg", "abcdefg", "abcdefg", "abcdefg", "abcdefg"),
            q.transformAll(f));
      assertEquals(Quintuple.of("abcdefg", 1, 42.0, "foobar", 0x1234L), q.transformFirst(f));
      assertEquals(Quintuple.of("a", "abcdefg", 42.0, "foobar", 0x1234L), q.transformSecond(f));
      assertEquals(Quintuple.of("a", 1, "abcdefg", "foobar", 0x1234L), q.transformThird(f));
      assertEquals(Quintuple.of("a", 1, 42.0, "abcdefg", 0x1234L), q.transformFourth(f));
      assertEquals(Quintuple.of("a", 1, 42.0, "foobar", "abcdefg"), q.transformFifth(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(q);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Quintuple<?, ?, ?, ?, ?> deserialized =
            (Quintuple<?, ?, ?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(q, deserialized);
   }
   
   // separate, combine
   
   @Test public void separate() {
      assertEquals(
            Quintuple.of(Collections.emptyList(), Collections.emptyList(),
                  Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
            Quintuple.separate(
                  Collections.<Quintuple<Integer, String, Double, Boolean, Float>>emptyList()));

      List<Quintuple<Integer, String, Double, Boolean, Float>> quintets =
            Arrays.asList(Quintuple.of(1, "a", 101.0, true, 321f),
                  Quintuple.of(2, "b", 222.0, false, 432f),
                  Quintuple.of(3, "c", 330.3, true, 543f));
      assertEquals(
            Quintuple.of(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
                  Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true),
                  Arrays.asList(321f, 432f, 543f)),
            Quintuple.separate(quintets));
   }

   private <A, B, C, D, E> void assertCombinedEquals(List<Quintuple<A, B, C, D, E>> expectedQuintets,
         Collection<? extends A> a, Collection<? extends B> b, Collection<? extends C> c,
         Collection<? extends D> d, Collection<? extends E> e) {
      assertEquals(expectedQuintets, Quintuple.combine(a, b, c, d, e));
      assertEquals(expectedQuintets, Quintuple.combine(Quintuple.of(a, b, c, d, e)));
   }
   
   @Test public void combine() {
      assertCombinedEquals(Collections.<Quintuple<Integer, String, Double, Boolean, Float>>emptyList(),
            Collections.<Integer>emptyList(), Collections.<String>emptySet(),
            Collections.<Double>emptyList(), Collections.<Boolean>emptySet(),
            Collections.<Float>emptyList());

      assertCombinedEquals(
            Arrays.asList(Quintuple.of(1, "a", 101.0, true, 321f),
                  Quintuple.of(2, "b", 222.0, false, 432f),
                  Quintuple.of(3, "c", 330.3, true, 543f)),
            Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true),
            Arrays.asList(321f, 432f, 543f));
   }

   private void assertCombineFails(Collection<?> a, Collection<?> b, Collection<?> c,
         Collection<?> d, Collection<?> e) {
      try {
         Quintuple.combine(a, b, c, d, e);
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }

      try {
         Quintuple.combine(Quintuple.of(a, b, c, d, e));
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }
   }

   @Test public void combine_unequalSizes() {
      assertCombineFails(Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
            Collections.emptyList(), Collections.singleton("abc"));
      assertCombineFails(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true),
            Arrays.asList(321f, 432f, 543f));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c", "d", "e"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true),
            Arrays.asList(321f, 432f, 543f));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3, 400.4, 555.5), Arrays.asList(true, false, true),
            Arrays.asList(321f, 432f, 543f));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true, false, true),
            Arrays.asList(321f, 432f, 543f));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true),
            Arrays.asList(321f, 432f, 543f, 654f, 765f));
   }
}
