package com.apriori.possible;

import static com.apriori.util.Predicates.alwaysAccept;
import static com.apriori.util.Predicates.alwaysReject;
import static com.apriori.util.Predicates.isEqualTo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

/**
 * Test cases for the basic functionality of any {@link Possible} implementation.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractPossibleTest {

   protected abstract Possible<String> valuePresent(String s);

   protected abstract Possible<String> valueAbsent();
   
   protected void checkSingletonSet(Set<String> set, String value) {
      assertEquals(1, set.size());
      assertFalse(set.isEmpty());
      assertTrue(set.contains(value));
      assertFalse(set.contains(value == null ? "abc" : null));
      Iterator<String> iter = set.iterator();
      assertTrue(iter.hasNext());
      assertEquals(value, iter.next());
      assertFalse(iter.hasNext());
      assertEquals(Collections.singleton(value), set);
      assertEquals(Collections.singleton(value).hashCode(), set.hashCode());
   }
   
   protected void checkPresentValue(Possible<String> p, final String value) {
      assertTrue(p.isPresent());
      assertEquals(value, p.get());
      assertEquals(value, p.getOr(null));
      assertEquals(value, p.getOr(value + "xyz"));
      assertEquals(value, p.getOrThrow(new RuntimeException()));
      checkSingletonSet(p.asSet(), value);
      
      assertEquals(123, (int) p.visit(new Possible.Visitor<String, Integer>() {
         @Override
         public Integer present(String t) {
            assertEquals(value, t);
            return 123;
         }

         @Override
         public Integer absent() {
            fail("visitor.absent() should not be called");
            return null;
         }
      }));
      
      Possible<String> transformed = p.transform((s) -> s + ":xyz");
      assertTrue(transformed.isPresent());
      assertEquals(value + ":xyz", transformed.get());
      
      Possible<String> stillPresent = p.filter(isEqualTo(value));
      assertTrue(stillPresent.isPresent());
      assertEquals(value, stillPresent.get());

      assertFalse(p.filter(alwaysReject()).isPresent());
   }
   
   @Test public void present() {
      checkPresentValue(valuePresent("abc"), "abc");
   }
   
   protected void checkEmptySet(Set<String> set) {
      assertEquals(0, set.size());
      assertTrue(set.isEmpty());
      assertFalse(set.contains(null));
      assertFalse(set.contains("abc"));
      assertFalse(set.iterator().hasNext());
      assertEquals(Collections.emptySet(), set);
      assertEquals(Collections.emptySet().hashCode(), set.hashCode());
   }
   
   protected void checkAbsentValue(Possible<String> p) {
      assertFalse(p.isPresent());
      try {
         p.get();
         fail("expecting IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      assertNull(p.getOr(null));
      assertEquals("xyz", p.getOr("xyz"));
      RuntimeException r = new RuntimeException();
      try {
         p.getOrThrow(r);
         fail("expecting RuntimeException");
      } catch (RuntimeException e) {
         assertSame(r, e);
      }
      checkEmptySet(p.asSet());
      
      assertEquals(123, (int) p.visit(new Possible.Visitor<String, Integer>() {
         @Override
         public Integer present(String t) {
            fail("visitor.present() should not be called");
            return null;
         }

         @Override
         public Integer absent() {
            return 123;
         }
      }));
      
      assertFalse(p.transform((s) -> s + ":xyz").isPresent());
      
      assertFalse(p.filter(alwaysAccept()).isPresent());
   }

   @Test public void absent() {
      checkAbsentValue(valueAbsent());
   }
   
   @Test public void or() {
      Possible<String> alternate = valuePresent("xyz");
      checkPresentValue(valuePresent("abc").or(alternate), "abc");
      checkPresentValue(valueAbsent().or(alternate), "xyz");
   }

   @Test public void transform() {
      Function<String, String> function = (s) -> s + ":xyz";
      checkPresentValue(valuePresent("abc").transform(function), "abc:xyz");
      checkAbsentValue(valueAbsent().transform(function));
   }

   @Test public void filter() {
      checkPresentValue(valuePresent("abc").filter(alwaysAccept()), "abc");
      checkAbsentValue(valueAbsent().filter(alwaysAccept()));

      checkAbsentValue(valuePresent("abc").filter(alwaysReject()));
      checkAbsentValue(valueAbsent().filter(alwaysReject()));
   }
}
