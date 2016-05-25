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
import java.util.Optional;

import org.junit.Test;

import com.bluegosling.possible.Possible;
import com.bluegosling.tuples.Pair;
import com.bluegosling.tuples.Trio;
import com.google.common.collect.ImmutableList;

public class ResultTest {
   
   // TODO: combine, join

   private <T, E> void checkSuccess(Result<T, E> r, T value, T other, boolean recurse) {
      assertTrue(r.isSuccessful());
      assertEquals(value, r.get());
      assertEquals(value, r.orElse(null));
      assertEquals(value, r.orElse(other));
      assertEquals(value, r.orElseGet(() -> other));
      assertEquals(value, r.orElseThrow(() -> new RuntimeException()));

      assertFalse(r.isFailed());
      assertThrows(IllegalStateException.class, () -> r.getFailure());
      
      Optional<T> opt = r.asOptional();
      if (value == null) {
         assertFalse(opt.isPresent());
         assertThrows(NoSuchElementException.class, () -> opt.get());
      } else {
         assertTrue(opt.isPresent());
         assertEquals(value, opt.get());
      }

      Possible<T> p = r.asPossible();
      assertTrue(p.isPresent());
      assertEquals(value, p.get());

      assertEquals(123, (int) r.visit(new Result.Visitor<T, E, Integer>() {
         @Override
         public Integer visitValue(T t) {
            assertEquals(value, t);
            return 123;
         }

         @Override
         public Integer visitError(E th) {
            fail("visitor.visitError() should not be called");
            return null;
         }
      }));
      
      Result<T, E> mapped = r.map(s -> other);
      if (recurse) {
         checkSuccess(mapped, other, value, false);
      } else {
         assertTrue(mapped.isSuccessful());
         assertFalse(mapped.isFailed());
         assertEquals(other, mapped.get());
      }

      Result<T, E> flatMapped = r.flatMap(s -> Result.ok(other));
      if (recurse) {
         checkSuccess(flatMapped, other, value, false);
      } else {
         assertTrue(flatMapped.isSuccessful());
         assertFalse(flatMapped.isFailed());
         assertEquals(other, flatMapped.get());
      }
      
      assertSame(r, r.recover(th -> other));
      assertSame(r, r.mapException(th -> new IllegalStateException()));

      List<Object> list = new ArrayList<>();
      r.ifFailed(list::add);
      assertEquals(Collections.emptyList(), list);
      r.ifSuccessful(list::add);
      assertEquals(Arrays.asList(value), list);
   }
   
   private <T, E> void checkError(Result<T, E> r, E th, E otherTh, T value, boolean recurse) {
      assertFalse(r.isSuccessful());
      assertThrows(NoSuchElementException.class, () -> r.get());
      assertEquals(null, r.orElse(null));
      assertEquals(value, r.orElse(value));
      assertEquals(value, r.orElseGet(() -> value));
      RuntimeException ex = new RuntimeException();
      assertSame(ex, assertThrows(RuntimeException.class, () -> r.orElseThrow(() -> ex)));

      assertTrue(r.isFailed());
      assertSame(th, r.getFailure());
      
      Optional<T> opt = r.asOptional();
      assertFalse(opt.isPresent());
      assertThrows(NoSuchElementException.class, () -> opt.get());

      Possible<T> p = r.asPossible();
      assertFalse(p.isPresent());
      assertThrows(NoSuchElementException.class, () -> p.get());

      assertEquals(123, (int) r.visit(new Result.Visitor<T, E, Integer>() {
         @Override
         public Integer visitValue(T t) {
            fail("visitor.visitValue() should not be called");
            return null;
         }

         @Override
         public Integer visitError(E t) {
            assertEquals(th, t);
            return 123;
         }
      }));
      
      assertSame(r, r.map(s -> value));
      assertSame(r, r.flatMap(s -> Result.ok(value)));

      Result<T, E> mapped = r.mapException(t -> otherTh);
      if (recurse) {
         checkError(mapped, otherTh, th, value, false);
      } else {
         assertFalse(mapped.isSuccessful());
         assertTrue(mapped.isFailed());
         assertSame(otherTh, mapped.getFailure());
      }

      Result<T, E> recovered = r.recover(t -> value);
      if (recurse) {
         checkSuccess(recovered, value, value, false);
      } else {
         assertTrue(recovered.isSuccessful());
         assertFalse(recovered.isFailed());
         assertEquals(value, recovered.get());
      }

      List<Object> list = new ArrayList<>();
      r.ifSuccessful(list::add);
      assertEquals(Collections.emptyList(), list);
      r.ifFailed(list::add);
      assertEquals(Arrays.asList(th), list);
   }
   
   @Test public void ok() {
      checkSuccess(Result.ok("abc"), "abc", "xyz", true);
      checkSuccess(Result.ok(null), null, "xyz", true);
   }

   @Test public void error() {
      Throwable th = new Throwable();
      Throwable otherTh = new Exception();
      checkError(Result.error(th), th, otherTh, "xyz", true);
      checkError(Result.error(null), null, otherTh, "xyz", true);
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
   
   @Test public void join() {
      List<Result<String, Throwable>> results = new ArrayList<>(3);
      results.add(Result.ok("foo"));
      results.add(Result.ok("bar"));
      results.add(Result.ok("baz"));
      List<String> expected = ImmutableList.of("foo", "bar", "baz");
      List<String> other = ImmutableList.of("snafu", "fubar");
      
      checkSuccess(Result.join(results), expected, other, true);
      
      Throwable th1 = new Throwable();
      Throwable th2 = new Exception();
      results.set(1, Result.error(th1));
      
      checkError(Result.join(results), th1, th2, other, true);
      
      Throwable th3 = new Throwable();
      results.set(0, Result.error(th2));

      checkError(Result.join(results), th2, th3, other, true);
   }
   
   @Test public void combineWith_2() {
      Result<String, String> r1 = Result.ok("ok");
      Result<String, String> r2 = Result.ok("ko");
      
      Pair<String, String> expected = Pair.create("ok", "ko");
      Pair<String, String> other = Pair.create("foo", "bar");
    
      checkSuccess(r1.combineWith(r2, Pair::create), expected, other, true);
      
      Result<String, String> failed = Result.error("kaboom!");
      
      checkError(failed.combineWith(r1, Pair::create), "kaboom!", "pow!", other, true);
      checkError(r2.combineWith(failed, Pair::create), "kaboom!", "pow!", other, true);
   }
   
   @Test public void combineWith_3() {
      Result<String, String> r1 = Result.ok("ok");
      Result<String, String> r2 = Result.ok("ko");
      Result<String, String> r3 = Result.ok("OK");
      
      Trio<String, String, String> expected = Trio.create("ok", "ko", "OK");
      Trio<String, String, String> other = Trio.create("foo", "bar", "baz");
    
      checkSuccess(r1.combineWith(r2, r3, Trio::create), expected, other, true);
      
      Result<String, String> failed = Result.error("kaboom!");
      
      checkError(failed.combineWith(r1, r2, Trio::create), "kaboom!", "pow!", other, true);
      checkError(r2.combineWith(r3, failed, Trio::create), "kaboom!", "pow!", other, true);
   }
   
   @Test public void successfulOnly() {
      List<Result<Integer, Integer>> r =
            ImmutableList.of(Result.ok(1), Result.ok(2), Result.ok(3));
      assertEquals(ImmutableList.of(1, 2, 3), Result.successfulOnly(r));

      r = ImmutableList.of(Result.error(1), Result.ok(2), Result.ok(3));
      assertEquals(ImmutableList.of(2, 3), Result.successfulOnly(r));

      r = ImmutableList.of(Result.ok(1), Result.error(2), Result.ok(3));
      assertEquals(ImmutableList.of(1, 3), Result.successfulOnly(r));

      r = ImmutableList.of(Result.ok(1), Result.ok(2), Result.error(3));
      assertEquals(ImmutableList.of(1, 2), Result.successfulOnly(r));

      r = ImmutableList.of(Result.error(1), Result.error(2), Result.error(3));
      assertEquals(ImmutableList.of(), Result.successfulOnly(r));
   }
   
   @Test public void failedOnly() {
      List<Result<Integer, Integer>> r =
            ImmutableList.of(Result.ok(1), Result.ok(2), Result.ok(3));
      assertEquals(ImmutableList.of(), Result.failedOnly(r));

      r = ImmutableList.of(Result.error(1), Result.ok(2), Result.ok(3));
      assertEquals(ImmutableList.of(1), Result.failedOnly(r));

      r = ImmutableList.of(Result.ok(1), Result.error(2), Result.ok(3));
      assertEquals(ImmutableList.of(2), Result.failedOnly(r));

      r = ImmutableList.of(Result.ok(1), Result.ok(2), Result.error(3));
      assertEquals(ImmutableList.of(3), Result.failedOnly(r));

      r = ImmutableList.of(Result.error(1), Result.error(2), Result.error(3));
      assertEquals(ImmutableList.of(1, 2, 3), Result.failedOnly(r));
   }
   
   @Test public void nullIfFailed() {
      List<Result<Integer, Integer>> r =
            ImmutableList.of(Result.ok(1), Result.ok(2), Result.ok(3));
      assertEquals(ImmutableList.of(1, 2, 3), Result.nullIfFailed(r));

      r = ImmutableList.of(Result.error(1), Result.ok(2), Result.ok(3));
      assertEquals(Arrays.asList(null, 2, 3), Result.nullIfFailed(r));

      r = ImmutableList.of(Result.ok(1), Result.error(2), Result.ok(3));
      assertEquals(Arrays.asList(1, null, 3), Result.nullIfFailed(r));

      r = ImmutableList.of(Result.ok(1), Result.ok(2), Result.error(3));
      assertEquals(Arrays.asList(1, 2, null), Result.nullIfFailed(r));

      r = ImmutableList.of(Result.error(1), Result.error(2), Result.error(3));
      assertEquals(Arrays.asList(null, null, null), Result.nullIfFailed(r));
   }
}
