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

public class AnyOfThreeTest {
   
   private final AnyOfThree<String, Integer, Double> a1 = AnyOfThree.withFirst("foo");
   private final AnyOfThree<String, Integer, Double> a2 = AnyOfThree.withSecond(123);
   private final AnyOfThree<String, Integer, Double> a3 = AnyOfThree.withThird(3.14159);
   
   @Test public void hasGetEqualsHashCode() {
      checkFirst(a1, "foo");
      checkSecond(a2, 123);
      checkThird(a3, 3.14159);
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

      
      Possible<Integer> p2a = a1.trySecond();
      assertFalse(p2a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2a.get());

      Possible<Integer> p2b = a2.trySecond();
      assertTrue(p2b.isPresent());
      assertEquals(123, p2b.get().intValue());

      Possible<Integer> p2c = a3.trySecond();
      assertFalse(p2c.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2c.get());

      
      Possible<Double> p3a = a1.tryThird();
      assertFalse(p3a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p3a.get());

      Possible<Double> p3b = a2.tryThird();
      assertFalse(p3b.isPresent());
      assertThrows(NoSuchElementException.class, () -> p3b.get());

      Possible<Double> p3c = a3.tryThird();
      assertTrue(p3c.isPresent());
      assertEquals(3.14159, p3c.get().doubleValue(), 0.0);
   }

   @Test public void mapElement() {
      checkFirst(a1.mapFirst(s -> 456), 456);
      checkFirst(a1.mapSecond(s -> 456), "foo");
      checkFirst(a1.mapThird(s -> 456), "foo");
      
      checkSecond(a2.mapFirst(s -> "baz"), 123);
      checkSecond(a2.mapSecond(s -> "baz"), "baz");
      checkSecond(a2.mapThird(s -> "baz"), 123);

      checkThird(a3.mapFirst(s -> -1.01), 3.14159);
      checkThird(a3.mapSecond(s -> -1.01), 3.14159);
      checkThird(a3.mapThird(s -> -1.01), -1.01);

      assertThrows(NullPointerException.class, () -> a1.mapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.mapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.mapThird(s -> null));
   }
   
   @Test public void flatMapElement() {
      checkSecond(a1.flatMapFirst(s -> AnyOfThree.withSecond(456)), 456);
      checkFirst(a1.flatMapSecond(s -> AnyOfThree.withFirst("baz")), "foo");
      checkFirst(a1.flatMapThird(s -> AnyOfThree.withThird(-1.01)), "foo");

      checkSecond(a2.flatMapFirst(s -> AnyOfThree.withSecond(456)), 123);
      checkFirst(a2.flatMapSecond(s -> AnyOfThree.withFirst("baz")), "baz");
      checkSecond(a2.flatMapThird(s -> AnyOfThree.withThird(-1.01)), 123);

      checkThird(a3.flatMapFirst(s -> AnyOfThree.withSecond(456)), 3.14159);
      checkThird(a3.flatMapSecond(s -> AnyOfThree.withFirst("baz")), 3.14159);
      checkThird(a3.flatMapThird(s -> AnyOfThree.withThird(-1.01)), -1.01);

      assertThrows(NullPointerException.class, () -> a1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.flatMapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.flatMapThird(s -> null));
   }
   
   @Test public void expandElement() {
      checkAnyOfFourSecond(a1.expandFirst(), "foo");
      checkAnyOfFourFirst(a1.expandSecond(), "foo");
      checkAnyOfFourFirst(a1.expandThird(), "foo");
      checkAnyOfFourFirst(a1.expandFourth(), "foo");
      
      checkAnyOfFourThird(a2.expandFirst(), 123);
      checkAnyOfFourThird(a2.expandSecond(), 123);
      checkAnyOfFourSecond(a2.expandThird(), 123);
      checkAnyOfFourSecond(a2.expandFourth(), 123);

      checkAnyOfFourFourth(a3.expandFirst(), 3.14159);
      checkAnyOfFourFourth(a3.expandSecond(), 3.14159);
      checkAnyOfFourFourth(a3.expandThird(), 3.14159);
      checkAnyOfFourThird(a3.expandFourth(), 3.14159);
   }
   
   @Test public void contractElement() {
      assertEquals(Either.withFirst(456), a1.contractFirst(s -> Either.withFirst(456)));
      assertEquals(Either.withFirst("foo"), a1.contractSecond(s -> Either.withFirst("bar")));
      assertEquals(Either.withFirst("foo"), a1.contractThird(s -> Either.withFirst("baz")));

      assertEquals(Either.withFirst(123), a2.contractFirst(s -> Either.withFirst(456)));
      assertEquals(Either.withFirst("bar"), a2.contractSecond(s -> Either.withFirst("bar")));
      assertEquals(Either.withSecond(123), a2.contractThird(s -> Either.withFirst("baz")));

      assertEquals(Either.withSecond(3.14159), a3.contractFirst(s -> Either.withFirst(456)));
      assertEquals(Either.withSecond(3.14159), a3.contractSecond(s -> Either.withFirst("bar")));
      assertEquals(Either.withFirst("baz"), a3.contractThird(s -> Either.withFirst("baz")));

      assertNull(a1.contractFirst(s -> null));
      assertNull(a2.contractSecond(s -> null));
      assertNull(a3.contractThird(s -> null));
   }
   
   @Test public void visit() {
      VariableInt c1 = new VariableInt();
      Variable<String> r1 = new Variable<>();
      VariableInt c2 = new VariableInt();
      Variable<Integer> r2 = new Variable<>();
      VariableInt c3 = new VariableInt();
      Variable<Double> r3 = new Variable<>();
      Choice.VisitorOfThree<String, Integer, Double, Integer> v =
            new Choice.VisitorOfThree<String, Integer, Double, Integer>() {
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
            };
            
      assertEquals(1, a1.visit(v).intValue());
      assertEquals(1, c1.get());
      assertEquals("foo", r1.get());
      assertEquals(0, c2.get());
      assertEquals(0, c3.get());
      
      c1.set(0); r1.set(null);

      assertEquals(2, a2.visit(v).intValue());
      assertEquals(1, c2.get());
      assertEquals(123, r2.get().intValue());
      assertEquals(0, c1.get());
      assertEquals(0, c3.get());

      c2.set(0); r2.set(null);

      assertEquals(3, a3.visit(v).intValue());
      assertEquals(1, c3.get());
      assertEquals(3.14159, r3.get().doubleValue(), 0);
      assertEquals(0, c1.get());
      assertEquals(0, c2.get());
   }
   
   @Test public void withNullElement() {
      assertThrows(NullPointerException.class, () -> AnyOfThree.withFirst(null));
      assertThrows(NullPointerException.class, () -> AnyOfThree.withSecond(null));
      assertThrows(NullPointerException.class, () -> AnyOfThree.withThird(null));
   }

   @Test public void of() {
      checkFirst(AnyOfThree.of("foo", null, null), "foo");
      checkSecond(AnyOfThree.of(null, "foo", null), "foo");
      checkThird(AnyOfThree.of(null, null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> AnyOfThree.of("foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> AnyOfThree.of("foo", null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfThree.of(null, "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfThree.of("foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> AnyOfThree.of(null, null, null));
   }

   @Test public void firstOf() {
      checkFirst(AnyOfThree.firstOf("foo", null, null), "foo");
      checkSecond(AnyOfThree.firstOf(null, "foo", null), "foo");
      checkThird(AnyOfThree.firstOf(null, null, "foo"), "foo");
      checkFirst(AnyOfThree.firstOf("foo", "bar", null), "foo");
      checkFirst(AnyOfThree.firstOf("foo", null, "bar"), "foo");
      checkSecond(AnyOfThree.firstOf(null, "foo", "bar"), "foo");
      checkFirst(AnyOfThree.firstOf("foo", "bar", "baz"), "foo");
      assertThrows(IllegalArgumentException.class, () -> AnyOfThree.firstOf(null, null, null));
   }

   private <T> void checkFirst(AnyOfThree<T, ?, ?> any, T value) {
      assertTrue(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertEquals(value, any.get());
      assertEquals(value, any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      
      assertEquals(any, AnyOfThree.withFirst(value));
      assertEquals(any.hashCode(), AnyOfThree.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfThree.withFirst(new Object()));
      assertNotEquals(any, AnyOfThree.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfThree.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfThree.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfThree.withThird(value).hashCode());
   }

   private <T> void checkSecond(AnyOfThree<?, T, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertTrue(any.hasSecond());
      assertFalse(any.hasThird());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertEquals(value, any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());

      assertEquals(any, AnyOfThree.withSecond(value));
      assertEquals(any.hashCode(), AnyOfThree.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfThree.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfThree.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfThree.withSecond(new Object()));
      assertNotEquals(any, AnyOfThree.withThird(value));
      assertNotEquals(any.hashCode(), AnyOfThree.withThird(value).hashCode());
   }

   private <T> void checkThird(AnyOfThree<?, ?, T> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertTrue(any.hasThird());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertEquals(value, any.getThird());

      assertEquals(any, AnyOfThree.withThird(value));
      assertEquals(any.hashCode(), AnyOfThree.withThird(value).hashCode());
      assertNotEquals(any, AnyOfThree.withFirst(value));
      assertNotEquals(any.hashCode(), AnyOfThree.withFirst(value).hashCode());
      assertNotEquals(any, AnyOfThree.withSecond(value));
      assertNotEquals(any.hashCode(), AnyOfThree.withSecond(value).hashCode());
      assertNotEquals(any, AnyOfThree.withThird(new Object()));
   }

   private <T> void checkAnyOfFourFirst(AnyOfFour<T, ?, ?, ?> any, T value) {
      assertTrue(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertEquals(value, any.get());
      assertEquals(value, any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
   }

   private <T> void checkAnyOfFourSecond(AnyOfFour<?, T, ?, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertTrue(any.hasSecond());
      assertFalse(any.hasThird());
      assertFalse(any.hasFourth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertEquals(value, any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
   }

   private <T> void checkAnyOfFourThird(AnyOfFour<?, ?, T, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertTrue(any.hasThird());
      assertFalse(any.hasFourth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertEquals(value, any.getThird());
      assertThrows(NoSuchElementException.class, () -> any.getFourth());
   }

   private <T> void checkAnyOfFourFourth(AnyOfFour<?, ?, ?, T> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertTrue(any.hasFourth());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
      assertEquals(value, any.getFourth());
   }
}
