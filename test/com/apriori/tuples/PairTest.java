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
 * Test cases for {@link Pair}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class PairTest {
   
   private final Pair<String, Integer> p = Pair.create("a", 1);
   
   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(p.contains(null));
      assertFalse(p.contains("b"));
      assertFalse(p.contains(new Object()));
      assertTrue(p.contains("a"));
      assertTrue(p.contains(1));
      assertTrue(Pair.create("a", null).contains(null));
      assertTrue(Pair.create(null, "a").contains(null));
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
      assertTrue(p.equals(Pair.create("a", 1)));
      assertTrue(p.equals(Trio.create("a", "b", 1).removeSecond()));
      assertTrue(p.equals(Unit.create(1).insertFirst("a")));
      assertFalse(p.equals(Empty.INSTANCE));
      assertFalse(p.equals(Unit.create("a")));
      assertFalse(p.equals(Pair.create("a", "b")));
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
      Trio<String, Integer, Double> t = p.add(42.0);
      assertEquals(Trio.create("a", 1, 42.0), t);
      assertEquals(t, p.insertThird(42.0));
      assertEquals(Trio.create(42.0, "a", 1), p.insertFirst(42.0));
      assertEquals(Trio.create("a", 42.0, 1), p.insertSecond(42.0));
   }
   
   @Test public void remove() {
      assertEquals(Unit.create(1), p.removeFirst());
      assertEquals(Unit.create("a"), p.removeSecond());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Pair.create("abcdefg", "abcdefg"), p.transformAll(f));
      assertEquals(Pair.create("abcdefg", 1), p.transformFirst(f));
      assertEquals(Pair.create("a", "abcdefg"), p.transformSecond(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(p);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Pair<?, ?> deserialized = (Pair<?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(p, deserialized);
   }
}
