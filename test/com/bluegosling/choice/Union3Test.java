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

public class Union3Test {
   
   private final Union3<String, Integer, Double> a1 = Union3.withFirst("foo");
   private final Union3<String, Integer, Double> a2 = Union3.withSecond(123);
   private final Union3<String, Integer, Double> a3 = Union3.withThird(3.14159);
   
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
      checkFirst(a1.mapFirst(s -> null), null);
      checkFirst(a1.mapSecond(s -> 456), "foo");
      checkFirst(a1.mapThird(s -> 456), "foo");
      
      checkSecond(a2.mapFirst(s -> "baz"), 123);
      checkSecond(a2.mapSecond(s -> "baz"), "baz");
      checkSecond(a2.mapSecond(s -> null), null);
      checkSecond(a2.mapThird(s -> "baz"), 123);

      checkThird(a3.mapFirst(s -> -1.01), 3.14159);
      checkThird(a3.mapSecond(s -> -1.01), 3.14159);
      checkThird(a3.mapThird(s -> -1.01), -1.01);
      checkThird(a3.mapThird(s -> null), null);
   }
   
   @Test public void flatMapElement() {
      checkSecond(a1.flatMapFirst(s -> Union3.withSecond(456)), 456);
      checkFirst(a1.flatMapSecond(s -> Union3.withFirst("baz")), "foo");
      checkFirst(a1.flatMapThird(s -> Union3.withThird(-1.01)), "foo");

      checkSecond(a2.flatMapFirst(s -> Union3.withSecond(456)), 123);
      checkFirst(a2.flatMapSecond(s -> Union3.withFirst("baz")), "baz");
      checkSecond(a2.flatMapThird(s -> Union3.withThird(-1.01)), 123);

      checkThird(a3.flatMapFirst(s -> Union3.withSecond(456)), 3.14159);
      checkThird(a3.flatMapSecond(s -> Union3.withFirst("baz")), 3.14159);
      checkThird(a3.flatMapThird(s -> Union3.withThird(-1.01)), -1.01);

      assertThrows(NullPointerException.class, () -> a1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.flatMapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.flatMapThird(s -> null));
   }
   
   @Test public void expandElement() {
      checkUnion4Second(a1.expandFirst(), "foo");
      checkUnion4First(a1.expandSecond(), "foo");
      checkUnion4First(a1.expandThird(), "foo");
      checkUnion4First(a1.expandFourth(), "foo");
      
      checkUnion4Third(a2.expandFirst(), 123);
      checkUnion4Third(a2.expandSecond(), 123);
      checkUnion4Second(a2.expandThird(), 123);
      checkUnion4Second(a2.expandFourth(), 123);

      checkUnion4Fourth(a3.expandFirst(), 3.14159);
      checkUnion4Fourth(a3.expandSecond(), 3.14159);
      checkUnion4Fourth(a3.expandThird(), 3.14159);
      checkUnion4Third(a3.expandFourth(), 3.14159);
   }
   
   @Test public void contractElement() {
      assertEquals(Union2.withFirst(456), a1.contractFirst(s -> Union2.withFirst(456)));
      assertEquals(Union2.withFirst("foo"), a1.contractSecond(s -> Union2.withFirst("bar")));
      assertEquals(Union2.withFirst("foo"), a1.contractThird(s -> Union2.withFirst("baz")));

      assertEquals(Union2.withFirst(123), a2.contractFirst(s -> Union2.withFirst(456)));
      assertEquals(Union2.withFirst("bar"), a2.contractSecond(s -> Union2.withFirst("bar")));
      assertEquals(Union2.withSecond(123), a2.contractThird(s -> Union2.withFirst("baz")));

      assertEquals(Union2.withSecond(3.14159), a3.contractFirst(s -> Union2.withFirst(456)));
      assertEquals(Union2.withSecond(3.14159), a3.contractSecond(s -> Union2.withFirst("bar")));
      assertEquals(Union2.withFirst("baz"), a3.contractThird(s -> Union2.withFirst("baz")));

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
   
   @Test public void of() {
      checkFirst(Union3.of("foo", null, null), "foo");
      checkSecond(Union3.of(null, "foo", null), "foo");
      checkThird(Union3.of(null, null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> Union3.of("foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> Union3.of("foo", null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union3.of(null, "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union3.of("foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union3.of(null, null, null));
   }

   @Test public void firstOf() {
      checkFirst(Union3.firstOf("foo", null, null), "foo");
      checkSecond(Union3.firstOf(null, "foo", null), "foo");
      checkThird(Union3.firstOf(null, null, "foo"), "foo");
      checkFirst(Union3.firstOf("foo", "bar", null), "foo");
      checkFirst(Union3.firstOf("foo", null, "bar"), "foo");
      checkSecond(Union3.firstOf(null, "foo", "bar"), "foo");
      checkFirst(Union3.firstOf("foo", "bar", "baz"), "foo");
      checkFirst(Union3.firstOf(null, null, null), null);
   }

   private <T> void checkFirst(Union3<T, ?, ?> union, T value) {
      assertTrue(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertEquals(value, union.get());
      assertEquals(value, union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      
      assertEquals(union, Union3.withFirst(value));
      assertEquals(union.hashCode(), Union3.withFirst(value).hashCode());
      assertNotEquals(union, Union3.withFirst(new Object()));
      assertNotEquals(union, Union3.withSecond(value));
      assertNotEquals(union.hashCode(), Union3.withSecond(value).hashCode());
      assertNotEquals(union, Union3.withThird(value));
      assertNotEquals(union.hashCode(), Union3.withThird(value).hashCode());
   }

   private <T> void checkSecond(Union3<?, T, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertTrue(union.hasSecond());
      assertFalse(union.hasThird());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertEquals(value, union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());

      assertEquals(union, Union3.withSecond(value));
      assertEquals(union.hashCode(), Union3.withSecond(value).hashCode());
      assertNotEquals(union, Union3.withFirst(value));
      assertNotEquals(union.hashCode(), Union3.withFirst(value).hashCode());
      assertNotEquals(union, Union3.withSecond(new Object()));
      assertNotEquals(union, Union3.withThird(value));
      assertNotEquals(union.hashCode(), Union3.withThird(value).hashCode());
   }

   private <T> void checkThird(Union3<?, ?, T> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertTrue(union.hasThird());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertEquals(value, union.getThird());

      assertEquals(union, Union3.withThird(value));
      assertEquals(union.hashCode(), Union3.withThird(value).hashCode());
      assertNotEquals(union, Union3.withFirst(value));
      assertNotEquals(union.hashCode(), Union3.withFirst(value).hashCode());
      assertNotEquals(union, Union3.withSecond(value));
      assertNotEquals(union.hashCode(), Union3.withSecond(value).hashCode());
      assertNotEquals(union, Union3.withThird(new Object()));
   }

   private <T> void checkUnion4First(Union4<T, ?, ?, ?> union, T value) {
      assertTrue(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertEquals(value, union.get());
      assertEquals(value, union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
   }

   private <T> void checkUnion4Second(Union4<?, T, ?, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertTrue(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertEquals(value, union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
   }

   private <T> void checkUnion4Third(Union4<?, ?, T, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertTrue(union.hasThird());
      assertFalse(union.hasFourth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertEquals(value, union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
   }

   private <T> void checkUnion4Fourth(Union4<?, ?, ?, T> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertTrue(union.hasFourth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertEquals(value, union.getFourth());
   }
}
