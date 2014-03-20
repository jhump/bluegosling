package com.apriori.tuples;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.function.Function;

/**
 * Test cases for {@link Empty}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class EmptyTest {

   private final Empty e = Empty.INSTANCE;

   // Collection-like operations

   @Test public void contains() {
      assertFalse(e.contains(null));
      assertFalse(e.contains("b"));
      assertFalse(e.contains(new Object()));
   }
   
   @Test public void isEmpty() {
      assertTrue(e.isEmpty());
   }
   
   @Test public void size() {
      assertEquals(0, e.size());
   }
   
   @Test public void iterator() {
      assertFalse(e.iterator().hasNext());
   }
   
   @Test public void asList() {
      assertEquals(0, e.asList().size());
      assertEquals(Collections.emptyList(), e.asList());
   }
   
   @Test public void toArray() {
      assertArrayEquals(new Object[0], e.toArray());
   }
   
   // equals & hashCode
   
   @Test public void equals() {
      assertTrue(e.equals(e));
      assertTrue(e.equals(Unit.create("a").removeFirst()));
      assertFalse(e.equals(Unit.create("a")));
      assertFalse(e.equals(Collections.emptyList()));
   }
   
   @Test public void hashCodeTest() {
      assertEquals(Collections.emptyList().hashCode(), e.hashCode());
   }
   
   // accessors / mutators
   
   @Test public void addAndInsert() {
      Unit<String> u = e.add("a");
      assertEquals(Unit.create("a"), u);
      assertEquals(u, e.insertFirst("a"));
   }
   
   @Test public void transform() {
      Function<Object, String> f = new Function<Object, String>() {
         @Override public String apply(Object o) {
            return "abcdefg";
         }
      };
      
      assertSame(Empty.INSTANCE, e.transformAll(f));
   }
   
   // serialization
   
   @Test public void serialization() throws Exception {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      new ObjectOutputStream(bos).writeObject(e);
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      Empty deserialized = (Empty) new ObjectInputStream(bis).readObject();
      
      assertSame(Empty.INSTANCE, deserialized);
   }
}