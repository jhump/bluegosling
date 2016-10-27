package com.bluegosling.result;

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
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.possible.Possible;
import com.bluegosling.result.FailedResultException;
import com.bluegosling.result.Result;
import com.bluegosling.tuples.Pair;
import com.bluegosling.tuples.Triple;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

public class ResultTest {
   
   @Test public void ok() {
      checkSuccess(Result.ok("abc"), "abc", "xyz");
      checkSuccess(Result.ok(null), null, "xyz");
   }

   @Test public void error() {
      Throwable th = new Throwable();
      Throwable otherTh = new Exception();
      checkError(Result.error(th), th, otherTh, "xyz");
      checkError(Result.error(null), null, otherTh, "xyz");
   }
   
   @Test public void asOptional() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      Optional<String> opt = r.asOptional();
      assertTrue(opt.isPresent());
      assertEquals("abc", opt.get());
      
      // ok, null
      r = Result.ok(null);
      opt = r.asOptional();
      assertFalse(opt.isPresent());
      assertThrows(NoSuchElementException.class, opt::get);

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
      opt = r.asOptional();
      assertFalse(opt.isPresent());
      assertThrows(NoSuchElementException.class, opt::get);
   }
   
   @Test public void asPossible() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      Possible<String> p = r.asPossible();
      assertTrue(p.isPresent());
      assertEquals("abc", p.get());

      // ok, null
      r = Result.ok(null);
      p = r.asPossible();
      assertTrue(p.isPresent());
      assertEquals(null, p.get());

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
      p = r.asPossible();
      assertFalse(p.isPresent());
      assertThrows(NoSuchElementException.class, p::get);
   }
   
   @Test public void asFuture() throws Exception {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      FluentFuture<String> f = r.asFuture();
      assertTrue(f.isDone());
      assertTrue(f.isSuccessful());
      assertFalse(f.isFailed());
      assertFalse(f.isCancelled());
      assertEquals("abc", f.get());
      assertEquals("abc", f.getResult());

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
      f = r.asFuture();
      assertTrue(f.isDone());
      assertFalse(f.isSuccessful());
      assertTrue(f.isFailed());
      assertFalse(f.isCancelled());
      Exception e = assertThrows(ExecutionException.class, f::get);
      assertSame(th, e.getCause());
      assertSame(th, f.getFailure());
      
      // error, not throwable
      Result<String, Integer> other = Result.error(123);
      f = other.asFuture();
      assertTrue(f.isDone());
      assertFalse(f.isSuccessful());
      assertTrue(f.isFailed());
      assertFalse(f.isCancelled());
      e = assertThrows(ExecutionException.class, f::get);
      FailedResultException failed = (FailedResultException) e.getCause();
      assertEquals(123, failed.getFailure());
      failed = (FailedResultException) f.getFailure();
      assertEquals(123, failed.getFailure());
   }
   
   @Test public void map() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      Result<String, Throwable> mapped = r.map(s -> "xyz");
      checkSuccess(mapped, "xyz", "abc");

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
      assertSame(r, r.map(s -> "abc"));
   }
   
   @Test public void mapError() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      assertSame(r, r.mapError(t -> new IllegalStateException()));

      // error
      Throwable th = new Throwable();
      Throwable otherTh = new Exception();
      r = Result.error(th);
      Result<String, Throwable> mapped = r.mapError(t -> otherTh);
      checkError(mapped, otherTh, th, "abc");
   }
   
   @Test public void flatMap() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      Result<String, Throwable> flatMapped = r.flatMap(s -> Result.ok("xyz"));
      checkSuccess(flatMapped, "xyz", "abc");

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
      assertSame(r, r.flatMap(s -> Result.ok("abc")));
   }
   
   @Test public void recover() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      assertSame(r, r.recover(t -> "xyz"));

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
      Result<String, Throwable> recovered = r.recover(t -> "abc");
      checkSuccess(recovered, "abc", "xyz");
   }
   
   @Test public void visit() {
      // ok
      Result<String, Throwable> r = Result.ok("abc");
      assertEquals(123, (int) r.visit(new Result.Visitor<String, Throwable, Integer>() {
         @Override
         public Integer visitValue(String t) {
            assertEquals("abc", t);
            return 123;
         }

         @Override
         public Integer visitError(Throwable th) {
            fail("visitor.visitError() should not be called");
            return null;
         }
      }));

      // error
      Throwable th = new Throwable();
      r = Result.error(th);
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
   
   @Test public void dereference() {
      // ok
      Result<String, Exception> r1 = Result.ok("abc");
      Result<Result<String, Exception>, RuntimeException> nested = Result.ok(r1);
      Result<String, Exception> unnested = Result.dereference(nested);
      checkSuccess(unnested, "abc", "xyz");
      
      // error
      RuntimeException e = new RuntimeException();
      nested = Result.error(e);
      unnested = Result.dereference(nested);
      checkError(unnested, e, new IllegalArgumentException(), "abc");
   }
   
   @Test public void fromCompletedFuture() {
      // ok
      Result<String, Throwable> r = Result.fromCompletedFuture(Futures.immediateFuture("abc"));
      checkSuccess(r, "abc", "xyz");
      r = Result.fromCompletedFuture(FluentFuture.completedFuture("abc"));
      checkSuccess(r, "abc", "xyz");
      
      // error
      Throwable th = new Throwable();
      Throwable otherTh = new Exception();
      r = Result.fromCompletedFuture(Futures.immediateFailedFuture(th));
      checkError(r, th, otherTh, "abc");
      r = Result.fromCompletedFuture(FluentFuture.failedFuture(th));
      checkError(r, th, otherTh, "abc");
   }
   
   @Test public void checkedGet() throws Throwable {
      assertEquals("abc", Result.checkedGet(Result.ok("abc")));
      
      Throwable th = new Throwable();
      Throwable thrown = assertThrows(Throwable.class, () -> Result.checkedGet(Result.error(th)));
      assertSame(th, thrown);
   }
   
   @Test public void firstSuccessfulOf() {
      List<Result<String, String>> results;
      
      results = ImmutableList.of(Result.ok("foo"), Result.error("bar"), Result.error("baz"));
      assertSame(results.get(0), Result.firstSuccessfulOf(results));
      
      results = ImmutableList.of(Result.error("foo"), Result.ok("bar"), Result.error("baz"));
      assertSame(results.get(1), Result.firstSuccessfulOf(results));

      results = ImmutableList.of(Result.error("foo"), Result.error("bar"), Result.ok("baz"));
      assertSame(results.get(2), Result.firstSuccessfulOf(results));
   }

   @Test public void firstSuccessfulOf_noneSuccessful() {
      List<Result<String, String>> results;
      // returns last result if none successful
      
      results = ImmutableList.of(Result.error("foo"));
      assertSame(results.get(0), Result.firstSuccessfulOf(results));

      results = ImmutableList.of(Result.error("foo"), Result.error("bar"), Result.error("baz"));
      assertSame(results.get(2), Result.firstSuccessfulOf(results));
   }
   
   @Test public void firstSuccessfulOf_noResults() {
      assertThrows(NoSuchElementException.class,
            () -> Result.firstSuccessfulOf(ImmutableList.of()));
   }
   
   @Test public void join() {
      List<Result<String, Throwable>> results = new ArrayList<>(3);
      results.add(Result.ok("foo"));
      results.add(Result.ok("bar"));
      results.add(Result.ok("baz"));
      List<String> expected = ImmutableList.of("foo", "bar", "baz");
      List<String> other = ImmutableList.of("snafu", "fubar");
      
      checkSuccess(Result.join(results), expected, other);
      
      Throwable th1 = new Throwable();
      Throwable th2 = new Exception();
      results.set(1, Result.error(th1));
      
      checkError(Result.join(results), th1, th2, other);
      
      Throwable th3 = new Throwable();
      results.set(0, Result.error(th2));

      checkError(Result.join(results), th2, th3, other);
   }
   
   @Test public void combineWith_2() {
      Result<String, String> r1 = Result.ok("ok");
      Result<String, String> r2 = Result.ok("ko");
      
      Pair<String, String> expected = Pair.of("ok", "ko");
      Pair<String, String> other = Pair.of("foo", "bar");
    
      checkSuccess(r1.combineWith(r2, Pair::of), expected, other);
      
      Result<String, String> failed = Result.error("kaboom!");
      
      checkError(failed.combineWith(r1, Pair::of), "kaboom!", "pow!", other);
      checkError(r2.combineWith(failed, Pair::of), "kaboom!", "pow!", other);
   }
   
   @Test public void combineWith_3() {
      Result<String, String> r1 = Result.ok("ok");
      Result<String, String> r2 = Result.ok("ko");
      Result<String, String> r3 = Result.ok("OK");
      
      Triple<String, String, String> expected = Triple.of("ok", "ko", "OK");
      Triple<String, String, String> other = Triple.of("foo", "bar", "baz");
    
      checkSuccess(r1.combineWith(r2, r3, Triple::of), expected, other);
      
      Result<String, String> failed = Result.error("kaboom!");
      
      checkError(failed.combineWith(r1, r2, Triple::of), "kaboom!", "pow!", other);
      checkError(r2.combineWith(r3, failed, Triple::of), "kaboom!", "pow!", other);
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
   
   private <T, E> void checkSuccess(Result<T, E> r, T value, T other) {
      assertTrue(r.isSuccessful());
      assertEquals(value, r.get());
      assertEquals(value, r.orElse(null));
      assertEquals(value, r.orElse(other));
      assertEquals(value, r.orElseGet(() -> other));
      assertEquals(value, r.orElseThrow(() -> new RuntimeException()));

      assertFalse(r.isFailed());
      assertThrows(IllegalStateException.class, () -> r.getFailure());
      
      List<Object> list = new ArrayList<>();
      r.ifFailed(list::add);
      assertEquals(Collections.emptyList(), list);
      r.ifSuccessful(list::add);
      assertEquals(Arrays.asList(value), list);
   }
   
   private <T, E> void checkError(Result<T, E> r, E th, E otherTh, T value) {
      assertFalse(r.isSuccessful());
      assertThrows(NoSuchElementException.class, () -> r.get());
      assertEquals(null, r.orElse(null));
      assertEquals(value, r.orElse(value));
      assertEquals(value, r.orElseGet(() -> value));
      RuntimeException ex = new RuntimeException();
      assertSame(ex, assertThrows(RuntimeException.class, () -> r.orElseThrow(() -> ex)));

      assertTrue(r.isFailed());
      assertSame(th, r.getFailure());
      
      List<Object> list = new ArrayList<>();
      r.ifSuccessful(list::add);
      assertEquals(Collections.emptyList(), list);
      r.ifFailed(list::add);
      assertEquals(Arrays.asList(th), list);
   }
}
