package com.bluegosling.tuples;

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
      Single<String> u = Single.of("a");
      
      Tuple t = Tuples.fromArray("a");
      assertSame(Single.class, t.getClass());
      assertEquals(u, t);
      
      t = Tuples.fromCollection(Collections.singleton("a"));
      assertSame(Single.class, t.getClass());
      assertEquals(u, t);
   }

   @Test public void pair() {
      Pair<String, Integer> p = Pair.of("a", 1);

      Tuple t = Tuples.fromArray("a", 1);
      assertSame(Pair.class, t.getClass());
      assertEquals(p, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1));
      assertSame(Pair.class, t.getClass());
      assertEquals(p, t);
   }

   @Test public void trio() {
      Triple<String, Integer, Double> trio = Triple.of("a", 1, 42.0);
      
      Tuple t = Tuples.fromArray("a", 1, 42.0);
      assertSame(Triple.class, t.getClass());
      assertEquals(trio, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0));
      assertSame(Triple.class, t.getClass());
      assertEquals(trio, t);
   }

   @Test public void quartet() {
      Quadruple<String, Integer, Double, String> q = Quadruple.of("a", 1, 42.0, "foobar");
      
      Tuple t = Tuples.fromArray("a", 1, 42.0, "foobar");
      assertSame(Quadruple.class, t.getClass());
      assertEquals(q, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0, "foobar"));
      assertSame(Quadruple.class, t.getClass());
      assertEquals(q, t);
   }

   @Test public void quintet() {
      Quintuple<String, Integer, Double, String, Long> q =
            Quintuple.of("a", 1, 42.0, "foobar", 0x1234L);
      
      Tuple t = Tuples.fromArray("a", 1, 42.0, "foobar", 0x1234L);
      assertSame(Quintuple.class, t.getClass());
      assertEquals(q, t);
      
      t = Tuples.fromCollection(Arrays.<Object>asList("a", 1, 42.0, "foobar", 0x1234L));
      assertSame(Quintuple.class, t.getClass());
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
