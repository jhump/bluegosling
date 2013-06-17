package com.apriori.tuples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Test cases for {@link Tuples}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TuplesTest {

   @Test public void empty() {
      // (it's a singleton, so all empty tuples refer to same instance)
      assertSame(Empty.INSTANCE, Tuples.fromArray());
      assertSame(Empty.INSTANCE, Tuples.fromCollection(Collections.emptyList()));
   }

   @Test public void unit() {
      Unit<String> u = Unit.create("a");
      
      Tuple t = Tuples.fromArray("a");
      assertSame(Unit.class, t.getClass());
      assertEquals(u, t);
      
      t = Tuples.fromCollection(Collections.singleton("a"));
      assertSame(Unit.class, t.getClass());
      assertEquals(u, t);
   }

   @Test public void pair() {
      Pair<String, Integer> p = Pair.create("a", 1);

      Tuple t = Tuples.fromArray("a", 1);
      assertSame(Pair.class, t.getClass());
      assertEquals(p, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1));
      assertSame(Pair.class, t.getClass());
      assertEquals(p, t);
   }

   @Test public void trio() {
      Trio<String, Integer, Double> trio = Trio.create("a", 1, 42.0);
      
      Tuple t = Tuples.fromArray("a", 1, 42.0);
      assertSame(Trio.class, t.getClass());
      assertEquals(trio, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0));
      assertSame(Trio.class, t.getClass());
      assertEquals(trio, t);
   }

   @Test public void quartet() {
      Quartet<String, Integer, Double, String> q = Quartet.create("a", 1, 42.0, "foobar");
      
      Tuple t = Tuples.fromArray("a", 1, 42.0, "foobar");
      assertSame(Quartet.class, t.getClass());
      assertEquals(q, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0, "foobar"));
      assertSame(Quartet.class, t.getClass());
      assertEquals(q, t);
   }

   @Test public void quintet() {
      Quintet<String, Integer, Double, String, Long> q =
            Quintet.create("a", 1, 42.0, "foobar", 0x1234L);
      
      Tuple t = Tuples.fromArray("a", 1, 42.0, "foobar", 0x1234L);
      assertSame(Quintet.class, t.getClass());
      assertEquals(q, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L));
      assertSame(Quintet.class, t.getClass());
      assertEquals(q, t);
   }

   @Test public void ntuple_6() {
      NTuple<String, Integer, Double, String, Long> n =
            NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "XYZ");
      
      Tuple t = Tuples.fromArray("a", 1, 42.0, "foobar", 0x1234L, "XYZ");
      assertSame(NTuple.class, t.getClass());
      assertEquals(n, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L, "XYZ"));
      assertSame(NTuple.class, t.getClass());
      assertEquals(n, t);
   }

   @Test public void ntuple_many() {
      NTuple<String, Integer, Double, String, Long> n =
            NTuple.create("a", 1, 42.0, "foobar", 0x1234L, "ABC", "DEF", "GHI", "MNO", "XYZ",
                  0, 1, 2, 3, 4, 5, 0.5, 0.25, 0.125, 0.6125);
      
      Tuple t = Tuples.fromArray("a", 1, 42.0, "foobar", 0x1234L, "ABC", "DEF", "GHI", "MNO", "XYZ",
            0, 1, 2, 3, 4, 5, 0.5, 0.25, 0.125, 0.6125);
      assertSame(NTuple.class, t.getClass());
      assertEquals(n, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L, "ABC", "DEF",
            "GHI", "MNO", "XYZ", 0, 1, 2, 3, 4, 5, 0.5, 0.25, 0.125, 0.6125));
      assertSame(NTuple.class, t.getClass());
      assertEquals(n, t);
   }
}
