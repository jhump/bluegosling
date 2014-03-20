package com.apriori.tuples;

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
 * Test cases for {@link Quartet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class QuartetTest {

   private final Quartet<String, Integer, Double, String> q = Quartet.create("a", 1, 42.0, "foobar");

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(q.contains(null));
      assertFalse(q.contains("b"));
      assertFalse(q.contains(new Object()));
      assertTrue(q.contains("a"));
      assertTrue(q.contains(1));
      assertTrue(q.contains(42.0));
      assertTrue(q.contains("foobar"));
      assertTrue(Quartet.create("a", 1, 42.0, null).contains(null));
      assertTrue(Quartet.create("a", 1, null, 42.0).contains(null));
      assertTrue(Quartet.create("a", null, 1, 42.0).contains(null));
      assertTrue(Quartet.create(null, "a", 1, 42.0).contains(null));
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
      assertTrue(q.equals(Quartet.create("a", 1, 42.0, "foobar")));
      assertTrue(q.equals(Quintet.create("XYZ", "a", 1, 42.0, "foobar").removeFirst()));
      assertTrue(q.equals(Trio.create(1, 42.0, "foobar").insertFirst("a")));
      assertFalse(q.equals(Empty.INSTANCE));
      assertFalse(q.equals(Unit.create("a")));
      assertFalse(q.equals(Pair.create("a", 1)));
      assertFalse(q.equals(Trio.create("a", 1, 42.0)));
      assertFalse(q.equals(Quartet.create("a", 1, 42.0, "baz")));
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
      Quintet<String, Integer, Double, String, Long> q5 = q.add(0x1234L);
      assertEquals(Quintet.create("a", 1, 42.0, "foobar", 0x1234L), q5);
      assertEquals(q5, q.insertFifth(0x1234L));
      assertEquals(Quintet.create(0x1234L, "a", 1, 42.0, "foobar"), q.insertFirst(0x1234L));
      assertEquals(Quintet.create("a", 0x1234L, 1, 42.0, "foobar"), q.insertSecond(0x1234L));
      assertEquals(Quintet.create("a", 1, 0x1234L, 42.0, "foobar"), q.insertThird(0x1234L));
      assertEquals(Quintet.create("a", 1, 42.0, 0x1234L, "foobar"), q.insertFourth(0x1234L));
   }
   
   @Test public void remove() {
      assertEquals(Trio.create(1, 42.0, "foobar"), q.removeFirst());
      assertEquals(Trio.create("a", 42.0, "foobar"), q.removeSecond());
      assertEquals(Trio.create("a", 1, "foobar"), q.removeThird());
      assertEquals(Trio.create("a", 1, 42.0), q.removeFourth());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Quartet.create("abcdefg", "abcdefg", "abcdefg", "abcdefg"), q.transformAll(f));
      assertEquals(Quartet.create("abcdefg", 1, 42.0, "foobar"), q.transformFirst(f));
      assertEquals(Quartet.create("a", "abcdefg", 42.0, "foobar"), q.transformSecond(f));
      assertEquals(Quartet.create("a", 1, "abcdefg", "foobar"), q.transformThird(f));
      assertEquals(Quartet.create("a", 1, 42.0, "abcdefg"), q.transformFourth(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(q);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Quartet<?, ?, ?, ?> deserialized =
            (Quartet<?, ?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(q, deserialized);
   }

   // separate, combine
   
   @Test public void separate() {
      assertEquals(
            Quartet.create(Collections.emptyList(), Collections.emptyList(),
                  Collections.emptyList(), Collections.emptyList()),
            Quartet.separate(Collections.<Quartet<Integer, String, Double, Boolean>>emptyList()));

      List<Quartet<Integer, String, Double, Boolean>> quartets =
            Arrays.asList(Quartet.create(1, "a", 101.0, true), Quartet.create(2, "b", 222.0, false),
                  Quartet.create(3, "c", 330.3, true));
      assertEquals(
            Quartet.create(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
                  Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true)),
            Quartet.separate(quartets));
   }

   private <A, B, C, D> void assertCombinedEquals(List<Quartet<A, B, C, D>> expectedQuartets,
         Collection<? extends A> a, Collection<? extends B> b, Collection<? extends C> c,
         Collection<? extends D> d) {
      assertEquals(expectedQuartets, Quartet.combine(a, b, c, d));
      assertEquals(expectedQuartets, Quartet.combine(Quartet.create(a, b, c, d)));
   }
   
   @Test public void combine() {
      assertCombinedEquals(Collections.<Quartet<Integer, String, Double, Boolean>>emptyList(),
            Collections.<Integer>emptyList(), Collections.<String>emptySet(),
            Collections.<Double>emptyList(), Collections.<Boolean>emptySet());

      assertCombinedEquals(
            Arrays.asList(Quartet.create(1, "a", 101.0, true), Quartet.create(2, "b", 222.0, false),
                  Quartet.create(3, "c", 330.3, true)),
            Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"),
            Arrays.asList(101.0, 222.0, 330.3), Arrays.asList(true, false, true));
   }

   private void assertCombineFails(Collection<?> a, Collection<?> b, Collection<?> c,
         Collection<?> d) {
      try {
         Quartet.combine(a, b, c, d);
         fail("expecting IllegalArgumentException but never thrown");
      } catch (IllegalArgumentException expected) {
      }

      try {
         Quartet.combine(Quartet.create(a, b, c, d));
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
