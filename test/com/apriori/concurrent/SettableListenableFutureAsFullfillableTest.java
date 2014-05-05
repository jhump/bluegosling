package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListener.forRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.apriori.possible.AbstractPossibleTest;
import com.apriori.possible.Fulfillable;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for the {@link Fulfillable} implementation returned by
 * {@link SettableFuture#asFulfillable()}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SettableListenableFutureAsFullfillableTest extends AbstractPossibleTest {
   @Override
   protected Fulfillable<String> valuePresent(String s) {
      SettableFuture<String> future = new SettableFuture<String>();
      future.setValue(s);
      return future.asFulfillable();
   }

   @Override
   protected Fulfillable<String> valueAbsent() {
      return new SettableFuture<String>().asFulfillable();
   }

   @Test public void fulfill() throws Exception {
      // already fulfilled cannot be fulfilled again
      Fulfillable<String> f = valuePresent("abc");
      Set<String> set = f.asSet();
      checkSingletonSet(set, "abc");
      assertFalse(f.fulfill("xyz"));
      checkPresentValue(f, "abc"); // value not changed
      checkSingletonSet(set, "abc");

      // initially unfulfilled
      SettableFuture<String> future = new SettableFuture<String>();
      final AtomicInteger listenerCount = new AtomicInteger();
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenerCount.incrementAndGet();
         }
      }), SameThreadExecutor.get());
      f = future.asFulfillable();
      set = f.asSet();
      checkEmptySet(set);
      // Fullfill!
      assertTrue(f.fulfill("xyz"));
      checkSingletonSet(set, "xyz"); // change is visible through set view
      checkPresentValue(f, "xyz");
      // and future is now done
      assertTrue(future.isDone());
      assertTrue(future.isSuccessful());
      assertFalse(future.isFailed());
      assertFalse(future.isCancelled());
      assertEquals(1, listenerCount.get());
      assertEquals("xyz", future.getResult());
      assertEquals("xyz", future.get());
      
      // can only fulfill once
      assertFalse(f.fulfill("abc"));
      checkPresentValue(f, "xyz");
      checkSingletonSet(set, "xyz");
   }
}
