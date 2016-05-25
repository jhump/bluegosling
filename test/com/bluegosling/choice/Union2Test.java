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

public class Union2Test {
   
   private final Union2<String, Integer> e1 = Union2.withFirst("foo");
   private final Union2<String, Integer> e2 = Union2.withSecond(123);
   
   @Test public void hasGetEqualsHashCode() {
      checkFirst(e1, "foo");
      checkSecond(e2, 123);
   }

   @Test public void tryElement() {
      Possible<String> p1a = e1.tryFirst();
      assertTrue(p1a.isPresent());
      assertEquals("foo", p1a.get());

      Possible<String> p1b = e2.tryFirst();
      assertFalse(p1b.isPresent());
      assertThrows(NoSuchElementException.class, () -> p1b.get());
      
      Possible<Integer> p2a = e1.trySecond();
      assertFalse(p2a.isPresent());
      assertThrows(NoSuchElementException.class, () -> p2a.get());

      Possible<Integer> p2b = e2.trySecond();
      assertTrue(p2b.isPresent());
      assertEquals(123, p2b.get().intValue());
   }

   @Test public void mapElement() {
      checkFirst(e1.mapFirst(s -> 456), 456);
      checkFirst(e1.mapFirst(s -> null), null);
      checkFirst(e1.mapSecond(s -> 456), "foo");
      
      checkSecond(e2.mapFirst(s -> "baz"), 123);
      checkSecond(e2.mapSecond(s -> "baz"), "baz");
      checkSecond(e2.mapSecond(s -> null), null);
   }
   
   @Test public void flatMapElement() {
      checkSecond(e1.flatMapFirst(s -> Union2.withSecond(456)), 456);
      checkFirst(e1.flatMapSecond(s -> Union2.withFirst("baz")), "foo");

      checkSecond(e2.flatMapFirst(s -> Union2.withSecond(456)), 123);
      checkFirst(e2.flatMapSecond(s -> Union2.withFirst("baz")), "baz");

      assertThrows(NullPointerException.class, () -> e1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> e2.flatMapSecond(s -> null));
   }
   
   @Test public void expandElement() {
      checkUnion3Second(e1.expandFirst(), "foo");
      checkUnion3First(e1.expandSecond(), "foo");
      checkUnion3First(e1.expandThird(), "foo");
      
      checkUnion3Third(e2.expandFirst(), 123);
      checkUnion3Third(e2.expandSecond(), 123);
      checkUnion3Second(e2.expandThird(), 123);
   }
   
   @Test public void exchangeElement() {
      checkSecond(e1.exchangeFirst(s -> 456), 456);
      checkSecond(e1.exchangeFirst(s -> null), null);
      checkFirst(e1.exchangeSecond(i -> "bar"), "foo");

      checkSecond(e2.exchangeFirst(s -> 456), 123);
      checkFirst(e2.exchangeSecond(i -> "bar"), "bar");
      checkFirst(e2.exchangeSecond(i -> null), null);
   }

   @Test public void contractElement() {
      assertEquals(456, e1.contractFirst(s -> 456).intValue());
      assertEquals("foo", e1.contractSecond(s -> "bar"));

      assertEquals(123, e2.contractFirst(s -> 456).intValue());
      assertEquals("bar", e2.contractSecond(s -> "bar"));

      assertNull(e1.contractFirst(s -> null));
      assertNull(e2.contractSecond(s -> null));
   }
   
   @Test public void visit() {
      VariableInt c1 = new VariableInt();
      Variable<String> r1 = new Variable<>();
      VariableInt c2 = new VariableInt();
      Variable<Integer> r2 = new Variable<>();
      Choice.VisitorOfTwo<String, Integer, Integer> v =
            new Choice.VisitorOfTwo<String, Integer, Integer>() {
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
            };
            
      assertEquals(1, e1.visit(v).intValue());
      assertEquals(1, c1.get());
      assertEquals("foo", r1.get());
      assertEquals(0, c2.get());
      
      c1.set(0); r1.set(null);

      assertEquals(2, e2.visit(v).intValue());
      assertEquals(1, c2.get());
      assertEquals(123, r2.get().intValue());
      assertEquals(0, c1.get());
   }
   
   @Test public void swap() {
      checkSecond(e1.swap(), "foo");
      checkFirst(e2.swap(), 123);
   }
   
   @Test public void of() {
      checkFirst(Union2.of("foo", null), "foo");
      checkSecond(Union2.of(null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> Union2.of("foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Union2.of(null, null));
   }

   @Test public void firstOf() {
      checkFirst(Union2.firstOf("foo", null), "foo");
      checkSecond(Union2.firstOf(null, "foo"), "foo");
      checkFirst(Union2.firstOf("foo", "bar"), "foo");
      checkFirst(Union2.firstOf(null, null), null);
   }

   private <T> void checkFirst(Union2<T, ?> union, T value) {
      assertTrue(union.hasFirst());
      assertFalse(union.hasSecond());
      assertEquals(value, union.get());
      assertEquals(value, union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      
      assertEquals(union, Union2.withFirst(value));
      assertEquals(union.hashCode(), Union2.withFirst(value).hashCode());
      assertNotEquals(union, Union2.withFirst(new Object()));
      assertNotEquals(union, Union2.withSecond(value));
      assertNotEquals(union.hashCode(), Union2.withSecond(value).hashCode());
   }

   private <T> void checkSecond(Union2<?, T> union, T value) {
      assertFalse(union.hasFirst());
      assertTrue(union.hasSecond());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertEquals(value, union.getSecond());

      assertEquals(union, Union2.withSecond(value));
      assertEquals(union.hashCode(), Union2.withSecond(value).hashCode());
      assertNotEquals(union, Union2.withFirst(value));
      assertNotEquals(union.hashCode(), Union2.withFirst(value).hashCode());
      assertNotEquals(union, Union2.withSecond(new Object()));
   }

   private <T> void checkUnion3First(Union3<T, ?, ?> union, T value) {
      assertTrue(union.hasFirst());
      assertFalse(union.hasSecond());
      assertFalse(union.hasThird());
      assertEquals(value, union.get());
      assertEquals(value, union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
   }

   private <T> void checkUnion3Second(Union3<?, T, ?> union, T value) {
      assertFalse(union.hasFirst());
      assertTrue(union.hasSecond());
      assertFalse(union.hasThird());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertEquals(value, union.getSecond());
      assertThrows(NoSuchElementException.class, () -> union.getThird());
   }

   private <T> void checkUnion3Third(Union3<?, ?, T> union, T value) {
      assertFalse(union.hasFirst());
      assertFalse(union.hasSecond());
      assertTrue(union.hasThird());
      assertEquals(value, union.get());
      assertThrows(NoSuchElementException.class, () -> union.getFirst());
      assertThrows(NoSuchElementException.class, () -> union.getSecond());
      assertEquals(value, union.getThird());
   }
}
