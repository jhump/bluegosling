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

public class AnyOfFourTest {
   
   private final AnyOfFour<String, Integer, Double, Boolean> a1 = AnyOfFour.withFirst("foo");
   private final AnyOfFour<String, Integer, Double, Boolean> a2 = AnyOfFour.withSecond(123);
   private final AnyOfFour<String, Integer, Double, Boolean> a3 = AnyOfFour.withThird(3.14159);
   private final AnyOfFour<String, Integer, Double, Boolean> a4 = AnyOfFour.withFourth(true);
   
   @Test public void hasGetEqualsHashCode() {
      checkFirst(a1, "foo");
      checkSecond(a2, 123);
      checkThird(a3, 3.14159);
      checkFourth(a4, true);
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
   }

   @Test public void mapElement() {
      checkFirst(a1.mapFirst(s -> 456), 456);
      checkFirst(a1.mapSecond(s -> 456), "foo");
      checkFirst(a1.mapThird(s -> 456), "foo");
      checkFirst(a1.mapFourth(s -> 456), "foo");
      
      checkSecond(a2.mapFirst(s -> "baz"), 123);
      checkSecond(a2.mapSecond(s -> "baz"), "baz");
      checkSecond(a2.mapThird(s -> "baz"), 123);
      checkSecond(a2.mapFourth(s -> "baz"), 123);

      checkThird(a3.mapFirst(s -> -1.01), 3.14159);
      checkThird(a3.mapSecond(s -> -1.01), 3.14159);
      checkThird(a3.mapThird(s -> -1.01), -1.01);
      checkThird(a3.mapFourth(s -> -1.01), 3.14159);

      checkFourth(a4.mapFirst(s -> 99f), true);
      checkFourth(a4.mapSecond(s -> 99f), true);
      checkFourth(a4.mapThird(s -> 99f), true);
      checkFourth(a4.mapFourth(s -> 99f), 99f);

      assertThrows(NullPointerException.class, () -> a1.mapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.mapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.mapThird(s -> null));
      assertThrows(NullPointerException.class, () -> a4.mapFourth(s -> null));
   }
   
   @Test public void flatMapElement() {
      checkSecond(a1.flatMapFirst(s -> AnyOfFour.withSecond(456)), 456);
      checkFirst(a1.flatMapSecond(s -> AnyOfFour.withFirst("baz")), "foo");
      checkFirst(a1.flatMapThird(s -> AnyOfFour.withThird(-1.01)), "foo");
      checkFirst(a1.flatMapFourth(s -> AnyOfFour.withSecond(99)), "foo");

      checkSecond(a2.flatMapFirst(s -> AnyOfFour.withSecond(456)), 123);
      checkFirst(a2.flatMapSecond(s -> AnyOfFour.withFirst("baz")), "baz");
      checkSecond(a2.flatMapThird(s -> AnyOfFour.withThird(-1.01)), 123);
      checkSecond(a2.flatMapFourth(s -> AnyOfFour.withSecond(99)), 123);

      checkThird(a3.flatMapFirst(s -> AnyOfFour.withSecond(456)), 3.14159);
      checkThird(a3.flatMapSecond(s -> AnyOfFour.withFirst("baz")), 3.14159);
      checkThird(a3.flatMapThird(s -> AnyOfFour.withThird(-1.01)), -1.01);
      checkThird(a3.flatMapFourth(s -> AnyOfFour.withSecond(99)), 3.14159);

      checkFourth(a4.flatMapFirst(s -> AnyOfFour.withSecond(456)), true);
      checkFourth(a4.flatMapSecond(s -> AnyOfFour.withFirst("baz")), true);
      checkFourth(a4.flatMapThird(s -> AnyOfFour.withThird(-1.01)), true);
      checkSecond(a4.flatMapFourth(s -> AnyOfFour.withSecond(99)), 99);

      assertThrows(NullPointerException.class, () -> a1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.flatMapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.flatMapThird(s -> null));
      assertThrows(NullPointerException.class, () -> a4.flatMapFourth(s -> null));
   }
   
   @Test public void expandElement() {
      checkAnyOfFiveSecond(a1.expandFirst(), "foo");
      checkAnyOfFiveFirst(a1.expandSecond(), "foo");
      checkAnyOfFiveFirst(a1.expandThird(), "foo");
      checkAnyOfFiveFirst(a1.expandFourth(), "foo");
      checkAnyOfFiveFirst(a1.expandFifth(), "foo");
      
      checkAnyOfFiveThird(a2.expandFirst(), 123);
      checkAnyOfFiveThird(a2.expandSecond(), 123);
      checkAnyOfFiveSecond(a2.expandThird(), 123);
      checkAnyOfFiveSecond(a2.expandFourth(), 123);
      checkAnyOfFiveSecond(a2.expandFifth(), 123);

      checkAnyOfFiveFourth(a3.expandFirst(), 3.14159);
      checkAnyOfFiveFourth(a3.expandSecond(), 3.14159);
      checkAnyOfFiveFourth(a3.expandThird(), 3.14159);
      checkAnyOfFiveThird(a3.expandFourth(), 3.14159);
      checkAnyOfFiveThird(a3.expandFifth(), 3.14159);

      checkAnyOfFiveFifth(a4.expandFirst(), true);
      checkAnyOfFiveFifth(a4.expandSecond(), true);
      checkAnyOfFiveFifth(a4.expandThird(), true);
      checkAnyOfFiveFifth(a4.expandFourth(), true);
      checkAnyOfFiveFourth(a4.expandFifth(), true);
   }
   
   @Test public void contractElement() {
      assertEquals(AnyOfThree.withFirst(456), a1.contractFirst(s -> AnyOfThree.withFirst(456)));
      assertEquals(AnyOfThree.withFirst("foo"), a1.contractSecond(s -> AnyOfThree.withFirst("bar")));
      assertEquals(AnyOfThree.withFirst("foo"), a1.contractThird(s -> AnyOfThree.withFirst("baz")));
      assertEquals(AnyOfThree.withFirst("foo"), a1.contractFourth(s -> AnyOfThree.withThird(-1.01)));

      assertEquals(AnyOfThree.withFirst(123), a2.contractFirst(s -> AnyOfThree.withFirst(456)));
      assertEquals(AnyOfThree.withFirst("bar"), a2.contractSecond(s -> AnyOfThree.withFirst("bar")));
      assertEquals(AnyOfThree.withSecond(123), a2.contractThird(s -> AnyOfThree.withFirst("baz")));
      assertEquals(AnyOfThree.withSecond(123), a2.contractThird(s -> AnyOfThree.withFirst("baz")));
      assertEquals(AnyOfThree.withSecond(123), a2.contractFourth(s -> AnyOfThree.withThird(-1.01)));

      assertEquals(AnyOfThree.withSecond(3.14159), a3.contractFirst(s -> AnyOfThree.withFirst(456)));
      assertEquals(AnyOfThree.withSecond(3.14159), a3.contractSecond(s -> AnyOfThree.withFirst("bar")));
      assertEquals(AnyOfThree.withFirst("baz"), a3.contractThird(s -> AnyOfThree.withFirst("baz")));
      assertEquals(AnyOfThree.withThird(3.14159), a3.contractFourth(s -> AnyOfThree.withThird(-1.01)));

      assertEquals(AnyOfThree.withThird(true), a4.contractFirst(s -> AnyOfThree.withFirst(456)));
      assertEquals(AnyOfThree.withThird(true), a4.contractSecond(s -> AnyOfThree.withFirst("bar")));
      assertEquals(AnyOfThree.withThird(true), a4.contractThird(s -> AnyOfThree.withFirst("baz")));
      assertEquals(AnyOfThree.withThird(-1.01), a4.contractFourth(s -> AnyOfThree.withThird(-1.01)));

      assertNull(a1.contractFirst(s -> null));
      assertNull(a2.contractSecond(s -> null));
      assertNull(a3.contractThird(s -> null));
      assertNull(a4.contractFourth(s -> null));
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
      Choice.VisitorOfFour<String, Integer, Double, Boolean, Integer> v =
            new Choice.VisitorOfFour<String, Integer, Double, Boolean, Integer>() {
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
            };
            
      assertEquals(1, a1.visit(v).intValue());
      assertEquals(1, c1.get());
      assertEquals("foo", r1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c3.get());
      assertEquals(0, c4.get());
      
      c1.set(0); r1.set(null);

      assertEquals(2, a2.visit(v).intValue());
      assertEquals(1, c2.get());
      assertEquals(123, r2.get().intValue());
      assertEquals(0, c1.get());
      assertEquals(0, c3.get());
      assertEquals(0, c4.get());

      c2.set(0); r2.set(null);

      assertEquals(3, a3.visit(v).intValue());
      assertEquals(1, c3.get());
      assertEquals(3.14159, r3.get().doubleValue(), 0);
      assertEquals(0, c1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c4.get());

      c3.set(0); r3.set(null);

      assertEquals(4, a4.visit(v).intValue());
      assertEquals(1, c4.get());
      assertEquals(true, r4.get());
      assertEquals(0, c1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c3.get());
   }
   
   @Test public void withNullElement() {
      assertThrows(NullPointerException.class, () -> AnyOfFour.withFirst(null));
      assertThrows(NullPointerException.class, () -> AnyOfFour.withSecond(null));
      assertThrows(NullPointerException.class, () -> AnyOfFour.withThird(null));
      assertThrows(NullPointerException.class, () -> AnyOfFour.withFourth(null));
   }

   @Test public void of() {
      checkFirst(AnyOfFour.of("foo", null, null, null), "foo");
      checkSecond(AnyOfFour.of(null, "foo", null, null), "foo");
      checkThird(AnyOfFour.of(null, null, "foo", null), "foo");
      checkFourth(AnyOfFour.of(null, null, null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of("foo", "foo", null, null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of("foo", null, "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of(null, "foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of("foo", null, null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of(null, "foo", null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of(null, null, "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of("foo", "foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of(null, "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of("foo", "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.of(null, null, null, null));
   }

   @Test public void firstOf() {
      checkFirst(AnyOfFour.firstOf("foo", null, null, null), "foo");
      checkSecond(AnyOfFour.firstOf(null, "foo", null, null), "foo");
      checkThird(AnyOfFour.firstOf(null, null, "foo", null), "foo");
      checkFourth(AnyOfFour.firstOf(null, null, null, "foo"), "foo");
      checkFirst(AnyOfFour.firstOf("foo", "bar", null, null), "foo");
      checkFirst(AnyOfFour.firstOf("foo", null, "bar", null), "foo");
      checkSecond(AnyOfFour.firstOf(null, "foo", "bar", null), "foo");
      checkFirst(AnyOfFour.firstOf("foo", null, null, "bar"), "foo");
      checkSecond(AnyOfFour.firstOf(null, "foo", null, "bar"), "foo");
      checkThird(AnyOfFour.firstOf(null, null, "foo", "bar"), "foo");
      checkFirst(AnyOfFour.firstOf("foo", "bar", "baz", null), "foo");
      checkSecond(AnyOfFour.firstOf(null, "foo", "bar", "baz"), "foo");
      checkFirst(AnyOfFour.firstOf("foo", "bar", "baz", "snafu"), "foo");
      assertThrows(IllegalArgumentException.class, () -> AnyOfFour.firstOf(null, null, null, null));
   }

   private <T> void checkFirst(AnyOfFour<T, ?, ?, ?> any, T value) {
      assertTrue(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertEquals(value, any.get());
      assertEquals(value, any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
      
      assertEquals(any, AnyOfFour.withFirst(value));
      assertEquals(any.hashCode(), AnyOfFour.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFirst(new Object()));
      assertNotEquals(any, AnyOfFour.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFour.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withFourth(value).hashCode());
   }

   private <T> void checkSecond(AnyOfFour<?, T, ?, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertTrue(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertEquals(value, any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());

      assertEquals(any, AnyOfFour.withSecond(value));
      assertEquals(any.hashCode(), AnyOfFour.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFour.withSecond(new Object()));
      assertNotEquals(any, AnyOfFour.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withFourth(value).hashCode());
   }

   private <T> void checkThird(AnyOfFour<?, ?, T, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertTrue(any.hasThird());
      assertFalse(any.hasFourth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertEquals(value, any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());

      assertEquals(any, AnyOfFour.withThird(value));
      assertEquals(any.hashCode(), AnyOfFour.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFour.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFour.withThird(new Object()));
      assertNotEquals(any, AnyOfFour.withFourth(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withFourth(value).hashCode());
   }

   private <T> void checkFourth(AnyOfFour<?, ?, ?, T> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertTrue(any.hasFourth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertEquals(value, any.getFourth());

      assertEquals(any, AnyOfFour.withFourth(value));
      assertEquals(any.hashCode(), AnyOfFour.withFourth(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfFour.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfFour.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfFour.withThird(value).hashCode());
      assertNotEquals(any, AnyOfFour.withFourth(new Object()));
   }

   private <T> void checkAnyOfFiveFirst(AnyOfFive<T, ?, ?, ?, ?> any, T value) {
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
   }

   private <T> void checkAnyOfFiveSecond(AnyOfFive<?, T, ?, ?, ?> any, T value) {
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
   }

   private <T> void checkAnyOfFiveThird(AnyOfFive<?, ?, T, ?, ?> any, T value) {
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
   }

   private <T> void checkAnyOfFiveFourth(AnyOfFive<?, ?, ?, T, ?> any, T value) {
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
   }

   private <T> void checkAnyOfFiveFifth(AnyOfFive<?, ?, ?, ?, T> any, T value) {
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
   }
}
