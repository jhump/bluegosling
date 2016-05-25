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

public class EitherTest {
   
   private final Either<String, Integer> e1 = Either.withFirst("foo");
   private final Either<String, Integer> e2 = Either.withSecond(123);
   
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
      checkFirst(e1.mapSecond(s -> 456), "foo");
      
      checkSecond(e2.mapFirst(s -> "baz"), 123);
      checkSecond(e2.mapSecond(s -> "baz"), "baz");
      
      assertThrows(NullPointerException.class, () -> e1.mapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> e2.mapSecond(s -> null));
   }
   
   @Test public void flatMapElement() {
      checkSecond(e1.flatMapFirst(s -> Either.withSecond(456)), 456);
      checkFirst(e1.flatMapSecond(s -> Either.withFirst("baz")), "foo");

      checkSecond(e2.flatMapFirst(s -> Either.withSecond(456)), 123);
      checkFirst(e2.flatMapSecond(s -> Either.withFirst("baz")), "baz");

      assertThrows(NullPointerException.class, () -> e1.flatMapFirst(s -> null));
      assertThrows(NullPointerException.class, () -> e2.flatMapSecond(s -> null));
   }
   
   @Test public void expandElement() {
      checkAnyOfThreeSecond(e1.expandFirst(), "foo");
      checkAnyOfThreeFirst(e1.expandSecond(), "foo");
      checkAnyOfThreeFirst(e1.expandThird(), "foo");
      
      checkAnyOfThreeThird(e2.expandFirst(), 123);
      checkAnyOfThreeThird(e2.expandSecond(), 123);
      checkAnyOfThreeSecond(e2.expandThird(), 123);
   }
   
   @Test public void exchangeElement() {
      checkSecond(e1.exchangeFirst(s -> 456), 456);
      checkFirst(e1.exchangeSecond(i -> "bar"), "foo");

      checkSecond(e2.exchangeFirst(s -> 456), 123);
      checkFirst(e2.exchangeSecond(i -> "bar"), "bar");

      assertThrows(NullPointerException.class, () -> e1.exchangeFirst(s -> null));
      assertThrows(NullPointerException.class, () -> e2.exchangeSecond(s -> null));
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
   
   @Test public void withNullElement() {
      assertThrows(NullPointerException.class, () -> Either.withFirst(null));
      assertThrows(NullPointerException.class, () -> Either.withSecond(null));
   }

   @Test public void of() {
      checkFirst(Either.of("foo", null), "foo");
      checkSecond(Either.of(null, "foo"), "foo");
      assertThrows(IllegalArgumentException.class, () -> Either.of("foo", "foo"));
      assertThrows(IllegalArgumentException.class, () -> Either.of(null, null));
   }

   @Test public void firstOf() {
      checkFirst(Either.firstOf("foo", null), "foo");
      checkSecond(Either.firstOf(null, "foo"), "foo");
      checkFirst(Either.firstOf("foo", "bar"), "foo");
      assertThrows(IllegalArgumentException.class, () -> Either.firstOf(null, null));
   }
   
   private <T> void checkFirst(Either<T, ?> either, T value) {
      assertTrue(either.hasFirst());
      assertFalse(either.hasSecond());
      assertEquals(value, either.get());
      assertEquals(value, either.getFirst());
      assertThrows(NoSuchElementException.class, () -> either.getSecond());
      
      assertEquals(either, Either.withFirst(value));
      assertEquals(either.hashCode(), Either.withFirst(value).hashCode());
      assertNotEquals(either, Either.withFirst(new Object()));
      assertNotEquals(either, Either.withSecond(value));
      assertNotEquals(either.hashCode(), Either.withSecond(value).hashCode());
   }

   private <T> void checkSecond(Either<?, T> either, T value) {
      assertFalse(either.hasFirst());
      assertTrue(either.hasSecond());
      assertEquals(value, either.get());
      assertThrows(NoSuchElementException.class, () -> either.getFirst());
      assertEquals(value, either.getSecond());

      assertEquals(either, Either.withSecond(value));
      assertEquals(either.hashCode(), Either.withSecond(value).hashCode());
      assertNotEquals(either, Either.withFirst(value));
      assertNotEquals(either.hashCode(), Either.withFirst(value).hashCode());
      assertNotEquals(either, Either.withSecond(new Object()));
   }

   private <T> void checkAnyOfThreeFirst(AnyOfThree<T, ?, ?> any, T value) {
      assertTrue(any.hasFirst());
      assertFalse(any.hasSecond());
      assertFalse(any.hasThird());
      assertEquals(value, any.get());
      assertEquals(value, any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
   }

   private <T> void checkAnyOfThreeSecond(AnyOfThree<?, T, ?> any, T value) {
      assertFalse(any.hasFirst());
      assertTrue(any.hasSecond());
      assertFalse(any.hasThird());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertEquals(value, any.getSecond());
      assertThrows(NoSuchElementException.class, () -> any.getThird());
   }

   private <T> void checkAnyOfThreeThird(AnyOfThree<?, ?, T> any, T value) {
      assertFalse(any.hasFirst());
      assertFalse(any.hasSecond());
      assertTrue(any.hasThird());
      assertEquals(value, any.get());
      assertThrows(NoSuchElementException.class, () -> any.getFirst());
      assertThrows(NoSuchElementException.class, () -> any.getSecond());
      assertEquals(value, any.getThird());
   }
}
