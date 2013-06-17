package com.apriori.tuples;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.apriori.util.Function;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Test cases for {@link Trio}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TrioTest {

   private final Trio<String, Integer, Double> t = Trio.create("a", 1, 42.0);

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(t.contains(null));
      assertFalse(t.contains("b"));
      assertFalse(t.contains(new Object()));
      assertTrue(t.contains("a"));
      assertTrue(t.contains(1));
      assertTrue(t.contains(42.0));
      assertTrue(Trio.create("a", 1, null).contains(null));
      assertTrue(Trio.create("a", null, 1).contains(null));
      assertTrue(Trio.create(null, "a", 1).contains(null));
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
      assertTrue(t.equals(Trio.create("a", 1, 42.0)));
      assertTrue(t.equals(Quartet.create("XYZ", "a", 1, 42.0).removeFirst()));
      assertTrue(t.equals(Pair.create(1, 42.0).insertFirst("a")));
      assertFalse(t.equals(Empty.INSTANCE));
      assertFalse(t.equals(Unit.create("a")));
      assertFalse(t.equals(Pair.create("a", 1)));
      assertFalse(t.equals(Trio.create("a", 1, 43.43)));
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
      Quartet<String, Integer, Double, String> q = t.add("foobar");
      assertEquals(Quartet.create("a", 1, 42.0, "foobar"), q);
      assertEquals(q, t.insertFourth("foobar"));
      assertEquals(Quartet.create("foobar", "a", 1, 42.0), t.insertFirst("foobar"));
      assertEquals(Quartet.create("a", "foobar", 1, 42.0), t.insertSecond("foobar"));
      assertEquals(Quartet.create("a", 1, "foobar", 42.0), t.insertThird("foobar"));
   }
   
   @Test public void remove() {
      assertEquals(Pair.create(1, 42.0), t.removeFirst());
      assertEquals(Pair.create("a", 42.0), t.removeSecond());
      assertEquals(Pair.create("a", 1), t.removeThird());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Trio.create("abcdefg", "abcdefg", "abcdefg"), t.transformAll(f));
      assertEquals(Trio.create("abcdefg", 1, 42.0), t.transformFirst(f));
      assertEquals(Trio.create("a", "abcdefg", 42.0), t.transformSecond(f));
      assertEquals(Trio.create("a", 1, "abcdefg"), t.transformThird(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(t);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Trio<?, ?, ?> deserialized = (Trio<?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(t, deserialized);
   }
}
