package com.bluegosling.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static com.bluegosling.testing.MoreAsserts.assertNotEquals;
import static com.bluegosling.testing.MoreAsserts.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ResultTest {

   private void checkSuccess(Result<String, Throwable> r, String value, boolean recurse) {
      assertTrue(r.isSuccessful());
      assertEquals(value, r.get());
      assertEquals(value, r.orElse(null));
      assertEquals(value, r.orElse(value + "xyz"));
      assertEquals(value, r.orElseGet(() -> "xyz"));
      assertEquals(value, r.orElseThrow(() -> new RuntimeException()));

      assertFalse(r.isFailed());
      assertThrows(IllegalStateException.class, () -> r.getFailure());

      assertEquals(123, (int) r.visit(new Result.Visitor<String, Throwable, Integer>() {
         @Override
         public Integer visitValue(String t) {
            assertEquals(value, t);
            return 123;
         }

         @Override
         public Integer visitError(Throwable th) {
            fail("visitor.visitError() should not be called");
            return null;
         }
      }));
      
      Result<String, Throwable> mapped = r.map(s -> s + ":xyz");
      if (recurse) {
         checkSuccess(mapped, value + ":xyz", false);
      } else {
         assertTrue(mapped.isSuccessful());
         assertFalse(mapped.isFailed());
         assertEquals(value + ":xyz", mapped.get());
      }

      Result<String, Throwable> flatMapped = r.flatMap(s -> Result.ok(s + "s"));
      if (recurse) {
         checkSuccess(flatMapped, value + "s", false);
      } else {
         assertTrue(flatMapped.isSuccessful());
         assertFalse(flatMapped.isFailed());
         assertEquals(value + "s", flatMapped.get());
      }
      
      assertSame(r, r.recover(th -> "abc"));
      assertSame(r, r.mapException(th -> new IllegalStateException()));

      List<Object> list = new ArrayList<>();
      r.ifFailed(list::add);
      assertEquals(Collections.emptyList(), list);
      r.ifSuccessful(list::add);
      assertEquals(Arrays.asList(value), list);
   }
   
   private void checkError(Result<String, Throwable> r, Throwable th, boolean recurse) {
      assertFalse(r.isSuccessful());
      assertThrows(NoSuchElementException.class, () -> r.get());
      assertEquals(null, r.orElse(null));
      assertEquals("xyz", r.orElse("xyz"));
      assertEquals("xyz", r.orElseGet(() -> "xyz"));
      RuntimeException ex = new RuntimeException();
      assertSame(ex, assertThrows(RuntimeException.class, () -> r.orElseThrow(() -> ex)));

      assertTrue(r.isFailed());
      assertSame(th, r.getFailure());

      assertEquals(123, (int) r.visit(new Result.Visitor<String, Throwable, Integer>() {
         @Override
         public Integer visitValue(String t) {
            fail("visitor.visitValue() should not be called");
            return null;
         }

         @Override
         public Integer visitError(Throwable t) {
            assertEquals(th, t);
            return 123;
         }
      }));
      
      assertSame(r, r.map(s -> s + ":xyz"));
      assertSame(r, r.flatMap(s -> Result.ok(s + "s")));

      Result<String, Throwable> mapped = r.mapException(t -> ex);
      if (recurse) {
         checkError(mapped, ex, false);
      } else {
         assertFalse(mapped.isSuccessful());
         assertTrue(mapped.isFailed());
         assertSame(ex, mapped.getFailure());
      }

      Result<String, Throwable> recovered = r.recover(t -> "s");
      if (recurse) {
         checkSuccess(recovered, "s", false);
      } else {
         assertTrue(recovered.isSuccessful());
         assertFalse(recovered.isFailed());
         assertEquals("s", recovered.get());
      }

      List<Object> list = new ArrayList<>();
      r.ifSuccessful(list::add);
      assertEquals(Collections.emptyList(), list);
      r.ifFailed(list::add);
      assertEquals(Arrays.asList(th), list);
   }
   
   @Test public void ok() {
      checkSuccess(Result.ok("abc"), "abc", true);
      checkSuccess(Result.ok(null), null, true);
   }

   @Test public void error() {
      Throwable th = new Throwable();
      checkError(Result.error(th), th, true);
      checkError(Result.error(null), null, true);
   }
   
   @Test public void equals_hashCode() {
      Result<Integer, Integer> ok1 = Result.ok(100);
      Result<Integer, Integer> ok2 = Result.ok(100);
      Result<Integer, Integer> ok3 = Result.ok(null);
      Result<Integer, Integer> err1 = Result.error(100);
      Result<Integer, Integer> err2 = Result.error(100);
      Result<Integer, Integer> err3 = Result.error(null);
      
      assertEquals(ok1, ok1);
      assertEquals(ok1, ok2);
      assertEquals(ok1.hashCode(), ok2.hashCode());
      
      assertEquals(err1, err1);
      assertEquals(err1, err2);
      assertEquals(err1.hashCode(), err2.hashCode());
      
      assertNotEquals(ok1, ok3);
      assertNotEquals(ok1.hashCode(), ok3.hashCode());
      
      assertNotEquals(err1, err3);
      assertNotEquals(err1.hashCode(), err3.hashCode());
      
      assertNotEquals(ok1, err1);
      assertNotEquals(ok1.hashCode(), err1.hashCode());
      
      assertNotEquals(ok3, err3);
      assertNotEquals(ok3.hashCode(), err3.hashCode());
   }
}
