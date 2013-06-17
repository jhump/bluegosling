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
 * Test cases for {@link Quintet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class QuintetTest {

   private final Quintet<String, Integer, Double, String, Long> q =
         Quintet.create("a", 1, 42.0, "foobar", 0x1234L);

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
      assertTrue(Quintet.create("a", 1, 42.0, "foobar", null).contains(null));
      assertTrue(Quintet.create("a", 1, 42.0, null, "foobar").contains(null));
      assertTrue(Quintet.create("a", 1, null, 42.0, "foobar").contains(null));
      assertTrue(Quintet.create("a", null, 1, 42.0, "foobar").contains(null));
      assertTrue(Quintet.create(null, "a", 1, 42.0, "foobar").contains(null));
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
      assertTrue(q.equals(Quintet.create("a", 1, 42.0, "foobar", 0x1234L)));
      assertTrue(q.equals(NTuple.create("XYZ", "a", 1, 42.0, "foobar", 0x1234L).removeFirst()));
      assertTrue(q.equals(Quartet.create(1, 42.0, "foobar", 0x1234L).insertFirst("a")));
      assertFalse(q.equals(Empty.INSTANCE));
      assertFalse(q.equals(Unit.create("a")));
      assertFalse(q.equals(Pair.create("a", 1)));
      assertFalse(q.equals(Trio.create("a", 1, 42.0)));
      assertFalse(q.equals(Quartet.create("a", 1, 42.0, "baz")));
      assertFalse(q.equals(Quintet.create("a", 1, 42.0, "foobar", 0x2222L)));
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
      assertEquals(Quartet.create(1, 42.0, "foobar", 0x1234L), q.removeFirst());
      assertEquals(Quartet.create("a", 42.0, "foobar", 0x1234L), q.removeSecond());
      assertEquals(Quartet.create("a", 1, "foobar", 0x1234L), q.removeThird());
      assertEquals(Quartet.create("a", 1, 42.0, 0x1234L), q.removeFourth());
      assertEquals(Quartet.create("a", 1, 42.0, "foobar"), q.removeFifth());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Quintet.create("abcdefg", "abcdefg", "abcdefg", "abcdefg", "abcdefg"),
            q.transformAll(f));
      assertEquals(Quintet.create("abcdefg", 1, 42.0, "foobar", 0x1234L), q.transformFirst(f));
      assertEquals(Quintet.create("a", "abcdefg", 42.0, "foobar", 0x1234L), q.transformSecond(f));
      assertEquals(Quintet.create("a", 1, "abcdefg", "foobar", 0x1234L), q.transformThird(f));
      assertEquals(Quintet.create("a", 1, 42.0, "abcdefg", 0x1234L), q.transformFourth(f));
      assertEquals(Quintet.create("a", 1, 42.0, "foobar", "abcdefg"), q.transformFifth(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(q);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Quintet<?, ?, ?, ?, ?> deserialized =
            (Quintet<?, ?, ?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(q, deserialized);
   }
}
