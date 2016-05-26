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

public class Union4Test {
   
   private final Union4<String, Integer, Double, Boolean> a1 = Union4.withFirst("foo");
   private final Union4<String, Integer, Double, Boolean> a2 = Union4.withSecond(123);
   private final Union4<String, Integer, Double, Boolean> a3 = Union4.withThird(3.14159);
   private final Union4<String, Integer, Double, Boolean> a4 = Union4.withFourth(true);
   
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
      checkFirst(a1.mapFirst(s -> null), null);
      checkFirst(a1.mapSecond(s -> 456), "foo");
      checkFirst(a1.mapThird(s -> 456), "foo");
      checkFirst(a1.mapFourth(s -> 456), "foo");
      
      checkSecond(a2.mapFirst(s -> "baz"), 123);
      checkSecond(a2.mapSecond(s -> "baz"), "baz");
      checkSecond(a2.mapSecond(s -> null), null);
      checkSecond(a2.mapThird(s -> "baz"), 123);
      checkSecond(a2.mapFourth(s -> "baz"), 123);

      checkThird(a3.mapFirst(s -> -1.01), 3.14159);
      checkThird(a3.mapSecond(s -> -1.01), 3.14159);
      checkThird(a3.mapThird(s -> -1.01), -1.01);
      checkThird(a3.mapThird(s -> null), null);
      checkThird(a3.mapFourth(s -> -1.01), 3.14159);

      checkFourth(a4.mapFirst(s -> 99f), true);
      checkFourth(a4.mapSecond(s -> 99f), true);
      checkFourth(a4.mapThird(s -> 99f), true);
      checkFourth(a4.mapFourth(s -> 99f), 99f);
      checkFourth(a4.mapFourth(s -> null), null);
   }
   
   @Test public void flatMapElement() {
      checkSecond(a1.flatMapFirst(s -> Union4.withSecond(456)), 456);
      checkFirst(a1.flatMapSecond(s -> Union4.withFirst("baz")), "foo");
      checkFirst(a1.flatMapThird(s -> Union4.withThird(-1.01)), "foo");
      checkFirst(a1.flatMapFourth(s -> Union4.withSecond(99)), "foo");

      checkSecond(a2.flatMapFirst(s -> Union4.withSecond(456)), 123);
      checkFirst(a2.flatMapSecond(s -> Union4.withFirst("baz")), "baz");
      checkSecond(a2.flatMapThird(s -> Union4.withThird(-1.01)), 123);
      checkSecond(a2.flatMapFourth(s -> Union4.withSecond(99)), 123);

      checkThird(a3.flatMapFirst(s -> Union4.withSecond(456)), 3.14159);
      checkThird(a3.flatMapSecond(s -> Union4.withFirst("baz")), 3.14159);
      checkThird(a3.flatMapThird(s -> Union4.withThird(-1.01)), -1.01);
      checkThird(a3.flatMapFourth(s -> Union4.withSecond(99)), 3.14159);

      checkFourth(a4.flatMapFirst(s -> Union4.withSecond(456)), true);
      checkFourth(a4.flatMapSecond(s -> Union4.withFirst("baz")), true);
      checkFourth(a4.flatMapThird(s -> Union4.withThird(-1.01)), true);
      checkSecond(a4.flatMapFourth(s -> Union4.withSecond(99)), 99);

      assertThrows(NullPointerException.class, () -> a1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> a2.flatMapSecond(s -> null));
      assertThrows(NullPointerException.class, () -> a3.flatMapThird(s -> null));
      assertThrows(NullPointerException.class, () -> a4.flatMapFourth(s -> null));
   }
   
   @Test public void expandElement() {
      checkUnion5Second(a1.expandFirst(), "foo");
      checkUnion5First(a1.expandSecond(), "foo");
      checkUnion5First(a1.expandThird(), "foo");
      checkUnion5First(a1.expandFourth(), "foo");
      checkUnion5First(a1.expandFifth(), "foo");
      
      checkUnion5Third(a2.expandFirst(), 123);
      checkUnion5Third(a2.expandSecond(), 123);
      checkUnion5Second(a2.expandThird(), 123);
      checkUnion5Second(a2.expandFourth(), 123);
      checkUnion5Second(a2.expandFifth(), 123);

      checkUnion5Fourth(a3.expandFirst(), 3.14159);
      checkUnion5Fourth(a3.expandSecond(), 3.14159);
      checkUnion5Fourth(a3.expandThird(), 3.14159);
      checkUnion5Third(a3.expandFourth(), 3.14159);
      checkUnion5Third(a3.expandFifth(), 3.14159);

      checkUnion5Fifth(a4.expandFirst(), true);
      checkUnion5Fifth(a4.expandSecond(), true);
      checkUnion5Fifth(a4.expandThird(), true);
      checkUnion5Fifth(a4.expandFourth(), true);
      checkUnion5Fourth(a4.expandFifth(), true);
   }
   
   @Test public void contractElement() {
      assertEquals(Union3.withFirst(456), a1.contractFirst(s -> Union3.withFirst(456)));
      assertEquals(Union3.withFirst("foo"), a1.contractSecond(s -> Union3.withFirst("bar")));
      assertEquals(Union3.withFirst("foo"), a1.contractThird(s -> Union3.withFirst("baz")));
      assertEquals(Union3.withFirst("foo"), a1.contractFourth(s -> Union3.withThird(-1.01)));

      assertEquals(Union3.withFirst(123), a2.contractFirst(s -> Union3.withFirst(456)));
      assertEquals(Union3.withFirst("bar"), a2.contractSecond(s -> Union3.withFirst("bar")));
      assertEquals(Union3.withSecond(123), a2.contractThird(s -> Union3.withFirst("baz")));
      assertEquals(Union3.withSecond(123), a2.contractThird(s -> Union3.withFirst("baz")));
      assertEquals(Union3.withSecond(123), a2.contractFourth(s -> Union3.withThird(-1.01)));

      assertEquals(Union3.withSecond(3.14159), a3.contractFirst(s -> Union3.withFirst(456)));
      assertEquals(Union3.withSecond(3.14159), a3.contractSecond(s -> Union3.withFirst("bar")));
      assertEquals(Union3.withFirst("baz"), a3.contractThird(s -> Union3.withFirst("baz")));
      assertEquals(Union3.withThird(3.14159), a3.contractFourth(s -> Union3.withThird(-1.01)));

      assertEquals(Union3.withThird(true), a4.contractFirst(s -> Union3.withFirst(456)));
      assertEquals(Union3.withThird(true), a4.contractSecond(s -> Union3.withFirst("bar")));
      assertEquals(Union3.withThird(true), a4.contractThird(s -> Union3.withFirst("baz")));
      assertEquals(Union3.withThird(-1.01), a4.contractFourth(s -> Union3.withThird(-1.01)));

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
   
   @Test public void of() {
      checkFirst(Union4.of("foo", null, null, null), "foo");
      checkSecond(Union4.of(null, "foo", null, null), "foo");
      checkThird(Union4.of(null, null, "foo", null), "foo");
      checkFourth(Union4.of(null, null, null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> Union4.of("foo", "foo", null, null));
      assertThrows(IllegalArgumentException.class, () -> Union4.of("foo", null, "foo", null));
      assertThrows(IllegalArgumentException.class, () -> Union4.of(null, "foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> Union4.of("foo", null, null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union4.of(null, "foo", null, "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union4.of(null, null, "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union4.of("foo", "foo", "foo", null));
      assertThrows(IllegalArgumentException.class, () -> Union4.of(null, "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union4.of("foo", "foo", "foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union4.of(null, null, null, null));
   }

   @Test public void firstOf() {
      checkFirst(Union4.firstOf("foo", null, null, null), "foo");
      checkSecond(Union4.firstOf(null, "foo", null, null), "foo");
      checkThird(Union4.firstOf(null, null, "foo", null), "foo");
      checkFourth(Union4.firstOf(null, null, null, "foo"), "foo");
      checkFirst(Union4.firstOf("foo", "bar", null, null), "foo");
      checkFirst(Union4.firstOf("foo", null, "bar", null), "foo");
      checkSecond(Union4.firstOf(null, "foo", "bar", null), "foo");
      checkFirst(Union4.firstOf("foo", null, null, "bar"), "foo");
      checkSecond(Union4.firstOf(null, "foo", null, "bar"), "foo");
      checkThird(Union4.firstOf(null, null, "foo", "bar"), "foo");
      checkFirst(Union4.firstOf("foo", "bar", "baz", null), "foo");
      checkSecond(Union4.firstOf(null, "foo", "bar", "baz"), "foo");
      checkFirst(Union4.firstOf("foo", "bar", "baz", "snafu"), "foo");
      checkFirst(Union4.firstOf(null, null, null, null), null);
   }

   private <T> void checkFirst(Union4<T, ?, ?, ?> union, T value) {
      assertTrue(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertEquals(value, union.get());
      assertEquals(value, union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
      
      assertEquals(union, Union4.withFirst(value));
      assertEquals(union.hashCode(), Union4.withFirst(value).hashCode());
      assertNotEquals(union, Union4.withFirst(new Object()));
      assertNotEquals(union, Union4.withSecond(value));
      assertNotEquals(union.hashCode(), Union4.withSecond(value).hashCode());
      assertNotEquals(union, Union4.withThird(value));
      assertNotEquals(union.hashCode(), Union4.withThird(value).hashCode());
      assertNotEquals(union, Union4.withFourth(value));
      assertNotEquals(union.hashCode(), Union4.withFourth(value).hashCode());
   }

   private <T> void checkSecond(Union4<?, T, ?, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertTrue(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertEquals(value, union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());

      assertEquals(union, Union4.withSecond(value));
      assertEquals(union.hashCode(), Union4.withSecond(value).hashCode());
      assertNotEquals(union, Union4.withFirst(value));
      assertNotEquals(union.hashCode(), Union4.withFirst(value).hashCode());
      assertNotEquals(union, Union4.withSecond(new Object()));
      assertNotEquals(union, Union4.withThird(value));
      assertNotEquals(union.hashCode(), Union4.withThird(value).hashCode());
      assertNotEquals(union, Union4.withFourth(value));
      assertNotEquals(union.hashCode(), Union4.withFourth(value).hashCode());
   }

   private <T> void checkThird(Union4<?, ?, T, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertTrue(union.hasThird());
      assertFalse(union.hasFourth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertEquals(value, union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());

      assertEquals(union, Union4.withThird(value));
      assertEquals(union.hashCode(), Union4.withThird(value).hashCode());
      assertNotEquals(union, Union4.withFirst(value));
      assertNotEquals(union.hashCode(), Union4.withFirst(value).hashCode());
      assertNotEquals(union, Union4.withSecond(value));
      assertNotEquals(union.hashCode(), Union4.withSecond(value).hashCode());
      assertNotEquals(union, Union4.withThird(new Object()));
      assertNotEquals(union, Union4.withFourth(value));
      assertNotEquals(union.hashCode(), Union4.withFourth(value).hashCode());
   }

   private <T> void checkFourth(Union4<?, ?, ?, T> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertTrue(union.hasFourth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertEquals(value, union.getFourth());

      assertEquals(union, Union4.withFourth(value));
      assertEquals(union.hashCode(), Union4.withFourth(value).hashCode());
      assertNotEquals(union, Union4.withFirst(value));
      assertNotEquals(union.hashCode(), Union4.withFirst(value).hashCode());
      assertNotEquals(union, Union4.withSecond(value));
      assertNotEquals(union.hashCode(), Union4.withSecond(value).hashCode());
      assertNotEquals(union, Union4.withThird(value));
      assertNotEquals(union.hashCode(), Union4.withThird(value).hashCode());
      assertNotEquals(union, Union4.withFourth(new Object()));
   }

   private <T> void checkUnion5First(Union5<T, ?, ?, ?, ?> union, T value) {
      assertTrue(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertFalse(union.hasFifth());
      assertEquals(value, union.get());
      assertEquals(value, union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
      assertThrows(NoSuchElementException.class, () -> union.getFifth());
   }

   private <T> void checkUnion5Second(Union5<?, T, ?, ?, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertTrue(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertFalse(union.hasFifth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertEquals(value, union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
      assertThrows(NoSuchElementException.class, () -> union.getFifth());
   }

   private <T> void checkUnion5Third(Union5<?, ?, T, ?, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertTrue(union.hasThird());
      assertFalse(union.hasFourth());
      assertFalse(union.hasFifth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertEquals(value, union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
      assertThrows(NoSuchElementException.class, () -> union.getFifth());
   }

   private <T> void checkUnion5Fourth(Union5<?, ?, ?, T, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertTrue(union.hasFourth());
      assertFalse(union.hasFifth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertEquals(value, union.getFourth());
      assertThrows(NoSuchElementException.class, () -> union.getFifth());
   }

   private <T> void checkUnion5Fifth(Union5<?, ?, ?, ?, T> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertFalse(union.hasFourth());
      assertTrue(union.hasFifth());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
      assertThrows(NoSuchElementException.class, () -> union.getFourth());
      assertEquals(value, union.getFifth());
   }
}
