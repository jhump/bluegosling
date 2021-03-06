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
 * Test cases for {@link Quadruple}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class QuadrupleTest {

   private final Quadruple<String, Integer, Double, String> q = Quadruple.of("a", 1, 42.0, "foobar");

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(q.contains(null));
      assertFalse(q.contains("b"));
      assertFalse(q.contains(new Object()));
      assertTrue(q.contains("a"));
      assertTrue(q.contains(1));
      assertTrue(q.contains(42.0));
      assertTrue(q.contains("foobar"));
      assertTrue(Quadruple.of("a", 1, 42.0, null).contains(null));
      assertTrue(Quadruple.of("a", 1, null, 42.0).contains(null));
      assertTrue(Quadruple.of("a", null, 1, 42.0).contains(null));
      assertTrue(Quadruple.of(null, "a", 1, 42.0).contains(null));
   }
   
   @Test public void isEmpty() {
      assertFalse(q.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(4, q.size());
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
      assertFalse(iter.hasNext());
   }
   
   @Test public void asList() {
      List<?> list = q.asList();
      assertEquals(4, list.size());
      assertEquals("a", list.get(0));
      assertEquals(1, list.get(1));
      assertEquals(42.0, list.get(2));
      assertEquals("foobar", list.get(3));
      assertEquals(Arrays.<Object>asList("a", 1, 42.0, "foobar"), list);
   }
   
   @Test public void toArray() {
      assertArrayEquals(new Object[] { "a", 1, 42.0, "foobar" }, q.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(q.equals(q));
      assertTrue(q.equals(Quadruple.of("a", 1, 42.0, "foobar")));
      assertTrue(q.equals(Quintuple.of("XYZ", "a", 1, 42.0, "foobar").removeFirst()));
      assertTrue(q.equals(Triple.of(1, 42.0, "foobar").insertFirst("a")));
      assertFalse(q.equals(Empty.INSTANCE));
      assertFalse(q.equals(Single.of("a")));
      assertFalse(q.equals(Pair.of("a", 1)));
      assertFalse(q.equals(Triple.of("a", 1, 42.0)));
      assertFalse(q.equals(Quadruple.of("a", 1, 42.0, "baz")));
      assertFalse(q.equals(Arrays.<Object>asList("a", 1, 42.0, "foobar")));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(Arrays.<Object>asList("a", 1, 42.0, "foobar").hashCode(), q.hashCode());
   }
   
   // accessors / mutators

   @Test public void get() {
      assertEquals("a", q.getFirst());
      assertEquals((Integer) 1, q.getSecond());
      assertEquals((Double) 42.0, q.getThird());
      assertEquals("foobar", q.getFourth());
   }
   
   @Test public void addAndInsert() {
      Quintuple<String, Integer, Double, String, Long> q5 = q.add(0x1234L);
      assertEquals(Quintuple.of("a", 1, 42.0, "foobar", 0x1234L), q5);
      assertEquals(q5, q.insertFifth(0x1234L));
      assertEquals(Quintuple.of(0x1234L, "a", 1, 42.0, "foobar"), q.insertFirst(0x1234L));
      assertEquals(Quintuple.of("a", 0x1234L, 1, 42.0, "foobar"), q.insertSecond(0x1234L));
      assertEquals(Quintuple.of("a", 1, 0x1234L, 42.0, "foobar"), q.insertThird(0x1234L));
      assertEquals(Quintuple.of("a", 1, 42.0, 0x1234L, "foobar"), q.insertFourth(0x1234L));
   }
   
   @Test public void remove() {
      assertEquals(Triple.of(1, 42.0, "foobar"), q.removeFirst());
      assertEquals(Triple.of("a", 42.0, "foobar"), q.removeSecond());
      assertEquals(Triple.of("a", 1, "foobar"), q.removeThird());
      assertEquals(Triple.of("a", 1, 42.0), q.removeFourth());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Quadruple.of("abcdefg", "abcdefg", "abcdefg", "abcdefg"), q.transformAll(f));
      assertEquals(Quadruple.of("abcdefg", 1, 42.0, "foobar"), q.transformFirst(f));
      assertEquals(Quadruple.of("a", "abcdefg", 42.0, "foobar"), q.transformSecond(f));
      assertEquals(Quadruple.of("a", 1, "abcdefg", "foobar"), q.transformThird(f));
      assertEquals(Quadruple.of("a", 1, 42.0, "abcdefg"), q.transformFourth(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(q);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Quadruple<?, ?, ?, ?> deserialized =
            (Quadruple<?, ?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(q, deserialized);
   }

   // separate, combine
   
   @Test public void separate() {
      assertEquals(
            Quadruple.of(Collections.emptyList(), Collections.emptyList(),
                  Collections.emptyList(), Collections.emptyList()),
            Quadruple.separate(Collections.<Quadruple<Integer, String, Double, Boolean>>emptyList()));

      List<Quadruple<Integer, String, Double, Boolean>> quartets =
            Arrays.asList(Quadruple.of(1, "a", 101.0, true), Quadruple.of(2, "b", 222.0, false),
                  Quadruple.of(3, "c", 330.3, true));
      assertEquals(
            Quadruple.of(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
                  Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true)),
            Quadruple.separate(quartets));
   }

   private <A, B, C, D> void assertCombinedEquals(List<Quadruple<A, B, C, D>> expectedQuartets,
         Collection<? extends A> a, Collection<? extends B> b, Collection<? extends C> c,
         Collection<? extends D> d) {
      assertEquals(expectedQuartets, Quadruple.combine(a, b, c, d));
      assertEquals(expectedQuartets, Quadruple.combine(Quadruple.of(a, b, c, d)));
   }
   
   @Test public void combine() {
      assertCombinedEquals(Collections.<Quadruple<Integer, String, Double, Boolean>>emptyList(),
            Collections.<Integer>emptyList(), Collections.<String>emptySet(),
            Collections.<Double>emptyList(), Collections.<Boolean>emptySet());

      assertCombinedEquals(
            Arrays.asList(Quadruple.of(1, "a", 101.0, true), Quadruple.of(2, "b", 222.0, false),
                  Quadruple.of(3, "c", 330.3, true)),
            Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true));
   }

   private void assertCombineFails(Collection<?> a, Collection<?> b, Collection<?> c,
         Collection<?> d) {
      try {
         Quadruple.combine(a, b, c, d);
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }

      try {
         Quadruple.combine(Quadruple.of(a, b, c, d));
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }
}

   @Test public void combine_unequalSizes() {
      assertCombineFails(Collections.emptySet(), Collections.emptySet(), Collections.emptyList(),
            Collections.singleton("abc"));
      assertCombineFails(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c", "d", "e"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3, 400.4, 555.5), Arrays.asList(true, false, true));
      assertCombineFails(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true, false, true));
   }
}
