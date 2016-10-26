package com.bluegosling.possible;

import static com.bluegosling.concurrent.fluent.FutureListener.forRunnable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.SameThreadExecutor;
import com.bluegosling.concurrent.extras.FuturePossibles;
import com.bluegosling.concurrent.fluent.SettableFluentFuture;
import com.bluegosling.possible.Fulfillable;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for the {@link Fulfillable} implementation returned by
 * {@link Fulfillable#fromFuture(SettableFluentFuture)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FulfillableFromFluentFutureTest extends AbstractPossibleTest {
   @Override
   protected Fulfillable<String> valuePresent(String s) {
      SettableFluentFuture<String> future = new SettableFluentFuture<String>();
      future.setValue(s);
      return FuturePossibles.fulfillableFromFuture(future);
   }

   @Override
   protected Fulfillable<String> valueAbsent() {
      return FuturePossibles.fulfillableFromFuture(new SettableFluentFuture<String>());
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
      SettableFluentFuture<String> future = new SettableFluentFuture<String>();
      final AtomicInteger listenerCount = new AtomicInteger();
      future.addListener(forRunnable(new Runnable() {
         @Override public void run() {
            listenerCount.incrementAndGet();
         }
      }), SameThreadExecutor.get());
      f = FuturePossibles.fulfillableFromFuture(future);
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
