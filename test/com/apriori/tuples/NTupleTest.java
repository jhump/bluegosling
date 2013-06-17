package com.apriori.tuples;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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
 * Test cases for {@link NTuple}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class NTupleTest {

   private final NTuple<String, Integer, Double, String, Long> n =
         NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ");

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(n.contains(null));
      assertFalse(n.contains("b"));
      assertFalse(n.contains(new Object()));
      assertTrue(n.contains("a"));
      assertTrue(n.contains(1));
      assertTrue(n.contains(42.0));
      assertTrue(n.contains("foobar"));
      assertTrue(n.contains(0x1234L));
      assertTrue(n.contains("ABC"));
      assertTrue(n.contains("XYZ"));
      assertTrue(NTuple.create("a", 1, 42.0, "foobar", 2, 4, 8, null).contains(null));
      assertTrue(NTuple.create("a", 1, 42.0, "foobar", 2, 4, null, 8).contains(null));
      assertTrue(NTuple.create("a", 1, 42.0, "foobar", 2, null, 4, 8).contains(null));
      assertTrue(NTuple.create("a", 1, 42.0, "foobar", null, 2, 4, 8).contains(null));
      assertTrue(NTuple.create("a", 1, 42.0, null, "foobar", 2, 4, 8).contains(null));
      assertTrue(NTuple.create("a", 1, null, 42.0, "foobar", 2, 4, 8).contains(null));
      assertTrue(NTuple.create("a", null, 1, 42.0, "foobar", 2, 4, 8).contains(null));
      assertTrue(NTuple.create(null, "a", 1, 42.0, "foobar", 2, 4, 8).contains(null));
   }
   
   @Test public void isEmpty() {
      assertFalse(n.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(12, n.size());
   }
   
   @Test public void iterator() {
      Iterator<Object> iter = n.iterator();
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
      assertTrue(iter.hasNext());
      assertEquals("baz", iter.next());
      assertTrue(iter.hasNext());
      assertEquals(2, iter.next());
      assertTrue(iter.hasNext());
      assertEquals(4, iter.next());
      assertTrue(iter.hasNext());
      assertEquals(8, iter.next());
      assertTrue(iter.hasNext());
      assertEquals(16, iter.next());
      assertTrue(iter.hasNext());
      assertEquals("ABC", iter.next());
      assertTrue(iter.hasNext());
      assertEquals("XYZ", iter.next());
      assertFalse(iter.hasNext());
   }
   
   @Test public void asList() {
      List<?> list = n.asList();
      assertEquals(12, list.size());
      assertEquals("a", list.get(0));
      assertEquals(1, list.get(1));
      assertEquals(42.0, list.get(2));
      assertEquals("foobar", list.get(3));
      assertEquals(0x1234L, list.get(4));
      assertEquals("ABC", list.get(10));
      assertEquals("XYZ", list.get(11));
      assertEquals(
            Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            list);
   }
   
   @Test public void toArray() {
      assertArrayEquals(
            new Object[] { "a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ" },
            n.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(n.equals(n));
      assertTrue(n.equals(
            NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ")));
      assertTrue(n.equals(
            NTuple.create("XYZ", "a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ")
                  .removeFirst()));
      assertTrue(n.equals(
            NTuple.create(1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ")
                  .insertFirst("a")));
      assertTrue(n.equals(
            Quintet.create(1, 42.0, "foobar", 0x1234L, "baz").insertFirst("a")
                  .add(2).add(4).add(8).add(16).add("ABC").add("XYZ")));
      assertFalse(n.equals(Empty.INSTANCE));
      assertFalse(n.equals(Unit.create("a")));
      assertFalse(n.equals(Pair.create("a", 1)));
      assertFalse(n.equals(Trio.create("a", 1, 42.0)));
      assertFalse(n.equals(Quartet.create("a", 1, 42.0, "baz")));
      assertFalse(n.equals(Quintet.create("a", 1, 42.0, "foobar", 0x1234L)));
      assertFalse(n.equals(NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz")));
      assertFalse(n.equals(
            NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "DEF")));
      assertFalse(n.equals(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L)));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(
            Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ")
                  .hashCode(),
            n.hashCode());
   }
   
   // accessors / mutators

   @Test public void get() {
      assertEquals("a", n.getFirst());
      assertEquals((Integer) 1, n.getSecond());
      assertEquals((Double) 42.0, n.getThird());
      assertEquals("foobar", n.getFourth());
      assertEquals((Long) 0x1234L, n.getFifth());
   }
   
   @Test public void addAndInsert() {
      assertEquals(
            NTuple.create("boo", "a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.insertFirst("boo"));
      assertEquals(
            NTuple.create("a", "boo", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.insertSecond("boo"));
      assertEquals(
            NTuple.create("a", 1, "boo", 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.insertThird("boo"));
      assertEquals(
            NTuple.create("a", 1, 42.0, "boo", "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.insertFourth("boo"));
      assertEquals(
            NTuple.create("a", 1, 42.0, "foobar", "boo", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.insertFifth("boo"));
      assertEquals(
            NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ", "boo"),
            n.add("boo"));
   }
   
   @Test public void remove_sixItems() {
      NTuple<String, Integer, Double, String, Long> n6 =
            NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "baz");
      assertEquals(Quintet.create(1, 42.0, "foobar", 0x1234L, "baz"), n6.removeFirst());
      assertEquals(Quintet.create("a", 42.0, "foobar", 0x1234L, "baz"), n6.removeSecond());
      assertEquals(Quintet.create("a", 1, "foobar", 0x1234L, "baz"), n6.removeThird());
      assertEquals(Quintet.create("a", 1, 42.0, 0x1234L, "baz"), n6.removeFourth());
      assertEquals(Quintet.create("a", 1, 42.0, "foobar", "baz"), n6.removeFifth());
      // result has 5 items so should be instance of Quintet
      assertSame(Quintet.class, n6.removeFirst().getClass());
      assertSame(Quintet.class, n6.removeSecond().getClass());
      assertSame(Quintet.class, n6.removeThird().getClass());
      assertSame(Quintet.class, n6.removeFourth().getClass());
      assertSame(Quintet.class, n6.removeFifth().getClass());
   }
   
   @Test public void remove_moreThanSixItems() {
      assertEquals(NTuple.create(1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.removeFirst());
      assertEquals(NTuple.create("a", 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.removeSecond());
      assertEquals(NTuple.create("a", 1, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.removeThird());
      assertEquals(NTuple.create("a", 1, 42.0, 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.removeFourth());
      assertEquals(NTuple.create("a", 1, 42.0, "foobar", "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.removeFifth());
      // result has more than 5 items so should be instance of NTuple
      assertSame(NTuple.class, n.removeFirst().getClass());
      assertSame(NTuple.class, n.removeSecond().getClass());
      assertSame(NTuple.class, n.removeThird().getClass());
      assertSame(NTuple.class, n.removeFourth().getClass());
      assertSame(NTuple.class, n.removeFifth().getClass());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(
            NTuple.create("abcdefg", "abcdefg", "abcdefg", "abcdefg", "abcdefg", "abcdefg",
                  "abcdefg", "abcdefg", "abcdefg", "abcdefg", "abcdefg", "abcdefg"),
            n.transformAll(f));
      assertEquals(
            NTuple.create("abcdefg", 1, 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"),
            n.transformFirst(f));
      assertEquals(
            NTuple.create("a", "abcdefg", 42.0, "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"), 
            n.transformSecond(f));
      assertEquals(
            NTuple.create("a", 1, "abcdefg", "foobar", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"), 
            n.transformThird(f));
      assertEquals(
            NTuple.create("a", 1, 42.0, "abcdefg", 0x1234L, "baz", 2, 4, 8, 16, "ABC", "XYZ"), 
            n.transformFourth(f));
      assertEquals(
            NTuple.create("a", 1, 42.0, "foobar", "abcdefg", "baz", 2, 4, 8, 16, "ABC", "XYZ"), 
            n.transformFifth(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(n);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      NTuple<?, ?, ?, ?, ?> deserialized =
            (NTuple<?, ?, ?, ?, ?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(n, deserialized);
   }
}
