package com.bluegosling.choice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;

import com.bluegosling.possible.Possible;
import com.bluegosling.vars.Variable;
import com.bluegosling.vars.VariableInt;

import static com.bluegosling.testing.MoreAsserts.assertNotEquals;
import static com.bluegosling.testing.MoreAsserts.assertThrows;

public class AnyOfFiveTest {
   
   private final AnyOfFive<String, Integer, Double, Boolean, Float> a1 = AnyOfFive.withFirst("foo");
   private final AnyOfFive<String, Integer, Double, Boolean, Float> a2 = AnyOfFive.withSecond(123);
   private final AnyOfFive<String, Integer, Double, Boolean, Float> a3 = AnyOfFive.withThird(3.14159);
   private final AnyOfFive<String, Integer, Double, Boolean, Float> a4 = AnyOfFive.withFourth(true);
   private final AnyOfFive<String, Integer, Double, Boolean, Float> a5 = AnyOfFive.withFifth(100f);
   
   @Test public void hasGetEqualsHashCode() {
      checkFirst(a1, "foo");
      checkSecond(a2, 123);
      checkThird(a3, 3.14159);
      checkFourth(a4, true);
      checkFifth(a5, 100f);
   }
   
   @Test public void tryElement() {
      Possible<String> p1a = a1.tryFirst();
      assertTrue(p1a.isPresent());
      assertEquals("foo", p1a.get());

      Possible<String> p1b = a2.tryFirst();
      assertFalse(p1b.isPresent());
      assertThrows(NoSuchElementException.class, () -> p1b.get());

      Possible<String> p1c = a3.tryFirst();
      assertFalse(p1c.isPresent());
      assertThrows(NoSuchElementException.class, () -> p1c.get());

      Possible<String> p1d = a4.tryFirst();
      assertFalse(p1d.isPresent());
      assertThrows(NoSuchElementException.class, () -> p1d.get());

      Possible<String> p1e = a5.tryFirst();
      assertFalse(p1e.isPresent());
      assertThrows(NoSuchElementException.class, () -> p1e.get());

      
      Possible<Integer> p2a = a1.trySecond();
      assertFalse(p2a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2a.get());

      Possible<Integer> p2b = a2.trySecond();
      assertTrue(p2b.isPresent());
      assertEquals(123, p2b.get().intValue());

      Possible<Integer> p2c = a3.trySecond();
      assertFalse(p2c.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2c.get());

      Possible<Integer> p2d = a4.trySecond();
      assertFalse(p2d.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2d.get());

      Possible<Integer> p2e = a5.trySecond();
      assertFalse(p2e.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2e.get());

      
      Possible<Double> p3a = a1.tryThird();
      assertFalse(p3a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p3a.get());

      Possible<Double> p3b = a2.tryThird();
      assertFalse(p3b.isPresent());
      assertThrows(NoSuchElementException.class, () -> p3b.get());

      Possible<Double> p3c = a3.tryThird();
      assertTrue(p3c.isPresent());
      assertEquals(3.14159, p3c.get().doubleValue(), 0.0);

      Possible<Double> p3d = a4.tryThird();
      assertFalse(p3d.isPresent());
      assertThrows(NoSuchElementException.class, () -> p3d.get());

      Possible<Double> p3e = a5.tryThird();
      assertFalse(p3e.isPresent());
      assertThrows(NoSuchElementException.class, () -> p3e.get());


      Possible<Boolean> p4a = a1.tryFourth();
      assertFalse(p4a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p4a.get());

      Possible<Boolean> p4b = a2.tryFourth();
      assertFalse(p4b.isPresent());
      assertThrows(NoSuchElementException.class, () -> p4b.get());

      Possible<Boolean> p4c = a3.tryFourth();
      assertFalse(p4c.isPresent());
      assertThrows(NoSuchElementException.class, () -> p4c.get());

      Possible<Boolean> p4d = a4.tryFourth();
      assertTrue(p4d.isPresent());
      assertEquals(true, p4d.get());

      Possible<Boolean> p4e = a5.tryFourth();
      assertFalse(p4e.isPresent());
      assertThrows(NoSuchElementException.class, () -> p4e.get());

      
      Possible<Float> p5a = a1.tryFifth();
      assertFalse(p5a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p5a.get());

      Possible<Float> p5b = a2.tryFifth();
      assertFalse(p5b.isPresent());
      assertThrows(NoSuchElementException.class, () -> p5b.get());

      Possible<Float> p5c = a3.tryFifth();
      assertFalse(p5c.isPresent());
      assertThrows(NoSuchElementException.class, () -> p5c.get());

      Possible<Float> p5d = a4.tryFifth();
      assertFalse(p5d.isPresent());
      assertThrows(NoSuchElementException.class, () -> p5d.get());

      Possible<Float> p5e = a5.tryFifth();
      assertTrue(p5e.isPresent());
      assertEquals(100f, p5e.get(), 0f);
   }

   @Test public void mapElement() {
      checkFirst(a1.mapFirst(s -> 456), 456);
      checkFirst(a1.mapSecond(s -> 456), "foo");
      checkFirst(a1.mapThird(s -> 456), "foo");
      checkFirst(a1.mapFourth(s -> 456), "foo");
      checkFirst(a1.mapFifth(s -> 456), "foo");
      
      checkSecond(a2.mapFirst(s -> "baz"), 123);
      checkSecond(a2.mapSecond(s -> "baz"), "baz");
      checkSecond(a2.mapThird(s -> "baz"), 123);
      checkSecond(a2.mapFourth(s -> "baz"), 123);
      checkSecond(a2.mapFifth(s -> "baz"), 123);

      checkThird(a3.mapFirst(s -> -1.01), 3.14159);
      checkThird(a3.mapSecond(s -> -1.01), 3.14159);
      checkThird(a3.mapThird(s -> -1.01), -1.01);
      checkThird(a3.mapFourth(s -> -1.01), 3.14159);
      checkThird(a3.mapFifth(s -> -1.01), 3.14159);

      checkFourth(a4.mapFirst(s -> 99f), true);
      checkFourth(a4.mapSecond(s -> 99f), true);
      checkFourth(a4.mapThird(s -> 99f), true);
      checkFourth(a4.mapFourth(s -> 99f), 99f);
      checkFourth(a4.mapFifth(s -> 99f), true);

      checkFifth(a5.mapFirst(s -> false), 100f);
      checkFifth(a5.mapSecond(s -> false), 100f);
      checkFifth(a5.mapThird(s -> false), 100f);
      checkFifth(a5.mapFourth(s -> false), 100f);
      checkFifth(a5.mapFifth(s -> false), false);

      assertThrows(NullPointerException.class, () -> a1.mapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.mapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.mapThird(s -> null));
      assertThrows(NullPointerException.class, () -> a4.mapFourth(s -> null));
      assertThrows(NullPointerException.class, () -> a5.mapFifth(s -> null));
   }
   
   @Test public void flatMapElement() {
      checkSecond(a1.flatMapFirst(s -> AnyOfFive.withSecond(456)), 456);
      checkFirst(a1.flatMapSecond(s -> AnyOfFive.withFirst("baz")), "foo");
      checkFirst(a1.flatMapThird(s -> AnyOfFive.withThird(-1.01)), "foo");
      checkFirst(a1.flatMapFourth(s -> AnyOfFive.withSecond(99)), "foo");
      checkFirst(a1.flatMapFifth(s -> AnyOfFive.withFourth(false)), "foo");

      checkSecond(a2.flatMapFirst(s -> AnyOfFive.withSecond(456)), 123);
      checkFirst(a2.flatMapSecond(s -> AnyOfFive.withFirst("baz")), "baz");
      checkSecond(a2.flatMapThird(s -> AnyOfFive.withThird(-1.01)), 123);
      checkSecond(a2.flatMapFourth(s -> AnyOfFive.withSecond(99)), 123);
      checkSecond(a2.flatMapFifth(s -> AnyOfFive.withFourth(false)), 123);

      checkThird(a3.flatMapFirst(s -> AnyOfFive.withSecond(456)), 3.14159);
      checkThird(a3.flatMapSecond(s -> AnyOfFive.withFirst("baz")), 3.14159);
      checkThird(a3.flatMapThird(s -> AnyOfFive.withThird(-1.01)), -1.01);
      checkThird(a3.flatMapFourth(s -> AnyOfFive.withSecond(99)), 3.14159);
      checkThird(a3.flatMapFifth(s -> AnyOfFive.withFourth(false)), 3.14159);

      checkFourth(a4.flatMapFirst(s -> AnyOfFive.withSecond(456)), true);
      checkFourth(a4.flatMapSecond(s -> AnyOfFive.withFirst("baz")), true);
      checkFourth(a4.flatMapThird(s -> AnyOfFive.withThird(-1.01)), true);
      checkSecond(a4.flatMapFourth(s -> AnyOfFive.withSecond(99)), 99);
      checkFourth(a4.flatMapFifth(s -> AnyOfFive.withFourth(false)), true);

      checkFifth(a5.flatMapFirst(s -> AnyOfFive.withSecond(456)), 100f);
      checkFifth(a5.flatMapSecond(s -> AnyOfFive.withFirst("baz")), 100f);
      checkFifth(a5.flatMapThird(s -> AnyOfFive.withThird(-1.01)), 100f);
      checkFifth(a5.flatMapFourth(s -> AnyOfFive.withSecond(99)), 100f);
      checkFourth(a5.flatMapFifth(s -> AnyOfFive.withFourth(false)), false);

      assertThrows(NullPointerException.class, () -> a1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.flatMapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.flatMapThird(s -> null));
      assertThrows(NullPointerException.class, () -> a4.flatMapFourth(s -> null));
      assertThrows(NullPointerException.class, () -> a5.flatMapFifth(s -> null));
   }
   
   @Test public void contractElement() {
      assertEquals(AnyOfFour.withFirst(456), a1.contractFirst(s -> AnyOfFour.withFirst(456)));
      assertEquals(AnyOfFour.withFirst("foo"), a1.contractSecond(s -> AnyOfFour.withFirst("bar")));
      assertEquals(AnyOfFour.withFirst("foo"), a1.contractThird(s -> AnyOfFour.withFirst("baz")));
      assertEquals(AnyOfFour.withFirst("foo"), a1.contractFourth(s -> AnyOfFour.withThird(-1.01)));
      assertEquals(AnyOfFour.withFirst("foo"), a1.contractFifth(s -> AnyOfFour.withFourth(false)));

      assertEquals(AnyOfFour.withFirst(123), a2.contractFirst(s -> AnyOfFour.withFirst(456)));
      assertEquals(AnyOfFour.withFirst("bar"), a2.contractSecond(s -> AnyOfFour.withFirst("bar")));
      assertEquals(AnyOfFour.withSecond(123), a2.contractThird(s -> AnyOfFour.withFirst("baz")));
      assertEquals(AnyOfFour.withSecond(123), a2.contractThird(s -> AnyOfFour.withFirst("baz")));
      assertEquals(AnyOfFour.withSecond(123), a2.contractFourth(s -> AnyOfFour.withThird(-1.01)));
      assertEquals(AnyOfFour.withSecond(123), a2.contractFifth(s -> AnyOfFour.withFourth(false)));

      assertEquals(AnyOfFour.withSecond(3.14159), a3.contractFirst(s -> AnyOfFour.withFirst(456)));
      assertEquals(AnyOfFour.withSecond(3.14159), a3.contractSecond(s -> AnyOfFour.withFirst("bar")));
      assertEquals(AnyOfFour.withFirst("baz"), a3.contractThird(s -> AnyOfFour.withFirst("baz")));
      assertEquals(AnyOfFour.withThird(3.14159), a3.contractFourth(s -> AnyOfFour.withThird(-1.01)));
      assertEquals(AnyOfFour.withThird(3.14159), a3.contractFifth(s -> AnyOfFour.withFourth(false)));

      assertEquals(AnyOfFour.withThird(true), a4.contractFirst(s -> AnyOfFour.withFirst(456)));
      assertEquals(AnyOfFour.withThird(true), a4.contractSecond(s -> AnyOfFour.withFirst("bar")));
      assertEquals(AnyOfFour.withThird(true), a4.contractThird(s -> AnyOfFour.withFirst("baz")));
      assertEquals(AnyOfFour.withThird(-1.01), a4.contractFourth(s -> AnyOfFour.withThird(-1.01)));
      assertEquals(AnyOfFour.withFourth(true), a4.contractFifth(s -> AnyOfFour.withFourth(false)));

      assertEquals(AnyOfFour.withFourth(100f), a5.contractFirst(s -> AnyOfFour.withFirst(456)));
      assertEquals(AnyOfFour.withFourth(100f), a5.contractSecond(s -> AnyOfFour.withFirst("bar")));
      assertEquals(AnyOfFour.withFourth(100f), a5.contractThird(s -> AnyOfFour.withFirst("baz")));
      assertEquals(AnyOfFour.withFourth(100f), a5.contractFourth(s -> AnyOfFour.withThird(-1.01)));
      assertEquals(AnyOfFour.withFourth(false), a5.contractFifth(s -> AnyOfFour.withFourth(false)));

      assertNull(a1.contractFirst(s -> null));
      assertNull(a2.contractSecond(s -> null));
      assertNull(a3.contractThird(s -> null));
      assertNull(a4.contractFourth(s -> null));
      assertNull(a5.contractFifth(s -> null));
   }
   
   @Test public void visit() {
      VariableInt c1 = new VariableInt();
      Variable<String> r1 = new Variable<>();
      VariableInt c2 = new VariableInt();
      Variable<Integer> r2 = new Variable<>();
      VariableInt c3 = new VariableInt();
      Variable<Double> r3 = new Variable<>();
      VariableInt c4 = new VariableInt();
      Variable<Boolean> r4 = new Variable<>();
      VariableInt c5 = new VariableInt();
      Variable<Float> r5 = new Variable<>();
      Choice.VisitorOfFive<String, Integer, Double, Boolean, Float, Integer> v =
            new Choice.VisitorOfFive<String, Integer, Double, Boolean, Float, Integer>() {
               @Override
               public Integer visitFirst(String a) {
                  c1.incrementAndGet();
                  r1.set(a);
                  return 1;
               }

               @Override
               public Integer visitSecond(Integer b) {
                  c2.incrementAndGet();
                  r2.set(b);
                  return 2;
               }
               
               @Override
               public Integer visitThird(Double c) {
                  c3.incrementAndGet();
                  r3.set(c);
                  return 3;
               }
               
               @Override
               public Integer visitFourth(Boolean d) {
                  c4.incrementAndGet();
                  r4.set(d);
                  return 4;
               }

               @Override
               public Integer visitFifth(Float e) {
                  c5.incrementAndGet();
                  r5.set(e);
                  return 5;
               }
         };
            
      assertEquals(1, a1.visit(v).intValue());
      assertEquals(1, c1.get());
      assertEquals("foo", r1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c3.get());
      assertEquals(0, c4.get());
      assertEquals(0, c5.get());
      
      c1.set(0); r1.set(null);

      assertEquals(2, a2.visit(v).intValue());
      assertEquals(1, c2.get());
      assertEquals(123, r2.get().intValue());
      assertEquals(0, c1.get());
      assertEquals(0, c3.get());
      assertEquals(0, c4.get());
      assertEquals(0, c5.get());

      c2.set(0); r2.set(null);

      assertEquals(3, a3.visit(v).intValue());
      assertEquals(1, c3.get());
      assertEquals(3.14159, r3.get().doubleValue(), 0);
      assertEquals(0, c1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c4.get());
      assertEquals(0, c5.get());

      c3.set(0); r3.set(null);

      assertEquals(4, a4.visit(v).intValue());
      assertEquals(1, c4.get());
      assertEquals(true, r4.get());
      assertEquals(0, c1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c3.get());
      assertEquals(0, c5.get());

      c4.set(0); r4.set(null);

      assertEquals(5, a5.visit(v).intValue());
      assertEquals(1, c5.get());
      assertEquals(100f, r5.get(), 0f);
      assertEquals(0, c1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c3.get());
      assertEquals(0, c4.get());
   }
   
   @Test public void withNullElement() {
      assertThrows(NullPointerException.class, () -> AnyOfFive.withFirst(null));
      assertThrows(NullPointerException.class, () -> AnyOfFive.withSecond(null));
      assertThrows(NullPointerException.class, () -> AnyOfFive.withThird(null));
      assertThrows(NullPointerException.class, () -> AnyOfFive.withFourth(null));
   }

   @Test public void of() {
      checkFirst(AnyOfFive.of("foo", null, null, null, null), "foo");
      checkSecond(AnyOfFive.of(null, "foo", null, null, null), "foo");
      checkThird(AnyOfFive.of(null, null, "foo", null, null), "foo");
      checkFourth(AnyOfFive.of(null, null, null, "foo", null), "foo");
      checkFifth(AnyOfFive.of(null, null, null, null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of("foo", "foo", null, null, null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of("foo", null, "foo", null, null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, "foo", "foo", null, null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, "foo", null, "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, null, "foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of("foo", null, null, null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, "foo", null, null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, null, "foo", null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, null, null, "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of("foo", "foo", "foo", null, null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, null, "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of("foo", "foo", "foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, "foo", "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of("foo", "foo", "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.of(null, null, null, null, null));
   }

   @Test public void firstOf() {
      checkFirst(AnyOfFive.firstOf("foo", null, null, null, null), "foo");
      checkSecond(AnyOfFive.firstOf(null, "foo", null, null, null), "foo");
      checkThird(AnyOfFive.firstOf(null, null, "foo", null, null), "foo");
      checkFourth(AnyOfFive.firstOf(null, null, null, "foo", null), "foo");
      checkFifth(AnyOfFive.firstOf(null, null, null, null, "foo"), "foo");
      checkFirst(AnyOfFive.firstOf("foo", "bar", null, null, null), "foo");
      checkFirst(AnyOfFive.firstOf("foo", null, "bar", null, null), "foo");
      checkSecond(AnyOfFive.firstOf(null, "foo", "bar", null, null), "foo");
      checkSecond(AnyOfFive.firstOf(null, "foo", null, "bar", null), "foo");
      checkThird(AnyOfFive.firstOf(null, null, "foo", "bar", null), "foo");
      checkFirst(AnyOfFive.firstOf("foo", null, null, null, "bar"), "foo");
      checkSecond(AnyOfFive.firstOf(null, "foo", null, null, "bar"), "foo");
      checkThird(AnyOfFive.firstOf(null, null, "foo", null, "bar"), "foo");
      checkFourth(AnyOfFive.firstOf(null, null, null, "foo", "bar"), "foo");
      checkFirst(AnyOfFive.firstOf("foo", "bar", "baz", null, null), "foo");
      checkThird(AnyOfFive.firstOf(null, null, "foo", "bar", "baz"), "foo");
      checkFirst(AnyOfFive.firstOf("foo", "bar", "baz", "snafu", null), "foo");
      checkSecond(AnyOfFive.firstOf(null, "foo", "bar", "baz", "snafu"), "foo");
      checkFirst(AnyOfFive.firstOf("foo", "bar", "baz", "snafu", "frobnitz"), "foo");
      assertThrows(IllegalArgumentException.class, () -> AnyOfFive.firstOf(null, null, null, null, null));
   }

   private <T> void checkFirst(AnyOfFive<T, ?, ?, ?, ?> any, T value) {
      assertTrue(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertFalse(any.hasFifth());
      assertEquals(value, any.get());
      assertEquals(value, any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
      assertThrows(NoSuchElementException.class, () -> any.getFifth());
      
      assertEquals(any, AnyOfFive.withFirst(value));
      assertEquals(any.hashCode(), AnyOfFive.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFirst(new Object()));
      assertNotEquals(any, AnyOfFive.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFive.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFourth(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFifth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFifth(value).hashCode());
   }

   private <T> void checkSecond(AnyOfFive<?, T, ?, ?, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertTrue(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertFalse(any.hasFifth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertEquals(value, any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
      assertThrows(NoSuchElementException.class, () -> any.getFifth());

      assertEquals(any, AnyOfFive.withSecond(value));
      assertEquals(any.hashCode(), AnyOfFive.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFive.withSecond(new Object()));
      assertNotEquals(any, AnyOfFive.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFourth(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFifth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFifth(value).hashCode());
   }

   private <T> void checkThird(AnyOfFive<?, ?, T, ?, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertTrue(any.hasThird());
      assertFalse(any.hasFourth());
      assertFalse(any.hasFifth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertEquals(value, any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
      assertThrows(NoSuchElementException.class, () -> any.getFifth());

      assertEquals(any, AnyOfFive.withThird(value));
      assertEquals(any.hashCode(), AnyOfFive.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFive.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFive.withThird(new Object()));
      assertNotEquals(any, AnyOfFive.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFourth(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFifth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFifth(value).hashCode());
   }

   private <T> void checkFourth(AnyOfFive<?, ?, ?, T, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertTrue(any.hasFourth());
      assertFalse(any.hasFifth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertEquals(value, any.getFourth());
      assertThrows(NoSuchElementException.class, () -> any.getFifth());

      assertEquals(any, AnyOfFive.withFourth(value));
      assertEquals(any.hashCode(), AnyOfFive.withFourth(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFive.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFive.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFourth(new Object()));
      assertNotEquals(any, AnyOfFive.withFifth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFifth(value).hashCode());
   }

   private <T> void checkFifth(AnyOfFive<?, ?, ?, ?, T> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertTrue(any.hasFifth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
      assertEquals(value, any.getFifth());

      assertEquals(any, AnyOfFive.withFifth(value));
      assertEquals(any.hashCode(), AnyOfFive.withFifth(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFive.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFive.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFive.withFourth(value).hashCode());
      assertNotEquals(any, AnyOfFive.withFifth(new Object()));
   }
}
