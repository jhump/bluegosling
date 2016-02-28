package com.bluegosling.tuples;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Test cases from {@link Unit}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class UnitTest {

   private final Unit<String> u = Unit.create("a");

   // Collection-like operations
   
   @Test public void contains() {
      assertFalse(u.contains(null));
      assertFalse(u.contains("b"));
      assertFalse(u.contains(new Object()));
      assertTrue(u.contains("a"));
      assertTrue(Unit.create(null).contains(null));
   }
   
   @Test public void isEmpty() {
      assertFalse(u.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(1, u.size());
   }
   
   @Test public void iterator() {
      Iterator<Object> iter = u.iterator();
      assertTrue(iter.hasNext());
      assertEquals("a", iter.next());
      assertFalse(iter.hasNext());
   }
   
   @Test public void asList() {
      List<String> list = u.asList();
      assertEquals(1, list.size());
      assertEquals("a", list.get(0));
      assertEquals(Collections.singletonList("a"), list);
   }
   
   @Test public void toArray() {
      assertArrayEquals(new Object[] { "a" }, u.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(u.equals(u));
      assertTrue(u.equals(Unit.create("a")));
      assertTrue(u.equals(Pair.create("a", "b").removeSecond()));
      assertTrue(u.equals(Empty.INSTANCE.add("a")));
      assertFalse(u.equals(Empty.INSTANCE));
      assertFalse(u.equals(Unit.create("b")));
      assertFalse(u.equals(Pair.create("a", "b")));
      assertFalse(u.equals(Collections.singletonList("a")));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(Collections.singletonList("a").hashCode(), u.hashCode());
   }
   
   // accessors / mutators

   @Test public void get() {
      assertEquals("a", u.getFirst());
   }
   
   @Test public void addAndInsert() {
      Pair<String, Integer> p = u.add(1);
      assertEquals(Pair.create("a", 1), p);
      assertEquals(p, u.insertSecond(1));
      assertEquals(Pair.create(42.0, "a"), u.insertFirst(42.0));
   }
   
   @Test public void remove() {
      assertEquals(Empty.INSTANCE, u.removeFirst());
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertEquals(Unit.create("abcdefg"), u.transformAll(f));
      assertEquals(Unit.create("abcdefg"), u.transformFirst(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(u);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Unit<?> deserialized = (Unit<?>) new ObjectInputStream(bis).readObject();
      
      assertEquals(u, deserialized);
   }
   
   // extract, enclose
   
   @Test public void extract() {
      assertEquals(Collections.emptyList(), Unit.extract(Collections.<Unit<String>>emptyList()));

      List<Unit<Integer>> units = Arrays.asList(Unit.create(1), Unit.create(2), Unit.create(3));
      assertEquals(Arrays.asList(1, 2, 3), Unit.extract(units));
   }

   @Test public void enclose() {
      assertEquals(Collections.emptyList(), Unit.enclose(Collections.<String>emptyList()));

      List<Unit<Integer>> units = Arrays.asList(Unit.create(1), Unit.create(2), Unit.create(3));
      assertEquals(units, Unit.enclose(Arrays.asList(1, 2, 3)));
   }
}
